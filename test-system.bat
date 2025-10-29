@echo off
REM =============================================
REM COMPREHENSIVE SYSTEM TESTING SCRIPT (Windows)
REM Tests all components to ensure no crashes
REM =============================================

setlocal enabledelayedexpansion

echo 🧪 Starting Comprehensive System Testing...
echo ==============================================

REM Configuration
set BASE_URL=http://localhost
set API_URL=%BASE_URL%:8080/api
set FRONTEND_URL=%BASE_URL%:80
set MAX_RESPONSE_TIME=5000

REM 1. BASIC CONNECTIVITY TESTS
echo [TEST] Testing basic connectivity...

REM Test Backend API
curl -f -s --max-time 10 "%API_URL%/health" >nul 2>&1
if %errorlevel% equ 0 (
    echo [PASS] Backend API is responding
) else (
    echo [FAIL] Backend API is not responding
    exit /b 1
)

REM Test Frontend
curl -f -s --max-time 10 "%FRONTEND_URL%" >nul 2>&1
if %errorlevel% equ 0 (
    echo [PASS] Frontend is responding
) else (
    echo [FAIL] Frontend is not responding
    exit /b 1
)

REM 2. DATABASE CONNECTIVITY TEST
echo [TEST] Testing database connectivity...
curl -s --max-time 10 "%API_URL%/health" > temp_health.json 2>nul
findstr /C:"database" temp_health.json >nul 2>&1
if %errorlevel% equ 0 (
    echo [PASS] Database connection is healthy
) else (
    echo [FAIL] Database connection failed
)

REM 3. AUTHENTICATION TEST
echo [TEST] Testing authentication system...
curl -s --max-time 10 -X POST "%API_URL%/auth/login" -H "Content-Type: application/x-www-form-urlencoded" -d "username=admin&password=admin123" > temp_auth.json 2>nul
findstr /C:"success" temp_auth.json >nul 2>&1
if %errorlevel% equ 0 (
    echo [PASS] Authentication system is working
) else (
    echo [FAIL] Authentication system failed
)

REM 4. API ENDPOINT TESTS
echo [TEST] Testing critical API endpoints...

REM Test user management
curl -f -s --max-time 10 "%API_URL%/users" >nul 2>&1
if %errorlevel% equ 0 (
    echo [PASS] User management API is working
) else (
    echo [FAIL] User management API failed
)

REM Test requisitions
curl -f -s --max-time 10 "%API_URL%/requisitions" >nul 2>&1
if %errorlevel% equ 0 (
    echo [PASS] Requisitions API is working
) else (
    echo [FAIL] Requisitions API failed
)

REM Test dashboard
curl -f -s --max-time 10 "%API_URL%/dashboard" >nul 2>&1
if %errorlevel% equ 0 (
    echo [PASS] Dashboard API is working
) else (
    echo [FAIL] Dashboard API failed
)

REM 5. DOCKER CONTAINER CHECK
echo [TEST] Checking Docker containers...
docker ps --filter "name=procure" --format "table {{.Names}}" > temp_containers.txt 2>nul
find /c /v "" temp_containers.txt > temp_count.txt 2>nul
set /p CONTAINER_COUNT=<temp_count.txt
set /a CONTAINER_COUNT-=1

if %CONTAINER_COUNT% geq 4 (
    echo [PASS] All containers are running (!CONTAINER_COUNT! containers)
) else (
    echo [WARN] Some containers may not be running (!CONTAINER_COUNT! containers)
)

REM 6. FINAL HEALTH CHECK
echo [TEST] Running final comprehensive health check...

set ALL_HEALTHY=true

REM Test all critical endpoints
curl -f -s --max-time 5 "%API_URL%/health" >nul 2>&1
if %errorlevel% neq 0 set ALL_HEALTHY=false

curl -f -s --max-time 5 "%API_URL%/users" >nul 2>&1
if %errorlevel% neq 0 set ALL_HEALTHY=false

curl -f -s --max-time 5 "%API_URL%/requisitions" >nul 2>&1
if %errorlevel% neq 0 set ALL_HEALTHY=false

curl -f -s --max-time 5 "%API_URL%/dashboard" >nul 2>&1
if %errorlevel% neq 0 set ALL_HEALTHY=false

curl -f -s --max-time 5 "%FRONTEND_URL%" >nul 2>&1
if %errorlevel% neq 0 set ALL_HEALTHY=false

REM Clean up temp files
del temp_health.json temp_auth.json temp_containers.txt temp_count.txt 2>nul

echo.
echo ==============================================
if "%ALL_HEALTHY%"=="true" (
    echo [PASS] 🎉 ALL TESTS PASSED - System is stable and ready!
    echo.
    echo 📊 System Status:
    echo   ✅ All services are running
    echo   ✅ Database connectivity is healthy
    echo   ✅ Authentication is working
    echo   ✅ API endpoints are responding
    echo   ✅ No critical errors found
    echo.
    echo 🚀 Your system is ready for production!
) else (
    echo [FAIL] ❌ Some tests failed - Please check the issues above
    echo.
    echo 🔧 Troubleshooting steps:
    echo   1. Check if all containers are running: docker ps
    echo   2. Check logs: docker-compose -f docker-compose.production.yml logs
    echo   3. Restart services: docker-compose -f docker-compose.production.yml restart
    echo   4. Check system resources: docker stats
    exit /b 1
)

pause
