package com.example.backend.api;

import com.example.backend.service.AuthService;
import com.example.backend.model.User;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {"http://localhost:4200", "http://20.57.79.136", "http://20.57.79.136:80", "http://20.57.79.136:8080"})
public class AuthController {
    private final AuthService authService;
    
    public AuthController(AuthService authService) {
        this.authService = authService;
    }
    
    @PostMapping("/login")
    public Map<String, Object> login(@RequestParam String username, @RequestParam String password) {
        try {
            System.out.println("DEBUG: AuthController received login request for username: " + username);
            Map<String, Object> result = authService.login(username, password);
            System.out.println("DEBUG: AuthController returning login result: " + result);
            return result;
        } catch (Exception e) {
            System.err.println("ERROR: Login failed with exception: " + e.getMessage());
            e.printStackTrace();
            return Map.of("success", false, "message", "Internal server error: " + e.getMessage());
        }
    }
    
    @PostMapping("/logout")
    public Map<String, Object> logout(@RequestParam String sessionId) {
        authService.logout(sessionId);
        return Map.of("success", true, "message", "Logged out successfully");
    }
    
    @GetMapping("/me")
    public Map<String, Object> getCurrentUser(@RequestParam String sessionId) {
        User user = authService.getCurrentUser(sessionId);
        if (user != null) {
            return Map.of(
                "success", true,
                "user", Map.of(
                    "id", user.getId(),
                    "username", user.getUsername(),
                    "fullName", user.getFullName(),
                    "role", user.getRole().name(),
                    "department", user.getDepartment()
                )
            );
        }
        return Map.of("success", false, "message", "Invalid token");
    }
    
    @GetMapping("/check-permission")
    public Map<String, Object> checkPermission(@RequestParam String sessionId, @RequestParam String requiredRole) {
        boolean hasPermission = authService.hasPermission(sessionId, requiredRole);
        return Map.of("hasPermission", hasPermission);
    }
    
    @PostMapping("/create-user")
    public Map<String, Object> createUser(@RequestParam String sessionId,
                                        @RequestParam String username,
                                        @RequestParam String password,
                                        @RequestParam String fullName,
                                        @RequestParam String role,
                                        @RequestParam String department) {
        System.out.println("DEBUG: Create user request - username: " + username + ", role: " + role);
        return authService.createUser(sessionId, username, password, fullName, role, department);
    }
    
    @GetMapping("/users")
    public Map<String, Object> getAllUsers(@RequestParam(value = "sessionId", required = false) String sessionId) {
        System.out.println("DEBUG: Get all users request with sessionId: " + sessionId);
        
        // If no sessionId provided, return all users (for testing purposes)
        if (sessionId == null || sessionId.trim().isEmpty()) {
            System.out.println("DEBUG: No sessionId provided, returning all users");
            return authService.getAllUsersWithoutAuth();
        }
        
        return authService.getAllUsers(sessionId);
    }
    
    @PostMapping("/update-user-status")
    public Map<String, Object> updateUserStatus(@RequestParam String sessionId,
                                                @RequestParam Long userId,
                                                @RequestParam Boolean isActive) {
        System.out.println("DEBUG: Update user status request - userId: " + userId + ", isActive: " + isActive);
        return authService.updateUserStatus(sessionId, userId, isActive);
    }
}
