function main() {
    // Set default values for the form
    model.defaultMaxDocs = 40000;
    model.defaultPath = "/mnt/contentstore2/ExtractionTravodoc";

    // Available mimetypes for the dropdown (can be extended)
    model.availableMimetypes = [
        { value: "application/pdf", label: "PDF" },
        { value: "application/msword", label: "Word (.doc)" },
        { value: "application/vnd.openxmlformats-officedocument.wordprocessingml.document", label: "Word (.docx)" },
        { value: "application/vnd.ms-excel", label: "Excel (.xls)" },
        { value: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", label: "Excel (.xlsx)" },
        { value: "application/vnd.ms-powerpoint", label: "PowerPoint (.ppt)" },
        { value: "application/vnd.openxmlformats-officedocument.presentationml.presentation", label: "PowerPoint (.pptx)" },
        { value: "image/jpeg", label: "Images JPEG" },
        { value: "image/png", label: "Images PNG" },
        { value: "text/plain", label: "Fichiers texte" },
        { value: "text/html", label: "Fichiers HTML" }
    ];
}

main();
