package com.lanmessenger.utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Utility class for loading and accessing application configuration
 */
public class ConfigUtil {
    private static final Properties properties = new Properties();
    private static boolean loaded = false;

    /**
     * Load configuration from config.properties file
     */
    public static void loadConfig() {
        if (loaded) {
            return;
        }

        try {
            // Try to load from file system first
            try (FileInputStream fis = new FileInputStream("src/main/resources/config.properties")) {
                properties.load(fis);
                System.out.println("[CONFIG] Loaded configuration from file system");
                loaded = true;
                return;
            } catch (IOException e) {
                // File not found in file system, try classpath
            }

            // Try to load from classpath
            try (InputStream is = ConfigUtil.class.getClassLoader().getResourceAsStream("config.properties")) {
                if (is != null) {
                    properties.load(is);
                    System.out.println("[CONFIG] Loaded configuration from classpath");
                    loaded = true;
                    return;
                }
            }

            // If no config file found, use defaults
            System.out.println("[CONFIG] No config file found, using defaults");
            loadDefaults();
            loaded = true;

        } catch (IOException e) {
            System.err.println("[CONFIG] Error loading configuration: " + e.getMessage());
            loadDefaults();
            loaded = true;
        }
    }

    /**
     * Load default configuration values
     */
    private static void loadDefaults() {
        // Database defaults
        properties.setProperty("db.url", "jdbc:mysql://localhost:3306/lan_messenger?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true");
        properties.setProperty("db.username", "root");
        properties.setProperty("db.password", "");

        // Server defaults
        properties.setProperty("server.port", "5000");
        properties.setProperty("server.host", "localhost");
        properties.setProperty("server.max.clients", "100");

        // File transfer defaults
        properties.setProperty("file.upload.dir", "uploads");
        properties.setProperty("file.max.size", "52428800"); // 50 MB

        // Security defaults
        properties.setProperty("security.bcrypt.rounds", "12");
    }

    /**
     * Get configuration property value
     */
    public static String get(String key) {
        if (!loaded) {
            loadConfig();
        }
        return properties.getProperty(key);
    }

    /**
     * Get configuration property with default value
     */
    public static String get(String key, String defaultValue) {
        if (!loaded) {
            loadConfig();
        }
        return properties.getProperty(key, defaultValue);
    }

    /**
     * Get integer property
     */
    public static int getInt(String key, int defaultValue) {
        String value = get(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Get long property
     */
    public static long getLong(String key, long defaultValue) {
        String value = get(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Get boolean property
     */
    public static boolean getBoolean(String key, boolean defaultValue) {
        String value = get(key);
        if (value == null) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value);
    }

    // Convenient accessor methods for common properties

    public static String getDatabaseUrl() {
        return get("db.url");
    }

    public static String getDatabaseUsername() {
        return get("db.username");
    }

    public static String getDatabasePassword() {
        return get("db.password");
    }

    public static int getServerPort() {
        return getInt("server.port", 5000);
    }

    public static String getServerHost() {
        return get("server.host", "localhost");
    }

    public static int getMaxClients() {
        return getInt("server.max.clients", 100);
    }

    public static String getFileUploadDir() {
        return get("file.upload.dir", "uploads");
    }

    public static long getMaxFileSize() {
        return getLong("file.max.size", 52428800L);
    }

    public static int getBCryptRounds() {
        return getInt("security.bcrypt.rounds", 12);
    }
}
