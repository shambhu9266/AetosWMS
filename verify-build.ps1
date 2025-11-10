# Build Verification Script for AetosWMS
# Verifies that both backend and frontend builds are complete and valid

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  AetosWMS Build Verification" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$allChecksPassed = $true

# Check Backend Build
Write-Host "[Backend] Checking Spring Boot JAR..." -ForegroundColor Yellow
$backendJar = "backend/target/backend-0.0.1-SNAPSHOT.jar"

if (Test-Path $backendJar) {
    $jarInfo = Get-Item $backendJar
    $sizeMB = [math]::Round($jarInfo.Length / 1MB, 2)
    
    Write-Host "  ✓ JAR file exists" -ForegroundColor Green
    Write-Host "    Location: $backendJar" -ForegroundColor Gray
    Write-Host "    Size: $sizeMB MB" -ForegroundColor Gray
    Write-Host "    Last Modified: $($jarInfo.LastWriteTime)" -ForegroundColor Gray
    
    # Check if JAR is valid (has reasonable size)
    if ($jarInfo.Length -lt 1MB) {
        Write-Host "  ✗ WARNING: JAR file seems too small!" -ForegroundColor Red
        $allChecksPassed = $false
    } else {
        Write-Host "  ✓ JAR size is valid" -ForegroundColor Green
    }
    
    # Try to verify it's a Spring Boot JAR (check if it contains BOOT-INF)
    try {
        Add-Type -AssemblyName System.IO.Compression.FileSystem
        $zip = [System.IO.Compression.ZipFile]::OpenRead($backendJar.FullName)
        $hasBootInf = $zip.Entries | Where-Object { $_.FullName -like "BOOT-INF/*" } | Measure-Object
        $zip.Dispose()
        
        if ($hasBootInf.Count -gt 0) {
            Write-Host "  ✓ Valid Spring Boot executable JAR" -ForegroundColor Green
        } else {
            Write-Host "  ✗ WARNING: JAR may not be a Spring Boot executable" -ForegroundColor Yellow
        }
    } catch {
        Write-Host "  ⚠ Could not verify JAR structure: $_" -ForegroundColor Yellow
    }
} else {
    Write-Host "  ✗ JAR file not found!" -ForegroundColor Red
    $allChecksPassed = $false
}

Write-Host ""

# Check Frontend Build
Write-Host "[Frontend] Checking Angular production build..." -ForegroundColor Yellow
$frontendIndex = "frontend/dist/frontend/browser/index.html"

if (Test-Path $frontendIndex) {
    Write-Host "  ✓ index.html exists" -ForegroundColor Green
    
    # Check for required files
    $requiredFiles = @(
        "frontend/dist/frontend/browser/index.html",
        "frontend/dist/frontend/browser/main-*.js",
        "frontend/dist/frontend/browser/polyfills-*.js",
        "frontend/dist/frontend/browser/styles-*.css"
    )
    
    $missingFiles = @()
    foreach ($pattern in $requiredFiles) {
        $found = Get-ChildItem -Path (Split-Path $pattern -Parent) -Filter (Split-Path $pattern -Leaf) -ErrorAction SilentlyContinue
        if (-not $found) {
            $missingFiles += $pattern
        }
    }
    
    if ($missingFiles.Count -eq 0) {
        Write-Host "  ✓ All required files present" -ForegroundColor Green
        
        # List main files
        $mainFiles = Get-ChildItem "frontend/dist/frontend/browser" -File | 
            Select-Object Name, @{Name="SizeKB";Expression={[math]::Round($_.Length/1KB, 2)}}
        
        Write-Host "`n  Build Files:" -ForegroundColor Gray
        foreach ($file in $mainFiles) {
            $sizeStr = "$($file.SizeKB) KB"
            Write-Host "    - $($file.Name) ($sizeStr)" -ForegroundColor Gray
        }
        
        # Check assets folder
        if (Test-Path "frontend/dist/frontend/browser/assets") {
            $assetCount = (Get-ChildItem "frontend/dist/frontend/browser/assets" -Recurse -File).Count
            Write-Host "    - assets/ directory with $assetCount files" -ForegroundColor Gray
        }
        
        # Verify index.html structure
        $indexContent = Get-Content $frontendIndex -Raw
        if ($indexContent -match "app-root" -and $indexContent -match "main-.*\.js") {
            Write-Host "  ✓ index.html structure is valid" -ForegroundColor Green
        } else {
            Write-Host "  ✗ index.html structure may be invalid" -ForegroundColor Red
            $allChecksPassed = $false
        }
    } else {
        Write-Host "  ✗ Missing required files:" -ForegroundColor Red
        foreach ($file in $missingFiles) {
            Write-Host "    - $file" -ForegroundColor Red
        }
        $allChecksPassed = $false
    }
} else {
    Write-Host "  ✗ Frontend build not found!" -ForegroundColor Red
    $allChecksPassed = $false
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan

if ($allChecksPassed) {
    Write-Host "  ✓ ALL CHECKS PASSED" -ForegroundColor Green
    Write-Host "  Builds are ready for deployment!" -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Cyan
    exit 0
} else {
    Write-Host "  ✗ SOME CHECKS FAILED" -ForegroundColor Red
    Write-Host "  Please review the issues above" -ForegroundColor Red
    Write-Host "========================================" -ForegroundColor Cyan
    exit 1
}

