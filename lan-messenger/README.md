# LAN Messenger Chat Application

A real-time desktop chat application for local area networks built with Java, JavaFX, and MySQL.

## ⚡ Quick Start (For Windows)

### Prerequisites
- JDK 21 installed
- Maven installed  
- MySQL Server installed and running

### Setup Steps

1. **Create the database:**
```bash
mysql -u root -p < database/schema.sql
```

2. **Configure the application:**
Edit `src/main/resources/config.properties` and set your MySQL password:
```properties
db.password=your_mysql_root_password
```

3. **Build the project:**
```bash
mvn clean install
```

4. **Start the server:**
```bash
mvn exec:java@server
```

5. **Start clients** (in separate terminals):
```bash
mvn exec:java@client
```

That's it! Register a user, login, and start chatting!

## Overview

LAN Messenger allows multiple users connected to the same Wi-Fi network or LAN to communicate in real-time. The application features user authentication, private messaging, group chats, file sharing, and persistent chat history.

## Technology Stack

- **Language**: Java 21
- **GUI**: JavaFX with CSS styling
- **Networking**: Java Socket Programming (TCP)
- **Concurrency**: Multi-threaded server with ExecutorService
- **Database**: MySQL 8.0+
- **Database Connectivity**: JDBC with MySQL Connector/J
- **Build Tool**: Maven
- **Security**: BCrypt password hashing

## Features

### ✅ User Authentication
- Secure user registration with unique usernames
- Login with password verification
- BCrypt password hashing (never stores plain-text passwords)
- Session management with online/offline status

### ✅ Real-Time Messaging
- Instant message delivery using TCP sockets
- Private one-on-one conversations
- Group chat rooms
- Online/offline user status indicators
- Message timestamps
- Broadcast to all conversation members

### ✅ Persistent Chat History
- All messages saved to MySQL database
- Load previous conversations on login
- Chronological message ordering
- Search functionality (future enhancement)
- Pagination for older messages (future enhancement)

### ✅ File Sharing
- Select and send files through JavaFX file chooser
- Transfer files over LAN between users
- Display file names, sizes, and transfer progress
- Files stored on server disk with metadata in database
- Validation of file sizes and permissions

### ✅ Modern User Interface
- Clean, professional desktop design
- Message bubbles with sender identification
- Scrollable chat history
- Contact sidebar
- Search functionality
- Connection status indicators
- Responsive layout

## System Architecture

```
┌─────────────────┐         ┌─────────────────┐         ┌─────────────────┐
│   JavaFX Client │◄───────►│  Java TCP Server│◄───────►│  MySQL Database │
│   (Desktop GUI) │  Socket │  (Multithreaded)│  JDBC   │   (Persistence) │
└─────────────────┘         └─────────────────┘         └─────────────────┘
```

**Communication Flow:**
1. Client sends protocol message to server via TCP socket
2. Server validates and processes the request
3. Server persists data to MySQL database
4. Server broadcasts updates to relevant connected clients
5. Clients receive updates and refresh UI on JavaFX Application Thread

## Database Schema

### Tables
1. **users** - User accounts with hashed passwords
2. **conversations** - Private and group chat containers
3. **conversation_members** - Many-to-many relationship between users and conversations
4. **messages** - All chat messages with timestamps
5. **files** - File metadata (actual files stored on disk)

### Relationships
- Users can participate in multiple conversations
- Conversations contain multiple messages
- Messages belong to one conversation and one sender
- Files are attached to specific messages

## Project Structure

```
lan-messenger/
├── pom.xml                          # Maven configuration
├── README.md                        # This file
├── ARCHITECTURE.md                  # Detailed architecture documentation
├── .gitignore                       # Git ignore rules
├── database/
│   └── schema.sql                   # Database setup script
├── uploads/                         # Server file storage (gitignored)
└── src/
    └── main/
        ├── java/com/lanmessenger/
        │   ├── client/              # Client application
        │   ├── server/              # Server application
        │   ├── network/             # Protocol definitions
        │   ├── database/            # JDBC data access layer
        │   ├── models/              # Domain objects
        │   ├── services/            # Business logic
        │   ├── controllers/         # JavaFX controllers
        │   └── utils/               # Helper utilities
        └── resources/
            ├── fxml/                # JavaFX layouts
            ├── css/                 # Stylesheets
            └── config.properties    # Configuration (gitignored)
```

## Prerequisites

### Required Software
1. **JDK 21** - [Download from Oracle](https://www.oracle.com/java/technologies/downloads/#java21) or use OpenJDK
2. **Maven 3.9+** - [Download from Apache Maven](https://maven.apache.org/download.cgi)
3. **MySQL 8.0+** - [Download MySQL Community Server](https://dev.mysql.com/downloads/mysql/)
4. **MySQL Workbench** (optional) - [Download](https://dev.mysql.com/downloads/workbench/)

### Development Tools (Optional)
- **IntelliJ IDEA** or **VS Code** with Java extensions
- **Git** for version control

## Installation and Setup

### Step 1: Install JDK 21

**Windows:**
1. Download JDK 21 installer from Oracle or Adoptium
2. Run the installer and follow the wizard
3. Set `JAVA_HOME` environment variable:
   - Right-click "This PC" → Properties → Advanced system settings
   - Environment Variables → New System Variable
   - Variable name: `JAVA_HOME`
   - Variable value: `C:\Program Files\Java\jdk-21` (adjust path)
4. Add to PATH: `%JAVA_HOME%\bin`
5. Verify: Open Command Prompt and run `java -version`

### Step 2: Install Maven

**Windows:**
1. Download Maven binary zip from Apache
2. Extract to `C:\Program Files\Apache\maven`
3. Add to PATH: `C:\Program Files\Apache\maven\bin`
4. Verify: `mvn -version`

### Step 3: Install and Configure MySQL

1. **Install MySQL Server:**
   - Download MySQL Installer for Windows
   - Choose "Server only" or "Full" installation
   - Set root password (remember this!)
   - Keep default port 3306
   - Configure as Windows Service (recommended)

2. **Create Database:**
   ```bash
   # Open MySQL Command Line Client or MySQL Workbench
   mysql -u root -p
   ```
   
   ```sql
   CREATE DATABASE lan_messenger CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```

3. **Run Schema Script:**
   ```bash
   # From lan-messenger directory
   mysql -u root -p lan_messenger < database/schema.sql
   ```

4. **Create Application User (recommended):**
   ```sql
   CREATE USER 'lanmessenger'@'localhost' IDENTIFIED BY 'your_secure_password';
   GRANT ALL PRIVILEGES ON lan_messenger.* TO 'lanmessenger'@'localhost';
   FLUSH PRIVILEGES;
   ```

### Step 4: Configure Application

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

**Important:** Add `config.properties` to `.gitignore` to avoid committing credentials!

### Step 5: Build the Project

```bash
cd lan-messenger
mvn clean install
```

This will:
- Download all dependencies
- Compile Java source code
- Run tests (if present)
- Create executable JAR files

## Running the Application

### Start the Server

The server must be running before any clients can connect.

**Option 1: Using Maven**
```bash
mvn exec:java -Dexec.mainClass="com.lanmessenger.server.ServerApp"
```

**Option 2: Using JAR**
```bash
java -cp target/lan-messenger-1.0.jar com.lanmessenger.server.ServerApp
```

**Expected Output:**
```
[SERVER] LAN Messenger Server starting...
[SERVER] Database connection established
[SERVER] Server listening on port 5000
[SERVER] Waiting for client connections...
```

### Find Your Server IP Address

Clients need to know the server's IP address to connect.

**Windows Command Prompt:**
```bash
ipconfig
```

Look for "IPv4 Address" under your active network adapter (usually starts with 192.168.x.x or 10.x.x.x).

Example: `192.168.1.100`

### Start Client(s)

You can run multiple clients on the same or different computers.

**On the same computer (testing):**
```bash
# Terminal 1
mvn exec:java -Dexec.mainClass="com.lanmessenger.client.ClientApp"

# Terminal 2
mvn exec:java -Dexec.mainClass="com.lanmessenger.client.ClientApp"
```

**On different computers:**
1. Copy the entire `lan-messenger` folder to other computers
2. Ensure JDK 21 and Maven are installed
3. Edit `src/main/resources/config.properties` to point to server IP:
   ```properties
   server.host=192.168.1.100
   server.port=5000
   ```
4. Run: `mvn exec:java -Dexec.mainClass="com.lanmessenger.client.ClientApp"`

### Configure Windows Firewall

If clients cannot connect, allow the server port through Windows Firewall:

1. Open Windows Defender Firewall
2. Advanced settings → Inbound Rules → New Rule
3. Port → TCP → Specific local port: 5000
4. Allow the connection
5. Apply to Domain, Private, and Public (or only Private for LAN)
6. Name: "LAN Messenger Server"

## Testing Checklist

### User Authentication
- [ ] Register a new user with unique username
- [ ] Attempt to register duplicate username (should fail)
- [ ] Login with correct credentials
- [ ] Login with incorrect credentials (should fail)
- [ ] Verify password is hashed in database (not plain text)

### Real-Time Messaging
- [ ] Send message from User A to User B in private conversation
- [ ] Verify User B receives message instantly
- [ ] Check message appears in both users' chat windows
- [ ] Send message in group chat with 3+ users
- [ ] Verify all group members receive the message

### Chat History
- [ ] Close client and reopen
- [ ] Login again and verify previous messages are loaded
- [ ] Verify messages appear in chronological order
- [ ] Check timestamps are correct

### File Sharing
- [ ] Select and send a small file (e.g., image)
- [ ] Verify recipient receives file metadata
- [ ] Check file is saved in uploads/ directory
- [ ] Verify file metadata is in database

### Connection Management
- [ ] Disconnect client (close window) and verify server detects disconnection
- [ ] Verify other users see offline status
- [ ] Reconnect and verify online status updates

### Multiple Clients
- [ ] Run 3+ clients simultaneously
- [ ] Send messages between all combinations
- [ ] Verify no message loss or duplication

## Troubleshooting

### "Connection refused" Error
- **Cause:** Server is not running or wrong IP/port
- **Solution:** Verify server is running, check IP address with `ipconfig`, ensure firewall allows port 5000

### "Access denied for user" Database Error
- **Cause:** Wrong database credentials
- **Solution:** Check `config.properties` username/password match MySQL user

### JavaFX Runtime Components Missing
- **Cause:** JavaFX not included in JDK
- **Solution:** Maven automatically downloads JavaFX dependencies. Ensure you run `mvn clean install`

### Port Already in Use
- **Cause:** Another application is using port 5000
- **Solution:** Change `server.port` in `config.properties` or kill the other process

### Messages Not Appearing
- **Cause:** Client/server version mismatch or protocol error
- **Solution:** Ensure all clients and server are running the same version. Check server logs for errors.

## Security Notes

### Current Implementation
- ✅ BCrypt password hashing
- ✅ Prepared SQL statements (SQL injection prevention)
- ✅ Server-side authorization checks
- ✅ Input validation
- ✅ File upload size limits
- ⚠️ **Plain TCP** (no encryption)

### Production Recommendations
- **Use TLS/SSL:** Encrypt all network communication using `SSLSocket`
- **Rate Limiting:** Prevent spam and DoS attacks
- **Session Tokens:** Use JWT or similar instead of persistent socket authentication
- **HTTPS for Web Version:** If building web client in future
- **Database User Permissions:** Limit lanmessenger user to only required privileges
- **Regular Updates:** Keep dependencies updated for security patches

### Important Warning
This application transmits data in **plain text** over the network. It is suitable for:
- Trusted local area networks (home, small office)
- Development and testing environments
- Educational purposes

**Do NOT use over public networks or the internet without adding TLS encryption.**

## Future Enhancements

### Planned Features
- [ ] Message search functionality
- [ ] Pagination for loading older messages
- [ ] Read receipts and typing indicators
- [ ] User profile pictures
- [ ] Emoji support
- [ ] Voice/video calling (requires WebRTC or similar)
- [ ] End-to-end encryption
- [ ] Mobile client (Android/iOS)
- [ ] Desktop notifications
- [ ] Message editing and deletion
- [ ] User blocking and reporting
- [ ] Admin panel for server management

### Technical Improvements
- [ ] Connection pooling for database (HikariCP)
- [ ] Redis for session management and caching
- [ ] Message queue for reliable delivery (RabbitMQ/Kafka)
- [ ] Load balancing for multiple server instances
- [ ] WebSocket support for web clients
- [ ] Automated testing (JUnit, Mockito)
- [ ] CI/CD pipeline
- [ ] Docker containerization
- [ ] Kubernetes orchestration

## Contributing

This project is for educational purposes. If you'd like to contribute:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is created for educational purposes. Feel free to use and modify as needed.

## Support

For questions or issues:
1. Check the troubleshooting section above
2. Review the ARCHITECTURE.md for technical details
3. Check server logs for error messages
4. Verify all prerequisites are correctly installed

## Acknowledgments

Built as a comprehensive learning project demonstrating:
- Java networking with sockets
- Multi-threaded server design
- JavaFX desktop application development
- JDBC database integration
- Client-server architecture
- Real-time communication protocols

---

**Version:** 1.0.0  
**Last Updated:** October 13, 2026  
**Status:** In Development (Phase 1 Complete)
