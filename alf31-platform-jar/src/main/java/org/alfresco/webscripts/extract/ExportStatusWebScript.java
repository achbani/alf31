package org.alfresco.webscripts.extract;

import java.util.HashMap;
import java.util.Map;

import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

/**
 * WebScript to poll the status of an export job.
 */
public class ExportStatusWebScript extends DeclarativeWebScript {

    private ExportStatusService exportStatusService;

    @Override
    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
        Map<String, Object> model = new HashMap<>();

        // Disable caching for real-time updates
        cache.setNeverCache(true);

        String jobId = req.getParameter("jobId");

        if (jobId == null || jobId.trim().isEmpty()) {
            status.setCode(400);
            model.put("error", "Missing jobId parameter");
            return model;
        }

        ExportStatusService.ExportStatus exportStatus = exportStatusService.getStatus(jobId);

        if (exportStatus == null) {
            status.setCode(404);
            model.put("error", "Export job not found: " + jobId);
            return model;
        }

        // Build response model
        model.put("jobId", exportStatus.getJobId());
        model.put("status", exportStatus.getStatus());
        model.put("maxDocs", exportStatus.getMaxDocs());
        model.put("extractedCount", exportStatus.getExtractedCount());
        model.put("progress", exportStatus.getProgressPercentage());
        model.put("message", exportStatus.getMessage());
        model.put("keywords", exportStatus.getKeywords());
        model.put("mimetype", exportStatus.getMimetype());
        model.put("extractionPath", exportStatus.getExtractionPath());
        model.put("startTime", exportStatus.getStartTime());
        model.put("endTime", exportStatus.getEndTime());
        model.put("duration", exportStatus.getDurationMs());

        return model;
    }

    // Spring setter
    public void setExportStatusService(ExportStatusService exportStatusService) {
        this.exportStatusService = exportStatusService;
    }
}
