package com.lanmessenger.services;

import com.lanmessenger.database.FileDAO;
import com.lanmessenger.models.FileMetadata;
import com.lanmessenger.utils.ConfigUtil;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * Service for file transfer operations
 */
public class FileService {
    private final FileDAO fileDAO;
    private final String uploadDir;
    private final long maxFileSize;

    public FileService() {
        this.fileDAO = new FileDAO();
        this.uploadDir = ConfigUtil.getFileUploadDir();
        this.maxFileSize = ConfigUtil.getMaxFileSize();
        
        // Create upload directory if it doesn't exist
        createUploadDirectory();
    }

    /**
     * Create upload directory
     */
    private void createUploadDirectory() {
        try {
            Path dirPath = Paths.get(uploadDir);
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
                System.out.println("[FileService] Created upload directory: " + uploadDir);
            }
        } catch (IOException e) {
            System.err.println("[FileService] Error creating upload directory: " + e.getMessage());
        }
    }

    /**
     * Save a file and its metadata
     * 
     * @param messageId The associated message ID
     * @param originalFilename The original filename
     * @param fileData The file content as byte array
     * @return FileMetadata if successful, null otherwise
     */
    public FileMetadata saveFile(int messageId, String originalFilename, byte[] fileData) {
        // Validate file size
        if (fileData.length > maxFileSize) {
            System.err.println("[FileService] File too large: " + fileData.length + " bytes");
            return null;
        }

        // Sanitize filename and create storage path
        String sanitizedFilename = sanitizeFilename(originalFilename);
        String storageFilename = UUID.randomUUID() + "_" + sanitizedFilename;
        String storagePath = uploadDir + File.separator + storageFilename;

        try {
            // Write file to disk
            Path filePath = Paths.get(storagePath);
            Files.write(filePath, fileData);
            System.out.println("[FileService] File saved: " + storagePath);

            // Save metadata to database
            FileMetadata metadata = fileDAO.saveFileMetadata(
                    messageId, 
                    originalFilename, 
                    storagePath, 
                    fileData.length
            );

            return metadata;

        } catch (IOException e) {
            System.err.println("[FileService] Error saving file: " + e.getMessage());
            return null;
        }
    }

    /**
     * Get file metadata
     */
    public FileMetadata getFileMetadata(int fileId) {
        return fileDAO.getFileMetadata(fileId);
    }

    /**
     * Get file metadata by message ID
     */
    public FileMetadata getFileMetadataByMessageId(int messageId) {
        return fileDAO.getFileMetadataByMessageId(messageId);
    }

    /**
     * Read file contents
     */
    public byte[] readFile(String storagePath) {
        try {
            Path filePath = Paths.get(storagePath);
            if (Files.exists(filePath)) {
                return Files.readAllBytes(filePath);
            } else {
                System.err.println("[FileService] File not found: " + storagePath);
                return null;
            }
        } catch (IOException e) {
            System.err.println("[FileService] Error reading file: " + e.getMessage());
            return null;
        }
    }

    /**
     * Sanitize filename to prevent directory traversal
     */
    private String sanitizeFilename(String filename) {
        if (filename == null) {
            return "file";
        }
        
        // Remove path separators and parent directory references
        String sanitized = filename.replaceAll("[/\\\\]", "_")
                                  .replaceAll("\\.\\.", "_");
        
        // Limit length
        if (sanitized.length() > 100) {
            String extension = "";
            int dotIndex = sanitized.lastIndexOf('.');
            if (dotIndex > 0) {
                extension = sanitized.substring(dotIndex);
            }
            sanitized = sanitized.substring(0, 100 - extension.length()) + extension;
        }
        
        return sanitized;
    }

    /**
     * Validate file extension
     */
    public boolean isValidFileExtension(String filename) {
        // For now, allow all extensions
        // Can be enhanced with allowlist from config
        return filename != null && !filename.trim().isEmpty();
    }
}
