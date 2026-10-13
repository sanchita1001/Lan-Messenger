package com.lanmessenger.server;

import com.lanmessenger.database.DatabaseConnection;
import com.lanmessenger.utils.ConfigUtil;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Main server application
 * Listens for client connections and spawns ClientHandler threads
 */
public class ServerApp {
    private static volatile boolean running = true;
    private static ServerSocket serverSocket;
    private static final OnlineUserRegistry userRegistry = new OnlineUserRegistry();

    public static void main(String[] args) {
        System.out.println("===========================================");
        System.out.println("   LAN Messenger Server                   ");
        System.out.println("===========================================");

        // Load configuration
        ConfigUtil.loadConfig();
        int port = ConfigUtil.getServerPort();

        // Test database connection
        System.out.println("[SERVER] Testing database connection...");
        if (!DatabaseConnection.testConnection()) {
            System.err.println("[SERVER] Failed to connect to database!");
            System.err.println("[SERVER] Please check your database configuration in config.properties");
            System.err.println("[SERVER] Ensure MySQL is running and credentials are correct.");
            return;
        }
        System.out.println("[SERVER] Database connection successful!");

        // Start server
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("[SERVER] Server listening on port " + port);
            System.out.println("[SERVER] Waiting for client connections...");
            System.out.println("[SERVER] Press Ctrl+C to shutdown");
            System.out.println("===========================================\n");

            // Register shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\n[SERVER] Shutdown signal received...");
                shutdown();
            }));

            // Accept client connections
            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("[SERVER] New connection from: " + clientSocket.getInetAddress());

                    // Create and start client handler thread
                    ClientHandler clientHandler = new ClientHandler(clientSocket, userRegistry);
                    Thread thread = new Thread(clientHandler);
                    thread.setDaemon(true);
                    thread.start();

                } catch (IOException e) {
                    if (running) {
                        System.err.println("[SERVER] Error accepting client connection: " + e.getMessage());
                    }
                }
            }

        } catch (IOException e) {
            System.err.println("[SERVER] Failed to start server: " + e.getMessage());
            e.printStackTrace();
        } finally {
            shutdown();
        }
    }

    /**
     * Shutdown server gracefully
     */
    private static void shutdown() {
        if (!running) {
            return;
        }

        running = false;
        System.out.println("[SERVER] Shutting down server...");

        // Close server socket
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
                System.out.println("[SERVER] Server socket closed");
            } catch (IOException e) {
                System.err.println("[SERVER] Error closing server socket: " + e.getMessage());
            }
        }

        System.out.println("[SERVER] Server shutdown complete");
        System.out.println("===========================================");
    }
}
