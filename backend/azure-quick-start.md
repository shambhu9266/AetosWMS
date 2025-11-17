# Azure Quick Start Guide

## Quick Build Command

**Windows:**
```powershell
cd AetosWMS\backend
.\build-azure.ps1
```

**Linux/Mac:**
```bash
cd AetosWMS/backend
chmod +x build-azure.sh
./build-azure.sh
```

**Manual:**
```bash
cd AetosWMS/backend
mvn clean package -DskipTests
```

## Output

After building, you'll find:
- **JAR File**: `target/backend-0.0.1-SNAPSHOT.jar`
- **Size**: ~80-100 MB (includes all dependencies)

## Azure Deployment Steps

1. **Build the JAR** (see commands above)

2. **Create Azure App Service** (via Portal or CLI):
   - Runtime: Java 17
   - OS: Linux (recommended)
   - Plan: Basic B1 or higher

3. **Upload JAR File**:
   - Via Azure Portal: Deployment Center → FTP
   - Via Azure CLI: `az webapp deploy --name <app-name> --resource-group <rg> --type jar --src-path target/backend-0.0.1-SNAPSHOT.jar`

4. **Configure Environment Variables** in Azure Portal:
   ```
   SPRING_PROFILES_ACTIVE=production
   PORT=8080
   DB_USERNAME=your_username
   DB_PASSWORD=your_password
   DB_HOST=your_postgres_server.postgres.database.azure.com
   CORS_ALLOWED_ORIGINS=http://your-frontend-url.com
   ```

5. **Set Startup Command**:
   ```
   java -jar backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=production
   ```

## Files Created

- `build-azure.sh` - Linux/Mac build script
- `build-azure.ps1` - Windows build script
- `AZURE_DEPLOYMENT.md` - Complete deployment guide
- `.azure/appsettings.json` - Azure configuration template
- `.deployment` - Deployment configuration

For detailed instructions, see `AZURE_DEPLOYMENT.md`

