#!/bin/bash

# Azure Build Script for Backend
# This script builds the backend JAR for Azure App Service deployment

echo "========================================"
echo "  Azure Backend Build Script"
echo "========================================"
echo ""

# Get the script directory
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$SCRIPT_DIR"

# Clean previous builds
echo "[1/3] Cleaning previous builds..."
mvn clean
if [ $? -ne 0 ]; then
    echo "Clean failed!"
    exit 1
fi
echo "✓ Clean completed"

# Build the application
echo ""
echo "[2/3] Building Backend (Spring Boot) for Azure..."
mvn clean package -DskipTests -Pproduction
if [ $? -ne 0 ]; then
    echo "Build failed!"
    exit 1
fi
echo "✓ Build completed successfully"

# Verify JAR file
echo ""
echo "[3/3] Verifying build artifacts..."
JAR_FILE="target/backend-0.0.1-SNAPSHOT.jar"
if [ -f "$JAR_FILE" ]; then
    JAR_SIZE=$(du -h "$JAR_FILE" | cut -f1)
    echo "✓ JAR file created: $JAR_FILE ($JAR_SIZE)"
else
    echo "✗ JAR file not found!"
    exit 1
fi

# Summary
echo ""
echo "========================================"
echo "  Build Summary"
echo "========================================"
echo "✓ Backend JAR: $JAR_FILE"
echo ""
echo "Next steps for Azure deployment:"
echo "  1. Upload JAR to Azure App Service"
echo "  2. Set environment variables in Azure Portal"
echo "  3. Configure startup command: java -jar backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=production"
echo ""
echo "Azure App Service Configuration:"
echo "  - Runtime: Java 17"
echo "  - Port: 8080 (or use PORT environment variable)"
echo "  - Startup Command: java -jar backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=production"
echo ""

