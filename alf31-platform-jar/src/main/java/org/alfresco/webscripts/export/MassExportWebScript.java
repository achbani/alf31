package org.alfresco.webscripts.export;

import org.alfresco.model.ContentModel;
import org.alfresco.model.GazodocExcelRow;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.service.cmr.repository.*;
import org.alfresco.service.cmr.search.QueryConsistency;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.export.ExcelParser;
import org.alfresco.service.export.GazodocMetadataExporter;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * WebScript pour l'export massif de documents depuis une liste Excel.
 * Lit un fichier Excel contenant les références des documents à exporter,
 * recherche chaque document dans Alfresco par cm:name, et exporte :
 * - Les fichiers vers un répertoire NAS
 * - Les métadonnées GAZODOC en CSV
 * - Un manifest JSON avec le résumé de l'export
 *
 * @author Alfresco SDK 3
 */
public class MassExportWebScript extends DeclarativeWebScript {

    private static final Log logger = LogFactory.getLog(MassExportWebScript.class);
    private static final String LOG_FILE_PREFIX = "MassExport_";
    private static final String LOG_FILE_SUFFIX = ".log";

    // Alfresco services
    private NodeService nodeService;
    private SearchService searchService;
    private ContentService contentService;
    private Repository repository;
    private RetryingTransactionHelper retryingTransactionHelper;
    private ExcelParser excelParser;
    private GazodocMetadataExporter metadataExporter;

    // Export parameters
    private String exportBasePath;
    private String exportPath;
    private String excelFileNodeRef;
    private String sheetName;

    // Logging
    private NodeRef logFileRef;

    // Map for handling duplicate file names
    private Map<String, Integer> fileNameCounters = new HashMap<>();

    // Statistics
    private int totalRows = 0;
    private int foundCount = 0;
    private int notFoundCount = 0;
    private int exportedCount = 0;
    private int errorCount = 0;

    @Override
    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
        try {
            return doExecuteImpl(req);
        } catch (Exception e) {
            logToFileAndConsole("ERROR", "Error during mass export: " + e.getMessage());
            logger.error("Exception during mass export", e);
            Map<String, Object> model = new HashMap<>();
            model.put("success", false);
            model.put("message", "Error during mass export: " + e.getMessage());
            return model;
        }
    }

    /**
     * Core implementation of the mass export logic
     */
    private Map<String, Object> doExecuteImpl(WebScriptRequest req) {
        // Initialize parameters
        initializeParameters(req);

        Map<String, Object> model = new HashMap<>();
        long startTime = System.currentTimeMillis();
        boolean success = false;
        String message = "";

        try {
            // Wrap entire export in a transaction
            retryingTransactionHelper.doInTransaction(new RetryingTransactionHelper.RetryingTransactionCallback<Void>() {
                @Override
                public Void execute() throws Throwable {
                    // Initialize log file
                    initLogFile();

                    logToFileAndConsole("INFO", "========================================");
                    logToFileAndConsole("INFO", "Starting MASS EXPORT process");
                    logToFileAndConsole("INFO", String.format("Excel file: %s", excelFileNodeRef));
                    logToFileAndConsole("INFO", String.format("Sheet name: %s", sheetName));
                    logToFileAndConsole("INFO", String.format("Export base path: %s", exportBasePath));
                    logToFileAndConsole("INFO", "========================================");

                    // Validate export path
                    validateExportPath();

                    // Parse Excel file
                    List<GazodocExcelRow> rows = parseExcelFile();
                    totalRows = rows.size();
                    logToFileAndConsole("INFO", "Parsed " + totalRows + " rows from Excel file");

                    // Search and export each document
                    exportDocuments(rows);

                    // Export metadata to CSV
                    exportMetadata(rows);

                    // Generate manifest
                    generateManifest(rows);

                    // Summary
                    String summary = String.format(
                        "Export complete: %d/%d documents exported (%d not found, %d errors)",
                        exportedCount, totalRows, notFoundCount, errorCount
                    );
                    logToFileAndConsole("INFO", summary);
                    logToFileAndConsole("INFO", "========================================");

                    return null;
                }
            }, false, true);

            message = String.format(
                "Export terminé avec succès. %d documents exportés sur %d (Non trouvés: %d, Erreurs: %d)",
                exportedCount, totalRows, notFoundCount, errorCount
            );
            success = true;

        } catch (Exception e) {
            message = "Erreur lors de l'export: " + e.getMessage();
            logger.error("Mass export failed", e);

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
        model.put("totalRows", totalRows);
        model.put("exportedCount", exportedCount);
        model.put("notFoundCount", notFoundCount);
        model.put("errorCount", errorCount);
        model.put("exportPath", exportPath != null ? exportPath : "");
        model.put("duration", duration);

        return model;
    }

    /**
     * Initialize parameters from request
     */
    private void initializeParameters(WebScriptRequest req) {
        this.excelFileNodeRef = req.getParameter("excelFileNodeRef");
        this.sheetName = req.getParameter("sheetName");

        if (excelFileNodeRef == null || excelFileNodeRef.isEmpty()) {
            throw new IllegalArgumentException("Parameter 'excelFileNodeRef' is required");
        }

        if (sheetName == null || sheetName.isEmpty()) {
            this.sheetName = "Finances"; // Default sheet name
        }
    }

    /**
     * Validate export path and create dated subfolder
     */
    private void validateExportPath() throws IOException {
        if (exportBasePath == null || exportBasePath.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "Export base path is not configured. " +
                "Please set 'export.base.path' in alfresco-global.properties"
            );
        }

        Path basePath = Paths.get(exportBasePath);
        if (!Files.exists(basePath)) {
            throw new IOException("Base export directory does not exist: " + exportBasePath);
        }

        if (!Files.isWritable(basePath)) {
            throw new IOException("Base export directory is not writable: " + exportBasePath);
        }

        logToFileAndConsole("INFO", "Base export directory verified: " + exportBasePath);

        // Create dated subfolder: MassExport_YYYYMMDD_HHmmss
        String dateFolder = "MassExport_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        Path datedPath = basePath.resolve(dateFolder);
        Files.createDirectories(datedPath);

        // Create subdirectories
        Files.createDirectories(datedPath.resolve("documents"));
        Files.createDirectories(datedPath.resolve("metadata"));

        this.exportPath = datedPath.toString();
        logToFileAndConsole("INFO", "Created export directory: " + this.exportPath);
    }

    /**
     * Parse Excel file
     */
    private List<GazodocExcelRow> parseExcelFile() throws Exception {
        logToFileAndConsole("INFO", "Parsing Excel file: " + excelFileNodeRef);

        NodeRef excelNode = new NodeRef(excelFileNodeRef);
        if (!nodeService.exists(excelNode)) {
            throw new IllegalArgumentException("Excel file not found: " + excelFileNodeRef);
        }

        return excelParser.parseExcelFileFromNodeRef(excelNode, sheetName);
    }

    /**
     * Search and export each document
     */
    private void exportDocuments(List<GazodocExcelRow> rows) {
        logToFileAndConsole("INFO", "Starting document export...");

        File documentsDir = new File(exportPath, "documents");

        for (GazodocExcelRow row : rows) {
            try {
                // Search document by cm:name
                NodeRef nodeRef = searchDocumentByName(row.getName());

                if (nodeRef != null) {
                    // Export document
                    boolean exported = exportDocument(nodeRef, documentsDir);

                    if (exported) {
                        row.setNodeRef(nodeRef.toString());
                        row.setStatus("EXPORTED");
                        foundCount++;
                        exportedCount++;
                        logToFileAndConsole("INFO", String.format("[%d/%d] EXPORTED: %s",
                            foundCount, totalRows, row.getName()));
                    } else {
                        row.setStatus("EXPORT_FAILED");
                        row.setStatusReason("Failed to export file content");
                        errorCount++;
                        logToFileAndConsole("WARN", String.format("[%d/%d] FAILED: %s",
                            foundCount, totalRows, row.getName()));
                    }
                } else {
                    row.setStatus("NOT_FOUND");
                    row.setStatusReason("Document not found in Alfresco");
                    notFoundCount++;
                    logToFileAndConsole("WARN", String.format("[%d/%d] NOT FOUND: %s",
                        notFoundCount + foundCount, totalRows, row.getName()));
                }

            } catch (Exception e) {
                row.setStatus("ERROR");
                row.setStatusReason(e.getMessage());
                errorCount++;
                logToFileAndConsole("ERROR", "Error processing row " + row.getRowNumber() + ": " + e.getMessage());
            }
        }
    }

    /**
     * Search document in Alfresco by cm:name
     */
    private NodeRef searchDocumentByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }

        // Build FTS query
        String query = String.format("@cm\\:name:\"%s\"", name.replace("\"", "\\\""));

        SearchParameters searchParams = new SearchParameters();
        searchParams.setLanguage(SearchService.LANGUAGE_FTS_ALFRESCO);
        searchParams.setQuery(query);
        searchParams.setMaxItems(1);
        searchParams.addStore(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);
        searchParams.setQueryConsistency(QueryConsistency.TRANSACTIONAL_IF_POSSIBLE);

        ResultSet results = null;
        try {
            results = searchService.query(searchParams);
            if (results.length() > 0) {
                return results.getNodeRef(0);
            }
        } catch (Exception e) {
            logger.error("Search error for: " + name, e);
        } finally {
            if (results != null) {
                results.close();
            }
        }

        return null;
    }

    /**
     * Export a single document to the export directory
     */
    private boolean exportDocument(NodeRef nodeRef, File exportDir) {
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
            logger.warn("No content for: " + fileName);
            return false;
        }

        // Write to file system
        File targetFile = new File(exportDir, uniqueFileName);
        InputStream inputStream = null;

        try {
            inputStream = reader.getContentInputStream();
            Files.copy(inputStream, targetFile.toPath());
            return true;

        } catch (Exception e) {
            logger.error("Failed to write file " + uniqueFileName, e);
            return false;
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    /**
     * Generate unique file name to avoid overwriting
     */
    private String getUniqueFileName(String fileName) {
        Integer counter = fileNameCounters.get(fileName);

        if (counter == null) {
            fileNameCounters.put(fileName, 1);
            return fileName;
        } else {
            fileNameCounters.put(fileName, counter + 1);
            return fileName.replaceFirst("(\\.[^.]*)?$", "_" + counter + "$1");
        }
    }

    /**
     * Export metadata to CSV
     */
    private void exportMetadata(List<GazodocExcelRow> rows) throws IOException {
        logToFileAndConsole("INFO", "Exporting metadata to CSV...");

        File metadataDir = new File(exportPath, "metadata");
        File metadataFile = new File(metadataDir, "export_metadata.csv");

        metadataExporter.exportMetadata(rows, metadataFile);

        logToFileAndConsole("INFO", "Metadata exported to: " + metadataFile.getAbsolutePath());
    }

    /**
     * Generate manifest JSON
     */
    private void generateManifest(List<GazodocExcelRow> rows) throws IOException {
        logToFileAndConsole("INFO", "Generating manifest...");

        JSONObject manifest = new JSONObject();
        manifest.put("exportDate", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(new Date()));
        manifest.put("excelFile", excelFileNodeRef);
        manifest.put("sheetName", sheetName);
        manifest.put("totalRows", totalRows);
        manifest.put("exportedCount", exportedCount);
        manifest.put("notFoundCount", notFoundCount);
        manifest.put("errorCount", errorCount);

        // Add statistics by status
        JSONObject stats = new JSONObject();
        Map<String, Integer> statusCounts = new HashMap<>();
        for (GazodocExcelRow row : rows) {
            String status = row.getStatus();
            statusCounts.put(status, statusCounts.getOrDefault(status, 0) + 1);
        }
        stats.put("byStatus", statusCounts);
        manifest.put("statistics", stats);

        // Add list of not found documents
        JSONArray notFound = new JSONArray();
        for (GazodocExcelRow row : rows) {
            if ("NOT_FOUND".equals(row.getStatus())) {
                JSONObject item = new JSONObject();
                item.put("rowNumber", row.getRowNumber());
                item.put("name", row.getName());
                item.put("referenceMetier", row.getReferenceMetier());
                notFound.put(item);
            }
        }
        manifest.put("notFoundDocuments", notFound);

        // Write to file
        File manifestFile = new File(exportPath, "manifest.json");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(manifestFile))) {
            writer.write(manifest.toString(2)); // Pretty print with indent
        }

        logToFileAndConsole("INFO", "Manifest generated: " + manifestFile.getAbsolutePath());
    }

    // ===== Logging methods =====

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

        // Write to console
        switch (level) {
            case "INFO": logger.info(message); break;
            case "WARN": logger.warn(message); break;
            case "ERROR": logger.error(message); break;
            default: logger.debug(message);
        }
    }

    private void closeLogFile() {
        if (logFileRef != null) {
            logFileRef = null;
        }
    }

    // ===== Setters for dependency injection =====

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

    public void setExcelParser(ExcelParser excelParser) {
        this.excelParser = excelParser;
    }

    public void setMetadataExporter(GazodocMetadataExporter metadataExporter) {
        this.metadataExporter = metadataExporter;
    }
}
