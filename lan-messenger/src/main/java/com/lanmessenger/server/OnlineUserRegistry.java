package com.lanmessenger.server;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe registry of online users and their client handlers
 */
public class OnlineUserRegistry {
    private final ConcurrentHashMap<Integer, ClientHandler> onlineUsers;

    public OnlineUserRegistry() {
        this.onlineUsers = new ConcurrentHashMap<>();
    }

    /**
     * Register an online user
     */
    public void addUser(int userId, ClientHandler clientHandler) {
        onlineUsers.put(userId, clientHandler);
        System.out.println("[Registry] User " + userId + " is now online. Total online: " + onlineUsers.size());
    }

    /**
     * Remove an online user
     */
    public void removeUser(int userId) {
        onlineUsers.remove(userId);
        System.out.println("[Registry] User " + userId + " is now offline. Total online: " + onlineUsers.size());
    }

    /**
     * Get client handler for a user
     */
    public ClientHandler getClientHandler(int userId) {
        return onlineUsers.get(userId);
    }

    /**
     * Check if user is online
     */
    public boolean isUserOnline(int userId) {
        return onlineUsers.containsKey(userId);
    }

    /**
     * Get all online user IDs
     */
    public List<Integer> getOnlineUserIds() {
        return new ArrayList<>(onlineUsers.keySet());
    }

    /**
     * Get count of online users
     */
    public int getOnlineCount() {
        return onlineUsers.size();
    }

    /**
     * Get all client handlers
     */
    public List<ClientHandler> getAllClientHandlers() {
        return new ArrayList<>(onlineUsers.values());
    }
}
