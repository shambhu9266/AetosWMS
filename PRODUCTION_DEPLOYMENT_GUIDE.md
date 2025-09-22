# 🚀 Production Deployment Guide

## Overview
This guide will help you deploy the Procure Management System on a production server with high performance, scalability, and reliability.

## 🏗️ Architecture

### Components
- **Frontend**: Angular application served by Nginx
- **Backend**: Spring Boot application with PostgreSQL
- **Database**: PostgreSQL with optimized indexes
- **Cache**: Redis for session management and caching
- **Load Balancer**: Nginx for high availability

### Performance Features
- ✅ Database indexing for fast queries
- ✅ Pagination for large datasets
- ✅ Caching for frequently accessed data
- ✅ Connection pooling
- ✅ Async processing
- ✅ Gzip compression
- ✅ Static asset optimization

## 📋 Prerequisites

### Server Requirements
- **CPU**: 4+ cores recommended
- **RAM**: 8GB+ recommended (4GB minimum)
- **Storage**: 50GB+ SSD recommended
- **OS**: Ubuntu 20.04+ or CentOS 8+

### Software Requirements
- Docker & Docker Compose
- Git
- SSL certificates (for HTTPS)

## 🚀 Quick Deployment

### 1. Clone and Setup
```bash
# Clone the repository
git clone <your-repo-url>
cd procure-management-system

# Create environment file
cp .env.example .env
# Edit .env with your production values
```

### 2. Configure Environment
```bash
# Edit .env file
nano .env

# Required variables:
DB_PASSWORD=your_secure_database_password
ADMIN_USERNAME=your_admin_username
ADMIN_PASSWORD=your_secure_admin_password
MAIL_USERNAME=your_email@domain.com
MAIL_PASSWORD=your_email_password
```

### 3. Deploy with Docker
```bash
# Start all services
docker-compose -f docker-compose.production.yml up -d

# Check status
docker-compose -f docker-compose.production.yml ps

# View logs
docker-compose -f docker-compose.production.yml logs -f
```

### 4. Verify Deployment
```bash
# Check if services are running
curl http://localhost/api/health
curl http://localhost/health

# Access the application
# Frontend: http://your-server-ip
# Backend API: http://your-server-ip/api
```

## 🔧 Performance Optimization

### Database Optimization
```sql
-- The system automatically creates these indexes:
-- - Requisition status and date indexes
-- - User-specific indexes
-- - PDF status indexes
-- - Notification indexes
-- - Composite indexes for common queries
```

### Caching Strategy
- **Dashboard Stats**: Cached for 5 minutes
- **User Data**: Cached for 5 minutes
- **Budget Data**: Cached for 10 minutes
- **Static Assets**: Cached for 1 year

### Connection Pooling
- **Max Connections**: 20
- **Min Idle**: 5
- **Connection Timeout**: 20 seconds
- **Idle Timeout**: 5 minutes

## 📊 Monitoring

### Health Checks
```bash
# Application health
curl http://localhost/api/health

# Database health
docker exec procure_postgres pg_isready

# Redis health
docker exec procure_redis redis-cli ping
```

### Logs
```bash
# Application logs
docker-compose -f docker-compose.production.yml logs backend

# Database logs
docker-compose -f docker-comduction.yml logs postgres

# Nginx logs
docker-compose -f docker-compose.production.yml logs frontend
```

### Metrics
- **Prometheus**: http://localhost:8080/actuator/prometheus
- **Health**: http://localhost:8080/actuator/health
- **Info**: http://localhost:8080/actuator/info

## 🔒 Security

### SSL/HTTPS Setup
```bash
# Place your SSL certificates in ssl_certs volume
# Update nginx.conf to use HTTPS
# Redirect HTTP to HTTPS
```

### Firewall Configuration
```bash
# Allow only necessary ports
ufw allow 22    # SSH
ufw allow 80    # HTTP
ufw allow 443   # HTTPS
ufw enable
```

### Database Security
- Use strong passwords
- Enable SSL connections
- Regular backups
- Access restrictions

## 📈 Scaling

### Horizontal Scaling
```yaml
# Scale backend instances
docker-compose -f docker-compose.production.yml up -d --scale backend=3

# Use load balancer (nginx) to distribute traffic
```

### Vertical Scaling
```yaml
# Increase resources in docker-compose.production.yml
deploy:
  resources:
    limits:
      memory: 2G
      cpus: '1.0'
```

## 🔄 Backup & Recovery

### Database Backup
```bash
# Create backup
docker exec procure_postgres pg_dump -U procure_user procuredb > backup_$(date +%Y%m%d_%H%M%S).sql

# Restore backup
docker exec -i procure_postgres psql -U procure_user procuredb < backup_file.sql
```

### File Backup
```bash
# Backup uploaded files
docker run --rm -v procure_file_uploads:/data -v $(pwd):/backup alpine tar czf /backup/files_backup.tar.gz -C /data .
```

## 🛠️ Maintenance

### Updates
```bash
# Pull latest changes
git pull origin main

# Rebuild and restart
docker-compose -f docker-compose.production.yml down
docker-compose -f docker-compose.production.yml up -d --build
```

### Cache Management
```bash
# Clear application cache
curl -X POST http://localhost/api/optimized/clear-cache \
  -H "Authorization: Bearer your-admin-token"
```

### Database Maintenance
```sql
-- Analyze tables for query optimization
ANALYZE;

-- Vacuum to reclaim space
VACUUM ANALYZE;
```

## 🚨 Troubleshooting

### Common Issues

#### High Memory Usage
```bash
# Check memory usage
docker stats

# Optimize JVM settings in Dockerfile
ENV JAVA_OPTS="-Xms1g -Xmx2g -XX:+UseG1GC"
```

#### Slow Queries
```bash
# Enable slow query logging
# Check database indexes
# Use EXPLAIN ANALYZE for slow queries
```

#### Connection Issues
```bash
# Check connection pool settings
# Verify network connectivity
# Check firewall rules
```

## 📞 Support

### Performance Monitoring
- Monitor CPU, Memory, Disk usage
- Track response times
- Monitor error rates
- Set up alerts

### Log Analysis
- Use ELK stack for log analysis
- Monitor application logs
- Track user activities
- Performance metrics

## 🎯 Best Practices

1. **Regular Backups**: Daily database and file backups
2. **Monitoring**: Set up comprehensive monitoring
3. **Updates**: Keep system updated
4. **Security**: Regular security audits
5. **Performance**: Monitor and optimize regularly
6. **Documentation**: Keep deployment docs updated

## 📊 Expected Performance

### Benchmarks
- **Response Time**: < 200ms for cached data
- **Throughput**: 1000+ requests/minute
- **Concurrent Users**: 500+ users
- **Database**: < 50ms query time
- **File Upload**: 10MB files in < 5 seconds

### Capacity Planning
- **Users**: 1000+ concurrent users
- **Data**: 1M+ requisitions
- **Files**: 100GB+ storage
- **Growth**: 20% monthly growth support

---

**Note**: This deployment is optimized for production use with high performance and scalability. Monitor your system regularly and adjust resources based on actual usage patterns.
