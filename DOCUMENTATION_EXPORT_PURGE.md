# Documentation - Export et Purge Massive GAZODOC

## Vue d'ensemble

Ce document explique comment utiliser les deux scripts de traitement massif de documents GAZODOC:
- **Export Massif**: Exporter des documents et leurs métadonnées vers un répertoire NAS
- **Purge Massive**: Supprimer définitivement des documents de GAZODOC

## Prérequis généraux

### 1. Fichier Excel
Préparez un fichier Excel (.xls ou .xlsx) contenant la liste des documents à traiter.

**Colonnes requises:**
- **Colonne 1 (Name)**: Identifiant unique du document (cm:name dans Alfresco)
- **Colonne 15 (Nom du document)**: Nom du fichier pour l'export

**Format de l'onglet:**
- Nom par défaut: `Finances`
- La première ligne doit contenir les en-têtes
- Les données commencent à la ligne 2

**Exemple:**
```
Name                | ... | Nom du document      | ...
FI_2023_001        | ... | Facture_Client_A.pdf | ...
FI_2023_002        | ... | Bon_Commande_B.pdf   | ...
```

### 2. Upload du fichier Excel dans GAZODOC
1. Connectez-vous à GAZODOC Share
2. Uploadez votre fichier Excel dans un espace documentaire
3. Clic droit sur le fichier → "Voir les détails"
4. Copiez le **NodeRef** (format: `workspace://SpacesStore/xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx`)

---

## Export Massif

### Objectif
Exporter en masse des documents GAZODOC vers un répertoire NAS avec leurs métadonnées.

### Accès
```
http://[serveur-gazodoc]/alfresco/s/api/export/mass/form
```

### Étapes d'utilisation

#### 1. Ouvrir le formulaire d'export
Accédez à l'URL ci-dessus dans votre navigateur.

#### 2. Remplir le formulaire
- **NodeRef du fichier Excel**: Collez le NodeRef copié précédemment
- **Nom de l'onglet Excel**: Entrez le nom de l'onglet (par défaut: `Finances`)

#### 3. Lancer l'export
Cliquez sur le bouton **"Lancer l'export massif"**

#### 4. Attendre la fin du traitement
Le processus peut prendre plusieurs minutes selon le nombre de documents.

### Résultats de l'export

Une fois terminé, vous verrez un résumé:
- **Total lignes Excel**: Nombre total de lignes traitées
- **Documents exportés**: Nombre de documents exportés avec succès
- **Documents non trouvés**: Documents présents dans l'Excel mais absents de GAZODOC
- **Erreurs**: Nombre d'erreurs rencontrées

### Structure du répertoire d'export

Les fichiers sont exportés dans le répertoire configuré (export.base.path):

```
[export.base.path]/export_YYYYMMDD_HHmmss/
├── documents/
│   ├── FI_2023_001/
│   │   └── Facture_Client_A.pdf
│   ├── FI_2023_002/
│   │   └── Bon_Commande_B.pdf
│   └── ...
├── metadata/
│   └── export_metadata.csv
├── manifest.json
├── export_report.csv
└── MassExport_YYYYMMDD_HHmmss.log
```

**Fichiers générés:**
- **documents/[Name]/[NomDocument]**: Fichiers exportés organisés par sous-dossiers
- **metadata/export_metadata.csv**: Toutes les métadonnées GAZODOC (30 colonnes)
- **manifest.json**: Résumé de l'export au format JSON
- **export_report.csv**: Rapport des erreurs et documents non trouvés
- **MassExport_*.log**: Journal détaillé de l'opération

### Interprétation des statuts

Dans le fichier `export_report.csv`, vous trouverez les statuts suivants:
- **EXPORTE**: Document exporté avec succès
- **NON_TROUVE**: Document non trouvé dans GAZODOC
- **ECHEC_EXPORT**: Erreur lors de l'export
- **ERREUR**: Erreur générale

---

## Purge Massive

### Objectif
Supprimer définitivement des documents de GAZODOC.

### ⚠️ AVERTISSEMENTS IMPORTANTS

**CETTE OPÉRATION EST IRRÉVERSIBLE !**

**Avant toute purge:**
1. Effectuez un **backup complet** de GAZODOC
2. Effectuez un **export massif** des documents à purger
3. Vérifiez que l'export est complet et valide
4. **Aucune validation n'est effectuée** - Tous les documents trouvés seront supprimés

### Accès
```
http://[serveur-gazodoc]/alfresco/s/api/purge/mass/form
```

### Étapes d'utilisation

#### 1. BACKUP ET EXPORT
**OBLIGATOIRE avant toute purge:**
- Effectuez un backup complet de GAZODOC
- Lancez un export massif des documents (voir section précédente)
- Vérifiez que l'export contient tous les documents

#### 2. Ouvrir le formulaire de purge
Accédez à l'URL ci-dessus dans votre navigateur.

#### 3. Remplir le formulaire
- **NodeRef du fichier Excel**: Collez le NodeRef du fichier Excel
- **Nom de l'onglet Excel**: Entrez le nom de l'onglet (par défaut: `Finances`)

**Note**: Utilisez le **même fichier Excel** que pour l'export.

#### 4. Lancer la purge
Cliquez sur le bouton **"LANCER LA PURGE"**

**Confirmez** que vous avez bien:
- Effectué le backup
- Effectué l'export massif
- Vérifié la validité de l'export

#### 5. Attendre la fin du traitement
Le processus peut prendre plusieurs minutes selon le nombre de documents.

### Résultats de la purge

Une fois terminé, vous verrez un résumé:
- **Total lignes Excel**: Nombre total de lignes traitées
- **Documents supprimés**: Nombre de documents supprimés avec succès
- **Documents non trouvés**: Documents présents dans l'Excel mais absents de GAZODOC (déjà supprimés?)
- **Erreurs de suppression**: Nombre d'erreurs rencontrées

### Fichiers générés

Les résultats sont enregistrés dans le répertoire configuré:

```
[export.base.path]/purge_YYYYMMDD_HHmmss/
├── purge_report.csv
└── MassPurge_YYYYMMDD_HHmmss.log
```

**Fichiers générés:**
- **purge_report.csv**: Rapport détaillé de la purge (statut de chaque document)
- **MassPurge_*.log**: Journal détaillé de l'opération

### Interprétation des statuts

Dans le fichier `purge_report.csv`, vous trouverez les statuts suivants:
- **SUPPRIME**: Document supprimé avec succès
- **NON_TROUVE**: Document non trouvé dans GAZODOC
- **ECHEC_SUPPRESSION**: Erreur lors de la suppression
- **ERREUR**: Erreur générale

---

## Workflow recommandé (Export + Purge)

### Étape 1: Préparation
1. Préparez votre fichier Excel avec la liste des documents
2. Uploadez le fichier Excel dans GAZODOC
3. Récupérez le NodeRef du fichier Excel

### Étape 2: Export (OBLIGATOIRE)
1. Accédez au formulaire d'export: `/alfresco/s/api/export/mass/form`
2. Entrez le NodeRef et le nom de l'onglet
3. Lancez l'export
4. **Attendez la fin complète** de l'export
5. Vérifiez les statistiques:
   - Combien de documents exportés ?
   - Combien de documents non trouvés ?
   - Y a-t-il des erreurs ?

### Étape 3: Vérification de l'export
1. Accédez au répertoire d'export sur le NAS
2. Vérifiez que les fichiers sont présents
3. Vérifiez le fichier `metadata/export_metadata.csv`
4. Consultez le fichier `export_report.csv` pour voir les documents non exportés

### Étape 4: Backup
1. Effectuez un backup complet de GAZODOC
2. Vérifiez que le backup est valide

### Étape 5: Purge
1. Accédez au formulaire de purge: `/alfresco/s/api/purge/mass/form`
2. Entrez le **même NodeRef** que pour l'export
3. Entrez le **même nom d'onglet** que pour l'export
4. Lancez la purge
5. **Attendez la fin complète** de la purge
6. Vérifiez les statistiques:
   - Combien de documents supprimés ?
   - Combien de documents non trouvés ? (normal si déjà supprimés)
   - Y a-t-il des erreurs ?

### Étape 6: Vérification post-purge
1. Consultez le fichier `purge_report.csv`
2. Vérifiez que tous les documents ont été supprimés (statut SUPPRIME)
3. Si des erreurs sont présentes, analysez les raisons dans le fichier log

---

## Dépannage

### Export: "Document non trouvé"
**Cause**: Le document n'existe pas dans GAZODOC ou la colonne "Name" est incorrecte.

**Solutions**:
1. Vérifiez que la valeur dans la colonne "Name" correspond exactement au cm:name du document
2. Recherchez manuellement le document dans GAZODOC
3. Vérifiez que le document n'a pas été supprimé ou déplacé

### Export: "Erreur d'export"
**Cause**: Problème lors de la lecture du contenu ou de l'écriture sur le NAS.

**Solutions**:
1. Vérifiez les permissions d'écriture sur le répertoire NAS
2. Vérifiez que le disque n'est pas plein
3. Consultez le fichier log pour plus de détails

### Purge: "Erreur de suppression"
**Cause**: Le document ne peut pas être supprimé (permissions, verrouillage, etc.).

**Solutions**:
1. Vérifiez que vous avez les droits de suppression
2. Vérifiez que le document n'est pas verrouillé (check-out)
3. Vérifiez qu'il n'y a pas de règles Alfresco bloquant la suppression
4. Consultez le fichier log pour plus de détails

### Le formulaire ne charge pas
**Cause**: Problème de configuration ou de déploiement.

**Solutions**:
1. Vérifiez que le serveur GAZODOC est démarré
2. Vérifiez l'URL (doit contenir `/alfresco/s/api/`)
3. Vérifiez les logs du serveur GAZODOC

### Le traitement est très lent
**Cause**: Normal pour un grand nombre de documents.

**Informations**:
- L'export peut prendre plusieurs heures pour des milliers de documents
- La purge est généralement plus rapide que l'export
- Vous pouvez suivre la progression dans les logs en temps réel

---

## Configuration serveur

### Paramètre export.base.path

Le répertoire de base pour les exports et purges est défini dans la configuration GAZODOC:

**Fichier**: `alfresco-global.properties`

**Paramètre**:
```properties
export.base.path=/path/to/nas/exports
```

**Valeur par défaut**: Si non configuré, utilise `/tmp/gazodoc_exports`

**Important**:
- Le répertoire doit exister
- Le serveur GAZODOC doit avoir les droits d'écriture
- Préférez un montage NAS pour la production

---

## Contact et support

Pour toute question ou problème:
1. Consultez les fichiers logs dans les répertoires d'export/purge
2. Vérifiez les logs du serveur GAZODOC (alfresco.log)
3. Contactez l'équipe GAZODOC avec les informations suivantes:
   - Fichier log de l'opération
   - Fichier rapport CSV
   - Nombre de documents traités
   - Message d'erreur exact

---

## Annexes

### Format du fichier export_metadata.csv

Le fichier contient 30 colonnes de métadonnées GAZODOC:
1. NodeRef
2. Name
3. Nom du document
4. Titre
5. Date de création
6. Date de modification
7. Créateur
8. Modificateur
9. Type de contenu
10. Description
11. État
12. Validateur
13. Date de validation
14. Domaine métier (nom de catégorie)
15. Sous domaine métier (nom de catégorie)
16. Mots-clés (noms de catégories)
17. Type document (nom de catégorie)
18. Référence métier
19. Région (nom de catégorie)
20. Processus (nom de catégorie)
21. Origine (nom de catégorie)
22. Durée de conservation (années)
23. Date de fin de conservation
24. Commentaire
25. Version
26. Taille (octets)
27. Type MIME
28. Extension
29. Archivé (oui/non)
30. Date d'archivage

**Note**: Les catégories (colonnes 14-21) sont résolues en noms lisibles (et non en NodeRef).

### Format du fichier manifest.json

Fichier JSON contenant:
```json
{
  "exportDate": "2024-01-15T14:30:00",
  "totalRows": 5000,
  "exportedCount": 4596,
  "notFoundCount": 15,
  "errorCount": 4,
  "duration": "1245.67",
  "excelFile": {
    "nodeRef": "workspace://SpacesStore/...",
    "name": "Liste_Finances.xlsx",
    "sheetName": "Finances"
  },
  "exportedDocuments": [
    {
      "rowNumber": 2,
      "name": "FI_2023_001",
      "nomDocument": "Facture_Client_A.pdf",
      "nodeRef": "workspace://SpacesStore/...",
      "exportPath": "documents/FI_2023_001/Facture_Client_A.pdf"
    }
  ],
  "notFoundDocuments": [...],
  "errorDocuments": [...]
}
```
