package com.lanmessenger.network;

import org.json.JSONObject;

/**
 * Represents a protocol message exchanged between client and server
 * Uses JSON format with a type field to distinguish message types
 */
public class Message {
    
    // Client to Server Message Types
    public static final String REGISTER = "REGISTER";
    public static final String LOGIN = "LOGIN";
    public static final String SEND_MESSAGE = "SEND_MESSAGE";
    public static final String LOAD_HISTORY = "LOAD_HISTORY";
    public static final String CREATE_CONVERSATION = "CREATE_CONVERSATION";
    public static final String LIST_CONVERSATIONS = "LIST_CONVERSATIONS";
    public static final String SEND_FILE = "SEND_FILE";
    public static final String HEARTBEAT = "HEARTBEAT";
    public static final String LOGOUT = "LOGOUT";
    
    // Server to Client Message Types
    public static final String AUTH_SUCCESS = "AUTH_SUCCESS";
    public static final String AUTH_FAILURE = "AUTH_FAILURE";
    public static final String NEW_MESSAGE = "NEW_MESSAGE";
    public static final String HISTORY_RESPONSE = "HISTORY_RESPONSE";
    public static final String CONVERSATION_LIST = "CONVERSATION_LIST";
    public static final String USER_STATUS = "USER_STATUS";
    public static final String FILE_TRANSFER = "FILE_TRANSFER";
    public static final String ERROR = "ERROR";
    public static final String REGISTER_SUCCESS = "REGISTER_SUCCESS";
    public static final String REGISTER_FAILURE = "REGISTER_FAILURE";
    
    private final JSONObject data;

    /**
     * Create a new message from JSON object
     */
    public Message(JSONObject data) {
        this.data = data;
    }

    /**
     * Create a new message with a type
     */
    public Message(String type) {
        this.data = new JSONObject();
        this.data.put("type", type);
    }

    /**
     * Get the message type
     */
    public String getType() {
        return data.optString("type", "");
    }

    /**
     * Get the underlying JSON object
     */
    public JSONObject getData() {
        return data;
    }

    /**
     * Put a value in the message
     */
    public Message put(String key, Object value) {
        data.put(key, value);
        return this;
    }

    /**
     * Get a string value from the message
     */
    public String getString(String key) {
        return data.optString(key, "");
    }

    /**
     * Get an integer value from the message
     */
    public int getInt(String key) {
        return data.optInt(key, 0);
    }

    /**
     * Get a long value from the message
     */
    public long getLong(String key) {
        return data.optLong(key, 0L);
    }

    /**
     * Get a boolean value from the message
     */
    public boolean getBoolean(String key) {
        return data.optBoolean(key, false);
    }

    /**
     * Check if key exists
     */
    public boolean has(String key) {
        return data.has(key);
    }

    /**
     * Convert to JSON string
     */
    public String toJSONString() {
        return data.toString();
    }

    /**
     * Parse JSON string to Message
     */
    public static Message fromJSONString(String json) {
        return new Message(new JSONObject(json));
    }

    @Override
    public String toString() {
        return data.toString();
    }

    // Factory methods for common message types

    public static Message registerRequest(String username, String password) {
        return new Message(REGISTER)
                .put("username", username)
                .put("password", password);
    }

    public static Message loginRequest(String username, String password) {
        return new Message(LOGIN)
                .put("username", username)
                .put("password", password);
    }

    public static Message sendMessageRequest(int conversationId, String text) {
        return new Message(SEND_MESSAGE)
                .put("conversationId", conversationId)
                .put("text", text);
    }

    public static Message loadHistoryRequest(int conversationId, int limit, int offset) {
        return new Message(LOAD_HISTORY)
                .put("conversationId", conversationId)
                .put("limit", limit)
                .put("offset", offset);
    }

    public static Message listConversationsRequest() {
        return new Message(LIST_CONVERSATIONS);
    }

    public static Message authSuccess(int userId, String username) {
        return new Message(AUTH_SUCCESS)
                .put("userId", userId)
                .put("username", username);
    }

    public static Message authFailure(String error) {
        return new Message(AUTH_FAILURE)
                .put("error", error);
    }

    public static Message registerSuccess(String message) {
        return new Message(REGISTER_SUCCESS)
                .put("message", message);
    }

    public static Message registerFailure(String error) {
        return new Message(REGISTER_FAILURE)
                .put("error", error);
    }

    public static Message error(String errorMessage) {
        return new Message(ERROR)
                .put("error", errorMessage);
    }

    public static Message heartbeat() {
        return new Message(HEARTBEAT)
                .put("timestamp", System.currentTimeMillis());
    }
}
