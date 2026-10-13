# Getting Started with LAN Messenger

Welcome! This guide will get you up and running in **5 simple steps**.

---

## ⚡ Quick Start (5 Steps)

### Step 1: Install Prerequisites

You need:
1. **JDK 21** - [Download here](https://www.oracle.com/java/technologies/downloads/#java21)
2. **Maven** - [Download here](https://maven.apache.org/download.cgi)
3. **MySQL Server** - [Download here](https://dev.mysql.com/downloads/mysql/)

**Verify installations:**
```cmd
java -version
mvn -version
mysql --version
```

### Step 2: Setup Database

**Option A: Using the script (easiest)**
```cmd
cd lan-messenger
setup-database.bat
```
Enter your MySQL root password when prompted.

**Option B: Manual setup**
```cmd
mysql -u root -p < database\schema.sql
```

### Step 3: Configure Application

Edit `src\main\resources\config.properties`:
```properties
db.password=your_mysql_password
```
Replace `your_mysql_password` with your actual MySQL root password.

### Step 4: Build Project

```cmd
cd lan-messenger
mvn clean install
```

Wait for "BUILD SUCCESS" message.

### Step 5: Run!

**Terminal 1 - Start Server:**
```cmd
run-server.bat
```
Or: `mvn exec:java -Dexec.mainClass="com.lanmessenger.server.ServerApp"`

**Terminal 2 - Start Client:**
```cmd
run-client.bat
```
Or: `mvn exec:java -Dexec.mainClass="com.lanmessenger.client.ClientApp"`

---

## 🎮 First Time Usage

### Test with Sample Users

The database comes with 3 test users:

| Username | Password |
|----------|----------|
| alice | password123 |
| bob | password123 |
| charlie | password123 |

### Quick Test Scenario

1. **Start the server** (run-server.bat)
2. **Open Client 1:**
   - Run `run-client.bat`
   - Login as **alice** with password **password123**
3. **Open Client 2:**
   - Open another Command Prompt
   - Run `run-client.bat` again
   - Login as **bob** with password **password123**
4. **Send Messages:**
   - In alice's client, double-click on **"1: bob"** in the conversation list
   - Type a message and click Send
   - Watch it appear in bob's window!
5. **Test Persistence:**
   - Close both clients
   - Reopen and login again
   - Your messages are still there!

---

## 📱 What You Can Do

### ✅ Current Features

- **Register** new users
- **Login** with password authentication
- **Send messages** in real-time
- **Private conversations** (1-on-1 chat)
- **Group conversations** (pre-created in database)
- **Message history** (loads automatically)
- **Online status** (see who's online)
- **Multiple clients** (chat from different computers)

### 🎯 How to Use Each Feature

#### Register a New User
1. On login screen, click **Register**
2. Enter username (3-50 chars, letters/numbers/underscore only)
3. Enter password (min 6 chars)
4. Click Register
5. If successful, click Login

#### Send a Message
1. Double-click a conversation in the left sidebar
2. Type your message in the bottom text field
3. Click **Send** or press Enter

#### View Online Users
- Users with a green indicator are online
- Shown in the conversation list

#### Load Message History
- Automatically loads when you open a conversation
- Shows the last 50 messages

---

## 🌐 Running on Multiple Computers

### Setup (One-Time)

**On the Server Computer:**

1. Find your IP address:
   ```cmd
   ipconfig
   ```
   Look for "IPv4 Address" (e.g., `192.168.1.100`)

2. Allow through firewall:
   - Windows Firewall → Advanced Settings → Inbound Rules → New Rule
   - Port → TCP → Port 5000 → Allow

3. Start the server:
   ```cmd
   run-server.bat
   ```

**On Each Client Computer:**

1. Copy the `lan-messenger` folder to the computer

2. Edit `src\main\resources\config.properties`:
   ```properties
   server.host=192.168.1.100
   server.port=5000
   ```
   (Use the actual server IP address)

3. Build (first time only):
   ```cmd
   mvn clean install
   ```

4. Run the client:
   ```cmd
   run-client.bat
   ```

---

## 🛠️ Troubleshooting

### Problem: "Connection refused"

**Cause:** Can't connect to server

**Solutions:**
1. ✅ Verify server is running (check server terminal)
2. ✅ Check `config.properties` has correct `server.host`
3. ✅ Ping the server: `ping 192.168.1.100`
4. ✅ Check firewall allows port 5000

---

### Problem: "Access denied for user 'root'"

**Cause:** Wrong MySQL password

**Solutions:**
1. ✅ Verify password in `config.properties`
2. ✅ Test: `mysql -u root -p` (enter same password)
3. ✅ If forgotten, reset MySQL root password

---

### Problem: "Table 'lan_messenger.users' doesn't exist"

**Cause:** Database not created

**Solutions:**
1. ✅ Run `setup-database.bat` again
2. ✅ Or manually: `mysql -u root -p < database\schema.sql`
3. ✅ Check MySQL service is running

---

### Problem: Client window doesn't open

**Cause:** JavaFX issue

**Solutions:**
1. ✅ Check console for errors
2. ✅ Verify JDK 21 installed: `java -version`
3. ✅ Rebuild: `mvn clean install`

---

### Problem: Server closes immediately

**Cause:** Database connection failed

**Solutions:**
1. ✅ Check MySQL is running (Services → MySQL80)
2. ✅ Verify `config.properties` password
3. ✅ Check database exists: `mysql -u root -p` → `USE lan_messenger;`

---

## 📚 Next Steps

Once you have it working:

1. **Read the Documentation:**
   - `README.md` - Overview
   - `BUILD_AND_RUN.md` - Detailed setup guide
   - `ARCHITECTURE.md` - How it works internally
   - `PROJECT_STATUS.md` - What's implemented

2. **Customize:**
   - Change server port in `config.properties`
   - Add more test users in database
   - Modify UI colors and layout

3. **Enhance:**
   - Add file sharing feature
   - Create group chat UI
   - Implement message search
   - Add better styling

4. **Deploy:**
   - Run on your home network
   - Test with friends on same WiFi
   - Try running from different rooms

---

## 🎯 Project Structure

```
lan-messenger/
├── run-server.bat           ← Start server (double-click)
├── run-client.bat           ← Start client (double-click)
├── setup-database.bat       ← Setup database (run once)
├── pom.xml                  ← Maven configuration
├── database/
│   └── schema.sql          ← Database structure
├── src/main/
│   ├── java/               ← Java source code
│   └── resources/
│       └── config.properties ← Configuration file
└── uploads/                ← File storage directory
```

---

## 💡 Tips

1. **Always start the server first** before starting clients
2. **Keep the server terminal open** while using the application
3. **Multiple clients:** Just run `run-client.bat` multiple times
4. **Test locally first:** Use `localhost` before trying on other computers
5. **Check server logs:** Errors will appear in the server terminal
6. **Database GUI:** Use MySQL Workbench to view data visually

---

## 🏆 Success Checklist

Run through this checklist to verify everything works:

- [ ] Prerequisites installed (Java 21, Maven, MySQL)
- [ ] Database created successfully
- [ ] Config file has correct MySQL password
- [ ] Project builds without errors (`mvn clean install`)
- [ ] Server starts and shows "listening on port 5000"
- [ ] Client GUI opens with login screen
- [ ] Can login as alice/bob with password123
- [ ] Conversation list appears
- [ ] Can open a conversation
- [ ] Can send and receive messages
- [ ] Messages persist after closing and reopening

If all checked ✅ - **Congratulations! You're ready to chat!** 🎉

---

## 📞 Need More Help?

**Detailed Guides:**
- 📖 `BUILD_AND_RUN.md` - Complete Windows setup guide (500+ lines)
- 🏗️ `ARCHITECTURE.md` - Technical deep-dive (1500+ lines)
- 📊 `PROJECT_STATUS.md` - What's implemented and working

**Quick Reference:**
- 🔧 Change MySQL password → edit `config.properties`
- 🌐 Change server IP → edit `config.properties` on clients
- 🔌 Change server port → edit `config.properties` on server AND clients
- 💾 View database → `mysql -u root -p` then `USE lan_messenger;`

---

**Happy Chatting! 🚀💬**

Questions? Check the detailed guides in the project folder!
