# Build Script for AetosWMS Deployment
# This script builds both backend and frontend for production deployment

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  AetosWMS Production Build Script" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Get the script directory
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $scriptDir

# Build Backend
Write-Host "[1/2] Building Backend (Spring Boot)..." -ForegroundColor Yellow
Set-Location backend
mvn clean package -DskipTests
if ($LASTEXITCODE -ne 0) {
    Write-Host "Backend build failed!" -ForegroundColor Red
    exit 1
}
Write-Host "✓ Backend build completed successfully" -ForegroundColor Green
Write-Host "  JAR location: backend/target/backend-0.0.1-SNAPSHOT.jar" -ForegroundColor Gray
Set-Location ..

# Build Frontend
Write-Host ""
Write-Host "[2/2] Building Frontend (Angular)..." -ForegroundColor Yellow
Set-Location frontend
npm run build
if ($LASTEXITCODE -ne 0) {
    Write-Host "Frontend build failed!" -ForegroundColor Red
    exit 1
}
Write-Host "✓ Frontend build completed successfully" -ForegroundColor Green
Write-Host "  Build location: frontend/dist/frontend" -ForegroundColor Gray
Set-Location ..

# Summary
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Build Summary" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "✓ Backend JAR: backend/target/backend-0.0.1-SNAPSHOT.jar" -ForegroundColor Green
Write-Host "✓ Frontend: frontend/dist/frontend" -ForegroundColor Green
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Yellow
Write-Host "  1. For Docker deployment: docker-compose -f docker-compose.production.yml build" -ForegroundColor Gray
Write-Host "  2. For manual deployment: Copy JAR and dist folder to your server" -ForegroundColor Gray
Write-Host ""

