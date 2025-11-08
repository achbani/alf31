package org.alfresco.model;

import org.alfresco.service.namespace.QName;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author sopra
 */
@SuppressWarnings("unused")
public final class Fiche {

    //----------
    // Constraints
    //----------

    private static final String STATE_DOC_ELAB = "ELAB";
    private static final String STATE_DOC_MODIF = "REF/MODIF";
    public static final String STATE_DOC_ATTENTVALID = "ATTENTVALID";
    private static final String STATE_DOC_REF = "REF";
    private static final String STATE_DOC_REV = "REF/REV";
    private static final String STATE_DOC_ARCHIVE = "ARCHIVE";

    @SuppressWarnings("squid:S1171") // non static initialisation
    public static final Map<String, String> CONSTRAINTS_STATE_DOC = Collections.unmodifiableMap(new HashMap<String, String>() {
        private static final long serialVersionUID = 1175429724859868726L;

        {
            put(STATE_DOC_ELAB, "Provisoire");
            put(STATE_DOC_MODIF, "Valide, en cours de modification");
            put(STATE_DOC_ATTENTVALID, "En cours de validation");
            put(STATE_DOC_REF, "Valide");
            put(STATE_DOC_REV, "Valide : doit être revu ou archivé");
            put(STATE_DOC_ARCHIVE, "Archivé");
        }
    });

    @SuppressWarnings("squid:S1171") // non static initialisation
    public static final Map<String, String> CONSTRAINTS_CONFIDENTIAL = Collections.unmodifiableMap(new HashMap<String, String>() {
        private static final long serialVersionUID = -4841508140086729236L;

        {
            put("C4", "C4 Strictement confidentiel");
            put("C3", "C3 Diffusion restreinte");
            put("C2", "C2 Usage interne");
            put("C1", "C1 Diffusion libre");
        }
    });

    //----------
    // Model
    //----------

    // General
    private static final String MODEL_NAMESPACE = "http://www.alfresco.org/model/grdf/fiche/1.0";
    public static final String MODEL_PREFIXE = "grdfFiche";

    // Aspects
    public static final QName ASPECT_DELEGATION = QName.createQName(MODEL_NAMESPACE, "sousDomaineMetier");
    public static final QName ASPECT_META_FICHE = QName.createQName(MODEL_NAMESPACE, "ficheMeta");
    public static final QName ASPECT_OVERDATED = QName.createQName(MODEL_NAMESPACE, "overDated");
    public static final QName ASPECT_MIGRATION = QName.createQName(MODEL_NAMESPACE, "ficheMigration");
    public static final QName ASPECT_REFERENCE = QName.createQName(MODEL_NAMESPACE, "ficheReference");
    public static final QName ASPECT_DOMAINE = QName.createQName(MODEL_NAMESPACE, "domaineMetier");
    public static final QName ASPECT_MAIL_DEJA_ENVOYE = QName.createQName(MODEL_NAMESPACE, "mailDejaEnvoye");
    public static final QName ASPECT_REFERENCE_COMPTEUR = QName.createQName(MODEL_NAMESPACE, "referenceCompteur");

    // Properties
    public static final QName PROP_DELEGATION = QName.createQName(MODEL_NAMESPACE, "sousDomaineMetier");
    public static final QName PROP_DOC_NAME = QName.createQName(MODEL_NAMESPACE, "docName");
    public static final QName PROP_DATE_VALIDATION = QName.createQName(MODEL_NAMESPACE, "dateValidation");
    public static final QName PROP_DATE_SIGNATURE = QName.createQName(MODEL_NAMESPACE, "dateSignature");
    public static final QName PROP_ANNULE_REMPLACE = QName.createQName(MODEL_NAMESPACE, "annuleRemplace");
    public static final QName PROP_ETAT_DOC = QName.createQName(MODEL_NAMESPACE, "docState");
    public static final QName PROP_MOTS_CLEFS = QName.createQName(MODEL_NAMESPACE, "motClef");
    public static final QName PROP_DOMAINE_METIER = QName.createQName(MODEL_NAMESPACE, "domaineMetier");
    public static final QName PROP_PROCESSUS = QName.createQName(MODEL_NAMESPACE, "processus");
    public static final QName PROP_SAVOIR_FAIRE = QName.createQName(MODEL_NAMESPACE, "savoirFaire");
    public static final QName PROP_ORIGINE = QName.createQName(MODEL_NAMESPACE, "origine");
    public static final QName PROP_COMMENTAIRE = QName.createQName(MODEL_NAMESPACE, "commentaire");
    public static final QName PROP_CONTRIBUTEUR = QName.createQName(MODEL_NAMESPACE, "contributeur");
    public static final QName PROP_TYPE_DOC = QName.createQName(MODEL_NAMESPACE, "typeDocument");
    public static final QName PROP_REGION = QName.createQName(MODEL_NAMESPACE, "region");
    public static final QName PROP_REF_ANCIENNE = QName.createQName(MODEL_NAMESPACE, "referenceAncienne");
    public static final QName PROP_CONFIDENTIALITE = QName.createQName(MODEL_NAMESPACE, "confidentialite");
    public static final QName PROP_DUREE_VALIDITE = QName.createQName(MODEL_NAMESPACE, "dureeValidite");
    public static final QName PROP_DESTINATAIRE = QName.createQName(MODEL_NAMESPACE, "destinataire");
    public static final QName PROP_DATE_APPLICATION = QName.createQName(MODEL_NAMESPACE, "dateApplication");
    public static final QName PROP_DATE_FIN_APPLICATION = QName.createQName(MODEL_NAMESPACE, "dateFinApplication");
    public static final QName PROP_REFERENCE_COMPTEUR_MAP = QName.createQName(MODEL_NAMESPACE, "referenceCompteurMap");
    public static final QName PROP_DOCS_VALIDEUR = QName.createQName(MODEL_NAMESPACE, "valideur");
    public static final QName PROP_DUREE_CONSERVATION = QName.createQName(MODEL_NAMESPACE, "dureeConservation");

    // Associations
    public static final QName ASSOC_DOCS_ASSOCIES = QName.createQName(MODEL_NAMESPACE, "documentAssocie");
    public static final QName ASSOC_DOCS_ANNEXES = QName.createQName(MODEL_NAMESPACE, "documentAnnexe");

    //----------
    // Liste des propriété à affiché pour le workflow
    //----------

    public static final List<QName> WORKFLOW_MANDATORY = Collections.unmodifiableList(Arrays.asList(
            PROP_DOMAINE_METIER,
            ContentModel.PROP_TITLE,
            ContentModel.PROP_AUTHOR,
            PROP_REGION,
            PROP_PROCESSUS,
            PROP_SAVOIR_FAIRE,
            PROP_ORIGINE,
            PROP_CONFIDENTIALITE,
            PROP_DUREE_VALIDITE,
            PROP_DATE_APPLICATION,
            PROP_TYPE_DOC,
            PROP_DESTINATAIRE,
            PROP_MOTS_CLEFS,
            ContentModel.PROP_DESCRIPTION
    ));

    //----------
    // Constructors
    //----------

    private Fiche() {
    }
}


