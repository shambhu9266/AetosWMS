-- =============================================
-- ADVANCED INDEXES FOR HIGH VOLUME DATA
-- This migration adds advanced indexes without partitioning
-- =============================================

-- 1. COVERING INDEXES (Include frequently accessed columns)
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_requisition_covering_status 
ON requisition(status, created_at DESC) 
INCLUDE (id, created_by, department, item_name, quantity, price);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_requisition_covering_user 
ON requisition(created_by, created_at DESC) 
INCLUDE (id, status, department, item_name, quantity, price);

-- 2. PARTIAL INDEXES FOR ACTIVE DATA
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_requisition_active 
ON requisition(created_at DESC) 
WHERE status IN ('PENDING_IT_APPROVAL', 'PENDING_FINANCE_APPROVAL', 'APPROVED');

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_notification_unread_active 
ON notification(user_id, timestamp DESC) 
WHERE is_read = false;

-- 3. COMPOSITE INDEXES FOR COMPLEX QUERIES
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_requisition_dept_status_date 
ON requisition(department, status, created_at DESC);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_vendor_pdf_requisition_stage 
ON vendor_pdf(requisition_id, approval_stage, uploaded_at DESC);

-- 4. FUNCTIONAL INDEXES FOR TEXT SEARCH
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_requisition_item_name_gin 
ON requisition USING gin(to_tsvector('english', item_name));

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_user_fullname_gin 
ON users USING gin(to_tsvector('english', full_name));

-- 5. BRIN INDEXES FOR LARGE TIME-SERIES DATA
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_requisition_created_at_brin 
ON requisition USING brin(created_at);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_notification_timestamp_brin 
ON notification USING brin(timestamp);

-- 6. UNIQUE INDEXES FOR DATA INTEGRITY
CREATE UNIQUE INDEX CONCURRENTLY IF NOT EXISTS idx_user_username_active 
ON users(username) WHERE is_active = true;

-- 7. FOREIGN KEY INDEXES (if not already present)
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_requisition_items_requisition_fk 
ON requisition_items(requisition_id);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_vendor_pdf_requisition_fk 
ON vendor_pdf(requisition_id);

-- 8. ADDITIONAL PERFORMANCE INDEXES
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_requisition_created_by_status 
ON requisition(created_by, status, created_at DESC);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_requisition_department_created_by 
ON requisition(department, created_by, created_at DESC);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_notification_user_read_timestamp 
ON notification(user_id, is_read, timestamp DESC);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_vendor_pdf_uploaded_by_processed 
ON vendor_pdf(uploaded_by, is_processed, uploaded_at DESC);

-- 9. INDEXES FOR APPROVAL WORKFLOW
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_requisition_approved_by_it 
ON requisition(approved_by_it, created_at DESC) WHERE approved_by_it IS NOT NULL;

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_requisition_approved_by_finance 
ON requisition(approved_by_finance, created_at DESC) WHERE approved_by_finance IS NOT NULL;

-- 10. INDEXES FOR BUDGET QUERIES
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_budget_department_active 
ON budget(department) WHERE total_budget > 0;

-- 11. ADD COMMENTS FOR DOCUMENTATION
COMMENT ON INDEX idx_requisition_covering_status IS 'Covering index for status-based queries with frequently accessed columns';
COMMENT ON INDEX idx_requisition_covering_user IS 'Covering index for user-based queries with frequently accessed columns';
COMMENT ON INDEX idx_requisition_active IS 'Partial index for active requisitions only';
COMMENT ON INDEX idx_notification_unread_active IS 'Partial index for unread notifications only';
COMMENT ON INDEX idx_requisition_dept_status_date IS 'Composite index for department and status queries';
COMMENT ON INDEX idx_vendor_pdf_requisition_stage IS 'Composite index for PDF approval workflow';
COMMENT ON INDEX idx_requisition_item_name_gin IS 'Full-text search index for item names';
COMMENT ON INDEX idx_user_fullname_gin IS 'Full-text search index for user full names';
COMMENT ON INDEX idx_requisition_created_at_brin IS 'BRIN index for time-series data on requisitions';
COMMENT ON INDEX idx_notification_timestamp_brin IS 'BRIN index for time-series data on notifications';
