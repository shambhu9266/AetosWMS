#!/bin/bash

# Build Script for AetosWMS Deployment
# This script builds both backend and frontend for production deployment

echo "========================================"
echo "  AetosWMS Production Build Script"
echo "========================================"
echo ""

# Get the script directory
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$SCRIPT_DIR"

# Build Backend
echo "[1/2] Building Backend (Spring Boot)..."
cd backend
mvn clean package -DskipTests
if [ $? -ne 0 ]; then
    echo "Backend build failed!"
    exit 1
fi
echo "✓ Backend build completed successfully"
echo "  JAR location: backend/target/backend-0.0.1-SNAPSHOT.jar"
cd ..

# Build Frontend
echo ""
echo "[2/2] Building Frontend (Angular)..."
cd frontend
npm run build
if [ $? -ne 0 ]; then
    echo "Frontend build failed!"
    exit 1
fi
echo "✓ Frontend build completed successfully"
echo "  Build location: frontend/dist/frontend"
cd ..

# Summary
echo ""
echo "========================================"
echo "  Build Summary"
echo "========================================"
echo "✓ Backend JAR: backend/target/backend-0.0.1-SNAPSHOT.jar"
echo "✓ Frontend: frontend/dist/frontend"
echo ""
echo "Next steps:"
echo "  1. For Docker deployment: docker-compose -f docker-compose.production.yml build"
echo "  2. For manual deployment: Copy JAR and dist folder to your server"
echo ""

