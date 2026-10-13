@echo off
echo ============================================
echo   LAN Messenger Server
echo ============================================
echo.
echo Starting server on port 5000...
echo Press Ctrl+C to stop the server
echo.

cd /d %~dp0
mvn exec:java -Dexec.mainClass="com.lanmessenger.server.ServerApp"

pause
