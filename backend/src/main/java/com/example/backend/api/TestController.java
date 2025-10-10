package com.example.backend.api;

import com.example.backend.repo.UserRepository;
import com.example.backend.model.User;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
@CrossOrigin(origins = "http://localhost:4200")
public class TestController {
    
    @Autowired
    private UserRepository userRepository;
    
    @GetMapping("/users")
    public Map<String, Object> getAllUsers() {
        try {
            List<User> users = userRepository.findAll();
            System.out.println("DEBUG: Found " + users.size() + " users in database");
            for (User user : users) {
                System.out.println("DEBUG: User - ID: " + user.getId() + 
                                 ", Username: " + user.getUsername() + 
                                 ", Password: " + user.getPassword() + 
                                 ", IsActive: " + user.getIsActive());
            }
            return Map.of("success", true, "count", users.size(), "users", users);
        } catch (Exception e) {
            System.out.println("DEBUG: Error getting users: " + e.getMessage());
            e.printStackTrace();
            return Map.of("success", false, "error", e.getMessage());
        }
    }
    
    @GetMapping("/check-superadmin")
    public Map<String, Object> checkSuperadmin() {
        try {
            var userOpt = userRepository.findByUsername("superadmin");
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                System.out.println("DEBUG: Superadmin user found - Username: " + user.getUsername() + 
                                 ", Password: " + user.getPassword() + 
                                 ", IsActive: " + user.getIsActive() + 
                                 ", Role: " + user.getRole().name());
                return Map.of(
                    "success", true, 
                    "found", true,
                    "username", user.getUsername(),
                    "password", user.getPassword(),
                    "isActive", user.getIsActive(),
                    "role", user.getRole().name()
                );
            } else {
                System.out.println("DEBUG: Superadmin user not found in database");
                return Map.of("success", true, "found", false);
            }
        } catch (Exception e) {
            System.out.println("DEBUG: Error checking superadmin: " + e.getMessage());
            e.printStackTrace();
            return Map.of("success", false, "error", e.getMessage());
        }
    }
}
