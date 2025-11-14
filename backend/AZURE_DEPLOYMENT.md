# Azure Deployment Guide for Backend

This guide explains how to deploy the AetosWMS backend to Azure App Service.

## Prerequisites

- Azure account with active subscription
- Azure CLI installed (optional, for command-line deployment)
- Maven 3.6+ installed
- Java 17 JDK installed

## Build for Azure

### Option 1: Using Build Script (Recommended)

**Windows (PowerShell):**
```powershell
cd AetosWMS\backend
.\build-azure.ps1
```

**Linux/Mac (Bash):**
```bash
cd AetosWMS/backend
chmod +x build-azure.sh
./build-azure.sh
```

### Option 2: Manual Build

```bash
cd AetosWMS/backend
mvn clean package -DskipTests
```

The JAR file will be created at: `target/backend-0.0.1-SNAPSHOT.jar`

## Azure App Service Deployment

### Method 1: Azure Portal (Web UI)

1. **Create App Service:**
   - Go to Azure Portal → Create a resource → Web App
   - Select:
     - **Runtime stack**: Java 17
     - **Operating System**: Linux (recommended) or Windows
     - **Pricing tier**: Choose based on your needs

2. **Configure Application Settings:**
   - Go to Configuration → Application settings
   - Add the following environment variables:
     ```
     SPRING_PROFILES_ACTIVE=production
     PORT=8080
     JAVA_OPTS=-Xms512m -Xmx1g -XX:+UseG1GC
     
     # Database Configuration
     DB_USERNAME=your_db_username
     DB_PASSWORD=your_db_password
     DB_HOST=your_postgres_server.postgres.database.azure.com
     
     # CORS Configuration
     CORS_ALLOWED_ORIGINS=http://your-frontend-url.com,http://20.57.79.136
     
     # JWT Configuration
     JWT_SECRET=your-secret-key-min-32-characters
     JWT_EXPIRATION=86400000
     ```

3. **Configure Startup Command:**
   - Go to Configuration → General settings
   - Set **Startup Command**:
     ```
     java -jar backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=production
     ```

4. **Deploy JAR File:**
   - Go to Deployment Center
   - Choose deployment method:
     - **FTP/FTPS**: Upload JAR file manually
     - **GitHub Actions**: Automated deployment
     - **Azure DevOps**: CI/CD pipeline
     - **Local Git**: Push from local repository

### Method 2: Azure CLI

```bash
# Login to Azure
az login

# Create Resource Group (if not exists)
az group create --name aetoswms-rg --location eastus

# Create App Service Plan
az appservice plan create \
  --name aetoswms-plan \
  --resource-group aetoswms-rg \
  --sku B1 \
  --is-linux

# Create Web App
az webapp create \
  --name aetoswms-backend \
  --resource-group aetoswms-rg \
  --plan aetoswms-plan \
  --runtime "JAVA:17-java17"

# Configure Java version
az webapp config set \
  --name aetoswms-backend \
  --resource-group aetoswms-rg \
  --java-version 17 \
  --java-container "JAVA SE" \
  --java-container-version "17"

# Set startup command
az webapp config set \
  --name aetoswms-backend \
  --resource-group aetoswms-rg \
  --startup-file "java -jar backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=production"

# Set environment variables
az webapp config appsettings set \
  --name aetoswms-backend \
  --resource-group aetoswms-rg \
  --settings \
    SPRING_PROFILES_ACTIVE=production \
    PORT=8080 \
    JAVA_OPTS="-Xms512m -Xmx1g -XX:+UseG1GC" \
    DB_USERNAME=your_db_username \
    DB_PASSWORD=your_db_password \
    CORS_ALLOWED_ORIGINS="http://your-frontend-url.com,http://20.57.79.136"

# Deploy JAR file
az webapp deploy \
  --name aetoswms-backend \
  --resource-group aetoswms-rg \
  --type jar \
  --src-path target/backend-0.0.1-SNAPSHOT.jar
```

### Method 3: FTP Deployment

1. Get FTP credentials from Azure Portal:
   - Go to Deployment Center → FTPS credentials
   - Note down: FTP hostname, username, password

2. Upload JAR file using FTP client:
   ```
   Host: <your-app>.ftp.azurewebsites.windows.net
   Username: <your-app>\<username>
   Password: <password>
   Path: /site/wwwroot/
   ```

3. Upload the JAR file to `/site/wwwroot/backend-0.0.1-SNAPSHOT.jar`

## Azure Database Configuration

### Azure Database for PostgreSQL

1. **Create PostgreSQL Server:**
   ```bash
   az postgres flexible-server create \
     --name aetoswms-db \
     --resource-group aetoswms-rg \
     --location eastus \
     --admin-user adminuser \
     --admin-password YourPassword123! \
     --sku-name Standard_B1ms \
     --tier Burstable \
     --version 14
   ```

2. **Create Database:**
   ```bash
   az postgres flexible-server db create \
     --resource-group aetoswms-rg \
     --server-name aetoswms-db \
     --database-name procuredb
   ```

3. **Configure Firewall:**
   - Allow Azure services: Yes
   - Add your App Service outbound IPs

4. **Update Application Settings:**
   ```
   spring.datasource.url=jdbc:postgresql://aetoswms-db.postgres.database.azure.com:5432/procuredb?sslmode=require
   spring.datasource.username=adminuser
   spring.datasource.password=YourPassword123!
   ```

## Environment Variables for Azure

Set these in Azure Portal → Configuration → Application settings:

### Required Variables:
```
SPRING_PROFILES_ACTIVE=production
PORT=8080
JAVA_OPTS=-Xms512m -Xmx1g -XX:+UseG1GC

# Database
DB_USERNAME=your_db_username
DB_PASSWORD=your_db_password
DB_HOST=your_postgres_server.postgres.database.azure.com

# CORS
CORS_ALLOWED_ORIGINS=http://your-frontend-url.com,http://20.57.79.136

# JWT
JWT_SECRET=your-secret-key-min-32-characters
JWT_EXPIRATION=86400000
```

### Optional Variables:
```
REDIS_HOST=your_redis_cache.redis.cache.windows.net
REDIS_PORT=6380
REDIS_PASSWORD=your_redis_password

MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your_email@gmail.com
MAIL_PASSWORD=your_app_password
```

## Startup Command

In Azure Portal → Configuration → General settings:

```
java -jar backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=production
```

Or if using PORT environment variable:
```
java -jar backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=production --server.port=${PORT}
```

## Health Check

Azure will automatically check:
- Health endpoint: `https://your-app.azurewebsites.net/api/health`
- Or configure custom health check in App Service settings

## Monitoring

1. **Application Insights:**
   - Enable in Azure Portal
   - Add dependency to monitor database, Redis, etc.

2. **Logs:**
   - View logs: Azure Portal → App Service → Log stream
   - Download logs: Azure Portal → App Service → Advanced Tools → Go → Log Files

## Troubleshooting

### Common Issues:

1. **Port Configuration:**
   - Azure uses `PORT` environment variable
   - Update `application-production.properties` to use `${PORT:8080}`

2. **CORS Issues:**
   - Ensure `CORS_ALLOWED_ORIGINS` includes your frontend URL
   - Check that CORS is enabled in SecurityConfig

3. **Database Connection:**
   - Verify firewall rules allow Azure services
   - Check SSL mode (Azure requires SSL)

4. **Memory Issues:**
   - Adjust `JAVA_OPTS` based on your App Service Plan
   - Monitor in Azure Portal → Metrics

## Quick Deploy Commands

```bash
# Build
cd backend
mvn clean package -DskipTests

# Deploy via Azure CLI
az webapp deploy \
  --name aetoswms-backend \
  --resource-group aetoswms-rg \
  --type jar \
  --src-path target/backend-0.0.1-SNAPSHOT.jar
```

## Support

For issues, check:
- Azure App Service logs
- Application Insights
- Spring Boot logs in Log stream

