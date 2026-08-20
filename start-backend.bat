@echo off
title WorkoutSmart Backend (8080)
cd /d "%~dp0backend"
if not exist ".env" (
    echo [ERROR] File .env not found in backend^. Copy .env.example to .env and configure it.
    pause
    exit /b 1
)
if "%JAVA_HOME%"=="" (
    if exist "C:\Program Files\Java\jdk-17" (
        set "JAVA_HOME=C:\Program Files\Java\jdk-17"
    ) else if exist "C:\Program Files\Java\jdk-21" (
        set "JAVA_HOME=C:\Program Files\Java\jdk-21"
    )
)
set JWT_SECRET=dev-secret-0123456789abcdef0123456789abcdef
call mvnw.cmd spring-boot:run
pause
