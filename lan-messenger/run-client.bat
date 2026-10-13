@echo off
echo ============================================
echo   LAN Messenger Client
echo ============================================
echo.
echo Starting client application...
echo.

cd /d %~dp0
mvn exec:java -Dexec.mainClass="com.lanmessenger.client.ClientApp"

pause
