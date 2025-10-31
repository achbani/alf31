package org.alfresco.webscripts.extract;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service to track the status of document extraction jobs.
 * Stores job status in memory for polling by the UI.
 */
public class ExportStatusService implements Serializable {

    private static final long serialVersionUID = 1L;

    // Store export statuses by job ID
    private final Map<String, ExportStatus> exportStatuses = new ConcurrentHashMap<>();

    // Maximum time to keep completed jobs in memory (30 minutes)
    private static final long MAX_RETENTION_MS = 30 * 60 * 1000;

    /**
     * Create a new export job and return its ID.
     */
    public String createExportJob(String username, int maxDocs, String keywords, String mimetype) {
        String jobId = generateJobId();
        ExportStatus status = new ExportStatus();
        status.setJobId(jobId);
        status.setUsername(username);
        status.setStatus("RUNNING");
        status.setMaxDocs(maxDocs);
        status.setExtractedCount(0);
        status.setKeywords(keywords);
        status.setMimetype(mimetype);
        status.setStartTime(new Date());
        status.setMessage("Export en cours d'initialisation...");

        exportStatuses.put(jobId, status);
        return jobId;
    }

    /**
     * Update the status of an export job.
     */
    public void updateStatus(String jobId, String status, int extractedCount, String message) {
        ExportStatus exportStatus = exportStatuses.get(jobId);
        if (exportStatus != null) {
            exportStatus.setStatus(status);
            exportStatus.setExtractedCount(extractedCount);
            exportStatus.setMessage(message);

            if ("COMPLETED".equals(status) || "FAILED".equals(status)) {
                exportStatus.setEndTime(new Date());
            }
        }
    }

    /**
     * Update extraction path.
     */
    public void updateExtractionPath(String jobId, String extractionPath) {
        ExportStatus exportStatus = exportStatuses.get(jobId);
        if (exportStatus != null) {
            exportStatus.setExtractionPath(extractionPath);
        }
    }

    /**
     * Get the status of an export job.
     */
    public ExportStatus getStatus(String jobId) {
        return exportStatuses.get(jobId);
    }

    /**
     * Remove old completed jobs from memory.
     */
    public void cleanupOldJobs() {
        long now = System.currentTimeMillis();
        exportStatuses.entrySet().removeIf(entry -> {
            ExportStatus status = entry.getValue();
            if (status.getEndTime() != null) {
                long age = now - status.getEndTime().getTime();
                return age > MAX_RETENTION_MS;
            }
            return false;
        });
    }

    /**
     * Generate a unique job ID.
     */
    private String generateJobId() {
        return "export_" + System.currentTimeMillis() + "_" +
               Thread.currentThread().getId();
    }

    /**
     * Inner class to hold export status information.
     */
    public static class ExportStatus implements Serializable {
        private static final long serialVersionUID = 1L;

        private String jobId;
        private String username;
        private String status; // RUNNING, COMPLETED, FAILED
        private int maxDocs;
        private int extractedCount;
        private String keywords;
        private String mimetype;
        private String extractionPath;
        private String message;
        private Date startTime;
        private Date endTime;

        // Getters and setters
        public String getJobId() { return jobId; }
        public void setJobId(String jobId) { this.jobId = jobId; }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public int getMaxDocs() { return maxDocs; }
        public void setMaxDocs(int maxDocs) { this.maxDocs = maxDocs; }

        public int getExtractedCount() { return extractedCount; }
        public void setExtractedCount(int extractedCount) { this.extractedCount = extractedCount; }

        public String getKeywords() { return keywords; }
        public void setKeywords(String keywords) { this.keywords = keywords; }

        public String getMimetype() { return mimetype; }
        public void setMimetype(String mimetype) { this.mimetype = mimetype; }

        public String getExtractionPath() { return extractionPath; }
        public void setExtractionPath(String extractionPath) { this.extractionPath = extractionPath; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public Date getStartTime() { return startTime; }
        public void setStartTime(Date startTime) { this.startTime = startTime; }

        public Date getEndTime() { return endTime; }
        public void setEndTime(Date endTime) { this.endTime = endTime; }

        public long getDurationMs() {
            if (startTime == null) return 0;
            Date end = endTime != null ? endTime : new Date();
            return end.getTime() - startTime.getTime();
        }

        public int getProgressPercentage() {
            if (maxDocs == 0) return 0;
            return Math.min(100, (extractedCount * 100) / maxDocs);
        }
    }
}
