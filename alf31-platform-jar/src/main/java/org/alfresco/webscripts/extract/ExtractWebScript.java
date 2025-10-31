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

/**
 * WebScript for extracting documents from Alfresco repository based on keyword search and mimetype filtering.
 * This implementation replaces the old region-based extraction with a more flexible search-based approach.
 */
public class ExtractWebScript extends DeclarativeWebScript {
    private static final Log logger = LogFactory.getLog(ExtractWebScript.class);
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

    // Extraction parameters
    private String extractionBasePath;  // Configured via Spring from alfresco-global.properties
    private String extractionPath;      // Actual path including dated subfolder
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
     * Core implementation of the extraction logic.
     */
    private Map<String, Object> doExecuteImpl(WebScriptRequest req) {
        // Initialize parameters from request/session
        initializeParameters(req);

        // Initialize log file
        initLogFile();

        logToFileAndConsole("INFO", "========================================");
        logToFileAndConsole("INFO", "Starting export process");
        logToFileAndConsole("INFO", String.format("Parameters: maxDocs=%d, basePath=%s", maxDocs, extractionBasePath));
        logToFileAndConsole("INFO", String.format("Keywords: '%s'", keywords != null && !keywords.isEmpty() ? keywords : "(none)"));
        logToFileAndConsole("INFO", String.format("Mimetype: %s", mimetype != null && !mimetype.isEmpty() ? mimetype : "(all)"));
        logToFileAndConsole("INFO", "========================================");

        Map<String, Object> model = new HashMap<>();

        try {
            // Validate extraction path
            validateExtractionPath();

            // Perform search and extraction
            int extractedCount = performSearchAndExtract();

            // Build result
            String exitMessage = String.format("Export completed successfully. Extracted %d documents.", extractedCount);
            logToFileAndConsole("INFO", exitMessage);
            logToFileAndConsole("INFO", "========================================");

            model.put("success", true);
            model.put("extractedCount", extractedCount);
            model.put("message", exitMessage);

        } catch (Exception e) {
            String errorMsg = "Error during extraction: " + e.getMessage();
            logToFileAndConsole("ERROR", errorMsg);
            logger.error("Extraction failed", e);

            model.put("success", false);
            model.put("extractedCount", docCount.get());
            model.put("message", errorMsg);
        } finally {
            closeLogFile();
        }

        return model;
    }

    /**
     * Initialize extraction parameters from request.
     */
    private void initializeParameters(WebScriptRequest req) {
        // Get parameters from request
        String maxDocsParam = req.getParameter("maxDocs");
        this.maxDocs = (maxDocsParam != null && !maxDocsParam.isEmpty()) ?
            Integer.parseInt(maxDocsParam) : 40000;

        String keywordsParam = req.getParameter("keywords");
        this.keywords = (keywordsParam != null) ? keywordsParam.trim() : "";

        // Handle single mimetype selection
        String mimetypeParam = req.getParameter("mimetype");
        this.mimetype = (mimetypeParam != null && !mimetypeParam.isEmpty()) ?
            mimetypeParam.trim() : "";

        // extractionBasePath is injected via Spring from alfresco-global.properties
        // No need to get it from request parameters
    }


    /**
     * Validate extraction base path and create dated subfolder.
     * The base path must exist (not created by code for security reasons).
     */
    private void validateExtractionPath() throws IOException {
        // Check that extractionBasePath is configured
        if (extractionBasePath == null || extractionBasePath.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "Extraction base path is not configured. " +
                "Please set 'extraction.base.path' in alfresco-global.properties"
            );
        }

        // Check that base directory exists (do NOT create it)
        Path basePath = Paths.get(extractionBasePath);
        if (!Files.exists(basePath)) {
            String errorMsg = String.format(
                "WARNING: Base extraction directory does not exist: %s. " +
                "Please create this directory and ensure the Alfresco process has write permissions. " +
                "Configure the path in alfresco-global.properties using 'extraction.base.path' property.",
                extractionBasePath
            );
            logToFileAndConsole("ERROR", errorMsg);
            throw new IOException(errorMsg);
        }

        // Check that base directory is writable
        if (!Files.isWritable(basePath)) {
            String errorMsg = String.format(
                "WARNING: Base extraction directory is not writable: %s. " +
                "Please grant write permissions to the Alfresco process.",
                extractionBasePath
            );
            logToFileAndConsole("ERROR", errorMsg);
            throw new IOException(errorMsg);
        }

        logToFileAndConsole("INFO", "Base extraction directory verified: " + extractionBasePath);

        // Create dated subfolder: Export_YYYYMMDD_HHmmss
        String dateFolder = "Export_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        Path datedPath = basePath.resolve(dateFolder);
        Files.createDirectories(datedPath);

        // Update extractionPath to point to the dated subfolder
        this.extractionPath = datedPath.toString();
        logToFileAndConsole("INFO", "Created dated export directory: " + this.extractionPath);
    }

    /**
     * Build FTS-Alfresco search query based on keywords and mimetype.
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
            query.append(" OR TEXT:\"").append(cleanKeywords).append("\"");

            query.append(")");
        }

        // Add mimetype filter if present
        if (mimetype != null && !mimetype.trim().isEmpty()) {
            query.append(" AND @cm\\:content.mimetype:\"").append(mimetype).append("\"");
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
        logToFileAndConsole("INFO", "Search query: " + query);

        // Create extraction directory
        File extractionDir = new File(extractionPath);

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
                        if (extractDocument(nodeRef, extractionDir)) {
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
     * Extract a single document to the extraction directory.
     */
    private boolean extractDocument(NodeRef nodeRef, File extractionDir) {
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

        // Write to file system
        File targetFile = new File(extractionDir, uniqueFileName);
        InputStream inputStream = null;

        try {
            inputStream = reader.getContentInputStream();
            Files.copy(inputStream, targetFile.toPath());

            long fileSize = targetFile.length();
            logToFileAndConsole("INFO", String.format("Extracted [%d]: %s (%s)",
                docCount.get() + 1, uniqueFileName, formatFileSize(fileSize)));

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
            String newFileName = fileName.replaceFirst("(\\.[^.]*)?$", "_" + counter + "$1");
            return newFileName;
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

    public void setExtractionBasePath(String path) {
        this.extractionBasePath = path;
    }
}
