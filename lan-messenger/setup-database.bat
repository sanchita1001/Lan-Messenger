@echo off
echo ============================================
echo   LAN Messenger Database Setup
echo ============================================
echo.
echo This will create the lan_messenger database
echo and all required tables.
echo.
echo Make sure MySQL Server is running!
echo.
pause

cd /d %~dp0

echo.
echo Executing schema.sql...
echo Please enter your MySQL root password when prompted.
echo.

mysql -u root -p < database\schema.sql

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ============================================
    echo   SUCCESS! Database created successfully
    echo ============================================
    echo.
    echo Test users created:
    echo   Username: alice   Password: password123
    echo   Username: bob     Password: password123
    echo   Username: charlie Password: password123
    echo.
    echo Next step: Edit src\main\resources\config.properties
    echo            and set your MySQL password
    echo.
) else (
    echo.
    echo ============================================
    echo   ERROR! Database setup failed
    echo ============================================
    echo.
    echo Please check:
    echo   1. MySQL Server is running
    echo   2. You entered the correct password
    echo   3. MySQL is installed in the default location
    echo.
)

pause
