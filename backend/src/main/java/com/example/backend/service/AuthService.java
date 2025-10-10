package com.example.backend.service;

import com.example.backend.model.User;
import com.example.backend.model.UserRole;
import com.example.backend.repo.UserRepository;
import com.example.backend.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class AuthService {
    private final UserRepository userRepository;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
        initializeDefaultUsers();
    }
    
    private void initializeDefaultUsers() {
        // Create default users if they don't exist
        System.out.println("DEBUG: Initializing default users...");
        if (userRepository.findByUsername("superadmin").isEmpty()) {
            System.out.println("DEBUG: Creating superadmin user...");
            User superadmin = new User();
            superadmin.setUsername("superadmin");
            superadmin.setPassword("admin123");
            superadmin.setFullName("Super Admin");
            superadmin.setRole(UserRole.SUPERADMIN);
            superadmin.setDepartment("IT");
            superadmin.setIsActive(true);
            User savedUser = userRepository.save(superadmin);
            System.out.println("DEBUG: Superadmin user created with ID: " + savedUser.getId());
        } else {
            System.out.println("DEBUG: Superadmin user already exists");
            // Update existing superadmin user to ensure correct password and status
            User existingUser = userRepository.findByUsername("superadmin").get();
            System.out.println("DEBUG: Existing superadmin - Password: " + existingUser.getPassword() + 
                             ", IsActive: " + existingUser.getIsActive());
            
            // Reset password and ensure user is active
            existingUser.setPassword("admin123");
            existingUser.setIsActive(true);
            userRepository.save(existingUser);
            System.out.println("DEBUG: Superadmin user updated with correct password and status");
        }
        
        if (userRepository.findByUsername("shambhu").isEmpty()) {
            User shambhu = new User();
            shambhu.setUsername("shambhu");
            shambhu.setPassword("shambhu123");
            shambhu.setFullName("Shambhu Sir");
            shambhu.setRole(UserRole.IT_MANAGER);
            shambhu.setDepartment("IT");
            userRepository.save(shambhu);
        }
        
        if (userRepository.findByUsername("joshi").isEmpty()) {
            User joshi = new User();
            joshi.setUsername("joshi");
            joshi.setPassword("joshi123");
            joshi.setFullName("Joshi Sir");
            joshi.setRole(UserRole.FINANCE_MANAGER);
            joshi.setDepartment("Finance");
            userRepository.save(joshi);
        }
        
        if (userRepository.findByUsername("employee1").isEmpty()) {
            User employee1 = new User();
            employee1.setUsername("employee1");
            employee1.setPassword("emp123");
            employee1.setFullName("John Doe");
            employee1.setRole(UserRole.EMPLOYEE);
            employee1.setDepartment("IT");
            userRepository.save(employee1);
        }
        
        if (userRepository.findByUsername("employee2").isEmpty()) {
            User employee2 = new User();
            employee2.setUsername("employee2");
            employee2.setPassword("emp123");
            employee2.setFullName("Jane Smith");
            employee2.setRole(UserRole.EMPLOYEE);
            employee2.setDepartment("Sales");
            userRepository.save(employee2);
        }
        
        // Create Department Managers
        if (userRepository.findByUsername("salesmanager").isEmpty()) {
            User salesManager = new User();
            salesManager.setUsername("salesmanager");
            salesManager.setPassword("sales123");
            salesManager.setFullName("Sales Manager");
            salesManager.setRole(UserRole.DEPARTMENT_MANAGER);
            salesManager.setDepartment("Sales");
            userRepository.save(salesManager);
        }
        
        if (userRepository.findByUsername("itmanager").isEmpty()) {
            User itManager = new User();
            itManager.setUsername("itmanager");
            itManager.setPassword("it123");
            itManager.setFullName("IT Department Manager");
            itManager.setRole(UserRole.DEPARTMENT_MANAGER);
            itManager.setDepartment("IT");
            userRepository.save(itManager);
        }
    }
    
    public Map<String, Object> login(String username, String password) {
        System.out.println("DEBUG: Login attempt for username: " + username);
        Optional<User> userOpt = userRepository.findByUsername(username);
        
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            System.out.println("DEBUG: User found - Username: " + user.getUsername() + 
                             ", Password matches: " + user.getPassword().equals(password) + 
                             ", IsActive: " + user.getIsActive());
            
            if (user.getPassword().equals(password) && user.getIsActive()) {
                String token = jwtUtil.generateToken(username, user.getRole().name(), user.getDepartment(), user.getId());
                
                System.out.println("DEBUG: Login successful for user: " + username + ", JWT token generated");
                
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("token", token);
                response.put("user", Map.of(
                    "id", user.getId(),
                    "username", user.getUsername(),
                    "fullName", user.getFullName(),
                    "role", user.getRole().name(),
                    "department", user.getDepartment()
                ));
                return response;
            } else {
                System.out.println("DEBUG: Login failed - Password mismatch or user inactive");
            }
        } else {
            System.out.println("DEBUG: User not found in database");
        }
        
        System.out.println("DEBUG: Login failed for username: " + username);
        return Map.of("success", false, "message", "Invalid credentials");
    }
    
    public User getCurrentUser(String token) {
        try {
            String username = jwtUtil.extractUsername(token);
            Optional<User> userOpt = userRepository.findByUsername(username);
            if (userOpt.isPresent() && userOpt.get().getIsActive()) {
                return userOpt.get();
            }
        } catch (Exception e) {
            System.out.println("DEBUG: Error extracting user from token: " + e.getMessage());
        }
        return null;
    }
    
    public void logout(String token) {
        // JWT tokens are stateless, so no server-side logout needed
        // Token will expire naturally
        System.out.println("DEBUG: User logged out (JWT token will expire naturally)");
    }
    
    public boolean hasPermission(String token, String requiredRole) {
        try {
            String role = jwtUtil.extractRole(token);
            if (role == null) return false;
            
            // SUPERADMIN has all permissions
            if (role.equals("SUPERADMIN")) return true;
            
            // Check specific role permissions
            return role.equals(requiredRole);
        } catch (Exception e) {
            System.out.println("DEBUG: Error checking permissions: " + e.getMessage());
            return false;
        }
    }
    
    public boolean isEmployee(User user) {
        return user != null && user.getRole() == UserRole.EMPLOYEE;
    }
    
    public boolean hasRole(User user, String roleName) {
        return user != null && user.getRole().name().equals(roleName);
    }
    
    public boolean isDepartmentManager(User user) {
        return user != null && user.getRole() == UserRole.DEPARTMENT_MANAGER;
    }
    
    public boolean canApproveDepartmentRequests(User user, String requestDepartment) {
        if (user == null) return false;
        
        // SUPERADMIN can approve any department
        if (user.getRole() == UserRole.SUPERADMIN) return true;
        
        // Department Manager can only approve their own department
        if (user.getRole() == UserRole.DEPARTMENT_MANAGER) {
            return user.getDepartment().equals(requestDepartment);
        }
        
        return false;
    }
    
    
    public Map<String, Object> createUser(String token, String username, String password, String fullName, String role, String department) {
        try {
            // Check if current user is SUPERADMIN
            User currentUser = getCurrentUser(token);
            if (currentUser == null || currentUser.getRole() != UserRole.SUPERADMIN) {
                return Map.of("success", false, "message", "Access denied. Only SUPERADMIN can create users.");
            }
            
            // Check if username already exists
            if (userRepository.findByUsername(username).isPresent()) {
                return Map.of("success", false, "message", "Username already exists.");
            }
            
            // Validate role
            UserRole userRole;
            try {
                userRole = UserRole.valueOf(role.toUpperCase());
            } catch (IllegalArgumentException e) {
                return Map.of("success", false, "message", "Invalid role. Valid roles: SUPERADMIN, IT_MANAGER, FINANCE_MANAGER, DEPARTMENT_MANAGER, EMPLOYEE");
            }
            
            // Create new user
            User newUser = new User();
            newUser.setUsername(username);
            newUser.setPassword(password);
            newUser.setFullName(fullName);
            newUser.setRole(userRole);
            newUser.setDepartment(department);
            newUser.setIsActive(true);
            
            User savedUser = userRepository.save(newUser);
            
            System.out.println("DEBUG: User created successfully: " + username + " with role: " + role);
            
            return Map.of(
                "success", true, 
                "message", "User created successfully",
                "user", Map.of(
                    "id", savedUser.getId(),
                    "username", savedUser.getUsername(),
                    "fullName", savedUser.getFullName(),
                    "role", savedUser.getRole().name(),
                    "department", savedUser.getDepartment(),
                    "isActive", savedUser.getIsActive()
                )
            );
            
        } catch (Exception e) {
            System.out.println("DEBUG: Error creating user: " + e.getMessage());
            e.printStackTrace();
            return Map.of("success", false, "message", "Error creating user: " + e.getMessage());
        }
    }
    
    public Map<String, Object> getAllUsers(String token) {
        try {
            // Check if current user is SUPERADMIN
            User currentUser = getCurrentUser(token);
            if (currentUser == null || currentUser.getRole() != UserRole.SUPERADMIN) {
                return Map.of("success", false, "message", "Access denied. Only SUPERADMIN can view all users.");
            }
            
            List<User> users = userRepository.findAll();
            List<Map<String, Object>> userList = new java.util.ArrayList<>();
            for (User user : users) {
                Map<String, Object> userMap = new HashMap<>();
                userMap.put("id", user.getId());
                userMap.put("username", user.getUsername());
                userMap.put("fullName", user.getFullName());
                userMap.put("role", user.getRole().name());
                userMap.put("department", user.getDepartment());
                userMap.put("isActive", user.getIsActive());
                userList.add(userMap);
            }
            
            return Map.of("success", true, "users", userList);
            
        } catch (Exception e) {
            System.out.println("DEBUG: Error getting users: " + e.getMessage());
            return Map.of("success", false, "message", "Error getting users: " + e.getMessage());
        }
    }
    
    public Map<String, Object> getAllUsersWithoutAuth() {
        try {
            System.out.println("DEBUG: Getting all users without authentication");
            List<User> users = userRepository.findAll();
            List<Map<String, Object>> userList = new java.util.ArrayList<>();
            for (User user : users) {
                Map<String, Object> userMap = new HashMap<>();
                userMap.put("id", user.getId());
                userMap.put("username", user.getUsername());
                userMap.put("fullName", user.getFullName());
                userMap.put("role", user.getRole().name());
                userMap.put("department", user.getDepartment());
                userMap.put("isActive", user.getIsActive());
                userList.add(userMap);
            }
            
            System.out.println("DEBUG: Found " + users.size() + " users");
            return Map.of("success", true, "users", userList);
            
        } catch (Exception e) {
            System.out.println("DEBUG: Error getting users without auth: " + e.getMessage());
            e.printStackTrace();
            return Map.of("success", false, "message", "Error getting users: " + e.getMessage());
        }
    }
    
    public Map<String, Object> updateUserStatus(String token, Long userId, Boolean isActive) {
        try {
            // Check if current user is SUPERADMIN
            User currentUser = getCurrentUser(token);
            if (currentUser == null || currentUser.getRole() != UserRole.SUPERADMIN) {
                return Map.of("success", false, "message", "Access denied. Only SUPERADMIN can update users.");
            }
            
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                return Map.of("success", false, "message", "User not found.");
            }
            
            user.setIsActive(isActive);
            userRepository.save(user);
            
            return Map.of("success", true, "message", "User status updated successfully");
            
        } catch (Exception e) {
            System.out.println("DEBUG: Error updating user status: " + e.getMessage());
            return Map.of("success", false, "message", "Error updating user status: " + e.getMessage());
        }
    }
}
