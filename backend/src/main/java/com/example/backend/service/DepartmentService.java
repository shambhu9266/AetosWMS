package com.example.backend.service;

import com.example.backend.model.Department;
import com.example.backend.model.User;
import com.example.backend.model.UserRole;
import com.example.backend.repo.DepartmentRepository;
import com.example.backend.repo.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class DepartmentService {
    
    @Autowired
    private DepartmentRepository departmentRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private AuthService authService;
    
    public Map<String, Object> getAllDepartments(String token) {
        try {
            // Check if current user has permission (SUPERADMIN or DEPARTMENT_MANAGER)
            User currentUser = authService.getCurrentUser(token);
            if (currentUser == null) {
                return Map.of("success", false, "message", "Invalid session");
            }
            
            if (currentUser.getRole() != UserRole.SUPERADMIN && 
                currentUser.getRole() != UserRole.DEPARTMENT_MANAGER) {
                return Map.of("success", false, "message", "Access denied. Only SUPERADMIN and Department Managers can view departments.");
            }
            
            List<Department> departments = departmentRepository.findByIsActiveTrueOrderByNameAsc();
            
            // Convert to response format
            List<Map<String, Object>> departmentList = departments.stream()
                .map(this::convertToMap)
                .toList();
            
            return Map.of("success", true, "departments", departmentList);
            
        } catch (Exception e) {
            System.out.println("DEBUG: Error getting departments: " + e.getMessage());
            return Map.of("success", false, "message", "Error getting departments: " + e.getMessage());
        }
    }
    
    public Map<String, Object> getDepartmentById(Long id, String token) {
        try {
            User currentUser = authService.getCurrentUser(token);
            if (currentUser == null) {
                return Map.of("success", false, "message", "Invalid session");
            }
            
            Optional<Department> departmentOpt = departmentRepository.findById(id);
            if (departmentOpt.isEmpty()) {
                return Map.of("success", false, "message", "Department not found");
            }
            
            Department department = departmentOpt.get();
            return Map.of("success", true, "department", convertToMap(department));
            
        } catch (Exception e) {
            System.out.println("DEBUG: Error getting department: " + e.getMessage());
            return Map.of("success", false, "message", "Error getting department: " + e.getMessage());
        }
    }
    
    public Map<String, Object> createDepartment(String token, String name, String description, 
                                               String managerName, String managerUsername, BigDecimal budget) {
        try {
            // Check if current user is SUPERADMIN
            User currentUser = authService.getCurrentUser(token);
            if (currentUser == null) {
                return Map.of("success", false, "message", "Invalid session");
            }
            
            if (currentUser.getRole() != UserRole.SUPERADMIN) {
                return Map.of("success", false, "message", "Access denied. Only SUPERADMIN can create departments.");
            }
            
            // Check if department name already exists
            if (departmentRepository.findByName(name).isPresent()) {
                return Map.of("success", false, "message", "Department with this name already exists.");
            }
            
            // Validate manager username if provided
            if (managerUsername != null && !managerUsername.trim().isEmpty()) {
                Optional<User> managerOpt = userRepository.findByUsername(managerUsername);
                if (managerOpt.isEmpty()) {
                    return Map.of("success", false, "message", "Manager username not found.");
                }
                
                User manager = managerOpt.get();
                if (manager.getRole() != UserRole.DEPARTMENT_MANAGER) {
                    return Map.of("success", false, "message", "User is not a department manager.");
                }
                
                // Check if this manager is already assigned to another department
                Optional<Department> existingDept = departmentRepository.findByManagerUsername(managerUsername);
                if (existingDept.isPresent()) {
                    return Map.of("success", false, "message", "This manager is already assigned to another department.");
                }
            }
            
            // Create new department
            Department department = new Department(name, description, managerName, managerUsername, budget);
            Department savedDepartment = departmentRepository.save(department);
            
            System.out.println("DEBUG: Department created successfully: " + savedDepartment.getName());
            return Map.of("success", true, "message", "Department created successfully", "department", convertToMap(savedDepartment));
            
        } catch (Exception e) {
            System.out.println("DEBUG: Error creating department: " + e.getMessage());
            return Map.of("success", false, "message", "Error creating department: " + e.getMessage());
        }
    }
    
    public Map<String, Object> updateDepartment(Long id, String token, String name, String description, 
                                               String managerName, String managerUsername, BigDecimal budget) {
        try {
            // Check if current user is SUPERADMIN
            User currentUser = authService.getCurrentUser(token);
            if (currentUser == null) {
                return Map.of("success", false, "message", "Invalid session");
            }
            
            if (currentUser.getRole() != UserRole.SUPERADMIN) {
                return Map.of("success", false, "message", "Access denied. Only SUPERADMIN can update departments.");
            }
            
            Optional<Department> departmentOpt = departmentRepository.findById(id);
            if (departmentOpt.isEmpty()) {
                return Map.of("success", false, "message", "Department not found");
            }
            
            Department department = departmentOpt.get();
            
            // Check if new name conflicts with existing departments
            if (name != null && !name.equals(department.getName())) {
                if (departmentRepository.existsByNameAndIdNot(name, id)) {
                    return Map.of("success", false, "message", "Department with this name already exists.");
                }
                department.setName(name);
            }
            
            // Validate manager username if provided
            if (managerUsername != null && !managerUsername.trim().isEmpty()) {
                Optional<User> managerOpt = userRepository.findByUsername(managerUsername);
                if (managerOpt.isEmpty()) {
                    return Map.of("success", false, "message", "Manager username not found.");
                }
                
                User manager = managerOpt.get();
                if (manager.getRole() != UserRole.DEPARTMENT_MANAGER) {
                    return Map.of("success", false, "message", "User is not a department manager.");
                }
                
                // Check if this manager is already assigned to another department
                Optional<Department> existingDept = departmentRepository.findByManagerUsername(managerUsername);
                if (existingDept.isPresent() && !existingDept.get().getId().equals(id)) {
                    return Map.of("success", false, "message", "This manager is already assigned to another department.");
                }
            }
            
            // Update fields
            if (description != null) department.setDescription(description);
            if (managerName != null) department.setManagerName(managerName);
            if (managerUsername != null) department.setManagerUsername(managerUsername);
            if (budget != null) department.setBudget(budget);
            
            Department updatedDepartment = departmentRepository.save(department);
            
            System.out.println("DEBUG: Department updated successfully: " + updatedDepartment.getName());
            return Map.of("success", true, "message", "Department updated successfully", "department", convertToMap(updatedDepartment));
            
        } catch (Exception e) {
            System.out.println("DEBUG: Error updating department: " + e.getMessage());
            return Map.of("success", false, "message", "Error updating department: " + e.getMessage());
        }
    }
    
    public Map<String, Object> deleteDepartment(Long id, String token) {
        try {
            // Check if current user is SUPERADMIN
            User currentUser = authService.getCurrentUser(token);
            if (currentUser == null) {
                return Map.of("success", false, "message", "Invalid session");
            }
            
            if (currentUser.getRole() != UserRole.SUPERADMIN) {
                return Map.of("success", false, "message", "Access denied. Only SUPERADMIN can delete departments.");
            }
            
            Optional<Department> departmentOpt = departmentRepository.findById(id);
            if (departmentOpt.isEmpty()) {
                return Map.of("success", false, "message", "Department not found");
            }
            
            Department department = departmentOpt.get();
            
            // Soft delete - set isActive to false
            department.setIsActive(false);
            departmentRepository.save(department);
            
            System.out.println("DEBUG: Department deleted successfully: " + department.getName());
            return Map.of("success", true, "message", "Department deleted successfully");
            
        } catch (Exception e) {
            System.out.println("DEBUG: Error deleting department: " + e.getMessage());
            return Map.of("success", false, "message", "Error deleting department: " + e.getMessage());
        }
    }
    
    public Map<String, Object> getDepartmentStats(String token) {
        try {
            User currentUser = authService.getCurrentUser(token);
            if (currentUser == null) {
                return Map.of("success", false, "message", "Invalid session");
            }
            
            if (currentUser.getRole() != UserRole.SUPERADMIN) {
                return Map.of("success", false, "message", "Access denied. Only SUPERADMIN can view department statistics.");
            }
            
            long totalDepartments = departmentRepository.countByIsActive(true);
            List<Department> departments = departmentRepository.findByIsActiveTrueOrderByNameAsc();
            
            BigDecimal totalBudget = departments.stream()
                .map(Department::getBudget)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalDepartments", totalDepartments);
            stats.put("totalBudget", totalBudget);
            stats.put("averageBudget", totalDepartments > 0 ? totalBudget.divide(BigDecimal.valueOf(totalDepartments), 2, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO);
            
            return Map.of("success", true, "stats", stats);
            
        } catch (Exception e) {
            System.out.println("DEBUG: Error getting department stats: " + e.getMessage());
            return Map.of("success", false, "message", "Error getting department statistics: " + e.getMessage());
        }
    }
    
    private Map<String, Object> convertToMap(Department department) {
        Map<String, Object> deptMap = new HashMap<>();
        deptMap.put("id", department.getId());
        deptMap.put("name", department.getName());
        deptMap.put("description", department.getDescription());
        deptMap.put("manager", department.getManagerName());
        deptMap.put("managerUsername", department.getManagerUsername());
        deptMap.put("budget", department.getBudget());
        deptMap.put("active", department.getIsActive());
        deptMap.put("createdAt", department.getCreatedAt());
        deptMap.put("updatedAt", department.getUpdatedAt());
        return deptMap;
    }
}
