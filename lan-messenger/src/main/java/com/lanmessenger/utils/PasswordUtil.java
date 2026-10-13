package com.lanmessenger.utils;

import at.favre.lib.crypto.bcrypt.BCrypt;

/**
 * Utility class for password hashing and verification using BCrypt
 */
public class PasswordUtil {

    /**
     * Hash a plain text password using BCrypt
     * 
     * @param plainPassword The plain text password
     * @return BCrypt hashed password
     */
    public static String hashPassword(String plainPassword) {
        int rounds = ConfigUtil.getBCryptRounds();
        return BCrypt.withDefaults().hashToString(rounds, plainPassword.toCharArray());
    }

    /**
     * Verify a plain text password against a BCrypt hash
     * 
     * @param plainPassword The plain text password to verify
     * @param hashedPassword The BCrypt hash to verify against
     * @return true if password matches, false otherwise
     */
    public static boolean verifyPassword(String plainPassword, String hashedPassword) {
        if (plainPassword == null || hashedPassword == null) {
            return false;
        }
        
        try {
            BCrypt.Result result = BCrypt.verifyer().verify(plainPassword.toCharArray(), hashedPassword);
            return result.verified;
        } catch (Exception e) {
            System.err.println("[PASSWORD] Error verifying password: " + e.getMessage());
            return false;
        }
    }

    /**
     * Validate password strength (basic validation)
     * 
     * @param password The password to validate
     * @return true if password meets minimum requirements
     */
    public static boolean isValidPassword(String password) {
        if (password == null || password.length() < 6) {
            return false;
        }
        // Add more complex validation rules here if needed
        return true;
    }

    /**
     * Validate username format
     * 
     * @param username The username to validate
     * @return true if username is valid
     */
    public static boolean isValidUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }
        
        // Username must be 3-50 characters, alphanumeric and underscore only
        return username.matches("^[a-zA-Z0-9_]{3,50}$");
    }
}
