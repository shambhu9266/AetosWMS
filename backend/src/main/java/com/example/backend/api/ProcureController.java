package com.example.backend.api;

import com.example.backend.model.Budget;
import com.example.backend.model.Notification;
import com.example.backend.model.Requisition;
import com.example.backend.model.RequisitionStatus;
import com.example.backend.model.User;
import com.example.backend.model.UserRole;
import com.example.backend.repo.NotificationRepository;
import com.example.backend.repo.RequisitionRepository;
import com.example.backend.service.ProcureService;
import com.example.backend.service.AuthService;
import com.example.backend.service.EmailService;
import com.example.backend.dto.CreateRequisitionRequest;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:4200")
public class ProcureController {
    private final ProcureService service;
    private final RequisitionRepository requisitionRepository;
    private final NotificationRepository notificationRepository;
    private final AuthService authService;
    private final EmailService emailService;

    public ProcureController(ProcureService service, RequisitionRepository requisitionRepository, NotificationRepository notificationRepository, AuthService authService, EmailService emailService) {
        this.service = service;
        this.requisitionRepository = requisitionRepository;
        this.notificationRepository = notificationRepository;
        this.authService = authService;
        this.emailService = emailService;
    }

    @PostMapping("/requisitions")
    public Map<String, Object> create(@RequestParam String itemName,
                                      @RequestParam Integer quantity,
                                      @RequestParam BigDecimal price,
                                      @RequestParam String department,
                                      @RequestAttribute("username") String username,
                                      @RequestAttribute("role") String role,
                                      @RequestAttribute("department") String userDepartment,
                                      @RequestAttribute("userId") Long userId) {
        System.out.println("DEBUG: Received create requisition request for user: " + username);
        // User information is already validated by JWT filter
        
        System.out.println("DEBUG: Valid JWT token for user: " + username);
        Requisition requisition = service.createRequisition(username, itemName, quantity, price, department);
        return Map.of("success", true, "requisition", requisition);
    }

    @PostMapping("/requisitions/multiple")
    public Map<String, Object> createMultiple(@RequestBody CreateRequisitionRequest request,
                                            @RequestAttribute("username") String username,
                                            @RequestAttribute("role") String role,
                                            @RequestAttribute("department") String userDepartment,
                                            @RequestAttribute("userId") Long userId) {
        System.out.println("DEBUG: ===== CONTROLLER: Received create multiple requisition request =====");
        System.out.println("DEBUG: User: " + username);
        System.out.println("DEBUG: Request department: " + request.getDepartment());
        System.out.println("DEBUG: Request items count: " + (request.getItems() != null ? request.getItems().size() : "null"));
        
        // User information is already validated by JWT filter
        System.out.println("DEBUG: Valid JWT token for user: " + username);
        try {
            System.out.println("DEBUG: ===== CALLING SERVICE METHOD =====");
            Requisition requisition = service.createRequisitionWithItems(
                username, 
                request.getDepartment(), 
                request.getItems()
            );
            System.out.println("DEBUG: ===== SERVICE METHOD COMPLETED =====");
            System.out.println("DEBUG: Created requisition with ID: " + requisition.getId());
            return Map.of("success", true, "requisition", requisition, "requisitionId", requisition.getId());
        } catch (Exception e) {
            System.err.println("Error creating requisition with multiple items: " + e.getMessage());
            e.printStackTrace();
            return Map.of("success", false, "message", "Failed to create requisition: " + e.getMessage());
        }
    }

    @GetMapping("/requisitions")
    public Map<String, Object> getAllRequisitions(@RequestParam(value = "sessionId", required = false) String sessionId) {
        System.out.println("DEBUG: Getting all requisitions with sessionId: " + sessionId);
        
        // If no sessionId provided, return all requisitions (for testing purposes)
        if (sessionId == null || sessionId.trim().isEmpty()) {
            System.out.println("DEBUG: No sessionId provided, returning all requisitions");
            List<Requisition> requisitions = requisitionRepository.findAll();
            return Map.of("success", true, "requisitions", requisitions);
        }
        
        User currentUser = authService.getCurrentUser(sessionId);
        if (currentUser == null) {
            System.out.println("DEBUG: Invalid session for sessionId: " + sessionId);
            return Map.of("success", false, "message", "Invalid session");
        }
        
        System.out.println("DEBUG: Current user: " + currentUser.getUsername() + ", Role: " + currentUser.getRole());
        List<Requisition> requisitions = requisitionRepository.findAll();
        return Map.of("success", true, "requisitions", requisitions);
    }

    @GetMapping("/requisitions/pending/it")
    public Map<String, Object> pendingIt(@RequestParam(value = "sessionId", required = false) String sessionId) {
        System.out.println("DEBUG: IT pending endpoint called with sessionId: " + sessionId);
        User currentUser = authService.getCurrentUser(sessionId);
        if (currentUser == null) {
            System.out.println("DEBUG: Invalid session for sessionId: " + sessionId);
            return Map.of("success", false, "message", "Invalid session");
        }
        
        System.out.println("DEBUG: Current user: " + currentUser.getUsername() + ", Role: " + currentUser.getRole());
        
        if (!currentUser.getRole().equals(UserRole.IT_MANAGER) && !currentUser.getRole().equals(UserRole.SUPERADMIN)) {
            System.out.println("DEBUG: User is not IT_MANAGER or SUPERADMIN: " + currentUser.getUsername());
            return Map.of("success", false, "message", "Access denied");
        }
        
        List<Requisition> requisitions = requisitionRepository.findByStatus(RequisitionStatus.PENDING_IT_APPROVAL);
        System.out.println("DEBUG: Found " + requisitions.size() + " pending IT requisitions");
        return Map.of("success", true, "requisitions", requisitions);
    }

    @GetMapping("/requisitions/pending/department")
    public Map<String, Object> pendingDepartment(@RequestParam(value = "sessionId", required = false) String sessionId) {
        System.out.println("DEBUG: Department manager endpoint called with sessionId: " + sessionId);
        User currentUser = authService.getCurrentUser(sessionId);
        if (currentUser == null) {
            System.out.println("DEBUG: Invalid session for sessionId: " + sessionId);
            return Map.of("success", false, "message", "Invalid session");
        }
        
        System.out.println("DEBUG: Current user: " + currentUser.getUsername() + ", Department: " + currentUser.getDepartment());
        
        if (!authService.isDepartmentManager(currentUser) && currentUser.getRole() != UserRole.SUPERADMIN) {
            System.out.println("DEBUG: User is not a department manager or superadmin: " + currentUser.getUsername());
            return Map.of("success", false, "message", "Access denied");
        }
        
        List<Requisition> requisitions;
        if (currentUser.getRole() == UserRole.SUPERADMIN) {
            // SUPERADMIN can see all pending department approvals from all departments
            requisitions = requisitionRepository.findByStatus(RequisitionStatus.PENDING_DEPARTMENT_APPROVAL);
            System.out.println("DEBUG: SUPERADMIN - Found " + requisitions.size() + " pending department requisitions from all departments");
        } else {
            // Department managers can only see requests from their department
            requisitions = requisitionRepository.findByStatusAndDepartment(
                RequisitionStatus.PENDING_DEPARTMENT_APPROVAL, 
                currentUser.getDepartment()
            );
            System.out.println("DEBUG: Department manager - Found " + requisitions.size() + " pending requisitions for department: " + currentUser.getDepartment());
        }
        
        for (Requisition req : requisitions) {
            System.out.println("DEBUG: Requisition ID: " + req.getId() + ", Status: " + req.getStatus() + ", Department: " + req.getDepartment());
        }
        return Map.of("success", true, "requisitions", requisitions);
    }

    @GetMapping("/requisitions/pending/finance")
    public Map<String, Object> pendingFinance(@RequestParam String sessionId) {
        if (!authService.hasPermission(sessionId, "FINANCE_MANAGER")) {
            return Map.of("success", false, "message", "Access denied");
        }
        List<Requisition> requisitions = requisitionRepository.findByStatus(RequisitionStatus.PENDING_FINANCE_APPROVAL);
        return Map.of("success", true, "requisitions", requisitions);
    }

    @PostMapping("/requisitions/{id}/department-decision")
    public Map<String, Object> departmentDecision(@PathVariable Long id,
                                                  @RequestParam String sessionId,
                                                  @RequestParam String decision,
                                                  @RequestParam(required = false) String comments) {
        User currentUser = authService.getCurrentUser(sessionId);
        if (currentUser == null) {
            return Map.of("success", false, "message", "Invalid session");
        }
        
        if (!authService.isDepartmentManager(currentUser)) {
            return Map.of("success", false, "message", "Access denied");
        }
        
        Requisition requisition = service.departmentDecision(id, currentUser.getUsername(), decision, comments);
        return Map.of("success", true, "requisition", requisition);
    }

    @PostMapping("/requisitions/{id}/it-decision")
    public Map<String, Object> itDecision(@PathVariable Long id,
                                          @RequestParam String sessionId,
                                          @RequestParam String decision,
                                          @RequestParam(required = false) String comments) {
        if (!authService.hasPermission(sessionId, "IT_MANAGER")) {
            return Map.of("success", false, "message", "Access denied");
        }
        
        User currentUser = authService.getCurrentUser(sessionId);
        Requisition requisition = service.itDecision(id, currentUser.getUsername(), decision, comments);
        return Map.of("success", true, "requisition", requisition);
    }

    @PostMapping("/requisitions/{id}/finance-decision")
    public Map<String, Object> financeDecision(@PathVariable Long id,
                                               @RequestParam String sessionId,
                                               @RequestParam String decision,
                                               @RequestParam(required = false) String comments) {
        try {
            if (!authService.hasPermission(sessionId, "FINANCE_MANAGER")) {
                return Map.of("success", false, "message", "Access denied");
            }
            
            User currentUser = authService.getCurrentUser(sessionId);
            if (currentUser == null) {
                return Map.of("success", false, "message", "Invalid session");
            }
            
            Requisition requisition = service.financeDecision(id, currentUser.getUsername(), decision, comments, currentUser.getDepartment());
            return Map.of("success", true, "requisition", requisition);
        } catch (IllegalArgumentException e) {
            System.err.println("Finance decision error: " + e.getMessage());
            return Map.of("success", false, "message", e.getMessage());
        } catch (IllegalStateException e) {
            System.err.println("Finance decision error: " + e.getMessage());
            return Map.of("success", false, "message", e.getMessage());
        } catch (Exception e) {
            System.err.println("Finance decision error: " + e.getMessage());
            e.printStackTrace();
            return Map.of("success", false, "message", "Failed to process Finance decision: " + e.getMessage());
        }
    }

    @GetMapping("/notifications")
    public Map<String, Object> unread(@RequestParam String sessionId) {
        User currentUser = authService.getCurrentUser(sessionId);
        if (currentUser == null) {
            return Map.of("success", false, "message", "Invalid session");
        }
        
        List<Notification> notifications = notificationRepository.findByUserIdAndIsReadFalse(currentUser.getUsername());
        System.out.println("DEBUG: Fetching notifications for user: " + currentUser.getUsername() + 
                          ", Found " + notifications.size() + " unread notifications");
        
        // Also get all notifications for debugging
        List<Notification> allNotifications = notificationRepository.findByUserIdOrderByTimestampDesc(currentUser.getUsername());
        System.out.println("DEBUG: Total notifications for user: " + allNotifications.size());
        
        return Map.of("success", true, "notifications", notifications);
    }

    @DeleteMapping("/notifications/{notificationId}")
    public Map<String, Object> deleteNotification(@PathVariable Long notificationId, 
                                                  @RequestParam String sessionId) {
        try {
            User currentUser = authService.getCurrentUser(sessionId);
            if (currentUser == null) {
                return Map.of("success", false, "message", "Invalid session");
            }
            
            // Check if notification exists and belongs to the current user
            Notification notification = notificationRepository.findById(notificationId).orElse(null);
            if (notification == null) {
                return Map.of("success", false, "message", "Notification not found");
            }
            
            if (!notification.getUserId().equals(currentUser.getUsername())) {
                return Map.of("success", false, "message", "Access denied. You can only delete your own notifications.");
            }
            
            // Delete the notification
            notificationRepository.delete(notification);
            
            return Map.of("success", true, "message", "Notification deleted successfully");
            
        } catch (Exception e) {
            System.err.println("Error deleting notification: " + e.getMessage());
            e.printStackTrace();
            return Map.of("success", false, "message", "Failed to delete notification: " + e.getMessage());
        }
    }

    @GetMapping("/budgets")
    public Map<String, Object> budgets(@RequestParam String sessionId) {
        User currentUser = authService.getCurrentUser(sessionId);
        if (currentUser == null) {
            return Map.of("success", false, "message", "Invalid session");
        }
        
        List<Budget> budgets = service.getAllBudgets();
        return Map.of("success", true, "budgets", budgets);
    }

    @GetMapping("/requisitions/approved-this-month")
    public Map<String, Object> approvedThisMonth(@RequestParam String sessionId) {
        User currentUser = authService.getCurrentUser(sessionId);
        if (currentUser == null) {
            return Map.of("success", false, "message", "Invalid session");
        }
        
        List<Requisition> approvedRequisitions = service.getApprovedThisMonth();
        return Map.of("success", true, "requisitions", approvedRequisitions);
    }

    @GetMapping("/requisitions/active-orders")
    public Map<String, Object> getActiveOrders(@RequestParam String sessionId) {
        User currentUser = authService.getCurrentUser(sessionId);
        if (currentUser == null) {
            return Map.of("success", false, "message", "Invalid session");
        }
        
        // Get requisitions that are approved but not yet completed (active orders)
        List<Requisition> activeOrders = requisitionRepository.findByStatus(RequisitionStatus.APPROVED);
        return Map.of("success", true, "requisitions", activeOrders);
    }

    @GetMapping("/requisitions/recent")
    public Map<String, Object> getRecentRequisitions(@RequestParam String sessionId) {
        User currentUser = authService.getCurrentUser(sessionId);
        if (currentUser == null) {
            return Map.of("success", false, "message", "Invalid session");
        }
        
        // Get recent requisitions (last 10, ordered by creation date)
        List<Requisition> recentRequisitions = requisitionRepository.findTop10ByOrderByCreatedAtDesc();
        return Map.of("success", true, "requisitions", recentRequisitions);
    }
    
    @PutMapping("/requisitions/{id}")
    public Map<String, Object> updateRequisition(@PathVariable Long id,
                                                @RequestParam String sessionId,
                                                @RequestBody CreateRequisitionRequest request) {
        try {
            System.out.println("DEBUG: Update requisition request - ID: " + id + ", SessionId: " + sessionId);
            
            User currentUser = authService.getCurrentUser(sessionId);
            if (currentUser == null) {
                System.out.println("DEBUG: Invalid session for sessionId: " + sessionId);
                return Map.of("success", false, "message", "Invalid session");
            }
            
            System.out.println("DEBUG: Valid session for user: " + currentUser.getUsername());
            
            // Find the requisition
            Requisition requisition = requisitionRepository.findById(id).orElse(null);
            if (requisition == null) {
                System.out.println("DEBUG: Requisition not found with ID: " + id);
                return Map.of("success", false, "message", "Requisition not found");
            }
            
            System.out.println("DEBUG: Found requisition: " + requisition.getId() + " by " + requisition.getCreatedBy());
            
            // Update the requisition
            Requisition updatedRequisition = service.updateRequisitionWithItems(
                id,
                request.getDepartment(),
                request.getItems()
            );
            
            System.out.println("DEBUG: Requisition updated successfully");
            return Map.of("success", true, "requisition", updatedRequisition, "message", "Requisition updated successfully");
            
        } catch (Exception e) {
            System.err.println("Error updating requisition: " + e.getMessage());
            e.printStackTrace();
            return Map.of("success", false, "message", "Failed to update requisition: " + e.getMessage());
        }
    }

    @DeleteMapping("/requisitions/{id}")
    public Map<String, Object> deleteRequisition(@PathVariable Long id,
                                                @RequestParam String sessionId) {
        try {
            System.out.println("DEBUG: Delete requisition request - ID: " + id + ", SessionId: " + sessionId);
            
            User currentUser = authService.getCurrentUser(sessionId);
            if (currentUser == null) {
                System.out.println("DEBUG: Invalid session for sessionId: " + sessionId);
                return Map.of("success", false, "message", "Invalid session");
            }
            
            System.out.println("DEBUG: Valid session for user: " + currentUser.getUsername());
            
            // Find the requisition
            Requisition requisition = requisitionRepository.findById(id).orElse(null);
            if (requisition == null) {
                System.out.println("DEBUG: Requisition not found with ID: " + id);
                return Map.of("success", false, "message", "Requisition not found");
            }
            
            System.out.println("DEBUG: Found requisition: " + requisition.getId() + " by " + requisition.getCreatedBy());
            
            // Allow any logged-in user to delete any requisition
            // Allow deletion of requisitions in any status
            
            // Delete the requisition (items will be deleted automatically due to cascade)
            requisitionRepository.delete(requisition);
            
            System.out.println("DEBUG: Requisition deleted successfully");
            return Map.of("success", true, "message", "Requisition deleted successfully");
            
        } catch (Exception e) {
            System.err.println("Error deleting requisition: " + e.getMessage());
            e.printStackTrace();
            return Map.of("success", false, "message", "Failed to delete requisition: " + e.getMessage());
        }
    }

    @PostMapping("/requisitions/{id}/send-email")
    public Map<String, Object> sendRequisitionEmail(@PathVariable Long id,
                                                   @RequestParam String sessionId,
                                                   @RequestParam String vendorEmail,
                                                   @RequestParam String vendorName) {
        try {
            // Check if user has IT_MANAGER permission
            User currentUser = authService.getCurrentUser(sessionId);
            if (currentUser == null || !"IT_MANAGER".equals(currentUser.getRole().name())) {
                return Map.of("success", false, "message", "Access denied. Only IT Managers can send emails.");
            }
            
            // Find the requisition
            Requisition requisition = requisitionRepository.findById(id).orElse(null);
            if (requisition == null) {
                return Map.of("success", false, "message", "Requisition not found");
            }
            
            // Send email
            boolean emailSent = emailService.sendRequisitionEmail(
                vendorEmail, 
                vendorName, 
                requisition, 
                sessionId
            );
            
            if (emailSent) {
                return Map.of("success", true, "message", "Requisition inquiry sent to vendor successfully");
            } else {
                return Map.of("success", false, "message", "Failed to send email. Please check email configuration.");
            }
            
        } catch (Exception e) {
            System.err.println("Error sending requisition email: " + e.getMessage());
            e.printStackTrace();
            return Map.of("success", false, "message", "Failed to send email: " + e.getMessage());
        }
    }
}


