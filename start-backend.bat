@echo off
title WorkoutSmart Backend (8080)
cd /d "%~dp0backend"
set JWT_SECRET=dev-secret-0123456789abcdef0123456789abcdef
java -jar target\backend-0.1.0-SNAPSHOT.jar > "%~dp0backend-run.log" 2>&1
pause
