package com.lanmessenger.client;

import com.lanmessenger.network.Message;
import com.lanmessenger.utils.ConfigUtil;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;

/**
 * Simple JavaFX Client Application
 * Provides a basic GUI for testing the chat functionality
 */
public class ClientApp extends Application {
    private ServerConnection serverConnection;
    private Stage primaryStage;
    
    // User session
    private Integer userId;
    private String username;
    private Integer currentConversationId;
    
    // UI Components
    private TextArea chatArea;
    private TextField messageField;
    private ListView<String> conversationListView;
    private Label statusLabel;
    private Label conversationTitleLabel;
    private ListView<String> onlineUsersListView; // For showing online users
    private JSONArray cachedOnlineUsers; // Cache online users data

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("LAN Messenger");
        
        // Load configuration
        ConfigUtil.loadConfig();
        
        // Show login screen
        showLoginScreen();
    }

    /**
     * Show login/registration screen
     */
    private void showLoginScreen() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(40));
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #2c3e50;");

        Label titleLabel = new Label("LAN Messenger");
        titleLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: white;");
        
        // Server configuration section
        HBox serverConfigBox = new HBox(10);
        serverConfigBox.setAlignment(Pos.CENTER);
        
        TextField serverHostField = new TextField(ConfigUtil.getServerHost());
        serverHostField.setPromptText("Server IP");
        serverHostField.setPrefWidth(200);
        
        TextField serverPortField = new TextField(String.valueOf(ConfigUtil.getServerPort()));
        serverPortField.setPromptText("Port");
        serverPortField.setPrefWidth(80);
        
        serverConfigBox.getChildren().addAll(
            new Label("Server:") {{setStyle("-fx-text-fill: white;");}},
            serverHostField,
            new Label("Port:") {{setStyle("-fx-text-fill: white;");}},
            serverPortField
        );

        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");
        usernameField.setMaxWidth(300);

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        passwordField.setMaxWidth(300);

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #e74c3c;");

        Button loginButton = new Button("Login");
        loginButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 14px;");
        loginButton.setPrefWidth(140);

        Button registerButton = new Button("Register");
        registerButton.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-size: 14px;");
        registerButton.setPrefWidth(140);

        HBox buttonBox = new HBox(10, loginButton, registerButton);
        buttonBox.setAlignment(Pos.CENTER);

        root.getChildren().addAll(titleLabel, serverConfigBox, usernameField, passwordField, errorLabel, buttonBox);

        // Login action
        loginButton.setOnAction(e -> {
            String user = usernameField.getText().trim();
            String pass = passwordField.getText();
            String host = serverHostField.getText().trim();
            String portStr = serverPortField.getText().trim();
            
            if (user.isEmpty() || pass.isEmpty()) {
                errorLabel.setText("Please enter username and password");
                return;
            }
            
            if (host.isEmpty()) {
                errorLabel.setText("Please enter server IP address");
                return;
            }
            
            int port;
            try {
                port = Integer.parseInt(portStr);
            } catch (NumberFormatException ex) {
                errorLabel.setText("Invalid port number");
                return;
            }

            errorLabel.setText("Connecting to " + host + ":" + port + "...");
            new Thread(() -> login(user, pass, host, port, errorLabel)).start();
        });

        // Register action
        registerButton.setOnAction(e -> {
            String user = usernameField.getText().trim();
            String pass = passwordField.getText();
            String host = serverHostField.getText().trim();
            String portStr = serverPortField.getText().trim();
            
            if (user.isEmpty() || pass.isEmpty()) {
                errorLabel.setText("Please enter username and password");
                return;
            }
            
            if (host.isEmpty()) {
                errorLabel.setText("Please enter server IP address");
                return;
            }
            
            int port;
            try {
                port = Integer.parseInt(portStr);
            } catch (NumberFormatException ex) {
                errorLabel.setText("Invalid port number");
                return;
            }

            errorLabel.setText("Connecting to " + host + ":" + port + "...");
            new Thread(() -> register(user, pass, host, port, errorLabel)).start();
        });

        // Enter key login
        passwordField.setOnAction(e -> loginButton.fire());

        Scene scene = new Scene(root, 500, 400);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /**
     * Attempt to register
     */
    private void register(String username, String password, String host, int port, Label errorLabel) {
        serverConnection = new ServerConnection(host, port);
        if (!serverConnection.connect()) {
            Platform.runLater(() -> errorLabel.setText("Cannot connect to server at " + host + ":" + port));
            return;
        }

        // Send register request
        Message registerMsg = Message.registerRequest(username, password);
        serverConnection.sendMessage(registerMsg);

        // Wait for response
        try {
            Message response = serverConnection.getNextMessage();
            
            if (Message.REGISTER_SUCCESS.equals(response.getType())) {
                Platform.runLater(() -> {
                    errorLabel.setStyle("-fx-text-fill: #2ecc71;");
                    errorLabel.setText("Registration successful! Please login.");
                });
                serverConnection.disconnect();
            } else {
                Platform.runLater(() -> errorLabel.setText("Registration failed: " + response.getString("error")));
                serverConnection.disconnect();
            }
        } catch (InterruptedException e) {
            Platform.runLater(() -> errorLabel.setText("Connection interrupted"));
        }
    }

    /**
     * Attempt to login
     */
    private void login(String username, String password, String host, int port, Label errorLabel) {
        serverConnection = new ServerConnection(host, port);
        if (!serverConnection.connect()) {
            Platform.runLater(() -> errorLabel.setText("Cannot connect to server at " + host + ":" + port));
            return;
        }

        // Set message handler
        serverConnection.setMessageHandler(new ServerConnection.MessageHandler() {
            @Override
            public void handleMessage(Message message) {
                handleServerMessage(message);
            }

            @Override
            public void onConnectionLost() {
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Connection Lost");
                    alert.setContentText("Connection to server was lost");
                    alert.showAndWait();
                    showLoginScreen();
                });
            }
        });

        // Send login request
        Message loginMsg = Message.loginRequest(username, password);
        serverConnection.sendMessage(loginMsg);
    }

    /**
     * Handle messages from server
     */
    private void handleServerMessage(Message message) {
        String type = message.getType();

        switch (type) {
            case Message.AUTH_SUCCESS:
                this.userId = message.getInt("userId");
                this.username = message.getString("username");
                Platform.runLater(() -> {
                    showChatScreen();
                    requestConversationList();
                });
                break;

            case Message.AUTH_FAILURE:
                Platform.runLater(() -> showLoginScreen());
                serverConnection.disconnect();
                break;

            case Message.CONVERSATION_LIST:
                Platform.runLater(() -> updateConversationList(message));
                break;
                
            case Message.CONVERSATION_CREATED:
                Platform.runLater(() -> {
                    // Refresh conversation list to show new conversation
                    requestConversationList();
                });
                break;
                
            case Message.ONLINE_USERS_LIST:
                Platform.runLater(() -> handleOnlineUsersList(message));
                break;

            case Message.HISTORY_RESPONSE:
                Platform.runLater(() -> displayHistory(message));
                break;

            case Message.NEW_MESSAGE:
                Platform.runLater(() -> displayNewMessage(message));
                break;

            case Message.USER_STATUS:
                Platform.runLater(() -> {
                    String user = message.getString("username");
                    String status = message.getString("status");
                    statusLabel.setText(user + " is now " + status.toLowerCase());
                });
                break;

            case Message.ERROR:
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Error");
                    alert.setContentText(message.getString("error"));
                    alert.show();
                });
                break;
                
            case Message.FILE_TRANSFER:
                Platform.runLater(() -> handleIncomingFile(message));
                break;
                
            case Message.FILE_DATA:
                Platform.runLater(() -> handleFileData(message));
                break;
        }
    }
    
    /**
     * Handle file data response (download)
     */
    private void handleFileData(Message message) {
        try {
            String filename = message.getString("filename");
            String base64Data = message.getString("fileData");
            long fileSize = message.getLong("fileSize");
            
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save File");
            fileChooser.setInitialFileName(filename);
            
            File saveFile = fileChooser.showSaveDialog(primaryStage);
            if (saveFile == null) {
                return; // User cancelled
            }
            
            byte[] fileData = Base64.getDecoder().decode(base64Data);
            Files.write(saveFile.toPath(), fileData);
            
            showAlert("Success", String.format("File saved successfully!\n%s (%.2f KB)", 
                filename, fileSize / 1024.0));
            
        } catch (Exception e) {
            showAlert("Save Error", "Failed to save file: " + e.getMessage());
        }
    }
    
    /**
     * Handle incoming file transfer notification
     */
    private void handleIncomingFile(Message message) {
        int convId = message.getInt("conversationId");
        
        // Only display if it's for the current conversation
        if (currentConversationId != null && currentConversationId == convId) {
            int senderId = message.getInt("senderId");
            String sender = message.getString("senderName");
            String filename = message.getString("filename");
            long fileSize = message.getLong("fileSize");
            int fileId = message.getInt("fileId");
            String timestamp = message.getString("timestamp");
            
            // Display file message in chat for both sender and receiver
            String formattedTime = formatTimestamp(timestamp);
            chatArea.appendText(String.format("[%s] %s sent a file:\n", formattedTime, sender));
            chatArea.appendText(String.format("    📎 %s (%.2f KB)\n", filename, fileSize / 1024.0));
            chatArea.appendText(String.format("    To download: Type '/download %d' in the message field\n\n", fileId));
            
            // Only auto-download if this user is NOT the sender
            if (senderId != this.userId) {
                downloadFile(fileId);
            } else {
                statusLabel.setText("File sent successfully!");
            }
        }
    }
    
    /**
     * Download a file from server
     */
    private void downloadFile(int fileId) {
        Message request = new Message(Message.REQUEST_FILE);
        request.put("fileId", fileId);
        serverConnection.sendMessage(request);
        
        statusLabel.setText("Downloading file...");
    }
    
    /**
     * Save received file to disk (deprecated - now uses downloadFile)
     */
    private void saveReceivedFile(Message message) {
        try {
            String filename = message.getString("filename");
            String base64Data = message.getString("fileData");
            
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save File");
            fileChooser.setInitialFileName(filename);
            
            File saveFile = fileChooser.showSaveDialog(primaryStage);
            if (saveFile == null) {
                return; // User cancelled
            }
            
            byte[] fileData = Base64.getDecoder().decode(base64Data);
            Files.write(saveFile.toPath(), fileData);
            
            showAlert("Success", "File saved successfully!");
            
        } catch (Exception e) {
            showAlert("Save Error", "Failed to save file: " + e.getMessage());
        }
    }

    /**
     * Show main chat screen
     */
    private void showChatScreen() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #ecf0f1;");

        // Left sidebar - conversations list
        VBox sidebar = new VBox(10);
        sidebar.setPrefWidth(200);
        sidebar.setStyle("-fx-background-color: #34495e; -fx-padding: 10;");

        Label sidebarTitle = new Label("Conversations");
        sidebarTitle.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");

        Button newChatButton = new Button("+ New Chat");
        newChatButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-size: 12px;");
        newChatButton.setMaxWidth(Double.MAX_VALUE);
        newChatButton.setOnAction(e -> showNewChatDialog());

        conversationListView = new ListView<>();
        conversationListView.setStyle("-fx-background-color: #2c3e50; -fx-text-fill: white;");
        conversationListView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                String selected = conversationListView.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    openConversation(selected);
                }
            }
        });

        sidebar.getChildren().addAll(sidebarTitle, newChatButton, conversationListView);
        VBox.setVgrow(conversationListView, Priority.ALWAYS);

        // Center - chat area
        VBox centerBox = new VBox(10);
        centerBox.setPadding(new Insets(10));

        conversationTitleLabel = new Label("Select a conversation");
        conversationTitleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        chatArea = new TextArea();
        chatArea.setEditable(false);
        chatArea.setWrapText(true);
        chatArea.setStyle("-fx-control-inner-background: white; -fx-font-size: 13px;");
        VBox.setVgrow(chatArea, Priority.ALWAYS);

        HBox inputBox = new HBox(10);
        messageField = new TextField();
        messageField.setPromptText("Type a message...");
        messageField.setDisable(true);
        HBox.setHgrow(messageField, Priority.ALWAYS);

        Button sendButton = new Button("Send");
        sendButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white;");
        sendButton.setDisable(true);
        sendButton.setOnAction(e -> sendMessage());
        
        Button attachButton = new Button("📎 Attach File");
        attachButton.setStyle("-fx-background-color: #9b59b6; -fx-text-fill: white;");
        attachButton.setDisable(true);
        attachButton.setOnAction(e -> attachAndSendFile());

        messageField.setOnAction(e -> sendMessage());

        inputBox.getChildren().addAll(messageField, attachButton, sendButton);

        centerBox.getChildren().addAll(conversationTitleLabel, chatArea, inputBox);

        // Bottom status bar with logout button
        HBox statusBar = new HBox(10);
        statusBar.setStyle("-fx-padding: 5; -fx-background-color: #34495e;");
        statusBar.setAlignment(Pos.CENTER_LEFT);
        
        statusLabel = new Label("Logged in as: " + username);
        statusLabel.setStyle("-fx-text-fill: white;");
        HBox.setHgrow(statusLabel, Priority.ALWAYS);
        
        Button logoutButton = new Button("Logout");
        logoutButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
        logoutButton.setOnAction(e -> logout());
        
        statusBar.getChildren().addAll(statusLabel, logoutButton);

        root.setLeft(sidebar);
        root.setCenter(centerBox);
        root.setBottom(statusBar);

        Scene scene = new Scene(root, 800, 600);
        primaryStage.setScene(scene);
        primaryStage.setTitle("LAN Messenger - " + username);
        
        // Handle window close event
        primaryStage.setOnCloseRequest(e -> {
            if (serverConnection != null && serverConnection.isConnected()) {
                serverConnection.disconnect();
            }
        });
    }

    /**
     * Handle online users list response
     */
    private void handleOnlineUsersList(Message message) {
        cachedOnlineUsers = message.getData().getJSONArray("users");
        
        // If the online users dialog is showing, update it
        if (onlineUsersListView != null) {
            onlineUsersListView.getItems().clear();
            for (int i = 0; i < cachedOnlineUsers.length(); i++) {
                JSONObject user = cachedOnlineUsers.getJSONObject(i);
                String username = user.getString("username");
                onlineUsersListView.getItems().add(username);
            }
        }
    }
    
    /**
     * Show dialog to start a new chat with an online user
     */
    private void showNewChatDialog() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("New Chat");
        dialog.setHeaderText("Select an online user to start chatting");

        // Create list view of online users
        onlineUsersListView = new ListView<>();
        
        // Request online users from server
        serverConnection.sendMessage(Message.listOnlineUsersRequest());
        
        // Show loading message
        onlineUsersListView.getItems().add("Loading online users...");
        
        // Wait a moment for the response
        new Thread(() -> {
            try {
                Thread.sleep(500);
                Platform.runLater(() -> {
                    if (cachedOnlineUsers == null || cachedOnlineUsers.length() == 0) {
                        onlineUsersListView.getItems().clear();
                        onlineUsersListView.getItems().add("No other users online");
                    }
                });
            } catch (InterruptedException e) {
                // Ignore
            }
        }).start();

        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        Label instruction = new Label("Double-click a user to start chatting:");
        instruction.setStyle("-fx-font-size: 13px;");
        
        Button refreshButton = new Button("🔄 Refresh");
        refreshButton.setOnAction(e -> {
            onlineUsersListView.getItems().clear();
            onlineUsersListView.getItems().add("Loading...");
            serverConnection.sendMessage(Message.listOnlineUsersRequest());
        });
        
        content.getChildren().addAll(instruction, refreshButton, onlineUsersListView);
        
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefWidth(300);
        dialog.getDialogPane().setPrefHeight(400);

        // Add buttons
        ButtonType selectButton = new ButtonType("Chat", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(selectButton, cancelButton);

        // Handle double-click
        onlineUsersListView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                String selectedUser = onlineUsersListView.getSelectionModel().getSelectedItem();
                if (selectedUser != null && !selectedUser.startsWith("Loading") && !selectedUser.startsWith("No other")) {
                    openChatWithUser(selectedUser);
                    dialog.close();
                }
            }
        });

        // Handle select button
        dialog.setResultConverter(button -> {
            if (button == selectButton) {
                String selected = onlineUsersListView.getSelectionModel().getSelectedItem();
                if (selected != null && !selected.startsWith("Loading") && !selected.startsWith("No other")) {
                    return selected;
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(selectedUser -> {
            if (selectedUser != null && !selectedUser.isEmpty()) {
                openChatWithUser(selectedUser);
            }
        });
        
        // Clear reference when dialog closes
        onlineUsersListView = null;
    }

    /**
     * Open chat with a specific user
     */
    private void openChatWithUser(String userName) {
        // First, try to find existing conversation with this user
        for (String conv : conversationListView.getItems()) {
            if (conv.toLowerCase().contains(userName.toLowerCase())) {
                // Select and open this conversation
                conversationListView.getSelectionModel().select(conv);
                openConversation(conv);
                statusLabel.setText("Chatting with: " + userName);
                return;
            }
        }
        
        // Conversation not found - need to create one or get user ID
        // For now, we'll request the server to create/find a private conversation
        createOrOpenPrivateConversation(userName);
    }
    
    /**
     * Create or open a private conversation with a user
     */
    private void createOrOpenPrivateConversation(String userName) {
        // Find the user ID from cached online users
        if (cachedOnlineUsers == null) {
            showAlert("Error", "Please try again. User information not loaded.");
            return;
        }
        
        int targetUserId = -1;
        for (int i = 0; i < cachedOnlineUsers.length(); i++) {
            JSONObject user = cachedOnlineUsers.getJSONObject(i);
            if (user.getString("username").equalsIgnoreCase(userName)) {
                targetUserId = user.getInt("userId");
                break;
            }
        }
        
        if (targetUserId == -1) {
            showAlert("Error", "Could not find user: " + userName);
            return;
        }
        
        // Send request to server to create or get private conversation
        Message createConvMsg = new Message(Message.CREATE_CONVERSATION);
        createConvMsg.put("conversationType", "PRIVATE");
        createConvMsg.put("targetUserId", targetUserId);
        serverConnection.sendMessage(createConvMsg);
        
        statusLabel.setText("Creating conversation with " + userName + "...");
        
        // Wait for conversation list to update and then open it
        new Thread(() -> {
            try {
                Thread.sleep(500);
                Platform.runLater(() -> {
                    // Refresh conversation list
                    requestConversationList();
                    
                    // Wait a bit more for the list to update
                    new Thread(() -> {
                        try {
                            Thread.sleep(300);
                            Platform.runLater(() -> {
                                // Try to open the conversation again
                                for (String conv : conversationListView.getItems()) {
                                    if (conv.toLowerCase().contains(userName.toLowerCase())) {
                                        conversationListView.getSelectionModel().select(conv);
                                        openConversation(conv);
                                        statusLabel.setText("Chatting with: " + userName);
                                        return;
                                    }
                                }
                                statusLabel.setText("Ready");
                            });
                        } catch (InterruptedException e) {
                            // Ignore
                        }
                    }).start();
                });
            } catch (InterruptedException e) {
                // Ignore
            }
        }).start();
    }

    /**
     * Request conversation list from server
     */
    private void requestConversationList() {
        serverConnection.sendMessage(Message.listConversationsRequest());
    }

    /**
     * Update conversation list view
     */
    private void updateConversationList(Message message) {
        conversationListView.getItems().clear();
        
        JSONArray conversations = message.getData().getJSONArray("conversations");
        
        for (int i = 0; i < conversations.length(); i++) {
            JSONObject conv = conversations.getJSONObject(i);
            int id = conv.getInt("conversationId");
            String name = conv.getString("name");
            conversationListView.getItems().add(id + ": " + name);
        }
    }

    /**
     * Open a conversation
     */
    private void openConversation(String item) {
        String[] parts = item.split(":");
        currentConversationId = Integer.parseInt(parts[0].trim());
        String name = parts.length > 1 ? parts[1].trim() : "Chat";
        
        conversationTitleLabel.setText(name + " (Conversation ID: " + currentConversationId + ")");
        chatArea.clear();
        messageField.setDisable(false);
        
        // Enable both send and attach buttons
        HBox inputBox = (HBox) messageField.getParent();
        inputBox.getChildren().forEach(node -> {
            if (node instanceof Button) {
                node.setDisable(false);
            }
        });

        // Load history
        Message loadHistoryMsg = Message.loadHistoryRequest(currentConversationId, 50, 0);
        serverConnection.sendMessage(loadHistoryMsg);
        
        statusLabel.setText("💡 Tip: To download a file, type '/download <fileId>' in the message box");
    }
    
    /**
     * Attach and send a file
     */
    private void attachAndSendFile() {
        if (currentConversationId == null) {
            showAlert("No Conversation", "Please select a conversation first.");
            return;
        }
        
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select File to Send");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("All Files", "*.*"),
            new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif"),
            new FileChooser.ExtensionFilter("Documents", "*.pdf", "*.doc", "*.docx", "*.txt"),
            new FileChooser.ExtensionFilter("Archives", "*.zip", "*.rar", "*.7z")
        );
        
        File selectedFile = fileChooser.showOpenDialog(primaryStage);
        if (selectedFile == null) {
            return; // User cancelled
        }
        
        // Check file size (max 50MB)
        long maxSize = 50 * 1024 * 1024; // 50 MB
        if (selectedFile.length() > maxSize) {
            showAlert("File Too Large", 
                String.format("File size is %.2f MB. Maximum allowed is 50 MB.", 
                selectedFile.length() / (1024.0 * 1024.0)));
            return;
        }
        
        // Show progress dialog
        Alert progressAlert = new Alert(Alert.AlertType.INFORMATION);
        progressAlert.setTitle("Sending File");
        progressAlert.setHeaderText("Uploading: " + selectedFile.getName());
        progressAlert.setContentText("Please wait...");
        progressAlert.show();
        
        // Send file in background thread
        new Thread(() -> {
            try {
                byte[] fileData = Files.readAllBytes(selectedFile.toPath());
                String base64Data = Base64.getEncoder().encodeToString(fileData);
                
                Message fileMsg = new Message(Message.SEND_FILE);
                fileMsg.put("conversationId", currentConversationId);
                fileMsg.put("filename", selectedFile.getName());
                fileMsg.put("fileSize", selectedFile.length());
                fileMsg.put("fileData", base64Data);
                
                serverConnection.sendMessage(fileMsg);
                
                Platform.runLater(() -> {
                    progressAlert.close();
                    statusLabel.setText("File uploaded successfully. Waiting for confirmation...");
                });
                
            } catch (IOException e) {
                Platform.runLater(() -> {
                    progressAlert.close();
                    showAlert("File Error", "Failed to read file: " + e.getMessage());
                });
            }
        }).start();
    }
    
    /**
     * Show alert dialog
     */
    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.show();
    }

    /**
     * Display chat history
     */
    private void displayHistory(Message message) {
        chatArea.clear();
        
        JSONArray messages = message.getData().getJSONArray("messages");
        
        for (int i = 0; i < messages.length(); i++) {
            JSONObject msg = messages.getJSONObject(i);
            int messageId = msg.getInt("messageId");
            String sender = msg.getString("senderName");
            String text = msg.getString("text");
            String timestamp = msg.getString("timestamp");
            
            appendMessage(sender, text, timestamp);
        }
        
        // Note: File metadata is stored separately and needs to be loaded from database
        // For now, file messages show as text with [File: filename] format
    }

    /**
     * Display a new incoming message
     */
    private void displayNewMessage(Message message) {
        int convId = message.getInt("conversationId");
        
        // Only display if it's for the current conversation
        if (currentConversationId != null && currentConversationId == convId) {
            String sender = message.getString("senderName");
            String text = message.getString("text");
            String timestamp = message.getString("timestamp");
            
            appendMessage(sender, text, timestamp);
        }
    }

    /**
     * Append a message to the chat area
     */
    private void appendMessage(String sender, String text, String timestamp) {
        String formattedTime = formatTimestamp(timestamp);
        String line = String.format("[%s] %s: %s\n", formattedTime, sender, text);
        chatArea.appendText(line);
    }

    /**
     * Format timestamp for display
     */
    private String formatTimestamp(String timestamp) {
        try {
            return timestamp.substring(11, 19); // Extract HH:MM:SS
        } catch (Exception e) {
            return timestamp;
        }
    }

    /**
     * Send a message
     */
    private void sendMessage() {
        if (currentConversationId == null) {
            return;
        }

        String text = messageField.getText().trim();
        if (text.isEmpty()) {
            return;
        }
        
        // Check for special commands
        if (text.startsWith("/download ")) {
            try {
                String fileIdStr = text.substring(10).trim();
                int fileId = Integer.parseInt(fileIdStr);
                downloadFile(fileId);
                messageField.clear();
                return;
            } catch (Exception e) {
                showAlert("Invalid Command", "Usage: /download <fileId>\nExample: /download 123");
                messageField.clear();
                return;
            }
        }

        Message msg = Message.sendMessageRequest(currentConversationId, text);
        serverConnection.sendMessage(msg);

        messageField.clear();
    }

    /**
     * Logout and return to login screen
     */
    private void logout() {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Logout");
        confirmation.setHeaderText("Are you sure you want to logout?");
        confirmation.setContentText("You will be disconnected from the server.");
        
        confirmation.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                // Disconnect from server
                if (serverConnection != null) {
                    serverConnection.disconnect();
                    serverConnection = null;
                }
                
                // Reset session
                userId = null;
                username = null;
                currentConversationId = null;
                
                // Show login screen
                showLoginScreen();
            }
        });
    }

    @Override
    public void stop() {
        if (serverConnection != null) {
            serverConnection.disconnect();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
