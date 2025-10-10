-- =============================================
-- PERFORMANCE MONITORING FOR HIGH VOLUME
-- This migration creates monitoring views and functions
-- =============================================

-- 1. ENABLE PERFORMANCE MONITORING EXTENSIONS
CREATE EXTENSION IF NOT EXISTS pg_stat_statements;
CREATE EXTENSION IF NOT EXISTS pg_buffercache;

-- 2. CREATE PERFORMANCE MONITORING VIEWS
CREATE OR REPLACE VIEW performance_summary AS
SELECT 
    'Slow Queries (>1s)' as metric,
    count(*) as value
FROM pg_stat_statements 
WHERE mean_time > 1000
UNION ALL
SELECT 
    'Total Connections',
    count(*)
FROM pg_stat_activity 
WHERE datname = current_database()
UNION ALL
SELECT 
    'Active Connections',
    count(*)
FROM pg_stat_activity 
WHERE datname = current_database() AND state = 'active'
UNION ALL
SELECT 
    'Idle Connections',
    count(*)
FROM pg_stat_activity 
WHERE datname = current_database() AND state = 'idle'
UNION ALL
SELECT 
    'Database Size (MB)',
    round(pg_database_size(current_database()) / 1024 / 1024);

-- 3. CREATE TABLE SIZE MONITORING
CREATE OR REPLACE VIEW table_size_monitor AS
SELECT 
    tablename,
    pg_size_pretty(pg_total_relation_size(tablename::regclass)) as size,
    pg_total_relation_size(tablename::regclass) as size_bytes,
    pg_size_pretty(pg_relation_size(tablename::regclass)) as table_size,
    pg_size_pretty(pg_total_relation_size(tablename::regclass) - pg_relation_size(tablename::regclass)) as index_size
FROM pg_tables 
WHERE schemaname = 'public'
ORDER BY size_bytes DESC;

-- 4. CREATE INDEX USAGE MONITOR
CREATE OR REPLACE VIEW index_usage_monitor AS
SELECT 
    schemaname,
    tablename,
    indexname,
    idx_scan as times_used,
    idx_tup_read as tuples_read,
    idx_tup_fetch as tuples_fetched,
    pg_size_pretty(pg_relation_size(indexrelid)) as index_size,
    CASE 
        WHEN idx_scan = 0 THEN 'UNUSED'
        WHEN idx_scan < 100 THEN 'LOW USAGE'
        WHEN idx_scan < 1000 THEN 'MODERATE USAGE'
        ELSE 'HIGH USAGE'
    END as usage_status
FROM pg_stat_user_indexes 
WHERE schemaname = 'public'
ORDER BY idx_scan DESC;

-- 5. CREATE UNUSED INDEXES VIEW
CREATE OR REPLACE VIEW unused_indexes AS
SELECT 
    schemaname,
    tablename,
    indexname,
    pg_size_pretty(pg_relation_size(indexrelid)) as index_size,
    pg_relation_size(indexrelid) as size_bytes
FROM pg_stat_user_indexes 
WHERE idx_scan = 0 
AND schemaname = 'public'
ORDER BY pg_relation_size(indexrelid) DESC;

-- 6. CREATE SLOW QUERIES VIEW
CREATE OR REPLACE VIEW slow_queries AS
SELECT 
    query,
    calls,
    total_time,
    mean_time,
    rows,
    100.0 * shared_blks_hit / nullif(shared_blks_hit + shared_blks_read, 0) AS hit_percent,
    pg_size_pretty(shared_blks_hit * 8192) as cache_hit_size,
    pg_size_pretty(shared_blks_read * 8192) as disk_read_size
FROM pg_stat_statements 
WHERE mean_time > 100  -- Queries taking more than 100ms on average
ORDER BY mean_time DESC
LIMIT 20;

-- 7. CREATE CONNECTION MONITORING
CREATE OR REPLACE VIEW connection_monitor AS
SELECT 
    state,
    count(*) as connection_count,
    round(avg(extract(epoch from (now() - state_change)))) as avg_duration_seconds
FROM pg_stat_activity 
WHERE datname = current_database()
GROUP BY state
ORDER BY connection_count DESC;

-- 8. CREATE CACHE HIT RATIO MONITOR
CREATE OR REPLACE VIEW cache_hit_ratio AS
SELECT 
    'Buffer Cache Hit Ratio' as cache_type,
    round(100.0 * sum(blks_hit) / (sum(blks_hit) + sum(blks_read)), 2) as hit_ratio_percent
FROM pg_stat_database 
WHERE datname = current_database()
UNION ALL
SELECT 
    'Index Cache Hit Ratio',
    round(100.0 * sum(idx_blks_hit) / (sum(idx_blks_hit) + sum(idx_blks_read)), 2)
FROM pg_stat_database 
WHERE datname = current_database();

-- 9. CREATE TABLE STATISTICS MONITOR
CREATE OR REPLACE VIEW table_statistics AS
SELECT 
    schemaname,
    tablename,
    n_tup_ins as inserts,
    n_tup_upd as updates,
    n_tup_del as deletes,
    n_live_tup as live_tuples,
    n_dead_tup as dead_tuples,
    last_vacuum,
    last_autovacuum,
    last_analyze,
    last_autoanalyze
FROM pg_stat_user_tables 
WHERE schemaname = 'public'
ORDER BY n_live_tup DESC;

-- 10. CREATE PERFORMANCE ALERT FUNCTION
CREATE OR REPLACE FUNCTION check_performance_alerts()
RETURNS TABLE(alert_type text, message text, severity text) AS $$
BEGIN
    -- Check for slow queries
    IF EXISTS (SELECT 1 FROM pg_stat_statements WHERE mean_time > 5000) THEN
        RETURN QUERY SELECT 'SLOW_QUERIES'::text, 
            'Queries taking more than 5 seconds detected'::text, 
            'HIGH'::text;
    END IF;
    
    -- Check for high connection count
    IF (SELECT count(*) FROM pg_stat_activity WHERE datname = current_database()) > 80 THEN
        RETURN QUERY SELECT 'HIGH_CONNECTIONS'::text, 
            'Connection count is above 80'::text, 
            'MEDIUM'::text;
    END IF;
    
    -- Check for low cache hit ratio
    IF (SELECT round(100.0 * sum(blks_hit) / (sum(blks_hit) + sum(blks_read)), 2) 
        FROM pg_stat_database WHERE datname = current_database()) < 90 THEN
        RETURN QUERY SELECT 'LOW_CACHE_HIT_RATIO'::text, 
            'Buffer cache hit ratio is below 90%'::text, 
            'MEDIUM'::text;
    END IF;
    
    -- Check for unused indexes
    IF EXISTS (SELECT 1 FROM pg_stat_user_indexes WHERE idx_scan = 0 AND schemaname = 'public') THEN
        RETURN QUERY SELECT 'UNUSED_INDEXES'::text, 
            'Unused indexes detected - consider removing'::text, 
            'LOW'::text;
    END IF;
    
    -- Check for tables needing vacuum
    IF EXISTS (SELECT 1 FROM pg_stat_user_tables 
               WHERE n_dead_tup > n_live_tup * 0.1 AND schemaname = 'public') THEN
        RETURN QUERY SELECT 'TABLES_NEED_VACUUM'::text, 
            'Tables with high dead tuple ratio detected'::text, 
            'MEDIUM'::text;
    END IF;
    
    RETURN;
END;
$$ LANGUAGE plpgsql;

-- 11. CREATE PERFORMANCE REPORT FUNCTION
CREATE OR REPLACE FUNCTION generate_performance_report()
RETURNS void AS $$
DECLARE
    report_text text := '';
BEGIN
    report_text := '=== PERFORMANCE REPORT ===' || chr(10);
    report_text := report_text || 'Generated at: ' || now() || chr(10) || chr(10);
    
    -- Database size
    report_text := report_text || 'Database Size: ' || 
        pg_size_pretty(pg_database_size(current_database())) || chr(10);
    
    -- Connection count
    report_text := report_text || 'Active Connections: ' || 
        (SELECT count(*) FROM pg_stat_activity WHERE datname = current_database() AND state = 'active') || chr(10);
    
    -- Cache hit ratio
    report_text := report_text || 'Cache Hit Ratio: ' || 
        (SELECT round(100.0 * sum(blks_hit) / (sum(blks_hit) + sum(blks_read)), 2) 
         FROM pg_stat_database WHERE datname = current_database()) || '%' || chr(10);
    
    -- Slow queries count
    report_text := report_text || 'Slow Queries (>1s): ' || 
        (SELECT count(*) FROM pg_stat_statements WHERE mean_time > 1000) || chr(10);
    
    -- Unused indexes count
    report_text := report_text || 'Unused Indexes: ' || 
        (SELECT count(*) FROM pg_stat_user_indexes WHERE idx_scan = 0 AND schemaname = 'public') || chr(10);
    
    RAISE NOTICE '%', report_text;
    
    -- Log the report
    INSERT INTO system_log (operation, details, created_at) 
    VALUES ('PERFORMANCE_REPORT', report_text, CURRENT_TIMESTAMP);
END;
$$ LANGUAGE plpgsql;

-- 12. CREATE AUTOMATIC VACUUM TUNING
ALTER TABLE requisition SET (autovacuum_vacuum_scale_factor = 0.1);
ALTER TABLE notification SET (autovacuum_vacuum_scale_factor = 0.1);
ALTER TABLE vendor_pdf SET (autovacuum_vacuum_scale_factor = 0.1);
ALTER TABLE users SET (autovacuum_vacuum_scale_factor = 0.1);

-- 13. ADD COMMENTS FOR DOCUMENTATION
COMMENT ON VIEW performance_summary IS 'Summary of key performance metrics';
COMMENT ON VIEW table_size_monitor IS 'Monitor table and index sizes';
COMMENT ON VIEW index_usage_monitor IS 'Monitor index usage statistics';
COMMENT ON VIEW unused_indexes IS 'Identify unused indexes for potential removal';
COMMENT ON VIEW slow_queries IS 'Top 20 slowest queries by average execution time';
COMMENT ON VIEW connection_monitor IS 'Monitor database connections by state';
COMMENT ON VIEW cache_hit_ratio IS 'Monitor cache hit ratios';
COMMENT ON VIEW table_statistics IS 'Table statistics and maintenance information';
COMMENT ON FUNCTION check_performance_alerts() IS 'Check for performance issues and return alerts';
COMMENT ON FUNCTION generate_performance_report() IS 'Generate comprehensive performance report';
