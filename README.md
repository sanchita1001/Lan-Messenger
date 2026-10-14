# LAN Messenger

A real-time desktop chat application for local area networks (LANs), built with Java, JavaFX, and MySQL.

##  Features

### User Authentication
- Secure user registration with unique usernames
- Login with password verification
- BCrypt password hashing
- Session management with online/offline status

### Real-Time Messaging
- Instant message delivery using TCP sockets
- Private one-to-one conversations
- Group chat rooms
- Online/offline user status indicators
- Message timestamps
- Broadcast messages to conversation members

### Persistent Chat History
- Messages stored in a MySQL database
- Previous conversations loaded on login
- Chronological message ordering
- Message search and pagination planned for future enhancements

### File Sharing
- Select files using the JavaFX file chooser
- Transfer files between users over a LAN
- Display file names, sizes, and transfer progress
- Store files on the server with metadata in the database
- Validate file sizes and permissions

### Modern User Interface
- Clean desktop interface
- Message bubbles with sender identification
- Scrollable chat history
- Contact sidebar
- Search functionality
- Connection status indicators
- Responsive layout

##  Technology Stack

| Component | Technology |
|---|---|
| Language | Java 21 |
| GUI | JavaFX with CSS |
| Networking | Java TCP Socket Programming |
| Concurrency | Multithreaded server using ExecutorService |
| Database | MySQL 8.0+ |
| Database Connectivity | JDBC with MySQL Connector/J |
| Build Tool | Maven |
| Security | BCrypt password hashing |

##  Quick Start (Windows)

### Prerequisites

- JDK 21
- Maven 3.9+
- MySQL Server 8.0+

### Installation

**1. Create the database**

From the project root, run:

```bash
mysql -u root -p < database/schema.sql
```

**2. Configure the application**

Edit `src/main/resources/config.properties` and configure your MySQL credentials.

```properties
db.password=your_mysql_root_password
```

**3. Build the project**

```bash
mvn clean install
```

**4. Start the server**

```bash
mvn exec:java@server
```

**5. Start the clients**

Open separate terminals and run:

```bash
mvn exec:java@client
```

Register an account, log in, and start chatting!

##  System Architecture

```text
┌─────────────────┐         ┌─────────────────┐         ┌─────────────────┐
│   JavaFX Client │◄───────►│  Java TCP Server│◄───────►│  MySQL Database │
│   (Desktop GUI) │  Socket │ (Multithreaded) │  JDBC   │  (Persistence)  │
└─────────────────┘         └─────────────────┘         └─────────────────┘
```

### Communication Flow

1. The client sends a request to the server through a TCP socket.
2. The server validates and processes the request.
3. The server stores relevant data in MySQL.
4. The server broadcasts updates to connected clients.
5. Clients receive updates and refresh their interfaces on the JavaFX Application Thread.

##  Database Schema

### Tables

| Table | Description |
|---|---|
| `users` | User accounts with hashed passwords |
| `conversations` | Private and group chat containers |
| `conversation_members` | Relationships between users and conversations |
| `messages` | Messages with timestamps |
| `files` | File metadata; actual files are stored on disk |

### Relationships

- Users can participate in multiple conversations.
- Conversations contain multiple messages.
- Messages belong to one conversation and one sender.
- Files are attached to specific messages.

##  Project Structure

```text
lan-messenger/
├── pom.xml
├── README.md
├── ARCHITECTURE.md
├── .gitignore
├── database/
│   └── schema.sql
├── uploads/
└── src/
    └── main/
        ├── java/com/lanmessenger/
        │   ├── client/
        │   ├── server/
        │   ├── network/
        │   ├── database/
        │   ├── models/
        │   ├── services/
        │   ├── controllers/
        │   └── utils/
        └── resources/
            ├── fxml/
            ├── css/
            └── config.properties
```

##  Detailed Setup Instructions

### Step 1: Install JDK 21

1. Download JDK 21 from Oracle or Adoptium.
2. Install it on your Windows system.
3. Configure the `JAVA_HOME` environment variable.
4. Add `%JAVA_HOME%\bin` to your system `PATH`.
5. Verify the installation:

```bash
java -version
```

### Step 2: Install Maven

1. Download Maven from Apache.
2. Extract the archive to your preferred directory.
3. Add the Maven `bin` directory to your system `PATH`.
4. Verify the installation:

```bash
mvn -version
```

### Step 3: Install and Configure MySQL

Install MySQL Server and configure a root password. The default port is `3306`.

Create the database:

```sql
CREATE DATABASE lan_messenger
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;
```

Run the schema script:

```bash
mysql -u root -p lan_messenger < database/schema.sql
```

Create a dedicated application user:

```sql
CREATE USER 'lanmessenger'@'localhost'
IDENTIFIED BY 'your_secure_password';

GRANT ALL PRIVILEGES ON lan_messenger.*
TO 'lanmessenger'@'localhost';

FLUSH PRIVILEGES;
```

### Step 4: Configure the Application

Create `src/main/resources/config.properties`:

```properties
# Database Configuration
db.url=jdbc:mysql://localhost:3306/lan_messenger?useSSL=false&serverTimezone=UTC
db.username=lanmessenger
db.password=your_secure_password

# Server Configuration
server.port=5000
server.max.clients=100

# File Transfer Configuration
file.upload.dir=uploads
file.max.size=52428800
```

**Important:** Add `config.properties` to `.gitignore` to avoid committing database credentials.

### Step 5: Build the Project

From the project root, run:

```bash
mvn clean install
```

This downloads dependencies, compiles the source code, runs tests if configured, and builds the project artifacts.

##  Running the Application

### Start the Server

The server must be running before clients can connect.

Using Maven:

```bash
mvn exec:java -Dexec.mainClass="com.lanmessenger.server.ServerApp"
```

Alternatively, using a JAR:

```bash
java -cp target/lan-messenger-1.0.jar com.lanmessenger.server.ServerApp
```

Expected output:

```text
[SERVER] LAN Messenger Server starting...
[SERVER] Database connection established
[SERVER] Server listening on port 5000
[SERVER] Waiting for client connections...
```

### Find the Server IP Address

On Windows, run:

```bash
ipconfig
```

Look for the IPv4 address under your active network adapter.

Example: `192.168.1.100`

### Start Client(s)

On the same computer, open a separate terminal for each client:

```bash
mvn exec:java -Dexec.mainClass="com.lanmessenger.client.ClientApp"
```

To connect from another computer:

1. Copy the project folder to the client computer.
2. Ensure JDK 21 and Maven are installed.
3. Configure the client to connect to the server IP address:

```properties
server.host=192.168.1.100
server.port=5000
```

4. Start the client:

```bash
mvn exec:java -Dexec.mainClass="com.lanmessenger.client.ClientApp"
```

### Configure Windows Firewall

If clients cannot connect:

1. Open Windows Defender Firewall.
2. Navigate to **Advanced settings → Inbound Rules → New Rule**.
3. Select **Port**, then **TCP**, and enter `5000`.
4. Allow the connection.
5. Apply the rule to the appropriate network profiles. For a trusted LAN, prefer the Private profile.
6. Name the rule `LAN Messenger Server`.

##  Testing Checklist

### User Authentication
- [ ] Register a new user with a unique username.
- [ ] Attempt to register a duplicate username.
- [ ] Log in with correct credentials.
- [ ] Verify that incorrect passwords are rejected.
- [ ] Verify that passwords are hashed in the database.

### Real-Time Messaging
- [ ] Send a private message between two users.
- [ ] Verify that the recipient receives the message instantly.
- [ ] Confirm the message appears in both chat windows.
- [ ] Send a group message to three or more users.
- [ ] Verify that all group members receive the message.

### Chat History
- [ ] Close and reopen a client.
- [ ] Log in again and verify previous messages are loaded.
- [ ] Verify chronological ordering.
- [ ] Check message timestamps.

### File Sharing
- [ ] Select and send a small file.
- [ ] Verify the recipient receives the file.
- [ ] Check that the file is saved in the `uploads/` directory.
- [ ] Verify file metadata in the database.

### Connection Management
- [ ] Close a client and verify the server detects disconnection.
- [ ] Verify that other users see the offline status.
- [ ] Reconnect and verify online status updates.

### Multiple Clients
- [ ] Run three or more clients simultaneously.
- [ ] Send messages between different clients.
- [ ] Verify that messages are delivered without loss or duplication.

## 🛠️ Troubleshooting

### Connection Refused

**Possible causes:** The server is not running, or the IP address or port is incorrect.

**Solutions:**
- Verify that the server is running.
- Check the IP address using `ipconfig`.
- Ensure the firewall allows TCP port `5000`.

### Database Access Denied

**Possible cause:** Incorrect database credentials.

**Solution:** Check the username and password in `config.properties`.

### JavaFX Runtime Components Missing

**Possible cause:** Required JavaFX dependencies are missing.

**Solution:** Verify the JavaFX dependencies in `pom.xml` and run:

```bash
mvn clean install
```

### Port Already in Use

**Possible cause:** Another application is using port `5000`.

**Solution:** Change `server.port` in `config.properties` or stop the process using that port.

### Messages Not Appearing

**Possible causes:** Client/server version mismatch or a protocol error.

**Solution:** Ensure the server and all clients use the same version. Check the server logs for errors.

##  Security

### Current Security Measures

- BCrypt password hashing
- Prepared SQL statements to help prevent SQL injection
- Server-side authorization checks
- Input validation
- File upload size limits

### Limitations

Network communication uses plain TCP and is **not encrypted**.

### Production Recommendations

- Use TLS/SSL to encrypt network communication.
- Implement rate limiting to help prevent spam and denial-of-service attacks.
- Use secure session management.
- Restrict database permissions to the privileges the application requires.
- Keep dependencies updated.
- Use HTTPS if a web client is added.

> **Warning:** This application transmits data in plain text over the network. It is intended for trusted local networks, development, testing, and educational use. Do not use it over public networks or the internet without adding TLS encryption.

##  Future Enhancements

### Planned Features

- Message search functionality
- Pagination for older messages
- Read receipts and typing indicators
- User profile pictures
- Emoji support
- Voice and video calling
- End-to-end encryption
- Android/iOS client
- Desktop notifications
- Message editing and deletion
- User blocking and reporting
- Admin panel for server management

### Technical Improvements

- Database connection pooling with HikariCP
- Redis for session management and caching
- RabbitMQ or Kafka for reliable message delivery
- Load balancing for multiple server instances
- WebSocket support for web clients
- Automated testing with JUnit and Mockito
- CI/CD pipeline
- Docker containerization
- Kubernetes orchestration

##  Contributing

Contributions are welcome.

1. Fork the repository.
2. Create a feature branch:

```bash
git checkout -b feature/amazing-feature
```

3. Commit your changes:

```bash
git commit -m "Add some amazing feature"
```

4. Push the branch:

```bash
git push origin feature/amazing-feature
```

5. Open a Pull Request.

##  License

This project was created for educational purposes. Feel free to use and modify it as needed.

##  Support

For questions or issues:

- Review the troubleshooting section.
- Consult `ARCHITECTURE.md` for technical details.
- Check server logs for errors.
- Verify that all prerequisites are installed and configured correctly.

##  Acknowledgments

This project demonstrates:

- Java networking with sockets
- Multithreaded server design
- JavaFX desktop application development
- JDBC database integration
- Client-server architecture
- Real-time communication protocols
