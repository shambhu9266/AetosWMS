-- Add department column to vendor_pdf table
ALTER TABLE vendor_pdf ADD COLUMN department VARCHAR(255);

-- Create index for better performance on department queries
CREATE INDEX IF NOT EXISTS idx_vendor_pdf_department ON vendor_pdf(department);

-- Update existing records with department information based on uploadedBy user's department
UPDATE vendor_pdf 
SET department = (
    SELECT u.department 
    FROM users u 
    WHERE u.username = vendor_pdf.uploaded_by
)
WHERE department IS NULL;

-- Set default department for any records that couldn't be matched
UPDATE vendor_pdf 
SET department = 'Sales' 
WHERE department IS NULL;