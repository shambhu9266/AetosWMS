-- Create requisition_items table for multiple items per requisition
-- This migration adds support for multiple line items in a single requisition

-- Create requisition_items table
CREATE TABLE IF NOT EXISTS requisition_items (
    id BIGSERIAL PRIMARY KEY,
    requisition_id BIGINT NOT NULL,
    item_name VARCHAR(255) NOT NULL,
    quantity INTEGER NOT NULL,
    price DECIMAL(19,2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign key constraint
    CONSTRAINT fk_requisition_items_requisition 
        FOREIGN KEY (requisition_id) 
        REFERENCES requisition(id) 
        ON DELETE CASCADE
);

-- Create index for better performance
CREATE INDEX IF NOT EXISTS idx_requisition_items_requisition_id 
    ON requisition_items(requisition_id);

-- Add comments for documentation
COMMENT ON TABLE requisition_items IS 'Line items for requisitions - supports multiple items per requisition';
COMMENT ON COLUMN requisition_items.requisition_id IS 'Foreign key to requisition table';
COMMENT ON COLUMN requisition_items.item_name IS 'Name of the item being requested';
COMMENT ON COLUMN requisition_items.quantity IS 'Quantity of the item';
COMMENT ON COLUMN requisition_items.price IS 'Unit price of the item';
COMMENT ON COLUMN requisition_items.created_at IS 'Timestamp when the item was added';
