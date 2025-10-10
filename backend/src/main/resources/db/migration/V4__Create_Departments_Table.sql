-- Create departments table
CREATE TABLE IF NOT EXISTS departments (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description VARCHAR(500),
    manager_name VARCHAR(255),
    manager_username VARCHAR(255),
    budget DECIMAL(15,2),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_departments_name ON departments(name);
CREATE INDEX IF NOT EXISTS idx_departments_manager_username ON departments(manager_username);
CREATE INDEX IF NOT EXISTS idx_departments_is_active ON departments(is_active);

-- Create trigger for updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

DROP TRIGGER IF EXISTS update_departments_updated_at ON departments;
CREATE TRIGGER update_departments_updated_at 
    BEFORE UPDATE ON departments 
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();

-- Insert initial department data
INSERT INTO departments (name, description, manager_name, manager_username, budget, is_active) VALUES
('IT', 'Information Technology Department', 'IT Department Manager', 'itmanager', 150000.00, TRUE),
('Sales', 'Sales and Marketing Department', 'Sales Manager', 'salesmanager', 200000.00, TRUE),
('Finance', 'Finance and Accounting Department', 'Joshi Sir', 'joshi', 300000.00, TRUE),
('Management', 'Management and Administration', 'Management Head', NULL, 100000.00, TRUE),
('HR', 'Human Resources Department', 'HR Manager', NULL, 80000.00, TRUE),
('Operations', 'Operations and Logistics', 'Operations Manager', NULL, 180000.00, TRUE)
ON CONFLICT (name) DO UPDATE SET 
    description = EXCLUDED.description,
    manager_name = EXCLUDED.manager_name,
    manager_username = EXCLUDED.manager_username,
    budget = EXCLUDED.budget,
    updated_at = CURRENT_TIMESTAMP;
