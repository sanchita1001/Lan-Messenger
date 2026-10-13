package com.lanmessenger.client;

import com.lanmessenger.network.Message;
import com.lanmessenger.network.MessageProtocol;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Manages the client's connection to the server
 * Handles sending and receiving messages
 */
public class ServerConnection {
    private Socket socket;
    private MessageProtocol protocol;
    private final String host;
    private final int port;
    
    private Thread listenerThread;
    private volatile boolean running = false;
    
    private final BlockingQueue<Message> incomingMessages = new LinkedBlockingQueue<>();
    private MessageHandler messageHandler;

    public ServerConnection(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /**
     * Set the message handler for incoming messages
     */
    public void setMessageHandler(MessageHandler messageHandler) {
        this.messageHandler = messageHandler;
    }

    /**
     * Connect to the server
     */
    public boolean connect() {
        try {
            socket = new Socket(host, port);
            protocol = new MessageProtocol(socket);
            running = true;
            
            // Start listener thread
            listenerThread = new Thread(this::listenForMessages);
            listenerThread.setDaemon(true);
            listenerThread.start();
            
            System.out.println("[Client] Connected to server: " + host + ":" + port);
            return true;
            
        } catch (IOException e) {
            System.err.println("[Client] Failed to connect to server: " + e.getMessage());
            return false;
        }
    }

    /**
     * Listen for incoming messages from server
     * Runs in a separate thread
     */
    private void listenForMessages() {
        while (running) {
            try {
                Message message = protocol.receiveMessage();
                
                if (messageHandler != null) {
                    messageHandler.handleMessage(message);
                } else {
                    incomingMessages.offer(message);
                }
                
            } catch (IOException e) {
                if (running) {
                    System.err.println("[Client] Connection lost: " + e.getMessage());
                    if (messageHandler != null) {
                        messageHandler.onConnectionLost();
                    }
                    running = false;
                }
                break;
            }
        }
    }

    /**
     * Send a message to the server
     */
    public boolean sendMessage(Message message) {
        if (!isConnected()) {
            System.err.println("[Client] Not connected to server");
            return false;
        }

        try {
            protocol.sendMessage(message);
            return true;
        } catch (IOException e) {
            System.err.println("[Client] Error sending message: " + e.getMessage());
            return false;
        }
    }

    /**
     * Check if connected to server
     */
    public boolean isConnected() {
        return running && socket != null && !socket.isClosed();
    }

    /**
     * Disconnect from server
     */
    public void disconnect() {
        running = false;

        // Send logout message
        if (isConnected()) {
            try {
                sendMessage(new Message(Message.LOGOUT));
            } catch (Exception e) {
                // Ignore
            }
        }

        // Close protocol
        if (protocol != null) {
            try {
                protocol.close();
            } catch (Exception e) {
                // Ignore
            }
        }

        // Close socket
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                // Ignore
            }
        }

        System.out.println("[Client] Disconnected from server");
    }

    /**
     * Get next message from queue (blocking)
     */
    public Message getNextMessage() throws InterruptedException {
        return incomingMessages.take();
    }

    /**
     * Interface for handling incoming messages
     */
    public interface MessageHandler {
        void handleMessage(Message message);
        void onConnectionLost();
    }
}
