@echo off
setlocal
cd /d "%~dp0"
title WorkoutSmart Docker App

echo [INFO] Checking Docker Desktop...
docker info >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Docker Engine is not running.
    echo         Start Docker Desktop manually, then run this file again.
    goto finish
)

if not exist ".env.docker" (
    echo [ERROR] .env.docker was not found in the project root.
    echo         Copy .env.docker.example to .env.docker first.
    goto finish
)

echo [INFO] Building and starting the application...
docker compose --env-file .env.docker up -d --build
if errorlevel 1 (
    echo [ERROR] Docker application could not be started.
    goto finish
)

echo [INFO] Synchronizing PostgreSQL password with .env.docker...
docker compose --env-file .env.docker exec -T postgres psql -U postgres -d postgres -c "ALTER USER postgres PASSWORD 'postgres';" >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Could not synchronize the PostgreSQL password.
    echo         Check that the postgres container is healthy.
    goto finish
)

echo [INFO] Restarting backend with the synchronized database password...
docker compose --env-file .env.docker restart backend >nul
if errorlevel 1 (
    echo [ERROR] Backend could not be restarted.
    goto finish
)

echo [INFO] Waiting for backend to become ready...
set /a backend_attempts=0
:wait_backend
set /a backend_attempts+=1
if %backend_attempts% GEQ 31 (
    echo [ERROR] Backend did not become ready within 60 seconds.
    echo         Run: docker compose --env-file .env.docker logs backend
    goto finish
)
set "http_status="
for /f "delims=" %%S in ('curl.exe -s -o NUL -w "%%{http_code}" -X POST http://localhost/api/v1/auth/register -H "Content-Type: application/json" -d "{}"') do set "http_status=%%S"
if "%http_status%"=="502" (
    timeout /t 2 /nobreak >nul
    goto wait_backend
)
if "%http_status%"=="000" (
    timeout /t 2 /nobreak >nul
    goto wait_backend
)
if not "%http_status%"=="400" if not "%http_status%"=="422" (
    echo [WARN] Backend returned HTTP %http_status%. Continuing to registration.
)

echo.
echo [INFO] Application is running at http://localhost
echo [INFO] Enter credentials to create or request a user OTP.

powershell.exe -NoProfile -ExecutionPolicy Bypass -Command "$email = Read-Host 'Email'; $securePassword = Read-Host 'Password' -AsSecureString; $password = [System.Net.NetworkCredential]::new('', $securePassword).Password; $body = @{ email = $email; password = $password } | ConvertTo-Json; try { $response = Invoke-WebRequest -Uri 'http://localhost/api/v1/auth/register' -Method Post -ContentType 'application/json' -Body $body -UseBasicParsing; Write-Host ('[INFO] ' + $response.Content) } catch { if ($_.Exception.Response -and [int]$_.Exception.Response.StatusCode -eq 409) { try { $response = Invoke-WebRequest -Uri 'http://localhost/api/v1/auth/resend-otp' -Method Post -ContentType 'application/json' -Body (@{ email = $email } | ConvertTo-Json) -UseBasicParsing; Write-Host ('[INFO] Existing user: ' + $response.Content) } catch { Write-Host ('[ERROR] Could not resend OTP: ' + $_.Exception.Message); exit 1 } } else { Write-Host ('[ERROR] Registration failed: ' + $_.Exception.Message); exit 1 } }"
if errorlevel 1 goto finish

echo.
echo [INFO] Latest OTP log entry:
timeout /t 2 /nobreak >nul
docker compose --env-file .env.docker logs --since=30s backend | findstr /C:"[DEV-OTP]"
if errorlevel 1 echo [WARN] OTP log not found yet. Run: docker compose --env-file .env.docker logs -f backend

echo.
echo [INFO] Open http://localhost to use the app.
:finish
echo.
echo [INFO]  Type EXIT when you are finished.
cmd /k
endlocal
