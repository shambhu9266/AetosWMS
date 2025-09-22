-- Performance optimization indexes for production use
-- This file should be placed in src/main/resources/db/migration/ for Flyway

-- Requisition table indexes
CREATE INDEX IF NOT EXISTS idx_requisition_status ON requisition(status);
CREATE INDEX IF NOT EXISTS idx_requisition_created_by ON requisition(created_by);
CREATE INDEX IF NOT EXISTS idx_requisition_department ON requisition(department);
CREATE INDEX IF NOT EXISTS idx_requisition_created_at ON requisition(created_at);
CREATE INDEX IF NOT EXISTS idx_requisition_approved_by_it ON requisition(approved_by_it);
CREATE INDEX IF NOT EXISTS idx_requisition_approved_by_finance ON requisition(approved_by_finance);

-- Composite indexes for common queries
CREATE INDEX IF NOT EXISTS idx_requisition_status_created_at ON requisition(status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_requisition_created_by_created_at ON requisition(created_by, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_requisition_department_created_at ON requisition(department, created_at DESC);

-- Vendor PDF table indexes
CREATE INDEX IF NOT EXISTS idx_vendor_pdf_uploaded_by ON vendor_pdf(uploaded_by);
CREATE INDEX IF NOT EXISTS idx_vendor_pdf_uploaded_at ON vendor_pdf(uploaded_at);
CREATE INDEX IF NOT EXISTS idx_vendor_pdf_is_processed ON vendor_pdf(is_processed);
CREATE INDEX IF NOT EXISTS idx_vendor_pdf_is_rejected ON vendor_pdf(is_rejected);
CREATE INDEX IF NOT EXISTS idx_vendor_pdf_requisition_id ON vendor_pdf(requisition_id);

-- Composite indexes for PDF queries
CREATE INDEX IF NOT EXISTS idx_vendor_pdf_uploaded_by_uploaded_at ON vendor_pdf(uploaded_by, uploaded_at DESC);
CREATE INDEX IF NOT EXISTS idx_vendor_pdf_processed_uploaded_at ON vendor_pdf(is_processed, uploaded_at DESC);
CREATE INDEX IF NOT EXISTS idx_vendor_pdf_rejected_uploaded_at ON vendor_pdf(is_rejected, uploaded_at DESC);

-- Notification table indexes
CREATE INDEX IF NOT EXISTS idx_notification_user_id ON notification(user_id);
CREATE INDEX IF NOT EXISTS idx_notification_is_read ON notification(is_read);
CREATE INDEX IF NOT EXISTS idx_notification_timestamp ON notification(timestamp);

-- Composite indexes for notification queries
CREATE INDEX IF NOT EXISTS idx_notification_user_id_is_read ON notification(user_id, is_read);
CREATE INDEX IF NOT EXISTS idx_notification_user_id_timestamp ON notification(user_id, timestamp DESC);

-- Budget table indexes
CREATE INDEX IF NOT EXISTS idx_budget_department ON budget(department);

-- Requisition items table indexes
CREATE INDEX IF NOT EXISTS idx_requisition_items_requisition_id ON requisition_items(requisition_id);

-- Approval table indexes
CREATE INDEX IF NOT EXISTS idx_approval_requisition_id ON approval(requisition_id);
CREATE INDEX IF NOT EXISTS idx_approval_approver_role ON approval(approver_role);

-- User table indexes
CREATE INDEX IF NOT EXISTS idx_user_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_user_role ON users(role);
CREATE INDEX IF NOT EXISTS idx_user_department ON users(department);
CREATE INDEX IF NOT EXISTS idx_user_is_active ON users(is_active);

-- Composite index for user queries
CREATE INDEX IF NOT EXISTS idx_user_role_is_active ON users(role, is_active);

-- Add constraints for data integrity
ALTER TABLE requisition ADD CONSTRAINT IF NOT EXISTS chk_requisition_quantity_positive CHECK (quantity > 0);
ALTER TABLE requisition ADD CONSTRAINT IF NOT EXISTS chk_requisition_price_positive CHECK (price >= 0);
ALTER TABLE budget ADD CONSTRAINT IF NOT EXISTS chk_budget_total_positive CHECK (total_budget >= 0);
ALTER TABLE budget ADD CONSTRAINT IF NOT EXISTS chk_budget_remaining_positive CHECK (remaining_budget >= 0);

-- Add partial indexes for better performance on filtered queries
CREATE INDEX IF NOT EXISTS idx_requisition_pending_it ON requisition(created_at DESC) WHERE status = 'PENDING_IT_APPROVAL';
CREATE INDEX IF NOT EXISTS idx_requisition_pending_finance ON requisition(created_at DESC) WHERE status = 'PENDING_FINANCE_APPROVAL';
CREATE INDEX IF NOT EXISTS idx_requisition_approved ON requisition(created_at DESC) WHERE status = 'APPROVED';
CREATE INDEX IF NOT EXISTS idx_vendor_pdf_unprocessed ON vendor_pdf(uploaded_at DESC) WHERE is_processed = false;
CREATE INDEX IF NOT EXISTS idx_notification_unread ON notification(timestamp DESC) WHERE is_read = false;
