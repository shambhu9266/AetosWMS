package com.example.backend.repo;

import com.example.backend.model.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {
    
    // Find department by name
    Optional<Department> findByName(String name);
    
    // Find departments by active status
    List<Department> findByIsActive(Boolean isActive);
    
    // Find department by manager username
    Optional<Department> findByManagerUsername(String managerUsername);
    
    // Find departments with budget greater than specified amount
    @Query("SELECT d FROM Department d WHERE d.budget > :budget AND d.isActive = true")
    List<Department> findActiveDepartmentsWithBudgetGreaterThan(@Param("budget") java.math.BigDecimal budget);
    
    // Find departments by name containing (case insensitive)
    @Query("SELECT d FROM Department d WHERE LOWER(d.name) LIKE LOWER(CONCAT('%', :name, '%')) AND d.isActive = true")
    List<Department> findActiveDepartmentsByNameContaining(@Param("name") String name);
    
    // Count active departments
    long countByIsActive(Boolean isActive);
    
    // Find all active departments ordered by name
    List<Department> findByIsActiveTrueOrderByNameAsc();
    
    // Check if department name exists (excluding specific ID)
    @Query("SELECT COUNT(d) > 0 FROM Department d WHERE d.name = :name AND d.id != :id")
    boolean existsByNameAndIdNot(@Param("name") String name, @Param("id") Long id);
}
