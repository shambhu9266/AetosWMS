-- =============================================
-- SAFE MIGRATION RUNNER FOR DATABASE OPTIMIZATION
-- Run this script to apply all optimizations safely
-- =============================================

-- Check current database state
\echo '=== CHECKING CURRENT DATABASE STATE ==='
SELECT 'Current database size: ' || pg_size_pretty(pg_database_size(current_database())) as info;
SELECT 'Current table count: ' || count(*) as info FROM pg_tables WHERE schemaname = 'public';
SELECT 'Current index count: ' || count(*) as info FROM pg_indexes WHERE schemaname = 'public';

-- Run V5: Advanced Indexes
\echo '=== APPLYING ADVANCED INDEXES (V5) ==='
\i backend/src/main/resources/db/migration/V5__Advanced_Indexes.sql

-- Run V6: Data Archiving Strategy
\echo '=== APPLYING DATA ARCHIVING STRATEGY (V6) ==='
\i backend/src/main/resources/db/migration/V6__Data_Archiving_Strategy.sql

-- Run V7: Performance Monitoring
\echo '=== APPLYING PERFORMANCE MONITORING (V7) ==='
\i backend/src/main/resources/db/migration/V7__Performance_Monitoring.sql

-- Verify all optimizations
\echo '=== VERIFYING OPTIMIZATIONS ==='
SELECT 'New database size: ' || pg_size_pretty(pg_database_size(current_database())) as info;
SELECT 'New index count: ' || count(*) as info FROM pg_indexes WHERE schemaname = 'public';

-- Show performance summary
\echo '=== PERFORMANCE SUMMARY ==='
SELECT * FROM performance_summary;

-- Show table sizes
\echo '=== TABLE SIZES ==='
SELECT * FROM table_size_monitor LIMIT 10;

-- Show index usage
\echo '=== INDEX USAGE ==='
SELECT * FROM index_usage_monitor LIMIT 10;

\echo '=== MIGRATION COMPLETED SUCCESSFULLY ==='
