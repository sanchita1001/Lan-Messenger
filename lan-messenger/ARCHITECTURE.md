# LAN Messenger - Architecture Documentation

## Table of Contents
1. [System Overview](#system-overview)
2. [Component Architecture](#component-architecture)
3. [Database Design](#database-design)
4. [Network Protocol](#network-protocol)
5. [Thread Model](#thread-model)
6. [Security Architecture](#security-architecture)
7. [Message Flow](#message-flow)

---

## System Overview

LAN Messenger implements a **centralized client-server architecture** with three distinct tiers:

```
┌──────────────────────────────────────────────────────────────────────┐
│                         APPLICATION LAYER                             │
├──────────────────────────────────────────────────────────────────────┤
│                                                                       │
│  ┌─────────────────┐         ┌─────────────────┐                    │
│  │  JavaFX Client  │         │  JavaFX Client  │                    │
│  │   (User A)      │         │   (User B)      │   ... N Clients   │
│  └────────┬────────┘         └────────┬────────┘                    │
│           │                           │                              │
│           │  TCP Socket (JSON/UTF-8) │                              │
│           └───────────┬───────────────┘                              │
│                       │                                              │
├───────────────────────┼──────────────────────────────────────────────┤
│                       │                                              │
│              ┌────────▼────────┐                                     │
│              │  Java TCP Server│                                     │
│              │  (Multi-threaded)│                                    │
│              │                 │                                     │
│              │ • Auth Service  │                                     │
│              │ • Message Router│                                     │
│              │ • Online Registry│                                    │
│              └────────┬────────┘                                     │
│                       │                                              │
│                       │  JDBC (Connection Pool)                      │
│                       │                                              │
├───────────────────────┼──────────────────────────────────────────────┤
│              ┌────────▼────────┐                                     │
│              │  MySQL Database │                                     │
│              │                 │                                     │
│              │ • users         │                                     │
│              │ • conversations │                                     │
│              │ • messages      │                                     │
│              │ • files         │                                     │
│              └─────────────────┘                                     │
└──────────────────────────────────────────────────────────────────────┘
```

### Design Principles

1. **Separation of Concerns**: UI, business logic, networking, and data access are isolated
2. **Single Source of Truth**: Server is authoritative; clients are presentation layers
3. **Centralized Control**: All message routing goes through the server
4. **Persistent State**: Database stores all application state
5. **Thread Safety**: Proper synchronization for concurrent access
6. **Fail-Safe**: Graceful error handling and connection recovery

---

## Component Architecture

### 1. JavaFX Client Application

**Purpose**: User interface and client-side coordination

#### Key Classes

##### `ClientApp.java`
- **Role**: Application entry point, JavaFX lifecycle management
- **Responsibilities**:
  - Initialize JavaFX stage
  - Load login scene
  - Manage scene transitions
  - Handle application shutdown

##### `ServerConnection.java`
- **Role**: TCP connection management
- **Responsibilities**:
  - Establish socket connection to server
  - Send protocol messages
  - Receive and parse incoming messages
  - Maintain message listener thread
  - Handle reconnection logic
  - Delegate UI updates to JavaFX Application Thread

##### Controllers (`LoginController`, `RegisterController`, `ChatController`)
- **Role**: FXML controller classes
- **Responsibilities**:
  - Handle user input events
  - Update JavaFX nodes
  - Invoke ServerConnection methods
  - Display validation errors
  - Manage UI state

#### Client Thread Model

```
┌─────────────────────────────────────┐
│   JavaFX Application Thread         │  ← All UI updates
│   (Event dispatch, rendering)       │
└─────────────────────────────────────┘
              ▲
              │ Platform.runLater()
              │
┌─────────────┴───────────────────────┐
│   Message Listener Thread           │  ← Blocking socket read
│   (Continuous read loop)            │
└─────────────────────────────────────┘
              ▲
              │ Socket InputStream
              │
┌─────────────┴───────────────────────┐
│   Send Operations (Any Thread)      │  ← Synchronized writes
│   (Triggered by user actions)       │
└─────────────────────────────────────┘
```

**Concurrency Strategy**:
- **UI Thread**: Only touch JavaFX nodes on this thread
- **Listener Thread**: Blocks on socket reads, dispatches updates via `Platform.runLater()`
- **Send Operations**: Use synchronized block around `OutputStream.write()`

---

### 2. Java TCP Server

**Purpose**: Central message broker, authentication, and coordination

#### Key Classes

##### `ServerApp.java`
- **Role**: Server entry point and main accept loop
- **Responsibilities**:
  - Initialize database connection pool
  - Bind to server socket
  - Accept client connections in loop
  - Spawn ClientHandler for each connection
  - Handle graceful shutdown

##### `ClientHandler.java`
- **Role**: Per-client connection thread
- **Responsibilities**:
  - Authenticate client
  - Read incoming messages in loop
  - Validate and process requests
  - Invoke appropriate service methods
  - Broadcast messages to recipients
  - Handle client disconnection
  - Update online registry

##### `OnlineUserRegistry.java`
- **Role**: Thread-safe mapping of user IDs to active connections
- **Responsibilities**:
  - Register authenticated clients
  - Look up clients by user ID
  - Remove disconnected clients
  - Provide online user list
  - Synchronize access with `ConcurrentHashMap`

#### Server Thread Model

```
┌─────────────────────────────────────┐
│   Main Server Thread                │
│   (Accept loop)                     │
└────────┬────────────────────────────┘
         │
         ├──► ClientHandler Thread 1 (User A)
         │
         ├──► ClientHandler Thread 2 (User B)
         │
         ├──► ClientHandler Thread 3 (User C)
         │
         └──► ... N threads
```

Each `ClientHandler` runs independently, handling one client's requests.

**Concurrency Strategy**:
- **Thread-per-client**: Simple, scales to hundreds of clients
- **Shared Registry**: `ConcurrentHashMap` for thread-safe user lookup
- **Database Pool**: HikariCP connection pool (future enhancement)
- **Synchronized Writes**: Each ClientHandler synchronizes on its socket output stream

---

### 3. Database Layer

**Purpose**: Persistent storage and data access

#### Key Classes (Data Access Objects)

##### `DatabaseConnection.java`
- **Role**: Connection factory
- **Responsibilities**:
  - Load database credentials from config
  - Provide JDBC connections
  - Future: Integrate HikariCP connection pool

##### `UserDAO.java`
- **Role**: User table operations
- **Methods**:
  - `createUser(username, passwordHash)`
  - `getUserByUsername(username)`
  - `updateLastSeen(userId)`
  - `isUsernameTaken(username)`

##### `ConversationDAO.java`
- **Role**: Conversation and membership operations
- **Methods**:
  - `createConversation(type, name)`
  - `addMember(conversationId, userId)`
  - `getConversationsForUser(userId)`
  - `getConversationMembers(conversationId)`
  - `isUserMemberOfConversation(userId, conversationId)`

##### `MessageDAO.java`
- **Role**: Message operations
- **Methods**:
  - `saveMessage(conversationId, senderId, text)`
  - `getMessageHistory(conversationId, limit, offset)`
  - `searchMessages(conversationId, query)`

##### `FileDAO.java`
- **Role**: File metadata operations
- **Methods**:
  - `saveFileMetadata(messageId, filename, path, size)`
  - `getFileMetadata(fileId)`

#### Connection Management

**Current**: Simple `DriverManager.getConnection()` per request  
**Future**: HikariCP pool with min/max connections, health checks, timeouts

---

## Database Design

### Entity-Relationship Diagram

```
┌──────────────┐
│    users     │
├──────────────┤
│ user_id (PK) │
│ username (U) │
│ password_hash│
│ created_at   │
│ last_seen    │
└──────┬───────┘
       │
       │ 1:M (sender)
       │
       ┌────────────────────┐
       │                    │
       │                    ▼
       │            ┌────────────────┐
       │            │    messages    │
       │            ├────────────────┤
       │            │ message_id (PK)│
       │            │ conversation_id│◄─────┐
       │            │ sender_id (FK) │      │
       │            │ message_text   │      │
       │            │ sent_at        │      │
       │            └────────┬───────┘      │
       │                     │              │
       │                     │ 1:1          │
       │                     ▼              │
       │            ┌────────────────┐      │
       │            │     files      │      │
       │            ├────────────────┤      │
       │            │ file_id (PK)   │      │ M:1
       │            │ message_id (FK)│      │
       │            │ original_name  │      │
       │            │ storage_path   │      │
       │            │ file_size      │      │
       │            └────────────────┘      │
       │                                    │
       │ M:N                                │
       ▼                                    │
┌──────────────────────┐           ┌───────┴───────────┐
│conversation_members  │           │  conversations    │
├──────────────────────┤           ├───────────────────┤
│ conversation_id (PK) │◄──────────│ conversation_id   │
│ user_id (PK, FK)     │           │ type (ENUM)       │
│ joined_at            │           │ name              │
└──────────────────────┘           │ created_at        │
                                   └───────────────────┘
```

### Table Details

#### `users`
```sql
CREATE TABLE users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_seen TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_username (username),
    INDEX idx_last_seen (last_seen)
);
```

**Design Decisions**:
- `user_id` as surrogate key allows username changes (future enhancement)
- `UNIQUE` constraint on username prevents duplicates
- Index on `username` optimizes login queries
- `password_hash` stores BCrypt output (60 chars), 255 allows algorithm changes
- `last_seen` tracks activity, indexed for "online users" queries

#### `conversations`
```sql
CREATE TABLE conversations (
    conversation_id INT AUTO_INCREMENT PRIMARY KEY,
    conversation_type ENUM('PRIVATE', 'GROUP') NOT NULL,
    conversation_name VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_type (conversation_type)
);
```

**Design Decisions**:
- Unified table for private and group conversations (polymorphic design)
- `conversation_type` allows different business logic per type
- `conversation_name` NULL for private (derived from member names), set for groups
- Extensible: add channels, broadcasts, etc. in future

#### `conversation_members`
```sql
CREATE TABLE conversation_members (
    conversation_id INT NOT NULL,
    user_id INT NOT NULL,
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (conversation_id, user_id),
    FOREIGN KEY (conversation_id) REFERENCES conversations(conversation_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_user_conversations (user_id, conversation_id)
);
```

**Design Decisions**:
- Composite primary key ensures unique membership
- `ON DELETE CASCADE`: Deleting conversation removes memberships; deleting user removes their participations
- Index on `user_id` first optimizes "get all conversations for user X"
- `joined_at` enables "user joined group on..." timestamps

#### `messages`
```sql
CREATE TABLE messages (
    message_id INT AUTO_INCREMENT PRIMARY KEY,
    conversation_id INT NOT NULL,
    sender_id INT NOT NULL,
    message_text TEXT NOT NULL,
    sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (conversation_id) REFERENCES conversations(conversation_id) ON DELETE CASCADE,
    FOREIGN KEY (sender_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_conversation_time (conversation_id, sent_at DESC),
    FULLTEXT INDEX idx_message_search (message_text)
);
```

**Design Decisions**:
- `TEXT` type supports up to 65,535 bytes (sufficient for chat messages)
- Composite index `(conversation_id, sent_at DESC)` optimizes pagination queries:
  ```sql
  SELECT * FROM messages WHERE conversation_id = ? ORDER BY sent_at DESC LIMIT 50;
  ```
- `FULLTEXT` index enables search:
  ```sql
  SELECT * FROM messages WHERE MATCH(message_text) AGAINST('search term');
  ```
- `ON DELETE CASCADE`: Conversation deletion removes messages

#### `files`
```sql
CREATE TABLE files (
    file_id INT AUTO_INCREMENT PRIMARY KEY,
    message_id INT UNIQUE NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    storage_path VARCHAR(500) NOT NULL,
    file_size BIGINT NOT NULL,
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (message_id) REFERENCES messages(message_id) ON DELETE CASCADE,
    INDEX idx_message (message_id)
);
```

**Design Decisions**:
- 1:1 relationship with messages (each file is a special message)
- `UNIQUE` on message_id enforces one file per message
- `storage_path` stores relative path (e.g., `uploads/2026/10/uuid.pdf`)
- Actual file stored on disk, not in database (performance)
- `BIGINT` for file_size supports up to 9 exabytes

### Normalization Level

The schema is in **Third Normal Form (3NF)**:
- ✅ 1NF: All columns contain atomic values
- ✅ 2NF: No partial dependencies (all non-key attributes depend on entire primary key)
- ✅ 3NF: No transitive dependencies (non-key attributes depend only on primary key)

**Denormalization Considerations**:
For performance, we might denormalize:
- Add `last_message_at` to `conversations` for sorting
- Add `unread_count` to `conversation_members`
- Cache `sender_username` in `messages` to avoid JOIN

---

## Network Protocol

### Protocol Design: Length-Prefixed JSON

Each message consists of:
```
[4-byte length (big-endian integer)][JSON payload (UTF-8)]
```

#### Example Transmission

**Send Message: "Hello, World!"**

```
Length: 85 bytes
Payload: {"type":"SEND_MESSAGE","conversationId":5,"text":"Hello, World!"}

Wire format (hex):
00 00 00 55  7B 22 74 79 70 65 22 3A 22 53 45 4E ...
│          │  └─ JSON starts
└─ 85 in hex
```

#### Why Length-Prefix?

**Alternative 1: Delimiter-based (e.g., newline)**
- ❌ Fails if message contains delimiter
- ❌ Requires escaping

**Alternative 2: Fixed-length fields**
- ❌ Inflexible
- ❌ Wastes bandwidth

**Length-Prefix:**
- ✅ Unambiguous boundary detection
- ✅ Handles any content (Unicode, newlines, etc.)
- ✅ Efficient parsing

### Message Types

#### Client → Server

##### `REGISTER`
```json
{
  "type": "REGISTER",
  "username": "alice",
  "password": "SecurePass123!"
}
```

##### `LOGIN`
```json
{
  "type": "LOGIN",
  "username": "alice",
  "password": "SecurePass123!"
}
```

##### `SEND_MESSAGE`
```json
{
  "type": "SEND_MESSAGE",
  "conversationId": 42,
  "text": "Hello everyone!"
}
```

##### `LOAD_HISTORY`
```json
{
  "type": "LOAD_HISTORY",
  "conversationId": 42,
  "limit": 50,
  "offset": 0
}
```

##### `CREATE_CONVERSATION`
```json
{
  "type": "CREATE_CONVERSATION",
  "conversationType": "GROUP",
  "conversationName": "Project Team",
  "memberIds": [1, 2, 3, 4]
}
```

##### `LIST_CONVERSATIONS`
```json
{
  "type": "LIST_CONVERSATIONS"
}
```

##### `SEND_FILE`
```json
{
  "type": "SEND_FILE",
  "conversationId": 42,
  "filename": "document.pdf",
  "fileSize": 1048576,
  "fileData": "<base64-encoded-bytes>"
}
```

##### `HEARTBEAT`
```json
{
  "type": "HEARTBEAT",
  "timestamp": 1697203200000
}
```

#### Server → Client

##### `AUTH_SUCCESS`
```json
{
  "type": "AUTH_SUCCESS",
  "userId": 5,
  "username": "alice",
  "message": "Login successful"
}
```

##### `AUTH_FAILURE`
```json
{
  "type": "AUTH_FAILURE",
  "error": "Invalid credentials"
}
```

##### `NEW_MESSAGE`
```json
{
  "type": "NEW_MESSAGE",
  "messageId": 1234,
  "conversationId": 42,
  "senderId": 3,
  "senderName": "bob",
  "text": "Hello everyone!",
  "timestamp": "2026-10-13T18:30:00Z"
}
```

##### `HISTORY_RESPONSE`
```json
{
  "type": "HISTORY_RESPONSE",
  "conversationId": 42,
  "messages": [
    {
      "messageId": 1230,
      "senderId": 2,
      "senderName": "charlie",
      "text": "First message",
      "timestamp": "2026-10-13T18:00:00Z"
    },
    {
      "messageId": 1231,
      "senderId": 3,
      "senderName": "bob",
      "text": "Second message",
      "timestamp": "2026-10-13T18:15:00Z"
    }
  ],
  "hasMore": true
}
```

##### `CONVERSATION_LIST`
```json
{
  "type": "CONVERSATION_LIST",
  "conversations": [
    {
      "conversationId": 10,
      "type": "PRIVATE",
      "name": "alice",
      "lastMessageTime": "2026-10-13T17:00:00Z"
    },
    {
      "conversationId": 20,
      "type": "GROUP",
      "name": "Project Team",
      "memberCount": 5,
      "lastMessageTime": "2026-10-13T18:30:00Z"
    }
  ]
}
```

##### `USER_STATUS`
```json
{
  "type": "USER_STATUS",
  "userId": 3,
  "username": "bob",
  "status": "ONLINE"
}
```

##### `FILE_TRANSFER`
```json
{
  "type": "FILE_TRANSFER",
  "messageId": 1235,
  "conversationId": 42,
  "senderId": 5,
  "senderName": "alice",
  "filename": "document.pdf",
  "fileSize": 1048576,
  "fileId": 99,
  "timestamp": "2026-10-13T18:35:00Z"
}
```

##### `ERROR`
```json
{
  "type": "ERROR",
  "code": "UNAUTHORIZED",
  "message": "You are not a member of this conversation"
}
```

### Protocol Implementation

**Sending (Java)**:
```java
public void sendMessage(JSONObject message) throws IOException {
    String json = message.toString();
    byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);
    int length = jsonBytes.length;
    
    synchronized (outputStream) {
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
```

**Receiving (Java)**:
```java
public JSONObject receiveMessage() throws IOException {
    // Read length (4 bytes)
    int length = (inputStream.read() << 24) |
                 (inputStream.read() << 16) |
                 (inputStream.read() << 8) |
                 inputStream.read();
    
    // Read payload
    byte[] jsonBytes = new byte[length];
    int bytesRead = 0;
    while (bytesRead < length) {
        int count = inputStream.read(jsonBytes, bytesRead, length - bytesRead);
        if (count == -1) throw new EOFException();
        bytesRead += count;
    }
    
    String json = new String(jsonBytes, StandardCharsets.UTF_8);
    return new JSONObject(json);
}
```

---

## Thread Model

### Server Threading

```
┌─────────────────────────────────────────────────────────┐
│                    Main Thread                          │
│  - Bind to port                                         │
│  - Accept loop: serverSocket.accept()                   │
│  - Spawn ClientHandler thread per connection            │
└────────────────┬────────────────────────────────────────┘
                 │
                 ├───► ClientHandler-1 (User alice)
                 │     ├─ Read loop: socket.read()
                 │     ├─ Process: authenticate, route messages
                 │     └─ Write: send responses
                 │
                 ├───► ClientHandler-2 (User bob)
                 │     └─ ... same as above
                 │
                 └───► ClientHandler-N
```

**Synchronization Points**:
1. **OnlineUserRegistry**: `ConcurrentHashMap<Integer, ClientHandler>`
2. **Socket OutputStream**: `synchronized(outputStream) { ... }` in each ClientHandler
3. **Database Connections**: Future connection pool will handle

### Client Threading

```
┌─────────────────────────────────────────────────────────┐
│               JavaFX Application Thread                 │
│  - UI rendering                                         │
│  - Event handling (button clicks, etc.)                 │
│  - MUST be used for all UI updates                      │
└────────────────▲────────────────────────────────────────┘
                 │
                 │ Platform.runLater(() -> { updateUI(); })
                 │
┌────────────────┴────────────────────────────────────────┐
│               Message Listener Thread                   │
│  - Blocking read: socket.receiveMessage()               │
│  - Parse JSON                                           │
│  - Dispatch to UI thread                                │
└─────────────────────────────────────────────────────────┘
                 │
                 │ Socket connection
                 ▼
           [Server Socket]
```

**Key Rule**: Never update JavaFX nodes from listener thread!

**Correct**:
```java
// In listener thread
JSONObject msg = receiveMessage();
Platform.runLater(() -> {
    chatArea.appendText(msg.getString("text"));
});
```

**Incorrect** (will crash):
```java
// In listener thread
JSONObject msg = receiveMessage();
chatArea.appendText(msg.getString("text")); // ❌ IllegalStateException
```

---

## Security Architecture

### Password Security

**Hashing Algorithm**: BCrypt with work factor 12

```java
// Registration
String hashedPassword = BCrypt.hashpw(plainPassword, BCrypt.gensalt(12));
// Store hashedPassword in database

// Login
String storedHash = getUserPasswordHash(username);
boolean valid = BCrypt.checkpw(plainPassword, storedHash);
```

**Why BCrypt?**
- ✅ Industry-standard for password hashing
- ✅ Adaptive: can increase work factor as hardware improves
- ✅ Built-in salt generation
- ✅ Resistant to rainbow table and brute-force attacks

**Why Not Others?**
- ❌ MD5/SHA-1: Too fast, vulnerable to GPU cracking
- ❌ Plain SHA-256: No built-in salt, too fast
- ✅ Argon2: Better, but BCrypt is standard in Java ecosystem

### SQL Injection Prevention

**Always use PreparedStatement**:

**Secure**:
```java
String sql = "SELECT * FROM users WHERE username = ?";
PreparedStatement stmt = conn.prepareStatement(sql);
stmt.setString(1, username);
ResultSet rs = stmt.executeQuery();
```

**Vulnerable** (never do this):
```java
String sql = "SELECT * FROM users WHERE username = '" + username + "'";
// If username is: ' OR '1'='1
// SQL becomes: SELECT * FROM users WHERE username = '' OR '1'='1'
```

### Authorization Model

**Server-Side Checks**:
1. **Authentication**: Is this socket associated with a valid user?
2. **Membership**: Is this user a member of the target conversation?
3. **Ownership**: Does this user own the resource they're modifying?

**Example Flow**:
```java
// Client sends: SEND_MESSAGE to conversation 42
// Server:
if (clientHandler.getUserId() == null) {
    return error("NOT_AUTHENTICATED");
}

if (!conversationDAO.isUserMember(clientHandler.getUserId(), 42)) {
    return error("UNAUTHORIZED");
}

// Proceed with message send
```

**Never trust client**:
- ❌ Don't accept user ID from client payload
- ✅ Use authenticated session's user ID

### File Transfer Security

**Validation**:
```java
// Size limit
if (fileSize > MAX_FILE_SIZE) {
    return error("FILE_TOO_LARGE");
}

// Allowed extensions
String ext = getExtension(filename);
if (!ALLOWED_EXTENSIONS.contains(ext)) {
    return error("FILE_TYPE_NOT_ALLOWED");
}

// Safe storage path
String safeFilename = UUID.randomUUID() + "_" + sanitizeFilename(filename);
Path storagePath = Paths.get("uploads", safeFilename);

// Prevent directory traversal
if (!storagePath.normalize().startsWith("uploads")) {
    return error("INVALID_PATH");
}
```

### Network Security

**Current**: Plain TCP (no encryption)
- ✅ Suitable for trusted LANs
- ❌ Not suitable for public networks
- ⚠️ Passwords transmitted in plain text during login

**TLS Upgrade** (future):
```java
// Server
SSLServerSocketFactory sslFactory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
SSLServerSocket sslServerSocket = (SSLServerSocket) sslFactory.createServerSocket(5000);

// Client
SSLSocketFactory sslFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
SSLSocket sslSocket = (SSLSocket) sslFactory.createSocket(host, port);
```

**Certificate Requirements**:
- Generate with `keytool` or Let's Encrypt
- Distribute to clients (or use custom TrustManager)

---

## Message Flow

### Complete Flow: User A Sends Message to User B

```
┌────────┐         ┌────────┐         ┌────────┐         ┌────────┐
│User A  │         │Client A│         │ Server │         │Client B│         ┌────────┐
│ (UI)   │         │        │         │        │         │        │         │Database│
└───┬────┘         └───┬────┘         └───┬────┘         └───┬────┘         └───┬────┘
    │                  │                  │                  │                  │
    │ 1. Types "Hi"    │                  │                  │                  │
    │ 2. Clicks Send   │                  │                  │                  │
    │─────────────────>│                  │                  │                  │
    │                  │                  │                  │                  │
    │                  │ 3. Serialize     │                  │                  │
    │                  │ {"type":"SEND_MESSAGE",             │                  │
    │                  │  "conversationId":10,               │                  │
    │                  │  "text":"Hi"}    │                  │                  │
    │                  │                  │                  │                  │
    │                  │ 4. Send TCP packet                  │                  │
    │                  │─────────────────>│                  │                  │
    │                  │                  │                  │                  │
    │                  │                  │ 5. Validate:     │                  │
    │                  │                  │    - Is A authenticated?            │
    │                  │                  │    - Is A member of conv 10?        │
    │                  │                  │                  │                  │
    │                  │                  │ 6. INSERT message│                  │
    │                  │                  │─────────────────────────────────────>│
    │                  │                  │                  │                  │
    │                  │                  │ 7. Returns message_id, timestamp    │
    │                  │                  │<─────────────────────────────────────│
    │                  │                  │                  │                  │
    │                  │                  │ 8. Lookup members of conv 10        │
    │                  │                  │    (A, B online)                    │
    │                  │                  │                  │                  │
    │                  │ 9. Broadcast     │                  │                  │
    │                  │    {"type":"NEW_MESSAGE",           │                  │
    │                  │     "messageId":500,                │                  │
    │                  │     "conversationId":10,            │                  │
    │                  │     "senderId":1,                   │                  │
    │                  │     "senderName":"alice",           │                  │
    │                  │     "text":"Hi",                    │                  │
    │                  │     "timestamp":"2026-10-13T..."}   │                  │
    │                  │<─────────────────│                  │                  │
    │                  │                  │─────────────────>│                  │
    │                  │                  │                  │                  │
    │                  │10. Parse message │                  │11. Parse message │
    │                  │                  │                  │                  │
    │ 12. Platform.runLater()             │                  │13. Platform.runLater()
    │<─────────────────│                  │                  │<─────────────────│
    │                  │                  │                  │                  │
    │ 13. Update UI    │                  │                  │14. Update UI     │
    │ Show "Hi" in     │                  │                  │ Show "Hi" from   │
    │ sent messages    │                  │                  │ alice            │
    │                  │                  │                  │                  │
```

### Error Handling Flow

**Scenario**: User tries to send message to conversation they're not a member of

```
Client A                     Server                      Database
   │                           │                           │
   │ SEND_MESSAGE(conv=99)     │                           │
   │─────────────────────────> │                           │
   │                           │ Check membership          │
   │                           │─────────────────────────> │
   │                           │ Returns: false            │
   │                           │<───────────────────────── │
   │                           │                           │
   │ ERROR("UNAUTHORIZED")     │                           │
   │<───────────────────────── │                           │
   │                           │                           │
   │ Display error dialog      │                           │
   │                           │                           │
```

---

## Performance Considerations

### Current Bottlenecks

1. **Database Connections**: Creating new connection per query
   - **Solution**: HikariCP connection pool (Phase 10)

2. **Message Broadcasting**: Sequential writes to each client
   - **Solution**: Parallel broadcast with ExecutorService

3. **Large Message History**: Loading all messages at once
   - **Solution**: Pagination with LIMIT/OFFSET (Phase 7)

4. **File Transfer**: Blocking during large uploads
   - **Solution**: Chunked transfer with progress reporting (Phase 9)

### Scalability Limits

**Current Architecture**:
- **Max Clients**: ~1000 (limited by threads, not sockets)
- **Max Messages/sec**: ~1000 (database write throughput)
- **Max File Size**: 50 MB (configurable)

**Scaling Beyond**:
- Use thread pools instead of thread-per-client
- Add message queue (RabbitMQ, Kafka) for async processing
- Shard database by conversation ID
- Add Redis for session state and caching
- Deploy multiple server instances with load balancer

---

## Technology Choices Rationale

| Component | Choice | Alternatives Considered | Rationale |
|-----------|--------|-------------------------|-----------|
| GUI Framework | JavaFX | Swing, AWT, Web (Electron) | Modern, CSS styling, active development |
| Protocol | TCP | UDP, WebSocket, HTTP | Reliable, ordered, built-in flow control |
| Serialization | JSON | XML, Protobuf, Java Serialization | Human-readable, debug-friendly, standard |
| Database | MySQL | PostgreSQL, SQLite, MongoDB | Ubiquitous, ACID, mature tooling |
| Password Hashing | BCrypt | Argon2, PBKDF2, scrypt | Java ecosystem standard, proven |
| Build Tool | Maven | Gradle, Ant | Declarative, widely used, IDE support |
| Threading | Thread-per-client | NIO, Netty, async | Simple, sufficient for LAN scale |

---

**Version**: 1.0.0  
**Phase**: Phase 1 Complete  
**Last Updated**: October 13, 2026

This document will be updated as architecture evolves through subsequent phases.
