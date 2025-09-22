-- Add PDF reject functionality columns to vendor_pdf table
-- This migration adds the missing columns for PDF rejection feature

-- Add is_rejected column (boolean, defaults to false)
ALTER TABLE vendor_pdf ADD COLUMN IF NOT EXISTS is_rejected BOOLEAN DEFAULT FALSE;

-- Add rejection_reason column (text, nullable)
ALTER TABLE vendor_pdf ADD COLUMN IF NOT EXISTS rejection_reason TEXT;

-- Update existing records to have is_rejected = false
UPDATE vendor_pdf SET is_rejected = FALSE WHERE is_rejected IS NULL;

-- Add comments for documentation
COMMENT ON COLUMN vendor_pdf.is_rejected IS 'Indicates if the PDF has been rejected by Finance team';
COMMENT ON COLUMN vendor_pdf.rejection_reason IS 'Reason provided by Finance team for rejecting the PDF';
