package com.lanmessenger.models;

import java.sql.Timestamp;

/**
 * Model class representing a chat message
 */
public class ChatMessage {
    private int messageId;
    private int conversationId;
    private int senderId;
    private String senderName;
    private String messageText;
    private Timestamp sentAt;
    private boolean hasFile;
    private Integer fileId;

    public ChatMessage() {
    }

    public ChatMessage(int messageId, int conversationId, int senderId, String senderName, 
                      String messageText, Timestamp sentAt) {
        this.messageId = messageId;
        this.conversationId = conversationId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.messageText = messageText;
        this.sentAt = sentAt;
        this.hasFile = false;
    }

    // Getters and Setters
    public int getMessageId() {
        return messageId;
    }

    public void setMessageId(int messageId) {
        this.messageId = messageId;
    }

    public int getConversationId() {
        return conversationId;
    }

    public void setConversationId(int conversationId) {
        this.conversationId = conversationId;
    }

    public int getSenderId() {
        return senderId;
    }

    public void setSenderId(int senderId) {
        this.senderId = senderId;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getMessageText() {
        return messageText;
    }

    public void setMessageText(String messageText) {
        this.messageText = messageText;
    }

    public Timestamp getSentAt() {
        return sentAt;
    }

    public void setSentAt(Timestamp sentAt) {
        this.sentAt = sentAt;
    }

    public boolean isHasFile() {
        return hasFile;
    }

    public void setHasFile(boolean hasFile) {
        this.hasFile = hasFile;
    }

    public Integer getFileId() {
        return fileId;
    }

    public void setFileId(Integer fileId) {
        this.fileId = fileId;
    }

    @Override
    public String toString() {
        return "ChatMessage{" +
                "messageId=" + messageId +
                ", conversationId=" + conversationId +
                ", senderName='" + senderName + '\'' +
                ", messageText='" + messageText + '\'' +
                ", sentAt=" + sentAt +
                '}';
    }
}
