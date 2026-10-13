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
import javafx.stage.Stage;
import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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

        root.getChildren().addAll(titleLabel, usernameField, passwordField, errorLabel, buttonBox);

        // Login action
        loginButton.setOnAction(e -> {
            String user = usernameField.getText().trim();
            String pass = passwordField.getText();
            
            if (user.isEmpty() || pass.isEmpty()) {
                errorLabel.setText("Please enter username and password");
                return;
            }

            errorLabel.setText("Connecting...");
            new Thread(() -> login(user, pass, errorLabel)).start();
        });

        // Register action
        registerButton.setOnAction(e -> {
            String user = usernameField.getText().trim();
            String pass = passwordField.getText();
            
            if (user.isEmpty() || pass.isEmpty()) {
                errorLabel.setText("Please enter username and password");
                return;
            }

            errorLabel.setText("Connecting...");
            new Thread(() -> register(user, pass, errorLabel)).start();
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
    private void register(String username, String password, Label errorLabel) {
        // Connect to server
        String host = ConfigUtil.getServerHost();
        int port = ConfigUtil.getServerPort();
        
        serverConnection = new ServerConnection(host, port);
        if (!serverConnection.connect()) {
            Platform.runLater(() -> errorLabel.setText("Cannot connect to server"));
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
    private void login(String username, String password, Label errorLabel) {
        // Connect to server
        String host = ConfigUtil.getServerHost();
        int port = ConfigUtil.getServerPort();
        
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

        sidebar.getChildren().addAll(sidebarTitle, conversationListView);
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

        messageField.setOnAction(e -> sendMessage());

        inputBox.getChildren().addAll(messageField, sendButton);

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
        
        conversationTitleLabel.setText(name);
        chatArea.clear();
        messageField.setDisable(false);
        ((Button) messageField.getParent().getChildrenUnmodifiable().get(1)).setDisable(false);

        // Load history
        Message loadHistoryMsg = Message.loadHistoryRequest(currentConversationId, 50, 0);
        serverConnection.sendMessage(loadHistoryMsg);
    }

    /**
     * Display chat history
     */
    private void displayHistory(Message message) {
        chatArea.clear();
        
        JSONArray messages = message.getData().getJSONArray("messages");
        
        for (int i = 0; i < messages.length(); i++) {
            JSONObject msg = messages.getJSONObject(i);
            String sender = msg.getString("senderName");
            String text = msg.getString("text");
            String timestamp = msg.getString("timestamp");
            
            appendMessage(sender, text, timestamp);
        }
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
