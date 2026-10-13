package com.lanmessenger.database;

import com.lanmessenger.models.User;

import java.sql.*;

/**
 * Data Access Object for User operations
 */
public class UserDAO {

    /**
     * Create a new user
     * 
     * @param username User's username
     * @param passwordHash BCrypt hashed password
     * @return The created user with generated ID, or null if failed
     */
    public User createUser(String username, String passwordHash) {
        String sql = "INSERT INTO users (username, password_hash) VALUES (?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, username);
            stmt.setString(2, passwordHash);

            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int userId = generatedKeys.getInt(1);
                        return new User(userId, username, passwordHash, null, null);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] Error creating user: " + e.getMessage());
        }

        return null;
    }

    /**
     * Get user by username
     * 
     * @param username The username to search for
     * @return User object or null if not found
     */
    public User getUserByUsername(String username) {
        String sql = "SELECT user_id, username, password_hash, created_at, last_seen " +
                    "FROM users WHERE username = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return new User(
                        rs.getInt("user_id"),
                        rs.getString("username"),
                        rs.getString("password_hash"),
                        rs.getTimestamp("created_at"),
                        rs.getTimestamp("last_seen")
                );
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] Error getting user by username: " + e.getMessage());
        }

        return null;
    }

    /**
     * Get user by ID
     * 
     * @param userId The user ID
     * @return User object or null if not found
     */
    public User getUserById(int userId) {
        String sql = "SELECT user_id, username, password_hash, created_at, last_seen " +
                    "FROM users WHERE user_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return new User(
                        rs.getInt("user_id"),
                        rs.getString("username"),
                        rs.getString("password_hash"),
                        rs.getTimestamp("created_at"),
                        rs.getTimestamp("last_seen")
                );
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] Error getting user by ID: " + e.getMessage());
        }

        return null;
    }

    /**
     * Check if username already exists
     * 
     * @param username The username to check
     * @return true if username exists
     */
    public boolean isUsernameTaken(String username) {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] Error checking username: " + e.getMessage());
        }

        return false;
    }

    /**
     * Update user's last seen timestamp
     * 
     * @param userId The user ID
     * @return true if updated successfully
     */
    public boolean updateLastSeen(int userId) {
        String sql = "UPDATE users SET last_seen = CURRENT_TIMESTAMP WHERE user_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("[UserDAO] Error updating last seen: " + e.getMessage());
            return false;
        }
    }
}
