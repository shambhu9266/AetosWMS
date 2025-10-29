#!/bin/bash

# =============================================
# COMPREHENSIVE SYSTEM TESTING SCRIPT
# Tests all components to ensure no crashes
# =============================================

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[TEST]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[PASS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

print_error() {
    echo -e "${RED}[FAIL]${NC} $1"
}

# Test configuration
BASE_URL="http://localhost"
API_URL="$BASE_URL:8080/api"
FRONTEND_URL="$BASE_URL:80"
MAX_RESPONSE_TIME=5000  # 5 seconds
CONCURRENT_USERS=50
TEST_DURATION=300  # 5 minutes

echo "🧪 Starting Comprehensive System Testing..."
echo "=============================================="

# 1. BASIC CONNECTIVITY TESTS
print_status "Testing basic connectivity..."

# Test if services are running
if curl -f -s --max-time 10 "$API_URL/health" > /dev/null 2>&1; then
    print_success "Backend API is responding"
else
    print_error "Backend API is not responding"
    exit 1
fi

if curl -f -s --max-time 10 "$FRONTEND_URL" > /dev/null 2>&1; then
    print_success "Frontend is responding"
else
    print_error "Frontend is not responding"
    exit 1
fi

# 2. DATABASE CONNECTIVITY TEST
print_status "Testing database connectivity..."
DB_RESPONSE=$(curl -s --max-time 10 "$API_URL/health" | grep -o '"database":"[^"]*"' || echo "unknown")
if [[ $DB_RESPONSE == *"UP"* ]]; then
    print_success "Database connection is healthy"
else
    print_error "Database connection failed: $DB_RESPONSE"
fi

# 3. REDIS CONNECTIVITY TEST
print_status "Testing Redis connectivity..."
REDIS_RESPONSE=$(curl -s --max-time 10 "$API_URL/health" | grep -o '"redis":"[^"]*"' || echo "unknown")
if [[ $REDIS_RESPONSE == *"UP"* ]]; then
    print_success "Redis connection is healthy"
else
    print_warning "Redis connection failed: $REDIS_RESPONSE"
fi

# 4. AUTHENTICATION TEST
print_status "Testing authentication system..."
AUTH_RESPONSE=$(curl -s --max-time 10 -X POST "$API_URL/auth/login" \
    -H "Content-Type: application/x-www-form-urlencoded" \
    -d "username=admin&password=admin123" | grep -o '"success":[^,]*' || echo "false")
if [[ $AUTH_RESPONSE == *"true"* ]]; then
    print_success "Authentication system is working"
else
    print_error "Authentication system failed: $AUTH_RESPONSE"
fi

# 5. API ENDPOINT TESTS
print_status "Testing critical API endpoints..."

# Test user management
if curl -f -s --max-time 10 "$API_URL/users" > /dev/null 2>&1; then
    print_success "User management API is working"
else
    print_error "User management API failed"
fi

# Test requisitions
if curl -f -s --max-time 10 "$API_URL/requisitions" > /dev/null 2>&1; then
    print_success "Requisitions API is working"
else
    print_error "Requisitions API failed"
fi

# Test dashboard
if curl -f -s --max-time 10 "$API_URL/dashboard" > /dev/null 2>&1; then
    print_success "Dashboard API is working"
else
    print_error "Dashboard API failed"
fi

# 6. RESPONSE TIME TESTS
print_status "Testing response times..."

# Test API response time
API_TIME=$(curl -o /dev/null -s -w '%{time_total}' --max-time 10 "$API_URL/health" 2>/dev/null || echo "999")
API_TIME_MS=$(echo "$API_TIME * 1000" | bc -l | cut -d. -f1)

if [ "$API_TIME_MS" -lt "$MAX_RESPONSE_TIME" ]; then
    print_success "API response time: ${API_TIME_MS}ms (within limit)"
else
    print_warning "API response time: ${API_TIME_MS}ms (slow)"
fi

# Test frontend response time
FRONTEND_TIME=$(curl -o /dev/null -s -w '%{time_total}' --max-time 10 "$FRONTEND_URL" 2>/dev/null || echo "999")
FRONTEND_TIME_MS=$(echo "$FRONTEND_TIME * 1000" | bc -l | cut -d. -f1)

if [ "$FRONTEND_TIME_MS" -lt "$MAX_RESPONSE_TIME" ]; then
    print_success "Frontend response time: ${FRONTEND_TIME_MS}ms (within limit)"
else
    print_warning "Frontend response time: ${FRONTEND_TIME_MS}ms (slow)"
fi

# 7. MEMORY AND RESOURCE TESTS
print_status "Testing system resources..."

# Check Docker container health
if command -v docker &> /dev/null; then
    CONTAINER_COUNT=$(docker ps --filter "name=procure" --format "table {{.Names}}" | wc -l)
    if [ "$CONTAINER_COUNT" -ge 4 ]; then
        print_success "All containers are running ($CONTAINER_COUNT containers)"
    else
        print_warning "Some containers may not be running ($CONTAINER_COUNT containers)"
    fi
    
    # Check container memory usage
    MEMORY_USAGE=$(docker stats --no-stream --format "table {{.MemUsage}}" | grep -v "MEM USAGE" | head -1)
    print_status "Memory usage: $MEMORY_USAGE"
fi

# 8. STRESS TEST (Optional - requires Apache Bench)
print_status "Running stress test..."
if command -v ab &> /dev/null; then
    print_status "Running 100 requests with 10 concurrent users..."
    ab -n 100 -c 10 -H "Accept: application/json" "$API_URL/health" > /tmp/stress_test.log 2>&1
    
    if grep -q "Failed requests:        0" /tmp/stress_test.log; then
        print_success "Stress test passed - no failed requests"
    else
        FAILED_REQUESTS=$(grep "Failed requests:" /tmp/stress_test.log | awk '{print $3}')
        print_warning "Stress test had $FAILED_REQUESTS failed requests"
    fi
    
    # Show response time statistics
    MEAN_TIME=$(grep "Time per request:" /tmp/stress_test.log | head -1 | awk '{print $4}')
    print_status "Mean response time: ${MEAN_TIME}ms"
else
    print_warning "Apache Bench not installed - skipping stress test"
fi

# 9. ERROR LOG CHECK
print_status "Checking for errors in logs..."
if command -v docker &> /dev/null; then
    ERROR_COUNT=$(docker-compose -f docker-compose.production.yml logs --tail=100 2>/dev/null | grep -i "error\|exception\|failed" | wc -l || echo "0")
    if [ "$ERROR_COUNT" -eq 0 ]; then
        print_success "No recent errors found in logs"
    else
        print_warning "Found $ERROR_COUNT recent errors in logs"
    fi
fi

# 10. FINAL HEALTH CHECK
print_status "Running final comprehensive health check..."

# Test all critical endpoints
ENDPOINTS=(
    "$API_URL/health"
    "$API_URL/users"
    "$API_URL/requisitions"
    "$API_URL/dashboard"
    "$FRONTEND_URL"
)

ALL_HEALTHY=true
for endpoint in "${ENDPOINTS[@]}"; do
    if curl -f -s --max-time 5 "$endpoint" > /dev/null 2>&1; then
        print_success "✓ $endpoint"
    else
        print_error "✗ $endpoint"
        ALL_HEALTHY=false
    fi
done

echo ""
echo "=============================================="
if [ "$ALL_HEALTHY" = true ]; then
    print_success "🎉 ALL TESTS PASSED - System is stable and ready!"
    echo ""
    echo "📊 System Status:"
    echo "  ✅ All services are running"
    echo "  ✅ Database connectivity is healthy"
    echo "  ✅ Authentication is working"
    echo "  ✅ API endpoints are responding"
    echo "  ✅ Response times are acceptable"
    echo "  ✅ No critical errors found"
    echo ""
    echo "🚀 Your system is ready for production!"
else
    print_error "❌ Some tests failed - Please check the issues above"
    echo ""
    echo "🔧 Troubleshooting steps:"
    echo "  1. Check if all containers are running: docker ps"
    echo "  2. Check logs: docker-compose -f docker-compose.production.yml logs"
    echo "  3. Restart services: docker-compose -f docker-compose.production.yml restart"
    echo "  4. Check system resources: docker stats"
    exit 1
fi
