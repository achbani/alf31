<html>
<head>
    <title>GAZODOC - Export Massif</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            margin: 20px;
            background-color: #f5f5f5;
        }
        .container {
            max-width: 800px;
            margin: 0 auto;
            background-color: white;
            padding: 30px;
            border-radius: 8px;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
        }
        h1 {
            color: #333;
            border-bottom: 3px solid #007bff;
            padding-bottom: 10px;
        }
        h2 {
            color: #555;
            font-size: 18px;
            margin-top: 30px;
            border-left: 4px solid #007bff;
            padding-left: 10px;
        }
        .form-group {
            margin-bottom: 20px;
        }
        label {
            display: block;
            margin-bottom: 5px;
            font-weight: bold;
            color: #555;
        }
        input[type="text"] {
            width: 100%;
            padding: 10px;
            border: 1px solid #ddd;
            border-radius: 4px;
            box-sizing: border-box;
            font-size: 14px;
        }
        small {
            display: block;
            margin-top: 5px;
            color: #666;
            font-size: 12px;
        }
        button {
            padding: 12px 30px;
            background-color: #007bff;
            color: white;
            border: none;
            cursor: pointer;
            border-radius: 4px;
            font-size: 16px;
            font-weight: bold;
        }
        button:hover {
            background-color: #0056b3;
        }
        .required {
            color: red;
        }
        .info-box {
            background-color: #d1ecf1;
            border-left: 4px solid #0c5460;
            padding: 15px;
            margin: 20px 0;
            border-radius: 4px;
        }
        .info-box h3 {
            margin: 0 0 10px 0;
            color: #0c5460;
            font-size: 16px;
        }
        .info-box p {
            margin: 5px 0;
            color: #0c5460;
            font-size: 14px;
        }
        .step {
            background-color: #f8f9fa;
            padding: 10px;
            margin: 10px 0;
            border-radius: 4px;
            border-left: 3px solid #6c757d;
        }
        .step strong {
            color: #495057;
        }
    </style>
</head>
<body>
    <div class="container">
        <h1>🗂️ Export Massif de Documents GAZODOC</h1>

        <div class="info-box">
            <h3>ℹ️ À propos de cet outil</h3>
            <p><strong>Fonction:</strong> Exporter en masse des documents depuis une liste Excel</p>
            <p><strong>Sortie:</strong> Fichiers + métadonnées CSV + manifest JSON</p>
            <p><strong>Emplacement:</strong> Configurable via export.base.path</p>
        </div>

        <h2>📁 Prérequis</h2>
        <div class="step">
            <strong>1.</strong> Préparez un fichier Excel avec la colonne "Name" contenant les identifiants Alfresco (cm:name)
        </div>
        <div class="step">
            <strong>2.</strong> Uploadez le fichier Excel dans Alfresco
        </div>
        <div class="step">
            <strong>3.</strong> Récupérez le NodeRef du fichier (clic droit → Voir les détails)
        </div>

        <h2>📝 Formulaire d'export</h2>
        <form action="/alfresco/s/api/export/mass/start" method="POST">

            <!-- Champ 1 : NodeRef du fichier Excel -->
            <div class="form-group">
                <label for="excelFileNodeRef">NodeRef du fichier Excel : <span class="required">*</span></label>
                <input type="text"
                       id="excelFileNodeRef"
                       name="excelFileNodeRef"
                       placeholder="workspace://SpacesStore/xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
                       required />
                <small>
                    📌 <strong>Comment obtenir le NodeRef ?</strong><br/>
                    1. Allez sur le fichier Excel dans Alfresco Share<br/>
                    2. Clic droit → "Voir les détails"<br/>
                    3. Copiez le NodeRef (format: workspace://SpacesStore/...)
                </small>
            </div>

            <!-- Champ 2 : Nom de l'onglet Excel -->
            <div class="form-group">
                <label for="sheetName">Nom de l'onglet Excel :</label>
                <input type="text"
                       id="sheetName"
                       name="sheetName"
                       value="Finances"
                       placeholder="Finances" />
                <small>
                    Par défaut : "Finances"<br/>
                    Modifiez si votre onglet a un autre nom (ex: "Documents", "Purge", etc.)
                </small>
            </div>

            <div class="info-box" style="background-color: #fff3cd; border-color: #856404;">
                <h3>⚠️ Avant de lancer l'export</h3>
                <p>✅ Vérifiez que le chemin d'export est configuré (export.base.path)</p>
                <p>✅ Assurez-vous que le serveur a les droits d'écriture sur le répertoire</p>
                <p>✅ Vérifiez que le NodeRef est correct</p>
                <p>✅ Vérifiez le nom de l'onglet Excel</p>
            </div>

            <button type="submit">🚀 Lancer l'export massif</button>
        </form>

        <div style="margin-top: 30px; padding: 15px; background-color: #e7f3ff; border-radius: 4px;">
            <h3 style="margin: 0 0 10px 0; color: #004085;">📋 Que se passe-t-il après ?</h3>
            <p style="margin: 5px 0; color: #004085; font-size: 14px;">
                1. Lecture du fichier Excel (colonne "Name")<br/>
                2. Recherche de chaque document dans Alfresco par cm:name<br/>
                3. Export des fichiers vers le répertoire configuré<br/>
                4. Export des métadonnées GAZODOC en CSV<br/>
                5. Génération d'un manifest JSON avec statistiques<br/>
                6. Affichage du résumé avec chemins d'export
            </p>
        </div>

        <div style="margin-top: 20px; text-align: center;">
            <a href="/alfresco" style="color: #007bff; text-decoration: none;">← Retour à Alfresco</a>
        </div>
    </div>
</body>
</html>
