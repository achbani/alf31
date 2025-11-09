package org.alfresco.service.export;

import org.alfresco.model.GazodocExcelRow;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.model.ContentModel;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Service de parsing des fichiers Excel pour l'export et la purge de masse.
 * Lit le fichier Excel (format .xls ou .xlsx) et retourne une liste de GazodocExcelRow.
 *
 * @author Alfresco SDK 3
 */
public class ExcelParser {

    private static final Log logger = LogFactory.getLog(ExcelParser.class);
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy");

    private ContentService contentService;

    /**
     * Parse le fichier Excel depuis un NodeRef Alfresco
     *
     * @param excelFileNodeRef NodeRef du fichier Excel dans Alfresco
     * @param sheetName Nom de l'onglet à parser (ex: "Finances")
     * @return Liste des lignes parsées
     * @throws Exception Si erreur de parsing
     */
    public List<GazodocExcelRow> parseExcelFileFromNodeRef(NodeRef excelFileNodeRef, String sheetName)
            throws Exception {

        logger.info("Parsing Excel file: " + excelFileNodeRef + ", sheet: " + sheetName);

        // Récupérer le contenu du fichier Excel
        ContentReader reader = contentService.getReader(excelFileNodeRef, ContentModel.PROP_CONTENT);
        if (reader == null || !reader.exists()) {
            throw new IllegalArgumentException("Excel file not found or has no content: " + excelFileNodeRef);
        }

        InputStream inputStream = reader.getContentInputStream();
        return parseExcelFile(inputStream, sheetName);
    }

    /**
     * Parse le fichier Excel depuis un InputStream
     *
     * @param inputStream InputStream du fichier Excel
     * @param sheetName Nom de l'onglet à parser
     * @return Liste des lignes parsées
     * @throws Exception Si erreur de parsing
     */
    public List<GazodocExcelRow> parseExcelFile(InputStream inputStream, String sheetName)
            throws Exception {

        List<GazodocExcelRow> rows = new ArrayList<>();

        try {
            Workbook workbook = WorkbookFactory.create(inputStream);
            Sheet sheet = workbook.getSheet(sheetName);

            if (sheet == null) {
                throw new IllegalArgumentException("Sheet '" + sheetName + "' not found in Excel file. " +
                        "Available sheets: " + getSheetNames(workbook));
            }

            logger.info("Found sheet '" + sheetName + "' with " + sheet.getLastRowNum() + " rows");

            // Skip header row (row 0)
            int parsedCount = 0;
            int errorCount = 0;

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    logger.debug("Skipping empty row " + i);
                    continue;
                }

                try {
                    GazodocExcelRow excelRow = parseRow(row, i);
                    if (excelRow != null && excelRow.getName() != null && !excelRow.getName().trim().isEmpty()) {
                        rows.add(excelRow);
                        parsedCount++;
                    } else {
                        logger.debug("Skipping row " + i + " - no Name value");
                    }
                } catch (Exception e) {
                    logger.error("Error parsing row " + i + ": " + e.getMessage(), e);
                    errorCount++;
                }
            }

            logger.info("Parsing complete: " + parsedCount + " rows parsed, " + errorCount + " errors");

        } catch (Exception e) {
            logger.error("Error parsing Excel file", e);
            throw new Exception("Failed to parse Excel file: " + e.getMessage(), e);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Exception e) {
                    logger.warn("Error closing input stream", e);
                }
            }
        }

        return rows;
    }

    /**
     * Parse une ligne Excel et retourne un GazodocExcelRow
     */
    private GazodocExcelRow parseRow(Row row, int rowNumber) {
        GazodocExcelRow excelRow = new GazodocExcelRow(rowNumber);

        // Col 1: Name (obligatoire)
        excelRow.setName(getCellValue(row, 0));

        // Col 2: Référence métier
        excelRow.setReferenceMetier(getCellValue(row, 1));

        // Col 3: Title
        excelRow.setTitle(getCellValue(row, 2));

        // Col 4: Creator
        excelRow.setCreator(getCellValue(row, 3));

        // Col 5: Created Date
        excelRow.setCreatedDate(parseDate(getCellValue(row, 4)));

        // Col 6: Date signature
        excelRow.setDateSignature(parseDate(getCellValue(row, 5)));

        // Col 7: Date de validation
        excelRow.setDateValidation(parseDate(getCellValue(row, 6)));

        // Col 8: Date modification ou archivage
        excelRow.setDateModificationArchivage(parseDate(getCellValue(row, 7)));

        // Col 9: Durée de validité (en mois)
        String dureeStr = getCellValue(row, 8);
        if (dureeStr != null && !dureeStr.isEmpty()) {
            try {
                excelRow.setDureeValidite(Integer.parseInt(dureeStr));
            } catch (NumberFormatException e) {
                logger.warn("Invalid duration value at row " + rowNumber + ": " + dureeStr);
            }
        }

        // Col 10: Décision /date purge
        excelRow.setDecisionPurge(getCellValue(row, 9));

        // Col 11: Colonne2 (vide, ignorée)

        // Col 12: Domaine métier
        excelRow.setDomaineMetier(getCellValue(row, 11));

        // Col 13: Sous domaine métier
        excelRow.setSousDomaineMetier(getCellValue(row, 12));

        // Col 14: État du document
        excelRow.setEtatDocument(getCellValue(row, 13));

        // Col 15: Nom du document
        excelRow.setNomDocument(getCellValue(row, 14));

        // Col 16: Annule et remplace
        excelRow.setAnnuleRemplace(getCellValue(row, 15));

        // Col 17: Mots-clés
        excelRow.setMotsCles(getCellValue(row, 16));

        // Col 18: Résumé
        excelRow.setResume(getCellValue(row, 17));

        // Col 19: Auteur(s)
        excelRow.setAuteurs(getCellValue(row, 18));

        // Col 20: Commentaires
        excelRow.setCommentaires(getCellValue(row, 19));

        // Col 21: Contributeur(s)
        excelRow.setContributeurs(getCellValue(row, 20));

        // Col 22: Type de document
        excelRow.setTypeDocument(getCellValue(row, 21));

        // Col 23: Région
        excelRow.setRegion(getCellValue(row, 22));

        // Col 24: Processus
        excelRow.setProcessus(getCellValue(row, 23));

        // Col 25: Savoir-Faire
        excelRow.setSavoirFaire(getCellValue(row, 24));

        // Col 26: Origine
        excelRow.setOrigine(getCellValue(row, 25));

        // Col 27: Niveau de confidentialité
        excelRow.setNiveauConfidentialite(getCellValue(row, 26));

        // Col 28: Destinataire(s)
        excelRow.setDestinataires(getCellValue(row, 27));

        // Col 29: Date d'application
        excelRow.setDateApplication(parseDate(getCellValue(row, 28)));

        // Col 30: Colonne1 (contient "Err:502", ignorée)

        return excelRow;
    }

    /**
     * Récupère la valeur d'une cellule sous forme de String
     */
    private String getCellValue(Row row, int cellIndex) {
        Cell cell = row.getCell(cellIndex);
        if (cell == null) {
            return null;
        }

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();

            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    // Si c'est une date, la formater
                    Date date = cell.getDateCellValue();
                    return DATE_FORMAT.format(date);
                } else {
                    // Si c'est un nombre, le convertir en long pour éviter les décimales
                    return String.valueOf((long) cell.getNumericCellValue());
                }

            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());

            case FORMULA:
                // Évaluer la formule
                try {
                    return cell.getStringCellValue().trim();
                } catch (Exception e) {
                    return String.valueOf(cell.getNumericCellValue());
                }

            case BLANK:
                return null;

            default:
                return null;
        }
    }

    /**
     * Parse une date au format DD/MM/YYYY
     */
    private Date parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }

        try {
            return DATE_FORMAT.parse(dateStr);
        } catch (Exception e) {
            logger.warn("Error parsing date: " + dateStr);
            return null;
        }
    }

    /**
     * Retourne la liste des noms d'onglets du classeur
     */
    private String getSheetNames(Workbook workbook) {
        List<String> sheetNames = new ArrayList<>();
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            sheetNames.add(workbook.getSheetName(i));
        }
        return String.join(", ", sheetNames);
    }

    // ===== Setters for dependency injection =====

    public void setContentService(ContentService contentService) {
        this.contentService = contentService;
    }
}
