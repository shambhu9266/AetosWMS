-- Create budget table
CREATE TABLE IF NOT EXISTS budget (
    id BIGSERIAL PRIMARY KEY,
    department VARCHAR(255) UNIQUE NOT NULL,
    total_budget DECIMAL(15,2),
    remaining_budget DECIMAL(15,2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Insert budget data from departments table
INSERT INTO budget (department, total_budget, remaining_budget)
SELECT 
    name as department,
    budget as total_budget,
    budget as remaining_budget
FROM departments 
WHERE budget IS NOT NULL
ON CONFLICT (department) DO NOTHING;

-- Add indexes for budget queries
CREATE INDEX IF NOT EXISTS idx_budget_department ON budget(department);
CREATE INDEX IF NOT EXISTS idx_budget_remaining ON budget(remaining_budget);

-- Add constraints
ALTER TABLE budget ADD CONSTRAINT IF NOT EXISTS chk_budget_total_positive CHECK (total_budget >= 0);
ALTER TABLE budget ADD CONSTRAINT IF NOT EXISTS chk_budget_remaining_positive CHECK (remaining_budget >= 0);
