<html>
<head>
    <title>Export de documents - Progression</title>
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
        .progress-container {
            margin: 20px 0;
        }
        .progress-bar {
            width: 100%;
            height: 30px;
            background-color: #e9ecef;
            border-radius: 15px;
            overflow: hidden;
            position: relative;
        }
        .progress-fill {
            height: 100%;
            background: linear-gradient(90deg, #007bff, #0056b3);
            transition: width 0.3s ease;
            display: flex;
            align-items: center;
            justify-content: center;
            color: white;
            font-weight: bold;
        }
        .status-box {
            padding: 15px;
            border-radius: 4px;
            margin: 15px 0;
        }
        .status-running {
            background-color: #cfe2ff;
            border-left: 4px solid #007bff;
        }
        .status-completed {
            background-color: #d1e7dd;
            border-left: 4px solid #198754;
        }
        .status-failed {
            background-color: #f8d7da;
            border-left: 4px solid #dc3545;
        }
        .info-grid {
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 15px;
            margin: 20px 0;
        }
        .info-item {
            padding: 10px;
            background-color: #f8f9fa;
            border-radius: 4px;
        }
        .info-label {
            font-size: 12px;
            color: #6c757d;
            text-transform: uppercase;
        }
        .info-value {
            font-size: 18px;
            font-weight: bold;
            color: #333;
            margin-top: 5px;
        }
        .spinner {
            border: 3px solid #f3f3f3;
            border-top: 3px solid #007bff;
            border-radius: 50%;
            width: 30px;
            height: 30px;
            animation: spin 1s linear infinite;
            display: inline-block;
            margin-right: 10px;
            vertical-align: middle;
        }
        @keyframes spin {
            0% { transform: rotate(0deg); }
            100% { transform: rotate(360deg); }
        }
        .actions {
            margin-top: 20px;
            text-align: center;
        }
        .btn {
            padding: 10px 20px;
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
        .btn-secondary {
            background-color: #6c757d;
            color: white;
        }
        .btn-secondary:hover {
            background-color: #5a6268;
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>Export de Documents</h1>
        </div>

        <#if success?? && success>
            <div id="statusContainer">
                <div class="status-box status-running" id="statusBox">
                    <div class="spinner" id="spinner"></div>
                    <span id="statusMessage">Démarrage de l'export...</span>
                </div>

                <div class="progress-container">
                    <div class="progress-bar">
                        <div class="progress-fill" id="progressBar" style="width: 0%">
                            <span id="progressText">0%</span>
                        </div>
                    </div>
                </div>

                <div class="info-grid">
                    <div class="info-item">
                        <div class="info-label">Documents extraits</div>
                        <div class="info-value" id="extractedCount">0</div>
                    </div>
                    <div class="info-item">
                        <div class="info-label">Maximum</div>
                        <div class="info-value" id="maxDocs">-</div>
                    </div>
                    <div class="info-item">
                        <div class="info-label">Durée</div>
                        <div class="info-value" id="duration">0s</div>
                    </div>
                    <div class="info-item">
                        <div class="info-label">Statut</div>
                        <div class="info-value" id="statusText">EN COURS</div>
                    </div>
                </div>

                <div class="info-item" id="pathContainer" style="display:none; margin-top: 15px;">
                    <div class="info-label">Chemin d'extraction</div>
                    <div class="info-value" id="extractionPath" style="font-size: 14px; word-break: break-all;">-</div>
                </div>

                <div class="actions">
                    <a href="/alfresco/s/gedaff/extract/form" class="btn btn-secondary">Retour au formulaire</a>
                </div>
            </div>

            <script>
                var jobId = "${jobId!""}";
                var pollingInterval = null;
                var statusCheckCount = 0;
                var maxStatusChecks = 600; // 10 minutes max (600 * 1 second)

                function formatDuration(ms) {
                    var seconds = Math.floor(ms / 1000);
                    var minutes = Math.floor(seconds / 60);
                    var hours = Math.floor(minutes / 60);

                    if (hours > 0) {
                        return hours + "h " + (minutes % 60) + "m";
                    } else if (minutes > 0) {
                        return minutes + "m " + (seconds % 60) + "s";
                    } else {
                        return seconds + "s";
                    }
                }

                function updateStatus() {
                    if (statusCheckCount >= maxStatusChecks) {
                        clearInterval(pollingInterval);
                        document.getElementById('statusMessage').textContent =
                            "Délai d'attente dépassé. L'export continue en arrière-plan.";
                        return;
                    }

                    statusCheckCount++;

                    fetch('/alfresco/s/gedaff/export/status?jobId=' + jobId)
                        .then(response => response.json())
                        .then(data => {
                            // Update progress bar
                            var progress = data.progress || 0;
                            document.getElementById('progressBar').style.width = progress + '%';
                            document.getElementById('progressText').textContent = progress + '%';

                            // Update counts
                            document.getElementById('extractedCount').textContent =
                                (data.extractedCount || 0).toLocaleString();
                            document.getElementById('maxDocs').textContent =
                                (data.maxDocs || 0).toLocaleString();

                            // Update duration
                            document.getElementById('duration').textContent =
                                formatDuration(data.duration || 0);

                            // Update status message
                            document.getElementById('statusMessage').textContent = data.message || '';
                            document.getElementById('statusText').textContent = data.status || 'EN COURS';

                            // Update extraction path if available
                            if (data.extractionPath) {
                                document.getElementById('extractionPath').textContent = data.extractionPath;
                                document.getElementById('pathContainer').style.display = 'block';
                            }

                            // Update status box style
                            var statusBox = document.getElementById('statusBox');
                            var spinner = document.getElementById('spinner');

                            if (data.status === 'COMPLETED') {
                                statusBox.className = 'status-box status-completed';
                                spinner.style.display = 'none';
                                clearInterval(pollingInterval);

                                // Show success actions
                                document.querySelector('.actions').innerHTML =
                                    '<a href="/alfresco/s/gedaff/extract/form" class="btn btn-primary">Nouveau export</a>';

                            } else if (data.status === 'FAILED') {
                                statusBox.className = 'status-box status-failed';
                                spinner.style.display = 'none';
                                clearInterval(pollingInterval);

                                // Show retry actions
                                document.querySelector('.actions').innerHTML =
                                    '<a href="/alfresco/s/gedaff/extract/form" class="btn btn-primary">Réessayer</a>';
                            }
                        })
                        .catch(error => {
                            console.error('Error fetching status:', error);
                        });
                }

                // Start polling immediately
                updateStatus();
                pollingInterval = setInterval(updateStatus, 1000); // Poll every 1 second
            </script>

        <#else>
            <div class="status-box status-failed">
                <h2>✗ Erreur</h2>
                <p>${message!"Une erreur inconnue s'est produite"}</p>
            </div>
            <div class="actions">
                <a href="/alfresco/s/gedaff/extract/form" class="btn btn-primary">Retour au formulaire</a>
            </div>
        </#if>
    </div>
</body>
</html>
