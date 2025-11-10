<html>
<head>
    <title>GAZODOC - Purge Massive</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            margin: 20px;
            background-color: #f5f5f5;
        }
        .container {
            max-width: 900px;
            margin: 0 auto;
            background-color: white;
            padding: 30px;
            border-radius: 8px;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
        }
        h1 {
            color: #dc3545;
            border-bottom: 3px solid #dc3545;
            padding-bottom: 10px;
        }
        h2 {
            color: #555;
            font-size: 18px;
            margin-top: 30px;
            border-left: 4px solid #dc3545;
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
        input[type="text"], select {
            width: 100%;
            padding: 10px;
            border: 1px solid #ddd;
            border-radius: 4px;
            box-sizing: border-box;
            font-size: 14px;
        }
        input[type="checkbox"] {
            width: 20px;
            height: 20px;
            margin-right: 10px;
            vertical-align: middle;
        }
        .checkbox-group {
            display: flex;
            align-items: center;
            padding: 10px;
            background-color: #f8f9fa;
            border-radius: 4px;
            margin-bottom: 10px;
        }
        .checkbox-group label {
            margin: 0;
            font-weight: normal;
        }
        small {
            display: block;
            margin-top: 5px;
            color: #666;
            font-size: 12px;
        }
        button {
            padding: 12px 30px;
            background-color: #dc3545;
            color: white;
            border: none;
            cursor: pointer;
            border-radius: 4px;
            font-size: 16px;
            font-weight: bold;
            width: 100%;
        }
        button:hover {
            background-color: #c82333;
        }
        .required {
            color: red;
        }
        .warning-box {
            background-color: #fff3cd;
            border-left: 4px solid #856404;
            padding: 15px;
            margin: 20px 0;
            border-radius: 4px;
        }
        .warning-box h3 {
            margin: 0 0 10px 0;
            color: #856404;
            font-size: 16px;
        }
        .warning-box p {
            margin: 5px 0;
            color: #856404;
            font-size: 14px;
        }
        .danger-box {
            background-color: #f8d7da;
            border-left: 4px solid #721c24;
            padding: 15px;
            margin: 20px 0;
            border-radius: 4px;
        }
        .danger-box h3 {
            margin: 0 0 10px 0;
            color: #721c24;
            font-size: 16px;
        }
        .danger-box p {
            margin: 5px 0;
            color: #721c24;
            font-size: 14px;
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
        <h1>Purge Massive de Documents GAZODOC</h1>

        <div class="danger-box">
            <h3>ATTENTION - Opération IRRÉVERSIBLE</h3>
            <p><strong>Cette opération supprime définitivement les documents de GAZODOC !</strong></p>
            <p>Assurez-vous d'avoir effectué une sauvegarde complète avant toute purge</p>
            <p>Export massif obligatoire avant purge (via /api/export/mass/form)</p>
        </div>

        <h2>Prérequis OBLIGATOIRES</h2>
        <div class="step">
            <strong>1.</strong> Backup complet de GAZODOC effectué
        </div>
        <div class="step">
            <strong>2.</strong> Export massif des documents réalisé (via /api/export/mass/form)
        </div>
        <div class="step">
            <strong>3.</strong> Fichier Excel avec colonne "Name" uploadé dans GAZODOC
        </div>
        <div class="step">
            <strong>4.</strong> NodeRef du fichier Excel récupéré
        </div>

        <h2>Formulaire de purge</h2>
        <form action="/alfresco/s/api/purge/mass/start" method="POST">

            <!-- Champ 1 : NodeRef du fichier Excel -->
            <div class="form-group">
                <label for="excelFileNodeRef">NodeRef du fichier Excel : <span class="required">*</span></label>
                <input type="text"
                       id="excelFileNodeRef"
                       name="excelFileNodeRef"
                       placeholder="workspace://SpacesStore/xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
                       required />
                <small>
                    Même fichier Excel que pour l'export
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
                <small>Par défaut : "Finances"</small>
            </div>

            <div class="info-box">
                <h3>Fonctionnement</h3>
                <p><strong>Suppression directe et immédiate</strong> de tous les documents listés dans l'Excel</p>
                <p><strong>Aucune validation</strong> - Tous les documents trouvés seront supprimés</p>
            </div>

            <div class="warning-box">
                <h3>Avant de valider</h3>
                <p>Vérifiez que le NodeRef est correct</p>
                <p>Vérifiez que le nom de l'onglet correspond</p>
                <p>Assurez-vous que le backup et l'export sont OK</p>
            </div>

            <button type="submit" id="submitButton">LANCER LA PURGE</button>
        </form>

        <div style="margin-top: 30px; padding: 15px; background-color: #e7f3ff; border-radius: 4px;">
            <h3 style="margin: 0 0 10px 0; color: #004085;">Déroulement de la purge</h3>
            <p style="margin: 5px 0; color: #004085; font-size: 14px;">
                1. Lecture du fichier Excel<br/>
                2. Recherche de chaque document dans GAZODOC<br/>
                3. <strong style="color: #dc3545;">Suppression immédiate et définitive de tous les documents trouvés</strong><br/>
                4. Génération du rapport CSV (purge_report.csv)<br/>
                5. Affichage du résumé (deleted, not found, errors)
            </p>
        </div>

        <div style="margin-top: 20px; text-align: center;">
            <a href="/alfresco" style="color: #007bff; text-decoration: none;">Retour à GAZODOC</a>
        </div>
    </div>
</body>
</html>
