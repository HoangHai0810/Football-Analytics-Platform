@echo off
title ngrok ClickHouse Tunnel - Football Analytics
color 0A

echo ===============================================
echo   Football Analytics - ngrok ClickHouse Tunnel
echo ===============================================
echo.

set NGROK="C:\Users\ADMIN\AppData\Local\Microsoft\WinGet\Packages\Ngrok.Ngrok_Microsoft.Winget.Source_8wekyb3d8bbwe\ngrok.exe"

:: Check ngrok exists
if not exist %NGROK% (
    echo ERROR: ngrok not found at expected path!
    echo Please run: winget install --id Ngrok.Ngrok
    pause
    exit /b 1
)

:: Check ClickHouse is running
echo Checking ClickHouse on port 8123...
curl -s http://localhost:8123/ping >nul 2>&1
if %errorlevel% neq 0 (
    echo WARNING: ClickHouse not responding on localhost:8123
    echo Make sure ClickHouse is running before continuing.
    echo.
    pause
)

echo Starting ngrok tunnel for ClickHouse (port 8123)...
echo.
echo Dashboard: http://localhost:4040
echo.
echo *** KEEP THIS WINDOW OPEN ***
echo *** Closing this window will stop the tunnel ***
echo.

%NGROK% http 8123

pause
