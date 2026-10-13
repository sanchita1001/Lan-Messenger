package com.lanmessenger.models;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Model class representing a conversation (private or group)
 */
public class Conversation {
    public enum Type {
        PRIVATE, GROUP
    }

    private int conversationId;
    private Type conversationType;
    private String conversationName;
    private Timestamp createdAt;
    private List<User> members;

    public Conversation() {
        this.members = new ArrayList<>();
    }

    public Conversation(int conversationId, Type conversationType, String conversationName, Timestamp createdAt) {
        this.conversationId = conversationId;
        this.conversationType = conversationType;
        this.conversationName = conversationName;
        this.createdAt = createdAt;
        this.members = new ArrayList<>();
    }

    // Getters and Setters
    public int getConversationId() {
        return conversationId;
    }

    public void setConversationId(int conversationId) {
        this.conversationId = conversationId;
    }

    public Type getConversationType() {
        return conversationType;
    }

    public void setConversationType(Type conversationType) {
        this.conversationType = conversationType;
    }

    public String getConversationName() {
        return conversationName;
    }

    public void setConversationName(String conversationName) {
        this.conversationName = conversationName;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public List<User> getMembers() {
        return members;
    }

    public void setMembers(List<User> members) {
        this.members = members;
    }

    public void addMember(User user) {
        this.members.add(user);
    }

    /**
     * Get display name for the conversation
     * For private chats, returns the other user's name
     * For group chats, returns the conversation name
     */
    public String getDisplayName(int currentUserId) {
        if (conversationType == Type.GROUP) {
            return conversationName != null ? conversationName : "Group Chat";
        } else {
            // Private conversation - return the other user's name
            for (User member : members) {
                if (member.getUserId() != currentUserId) {
                    return member.getUsername();
                }
            }
            return "Private Chat";
        }
    }

    @Override
    public String toString() {
        return "Conversation{" +
                "conversationId=" + conversationId +
                ", conversationType=" + conversationType +
                ", conversationName='" + conversationName + '\'' +
                ", members=" + members.size() +
                '}';
    }
}
