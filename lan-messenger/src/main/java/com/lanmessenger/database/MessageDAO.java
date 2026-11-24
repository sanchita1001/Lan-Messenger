package com.lanmessenger.database;

import com.lanmessenger.models.ChatMessage;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Message operations
 */
public class MessageDAO {

    /**
     * Save a new message to the database
     * 
     * @return The created message with generated ID and timestamp
     */
    public ChatMessage saveMessage(int conversationId, int senderId, String text) {
        String sql = "INSERT INTO messages (conversation_id, sender_id, message_text) VALUES (?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, conversationId);
            stmt.setInt(2, senderId);
            stmt.setString(3, text);

            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int messageId = generatedKeys.getInt(1);
                        
                        // Retrieve the complete message with timestamp
                        return getMessageById(messageId);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[MessageDAO] Error saving message: " + e.getMessage());
        }

        return null;
    }

    /**
     * Get a message by ID
     */
    public ChatMessage getMessageById(int messageId) {
        String sql = "SELECT m.message_id, m.conversation_id, m.sender_id, u.username as sender_name, " +
                    "m.message_text, m.sent_at " +
                    "FROM messages m " +
                    "INNER JOIN users u ON m.sender_id = u.user_id " +
                    "WHERE m.message_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, messageId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return new ChatMessage(
                        rs.getInt("message_id"),
                        rs.getInt("conversation_id"),
                        rs.getInt("sender_id"),
                        rs.getString("sender_name"),
                        rs.getString("message_text"),
                        rs.getTimestamp("sent_at")
                );
            }
        } catch (SQLException e) {
            System.err.println("[MessageDAO] Error getting message: " + e.getMessage());
        }

        return null;
    }

    /**
     * Get message history for a conversation
     * 
     * @param conversationId The conversation ID
     * @param limit Maximum number of messages to retrieve
     * @param offset Number of messages to skip (for pagination)
     * @return List of messages in chronological order (oldest first)
     */
    public List<ChatMessage> getMessageHistory(int conversationId, int limit, int offset) {
        String sql = "SELECT m.message_id, m.conversation_id, m.sender_id, u.username as sender_name, " +
                    "m.message_text, m.sent_at " +
                    "FROM messages m " +
                    "INNER JOIN users u ON m.sender_id = u.user_id " +
                    "WHERE m.conversation_id = ? " +
                    "ORDER BY m.sent_at ASC " +
                    "LIMIT ? OFFSET ?";

        List<ChatMessage> messages = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, conversationId);
            stmt.setInt(2, limit);
            stmt.setInt(3, offset);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                ChatMessage message = new ChatMessage(
                        rs.getInt("message_id"),
                        rs.getInt("conversation_id"),
                        rs.getInt("sender_id"),
                        rs.getString("sender_name"),
                        rs.getString("message_text"),
                        rs.getTimestamp("sent_at")
                );
                messages.add(message);
            }
        } catch (SQLException e) {
            System.err.println("[MessageDAO] Error getting message history: " + e.getMessage());
        }

        return messages;
    }
}
