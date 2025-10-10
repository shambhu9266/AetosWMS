-- Add approval_stage column to vendor_pdf table
ALTER TABLE vendor_pdf ADD COLUMN approval_stage VARCHAR(50) DEFAULT 'DEPARTMENT';

-- Update existing PDFs to have DEPARTMENT stage
UPDATE vendor_pdf SET approval_stage = 'DEPARTMENT' WHERE approval_stage IS NULL;
