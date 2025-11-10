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
        button.dry-run {
            background-color: #ffc107;
            color: #000;
        }
        button.dry-run:hover {
            background-color: #e0a800;
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
    <script>
        function updateButtonStyle() {
            var dryRun = document.getElementById('dryRun').checked;
            var button = document.getElementById('submitButton');
            if (dryRun) {
                button.className = 'dry-run';
                button.innerHTML = '🧪 Lancer la SIMULATION (DRY-RUN)';
            } else {
                button.className = '';
                button.innerHTML = '🔴 LANCER LA PURGE RÉELLE';
            }
        }
    </script>
</head>
<body>
    <div class="container">
        <h1>🗑️ Purge Massive de Documents GAZODOC</h1>

        <div class="danger-box">
            <h3>⚠️ ATTENTION - Opération IRRÉVERSIBLE</h3>
            <p><strong>Cette opération supprime définitivement les documents d'Alfresco !</strong></p>
            <p>✅ Assurez-vous d'avoir effectué une sauvegarde complète avant toute purge</p>
            <p>✅ Utilisez OBLIGATOIREMENT le mode DRY-RUN en premier lieu</p>
        </div>

        <h2>📁 Prérequis OBLIGATOIRES</h2>
        <div class="step">
            <strong>1.</strong> ✅ Backup complet d'Alfresco effectué
        </div>
        <div class="step">
            <strong>2.</strong> ✅ Export massif des documents réalisé (via /api/export/mass/form)
        </div>
        <div class="step">
            <strong>3.</strong> ✅ Fichier Excel avec colonne "Name" uploadé dans Alfresco
        </div>
        <div class="step">
            <strong>4.</strong> ✅ NodeRef du fichier Excel récupéré
        </div>

        <h2>📝 Formulaire de purge</h2>
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
                    📌 Même fichier Excel que pour l'export
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

            <h2>⚙️ Options de purge</h2>

            <!-- Option 1 : Mode DRY-RUN -->
            <div class="form-group">
                <div class="checkbox-group" style="background-color: #fff3cd;">
                    <input type="checkbox"
                           id="dryRun"
                           name="dryRun"
                           value="true"
                           checked
                           onchange="updateButtonStyle()" />
                    <label for="dryRun">
                        <strong>🧪 Mode SIMULATION (DRY-RUN)</strong> - Recommandé pour la première exécution
                    </label>
                </div>
                <small>
                    ✅ <strong>ACTIVÉ par défaut</strong> : Simule la purge sans supprimer les documents<br/>
                    ⚠️ Décochez pour effectuer une vraie suppression (IRRÉVERSIBLE)
                </small>
            </div>

            <!-- Option 2 : Auto-archivage -->
            <div class="form-group">
                <div class="checkbox-group">
                    <input type="checkbox"
                           id="autoArchive"
                           name="autoArchive"
                           value="true"
                           checked />
                    <label for="autoArchive">
                        <strong>📦 Auto-archivage</strong> - Archiver automatiquement les documents en état "VALIDE"
                    </label>
                </div>
                <small>
                    ✅ <strong>ACTIVÉ par défaut</strong> : Les documents avec état="REF" (VALIDE) seront archivés avant suppression<br/>
                    ❌ Décoché : Les documents non archivés seront bloqués
                </small>
            </div>

            <div class="info-box">
                <h3>ℹ️ Règles de validation automatique</h3>
                <p>✅ <strong>État du document :</strong> Doit être "ARCHIVE" (ou "REF" si auto-archivage activé)</p>
                <p>✅ <strong>Durée de conservation :</strong> 5 ans par défaut après archivage</p>
                <p>❌ Les documents bloqués ne seront PAS supprimés (voir rapport)</p>
            </div>

            <div class="warning-box">
                <h3>⚠️ Avant de valider</h3>
                <p>✅ Vérifiez que le NodeRef est correct</p>
                <p>✅ Vérifiez que le mode DRY-RUN est activé (première fois)</p>
                <p>✅ Assurez-vous que le backup est OK</p>
            </div>

            <button type="submit" id="submitButton">🧪 Lancer la SIMULATION (DRY-RUN)</button>
        </form>

        <div style="margin-top: 30px; padding: 15px; background-color: #e7f3ff; border-radius: 4px;">
            <h3 style="margin: 0 0 10px 0; color: #004085;">📋 Déroulement de la purge</h3>
            <p style="margin: 5px 0; color: #004085; font-size: 14px;">
                <strong>Mode DRY-RUN (simulation) :</strong><br/>
                1. Lecture du fichier Excel<br/>
                2. Recherche de chaque document<br/>
                3. Validation des règles métier (sans suppression)<br/>
                4. Génération du rapport CSV<br/>
                5. Affichage du résumé (0 suppression effectuée)
            </p>
            <p style="margin: 15px 0 5px 0; color: #004085; font-size: 14px;">
                <strong>Mode RÉEL (dryRun décoché) :</strong><br/>
                1-2. Idem simulation<br/>
                3. Auto-archivage si nécessaire<br/>
                4. <strong style="color: #dc3545;">Suppression définitive des documents</strong><br/>
                5. Génération du rapport CSV<br/>
                6. Affichage du résumé
            </p>
        </div>

        <div style="margin-top: 20px; text-align: center;">
            <a href="/alfresco" style="color: #007bff; text-decoration: none;">← Retour à Alfresco</a>
        </div>
    </div>

    <script>
        // Initialize button text
        updateButtonStyle();
    </script>
</body>
</html>
