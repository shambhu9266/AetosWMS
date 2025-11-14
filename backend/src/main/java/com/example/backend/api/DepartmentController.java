package com.example.backend.api;

import com.example.backend.service.DepartmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/departments")
@CrossOrigin(origins = {"http://localhost:4200", "http://20.57.79.136", "http://20.57.79.136:80", "http://20.57.79.136:8080"})
public class DepartmentController {
    
    @Autowired
    private DepartmentService departmentService;
    
    @GetMapping
    public Map<String, Object> getAllDepartments(@RequestParam String sessionId) {
        System.out.println("DEBUG: Get all departments request");
        return departmentService.getAllDepartments(sessionId);
    }
    
    @GetMapping("/{id}")
    public Map<String, Object> getDepartmentById(@PathVariable Long id, @RequestParam String sessionId) {
        System.out.println("DEBUG: Get department by ID request - ID: " + id);
        return departmentService.getDepartmentById(id, sessionId);
    }
    
    @PostMapping
    public Map<String, Object> createDepartment(@RequestParam String sessionId,
                                               @RequestParam String name,
                                               @RequestParam String description,
                                               @RequestParam(required = false) String managerName,
                                               @RequestParam(required = false) String managerUsername,
                                               @RequestParam(required = false) BigDecimal budget) {
        System.out.println("DEBUG: Create department request - name: " + name + ", manager: " + managerName);
        return departmentService.createDepartment(sessionId, name, description, managerName, managerUsername, budget);
    }
    
    @PutMapping("/{id}")
    public Map<String, Object> updateDepartment(@PathVariable Long id,
                                               @RequestParam String sessionId,
                                               @RequestParam(required = false) String name,
                                               @RequestParam(required = false) String description,
                                               @RequestParam(required = false) String managerName,
                                               @RequestParam(required = false) String managerUsername,
                                               @RequestParam(required = false) BigDecimal budget) {
        System.out.println("DEBUG: Update department request - ID: " + id + ", name: " + name);
        return departmentService.updateDepartment(id, sessionId, name, description, managerName, managerUsername, budget);
    }
    
    @DeleteMapping("/{id}")
    public Map<String, Object> deleteDepartment(@PathVariable Long id, @RequestParam String sessionId) {
        System.out.println("DEBUG: Delete department request - ID: " + id);
        return departmentService.deleteDepartment(id, sessionId);
    }
    
    @GetMapping("/stats")
    public Map<String, Object> getDepartmentStats(@RequestParam String sessionId) {
        System.out.println("DEBUG: Get department statistics request");
        return departmentService.getDepartmentStats(sessionId);
    }
}
