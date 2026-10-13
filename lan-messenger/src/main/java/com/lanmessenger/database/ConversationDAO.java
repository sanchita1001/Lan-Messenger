package com.lanmessenger.database;

import com.lanmessenger.models.Conversation;
import com.lanmessenger.models.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Conversation operations
 */
public class ConversationDAO {

    /**
     * Create a new conversation
     */
    public int createConversation(Conversation.Type type, String name) {
        String sql = "INSERT INTO conversations (conversation_type, conversation_name) VALUES (?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, type.name());
            stmt.setString(2, name);

            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        return generatedKeys.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[ConversationDAO] Error creating conversation: " + e.getMessage());
        }

        return -1;
    }

    /**
     * Add a member to a conversation
     */
    public boolean addMember(int conversationId, int userId) {
        String sql = "INSERT INTO conversation_members (conversation_id, user_id) VALUES (?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, conversationId);
            stmt.setInt(2, userId);

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("[ConversationDAO] Error adding member: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get all conversations for a user
     */
    public List<Conversation> getConversationsForUser(int userId) {
        String sql = "SELECT c.conversation_id, c.conversation_type, c.conversation_name, c.created_at " +
                    "FROM conversations c " +
                    "INNER JOIN conversation_members cm ON c.conversation_id = cm.conversation_id " +
                    "WHERE cm.user_id = ? " +
                    "ORDER BY c.created_at DESC";

        List<Conversation> conversations = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Conversation.Type type = Conversation.Type.valueOf(rs.getString("conversation_type"));
                Conversation conv = new Conversation(
                        rs.getInt("conversation_id"),
                        type,
                        rs.getString("conversation_name"),
                        rs.getTimestamp("created_at")
                );

                // Load members for this conversation
                conv.setMembers(getConversationMembers(conv.getConversationId()));
                conversations.add(conv);
            }
        } catch (SQLException e) {
            System.err.println("[ConversationDAO] Error getting conversations: " + e.getMessage());
        }

        return conversations;
    }

    /**
     * Get all members of a conversation
     */
    public List<User> getConversationMembers(int conversationId) {
        String sql = "SELECT u.user_id, u.username, u.last_seen " +
                    "FROM users u " +
                    "INNER JOIN conversation_members cm ON u.user_id = cm.user_id " +
                    "WHERE cm.conversation_id = ?";

        List<User> members = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, conversationId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                User user = new User();
                user.setUserId(rs.getInt("user_id"));
                user.setUsername(rs.getString("username"));
                user.setLastSeen(rs.getTimestamp("last_seen"));
                members.add(user);
            }
        } catch (SQLException e) {
            System.err.println("[ConversationDAO] Error getting members: " + e.getMessage());
        }

        return members;
    }

    /**
     * Check if user is member of conversation
     */
    public boolean isUserMemberOfConversation(int userId, int conversationId) {
        String sql = "SELECT COUNT(*) FROM conversation_members " +
                    "WHERE conversation_id = ? AND user_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, conversationId);
            stmt.setInt(2, userId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("[ConversationDAO] Error checking membership: " + e.getMessage());
        }

        return false;
    }

    /**
     * Find private conversation between two users
     */
    public Integer findPrivateConversation(int userId1, int userId2) {
        String sql = "SELECT c.conversation_id " +
                    "FROM conversations c " +
                    "WHERE c.conversation_type = 'PRIVATE' " +
                    "AND c.conversation_id IN (" +
                    "    SELECT conversation_id FROM conversation_members WHERE user_id = ?" +
                    ") " +
                    "AND c.conversation_id IN (" +
                    "    SELECT conversation_id FROM conversation_members WHERE user_id = ?" +
                    ") " +
                    "AND (" +
                    "    SELECT COUNT(*) FROM conversation_members WHERE conversation_id = c.conversation_id" +
                    ") = 2";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId1);
            stmt.setInt(2, userId2);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("conversation_id");
            }
        } catch (SQLException e) {
            System.err.println("[ConversationDAO] Error finding private conversation: " + e.getMessage());
        }

        return null;
    }

    /**
     * Create or get private conversation between two users
     */
    public int createOrGetPrivateConversation(int userId1, int userId2) {
        // Check if conversation already exists
        Integer existingConvId = findPrivateConversation(userId1, userId2);
        if (existingConvId != null) {
            return existingConvId;
        }

        // Create new private conversation
        int convId = createConversation(Conversation.Type.PRIVATE, null);
        if (convId > 0) {
            addMember(convId, userId1);
            addMember(convId, userId2);
        }

        return convId;
    }
}
