<html>
<head>
    <title>Outil d'Export de Documents</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            margin: 20px;
            background-color: #f5f5f5;
        }
        .container {
            max-width: 700px;
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
        .form-group {
            margin-bottom: 20px;
        }
        label {
            display: block;
            margin-bottom: 5px;
            font-weight: bold;
            color: #555;
        }
        input[type="number"], input[type="text"] {
            width: 100%;
            padding: 8px;
            border: 1px solid #ddd;
            border-radius: 4px;
            box-sizing: border-box;
        }
        select {
            width: 100%;
            padding: 8px;
            border: 1px solid #ddd;
            border-radius: 4px;
            box-sizing: border-box;
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
            width: 100%;
        }
        button:hover {
            background-color: #0056b3;
        }
        .message {
            margin-top: 20px;
            padding: 15px;
            border-radius: 4px;
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
        .required {
            color: red;
        }
    </style>
</head>
<body>
    <div class="container">
        <h1>Export de documents</h1>
        <form action="/alfresco/s/api/export/start" method="POST">

            <!-- Champ 1 : Recherche par mots-clés -->
            <div class="form-group">
                <label for="keywords">Mots-clés de recherche :</label>
                <input type="text" id="keywords" name="keywords" />
                <small>Recherche dans le nom, titre et description des documents. Laissez vide pour tous les documents.</small>
            </div>

            <!-- Champ 2 : Type MIME (sélection unique) -->
            <div class="form-group">
                <label for="mimetype">Type de document :</label>
                <select id="mimetype" name="mimetype">
                    <option value="">inconnu</option>
                    <option value="application/pdf">PDF (.pdf)</option>
                    <option value="application/msword">Word (.doc)</option>
                    <option value="application/vnd.openxmlformats-officedocument.wordprocessingml.document">Word (.docx)</option>
                    <option value="application/vnd.ms-excel">Excel (.xls)</option>
                    <option value="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet">Excel (.xlsx)</option>
                    <option value="application/vnd.ms-powerpoint">PowerPoint (.ppt)</option>
                    <option value="application/vnd.openxmlformats-officedocument.presentationml.presentation">PowerPoint (.pptx)</option>
                    <option value="image/jpeg">Images JPEG</option>
                    <option value="image/png">Images PNG</option>
                    <option value="text/plain">Fichiers texte</option>
                    <option value="text/html">Fichiers HTML</option>
                </select>
                <small>Sélectionnez un type de document ou laissez "inconnu" pour exporter tous les formats.</small>
            </div>

            <!-- Champ 3 : Nombre maximum de documents -->
            <div class="form-group">
                <label for="maxDocs">Nombre maximum de documents : <span class="required">*</span></label>
                <input type="number" id="maxDocs" name="maxDocs"
                       value="250" min="1" max="100000" required />
                <small>Nombre maximum de documents à exporter (entre 1 et 100 000).</small>
            </div>

            <div class="info" style="margin-bottom: 20px; padding: 10px; background-color: #d1ecf1; color: #0c5460; border-radius: 4px; font-size: 14px;">
                <strong>Note :</strong> Le chemin d'export est configuré par l'administrateur système dans alfresco-global.properties.
            </div>

            <button type="submit">Lancer l'export</button>
        </form>
    </div>
</body>
</html>
