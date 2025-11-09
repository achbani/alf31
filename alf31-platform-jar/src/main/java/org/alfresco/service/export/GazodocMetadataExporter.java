package org.alfresco.service.export;

import org.alfresco.model.ContentModel;
import org.alfresco.model.Fiche;
import org.alfresco.model.GazodocExcelRow;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Service d'export des métadonnées GAZODOC au format CSV.
 * Extrait toutes les propriétés GAZODOC d'un document et les exporte dans un fichier CSV.
 *
 * @author Alfresco SDK 3
 */
public class GazodocMetadataExporter {

    private static final Log logger = LogFactory.getLog(GazodocMetadataExporter.class);
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy");
    private static final String CSV_SEPARATOR = ",";
    private static final String CSV_QUOTE = "\"";

    private NodeService nodeService;

    /**
     * Exporte les métadonnées de tous les documents dans un fichier CSV
     *
     * @param rows Liste des lignes Excel avec NodeRef trouvés
     * @param outputFile Fichier CSV de sortie
     * @throws IOException Si erreur d'écriture
     */
    public void exportMetadata(List<GazodocExcelRow> rows, File outputFile) throws IOException {
        logger.info("Exporting metadata to CSV: " + outputFile.getAbsolutePath());

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            // Écrire l'en-tête CSV
            writeHeader(writer);

            // Écrire chaque ligne
            int exportedCount = 0;
            for (GazodocExcelRow row : rows) {
                if (row.getNodeRef() != null) {
                    try {
                        NodeRef nodeRef = new NodeRef(row.getNodeRef());
                        writeMetadataRow(writer, row, nodeRef);
                        exportedCount++;
                    } catch (Exception e) {
                        logger.error("Error exporting metadata for row " + row.getRowNumber(), e);
                    }
                }
            }

            logger.info("Metadata export complete: " + exportedCount + " documents exported");
        }
    }

    /**
     * Écrit l'en-tête du fichier CSV
     */
    private void writeHeader(BufferedWriter writer) throws IOException {
        writer.write("Name,");
        writer.write("Référence métier,");
        writer.write("Title,");
        writer.write("Creator,");
        writer.write("Created Date,");
        writer.write("Date signature,");
        writer.write("Date de validation,");
        writer.write("Date modification ou archivage,");
        writer.write("Durée de validité du document (en mois),");
        writer.write("Domaine métier,");
        writer.write("Sous domaine métier,");
        writer.write("État du document,");
        writer.write("Nom du document,");
        writer.write("Annule et remplace,");
        writer.write("Mots-clés,");
        writer.write("Résumé,");
        writer.write("Auteur(s),");
        writer.write("Commentaires,");
        writer.write("Contributeur(s),");
        writer.write("Type de document,");
        writer.write("Région,");
        writer.write("Processus,");
        writer.write("Savoir-Faire,");
        writer.write("Origine,");
        writer.write("Niveau de confidentialité,");
        writer.write("Destinataire(s),");
        writer.write("Date d'application,");
        writer.write("NodeRef,");
        writer.write("Chemin Alfresco");
        writer.newLine();
    }

    /**
     * Écrit une ligne de métadonnées dans le fichier CSV
     */
    private void writeMetadataRow(BufferedWriter writer, GazodocExcelRow row, NodeRef nodeRef) throws IOException {
        // Vérifier que le node existe
        if (!nodeService.exists(nodeRef)) {
            logger.warn("Node does not exist: " + nodeRef);
            return;
        }

        // Name (cm:name)
        writer.write(csvValue(getProperty(nodeRef, ContentModel.PROP_NAME)));

        // Référence métier
        writer.write(csvValue(row.getReferenceMetier()));

        // Title (cm:title)
        writer.write(csvValue(getProperty(nodeRef, ContentModel.PROP_TITLE)));

        // Creator (cm:creator)
        writer.write(csvValue(getProperty(nodeRef, ContentModel.PROP_CREATOR)));

        // Created Date (cm:created)
        writer.write(csvValue(getDateProperty(nodeRef, ContentModel.PROP_CREATED)));

        // Date signature
        writer.write(csvValue(getDateProperty(nodeRef, Fiche.PROP_DATE_SIGNATURE)));

        // Date de validation
        writer.write(csvValue(getDateProperty(nodeRef, Fiche.PROP_DATE_VALIDATION)));

        // Date modification (cm:modified)
        writer.write(csvValue(getDateProperty(nodeRef, ContentModel.PROP_MODIFIED)));

        // Durée de validité
        writer.write(csvValue(getProperty(nodeRef, Fiche.PROP_DUREE_VALIDITE)));

        // Domaine métier
        writer.write(csvValue(getProperty(nodeRef, Fiche.PROP_DOMAINE_METIER)));

        // Sous domaine métier
        writer.write(csvValue(getProperty(nodeRef, Fiche.PROP_DELEGATION)));

        // État du document
        writer.write(csvValue(getProperty(nodeRef, Fiche.PROP_ETAT_DOC)));

        // Nom du document
        writer.write(csvValue(getProperty(nodeRef, Fiche.PROP_DOC_NAME)));

        // Annule et remplace
        writer.write(csvValue(getProperty(nodeRef, Fiche.PROP_ANNULE_REMPLACE)));

        // Mots-clés
        writer.write(csvValue(getProperty(nodeRef, Fiche.PROP_MOTS_CLEFS)));

        // Résumé (cm:description)
        writer.write(csvValue(getProperty(nodeRef, ContentModel.PROP_DESCRIPTION)));

        // Auteur(s) (cm:author)
        writer.write(csvValue(getProperty(nodeRef, ContentModel.PROP_AUTHOR)));

        // Commentaires
        writer.write(csvValue(getProperty(nodeRef, Fiche.PROP_COMMENTAIRE)));

        // Contributeur(s)
        writer.write(csvValue(getProperty(nodeRef, Fiche.PROP_CONTRIBUTEUR)));

        // Type de document
        writer.write(csvValue(getProperty(nodeRef, Fiche.PROP_TYPE_DOC)));

        // Région
        writer.write(csvValue(getProperty(nodeRef, Fiche.PROP_REGION)));

        // Processus
        writer.write(csvValue(getProperty(nodeRef, Fiche.PROP_PROCESSUS)));

        // Savoir-Faire
        writer.write(csvValue(getProperty(nodeRef, Fiche.PROP_SAVOIR_FAIRE)));

        // Origine
        writer.write(csvValue(getProperty(nodeRef, Fiche.PROP_ORIGINE)));

        // Niveau de confidentialité
        writer.write(csvValue(getProperty(nodeRef, Fiche.PROP_CONFIDENTIALITE)));

        // Destinataire(s)
        writer.write(csvValue(getProperty(nodeRef, Fiche.PROP_DESTINATAIRE)));

        // Date d'application
        writer.write(csvValue(getDateProperty(nodeRef, Fiche.PROP_DATE_APPLICATION)));

        // NodeRef
        writer.write(csvValue(nodeRef.toString()));

        // Chemin Alfresco
        writer.write(csvValue(nodeService.getPath(nodeRef).toString()));

        writer.newLine();
    }

    /**
     * Récupère une propriété du node
     */
    private String getProperty(NodeRef nodeRef, QName property) {
        Object value = nodeService.getProperty(nodeRef, property);
        return value != null ? value.toString() : "";
    }

    /**
     * Récupère une propriété de type Date et la formate
     */
    private String getDateProperty(NodeRef nodeRef, QName property) {
        Object value = nodeService.getProperty(nodeRef, property);
        if (value instanceof Date) {
            return DATE_FORMAT.format((Date) value);
        }
        return "";
    }

    /**
     * Formate une valeur pour le CSV (échappement des guillemets et virgules)
     */
    private String csvValue(String value) {
        if (value == null || value.isEmpty()) {
            return CSV_SEPARATOR;
        }

        // Échapper les guillemets
        String escaped = value.replace(CSV_QUOTE, CSV_QUOTE + CSV_QUOTE);

        // Entourer de guillemets si contient des caractères spéciaux
        if (escaped.contains(CSV_SEPARATOR) || escaped.contains("\n") || escaped.contains(CSV_QUOTE)) {
            return CSV_QUOTE + escaped + CSV_QUOTE + CSV_SEPARATOR;
        }

        return escaped + CSV_SEPARATOR;
    }

    // ===== Setters for dependency injection =====

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }
}
