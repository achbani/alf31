package org.alfresco.webscripts.extract;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.service.cmr.repository.*;
import org.alfresco.service.cmr.search.*;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.extensions.webscripts.*;

public class ExtractWebScript extends DeclarativeWebScript {
    private static final Log logger = LogFactory.getLog(ExtractWebScript.class);
    private static final int DEFAULT_BATCH_SIZE = 50;
    private static final String LOG_FILE_PREFIX = "Export_";
    private static final String LOG_FILE_SUFFIX = ".log";

    public static final String MODEL_NAMESPACE = "http://www.alfresco.org/model/gedaff/1.0";
    public static final QName ASPECT_CHECKED = QName.createQName(MODEL_NAMESPACE, "checked");

    private final AtomicInteger docCount = new AtomicInteger(0);
    private final Set<String> processedSiteIds = new HashSet<>();

    private NodeService nodeService;
    private SearchService searchService;
    private ContentService contentService;
    private Repository repository;
    private RetryingTransactionHelper retryingTransactionHelper;

    private String extractionPath;
    private int maxDocs;
    private NodeRef logFileRef;
    private ContentWriter contentWriter;

    /*
     *  Main method that handles the web script execution.
     *  It performs two passes of document extraction based on different date ranges.
     * */
    @Override
    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
        try{
            return doExecuteImpl(req);
        } catch (Exception e) {
            logToFileAndConsole("ERROR", "Error during export process: " + e.getMessage());
            return Collections.singletonMap("message", "Error during export process: " + e.getMessage());
        }
    }

    private Map<String, Object> doExecuteImpl(WebScriptRequest req) {
        initializeParameters(req);
        initLogFile();
        logToFileAndConsole("INFO", String.format("Starting export process. Max docs: %d, Path: %s", maxDocs, extractionPath));

        Map<String, Object> model = new HashMap<>();

        // Get parameters from the request
        String maxDocsParam = req.getParameter("maxDocs");
        String extractionPathParam = req.getParameter("extractionPath");

        if (maxDocsParam != null) {
            maxDocs = Integer.parseInt(maxDocsParam);
        }

        if (extractionPathParam != null) {
            extractionPath = extractionPathParam;
        }

        // Pass 1: Focuses on ensuring diversity in the extracted documents.
        // It selects at least 10 sites from each region in the repository.
        firstPass();

        // Extracts documents from sites created after 2020,
        // continuing until a maximum number of documents (maxDocs) is reached.
        secondPass();
        String exitMessage = String.format("Export completed. Processed %d documents.", docCount.get());
        logToFileAndConsole("INFO", exitMessage);
        closeLogFile();
        model.put("message", exitMessage);
        return model;
    }

    private void initializeParameters(WebScriptRequest req) {
        Optional.ofNullable(req.getParameter("maxDocs"))
                .ifPresent(param -> maxDocs = Integer.parseInt(param));
        Optional.ofNullable(req.getParameter("extractionPath"))
                .ifPresent(path -> extractionPath = path);
    }

    private void firstPass(){
        for (int region = 1; region <= 9; region++) {
            String query = "ASPECT:\"gedaff:siteArchive\" AND NOT ASPECT:\"gedaff:checked\"" +
                    " AND gedaff:perimetre:" + region +
                    " AND cm:created:[MIN TO 2020>";
            SearchParameters sp = new SearchParameters();
            sp.addStore(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);
            sp.setLanguage(SearchService.LANGUAGE_FTS_ALFRESCO);
            sp.setQuery(query);
            sp.setLimitBy(LimitBy.FINAL_SIZE);
            sp.setLimit(10);
            sp.setQueryConsistency(QueryConsistency.TRANSACTIONAL_IF_POSSIBLE);

            ResultSet results = null;
            try {
                results = searchService.query(sp);
                for(int i=0; i< results.length(); i++) {
                    NodeRef siteRef = results.getNodeRef(i);
                    logToFileAndConsole("DEBUG","Site found: " + siteRef);
                    if (!processedSiteIds.contains(siteRef.getId())) {
                        AuthenticationUtil.RunAsWork<Void> addAspectWork = () -> {
                            extractDocuments(siteRef);
                            RetryingTransactionHelper.RetryingTransactionCallback<Void> addAspectCallback = () -> {
                                nodeService.addAspect(siteRef, ASPECT_CHECKED, null);
                                return null;
                            };
                            return retryingTransactionHelper.doInTransaction(addAspectCallback);
                        };
                        AuthenticationUtil.runAs(addAspectWork, AuthenticationUtil.getSystemUserName());
                        processedSiteIds.add(siteRef.getId());
                    } else {
                        logToFileAndConsole("DEBUG", "Site already processed (flagged): " + siteRef);
                    }
                }
            } finally {
                if (results != null) {
                    results.close();
                }
            }
        }
    }

    private void secondPass(){
        int skipCount = 0;
        while (docCount.get() < maxDocs) {
            String query = "ASPECT:\"gedaff:siteArchive\" AND NOT ASPECT:\"gedaff:checked\"" +
                    " AND cm:created:[2020 TO MAX]";

            SearchParameters sp = new SearchParameters();
            sp.addStore(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);
            sp.setLanguage(SearchService.LANGUAGE_FTS_ALFRESCO);
            sp.setQuery(query);
            sp.setSkipCount(skipCount);
            sp.setMaxItems(DEFAULT_BATCH_SIZE);
            sp.setLimitBy(LimitBy.FINAL_SIZE);
            sp.setQueryConsistency(QueryConsistency.TRANSACTIONAL_IF_POSSIBLE);

            ResultSet results = null;
            boolean processedAnySites = false;

            try {
                results = searchService.query(sp);

                // If no results found, exit the loop
                if (results.length() == 0) {
                    logToFileAndConsole("INFO", "No more results found. Ending extraction.");
                    break;
                }

                for (ResultSetRow row : results) {
                    NodeRef siteRef = row.getNodeRef();
                    if (!processedSiteIds.contains(siteRef.getId())) {
                        AuthenticationUtil.RunAsWork<NodeRef> processSite = () -> {
                            RetryingTransactionHelper.RetryingTransactionCallback<NodeRef> extractSiteWork = () -> {
                                extractDocuments(siteRef);
                                if (!nodeService.hasAspect(siteRef, ASPECT_CHECKED)) {
                                    nodeService.addAspect(siteRef, ASPECT_CHECKED, null);
                                }
                                return null;
                            };
                            return retryingTransactionHelper.doInTransaction(extractSiteWork, false, true);
                        };
                        AuthenticationUtil.runAs(processSite, AuthenticationUtil.getSystemUserName());
                        processedSiteIds.add(siteRef.getId());
                        processedAnySites = true;
                    }
                }

                // Increment skipCount regardless of whether we processed any sites
                skipCount += DEFAULT_BATCH_SIZE;

                // Log progress
                logToFileAndConsole("DEBUG", "Processed batch with skipCount=" + skipCount +
                        ", processed sites in this batch=" + (processedAnySites ? "yes" : "no") +
                        ", total documents extracted=" + docCount);

            } finally {
                if (results != null) {
                    results.close();
                }
            }
        }
    }

    /* Extracts all documents from a site and saves them to a folder. */
    private void extractDocuments(NodeRef siteRef) {
        String siteName = (String) nodeService.getProperty(siteRef, ContentModel.PROP_NAME);
        Path siteFolderPath = Paths.get(extractionPath, siteName);

        logToFileAndConsole("INFO", "Starting extraction for site: " + siteName);

        try {
            if (!Files.exists(siteFolderPath)) {
                Files.createDirectories(siteFolderPath);
                logToFileAndConsole("DEBUG", "Created directory: " + siteFolderPath);
            } else {
                processedSiteIds.add(siteRef.getId());
            }

            NodeRef documentLibrary = nodeService.getChildByName(siteRef, ContentModel.ASSOC_CONTAINS, "documentLibrary");
            if (documentLibrary != null) {
                logToFileAndConsole("TRACE", "Processing document library for site: " + siteName);
                processFolder(documentLibrary, siteFolderPath);
            } else {
                logToFileAndConsole("TRACE", "Document library not found for site: " + siteName);
            }
        } catch (IOException e) {
            logToFileAndConsole("ERROR", "Error while creating directories or processing folder for site: " + siteName + ": " + e.getMessage());
        }

        logToFileAndConsole("INFO", "End of extraction for site: " + siteName + " with " + docCount + " documents extracted");
    }

    /* Recursively processes a folder and its children. */
    private void processFolder(NodeRef folderRef, Path siteFolderPath) throws IOException {
        List<ChildAssociationRef> children = nodeService.getChildAssocs(folderRef);
        logToFileAndConsole("DEBUG", "Processing folder: " + folderRef + " with " + children.size() + " children");

        for (ChildAssociationRef child : children) {
            NodeRef childRef = child.getChildRef();
            if (nodeService.getType(childRef).equals(ContentModel.TYPE_CONTENT)) {
                logToFileAndConsole("DEBUG", "Extracting document: " + childRef);
                extractDocument(childRef, siteFolderPath);
            } else if (nodeService.getType(childRef).equals(ContentModel.TYPE_FOLDER)) {
                logToFileAndConsole("TRACE", "Processing subfolder: " + childRef);
                processFolder(childRef, siteFolderPath);
            }
        }
    }

    /* Extracts a single document and saves it to the specified path. */
    private void extractDocument(NodeRef documentRef, Path siteFolderPath) {
        String documentName = (String) nodeService.getProperty(documentRef, ContentModel.PROP_NAME);
        Path documentPath = siteFolderPath.resolve(documentName);
        InputStream inputStream = null;

        logToFileAndConsole("INFO", "Starting extraction for document: " + documentName);

        try {
            ContentReader reader = contentService.getReader(documentRef, ContentModel.PROP_CONTENT);
            inputStream = reader.getContentInputStream();

            // If a document with the same name already exists in the extraction path,
            // it renames the new document to avoid overwriting.
            int counter = 1;
            while (Files.exists(documentPath)) {
                String newDocumentName = documentName.replaceFirst("(\\.[^.]*)?$", "_" + counter + "$1");
                documentPath = siteFolderPath.resolve(newDocumentName);
                counter++;
            }

            Files.copy(inputStream, documentPath);
            docCount.incrementAndGet();
            logToFileAndConsole("INFO", "Extracted: " + documentPath);
        } catch (Exception e) {
            logToFileAndConsole("ERROR", "Failed to extract " + documentName + ": " + e.getMessage());
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    private void initLogFile() {
        try {
            NodeRef person = repository.getFullyAuthenticatedPerson();
            if (person != null)
            {
                NodeRef userHome = repository.getUserHome(person);
                if (userHome != null) {
                    String logFileName = LOG_FILE_PREFIX + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + LOG_FILE_SUFFIX;

                    Map<QName, Serializable> properties = new HashMap<>();
                    properties.put(ContentModel.PROP_NAME, logFileName);

                    ChildAssociationRef association = nodeService.createNode(
                            userHome,
                            ContentModel.ASSOC_CONTAINS,
                            QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, QName.createValidLocalName(logFileName)),
                            ContentModel.TYPE_CONTENT,
                            properties);

                    logFileRef = association.getChildRef();
                    ContentWriter writer = contentService.getWriter(logFileRef, ContentModel.PROP_CONTENT, true);
                    writer.setMimetype(MimetypeMap.MIMETYPE_TEXT_PLAIN);
                    writer.setEncoding("UTF-8");
                    writer.putContent("");

                    logToFileAndConsole("INFO", "Log file initialized: " + logFileName);
                }
            }
        } catch (Exception e) {
            logger.error("Error initializing log file", e);
        }
    }

    private void logToFileAndConsole(String level, String message) {
        String formattedMessage = new Date() + " - " + level + " - " + message + "\n";

        if (logFileRef != null) {
            try {
                String currentContent = "";
                ContentReader reader = contentService.getReader(logFileRef, ContentModel.PROP_CONTENT);
                if (reader != null && reader.exists()) {
                    currentContent = reader.getContentString();
                }

                ContentWriter writer = contentService.getWriter(logFileRef, ContentModel.PROP_CONTENT, true);
                writer.setMimetype(MimetypeMap.MIMETYPE_TEXT_PLAIN);
                writer.setEncoding("UTF-8");
                writer.putContent(currentContent + formattedMessage);
            } catch (Exception e) {
                logger.error("Failed to write to log file", e);
            }
        }

        logToConsole(level, message);
    }

    private void logToConsole(String level, String message) {
        switch (level) {
            case "TRACE": logger.trace(message); break;
            case "DEBUG": logger.debug(message); break;
            case "INFO": logger.info(message); break;
            case "WARN": logger.warn(message); break;
            case "ERROR": logger.error(message); break;
            default: logger.debug(message);
        }
    }

    private void closeLogFile() {
        try {
            if (contentWriter != null) {
                contentWriter = null;
            }
            if (logFileRef != null) {
                logFileRef = null;
            }
            logger.info("Log file closed successfully");
        } catch (Exception e) {
            logger.error("Error closing log file", e);
        }
    }

    // Spring setters
    public void setNodeService(NodeService nodeService) { this.nodeService = nodeService; }
    public void setSearchService(SearchService searchService) { this.searchService = searchService; }
    public void setContentService(ContentService contentService) { this.contentService = contentService; }
    public void setRepository(Repository repository) { this.repository = repository; }
    public void setRetryingTransactionHelper(RetryingTransactionHelper helper) { this.retryingTransactionHelper = helper; }
    public void setExtractionPath(String path) { this.extractionPath = path; }
    public void setMaxDocs(int maxDocs) { this.maxDocs = maxDocs; }
}