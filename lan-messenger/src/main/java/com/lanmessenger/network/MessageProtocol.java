package com.lanmessenger.network;

import org.json.JSONObject;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Handles the serialization and deserialization of messages over TCP sockets
 * Uses length-prefixed JSON protocol:
 * [4-byte length (big-endian)][JSON payload (UTF-8)]
 */
public class MessageProtocol {
    private final InputStream inputStream;
    private final OutputStream outputStream;
    private final Object writeLock = new Object();

    /**
     * Create a MessageProtocol for a socket
     */
    public MessageProtocol(Socket socket) throws IOException {
        this.inputStream = socket.getInputStream();
        this.outputStream = socket.getOutputStream();
    }

    /**
     * Create a MessageProtocol with custom streams
     */
    public MessageProtocol(InputStream inputStream, OutputStream outputStream) {
        this.inputStream = inputStream;
        this.outputStream = outputStream;
    }

    /**
     * Send a message over the socket
     * 
     * @param message The message to send
     * @throws IOException if an I/O error occurs
     */
    public void sendMessage(Message message) throws IOException {
        String json = message.toJSONString();
        byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);
        int length = jsonBytes.length;

        synchronized (writeLock) {
            // Write length (4 bytes, big-endian)
            outputStream.write((length >> 24) & 0xFF);
            outputStream.write((length >> 16) & 0xFF);
            outputStream.write((length >> 8) & 0xFF);
            outputStream.write(length & 0xFF);

            // Write payload
            outputStream.write(jsonBytes);
            outputStream.flush();
        }
    }

    /**
     * Receive a message from the socket
     * This method blocks until a complete message is received
     * 
     * @return The received message
     * @throws IOException if an I/O error occurs or connection is closed
     */
    public Message receiveMessage() throws IOException {
        // Read length (4 bytes, big-endian)
        int b1 = inputStream.read();
        int b2 = inputStream.read();
        int b3 = inputStream.read();
        int b4 = inputStream.read();

        if (b1 == -1 || b2 == -1 || b3 == -1 || b4 == -1) {
            throw new EOFException("Connection closed by remote host");
        }

        int length = ((b1 & 0xFF) << 24) |
                     ((b2 & 0xFF) << 16) |
                     ((b3 & 0xFF) << 8) |
                     (b4 & 0xFF);

        // Validate length (prevent malicious large messages)
        if (length <= 0 || length > 10_000_000) { // 10 MB max
            throw new IOException("Invalid message length: " + length);
        }

        // Read payload
        byte[] jsonBytes = new byte[length];
        int totalBytesRead = 0;

        while (totalBytesRead < length) {
            int bytesRead = inputStream.read(jsonBytes, totalBytesRead, length - totalBytesRead);
            if (bytesRead == -1) {
                throw new EOFException("Connection closed while reading message payload");
            }
            totalBytesRead += bytesRead;
        }

        // Parse JSON
        String json = new String(jsonBytes, StandardCharsets.UTF_8);
        try {
            JSONObject jsonObject = new JSONObject(json);
            return new Message(jsonObject);
        } catch (Exception e) {
            throw new IOException("Failed to parse JSON message: " + e.getMessage(), e);
        }
    }

    /**
     * Close the streams
     */
    public void close() {
        try {
            if (inputStream != null) {
                inputStream.close();
            }
        } catch (IOException e) {
            // Ignore
        }
        
        try {
            if (outputStream != null) {
                outputStream.close();
            }
        } catch (IOException e) {
            // Ignore
        }
    }
}
