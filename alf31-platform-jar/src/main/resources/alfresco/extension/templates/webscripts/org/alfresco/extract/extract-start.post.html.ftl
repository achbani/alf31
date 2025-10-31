<html>
<head>
    <title>Export de documents - Résultat</title>
    <meta http-equiv="refresh" content="3;url=/alfresco/s/gedaff/extract/form">
    <style>
        body {
            font-family: Arial, sans-serif;
            margin: 20px;
            background-color: #f5f5f5;
        }
        .container {
            max-width: 700px;
            margin: 50px auto;
            background-color: white;
            padding: 30px;
            border-radius: 8px;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
        }
        .message {
            padding: 20px;
            border-radius: 4px;
            margin-bottom: 20px;
        }
        .success {
            background-color: #d4edda;
            color: #155724;
            border: 1px solid #c3e6cb;
        }
        .error {
            background-color: #f8d7da;
            color: #721c24;
            border: 1px solid #f5c6cb;
        }
        .info {
            margin-top: 15px;
            padding: 10px;
            background-color: #d1ecf1;
            color: #0c5460;
            border-radius: 4px;
            font-size: 14px;
        }
        h2 {
            margin-top: 0;
        }
        a {
            color: #007bff;
            text-decoration: none;
        }
        a:hover {
            text-decoration: underline;
        }
    </style>
</head>
<body>
    <div class="container">
        <#if success?? && success>
            <div class="message success">
                <h2>✓ Export lancé avec succès</h2>
                <p><strong>${message!""}</strong></p>
                <#if extractedCount??>
                    <p>Nombre de documents extraits : <strong>${extractedCount}</strong></p>
                </#if>
            </div>
        <#else>
            <div class="message error">
                <h2>✗ Erreur lors de l'export</h2>
                <p>${message!"Une erreur inconnue s'est produite"}</p>
                <#if extractedCount??>
                    <p>Documents extraits avant l'erreur : ${extractedCount}</p>
                </#if>
            </div>
        </#if>

        <div class="info">
            <p>Vous allez être redirigé vers le formulaire dans 3 secondes...</p>
            <p>Ou cliquez ici : <a href="/alfresco/s/gedaff/extract/form">Retour au formulaire</a></p>
        </div>
    </div>
</body>
</html>
