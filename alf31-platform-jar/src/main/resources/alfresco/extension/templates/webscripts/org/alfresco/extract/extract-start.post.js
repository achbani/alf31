function main() {
    try {
        // Get parameters from the form
        var maxDocs = parseInt(args.maxDocs) || 40000;
        var extractionPath = args.extractionPath || "/mnt/contentstore2/ExtractionTravodoc";
        var keywords = args.keywords || "";

        // Handle mimetypes - can be array or single value
        var mimetypes = args.mimetypes;
        var mimetypeArray = [];

        if (mimetypes) {
            if (typeof mimetypes === "string") {
                mimetypeArray = [mimetypes];
            } else if (Array.isArray(mimetypes)) {
                mimetypeArray = mimetypes;
            } else {
                // Handle object case (when multiple values are selected)
                mimetypeArray = [mimetypes.toString()];
            }
        }

        // Validate extractionPath
        if (!extractionPath || extractionPath.trim() === "") {
            throw new Error("Le chemin d'extraction est obligatoire");
        }

        // Security check: prevent directory traversal
        if (extractionPath.indexOf("..") !== -1 || extractionPath.indexOf("~") !== -1) {
            throw new Error("Chemin d'extraction invalide - caractères interdits détectés");
        }

        // Validate maxDocs
        if (maxDocs < 1 || maxDocs > 100000) {
            throw new Error("Le nombre de documents doit être entre 1 et 100000");
        }

        // Log the parameters
        logger.log("Starting extraction with parameters:");
        logger.log("  - maxDocs: " + maxDocs);
        logger.log("  - extractionPath: " + extractionPath);
        logger.log("  - keywords: " + keywords);
        logger.log("  - mimetypes: " + mimetypeArray.join(", "));

        // Store parameters in session for the Java WebScript
        session.setValue("maxDocs", maxDocs.toString());
        session.setValue("extractionPath", extractionPath);
        session.setValue("keywords", keywords.trim());
        session.setValue("mimetypes", mimetypeArray.join(","));

        // Build a summary message
        var summaryParts = [];
        summaryParts.push("Export lancé avec succès");

        if (keywords && keywords.trim() !== "") {
            summaryParts.push("Mots-clés: '" + keywords + "'");
        }

        if (mimetypeArray.length > 0) {
            summaryParts.push(mimetypeArray.length + " type(s) de fichier sélectionné(s)");
        }

        summaryParts.push("Maximum: " + maxDocs + " documents");

        // Store success status in session
        var status = {
            success: true,
            message: summaryParts.join(" | ")
        };
        session.setValue("extract_status", JSON.stringify(status));

    } catch (err) {
        // Store error status in session
        logger.error("Error starting extraction: " + err.message);
        var status = {
            success: false,
            message: "Erreur lors du lancement de l'export: " + err.message
        };
        session.setValue("extract_status", JSON.stringify(status));
    }

    // Redirect back to the form
    status.code = 302;
    status.location = "/alfresco/s/gedaff/extract/form";
}

main();
