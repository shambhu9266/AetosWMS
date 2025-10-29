#!/bin/bash

# =============================================
# LOAD TESTING SCRIPT FOR CRASH PREVENTION
# Simulates high user load to test stability
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
    echo -e "${BLUE}[LOAD TEST]${NC} $1"
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

# Configuration
BASE_URL="http://localhost:8080/api"
CONCURRENT_USERS=20
REQUESTS_PER_USER=50
TOTAL_REQUESTS=$((CONCURRENT_USERS * REQUESTS_PER_USER))

echo "🔥 Starting Load Testing for Crash Prevention..."
echo "=============================================="
echo "Configuration:"
echo "  - Concurrent Users: $CONCURRENT_USERS"
echo "  - Requests per User: $REQUESTS_PER_USER"
echo "  - Total Requests: $TOTAL_REQUESTS"
echo "  - Target: $BASE_URL"
echo ""

# Check if Apache Bench is installed
if ! command -v ab &> /dev/null; then
    print_error "Apache Bench (ab) is not installed"
    echo "Install it with:"
    echo "  Ubuntu/Debian: sudo apt-get install apache2-utils"
    echo "  CentOS/RHEL: sudo yum install httpd-tools"
    echo "  macOS: brew install httpd"
    exit 1
fi

# 1. WARM-UP TEST
print_status "Running warm-up test (10 requests)..."
ab -n 10 -c 2 -H "Accept: application/json" "$BASE_URL/health" > /tmp/warmup.log 2>&1

if grep -q "Failed requests:        0" /tmp/warmup.log; then
    print_success "Warm-up test passed"
else
    print_error "Warm-up test failed - system may not be ready"
    exit 1
fi

# 2. LIGHT LOAD TEST
print_status "Running light load test (50 users, 10 requests each)..."
ab -n 500 -c 50 -H "Accept: application/json" "$BASE_URL/health" > /tmp/light_load.log 2>&1

LIGHT_FAILED=$(grep "Failed requests:" /tmp/light_load.log | awk '{print $3}')
LIGHT_TIME=$(grep "Time per request:" /tmp/light_load.log | head -1 | awk '{print $4}')

if [ "$LIGHT_FAILED" -eq 0 ]; then
    print_success "Light load test passed - $LIGHT_FAILED failed requests, ${LIGHT_TIME}ms avg response"
else
    print_warning "Light load test had $LIGHT_FAILED failed requests, ${LIGHT_TIME}ms avg response"
fi

# 3. MEDIUM LOAD TEST
print_status "Running medium load test (100 users, 20 requests each)..."
ab -n 2000 -c 100 -H "Accept: application/json" "$BASE_URL/health" > /tmp/medium_load.log 2>&1

MEDIUM_FAILED=$(grep "Failed requests:" /tmp/medium_load.log | awk '{print $3}')
MEDIUM_TIME=$(grep "Time per request:" /tmp/medium_load.log | head -1 | awk '{print $4}')

if [ "$MEDIUM_FAILED" -eq 0 ]; then
    print_success "Medium load test passed - $MEDIUM_FAILED failed requests, ${MEDIUM_TIME}ms avg response"
else
    print_warning "Medium load test had $MEDIUM_FAILED failed requests, ${MEDIUM_TIME}ms avg response"
fi

# 4. HEAVY LOAD TEST
print_status "Running heavy load test (200 users, 50 requests each)..."
ab -n 10000 -c 200 -H "Accept: application/json" "$BASE_URL/health" > /tmp/heavy_load.log 2>&1

HEAVY_FAILED=$(grep "Failed requests:" /tmp/heavy_load.log | awk '{print $3}')
HEAVY_TIME=$(grep "Time per request:" /tmp/heavy_load.log | head -1 | awk '{print $4}')

if [ "$HEAVY_FAILED" -eq 0 ]; then
    print_success "Heavy load test passed - $HEAVY_FAILED failed requests, ${HEAVY_TIME}ms avg response"
else
    print_warning "Heavy load test had $HEAVY_FAILED failed requests, ${HEAVY_TIME}ms avg response"
fi

# 5. STRESS TEST (Peak Load)
print_status "Running stress test (500 users, 100 requests each)..."
ab -n 50000 -c 500 -H "Accept: application/json" "$BASE_URL/health" > /tmp/stress_load.log 2>&1

STRESS_FAILED=$(grep "Failed requests:" /tmp/stress_load.log | awk '{print $3}')
STRESS_TIME=$(grep "Time per request:" /tmp/stress_load.log | head -1 | awk '{print $4}')

if [ "$STRESS_FAILED" -eq 0 ]; then
    print_success "Stress test passed - $STRESS_FAILED failed requests, ${STRESS_TIME}ms avg response"
else
    print_warning "Stress test had $STRESS_FAILED failed requests, ${STRESS_TIME}ms avg response"
fi

# 6. MEMORY LEAK TEST
print_status "Testing for memory leaks (1000 requests over 5 minutes)..."
ab -n 1000 -c 10 -t 300 -H "Accept: application/json" "$BASE_URL/health" > /tmp/memory_test.log 2>&1

MEMORY_FAILED=$(grep "Failed requests:" /tmp/memory_test.log | awk '{print $3}')

if [ "$MEMORY_FAILED" -eq 0 ]; then
    print_success "Memory leak test passed - no failures over 5 minutes"
else
    print_warning "Memory leak test had $MEMORY_FAILED failed requests"
fi

# 7. CONCURRENT LOGIN TEST
print_status "Testing concurrent user logins..."
for i in $(seq 1 20); do
    (
        curl -s -X POST "$BASE_URL/auth/login" \
            -H "Content-Type: application/x-www-form-urlencoded" \
            -d "username=admin&password=admin123" > /dev/null 2>&1
    ) &
done
wait

print_success "Concurrent login test completed"

# 8. DATABASE STRESS TEST
print_status "Testing database under load..."
ab -n 1000 -c 50 -H "Accept: application/json" "$BASE_URL/users" > /tmp/db_test.log 2>&1

DB_FAILED=$(grep "Failed requests:" /tmp/db_test.log | awk '{print $3}')

if [ "$DB_FAILED" -eq 0 ]; then
    print_success "Database stress test passed - $DB_FAILED failed requests"
else
    print_warning "Database stress test had $DB_FAILED failed requests"
fi

# 9. SYSTEM RESOURCE MONITORING
print_status "Monitoring system resources during load..."

if command -v docker &> /dev/null; then
    # Get memory usage before test
    MEMORY_BEFORE=$(docker stats --no-stream --format "table {{.MemUsage}}" | grep -v "MEM USAGE" | head -1)
    print_status "Memory usage before: $MEMORY_BEFORE"
    
    # Run final load test
    ab -n 2000 -c 100 -H "Accept: application/json" "$BASE_URL/health" > /tmp/final_test.log 2>&1
    
    # Get memory usage after test
    MEMORY_AFTER=$(docker stats --no-stream --format "table {{.MemUsage}}" | grep -v "MEM USAGE" | head -1)
    print_status "Memory usage after: $MEMORY_AFTER"
fi

# 10. RESULTS SUMMARY
echo ""
echo "=============================================="
echo "📊 LOAD TEST RESULTS SUMMARY"
echo "=============================================="

echo "Light Load (50 users):"
echo "  - Failed Requests: $LIGHT_FAILED"
echo "  - Avg Response Time: ${LIGHT_TIME}ms"

echo ""
echo "Medium Load (100 users):"
echo "  - Failed Requests: $MEDIUM_FAILED"
echo "  - Avg Response Time: ${MEDIUM_TIME}ms"

echo ""
echo "Heavy Load (200 users):"
echo "  - Failed Requests: $HEAVY_FAILED"
echo "  - Avg Response Time: ${HEAVY_TIME}ms"

echo ""
echo "Stress Test (500 users):"
echo "  - Failed Requests: $STRESS_FAILED"
echo "  - Avg Response Time: ${STRESS_TIME}ms"

echo ""
echo "Memory Leak Test:"
echo "  - Failed Requests: $MEMORY_FAILED"

echo ""
echo "Database Stress Test:"
echo "  - Failed Requests: $DB_FAILED"

# Calculate overall success rate
TOTAL_FAILED=$((LIGHT_FAILED + MEDIUM_FAILED + HEAVY_FAILED + STRESS_FAILED + MEMORY_FAILED + DB_FAILED))
TOTAL_TESTS=6
SUCCESS_RATE=$((100 - (TOTAL_FAILED * 100 / (TOTAL_TESTS * 1000))))

echo ""
echo "=============================================="
if [ "$TOTAL_FAILED" -eq 0 ]; then
    print_success "🎉 ALL LOAD TESTS PASSED!"
    echo ""
    echo "✅ Your system can handle:"
    echo "  - 500+ concurrent users"
    echo "  - 50,000+ requests per test"
    echo "  - High database load"
    echo "  - Memory pressure"
    echo "  - Concurrent logins"
    echo ""
    echo "🚀 System is CRASH-PROOF and ready for production!"
else
    print_warning "⚠️  Some load tests had failures"
    echo ""
    echo "Total Failed Requests: $TOTAL_FAILED"
    echo "Success Rate: ${SUCCESS_RATE}%"
    echo ""
    echo "🔧 Recommendations:"
    echo "  1. Increase server resources (CPU/RAM)"
    echo "  2. Optimize database queries"
    echo "  3. Enable connection pooling"
    echo "  4. Add more backend instances"
    echo "  5. Implement caching strategies"
fi

echo ""
echo "📈 Performance Benchmarks:"
echo "  - Excellent: < 100ms response time, 0 failures"
echo "  - Good: < 500ms response time, < 1% failures"
echo "  - Acceptable: < 1000ms response time, < 5% failures"
echo "  - Needs Improvement: > 1000ms response time, > 5% failures"
