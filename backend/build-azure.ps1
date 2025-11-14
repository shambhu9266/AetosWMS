# Azure Build Script for Backend (PowerShell)
# This script builds the backend JAR for Azure App Service deployment

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Azure Backend Build Script" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Get the script directory
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $scriptDir

# Clean previous builds
Write-Host "[1/3] Cleaning previous builds..." -ForegroundColor Yellow
mvn clean
if ($LASTEXITCODE -ne 0) {
    Write-Host "Clean failed!" -ForegroundColor Red
    exit 1
}
Write-Host "✓ Clean completed" -ForegroundColor Green

# Build the application
Write-Host ""
Write-Host "[2/3] Building Backend (Spring Boot) for Azure..." -ForegroundColor Yellow
mvn clean package -DskipTests -Pproduction
if ($LASTEXITCODE -ne 0) {
    Write-Host "Build failed!" -ForegroundColor Red
    exit 1
}
Write-Host "✓ Build completed successfully" -ForegroundColor Green

# Verify JAR file
Write-Host ""
Write-Host "[3/3] Verifying build artifacts..." -ForegroundColor Yellow
$jarFile = "target\backend-0.0.1-SNAPSHOT.jar"
if (Test-Path $jarFile) {
    $jarSize = (Get-Item $jarFile).Length / 1MB
    Write-Host "✓ JAR file created: $jarFile ($([math]::Round($jarSize, 2)) MB)" -ForegroundColor Green
} else {
    Write-Host "✗ JAR file not found!" -ForegroundColor Red
    exit 1
}

# Summary
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Build Summary" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "✓ Backend JAR: $jarFile" -ForegroundColor Green
Write-Host ""
Write-Host "Next steps for Azure deployment:" -ForegroundColor Yellow
Write-Host "  1. Upload JAR to Azure App Service" -ForegroundColor Gray
Write-Host "  2. Set environment variables in Azure Portal" -ForegroundColor Gray
Write-Host "  3. Configure startup command: java -jar backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=production" -ForegroundColor Gray
Write-Host ""
Write-Host "Azure App Service Configuration:" -ForegroundColor Yellow
Write-Host "  - Runtime: Java 17" -ForegroundColor Gray
Write-Host "  - Port: 8080 (or use PORT environment variable)" -ForegroundColor Gray
Write-Host "  - Startup Command: java -jar backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=production" -ForegroundColor Gray
Write-Host ""

