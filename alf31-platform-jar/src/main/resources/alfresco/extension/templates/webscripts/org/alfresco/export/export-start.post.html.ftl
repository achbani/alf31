<html>
<head>
    <title>Export de documents - Résultat</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            margin: 20px;
            background-color: #f5f5f5;
        }
        .container {
            max-width: 800px;
            margin: 50px auto;
            background-color: white;
            padding: 30px;
            border-radius: 8px;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
        }
        .header {
            border-bottom: 3px solid #007bff;
            padding-bottom: 15px;
            margin-bottom: 25px;
        }
        .header h1 {
            margin: 0;
            color: #333;
        }
        .status-box {
            padding: 20px;
            border-radius: 4px;
            margin: 15px 0;
        }
        .status-completed {
            background-color: #d1e7dd;
            border-left: 4px solid #198754;
        }
        .status-failed {
            background-color: #f8d7da;
            border-left: 4px solid #dc3545;
        }
        .status-box h2 {
            margin: 0 0 10px 0;
            color: #333;
        }
        .info-grid {
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 15px;
            margin: 20px 0;
        }
        .info-item {
            padding: 15px;
            background-color: #f8f9fa;
            border-radius: 4px;
        }
        .info-label {
            font-size: 12px;
            color: #6c757d;
            text-transform: uppercase;
            margin-bottom: 5px;
        }
        .info-value {
            font-size: 18px;
            font-weight: bold;
            color: #333;
        }
        .info-item-full {
            grid-column: 1 / -1;
            padding: 15px;
            background-color: #f8f9fa;
            border-radius: 4px;
        }
        .actions {
            margin-top: 20px;
            text-align: center;
        }
        .btn {
            padding: 12px 24px;
            border-radius: 4px;
            border: none;
            cursor: pointer;
            font-size: 14px;
            text-decoration: none;
            display: inline-block;
            margin: 5px;
        }
        .btn-primary {
            background-color: #007bff;
            color: white;
        }
        .btn-primary:hover {
            background-color: #0056b3;
        }
        .success-icon {
            color: #198754;
            font-size: 48px;
            margin-bottom: 10px;
        }
        .error-icon {
            color: #dc3545;
            font-size: 48px;
            margin-bottom: 10px;
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>Export de documents</h1>
        </div>

        <#if success?? && success>
            <div class="status-box status-completed">
                <div class="success-icon">✓</div>
                <h2>Export terminé avec succès</h2>
                <p>${message!"L'export s'est terminé avec succès."}</p>
            </div>

            <div class="info-grid">
                <div class="info-item">
                    <div class="info-label">Documents exportés</div>
                    <div class="info-value">${extractedCount?c}</div>
                </div>
                <div class="info-item">
                    <div class="info-label">Maximum autorisé</div>
                    <div class="info-value">${maxDocs?c}</div>
                </div>
                <#if keywords?? && keywords?has_content>
                <div class="info-item">
                    <div class="info-label">Mots-clés utilisés</div>
                    <div class="info-value" style="font-size: 14px;">${keywords}</div>
                </div>
                </#if>
                <#if mimetype?? && mimetype?has_content>
                <div class="info-item">
                    <div class="info-label">Type MIME</div>
                    <div class="info-value" style="font-size: 14px;">${mimetype}</div>
                </div>
                </#if>
                <#if exportPath?? && exportPath?has_content>
                <div class="info-item-full">
                    <div class="info-label">Chemin d'extraction</div>
                    <div class="info-value" style="font-size: 14px; word-break: break-all;">${exportPath}</div>
                </div>
                </#if>
            </div>

            <div class="actions">
                <a href="/alfresco/s/api/export/form" class="btn btn-primary">Nouvel export</a>
            </div>

        <#else>
            <div class="status-box status-failed">
                <div class="error-icon">✗</div>
                <h2>Erreur lors de l'export</h2>
                <p>${message!"Une erreur inconnue s'est produite lors de l'export."}</p>
            </div>

            <div class="actions">
                <a href="/alfresco/s/api/export/form" class="btn btn-primary">Retour au formulaire</a>
            </div>
        </#if>
    </div>
</body>
</html>
