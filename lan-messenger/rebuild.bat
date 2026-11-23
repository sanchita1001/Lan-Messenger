@echo off
echo ============================================
echo   LAN Messenger - Rebuild Project
echo ============================================
echo.
echo Cleaning and recompiling...
echo.

cd /d %~dp0
mvn clean compile

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ============================================
    echo   BUILD SUCCESSFUL!
    echo ============================================
    echo.
    echo Next steps:
    echo   1. Stop the server (Ctrl+C if running)
    echo   2. Run: run-server.bat
    echo   3. Run: run-client.bat (for each client)
    echo.
) else (
    echo.
    echo ============================================
    echo   BUILD FAILED!
    echo ============================================
    echo.
    echo Check the error messages above.
    echo.
)

pause
