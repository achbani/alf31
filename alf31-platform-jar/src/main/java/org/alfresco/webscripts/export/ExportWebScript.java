package org.alfresco.webscripts.export;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.service.cmr.repository.*;
import org.alfresco.service.cmr.search.QueryConsistency;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * WebScript for exporting documents from Alfresco repository based on keyword search and mimetype filtering.
 */
public class ExportWebScript extends DeclarativeWebScript {
    private static final Log logger = LogFactory.getLog(ExportWebScript.class);
    private static final int DEFAULT_BATCH_SIZE = 50;
    private static final String LOG_FILE_PREFIX = "Export_";
    private static final String LOG_FILE_SUFFIX = ".log";
    private static final String FTS_ALFRESCO = SearchService.LANGUAGE_FTS_ALFRESCO;

    private final AtomicInteger docCount = new AtomicInteger(0);

    // Alfresco services
    private NodeService nodeService;
    private SearchService searchService;
    private ContentService contentService;
    private Repository repository;
    private RetryingTransactionHelper retryingTransactionHelper;

    // Export parameters
    private String exportBasePath;
    private String exportPath;
    private int maxDocs;
    private String keywords;
    private String mimetype;

    // Logging
    private NodeRef logFileRef;

    // Map for handling duplicate file names
    private Map<String, Integer> fileNameCounters = new HashMap<>();

    /**
     * Main entry point for the web script execution.
     */
    @Override
    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
        try {
            return doExecuteImpl(req);
        } catch (Exception e) {
            logToFileAndConsole("ERROR", "Error during export process: " + e.getMessage());
            logger.error("Exception during export", e);
            Map<String, Object> model = new HashMap<>();
            model.put("success", false);
            model.put("message", "Error during export process: " + e.getMessage());
            return model;
        }
    }

    /**
     * Core implementation of the export logic (synchronous).
     */
    private Map<String, Object> doExecuteImpl(WebScriptRequest req) {
        // Initialize parameters from request
        initializeParameters(req);

        Map<String, Object> model = new HashMap<>();
        long startTime = System.currentTimeMillis();
        int extractedCount = 0;
        String message = "";
        boolean success = false;

        try {
            // Wrap entire export in a transaction
            final int[] extractedCountHolder = new int[1];

            retryingTransactionHelper.doInTransaction(new RetryingTransactionHelper.RetryingTransactionCallback<Void>() {
                @Override
                public Void execute() throws Throwable {
                    // Initialize log file
                    initLogFile();

                    logToFileAndConsole("INFO", "========================================");
                    logToFileAndConsole("INFO", "Starting export process");
                    logToFileAndConsole("INFO", String.format("Parameters: maxDocs=%d, basePath=%s", maxDocs, exportBasePath));
                    logToFileAndConsole("INFO", String.format("Keywords: '%s'", keywords != null && !keywords.isEmpty() ? keywords : "(none)"));
                    logToFileAndConsole("INFO", String.format("Mimetype: %s", mimetype != null && !mimetype.isEmpty() ? mimetype : "(all)"));
                    logToFileAndConsole("INFO", "========================================");

                    // Validate export path
                    validateExportPath();

                    // Perform search and export
                    extractedCountHolder[0] = performSearchAndExtract();

                    // Build result
                    String exitMessage = String.format("Export terminé avec succès. %d documents extraits.", extractedCountHolder[0]);
                    logToFileAndConsole("INFO", exitMessage);
                    logToFileAndConsole("INFO", "========================================");

                    return null;
                }
            }, false, true);

            extractedCount = extractedCountHolder[0];
            message = String.format("Export terminé avec succès. %d documents extraits.", extractedCount);
            success = true;

        } catch (Exception e) {
            message = "Erreur lors de l'export: " + e.getMessage();
            logger.error("Export failed", e);

            // Try to log error to file if possible
            try {
                if (logFileRef != null) {
                    logToFileAndConsole("ERROR", message);
                }
            } catch (Exception logError) {
                logger.error("Failed to log error to file", logError);
            }

        } finally {
            try {
                closeLogFile();
            } catch (Exception e) {
                logger.error("Error closing log file", e);
            }
        }

        // Calculate duration
        long duration = (System.currentTimeMillis() - startTime) / 1000; // seconds

        // Build model for template
        model.put("success", success);
        model.put("message", message);
        model.put("extractedCount", extractedCount);
        model.put("maxDocs", maxDocs);
        model.put("keywords", keywords != null ? keywords : "");
        model.put("mimetype", mimetype != null ? mimetype : "");
        model.put("exportPath", exportPath != null ? exportPath : "");
        model.put("duration", duration);

        return model;
    }

    /**
     * Initialize export parameters from request.
     */
    private void initializeParameters(WebScriptRequest req) {
        // Get parameters from request
        String maxDocsParam = req.getParameter("maxDocs");
        this.maxDocs = (maxDocsParam != null && !maxDocsParam.isEmpty()) ?
            Integer.parseInt(maxDocsParam) : 250;

        String keywordsParam = req.getParameter("keywords");
        this.keywords = (keywordsParam != null) ? keywordsParam.trim() : "";

        // Handle single mimetype selection
        String mimetypeParam = req.getParameter("mimetype");
        this.mimetype = (mimetypeParam != null && !mimetypeParam.isEmpty()) ?
            mimetypeParam.trim() : "";
    }


    /**
     * Validate export base path and create dated subfolder.
     * The base path must exist (not created by code for security reasons).
     */
    private void validateExportPath() throws IOException {
        // Check that exportBasePath is configured
        if (exportBasePath == null || exportBasePath.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "Export base path is not configured. " +
                "Please set 'export.base.path' in alfresco-global.properties"
            );
        }

        // Check that base directory exists (do NOT create it)
        Path basePath = Paths.get(exportBasePath);
        if (!Files.exists(basePath)) {
            String errorMsg = String.format(
                "WARNING: Base export directory does not exist: %s. " +
                "Please create this directory and ensure the Alfresco process has write permissions. " +
                "Configure the path in alfresco-global.properties using 'export.base.path' property.",
                exportBasePath
            );
            logToFileAndConsole("ERROR", errorMsg);
            throw new IOException(errorMsg);
        }

        // Check that base directory is writable
        if (!Files.isWritable(basePath)) {
            String errorMsg = String.format(
                "WARNING: Base export directory is not writable: %s. " +
                "Please grant write permissions to the Alfresco process.",
                exportBasePath
            );
            logToFileAndConsole("ERROR", errorMsg);
            throw new IOException(errorMsg);
        }

        logToFileAndConsole("INFO", "Base export directory verified: " + exportBasePath);

        // Create dated subfolder: Export_YYYYMMDD_HHmmss
        String dateFolder = "Export_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        Path datedPath = basePath.resolve(dateFolder);
        Files.createDirectories(datedPath);

        // Update exportPath to point to the dated subfolder
        this.exportPath = datedPath.toString();
        logToFileAndConsole("INFO", "Created dated export directory: " + this.exportPath);
    }

    /**
     * Build FTS-Alfresco search query based on keywords and mimetype.
     * Uses hybrid approach: FTS query for performance + Java filter for reliability.
     */
    private String buildSearchQuery() {
        StringBuilder query = new StringBuilder();

        // Base: search only for content nodes (not folders)
        query.append("TYPE:\"cm:content\"");

        // Add keyword search if present
        if (keywords != null && !keywords.trim().isEmpty()) {
            String cleanKeywords = keywords.replace("\"", "\\\"");

            query.append(" AND (");

            // Search in multiple fields
            query.append("cm:name:\"").append(cleanKeywords).append("*\"");
            query.append(" OR cm:title:\"").append(cleanKeywords).append("*\"");
            query.append(" OR cm:description:\"").append(cleanKeywords).append("*\"");

            query.append(")");
        }

        // Add mimetype filter if present
        if (mimetype != null && !mimetype.trim().isEmpty()) {
            // Use full namespace URI for reliable mimetype filtering
            // Format: @{namespace}property:value
            // Note: No escaping needed - FTS-Alfresco accepts {} and : in namespace URIs
            query.append(" AND @{http://www.alfresco.org/model/content/1.0}content.mimetype:\"")
                 .append(mimetype)
                 .append("\"");
        }

        return query.toString();
    }

    /**
     * Perform search and extract documents in batches.
     */
    private int performSearchAndExtract() {
        int extractedCount = 0;
        int skipCount = 0;

        // Build search query
        String query = buildSearchQuery();
        logToFileAndConsole("INFO", "========================================");
        logToFileAndConsole("INFO", "Search query: " + query);
        logToFileAndConsole("INFO", "Mimetype filter: " + (mimetype != null && !mimetype.isEmpty() ? mimetype : "(none)"));
        logToFileAndConsole("INFO", "========================================");

        // Create export directory
        File exportDir = new File(exportPath);

        // Search and extract in batches
        while (extractedCount < maxDocs) {
            // Setup search parameters
            SearchParameters searchParams = new SearchParameters();
            searchParams.setLanguage(FTS_ALFRESCO);
            searchParams.setQuery(query);
            searchParams.setSkipCount(skipCount);
            searchParams.setMaxItems(DEFAULT_BATCH_SIZE);
            searchParams.addStore(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);
            searchParams.setQueryConsistency(QueryConsistency.TRANSACTIONAL_IF_POSSIBLE);

            ResultSet results = null;
            try {
                // Execute search
                results = searchService.query(searchParams);

                logToFileAndConsole("DEBUG", String.format("Batch %d: found %d results (skip=%d)",
                    (skipCount / DEFAULT_BATCH_SIZE + 1), results.length(), skipCount));

                // No more results
                if (results.length() == 0) {
                    logToFileAndConsole("INFO", "No more documents found");
                    break;
                }

                // Extract each document
                for (NodeRef nodeRef : results.getNodeRefs()) {
                    if (extractedCount >= maxDocs) {
                        logToFileAndConsole("INFO", "Reached maximum document limit: " + maxDocs);
                        break;
                    }

                    try {
                        if (extractDocument(nodeRef, exportDir)) {
                            extractedCount++;
                            docCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                        logToFileAndConsole("ERROR", "Failed to extract document " + nodeRef + ": " + e.getMessage());
                    }
                }

                skipCount += DEFAULT_BATCH_SIZE;

            } catch (Exception e) {
                logToFileAndConsole("ERROR", "Search failed at skip=" + skipCount + ": " + e.getMessage());
                break;
            } finally {
                if (results != null) {
                    results.close();
                }
            }
        }

        return extractedCount;
    }

    /**
     * Extract a single document to the export directory.
     */
    private boolean extractDocument(NodeRef nodeRef, File exportDir) {
        // Verify node exists
        if (!nodeService.exists(nodeRef)) {
            return false;
        }

        // Get document name
        String fileName = (String) nodeService.getProperty(nodeRef, ContentModel.PROP_NAME);
        if (fileName == null) {
            fileName = nodeRef.getId() + ".bin";
        }

        // Handle duplicate file names
        String uniqueFileName = getUniqueFileName(fileName);

        // Get content reader
        ContentReader reader = contentService.getReader(nodeRef, ContentModel.PROP_CONTENT);
        if (reader == null || !reader.exists()) {
            logToFileAndConsole("WARN", "No content for: " + fileName);
            return false;
        }

        // Get actual mimetype for debugging
        String actualMimetype = reader.getMimetype();

        // Write to file system
        File targetFile = new File(exportDir, uniqueFileName);
        InputStream inputStream = null;

        try {
            inputStream = reader.getContentInputStream();
            Files.copy(inputStream, targetFile.toPath());

            long fileSize = targetFile.length();
            logToFileAndConsole("INFO", String.format("Extracted [%d]: %s (%s) - mimetype: %s",
                docCount.get() + 1, uniqueFileName, formatFileSize(fileSize),
                actualMimetype != null ? actualMimetype : "unknown"));

            return true;

        } catch (Exception e) {
            logToFileAndConsole("ERROR", "Failed to write file " + uniqueFileName + ": " + e.getMessage());
            return false;
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    /**
     * Generate a unique file name to avoid overwriting existing files.
     */
    private String getUniqueFileName(String fileName) {
        Integer counter = fileNameCounters.get(fileName);

        if (counter == null) {
            fileNameCounters.put(fileName, 1);
            return fileName;
        } else {
            fileNameCounters.put(fileName, counter + 1);
            // Insert counter before file extension
            return fileName.replaceFirst("(\\.[^.]*)?$", "_" + counter + "$1");
        }
    }

    /**
     * Format file size in human-readable format.
     */
    private String formatFileSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.2f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.2f MB", size / (1024.0 * 1024));
        return String.format("%.2f GB", size / (1024.0 * 1024 * 1024));
    }

    /**
     * Initialize log file in user's home directory.
     */
    private void initLogFile() {
        try {
            NodeRef person = repository.getFullyAuthenticatedPerson();
            if (person != null) {
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

    /**
     * Write log message to both file and console.
     */
    private void logToFileAndConsole(String level, String message) {
        String formattedMessage = new Date() + " - " + level + " - " + message + "\n";

        // Write to log file in repository
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

        // Write to console
        logToConsole(level, message);
    }

    /**
     * Write log message to console only.
     */
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

    /**
     * Close log file resources.
     */
    private void closeLogFile() {
        try {
            if (logFileRef != null) {
                logFileRef = null;
            }
            logger.info("Log file closed successfully");
        } catch (Exception e) {
            logger.error("Error closing log file", e);
        }
    }

    // Spring setters for dependency injection
    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setSearchService(SearchService searchService) {
        this.searchService = searchService;
    }

    public void setContentService(ContentService contentService) {
        this.contentService = contentService;
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public void setRetryingTransactionHelper(RetryingTransactionHelper helper) {
        this.retryingTransactionHelper = helper;
    }

    public void setExportBasePath(String exportBasePath) {
        this.exportBasePath = exportBasePath;
    }
}
