#!/bin/bash

# =============================================
# REAL-TIME SYSTEM MONITORING SCRIPT
# Monitors system health to prevent crashes
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
    echo -e "${BLUE}[MONITOR]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[HEALTHY]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[CRITICAL]${NC} $1"
}

# Configuration
BASE_URL="http://localhost:8080/api"
FRONTEND_URL="http://localhost:80"
MONITOR_INTERVAL=30  # seconds
LOG_FILE="/tmp/system_monitor.log"
ALERT_THRESHOLD_CPU=80
ALERT_THRESHOLD_MEMORY=85
ALERT_THRESHOLD_RESPONSE_TIME=5000  # 5 seconds

# Create log file if it doesn't exist
touch "$LOG_FILE"

echo "🔍 Starting Real-Time System Monitoring..."
echo "=============================================="
echo "Monitoring interval: ${MONITOR_INTERVAL} seconds"
echo "Log file: $LOG_FILE"
echo "Press Ctrl+C to stop monitoring"
echo ""

# Function to log with timestamp
log_message() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" >> "$LOG_FILE"
}

# Function to check API health
check_api_health() {
    local response_time=$(curl -o /dev/null -s -w '%{time_total}' --max-time 10 "$BASE_URL/health" 2>/dev/null || echo "999")
    local response_time_ms=$(echo "$response_time * 1000" | bc -l | cut -d. -f1)
    
    if [ "$response_time_ms" -lt "$ALERT_THRESHOLD_RESPONSE_TIME" ]; then
        print_success "API Health: ${response_time_ms}ms"
        log_message "API Health: OK - ${response_time_ms}ms"
        return 0
    else
        print_error "API Health: ${response_time_ms}ms (SLOW)"
        log_message "API Health: SLOW - ${response_time_ms}ms"
        return 1
    fi
}

# Function to check database health
check_database_health() {
    local db_status=$(curl -s --max-time 10 "$BASE_URL/health" | grep -o '"database":"[^"]*"' | cut -d'"' -f4 || echo "DOWN")
    
    if [ "$db_status" = "UP" ]; then
        print_success "Database: $db_status"
        log_message "Database: OK - $db_status"
        return 0
    else
        print_error "Database: $db_status"
        log_message "Database: ERROR - $db_status"
        return 1
    fi
}

# Function to check Redis health
check_redis_health() {
    local redis_status=$(curl -s --max-time 10 "$BASE_URL/health" | grep -o '"redis":"[^"]*"' | cut -d'"' -f4 || echo "DOWN")
    
    if [ "$redis_status" = "UP" ]; then
        print_success "Redis: $redis_status"
        log_message "Redis: OK - $redis_status"
        return 0
    else
        print_warning "Redis: $redis_status"
        log_message "Redis: WARNING - $redis_status"
        return 1
    fi
}

# Function to check frontend health
check_frontend_health() {
    local response_time=$(curl -o /dev/null -s -w '%{time_total}' --max-time 10 "$FRONTEND_URL" 2>/dev/null || echo "999")
    local response_time_ms=$(echo "$response_time * 1000" | bc -l | cut -d. -f1)
    
    if [ "$response_time_ms" -lt "$ALERT_THRESHOLD_RESPONSE_TIME" ]; then
        print_success "Frontend: ${response_time_ms}ms"
        log_message "Frontend: OK - ${response_time_ms}ms"
        return 0
    else
        print_error "Frontend: ${response_time_ms}ms (SLOW)"
        log_message "Frontend: SLOW - ${response_time_ms}ms"
        return 1
    fi
}

# Function to check Docker containers
check_docker_containers() {
    if ! command -v docker &> /dev/null; then
        print_warning "Docker not available"
        return 1
    fi
    
    local running_containers=$(docker ps --filter "name=procure" --format "table {{.Names}}" | wc -l)
    local expected_containers=4  # postgres, redis, backend, frontend
    
    if [ "$running_containers" -ge "$expected_containers" ]; then
        print_success "Docker Containers: $running_containers/$expected_containers running"
        log_message "Docker Containers: OK - $running_containers/$expected_containers"
        return 0
    else
        print_error "Docker Containers: $running_containers/$expected_containers running"
        log_message "Docker Containers: ERROR - $running_containers/$expected_containers"
        return 1
    fi
}

# Function to check system resources
check_system_resources() {
    if ! command -v docker &> /dev/null; then
        return 1
    fi
    
    # Get container stats
    local stats=$(docker stats --no-stream --format "table {{.Container}}\t{{.CPUPerc}}\t{{.MemUsage}}" | grep -v "CONTAINER")
    
    while IFS= read -r line; do
        if [ -n "$line" ]; then
            local container=$(echo "$line" | awk '{print $1}')
            local cpu_percent=$(echo "$line" | awk '{print $2}' | sed 's/%//')
            local memory_usage=$(echo "$line" | awk '{print $3}')
            
            # Check CPU usage
            if (( $(echo "$cpu_percent > $ALERT_THRESHOLD_CPU" | bc -l) )); then
                print_warning "$container CPU: ${cpu_percent}% (HIGH)"
                log_message "$container CPU: HIGH - ${cpu_percent}%"
            else
                print_success "$container CPU: ${cpu_percent}%"
            fi
            
            # Check memory usage (extract percentage from memory string)
            local memory_percent=$(echo "$memory_usage" | grep -o '[0-9.]*%' | sed 's/%//' || echo "0")
            if (( $(echo "$memory_percent > $ALERT_THRESHOLD_MEMORY" | bc -l) )); then
                print_warning "$container Memory: ${memory_percent}% (HIGH)"
                log_message "$container Memory: HIGH - ${memory_percent}%"
            else
                print_success "$container Memory: ${memory_percent}%"
            fi
        fi
    done <<< "$stats"
}

# Function to check error logs
check_error_logs() {
    if ! command -v docker &> /dev/null; then
        return 1
    fi
    
    local error_count=$(docker-compose -f docker-compose.production.yml logs --tail=50 2>/dev/null | grep -i "error\|exception\|failed" | wc -l || echo "0")
    
    if [ "$error_count" -eq 0 ]; then
        print_success "Error Logs: No recent errors"
        log_message "Error Logs: OK - No recent errors"
    else
        print_warning "Error Logs: $error_count recent errors"
        log_message "Error Logs: WARNING - $error_count recent errors"
    fi
}

# Function to check disk space
check_disk_space() {
    local disk_usage=$(df -h / | awk 'NR==2 {print $5}' | sed 's/%//')
    
    if [ "$disk_usage" -lt 90 ]; then
        print_success "Disk Space: ${disk_usage}% used"
        log_message "Disk Space: OK - ${disk_usage}% used"
    else
        print_warning "Disk Space: ${disk_usage}% used (HIGH)"
        log_message "Disk Space: WARNING - ${disk_usage}% used"
    fi
}

# Function to perform comprehensive health check
comprehensive_health_check() {
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    echo ""
    echo "=============================================="
    echo "🔍 Health Check - $timestamp"
    echo "=============================================="
    
    local total_checks=0
    local failed_checks=0
    
    # Run all health checks
    check_api_health || ((failed_checks++))
    ((total_checks++))
    
    check_database_health || ((failed_checks++))
    ((total_checks++))
    
    check_redis_health || ((failed_checks++))
    ((total_checks++))
    
    check_frontend_health || ((failed_checks++))
    ((total_checks++))
    
    check_docker_containers || ((failed_checks++))
    ((total_checks++))
    
    check_system_resources
    ((total_checks++))
    
    check_error_logs || ((failed_checks++))
    ((total_checks++))
    
    check_disk_space || ((failed_checks++))
    ((total_checks++))
    
    # Calculate health score
    local health_score=$((100 - (failed_checks * 100 / total_checks)))
    
    echo ""
    echo "=============================================="
    echo "📊 Health Score: $health_score%"
    echo "   Total Checks: $total_checks"
    echo "   Failed Checks: $failed_checks"
    echo "   Passed Checks: $((total_checks - failed_checks))"
    
    if [ "$health_score" -ge 90 ]; then
        print_success "System Status: EXCELLENT"
        log_message "System Status: EXCELLENT - $health_score%"
    elif [ "$health_score" -ge 75 ]; then
        print_success "System Status: GOOD"
        log_message "System Status: GOOD - $health_score%"
    elif [ "$health_score" -ge 50 ]; then
        print_warning "System Status: FAIR"
        log_message "System Status: FAIR - $health_score%"
    else
        print_error "System Status: POOR"
        log_message "System Status: POOR - $health_score%"
    fi
    
    echo "=============================================="
}

# Function to show real-time stats
show_realtime_stats() {
    echo ""
    echo "📈 Real-Time Statistics:"
    echo "  - Monitoring since: $(date '+%Y-%m-%d %H:%M:%S')"
    echo "  - Log file: $LOG_FILE"
    echo "  - Alert thresholds:"
    echo "    * CPU: ${ALERT_THRESHOLD_CPU}%"
    echo "    * Memory: ${ALERT_THRESHOLD_MEMORY}%"
    echo "    * Response Time: ${ALERT_THRESHOLD_RESPONSE_TIME}ms"
    echo ""
}

# Main monitoring loop
main() {
    show_realtime_stats
    
    # Initial health check
    comprehensive_health_check
    
    # Start monitoring loop
    while true; do
        sleep "$MONITOR_INTERVAL"
        comprehensive_health_check
    done
}

# Handle Ctrl+C gracefully
trap 'echo ""; echo "🛑 Monitoring stopped by user"; echo "📄 Log file saved at: $LOG_FILE"; exit 0' INT

# Start monitoring
main
