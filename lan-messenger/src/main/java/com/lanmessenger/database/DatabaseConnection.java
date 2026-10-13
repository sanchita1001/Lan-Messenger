package com.lanmessenger.database;

import com.lanmessenger.utils.ConfigUtil;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Manages database connections
 * Future enhancement: Use HikariCP connection pool
 */
public class DatabaseConnection {
    
    /**
     * Get a new database connection
     * 
     * @return Database connection
     * @throws SQLException if connection fails
     */
    public static Connection getConnection() throws SQLException {
        String url = ConfigUtil.getDatabaseUrl();
        String username = ConfigUtil.getDatabaseUsername();
        String password = ConfigUtil.getDatabasePassword();

        try {
            // Load MySQL JDBC driver
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC Driver not found", e);
        }

        return DriverManager.getConnection(url, username, password);
    }

    /**
     * Test the database connection
     * 
     * @return true if connection successful
     */
    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            System.err.println("[DATABASE] Connection test failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Close a connection safely
     */
    public static void closeConnection(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                System.err.println("[DATABASE] Error closing connection: " + e.getMessage());
            }
        }
    }
}
