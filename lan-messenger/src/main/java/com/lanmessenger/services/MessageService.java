package com.lanmessenger.services;

import com.lanmessenger.database.ConversationDAO;
import com.lanmessenger.database.MessageDAO;
import com.lanmessenger.models.ChatMessage;
import com.lanmessenger.models.Conversation;
import com.lanmessenger.models.User;

import java.util.List;

/**
 * Service for message operations
 */
public class MessageService {
    private final MessageDAO messageDAO;
    private final ConversationDAO conversationDAO;

    public MessageService() {
        this.messageDAO = new MessageDAO();
        this.conversationDAO = new ConversationDAO();
    }

    /**
     * Send a message
     * 
     * @param conversationId The conversation ID
     * @param senderId The sender's user ID
     * @param text The message text
     * @return The created message with ID and timestamp
     */
    public ChatMessage sendMessage(int conversationId, int senderId, String text) {
        // Validate authorization
        if (!conversationDAO.isUserMemberOfConversation(senderId, conversationId)) {
            System.err.println("[MessageService] User " + senderId + " not authorized for conversation " + conversationId);
            return null;
        }

        // Validate message text
        if (text == null || text.trim().isEmpty()) {
            System.err.println("[MessageService] Cannot send empty message");
            return null;
        }

        // Save message
        ChatMessage message = messageDAO.saveMessage(conversationId, senderId, text);
        
        if (message != null) {
            System.out.println("[MessageService] Message saved: ID=" + message.getMessageId());
        }

        return message;
    }

    /**
     * Get message history for a conversation
     */
    public List<ChatMessage> getMessageHistory(int conversationId, int limit, int offset) {
        return messageDAO.getMessageHistory(conversationId, limit, offset);
    }

    /**
     * Get recent messages for a conversation
     */
    public List<ChatMessage> getRecentMessages(int conversationId, int limit) {
        return messageDAO.getRecentMessages(conversationId, limit);
    }

    /**
     * Get all conversations for a user
     */
    public List<Conversation> getUserConversations(int userId) {
        return conversationDAO.getConversationsForUser(userId);
    }

    /**
     * Get conversation members
     */
    public List<User> getConversationMembers(int conversationId) {
        return conversationDAO.getConversationMembers(conversationId);
    }

    /**
     * Create a group conversation
     */
    public int createGroupConversation(String name, List<Integer> memberIds) {
        if (memberIds == null || memberIds.size() < 2) {
            System.err.println("[MessageService] Group must have at least 2 members");
            return -1;
        }

        int convId = conversationDAO.createConversation(Conversation.Type.GROUP, name);
        
        if (convId > 0) {
            for (int userId : memberIds) {
                conversationDAO.addMember(convId, userId);
            }
            System.out.println("[MessageService] Group conversation created: ID=" + convId);
        }

        return convId;
    }

    /**
     * Create or get private conversation between two users
     */
    public int createOrGetPrivateConversation(int userId1, int userId2) {
        return conversationDAO.createOrGetPrivateConversation(userId1, userId2);
    }

    /**
     * Check if user is member of conversation
     */
    public boolean isUserMemberOfConversation(int userId, int conversationId) {
        return conversationDAO.isUserMemberOfConversation(userId, conversationId);
    }
}
