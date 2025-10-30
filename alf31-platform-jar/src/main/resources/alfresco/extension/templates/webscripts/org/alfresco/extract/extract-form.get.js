function main() {
    try {
        // Get parameters
        var maxDocs = parseInt(args.maxDocs) || 40000;
        var extractionPath = args.extractionPath || "/mnt/contentstore2/ExtractionTravodoc";

        // Get the extraction web script
        var extractScript = actions.create("extract-start");
        extractScript.parameters["maxDocs"] = maxDocs;
        extractScript.parameters["extractionPath"] = extractionPath;

        // Execute the extraction
        extractScript.execute();

        // Store success status in session
        var status = {
            success: true,
            message: "Extraction started successfully. Processing " + maxDocs + " documents to path " + extractionPath + "."
        };
        session.setValue("extract_status", jsonUtils.toJSONString(status));

    } catch (err) {
        // Store error status in session
        var status = {
            success: false,
            message: "Error starting extraction: " + err.message
        };
        session.setValue("extract_status", jsonUtils.toJSONString(status));
    }

    // Redirect back to the form
    status.code = 302;
    status.location = "/alfresco/s/gedaff/extract/form";
}