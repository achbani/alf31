package org.alfresco.model;

import java.util.Date;

/**
 * Représente une ligne du fichier Excel Finance pour l'export et la purge de masse.
 * Mapping des 30 colonnes du fichier Excel vers les propriétés GAZODOC.
 *
 * @author Alfresco SDK 3
 */
public class GazodocExcelRow {

    // ===== Colonnes principales =====

    /** Col 1: Name - Identifiant unique Alfresco (ex: GRDF-NAT-SMG-GUI-FIN-00576) */
    private String name;

    /** Col 2: Référence métier - Identifiant court (ex: PAPPU5) */
    private String referenceMetier;

    /** Col 3: Title - Titre du document */
    private String title;

    /** Col 4: Creator - Créateur du document */
    private String creator;

    /** Col 5: Created Date - Date de création */
    private Date createdDate;

    // ===== Dates =====

    /** Col 6: Date signature */
    private Date dateSignature;

    /** Col 7: Date de validation */
    private Date dateValidation;

    /** Col 8: Date modification ou archivage */
    private Date dateModificationArchivage;

    // ===== Métadonnées de gestion =====

    /** Col 9: Durée de validité du document (en mois) */
    private Integer dureeValidite;

    /** Col 10: Décision /date purge (ex: "A purger") */
    private String decisionPurge;

    /** Col 12: Domaine métier */
    private String domaineMetier;

    /** Col 13: Sous domaine métier */
    private String sousDomaineMetier;

    /** Col 14: État du document (ex: "Archivé") */
    private String etatDocument;

    /** Col 15: Nom du document (ex: PAPPU5.pdf) */
    private String nomDocument;

    // ===== Informations complémentaires =====

    /** Col 16: Annule et remplace */
    private String annuleRemplace;

    /** Col 17: Mots-clés */
    private String motsCles;

    /** Col 18: Résumé */
    private String resume;

    /** Col 19: Auteur(s) */
    private String auteurs;

    /** Col 20: Commentaires */
    private String commentaires;

    /** Col 21: Contributeur(s) */
    private String contributeurs;

    /** Col 22: Type de document (ex: "Guide", "PPP", "Enregistrement") */
    private String typeDocument;

    /** Col 23: Région (ex: "National") */
    private String region;

    /** Col 24: Processus (ex: "FIN - Controlling Finance") */
    private String processus;

    /** Col 25: Savoir-Faire */
    private String savoirFaire;

    /** Col 26: Origine (ex: "Interne") */
    private String origine;

    /** Col 27: Niveau de confidentialité (ex: "C1", "C2") */
    private String niveauConfidentialite;

    /** Col 28: Destinataire(s) */
    private String destinataires;

    /** Col 29: Date d'application */
    private Date dateApplication;

    // ===== Statut de traitement (calculé) =====

    /** Statut de validation/purge (OK, BLOCKED, DELETED, SKIPPED) */
    private String status;

    /** Raison du statut (message d'erreur ou de succès) */
    private String statusReason;

    /** NodeRef Alfresco du document trouvé */
    private String nodeRef;

    /** Numéro de ligne dans le fichier Excel (pour traçabilité) */
    private int rowNumber;

    // ===== Constructeurs =====

    public GazodocExcelRow() {
    }

    public GazodocExcelRow(int rowNumber) {
        this.rowNumber = rowNumber;
    }

    // ===== Getters et Setters =====

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getReferenceMetier() {
        return referenceMetier;
    }

    public void setReferenceMetier(String referenceMetier) {
        this.referenceMetier = referenceMetier;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public Date getDateSignature() {
        return dateSignature;
    }

    public void setDateSignature(Date dateSignature) {
        this.dateSignature = dateSignature;
    }

    public Date getDateValidation() {
        return dateValidation;
    }

    public void setDateValidation(Date dateValidation) {
        this.dateValidation = dateValidation;
    }

    public Date getDateModificationArchivage() {
        return dateModificationArchivage;
    }

    public void setDateModificationArchivage(Date dateModificationArchivage) {
        this.dateModificationArchivage = dateModificationArchivage;
    }

    public Integer getDureeValidite() {
        return dureeValidite;
    }

    public void setDureeValidite(Integer dureeValidite) {
        this.dureeValidite = dureeValidite;
    }

    public String getDecisionPurge() {
        return decisionPurge;
    }

    public void setDecisionPurge(String decisionPurge) {
        this.decisionPurge = decisionPurge;
    }

    public String getDomaineMetier() {
        return domaineMetier;
    }

    public void setDomaineMetier(String domaineMetier) {
        this.domaineMetier = domaineMetier;
    }

    public String getSousDomaineMetier() {
        return sousDomaineMetier;
    }

    public void setSousDomaineMetier(String sousDomaineMetier) {
        this.sousDomaineMetier = sousDomaineMetier;
    }

    public String getEtatDocument() {
        return etatDocument;
    }

    public void setEtatDocument(String etatDocument) {
        this.etatDocument = etatDocument;
    }

    public String getNomDocument() {
        return nomDocument;
    }

    public void setNomDocument(String nomDocument) {
        this.nomDocument = nomDocument;
    }

    public String getAnnuleRemplace() {
        return annuleRemplace;
    }

    public void setAnnuleRemplace(String annuleRemplace) {
        this.annuleRemplace = annuleRemplace;
    }

    public String getMotsCles() {
        return motsCles;
    }

    public void setMotsCles(String motsCles) {
        this.motsCles = motsCles;
    }

    public String getResume() {
        return resume;
    }

    public void setResume(String resume) {
        this.resume = resume;
    }

    public String getAuteurs() {
        return auteurs;
    }

    public void setAuteurs(String auteurs) {
        this.auteurs = auteurs;
    }

    public String getCommentaires() {
        return commentaires;
    }

    public void setCommentaires(String commentaires) {
        this.commentaires = commentaires;
    }

    public String getContributeurs() {
        return contributeurs;
    }

    public void setContributeurs(String contributeurs) {
        this.contributeurs = contributeurs;
    }

    public String getTypeDocument() {
        return typeDocument;
    }

    public void setTypeDocument(String typeDocument) {
        this.typeDocument = typeDocument;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getProcessus() {
        return processus;
    }

    public void setProcessus(String processus) {
        this.processus = processus;
    }

    public String getSavoirFaire() {
        return savoirFaire;
    }

    public void setSavoirFaire(String savoirFaire) {
        this.savoirFaire = savoirFaire;
    }

    public String getOrigine() {
        return origine;
    }

    public void setOrigine(String origine) {
        this.origine = origine;
    }

    public String getNiveauConfidentialite() {
        return niveauConfidentialite;
    }

    public void setNiveauConfidentialite(String niveauConfidentialite) {
        this.niveauConfidentialite = niveauConfidentialite;
    }

    public String getDestinataires() {
        return destinataires;
    }

    public void setDestinataires(String destinataires) {
        this.destinataires = destinataires;
    }

    public Date getDateApplication() {
        return dateApplication;
    }

    public void setDateApplication(Date dateApplication) {
        this.dateApplication = dateApplication;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatusReason() {
        return statusReason;
    }

    public void setStatusReason(String statusReason) {
        this.statusReason = statusReason;
    }

    public String getNodeRef() {
        return nodeRef;
    }

    public void setNodeRef(String nodeRef) {
        this.nodeRef = nodeRef;
    }

    public int getRowNumber() {
        return rowNumber;
    }

    public void setRowNumber(int rowNumber) {
        this.rowNumber = rowNumber;
    }

    @Override
    public String toString() {
        return "GazodocExcelRow{" +
                "rowNumber=" + rowNumber +
                ", name='" + name + '\'' +
                ", title='" + title + '\'' +
                ", etatDocument='" + etatDocument + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
