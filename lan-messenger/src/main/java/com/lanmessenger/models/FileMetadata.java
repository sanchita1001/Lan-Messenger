package com.lanmessenger.models;

import java.sql.Timestamp;

/**
 * Model class representing file transfer metadata
 */
public class FileMetadata {
    private int fileId;
    private int messageId;
    private String originalFilename;
    private String storagePath;
    private long fileSize;
    private Timestamp uploadedAt;

    public FileMetadata() {
    }

    public FileMetadata(int fileId, int messageId, String originalFilename, 
                       String storagePath, long fileSize, Timestamp uploadedAt) {
        this.fileId = fileId;
        this.messageId = messageId;
        this.originalFilename = originalFilename;
        this.storagePath = storagePath;
        this.fileSize = fileSize;
        this.uploadedAt = uploadedAt;
    }

    // Getters and Setters
    public int getFileId() {
        return fileId;
    }

    public void setFileId(int fileId) {
        this.fileId = fileId;
    }

    public int getMessageId() {
        return messageId;
    }

    public void setMessageId(int messageId) {
        this.messageId = messageId;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public Timestamp getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(Timestamp uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    /**
     * Get human-readable file size
     */
    public String getFormattedFileSize() {
        if (fileSize < 1024) {
            return fileSize + " B";
        } else if (fileSize < 1024 * 1024) {
            return String.format("%.2f KB", fileSize / 1024.0);
        } else if (fileSize < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", fileSize / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", fileSize / (1024.0 * 1024.0 * 1024.0));
        }
    }

    @Override
    public String toString() {
        return "FileMetadata{" +
                "fileId=" + fileId +
                ", originalFilename='" + originalFilename + '\'' +
                ", fileSize=" + getFormattedFileSize() +
                '}';
    }
}
