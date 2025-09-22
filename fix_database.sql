-- Complete Database Restoration Script
-- Run this script in your PostgreSQL database

-- Connect to your database first:
-- psql -U postgres -d procuredb

-- ==============================================
-- 1. RESTORE USERS DATA
-- ==============================================
INSERT INTO users (username, password, full_name, role, department, is_active) 
VALUES 
    ('superadmin', 'admin123', 'Super Admin', 'SUPERADMIN', 'IT', TRUE),
    ('shambhu', 'shambhu123', 'Shambhu Sir', 'IT_MANAGER', 'IT', TRUE),
    ('joshi', 'joshi123', 'Joshi Sir', 'FINANCE_MANAGER', 'Finance', TRUE),
    ('employee1', 'emp123', 'John Doe', 'EMPLOYEE', 'IT', TRUE),
    ('employee2', 'emp123', 'Jane Smith', 'EMPLOYEE', 'Sales', TRUE);

-- ==============================================
-- 2. RESTORE BUDGET DATA
-- ==============================================
INSERT INTO budget (department, total_budget, remaining_budget) 
VALUES 
    ('IT', 150000.00, 120000.00),
    ('Sales', 200000.00, 85000.00),
    ('Management', 100000.00, 45000.00),
    ('Finance', 300000.00, 250000.00);

-- ==============================================
-- 3. ADD MISSING COLUMNS TO EXISTING TABLES
-- ==============================================
-- Add missing columns to vendor_pdf table
ALTER TABLE vendor_pdf ADD COLUMN IF NOT EXISTS is_rejected BOOLEAN DEFAULT FALSE;
ALTER TABLE vendor_pdf ADD COLUMN IF NOT EXISTS rejection_reason TEXT;

-- Update existing records
UPDATE vendor_pdf SET is_rejected = FALSE WHERE is_rejected IS NULL;

-- ==============================================
-- 4. CREATE SAMPLE REQUISITIONS (Optional)
-- ==============================================
-- Insert sample requisitions for testing
INSERT INTO requisition (item_name, quantity, price, created_by, department, status, approved_by_it, approved_by_finance, created_at) 
VALUES 
    ('Laptop', 2, 1500.00, 'employee1', 'IT', 'PENDING_IT_APPROVAL', NULL, NULL, NOW()),
    ('Office Chair', 5, 200.00, 'employee2', 'Sales', 'PENDING_FINANCE_APPROVAL', 'shambhu', NULL, NOW()),
    ('Printer', 1, 800.00, 'employee1', 'IT', 'APPROVED', 'shambhu', 'joshi', NOW());

-- Insert corresponding requisition items
INSERT INTO requisition_items (requisition_id, item_name, quantity, price) 
VALUES 
    (1, 'Laptop', 2, 1500.00),
    (2, 'Office Chair', 5, 200.00),
    (3, 'Printer', 1, 800.00);

-- ==============================================
-- 5. CREATE SAMPLE NOTIFICATIONS (Optional)
-- ==============================================
INSERT INTO notification (user_id, message, is_read, timestamp) 
VALUES 
    ('shambhu', 'New requisition #1 requires IT approval', FALSE, NOW()),
    ('joshi', 'New requisition #2 requires Finance approval', FALSE, NOW()),
    ('employee1', 'Your requisition #3 has been approved', FALSE, NOW());

-- ==============================================
-- 6. VERIFICATION QUERIES
-- ==============================================
-- Show all users
SELECT 'USERS:' as table_name;
SELECT id, username, full_name, role, department, is_active FROM users ORDER BY id;

-- Show all budgets
SELECT 'BUDGETS:' as table_name;
SELECT id, department, total_budget, remaining_budget FROM budget ORDER BY id;

-- Show all requisitions
SELECT 'REQUISITIONS:' as table_name;
SELECT id, item_name, quantity, price, created_by, department, status, created_at FROM requisition ORDER BY id;

-- Show all requisition items
SELECT 'REQUISITION ITEMS:' as table_name;
SELECT id, requisition_id, item_name, quantity, price FROM requisition_items ORDER BY id;

-- Show all notifications
SELECT 'NOTIFICATIONS:' as table_name;
SELECT notification_id, user_id, message, is_read, timestamp FROM notification ORDER BY notification_id;

-- Show table structures
SELECT 'TABLE STRUCTURES:' as info;
\d users;
\d budget;
\d requisition;
\d requisition_items;
\d notification;
