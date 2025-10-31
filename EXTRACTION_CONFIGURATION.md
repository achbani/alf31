# Configuration de l'Export de Documents

## Configuration Requise

### 1. Configurer le Chemin d'Extraction

Le chemin d'extraction doit être configuré dans `alfresco-global.properties` pour des raisons de sécurité.

**Fichier :** `tomcat/shared/classes/alfresco-global.properties`

```properties
###############################
## Document Extraction Settings
###############################

## Base directory for document extraction exports
## This directory MUST exist and be writable by the Alfresco process
## Each export will create a dated subfolder (Export_YYYYMMDD_HHmmss) under this path
extraction.base.path=/mnt/contentstore2/ExtractionTravodoc
```

### 2. Créer le Répertoire de Base

**Important :** Le répertoire de base doit être créé manuellement par l'administrateur système. Il ne sera PAS créé automatiquement par l'application pour des raisons de sécurité.

```bash
# Créer le répertoire
sudo mkdir -p /mnt/contentstore2/ExtractionTravodoc

# Définir les permissions (remplacer 'alfresco' par l'utilisateur Alfresco)
sudo chown alfresco:alfresco /mnt/contentstore2/ExtractionTravodoc
sudo chmod 755 /mnt/contentstore2/ExtractionTravodoc
```

### 3. Vérifier les Permissions

Le processus Alfresco doit avoir les droits d'écriture sur ce répertoire :

```bash
# Vérifier les permissions
ls -ld /mnt/contentstore2/ExtractionTravodoc

# Exemple de sortie attendue :
# drwxr-xr-x 2 alfresco alfresco 4096 Oct 31 14:30 /mnt/contentstore2/ExtractionTravodoc
```

## Comportement de l'Application

### Structure des Répertoires

Chaque export crée automatiquement un sous-dossier daté :

```
/mnt/contentstore2/ExtractionTravodoc/
    ├── Export_20241031_143052/    ← Export du 31 oct 2024 à 14:30:52
    │   ├── document1.pdf
    │   ├── document2.docx
    │   └── document3.xlsx
    ├── Export_20241031_155823/    ← Export du 31 oct 2024 à 15:58:23
    │   ├── autre1.pdf
    │   └── autre2.pdf
    └── Export_20241101_092145/    ← Export du 1er nov 2024 à 09:21:45
        └── fichier.pdf
```

### Messages d'Erreur

Si le répertoire de base n'existe pas ou n'est pas accessible, l'export échouera avec un message d'erreur explicite :

#### Erreur : Répertoire inexistant
```
WARNING: Base extraction directory does not exist: /mnt/contentstore2/ExtractionTravodoc.
Please create this directory and ensure the Alfresco process has write permissions.
Configure the path in alfresco-global.properties using 'extraction.base.path' property.
```

**Solution :** Créer le répertoire avec les permissions appropriées (voir section 2).

#### Erreur : Répertoire non accessible en écriture
```
WARNING: Base extraction directory is not writable: /mnt/contentstore2/ExtractionTravodoc.
Please grant write permissions to the Alfresco process.
```

**Solution :** Ajuster les permissions du répertoire.

#### Erreur : Propriété non configurée
```
Extraction base path is not configured.
Please set 'extraction.base.path' in alfresco-global.properties
```

**Solution :** Ajouter la propriété `extraction.base.path` dans `alfresco-global.properties`.

## Redémarrage d'Alfresco

Après avoir modifié `alfresco-global.properties`, redémarrez Alfresco :

```bash
# Arrêter Alfresco
./alfresco.sh stop

# Démarrer Alfresco
./alfresco.sh start

# Ou redémarrer en une commande
./alfresco.sh restart
```

## Maintenance

### Nettoyage des Anciens Exports

Les exports ne sont pas supprimés automatiquement. L'administrateur doit mettre en place une stratégie de nettoyage :

```bash
# Exemple : Supprimer les exports de plus de 30 jours
find /mnt/contentstore2/ExtractionTravodoc/Export_* -type d -mtime +30 -exec rm -rf {} \;

# Créer une tâche cron pour automatiser le nettoyage
# Ouvrir crontab
crontab -e

# Ajouter cette ligne pour nettoyer tous les dimanches à 2h du matin
0 2 * * 0 find /mnt/contentstore2/ExtractionTravodoc/Export_* -type d -mtime +30 -exec rm -rf {} \;
```

### Monitoring de l'Espace Disque

Surveiller l'utilisation du disque :

```bash
# Vérifier l'espace utilisé
du -sh /mnt/contentstore2/ExtractionTravodoc/

# Lister les exports par taille
du -sh /mnt/contentstore2/ExtractionTravodoc/Export_* | sort -h
```

## Sécurité

### Avantages de cette Approche

1. **Pas de création de répertoire arbitraire** : Le code ne peut pas créer de répertoires n'importe où sur le système
2. **Configuration centralisée** : Le chemin est défini dans la configuration système
3. **Validation stricte** : Le code vérifie l'existence et les permissions avant de procéder
4. **Messages d'erreur clairs** : L'administrateur sait exactement quoi faire en cas de problème

### Bonnes Pratiques

1. **Utiliser un chemin dédié** : Ne pas utiliser un répertoire système ou partagé
2. **Limiter les permissions** : Seul le processus Alfresco doit avoir accès en écriture
3. **Surveillance régulière** : Vérifier périodiquement l'espace disque
4. **Archivage** : Déplacer les anciens exports vers un système d'archivage
5. **Backup** : Ne pas inclure ce répertoire dans les backups quotidiens (exports temporaires)

## Dépannage

### Vérifier la Configuration Spring

Vérifier que le bean est correctement configuré :

```bash
# Rechercher dans les logs de démarrage
grep "extraction.base.path" alfresco.log

# Vérifier que le bean est créé
grep "ExtractWebScript" alfresco.log
```

### Logs d'Export

Chaque export génère un fichier de log dans le répertoire personnel de l'utilisateur Alfresco :
- **Emplacement :** Répertoire personnel de l'utilisateur dans Alfresco Share
- **Format :** `Export_YYYYMMDD_HHmmss.log`
- **Contenu :** Détails de l'export, requêtes, documents extraits, erreurs

## Support

En cas de problème, fournir les informations suivantes :

1. Configuration de `extraction.base.path` dans `alfresco-global.properties`
2. Permissions du répertoire (`ls -ld <chemin>`)
3. Logs d'erreur de l'application
4. Fichier de log de l'export (si disponible)
5. Version d'Alfresco ACS
