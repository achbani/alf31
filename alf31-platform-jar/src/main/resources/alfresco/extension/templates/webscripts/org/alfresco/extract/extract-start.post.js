function main() {
    try {
        // Get parameters
        var maxDocs = parseInt(args.maxDocs) || 40000;
        var extractionPath = args.extractionPath || "/mnt/contentstore2/ExtractionTravodoc";

        // Log the parameters
        logger.log("Starting extraction with maxDocs: " + maxDocs + " and extractionPath: " + extractionPath);

        // Implement the extraction logic here
        // For example, you can call a Java-backed web script or perform the extraction directly in JavaScript

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