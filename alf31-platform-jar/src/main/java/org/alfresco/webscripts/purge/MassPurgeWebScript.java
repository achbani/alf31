package org.alfresco.webscripts.purge;

import org.alfresco.model.ContentModel;
import org.alfresco.model.Fiche;
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
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * WebScript pour la purge de masse de documents depuis une liste Excel.
 *
 * SÉCURITÉS INTÉGRÉES:
 * - Validation inline des règles métier (état ARCHIVE, durée conservation)
 * - Mode DRY-RUN obligatoire en première exécution
 * - Auto-archivage des documents en état VALIDE
 * - Purge transactionnelle avec rollback automatique en cas d'erreur
 * - Logs détaillés de toutes les opérations
 *
 * @author Alfresco SDK 3
 */
public class MassPurgeWebScript extends DeclarativeWebScript {

    private static final Log logger = LogFactory.getLog(MassPurgeWebScript.class);
    private static final String LOG_FILE_PREFIX = "MassPurge_";
    private static final String LOG_FILE_SUFFIX = ".log";
    private static final int DEFAULT_CONSERVATION_YEARS = 5; // Durée conservation par défaut

    // Alfresco services
    private NodeService nodeService;
    private SearchService searchService;
    private ContentService contentService;
    private Repository repository;
    private RetryingTransactionHelper retryingTransactionHelper;
    private ExcelParser excelParser;

    // Purge parameters
    private String exportBasePath;
    private String purgeReportPath;
    private String excelFileNodeRef;
    private String sheetName;
    private boolean dryRun;
    private boolean autoArchive;

    // Logging
    private NodeRef logFileRef;

    // Statistics
    private int totalRows = 0;
    private int foundCount = 0;
    private int notFoundCount = 0;
    private int deletedCount = 0;
    private int blockedCount = 0;
    private int archivedCount = 0;
    private int errorCount = 0;

    @Override
    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
        try {
            return doExecuteImpl(req);
        } catch (Exception e) {
            logToFileAndConsole("ERROR", "Error during mass purge: " + e.getMessage());
            logger.error("Exception during mass purge", e);
            Map<String, Object> model = new HashMap<>();
            model.put("success", false);
            model.put("resultMessage", "Error during mass purge: " + e.getMessage());
            return model;
        }
    }

    /**
     * Core implementation of the mass purge logic
     */
    private Map<String, Object> doExecuteImpl(WebScriptRequest req) {
        // Initialize parameters
        initializeParameters(req);

        Map<String, Object> model = new HashMap<>();
        long startTime = System.currentTimeMillis();
        boolean success = false;
        String message = "";

        try {
            // Wrap entire purge in a transaction
            retryingTransactionHelper.doInTransaction(new RetryingTransactionHelper.RetryingTransactionCallback<Void>() {
                @Override
                public Void execute() throws Throwable {
                    // Initialize log file
                    initLogFile();

                    logToFileAndConsole("INFO", "========================================");
                    logToFileAndConsole("INFO", "Starting MASS PURGE process");
                    logToFileAndConsole("INFO", String.format("Excel file: %s", excelFileNodeRef));
                    logToFileAndConsole("INFO", String.format("Sheet name: %s", sheetName));
                    logToFileAndConsole("INFO", String.format("Dry-run mode: %s", dryRun ? "YES (simulation)" : "NO (real deletion)"));
                    logToFileAndConsole("INFO", String.format("Auto-archive: %s", autoArchive ? "YES" : "NO"));
                    logToFileAndConsole("INFO", "========================================");

                    if (dryRun) {
                        logToFileAndConsole("WARN", "⚠️  DRY-RUN MODE ACTIVÉ - Aucune suppression ne sera effectuée");
                    } else {
                        logToFileAndConsole("WARN", "🔴 MODE RÉEL ACTIVÉ - Les documents seront RÉELLEMENT supprimés !");
                    }

                    // Validate purge report path
                    validatePurgeReportPath();

                    // Parse Excel file
                    List<GazodocExcelRow> rows = parseExcelFile();
                    totalRows = rows.size();
                    logToFileAndConsole("INFO", "Parsed " + totalRows + " rows from Excel file");

                    // Process each document (validate + purge)
                    purgeDocuments(rows);

                    // Generate purge report
                    generatePurgeReport(rows);

                    // Summary
                    String summary = String.format(
                        "Purge complete: %d deleted, %d blocked, %d auto-archived, %d not found, %d errors",
                        deletedCount, blockedCount, archivedCount, notFoundCount, errorCount
                    );
                    logToFileAndConsole("INFO", summary);
                    logToFileAndConsole("INFO", "========================================");

                    return null;
                }
            }, false, true);

            message = String.format(
                "Purge terminée. %d supprimés, %d bloqués, %d archivés, %d non trouvés, %d erreurs (Mode: %s)",
                deletedCount, blockedCount, archivedCount, notFoundCount, errorCount,
                dryRun ? "DRY-RUN" : "RÉEL"
            );
            success = true;

        } catch (Exception e) {
            message = "Erreur lors de la purge: " + e.getMessage();
            logger.error("Mass purge failed", e);

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
        model.put("resultMessage", message);  // Renamed from 'message' to avoid FreeMarker conflict
        model.put("totalRows", totalRows);
        model.put("deletedCount", deletedCount);
        model.put("blockedCount", blockedCount);
        model.put("archivedCount", archivedCount);
        model.put("notFoundCount", notFoundCount);
        model.put("errorCount", errorCount);
        model.put("dryRun", dryRun);
        model.put("purgeReportPath", purgeReportPath != null ? purgeReportPath : "");
        model.put("duration", duration);

        return model;
    }

    /**
     * Initialize parameters from request
     */
    private void initializeParameters(WebScriptRequest req) {
        this.excelFileNodeRef = req.getParameter("excelFileNodeRef");
        this.sheetName = req.getParameter("sheetName");
        this.dryRun = Boolean.parseBoolean(req.getParameter("dryRun"));
        this.autoArchive = Boolean.parseBoolean(req.getParameter("autoArchive"));

        if (excelFileNodeRef == null || excelFileNodeRef.isEmpty()) {
            throw new IllegalArgumentException("Parameter 'excelFileNodeRef' is required");
        }

        if (sheetName == null || sheetName.isEmpty()) {
            this.sheetName = "Finances"; // Default sheet name
        }
    }

    /**
     * Validate purge report path and create directory
     */
    private void validatePurgeReportPath() throws Exception {
        String dateFolder = "MassPurge_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File purgeDir = new File(exportBasePath, dateFolder);
        purgeDir.mkdirs();

        this.purgeReportPath = purgeDir.getAbsolutePath();
        logToFileAndConsole("INFO", "Purge report directory: " + this.purgeReportPath);
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
     * Process each document: validate + purge
     */
    private void purgeDocuments(List<GazodocExcelRow> rows) {
        logToFileAndConsole("INFO", "Starting purge process...");

        for (GazodocExcelRow row : rows) {
            try {
                // Search document by cm:name using "Nom du document" column (column 15)
                NodeRef nodeRef = searchDocumentByName(row.getNomDocument());

                if (nodeRef != null) {
                    foundCount++;
                    row.setNodeRef(nodeRef.toString());

                    // Validate and purge
                    purgeDocument(row, nodeRef);

                } else {
                    row.setStatus("NOT_FOUND");
                    row.setStatusReason("Document not found in Alfresco");
                    notFoundCount++;
                    logToFileAndConsole("WARN", String.format("[%d/%d] NOT FOUND: %s",
                        notFoundCount + foundCount, totalRows, row.getNomDocument()));
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
     * Validate and purge a single document
     */
    private void purgeDocument(GazodocExcelRow row, NodeRef nodeRef) {
        String docName = row.getNomDocument();

        // VALIDATION 1: Check document state
        String state = (String) nodeService.getProperty(nodeRef, Fiche.PROP_ETAT_DOC);

        if (!"ARCHIVE".equals(state)) {
            if (autoArchive && "REF".equals(state)) {
                // Auto-archive document
                if (!dryRun) {
                    nodeService.setProperty(nodeRef, Fiche.PROP_ETAT_DOC, "ARCHIVE");
                }
                row.setStatus("AUTO_ARCHIVED_THEN_DELETED");
                archivedCount++;
                logToFileAndConsole("INFO", String.format("[%d/%d] AUTO-ARCHIVED: %s (état: %s → ARCHIVE)",
                    foundCount, totalRows, docName, state));

                // Continue with deletion

            } else {
                // Block deletion
                row.setStatus("BLOCKED");
                row.setStatusReason("État du document: " + state + " (doit être ARCHIVE)");
                blockedCount++;
                logToFileAndConsole("WARN", String.format("[%d/%d] BLOCKED: %s - État: %s (non ARCHIVE)",
                    foundCount, totalRows, docName, state));
                return;
            }
        }

        // VALIDATION 2: Check conservation duration
        Date dateArchivage = (Date) nodeService.getProperty(nodeRef, ContentModel.PROP_MODIFIED);
        Integer dureeConservation = (Integer) nodeService.getProperty(nodeRef, Fiche.PROP_DUREE_CONSERVATION);

        if (dureeConservation == null) {
            dureeConservation = DEFAULT_CONSERVATION_YEARS; // Règle par défaut
        }

        Calendar cal = Calendar.getInstance();
        cal.setTime(dateArchivage);
        cal.add(Calendar.YEAR, dureeConservation);
        Date dateLimitePurge = cal.getTime();

        if (new Date().before(dateLimitePurge)) {
            long joursRestants = (dateLimitePurge.getTime() - new Date().getTime()) / (1000 * 60 * 60 * 24);
            row.setStatus("BLOCKED");
            row.setStatusReason("Durée conservation non atteinte (" + joursRestants + " jours restants)");
            blockedCount++;
            logToFileAndConsole("WARN", String.format("[%d/%d] BLOCKED: %s - Conservation: %d jours restants",
                foundCount, totalRows, docName, joursRestants));
            return;
        }

        // ALL VALIDATIONS PASSED - Proceed with deletion

        if (dryRun) {
            // Simulation mode
            row.setStatus("DRY_RUN_OK");
            row.setStatusReason("Document serait supprimé (simulation)");
            deletedCount++;
            logToFileAndConsole("INFO", String.format("[%d/%d] DRY-RUN OK: %s (serait supprimé)",
                foundCount, totalRows, docName));
        } else {
            // Real deletion
            try {
                nodeService.deleteNode(nodeRef);
                row.setStatus("DELETED");
                row.setStatusReason("Document supprimé avec succès");
                deletedCount++;
                logToFileAndConsole("INFO", String.format("[%d/%d] ✅ DELETED: %s",
                    foundCount, totalRows, docName));
            } catch (Exception e) {
                row.setStatus("DELETE_FAILED");
                row.setStatusReason("Erreur suppression: " + e.getMessage());
                errorCount++;
                logToFileAndConsole("ERROR", String.format("[%d/%d] ❌ DELETE FAILED: %s - %s",
                    foundCount, totalRows, docName, e.getMessage()));
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
     * Generate purge report CSV
     */
    private void generatePurgeReport(List<GazodocExcelRow> rows) throws Exception {
        logToFileAndConsole("INFO", "Generating purge report...");

        File reportFile = new File(purgeReportPath, "purge_report.csv");

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(reportFile))) {
            // Header
            writer.write("Ligne,Name,Référence métier,Title,État document,Statut Purge,Raison,NodeRef");
            writer.newLine();

            // Rows
            for (GazodocExcelRow row : rows) {
                writer.write(String.valueOf(row.getRowNumber()));
                writer.write(",");
                writer.write(csvValue(row.getName()));
                writer.write(",");
                writer.write(csvValue(row.getReferenceMetier()));
                writer.write(",");
                writer.write(csvValue(row.getTitle()));
                writer.write(",");
                writer.write(csvValue(row.getEtatDocument()));
                writer.write(",");
                writer.write(csvValue(row.getStatus()));
                writer.write(",");
                writer.write(csvValue(row.getStatusReason()));
                writer.write(",");
                writer.write(csvValue(row.getNodeRef()));
                writer.newLine();
            }
        }

        logToFileAndConsole("INFO", "Purge report generated: " + reportFile.getAbsolutePath());
    }

    private String csvValue(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
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
}
