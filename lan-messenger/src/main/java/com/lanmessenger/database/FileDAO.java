package com.lanmessenger.database;

import com.lanmessenger.models.FileMetadata;

import java.sql.*;

/**
 * Data Access Object for File operations
 */
public class FileDAO {

    /**
     * Save file metadata to database
     */
    public FileMetadata saveFileMetadata(int messageId, String originalFilename, 
                                        String storagePath, long fileSize) {
        String sql = "INSERT INTO files (message_id, original_filename, storage_path, file_size) " +
                    "VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, messageId);
            stmt.setString(2, originalFilename);
            stmt.setString(3, storagePath);
            stmt.setLong(4, fileSize);

            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int fileId = generatedKeys.getInt(1);
                        return getFileMetadata(fileId);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[FileDAO] Error saving file metadata: " + e.getMessage());
        }

        return null;
    }

    /**
     * Get file metadata by file ID
     */
    public FileMetadata getFileMetadata(int fileId) {
        String sql = "SELECT file_id, message_id, original_filename, storage_path, file_size, uploaded_at " +
                    "FROM files WHERE file_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, fileId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return new FileMetadata(
                        rs.getInt("file_id"),
                        rs.getInt("message_id"),
                        rs.getString("original_filename"),
                        rs.getString("storage_path"),
                        rs.getLong("file_size"),
                        rs.getTimestamp("uploaded_at")
                );
            }
        } catch (SQLException e) {
            System.err.println("[FileDAO] Error getting file metadata: " + e.getMessage());
        }

        return null;
    }

    /**
     * Get file metadata by message ID
     */
    public FileMetadata getFileMetadataByMessageId(int messageId) {
        String sql = "SELECT file_id, message_id, original_filename, storage_path, file_size, uploaded_at " +
                    "FROM files WHERE message_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, messageId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return new FileMetadata(
                        rs.getInt("file_id"),
                        rs.getInt("message_id"),
                        rs.getString("original_filename"),
                        rs.getString("storage_path"),
                        rs.getLong("file_size"),
                        rs.getTimestamp("uploaded_at")
                );
            }
        } catch (SQLException e) {
            System.err.println("[FileDAO] Error getting file metadata by message: " + e.getMessage());
        }

        return null;
    }
}
