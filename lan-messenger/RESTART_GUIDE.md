# How to Restart After Code Changes

## ⚠️ Important: Always Rebuild and Restart

When code changes are made, you MUST:
1. Rebuild the project
2. Stop the old server
3. Start the new server
4. Restart clients

---

## 🔄 Complete Restart Process

### Step 1: Stop Everything

**Stop the Server:**
- Go to the terminal running the server
- Press `Ctrl + C`
- Wait for it to fully stop

**Stop All Clients:**
- Close all client windows
- Or press `Ctrl + C` in each client terminal

---

### Step 2: Rebuild the Project

```powershell
cd C:\Users\Lenovo\Desktop\LAN_Messenger\lan-messenger
mvn clean compile
```

**Wait for:** `BUILD SUCCESS`

---

### Step 3: Start Server

```powershell
.\run-server.bat
```

Or:
```powershell
mvn exec:java -Dexec.mainClass="com.lanmessenger.server.ServerApp"
```

**Verify you see:**
- ✅ Server IP Addresses
- ✅ Database connection successful
- ✅ Server listening on port 5000

---

### Step 4: Start Clients

**Terminal 1 (Alice):**
```powershell
.\run-client.bat
```
Login: `alice` / `password123`

**Terminal 2 (Bob):**
```powershell
.\run-client.bat
```
Login: `bob` / `password123`

---

## 🐛 If You See "Unknown message type"

This means the server is running OLD code. Fix:

1. **Stop the server** (Ctrl+C)
2. **Rebuild:**
   ```powershell
   mvn clean compile
   ```
3. **Start server again:**
   ```powershell
   .\run-server.bat
   ```

---

## ✅ Quick Restart Commands

### Complete Restart (All-in-One):

**Stop everything first (Ctrl+C on all terminals), then:**

```powershell
# Rebuild
cd C:\Users\Lenovo\Desktop\LAN_Messenger\lan-messenger
mvn clean compile

# Start server
.\run-server.bat
```

**In new terminals:**
```powershell
# Start clients
cd C:\Users\Lenovo\Desktop\LAN_Messenger\lan-messenger
.\run-client.bat
```

---

## 📝 Why This Happens

- Maven compiles code to `target/classes/`
- The server and clients run from these compiled `.class` files
- If you don't rebuild, they run OLD code
- Always rebuild after ANY code change

---

## 🎯 Current Features to Test

After restart, test these NEW features:

### 1. Online Users List
- Click "**+ New Chat**"
- Should show all online users
- Click "**🔄 Refresh**" to update list

### 2. File Transfer
- Click "**📎 Attach File**"
- Select any file (max 50MB)
- Recipient gets download prompt

### 3. Custom Server IP
- On login screen
- Enter server IP (from server terminal)
- Enter port (default 5000)

---

## 🚨 Common Issues

### "Unknown message type: LIST_ONLINE_USERS"
**Fix:** Server not restarted. Stop server, rebuild, restart.

### "Connection refused"
**Fix:** Server not running. Start server first.

### "No other users online"
**Fix:** 
1. Check other clients are logged in
2. Click "🔄 Refresh" button
3. Wait 1-2 seconds for server response

### File transfer fails
**Fix:** 
1. Check file size < 50MB
2. Ensure both users are in same conversation
3. Check server logs for errors

---

## 📊 Verification Checklist

After restart, verify:

- [ ] Server shows IP addresses
- [ ] Database connection successful
- [ ] Clients can login
- [ ] Conversations load
- [ ] Messages send/receive
- [ ] "New Chat" shows online users
- [ ] File attach button visible
- [ ] Server IP field on login screen

If all checked ✅ - everything is working!

---

**Always remember:** Code change → Rebuild → Restart → Test
