package com.lanmessenger.services;

import com.lanmessenger.database.UserDAO;
import com.lanmessenger.models.User;
import com.lanmessenger.utils.PasswordUtil;

/**
 * Service for authentication operations
 */
public class AuthService {
    private final UserDAO userDAO;

    public AuthService() {
        this.userDAO = new UserDAO();
    }

    /**
     * Register a new user
     * 
     * @param username The username
     * @param password The plain text password
     * @return The created user, or null if registration failed
     */
    public User register(String username, String password) {
        // Validate username
        if (!PasswordUtil.isValidUsername(username)) {
            System.err.println("[AuthService] Invalid username format: " + username);
            return null;
        }

        // Validate password
        if (!PasswordUtil.isValidPassword(password)) {
            System.err.println("[AuthService] Password does not meet requirements");
            return null;
        }

        // Check if username already exists
        if (userDAO.isUsernameTaken(username)) {
            System.err.println("[AuthService] Username already taken: " + username);
            return null;
        }

        // Hash password
        String passwordHash = PasswordUtil.hashPassword(password);

        // Create user
        User user = userDAO.createUser(username, passwordHash);
        if (user != null) {
            System.out.println("[AuthService] User registered successfully: " + username);
        }

        return user;
    }

    /**
     * Authenticate a user
     * 
     * @param username The username
     * @param password The plain text password
     * @return The authenticated user, or null if authentication failed
     */
    public User login(String username, String password) {
        // Get user from database
        User user = userDAO.getUserByUsername(username);
        
        if (user == null) {
            System.err.println("[AuthService] User not found: " + username);
            return null;
        }

        // Verify password
        if (!PasswordUtil.verifyPassword(password, user.getPasswordHash())) {
            System.err.println("[AuthService] Invalid password for user: " + username);
            return null;
        }

        // Update last seen
        userDAO.updateLastSeen(user.getUserId());

        System.out.println("[AuthService] User logged in successfully: " + username);
        return user;
    }

    /**
     * Update user's last seen timestamp
     */
    public void updateLastSeen(int userId) {
        userDAO.updateLastSeen(userId);
    }
}
