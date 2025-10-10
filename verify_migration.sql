-- =============================================
-- VERIFICATION SCRIPT FOR DATABASE OPTIMIZATION
-- Run this after the main migration to verify results
-- =============================================

-- Check total index count
SELECT 'Total indexes: ' || count(*) as info FROM pg_indexes WHERE schemaname = 'public';

-- Check new indexes
SELECT 'New indexes created:' as info;
SELECT indexname FROM pg_indexes WHERE schemaname = 'public' 
AND indexname LIKE 'idx_%' ORDER BY indexname;

-- Check performance summary
SELECT 'Performance Summary:' as info;
SELECT * FROM performance_summary;

-- Check table sizes
SELECT 'Table Sizes:' as info;
SELECT * FROM table_size_monitor LIMIT 10;

-- Check index usage
SELECT 'Index Usage (Top 10):' as info;
SELECT * FROM index_usage_monitor LIMIT 10;

-- Check archive tables
SELECT 'Archive Tables:' as info;
SELECT t.relname as tablename FROM pg_class t 
JOIN pg_namespace n ON n.oid = t.relnamespace 
WHERE n.nspname = 'public' AND t.relkind = 'r' 
AND t.relname LIKE '%_archive';

-- Check system log
SELECT 'Recent System Logs:' as info;
SELECT * FROM system_log ORDER BY created_at DESC LIMIT 5;

-- Check performance alerts
SELECT 'Performance Alerts:' as info;
SELECT * FROM check_performance_alerts();
