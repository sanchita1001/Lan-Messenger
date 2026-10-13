package com.lanmessenger.server;

import com.lanmessenger.models.ChatMessage;
import com.lanmessenger.models.Conversation;
import com.lanmessenger.models.User;
import com.lanmessenger.network.Message;
import com.lanmessenger.network.MessageProtocol;
import com.lanmessenger.services.AuthService;
import com.lanmessenger.services.MessageService;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.Socket;
import java.util.List;

/**
 * Handles communication with a single client connection
 * Runs in its own thread
 */
public class ClientHandler implements Runnable {
    private final Socket socket;
    private final MessageProtocol protocol;
    private final OnlineUserRegistry userRegistry;
    private final AuthService authService;
    private final MessageService messageService;
    
    private Integer authenticatedUserId = null;
    private String authenticatedUsername = null;
    private volatile boolean running = true;

    public ClientHandler(Socket socket, OnlineUserRegistry userRegistry) throws IOException {
        this.socket = socket;
        this.protocol = new MessageProtocol(socket);
        this.userRegistry = userRegistry;
        this.authService = new AuthService();
        this.messageService = new MessageService();
    }

    @Override
    public void run() {
        System.out.println("[ClientHandler] New client connected from " + socket.getInetAddress());

        try {
            while (running) {
                Message message = protocol.receiveMessage();
                handleMessage(message);
            }
        } catch (IOException e) {
            if (running) {
                System.err.println("[ClientHandler] Connection error: " + e.getMessage());
            }
        } finally {
            cleanup();
        }
    }

    /**
     * Handle incoming message from client
     */
    private void handleMessage(Message message) {
        String type = message.getType();
        System.out.println("[ClientHandler] Received: " + type + " from user " + authenticatedUserId);

        try {
            switch (type) {
                case Message.REGISTER:
                    handleRegister(message);
                    break;
                case Message.LOGIN:
                    handleLogin(message);
                    break;
                case Message.SEND_MESSAGE:
                    handleSendMessage(message);
                    break;
                case Message.LOAD_HISTORY:
                    handleLoadHistory(message);
                    break;
                case Message.LIST_CONVERSATIONS:
                    handleListConversations();
                    break;
                case Message.HEARTBEAT:
                    handleHeartbeat(message);
                    break;
                case Message.LOGOUT:
                    handleLogout();
                    break;
                default:
                    sendError("Unknown message type: " + type);
            }
        } catch (Exception e) {
            System.err.println("[ClientHandler] Error handling message: " + e.getMessage());
            e.printStackTrace();
            sendError("Server error processing request");
        }
    }

    /**
     * Handle user registration
     */
    private void handleRegister(Message message) {
        String username = message.getString("username");
        String password = message.getString("password");

        User user = authService.register(username, password);

        if (user != null) {
            sendMessage(Message.registerSuccess("Registration successful! Please login."));
        } else {
            sendMessage(Message.registerFailure("Registration failed. Username may already be taken."));
        }
    }

    /**
     * Handle user login
     */
    private void handleLogin(Message message) {
        String username = message.getString("username");
        String password = message.getString("password");

        User user = authService.login(username, password);

        if (user != null) {
            this.authenticatedUserId = user.getUserId();
            this.authenticatedUsername = user.getUsername();
            
            // Register in online registry
            userRegistry.addUser(authenticatedUserId, this);
            
            sendMessage(Message.authSuccess(user.getUserId(), user.getUsername()));
            
            // Notify other users this user is online
            broadcastUserStatus(authenticatedUserId, authenticatedUsername, "ONLINE");
        } else {
            sendMessage(Message.authFailure("Invalid username or password"));
        }
    }

    /**
     * Handle send message request
     */
    private void handleSendMessage(Message message) {
        if (!isAuthenticated()) {
            sendError("Not authenticated");
            return;
        }

        int conversationId = message.getInt("conversationId");
        String text = message.getString("text");

        // Save message to database
        ChatMessage chatMessage = messageService.sendMessage(conversationId, authenticatedUserId, text);

        if (chatMessage != null) {
            // Broadcast to all conversation members
            broadcastNewMessage(chatMessage);
        } else {
            sendError("Failed to send message. You may not be a member of this conversation.");
        }
    }

    /**
     * Handle load history request
     */
    private void handleLoadHistory(Message message) {
        if (!isAuthenticated()) {
            sendError("Not authenticated");
            return;
        }

        int conversationId = message.getInt("conversationId");
        int limit = message.getInt("limit");
        int offset = message.getInt("offset");

        if (limit <= 0) {
            limit = 50;
        }

        // Check authorization
        if (!messageService.isUserMemberOfConversation(authenticatedUserId, conversationId)) {
            sendError("Not authorized to view this conversation");
            return;
        }

        List<ChatMessage> messages = messageService.getMessageHistory(conversationId, limit, offset);

        // Build response
        Message response = new Message(Message.HISTORY_RESPONSE);
        response.put("conversationId", conversationId);
        
        JSONArray messagesArray = new JSONArray();
        for (ChatMessage msg : messages) {
            JSONObject msgObj = new JSONObject();
            msgObj.put("messageId", msg.getMessageId());
            msgObj.put("senderId", msg.getSenderId());
            msgObj.put("senderName", msg.getSenderName());
            msgObj.put("text", msg.getMessageText());
            msgObj.put("timestamp", msg.getSentAt().toString());
            messagesArray.put(msgObj);
        }
        
        response.put("messages", messagesArray);
        response.put("hasMore", messages.size() == limit);
        
        sendMessage(response);
    }

    /**
     * Handle list conversations request
     */
    private void handleListConversations() {
        if (!isAuthenticated()) {
            sendError("Not authenticated");
            return;
        }

        List<Conversation> conversations = messageService.getUserConversations(authenticatedUserId);

        Message response = new Message(Message.CONVERSATION_LIST);
        JSONArray conversationsArray = new JSONArray();

        for (Conversation conv : conversations) {
            JSONObject convObj = new JSONObject();
            convObj.put("conversationId", conv.getConversationId());
            convObj.put("type", conv.getConversationType().name());
            convObj.put("name", conv.getDisplayName(authenticatedUserId));
            
            // Add member information
            JSONArray membersArray = new JSONArray();
            for (User member : conv.getMembers()) {
                JSONObject memberObj = new JSONObject();
                memberObj.put("userId", member.getUserId());
                memberObj.put("username", member.getUsername());
                memberObj.put("online", userRegistry.isUserOnline(member.getUserId()));
                membersArray.put(memberObj);
            }
            convObj.put("members", membersArray);
            
            conversationsArray.put(convObj);
        }

        response.put("conversations", conversationsArray);
        sendMessage(response);
    }

    /**
     * Handle heartbeat (keep-alive)
     */
    private void handleHeartbeat(Message message) {
        if (isAuthenticated()) {
            authService.updateLastSeen(authenticatedUserId);
        }
        // Optionally send heartbeat response
        sendMessage(Message.heartbeat());
    }

    /**
     * Handle logout
     */
    private void handleLogout() {
        if (isAuthenticated()) {
            broadcastUserStatus(authenticatedUserId, authenticatedUsername, "OFFLINE");
        }
        running = false;
    }

    /**
     * Broadcast a new message to all members of the conversation
     */
    private void broadcastNewMessage(ChatMessage chatMessage) {
        List<User> members = messageService.getConversationMembers(chatMessage.getConversationId());

        Message broadcast = new Message(Message.NEW_MESSAGE);
        broadcast.put("messageId", chatMessage.getMessageId());
        broadcast.put("conversationId", chatMessage.getConversationId());
        broadcast.put("senderId", chatMessage.getSenderId());
        broadcast.put("senderName", chatMessage.getSenderName());
        broadcast.put("text", chatMessage.getMessageText());
        broadcast.put("timestamp", chatMessage.getSentAt().toString());

        for (User member : members) {
            ClientHandler handler = userRegistry.getClientHandler(member.getUserId());
            if (handler != null) {
                handler.sendMessage(broadcast);
            }
        }
    }

    /**
     * Broadcast user status change
     */
    private void broadcastUserStatus(int userId, String username, String status) {
        Message statusMessage = new Message(Message.USER_STATUS);
        statusMessage.put("userId", userId);
        statusMessage.put("username", username);
        statusMessage.put("status", status);

        for (ClientHandler handler : userRegistry.getAllClientHandlers()) {
            if (handler.getAuthenticatedUserId() != null && handler.getAuthenticatedUserId() != userId) {
                handler.sendMessage(statusMessage);
            }
        }
    }

    /**
     * Send a message to this client
     */
    public void sendMessage(Message message) {
        try {
            protocol.sendMessage(message);
        } catch (IOException e) {
            System.err.println("[ClientHandler] Error sending message: " + e.getMessage());
            running = false;
        }
    }

    /**
     * Send an error message to client
     */
    private void sendError(String errorMessage) {
        sendMessage(Message.error(errorMessage));
    }

    /**
     * Check if client is authenticated
     */
    private boolean isAuthenticated() {
        return authenticatedUserId != null;
    }

    /**
     * Get authenticated user ID
     */
    public Integer getAuthenticatedUserId() {
        return authenticatedUserId;
    }

    /**
     * Cleanup resources
     */
    private void cleanup() {
        running = false;
        
        if (authenticatedUserId != null) {
            userRegistry.removeUser(authenticatedUserId);
            broadcastUserStatus(authenticatedUserId, authenticatedUsername, "OFFLINE");
        }

        try {
            protocol.close();
        } catch (Exception e) {
            // Ignore
        }

        try {
            socket.close();
        } catch (IOException e) {
            // Ignore
        }

        System.out.println("[ClientHandler] Connection closed for user " + authenticatedUserId);
    }
}
