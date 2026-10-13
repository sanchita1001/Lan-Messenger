# Build and Run Guide - LAN Messenger

## Complete Setup Instructions for Windows

### Step 1: Install Prerequisites

#### 1.1 Install JDK 21

1. Download JDK 21 from [Oracle](https://www.oracle.com/java/technologies/downloads/#java21) or [Adoptium](https://adoptium.net/)
2. Run the installer
3. **Set JAVA_HOME:**
   - Press `Win + X`, select "System"
   - Click "Advanced system settings" → "Environment Variables"
   - Under "System variables", click "New"
   - Variable name: `JAVA_HOME`
   - Variable value: `C:\Program Files\Java\jdk-21` (adjust to your install path)
4. **Add to PATH:**
   - In "System variables", find "Path", click "Edit"
   - Click "New", add: `%JAVA_HOME%\bin`
5. **Verify:**
   ```cmd
   java -version
   ```
   Should show: `java version "21..."`

#### 1.2 Install Maven

1. Download Maven from [Apache Maven](https://maven.apache.org/download.cgi)
2. Extract to `C:\Program Files\Apache\maven`
3. **Add to PATH:**
   - Environment Variables → System variables → Path → New
   - Add: `C:\Program Files\Apache\maven\bin`
4. **Verify:**
   ```cmd
   mvn -version
   ```

#### 1.3 Install MySQL Server

1. Download [MySQL Installer for Windows](https://dev.mysql.com/downloads/installer/)
2. Run installer, choose "Server only" or "Full"
3. **During setup:**
   - Choose "Development Computer" configuration
   - Set root password (remember this!)
   - Port: 3306 (default)
   - Configure as Windows Service: ✓
   - Start MySQL Server at System Startup: ✓
4. **Verify MySQL is running:**
   ```cmd
   mysql -u root -p
   ```
   Enter your root password

---

### Step 2: Setup the Database

#### 2.1 Create Database

Open Command Prompt in the `lan-messenger` directory:

```cmd
cd lan-messenger
mysql -u root -p < database\schema.sql
```

Enter your MySQL root password when prompted.

#### 2.2 Verify Database Created

```cmd
mysql -u root -p
```

Then in MySQL:
```sql
USE lan_messenger;
SHOW TABLES;
SELECT * FROM users;
```

You should see 3 test users (alice, bob, charlie) with password: `password123`

---

### Step 3: Configure the Application

Edit `src\main\resources\config.properties`:

```properties
# Set your MySQL root password here
db.password=YOUR_MYSQL_ROOT_PASSWORD

# If MySQL is on a different port, change it:
db.url=jdbc:mysql://localhost:3306/lan_messenger?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true

# Server port (change if 5000 is in use)
server.port=5000
```

**Important:** Change `YOUR_MYSQL_ROOT_PASSWORD` to your actual MySQL root password!

---

### Step 4: Build the Project

```cmd
cd lan-messenger
mvn clean install
```

**Expected output:**
```
[INFO] BUILD SUCCESS
[INFO] Total time: XX s
```

If you see errors:
- **MySQL JDBC Driver not found:** Maven will download it automatically (requires internet)
- **Compilation errors:** Ensure JDK 21 is installed correctly
- **JavaFX errors:** Maven will download JavaFX automatically

---

### Step 5: Run the Server

**Option 1: Using Maven (recommended for development)**
```cmd
mvn exec:java -Dexec.mainClass="com.lanmessenger.server.ServerApp"
```

**Option 2: Using the exec plugin shortcut**
```cmd
mvn exec:java@server
```

**Expected output:**
```
===========================================
   LAN Messenger Server                   
===========================================
[CONFIG] Loaded configuration from file system
[SERVER] Testing database connection...
[SERVER] Database connection successful!
[SERVER] Server listening on port 5000
[SERVER] Waiting for client connections...
[SERVER] Press Ctrl+C to shutdown
===========================================
```

**Keep this terminal open!** The server must run for clients to connect.

---

### Step 6: Run the Client

Open a **new** Command Prompt:

**Option 1: Using Maven**
```cmd
cd lan-messenger
mvn exec:java -Dexec.mainClass="com.lanmessenger.client.ClientApp"
```

**Option 2: Using the exec plugin shortcut**
```cmd
mvn exec:java@client
```

A JavaFX window should open with the login screen.

---

### Step 7: Test the Application

#### Test with Sample Users

The database comes with 3 test users:
- Username: `alice` | Password: `password123`
- Username: `bob` | Password: `password123`
- Username: `charlie` | Password: `password123`

**Testing scenario:**

1. **Client 1:** Login as `alice`
2. **Open another client:** In a new terminal, run `mvn exec:java@client` again
3. **Client 2:** Login as `bob`
4. **In Client 1 (alice):**
   - You should see bob in the online users
   - Double-click conversation "1: bob" (private conversation)
   - Type a message and click Send
5. **In Client 2 (bob):**
   - Select conversation "1: alice"
   - You should see alice's message appear

#### Register New Users

1. On the login screen, click "Register"
2. Enter a unique username (3-50 characters, alphanumeric + underscore)
3. Enter a password (minimum 6 characters)
4. Click Register
5. If successful, click Login and use the same credentials

---

### Step 8: Run on Multiple Computers (Same LAN)

#### On the Server Computer:

1. **Find your IP address:**
   ```cmd
   ipconfig
   ```
   Look for "IPv4 Address" under your active network adapter (e.g., `192.168.1.100`)

2. **Allow through Windows Firewall:**
   - Open "Windows Defender Firewall with Advanced Security"
   - Click "Inbound Rules" → "New Rule"
   - Select "Port" → Next
   - TCP, Specific local port: `5000` → Next
   - "Allow the connection" → Next
   - Check all profiles (Domain, Private, Public) → Next
   - Name: "LAN Messenger Server" → Finish

3. **Start the server:**
   ```cmd
   mvn exec:java@server
   ```

4. **Keep the server computer running** while clients are connected

#### On Client Computers:

1. **Copy the project** to the client computer (or clone from Git)

2. **Edit `src\main\resources\config.properties`:**
   ```properties
   server.host=192.168.1.100
   server.port=5000
   ```
   (Replace `192.168.1.100` with the actual server IP)

3. **Build (if needed):**
   ```cmd
   mvn clean install
   ```

4. **Run the client:**
   ```cmd
   mvn exec:java@client
   ```

---

## Running from JAR Files (Optional)

After building with `mvn clean install`, you can find JAR files in the `target/` directory:

**Start server:**
```cmd
java -jar target\lan-messenger-server.jar
```

**Start client:**
```cmd
java -jar target\lan-messenger-client.jar
```

**Note:** Make sure `config.properties` is in `src\main\resources\` and accessible from the JAR location.

---

## Troubleshooting

### "Connection refused" Error
**Problem:** Client cannot connect to server  
**Solutions:**
1. Verify server is running: Check the server terminal shows "Server listening on port 5000"
2. Check IP address: Run `ipconfig` on server computer
3. Check port: Ensure port 5000 is not used by another application
4. Check firewall: Ensure Windows Firewall allows port 5000
5. Ping test: From client computer, run `ping 192.168.1.100` (use actual server IP)

### "Access denied for user 'root'@'localhost'"
**Problem:** Database credentials incorrect  
**Solutions:**
1. Verify MySQL password in `config.properties` is correct
2. Test connection: `mysql -u root -p` and enter password
3. If password is forgotten, reset it using MySQL documentation

### "Table 'lan_messenger.users' doesn't exist"
**Problem:** Database schema not created  
**Solutions:**
1. Verify database exists: `mysql -u root -p`, then `SHOW DATABASES;`
2. Run schema script again: `mysql -u root -p < database\schema.sql`
3. Check for SQL errors in the output

### "JavaFX runtime components are missing"
**Problem:** JavaFX not found  
**Solutions:**
1. Ensure Maven downloaded dependencies: `mvn dependency:resolve`
2. Try rebuilding: `mvn clean install`
3. Verify JDK 21 is being used: `java -version`

### Server closes immediately after starting
**Problem:** Database connection failed  
**Solutions:**
1. Check MySQL service is running: Open Services (services.msc), find "MySQL80", ensure it's Started
2. Verify config.properties has correct credentials
3. Check database exists: `mysql -u root -p`, then `USE lan_messenger;`

### Client window doesn't appear
**Problem:** JavaFX initialization failed  
**Solutions:**
1. Check console for error messages
2. Ensure graphics drivers are up to date
3. Try running with: `mvn javafx:run`

### Messages not appearing in chat
**Problem:** Client/server communication issue  
**Solutions:**
1. Check server logs for errors
2. Verify both users are in the same conversation
3. Try refreshing conversation list
4. Check conversation ID matches

### Port 5000 already in use
**Problem:** Another application is using port 5000  
**Solutions:**
1. Change port in `config.properties`: `server.port=5001`
2. Update client configuration to match
3. Or find and close the application using port 5000: `netstat -ano | findstr :5000`

---

## Development Tips

### Running Server and Client in IDE (IntelliJ IDEA)

1. **Import project:**
   - File → Open → Select `lan-messenger` folder
   - IntelliJ will detect Maven project

2. **Run Server:**
   - Right-click `ServerApp.java` → Run 'ServerApp.main()'

3. **Run Client:**
   - Right-click `ClientApp.java` → Run 'ClientApp.main()'
   - To run multiple clients: Click dropdown next to Run button → Edit Configurations → Check "Allow multiple instances"

### Viewing Database Contents

**Using MySQL Command Line:**
```sql
USE lan_messenger;

-- View users
SELECT * FROM users;

-- View conversations
SELECT * FROM conversations;

-- View messages
SELECT m.message_id, u.username, m.message_text, m.sent_at
FROM messages m
JOIN users u ON m.sender_id = u.user_id
ORDER BY m.sent_at DESC
LIMIT 10;
```

**Using MySQL Workbench:**
1. Open MySQL Workbench
2. Connect to localhost
3. Select `lan_messenger` schema
4. Browse tables in the left panel

---

## Testing Checklist

- [ ] Server starts without errors
- [ ] Database connection successful
- [ ] Client connects to server
- [ ] User registration works
- [ ] User login works
- [ ] Invalid login shows error
- [ ] Conversation list loads
- [ ] Can open a conversation
- [ ] Messages are sent and received
- [ ] Messages persist (close and reopen client)
- [ ] Multiple clients can chat simultaneously
- [ ] User status updates (online/offline)
- [ ] Can run on multiple computers on same network

---

## Next Steps

Once basic functionality is working:

1. **Implement File Sharing** (Phase 9)
2. **Add Group Chat Creation** UI
3. **Improve UI Design** with better styling
4. **Add Features:**
   - Message search
   - User profile pictures
   - Typing indicators
   - Read receipts
   - Emoji support

---

## Performance Notes

- Server can handle ~100 concurrent clients with current architecture
- Message latency on LAN: typically < 50ms
- Database queries are not optimized yet (no connection pooling)

For production use, consider:
- HikariCP connection pool
- Message rate limiting
- Input sanitization
- TLS/SSL encryption
- Better error handling and logging

---

**Need Help?**

Check the following files:
- `README.md` - Project overview
- `ARCHITECTURE.md` - Technical details
- `database/schema.sql` - Database structure and sample queries

Happy chatting! 🚀
