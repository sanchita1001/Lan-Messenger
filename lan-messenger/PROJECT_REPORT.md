# LAN Messenger and File Transfer System

## Java Project Report

**Project Title:** LAN Messenger and File Transfer System

**Under the Guidance of:**  
Dr. Jayendra Barua  
Thapar Institute of Engineering and Technology

**Submitted By:**
- 1024031020 - Sanchita Jain
- 1024031024 - Dhwani Agarwal  
- 1024031025 - Yuvakshi Sood

**Department:** Computer Science Engineering  
**Institution:** Thapar Institute of Engineering and Technology, Patiala

**In Partial Fulfilment of the Requirements for the Degree of**  
Bachelor of Engineering in Computer Engineering

**Date:** November 2026

---

## Abstract

Offices, laboratories and campuses often need to exchange short messages and files between computers that share one network, yet most popular messengers route every message through an external cloud service. This makes them unsuitable where privacy matters, where internet access is restricted or where connectivity is unreliable.

The LAN Messenger and File Transfer System is a Java desktop application that lets users on the same local area network chat in real time and share files without depending on a public messaging service. It follows a client-server design. A multithreaded Java TCP server, built on sockets and standard Java threading, accepts many simultaneous client connections. Clients are JavaFX desktop applications styled with CSS. User accounts, conversations, messages and file metadata are stored in a MySQL database accessed through JDBC, and passwords are protected with BCrypt hashing. The application supports registration and login, online/offline presence, private one-to-one chats, group chat rooms, persistent chat history and file transfer with size validation and progress display.

The project is built incrementally with Maven, with the server, client, network, database, model, service, and utility code kept in separate packages.

**Keywords:** Java, JavaFX, TCP sockets, multithreading, client-server, MySQL, JDBC, BCrypt, LAN, file transfer

---

## Table of Contents

1. [Introduction](#chapter-1-introduction)
2. [System Design](#chapter-2-system-design)
3. [Implementation](#chapter-3-implementation)
4. [Results and Screenshots](#chapter-4-results-and-screenshots)
5. [References](#references)

---

## Chapter 1: Introduction

### 1.1 Introduction to the Project

A local area network connects many computers within one building or campus. Users on that network frequently need to talk to each other and pass documents around, and doing so through e-mail or an internet messenger is slow, exposes internal communication to third-party servers and fails when the internet is down.

The LAN Messenger and File Transfer System addresses this by keeping all traffic inside the local network. One machine runs the server; every user runs the JavaFX client and connects to the server's IP address and port (5000 by default). Users register, log in, see who is online, chat privately or in groups, and send files. The application is organized around three ideas:

1. The server is the single authority for authentication, message delivery and storage
2. Every client request is handled on its own thread so one slow user never blocks the others
3. The user interface is updated only on the JavaFX Application Thread so that it stays responsive

### 1.2 Problem Statement

Communication inside a local network is usually handled with tools that were not designed for it. The following table maps the main problems to the features of this system.

| Problem | How the application addresses it |
|---------|----------------------------------|
| Cloud messengers expose internal conversations to external servers | All messages travel over TCP inside the LAN to a server the organisation controls |
| Messaging stops when the internet is unavailable | Only local connectivity is needed |
| Several users must be served at the same time | Multithreaded server using standard Java threading, with one thread per client |
| Anyone could impersonate another user | Registration with unique usernames, login with password verification, BCrypt hashing |
| Knowing who is reachable | Online/offline status shown in the contact sidebar |
| Sending documents needs e-mail or USB drives | Built-in file transfer with size validation, progress display and server-side storage |
| Conversations are lost when a window closes | Messages stored in MySQL and reloaded in chronological order on login |
| Team discussions need more than two people | Group chat rooms with broadcast to all members |

### 1.3 Objectives of the Project

1. Provide real-time one-to-one and group messaging over TCP sockets
2. Support many concurrent users through a multithreaded server
3. Authenticate users securely using hashed passwords and session tracking
4. Allow files to be transferred between users on the LAN, with size limits and progress feedback
5. Persist users, conversations, messages and file metadata in a relational database
6. Provide a clean JavaFX desktop interface with message display, contact sidebar and connection status
7. Keep the code modular so that features can be added incrementally

### 1.4 Scope of the Project

The system is designed for a trusted LAN: one server machine and any number of client machines on the same network. It needs JDK 21, Maven 3.9 or later and MySQL Server 8.0 or later. The server port, upload folder and maximum file size (50 MB by default) are set in a configuration file. The server runs with Maven command on `com.lanmessenger.server.ServerApp`, and clients with `com.lanmessenger.client.ClientApp`; the Windows firewall must allow inbound TCP port 5000 on the server.

**In scope:** Registration and login, presence, private and group chat, chat history, file transfer, and a desktop UI.

**Out of scope in the present version:** Encryption of network traffic (communication is plain TCP), internet-wide access, mobile clients, voice and video calls, message search pagination, read receipts and an administration panel.

---

## Chapter 2: System Design

### 2.1 System Architecture

The application uses a three-tier client-server architecture:

```
┌─────────────────┐         ┌─────────────────┐         ┌─────────────────┐
│   JavaFX Client │◄───────►│  Java TCP Server│◄───────►│  MySQL Database │
│   (Desktop GUI) │  Socket │  (Multithreaded)│  JDBC   │   (Persistence) │
└─────────────────┘         └─────────────────┘         └─────────────────┘
```

**Components:**

1. **JavaFX client** (`client`, `resources/fxml`, `resources/css`). Shows the login, registration and chat screens, collects input, opens a socket to the server and updates the interface when data arrives.

2. **Network layer** (`network`). Defines how requests and responses travel between client and server over the socket using length-prefixed JSON protocol.

3. **Multithreaded server** (`server`, `services`). ServerApp listens on the configured port and spawns a new thread for each accepted connection. Services validate requests, apply authorisation checks and broadcast updates to the connected members of a conversation.

4. **Persistence layer** (`database`, `models`). JDBC repositories using prepared statements read and write the MySQL tables; model classes carry data between layers.

5. **Utilities** (`utils`). Shared helpers such as configuration loading and password hashing.

**Communication flow:**

1. The client sends a request through its TCP socket
2. The server validates and processes the request on a worker thread
3. Relevant data is stored in MySQL through JDBC
4. The server broadcasts the update to the connected clients concerned
5. Each client receives the update and refreshes its interface on the JavaFX Application Thread

### 2.2 Use Case Diagram

The actor is the user of the messenger; the server acts as a supporting system. 

**Use cases:** 
- Register
- Log in
- View online users
- Start private chat
- Create/join group chat
- Send message
- View chat history
- Send file
- Receive file
- Log out

### 2.3 Class Diagram

The main classes grouped by package:

**Server Package:**
- `ServerApp` - Entry point, accepts connections
- `ClientHandler` - Handles one client connection
- `OnlineUserRegistry` - Tracks connected users

**Client Package:**
- `ClientApp` - JavaFX application entry point
- `ServerConnection` - Manages TCP connection to server

**Network Package:**
- `Message` - Protocol message wrapper
- `MessageProtocol` - Serialization/deserialization

**Database Package:**
- `DatabaseConnection` - Connection factory
- `UserDAO` - User operations
- `ConversationDAO` - Conversation operations
- `MessageDAO` - Message operations
- `FileDAO` - File metadata operations

**Models Package:**
- `User`
- `Conversation`
- `ChatMessage`
- `FileMetadata`

**Services Package:**
- `AuthService` - Authentication logic
- `MessageService` - Message routing logic
- `FileService` - File storage logic

**Utils Package:**
- `ConfigUtil` - Configuration loading
- `PasswordUtil` - Password hashing

### 2.4 Activity Diagram

Typical session flow:

1. Start the server and connect to the database
2. Start the client
3. Register or log in
4. If credentials are wrong, show an error and retry
5. Load contacts and previous conversations
6. Choose a chat
7. Send a message or file
8. The server stores it and forwards it to online recipients
9. Log out and the server marks the user offline

### 2.5 Database Design / ER Diagram

The schema is created from `database/schema.sql` in a MySQL database named `lan_messenger` (character set utf8mb4). It has five tables:

| Table | Purpose |
|-------|---------|
| `users` | User accounts with unique usernames and BCrypt-hashed passwords; created_at and last_seen timestamps |
| `conversations` | Containers for private and group chats |
| `conversation_members` | Many-to-many link between users and conversations |
| `messages` | Message text with timestamp, sender and conversation |
| `files` | File metadata attached to messages; the file itself is stored on disk in the uploads folder |

**Relationships:**

- A user can belong to many conversations and a conversation has many members (through `conversation_members`)
- A conversation contains many messages; each message has exactly one sender and one conversation
- A file belongs to one message, and each message has at most one file because `message_id` is unique in the files table

**Design notes:**

- Passwords are never stored in plain text
- Actual file contents stay on the server's disk and only metadata (name, size, path, owner) is stored in MySQL
- All queries use prepared statements
- The schema uses InnoDB with ON DELETE CASCADE foreign keys
- Composite primary key on `conversation_members`
- Index on messages by conversation and time for fast history loading
- FULLTEXT index on message_text prepares for message search

### 2.6 Module Description

| Module | Purpose and main functionality |
|--------|-------------------------------|
| **User Authentication** | Registration with unique username, login with BCrypt verification, session and online/offline status |
| **Real-Time Messaging** | Private chat, group chat, timestamps, broadcast to conversation members over TCP |
| **Chat History** | Stores messages in MySQL, reloads them in order on login |
| **File Sharing** | File chooser, transfer to the server, size validation, progress display, metadata in database |
| **User Interface** | Message display, contact sidebar, connection status, scrollable history |
| **Connection Management** | Accepts clients, one thread per connection, detects disconnects, updates presence |

---

## Chapter 3: Implementation

### 3.1 Technologies and Tools Used

| Technology | Version | Purpose |
|------------|---------|---------|
| Java (JDK) | 21 | Implementation language |
| JavaFX | 21.0.1 | Desktop user interface |
| Java TCP sockets | JDK built-in | Client-server communication |
| Java Threading | JDK built-in | Concurrent client handling |
| MySQL | 8.0+ | Persistent storage |
| JDBC with MySQL Connector/J | 8.0.33 | Database access |
| BCrypt | 0.10.2 (at.favre.lib) | Password hashing |
| JSON | 20231013 (org.json) | Protocol serialization |
| Maven | 3.9+ | Build and dependency management |

### 3.2 Project Structure / Package Structure

```
lan-messenger/
├── pom.xml
├── README.md
├── ARCHITECTURE.md
├── database/schema.sql
├── uploads/
└── src/main/
    ├── java/com/lanmessenger/
    │   ├── client/         JavaFX client application
    │   ├── server/         ServerApp and connection handling
    │   ├── network/        Socket communication
    │   ├── database/       JDBC access
    │   ├── models/         Data classes
    │   ├── services/       Business logic
    │   └── utils/          Helpers
    └── resources/
        ├── fxml/           JavaFX layouts
        ├── css/            Stylesheets
        └── config.properties
```

Each package has one responsibility. `config.properties` holds the database URL and credentials, server port, upload directory and maximum file size.

### 3.3 Description of Classes and Interfaces

**Server Classes:**

| Class | Responsibility |
|-------|----------------|
| `ServerApp` | Entry point: loads configuration, connects to the database, opens the server socket on port 5000 and spawns a thread for each accepted connection |
| `ClientHandler` | Reads requests from one client socket and dispatches them to services. Runs in its own thread. |
| `OnlineUserRegistry` | Thread-safe registry of connected users |

**Service Classes:**

| Class | Responsibility |
|-------|----------------|
| `AuthService` | User registration and login validation |
| `MessageService` | Message creation, history retrieval, conversation management |
| `FileService` | File storage and retrieval |

**Client Classes:**

| Class | Responsibility |
|-------|----------------|
| `ClientApp` | JavaFX entry point: manages application lifecycle, scene transitions |
| `ServerConnection` | Sends requests and listens for incoming messages on a background thread |

**Database Classes:**

| Class | Responsibility |
|-------|----------------|
| `DatabaseConnection` | JDBC connection factory |
| `UserDAO` | User CRUD operations |
| `ConversationDAO` | Conversation and membership operations |
| `MessageDAO` | Message storage and retrieval |
| `FileDAO` | File metadata operations |

**Network Classes:**

| Class | Responsibility |
|-------|----------------|
| `Message` | JSON message wrapper with factory methods |
| `MessageProtocol` | Length-prefixed protocol implementation |

**Model Classes:**

| Class | Responsibility |
|-------|----------------|
| `User` | User entity with id, username |
| `Conversation` | Conversation entity with type (PRIVATE/GROUP) |
| `ChatMessage` | Message entity with text, timestamp, sender |
| `FileMetadata` | File information |

### 3.4 Implementation of Major Modules

#### A. User Authentication

1. **Registration** checks that the username is unique and stores the BCrypt hash of the password, never the password itself
2. **Login** compares the entered password with the stored hash using BCrypt; wrong credentials are rejected
3. On successful login the user is marked online and other clients are notified; on disconnect the status becomes offline

**Code Example:**
```java
// Registration
String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt(12));
userDAO.createUser(username, hashedPassword);

// Login
User user = userDAO.getUserByUsername(username);
if (user != null && BCrypt.checkpw(password, user.getPasswordHash())) {
    return user; // Login successful
}
return null; // Invalid credentials
```

#### B. Server and Multithreading

1. The server blocks on `accept()` in its main loop. Each accepted socket spawns a new `ClientHandler` thread, so many users are served in parallel
2. Shared structures such as `OnlineUserRegistry` use `ConcurrentHashMap` for thread safety
3. When a client closes its connection or the socket fails, the handler removes it and broadcasts the offline status

**Code Example:**
```java
while (running) {
    Socket clientSocket = serverSocket.accept();
    ClientHandler handler = new ClientHandler(clientSocket, userRegistry);
    new Thread(handler).start();
}
```

#### C. Real-Time Messaging

1. A message request carries the conversation, sender and text. The server validates the sender, saves the message with a timestamp, and forwards it to every online member of the conversation
2. Private chats are conversations with two members; group chats have more. The same code path serves both

**Code Example:**
```java
ChatMessage message = messageService.sendMessage(conversationId, senderId, text);
List<User> members = messageService.getConversationMembers(conversationId);

for (User member : members) {
    ClientHandler handler = userRegistry.getClientHandler(member.getUserId());
    if (handler != null) {
        handler.sendMessage(newMessageBroadcast);
    }
}
```

#### D. Chat History

1. When a user logs in, the client requests their conversations and the stored messages
2. Messages are returned in chronological order using an indexed query and shown in a scrollable view

**SQL Query:**
```sql
SELECT m.message_id, m.sender_id, u.username as sender_name, 
       m.message_text, m.sent_at
FROM messages m
INNER JOIN users u ON m.sender_id = u.user_id
WHERE m.conversation_id = ?
ORDER BY m.sent_at DESC
LIMIT 50;
```

#### E. File Sharing

1. The sender picks a file with the JavaFX file chooser. The client checks the size against the limit (50 MB by default)
2. The file is encoded as Base64 and sent over the socket
3. The server saves it in the `uploads/` directory; its metadata is stored in the `files` table and linked to a message
4. Recipient receives file metadata and can download by requesting the file ID

**Code Example:**
```java
// Client sends file
byte[] fileData = Files.readAllBytes(file.toPath());
String base64Data = Base64.getEncoder().encodeToString(fileData);
Message fileMsg = new Message("SEND_FILE");
fileMsg.put("filename", file.getName());
fileMsg.put("fileData", base64Data);

// Server saves file
byte[] fileBytes = Base64.getDecoder().decode(base64Data);
Path storagePath = Paths.get("uploads", UUID.randomUUID() + "_" + filename);
Files.write(storagePath, fileBytes);
```

#### F. Client-Side Concurrency

1. Network reads run on a background thread so the window never freezes
2. Anything that touches the interface is passed to the JavaFX Application Thread with `Platform.runLater`

**Code Example:**
```java
// Listener thread receives message
Message message = protocol.receiveMessage();

// Update UI on JavaFX thread
Platform.runLater(() -> {
    chatArea.appendText(message.getString("text") + "\n");
});
```

### 3.5 Exception Handling

| Situation | Mechanism |
|-----------|-----------|
| Server not running or wrong IP/port | The client catches the connection IOException and shows an error message; the connection status indicator changes |
| Client disconnects abruptly | The handler catches IOException / SocketException, closes the socket, removes the user and broadcasts offline status |
| Database failure | SQLException is caught in the database layer and reported to the caller; resources are closed with try-with-resources |
| Invalid input | Empty or duplicate usernames and oversized files are rejected with an error message |
| Configuration errors | Missing or invalid config.properties stops the server with a clear message |

### 3.6 Collection Framework and Concurrency Usage

| API | Use in the project |
|-----|-------------------|
| `ConcurrentHashMap` | Connected users registry for thread-safe access |
| `List`, `ArrayList` | Messages, contacts, conversation members |
| `Streams` | Filtering and mapping lists |
| Standard Java Threads | One thread per client connection |

### 3.7 Database Connectivity

1. **Connection:** The JDBC URL, username and password come from `config.properties` (for example `jdbc:mysql://localhost:3306/lan_messenger`)
2. **Prepared statements:** Every query binds values with `PreparedStatement`, which prevents SQL injection
3. **Setup:** `database/schema.sql` creates the tables; a dedicated MySQL user with privileges only on the `lan_messenger` database is recommended

**Code Example:**
```java
String sql = "SELECT * FROM users WHERE username = ?";
PreparedStatement stmt = conn.prepareStatement(sql);
stmt.setString(1, username);
ResultSet rs = stmt.executeQuery();
```

### 3.8 Important Code Snippets

**1. Server Accept Loop:**
```java
while (running) {
    Socket clientSocket = serverSocket.accept();
    System.out.println("[SERVER] Client connected: " + 
        clientSocket.getInetAddress());
    ClientHandler handler = new ClientHandler(clientSocket, userRegistry);
    new Thread(handler).start();
}
```

**2. Password Hashing with BCrypt:**
```java
// Hash password on registration
String hashedPassword = BCrypt.hashpw(plainPassword, BCrypt.gensalt(12));

// Verify password on login
boolean valid = BCrypt.checkpw(enteredPassword, storedHash);
```

**3. Message Broadcast:**
```java
List<User> members = messageService.getConversationMembers(conversationId);
Message broadcast = new Message("NEW_MESSAGE");
broadcast.put("text", messageText);
broadcast.put("senderId", senderId);

for (User member : members) {
    ClientHandler handler = userRegistry.getClientHandler(member.getUserId());
    if (handler != null) {
        handler.sendMessage(broadcast);
    }
}
```

**4. File Upload with Validation:**
```java
if (fileSize > MAX_FILE_SIZE) {
    return error("FILE_TOO_LARGE");
}

String safeFilename = UUID.randomUUID() + "_" + originalFilename;
Path storagePath = Paths.get("uploads", safeFilename);
Files.write(storagePath, fileData);

fileDAO.saveFileMetadata(messageId, originalFilename, 
    storagePath.toString(), fileSize);
```

**5. JDBC Prepared Statement:**
```java
String sql = "INSERT INTO messages (conversation_id, sender_id, message_text) VALUES (?, ?, ?)";
try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
    stmt.setInt(1, conversationId);
    stmt.setInt(2, senderId);
    stmt.setString(3, text);
    stmt.executeUpdate();
    
    ResultSet rs = stmt.getGeneratedKeys();
    if (rs.next()) {
        int messageId = rs.getInt(1);
        // Use messageId
    }
}
```

**6. Updating UI with Platform.runLater:**
```java
// In background thread
Message message = receiveMessage();

// Switch to JavaFX thread for UI update
Platform.runLater(() -> {
    chatArea.appendText(message.getString("text") + "\n");
    statusLabel.setText("New message received");
});
```

---

## Chapter 4: Results and Screenshots

### 4.1 Main Application Screen

The main chat window features:
- Left sidebar showing conversations list
- Center area displaying message history
- Input field with Send and Attach File buttons
- Status bar showing online status and connection information
- New Chat button for starting conversations

### 4.2 Functional Screenshots

**Required Screenshots:**
1. Registration screen and duplicate-username error
2. Login screen with server IP configuration
3. Contact list showing online and offline users
4. Private conversation between two clients
5. Group chat with three or more members
6. File selection and transfer progress
7. Received file shown in the chat
8. Server console log showing connections
9. MySQL tables showing hashed passwords and stored messages

### 4.3 Input and Output Demonstrations

**Server Console on Start-up:**
```
==========================================
LAN Messenger Server
==========================================
[CONFIG] Loaded configuration from file system
[SERVER] Testing database connection...
[SERVER] Database connection successful!
[SERVER] Server listening on port 5000
[SERVER] Waiting for client connections...
```

**Server Console During Operation:**
```
[SERVER] Client connected from /192.168.1.101
[ClientHandler] New client connected from /192.168.1.101
[ClientHandler] Received: LOGIN from user null
[ClientHandler] User 1 (alice) logged in
[ClientHandler] Received: LIST_CONVERSATIONS from user 1
[ClientHandler] Received: SEND_MESSAGE from user 1
[ClientHandler] Received: SEND_FILE from user 1
[ClientHandler] File transfer: document.pdf (1024 KB) to conversation 1
```

**Test Cases:**

| No. | Test | Expected Result | Actual Result |
|-----|------|----------------|---------------|
| 1 | Register with new username | Account created, password hashed | ✅ Pass |
| 2 | Register with existing username | Rejected with error message | ✅ Pass |
| 3 | Login with correct credentials | Login succeeds, user marked online | ✅ Pass |
| 4 | Login with wrong password | Rejected with authentication failure | ✅ Pass |
| 5 | Check database | Passwords stored as BCrypt hashes | ✅ Pass |
| 6 | Send private message | Recipient receives instantly | ✅ Pass |
| 7 | Send group message to 3+ users | All members receive it | ✅ Pass |
| 8 | Close and reopen client, log in | Previous messages loaded in order | ✅ Pass |
| 9 | Send a small file | Recipient receives, saved with metadata | ✅ Pass |
| 10 | Close a client | Others see it offline | ✅ Pass |
| 11 | Run 3+ clients simultaneously | No loss or duplication of messages | ✅ Pass |
| 12 | Create new conversation | Appears automatically for both users | ✅ Pass |
| 13 | Download file later using /download command | File downloads successfully | ✅ Pass |

### 4.4 Error Handling Demonstrations

1. **Connection Error:** Connecting while the server is stopped gives "Cannot connect to server" error
2. **File Size Limit:** Sending a file above 50 MB limit is rejected with "File Too Large" error
3. **Firewall Block:** A blocked firewall port produces connection timeout; fix is an inbound TCP rule for port 5000
4. **Invalid Credentials:** Wrong username or password shows "Invalid username or password" error
5. **Unauthorized Access:** Attempting to send message to conversation user is not member of shows "Not authorized" error

### 4.5 Limitations and Future Scope

**Limitations:**

1. Network traffic is sent as plain TCP and is not encrypted, so the application is intended for trusted local networks
2. The server must be running before clients connect
3. Clients need to manually enter the server's IP address
4. No message search or pagination in current version
5. File transfer limited to 50 MB per file

**Future Scope:**

1. **Security:** TLS/SSL encryption or end-to-end encryption
2. **Features:** Message search and pagination, read receipts, typing indicators, emoji support, profile pictures
3. **Functionality:** Message editing and deletion, desktop notifications, voice and video calling
4. **Platform:** Mobile clients (Android/iOS), web client
5. **Administration:** Admin panel for user management, server monitoring
6. **Performance:** HikariCP connection pooling, Redis caching, message queue for reliability
7. **Testing:** JUnit/Mockito automated tests, CI/CD pipeline
8. **Deployment:** Docker containerization, Kubernetes orchestration

**Summary:** The LAN Messenger and File Transfer System successfully delivers authenticated, real-time private and group messaging, persistent history and file sharing over a local network, using Java sockets, multithreaded server architecture, JavaFX and MySQL. All core features are implemented and tested.

---

## References

[1] Oracle, "Java Platform, Standard Edition 21 API Specification." https://docs.oracle.com/en/java/javase/21/docs/api/

[2] Oracle, "The Java Tutorials: Custom Networking (Sockets)." https://docs.oracle.com/javase/tutorial/networking/

[3] Oracle, "The Java Tutorials: Concurrency." https://docs.oracle.com/javase/tutorial/essential/concurrency/

[4] OpenJFX, "JavaFX Documentation." https://openjfx.io/

[5] Oracle, "MySQL 8.0 Reference Manual." https://dev.mysql.com/doc/refman/8.0/en/

[6] Oracle, "The Java Tutorials: JDBC Database Access." https://docs.oracle.com/javase/tutorial/jdbc/

[7] The Apache Software Foundation, "Apache Maven Documentation." https://maven.apache.org/guides/

[8] Provos and Mazieres, "A Future-Adaptable Password Scheme" (bcrypt), USENIX 1999.

[9] Favre, Florian. "BCrypt Java Library." https://github.com/patrickfav/bcrypt

[10] "org.json: A reference implementation of a JSON package in Java." https://github.com/stleary/JSON-java

---

**End of Report**

---

## Appendices

### Appendix A: Installation Guide

**Prerequisites:**
- JDK 21
- Maven 3.9+
- MySQL Server 8.0+

**Setup Steps:**

1. Create database:
```bash
mysql -u root -p < database/schema.sql
```

2. Configure application:
Edit `src/main/resources/config.properties`:
```properties
db.url=jdbc:mysql://localhost:3306/lan_messenger
db.username=root
db.password=your_password
server.port=5000
file.max.size=52428800
```

3. Build project:
```bash
mvn clean install
```

4. Start server:
```bash
mvn exec:java -Dexec.mainClass="com.lanmessenger.server.ServerApp"
```

5. Start clients:
```bash
mvn exec:java -Dexec.mainClass="com.lanmessenger.client.ClientApp"
```

### Appendix B: Configuration Options

| Property | Default | Description |
|----------|---------|-------------|
| db.url | jdbc:mysql://localhost:3306/lan_messenger | Database connection URL |
| db.username | root | Database username |
| db.password | - | Database password |
| server.port | 5000 | TCP port for server |
| server.host | localhost | Server hostname for clients |
| file.upload.dir | uploads | Directory for file storage |
| file.max.size | 52428800 | Maximum file size (50 MB) |

### Appendix C: Message Protocol Specification

**Message Format:** Length-prefixed JSON

**Structure:**
```
[4-byte length (big-endian)][JSON payload (UTF-8)]
```

**Message Types:**

Client → Server:
- REGISTER, LOGIN, SEND_MESSAGE, LOAD_HISTORY, LIST_CONVERSATIONS, LIST_ONLINE_USERS, CREATE_CONVERSATION, SEND_FILE, REQUEST_FILE, HEARTBEAT, LOGOUT

Server → Client:
- AUTH_SUCCESS, AUTH_FAILURE, NEW_MESSAGE, HISTORY_RESPONSE, CONVERSATION_LIST, CONVERSATION_CREATED, ONLINE_USERS_LIST, USER_STATUS, FILE_TRANSFER, FILE_DATA, ERROR

### Appendix D: Database Schema

**Tables:** users, conversations, conversation_members, messages, files

**Key Relationships:**
- Users ↔ Conversations (many-to-many via conversation_members)
- Conversations → Messages (one-to-many)
- Messages → Sender (many-to-one to users)
- Messages ↔ Files (one-to-one, optional)

**Character Set:** utf8mb4 for full Unicode support including emojis

---

**Project Status:** Phase 1 Complete  
**Version:** 1.0.0  
**Last Updated:** November 23, 2026
