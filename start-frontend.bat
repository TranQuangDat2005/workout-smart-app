@echo off
title WorkoutSmart Frontend (5173)
cd /d "%~dp0web"
if not exist "node_modules" (
    echo [INFO] node_modules not found. Running npm install...
    call npm.cmd install
)
call npm.cmd run dev
pause
