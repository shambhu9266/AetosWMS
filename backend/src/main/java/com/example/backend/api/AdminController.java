package com.example.backend.api;

import com.example.backend.service.OptimizedRequisitionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = {"http://localhost:4200", "http://20.57.79.136", "http://20.57.79.136:80", "http://20.57.79.136:8080"})
public class AdminController {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    private OptimizedRequisitionService optimizedRequisitionService;
    
    // Database monitoring endpoints
    @GetMapping("/performance-summary")
    public List<Map<String, Object>> getPerformanceSummary() {
        String sql = "SELECT * FROM performance_summary";
        return jdbcTemplate.queryForList(sql);
    }
    
    @GetMapping("/table-sizes")
    public List<Map<String, Object>> getTableSizes() {
        String sql = "SELECT * FROM table_size_monitor";
        return jdbcTemplate.queryForList(sql);
    }
    
    @GetMapping("/index-usage")
    public List<Map<String, Object>> getIndexUsage() {
        String sql = "SELECT * FROM index_usage_monitor";
        return jdbcTemplate.queryForList(sql);
    }
    
    @GetMapping("/unused-indexes")
    public List<Map<String, Object>> getUnusedIndexes() {
        String sql = "SELECT * FROM unused_indexes";
        return jdbcTemplate.queryForList(sql);
    }
    
    @GetMapping("/slow-queries")
    public List<Map<String, Object>> getSlowQueries() {
        String sql = "SELECT * FROM slow_queries";
        return jdbcTemplate.queryForList(sql);
    }
    
    @GetMapping("/connection-monitor")
    public List<Map<String, Object>> getConnectionMonitor() {
        String sql = "SELECT * FROM connection_monitor";
        return jdbcTemplate.queryForList(sql);
    }
    
    @GetMapping("/cache-hit-ratio")
    public List<Map<String, Object>> getCacheHitRatio() {
        String sql = "SELECT * FROM cache_hit_ratio";
        return jdbcTemplate.queryForList(sql);
    }
    
    @GetMapping("/table-statistics")
    public List<Map<String, Object>> getTableStatistics() {
        String sql = "SELECT * FROM table_statistics";
        return jdbcTemplate.queryForList(sql);
    }
    
    @GetMapping("/archive-monitoring")
    public List<Map<String, Object>> getArchiveMonitoring() {
        String sql = "SELECT * FROM archive_monitoring";
        return jdbcTemplate.queryForList(sql);
    }
    
    // Performance alerts
    @GetMapping("/performance-alerts")
    public List<Map<String, Object>> getPerformanceAlerts() {
        String sql = "SELECT * FROM check_performance_alerts()";
        return jdbcTemplate.queryForList(sql);
    }
    
    // Maintenance operations
    @PostMapping("/archive-old-data")
    public Map<String, Object> archiveOldData() {
        try {
            jdbcTemplate.execute("SELECT archive_old_data()");
            return Map.of("success", true, "message", "Data archiving completed successfully");
        } catch (Exception e) {
            return Map.of("success", false, "message", "Error archiving data: " + e.getMessage());
        }
    }
    
    @PostMapping("/cleanup-old-archives")
    public Map<String, Object> cleanupOldArchives() {
        try {
            jdbcTemplate.execute("SELECT cleanup_old_archives()");
            return Map.of("success", true, "message", "Archive cleanup completed successfully");
        } catch (Exception e) {
            return Map.of("success", false, "message", "Error cleaning up archives: " + e.getMessage());
        }
    }
    
    @PostMapping("/generate-performance-report")
    public Map<String, Object> generatePerformanceReport() {
        try {
            jdbcTemplate.execute("SELECT generate_performance_report()");
            return Map.of("success", true, "message", "Performance report generated successfully");
        } catch (Exception e) {
            return Map.of("success", false, "message", "Error generating report: " + e.getMessage());
        }
    }
    
    @PostMapping("/ensure-future-partitions")
    public Map<String, Object> ensureFuturePartitions() {
        try {
            jdbcTemplate.execute("SELECT ensure_future_partitions()");
            return Map.of("success", true, "message", "Future partitions ensured successfully");
        } catch (Exception e) {
            return Map.of("success", false, "message", "Error ensuring partitions: " + e.getMessage());
        }
    }
    
    // Cache management
    @PostMapping("/clear-cache")
    public Map<String, Object> clearCache() {
        try {
            optimizedRequisitionService.clearAllCaches();
            return Map.of("success", true, "message", "Cache cleared successfully");
        } catch (Exception e) {
            return Map.of("success", false, "message", "Error clearing cache: " + e.getMessage());
        }
    }
    
    // System logs
    @GetMapping("/system-logs")
    public List<Map<String, Object>> getSystemLogs(@RequestParam(defaultValue = "0") int page, 
                                                   @RequestParam(defaultValue = "50") int size) {
        String sql = "SELECT * FROM system_log ORDER BY created_at DESC LIMIT ? OFFSET ?";
        return jdbcTemplate.queryForList(sql, size, page * size);
    }
    
    // Database health check
    @GetMapping("/health")
    public Map<String, Object> getDatabaseHealth() {
        try {
            // Check database connectivity
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            
            // Get basic stats
            String sql = "SELECT * FROM performance_summary";
            List<Map<String, Object>> stats = jdbcTemplate.queryForList(sql);
            
            return Map.of(
                "status", "healthy",
                "timestamp", System.currentTimeMillis(),
                "stats", stats
            );
        } catch (Exception e) {
            return Map.of(
                "status", "unhealthy",
                "error", e.getMessage(),
                "timestamp", System.currentTimeMillis()
            );
        }
    }
}
