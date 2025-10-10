-- =============================================
-- DATA ARCHIVING STRATEGY FOR LARGE SCALE
-- This migration creates archiving tables and functions
-- =============================================

-- 1. CREATE ARCHIVE TABLES
CREATE TABLE requisition_archive (
    id BIGINT,
    item_name VARCHAR(255),
    quantity INTEGER,
    price DECIMAL(19,2),
    created_by VARCHAR(255),
    department VARCHAR(255),
    status VARCHAR(50),
    approved_by_it VARCHAR(255),
    approved_by_finance VARCHAR(255),
    created_at TIMESTAMP,
    archived_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE notification_archive (
    notification_id BIGINT,
    user_id VARCHAR(255),
    message VARCHAR(1000),
    is_read BOOLEAN,
    timestamp TIMESTAMP,
    archived_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE vendor_pdf_archive (
    id BIGINT,
    file_name VARCHAR(255),
    original_file_name VARCHAR(255),
    file_path VARCHAR(500),
    uploaded_by VARCHAR(255),
    description TEXT,
    requisition_id BIGINT,
    uploaded_at TIMESTAMP,
    is_processed BOOLEAN,
    is_rejected BOOLEAN,
    rejection_reason TEXT,
    approval_stage VARCHAR(50),
    archived_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. CREATE SYSTEM LOG TABLE FOR TRACKING OPERATIONS
CREATE TABLE IF NOT EXISTS system_log (
    id BIGSERIAL PRIMARY KEY,
    operation VARCHAR(100) NOT NULL,
    details TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 3. CREATE AUTOMATED ARCHIVING FUNCTIONS
CREATE OR REPLACE FUNCTION archive_old_requisitions()
RETURNS void AS $$
DECLARE
    archived_count INTEGER;
BEGIN
    -- Archive requisitions older than 1 year with completed status
    INSERT INTO requisition_archive 
    SELECT * FROM requisition 
    WHERE created_at < CURRENT_DATE - INTERVAL '1 year'
    AND status IN ('APPROVED', 'REJECTED', 'COMPLETED');
    
    GET DIAGNOSTICS archived_count = ROW_COUNT;
    
    -- Delete archived requisitions
    DELETE FROM requisition 
    WHERE created_at < CURRENT_DATE - INTERVAL '1 year'
    AND status IN ('APPROVED', 'REJECTED', 'COMPLETED');
    
    -- Log the operation
    INSERT INTO system_log (operation, details, created_at) 
    VALUES ('ARCHIVE_REQUISITIONS', 
            'Archived ' || archived_count || ' old requisitions', 
            CURRENT_TIMESTAMP);
    
    RAISE NOTICE 'Archived % requisitions older than 1 year', archived_count;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION archive_old_notifications()
RETURNS void AS $$
DECLARE
    archived_count INTEGER;
BEGIN
    -- Archive read notifications older than 6 months
    INSERT INTO notification_archive 
    SELECT * FROM notification 
    WHERE is_read = true 
    AND timestamp < CURRENT_DATE - INTERVAL '6 months';
    
    GET DIAGNOSTICS archived_count = ROW_COUNT;
    
    -- Delete archived notifications
    DELETE FROM notification 
    WHERE is_read = true 
    AND timestamp < CURRENT_DATE - INTERVAL '6 months';
    
    -- Log the operation
    INSERT INTO system_log (operation, details, created_at) 
    VALUES ('ARCHIVE_NOTIFICATIONS', 
            'Archived ' || archived_count || ' old notifications', 
            CURRENT_TIMESTAMP);
    
    RAISE NOTICE 'Archived % notifications older than 6 months', archived_count;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION archive_old_pdfs()
RETURNS void AS $$
DECLARE
    archived_count INTEGER;
BEGIN
    -- Archive processed PDFs older than 2 years
    INSERT INTO vendor_pdf_archive 
    SELECT * FROM vendor_pdf 
    WHERE is_processed = true 
    AND uploaded_at < CURRENT_DATE - INTERVAL '2 years';
    
    GET DIAGNOSTICS archived_count = ROW_COUNT;
    
    -- Delete archived PDFs
    DELETE FROM vendor_pdf 
    WHERE is_processed = true 
    AND uploaded_at < CURRENT_DATE - INTERVAL '2 years';
    
    -- Log the operation
    INSERT INTO system_log (operation, details, created_at) 
    VALUES ('ARCHIVE_PDFS', 
            'Archived ' || archived_count || ' old PDFs', 
            CURRENT_TIMESTAMP);
    
    RAISE NOTICE 'Archived % PDFs older than 2 years', archived_count;
END;
$$ LANGUAGE plpgsql;

-- 4. CREATE MASTER ARCHIVING FUNCTION
CREATE OR REPLACE FUNCTION archive_old_data()
RETURNS void AS $$
BEGIN
    RAISE NOTICE 'Starting data archiving process...';
    
    -- Archive requisitions
    PERFORM archive_old_requisitions();
    
    -- Archive notifications
    PERFORM archive_old_notifications();
    
    -- Archive PDFs
    PERFORM archive_old_pdfs();
    
    -- Log completion
    INSERT INTO system_log (operation, details, created_at) 
    VALUES ('ARCHIVE_COMPLETE', 
            'Data archiving process completed successfully', 
            CURRENT_TIMESTAMP);
    
    RAISE NOTICE 'Data archiving process completed successfully';
END;
$$ LANGUAGE plpgsql;

-- 5. CREATE CLEANUP FUNCTION FOR OLD ARCHIVES
CREATE OR REPLACE FUNCTION cleanup_old_archives()
RETURNS void AS $$
DECLARE
    deleted_count INTEGER;
BEGIN
    -- Delete archive records older than 5 years
    DELETE FROM requisition_archive 
    WHERE created_at < CURRENT_DATE - INTERVAL '5 years';
    
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    
    DELETE FROM notification_archive 
    WHERE timestamp < CURRENT_DATE - INTERVAL '5 years';
    
    DELETE FROM vendor_pdf_archive 
    WHERE uploaded_at < CURRENT_DATE - INTERVAL '5 years';
    
    -- Log the operation
    INSERT INTO system_log (operation, details, created_at) 
    VALUES ('CLEANUP_ARCHIVES', 
            'Cleaned up ' || deleted_count || ' old archive records', 
            CURRENT_TIMESTAMP);
    
    RAISE NOTICE 'Cleaned up % old archive records', deleted_count;
END;
$$ LANGUAGE plpgsql;

-- 6. CREATE MONITORING VIEWS
CREATE OR REPLACE VIEW archive_monitoring AS
SELECT 
    'requisition' as table_name,
    count(*) as current_records,
    (SELECT count(*) FROM requisition_archive) as archived_records,
    pg_size_pretty(pg_total_relation_size('requisition')) as current_size,
    pg_size_pretty(pg_total_relation_size('requisition_archive')) as archive_size
FROM requisition
UNION ALL
SELECT 
    'notification' as table_name,
    count(*) as current_records,
    (SELECT count(*) FROM notification_archive) as archived_records,
    pg_size_pretty(pg_total_relation_size('notification')) as current_size,
    pg_size_pretty(pg_total_relation_size('notification_archive')) as archive_size
FROM notification
UNION ALL
SELECT 
    'vendor_pdf' as table_name,
    count(*) as current_records,
    (SELECT count(*) FROM vendor_pdf_archive) as archived_records,
    pg_size_pretty(pg_total_relation_size('vendor_pdf')) as current_size,
    pg_size_pretty(pg_total_relation_size('vendor_pdf_archive')) as archive_size
FROM vendor_pdf;

-- 7. CREATE INDEXES ON ARCHIVE TABLES
CREATE INDEX IF NOT EXISTS idx_requisition_archive_created_at ON requisition_archive(created_at);
CREATE INDEX IF NOT EXISTS idx_requisition_archive_status ON requisition_archive(status);
CREATE INDEX IF NOT EXISTS idx_notification_archive_timestamp ON notification_archive(timestamp);
CREATE INDEX IF NOT EXISTS idx_notification_archive_user_id ON notification_archive(user_id);
CREATE INDEX IF NOT EXISTS idx_vendor_pdf_archive_uploaded_at ON vendor_pdf_archive(uploaded_at);
CREATE INDEX IF NOT EXISTS idx_vendor_pdf_archive_processed ON vendor_pdf_archive(is_processed);

-- 8. CREATE SCHEDULED JOBS (if pg_cron extension is available)
-- These will run automatically to maintain database size
DO $$
BEGIN
    -- Try to create scheduled jobs if pg_cron is available
    BEGIN
        PERFORM cron.schedule('archive-old-data', '0 2 * * 0', 'SELECT archive_old_data();');
        PERFORM cron.schedule('cleanup-old-archives', '0 3 * * 0', 'SELECT cleanup_old_archives();');
        RAISE NOTICE 'Scheduled jobs created successfully';
    EXCEPTION
        WHEN undefined_function THEN
            RAISE NOTICE 'pg_cron extension not available. Please create scheduled jobs manually.';
    END;
END $$;

-- 9. ADD COMMENTS FOR DOCUMENTATION
COMMENT ON TABLE requisition_archive IS 'Archived requisitions older than 1 year';
COMMENT ON TABLE notification_archive IS 'Archived notifications older than 6 months';
COMMENT ON TABLE vendor_pdf_archive IS 'Archived PDFs older than 2 years';
COMMENT ON TABLE system_log IS 'System operation log for tracking maintenance tasks';
COMMENT ON FUNCTION archive_old_data() IS 'Master function to archive old data from all tables';
COMMENT ON FUNCTION cleanup_old_archives() IS 'Cleans up archive records older than 5 years';
COMMENT ON VIEW archive_monitoring IS 'Monitoring view for archive table sizes and record counts';
