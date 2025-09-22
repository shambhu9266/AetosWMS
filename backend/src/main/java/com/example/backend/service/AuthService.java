package com.example.backend.service;

import com.example.backend.model.User;
import com.example.backend.model.UserRole;
import com.example.backend.repo.UserRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class AuthService {
    private final UserRepository userRepository;
    
    // Simple in-memory session storage (in production, use JWT or Spring Security)
    private final Map<String, User> activeSessions = new HashMap<>();
    
    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
        initializeDefaultUsers();
    }
    
    private void initializeDefaultUsers() {
        // Create default users if they don't exist
        if (userRepository.findByUsername("superadmin").isEmpty()) {
            User superadmin = new User();
            superadmin.setUsername("superadmin");
            superadmin.setPassword("admin123");
            superadmin.setFullName("Super Admin");
            superadmin.setRole(UserRole.SUPERADMIN);
            superadmin.setDepartment("IT");
            userRepository.save(superadmin);
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
    }
    
    public Map<String, Object> login(String username, String password) {
        System.out.println("DEBUG: Login attempt for username: " + username);
        Optional<User> userOpt = userRepository.findByUsername(username);
        
        if (userOpt.isPresent() && userOpt.get().getPassword().equals(password) && userOpt.get().getIsActive()) {
            User user = userOpt.get();
            String sessionId = generateSessionId();
            activeSessions.put(sessionId, user);
            
            System.out.println("DEBUG: Login successful for user: " + username + ", sessionId: " + sessionId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("sessionId", sessionId);
            response.put("user", Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "fullName", user.getFullName(),
                "role", user.getRole().name(),
                "department", user.getDepartment()
            ));
            return response;
        }
        
        System.out.println("DEBUG: Login failed for username: " + username);
        return Map.of("success", false, "message", "Invalid credentials");
    }
    
    public User getCurrentUser(String sessionId) {
        System.out.println("DEBUG: getCurrentUser called with sessionId: " + sessionId);
        System.out.println("DEBUG: Active sessions: " + activeSessions.keySet());
        User user = activeSessions.get(sessionId);
        System.out.println("DEBUG: Found user: " + (user != null ? user.getUsername() + " with role " + user.getRole().name() : "null"));
        return user;
    }
    
    public void logout(String sessionId) {
        activeSessions.remove(sessionId);
    }
    
    public boolean hasPermission(String sessionId, String requiredRole) {
        User user = getCurrentUser(sessionId);
        if (user == null) return false;
        
        // SUPERADMIN has all permissions
        if (user.getRole() == UserRole.SUPERADMIN) return true;
        
        // Check specific role permissions
        return user.getRole().name().equals(requiredRole);
    }
    
    private String generateSessionId() {
        return "session_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000);
    }
}
