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
import java.util.Arrays;
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
    public Map<String, Object> getAllRequisitions(@RequestAttribute("username") String username,
                                                  @RequestAttribute("role") String role,
                                                  @RequestAttribute("department") String department,
                                                  @RequestAttribute("userId") Long userId) {
        System.out.println("DEBUG: Getting all requisitions for user: " + username);
        
        System.out.println("DEBUG: Current user: " + username + ", Role: " + role);
        
        List<Requisition> requisitions;
        
        if (role.equals("SUPERADMIN")) {
            // SUPERADMIN can see all requisitions
            requisitions = requisitionRepository.findAll();
        } else if (role.equals("DEPARTMENT_MANAGER")) {
            // Department managers can see requisitions from their department only
            requisitions = requisitionRepository.findByDepartmentOrderByCreatedAtDesc(department, org.springframework.data.domain.Pageable.unpaged()).getContent();
        } else if (role.equals("IT_MANAGER")) {
            // IT managers can only see requisitions that have been approved by department manager
            requisitions = requisitionRepository.findTop10ByStatusInOrderByCreatedAtDesc(
                Arrays.asList(RequisitionStatus.PENDING_IT_APPROVAL, RequisitionStatus.PENDING_FINANCE_APPROVAL, 
                             RequisitionStatus.APPROVED, RequisitionStatus.REJECTED)
            );
        } else if (role.equals("FINANCE_MANAGER")) {
            // Finance managers can only see requisitions that have been approved by both department and IT managers
            requisitions = requisitionRepository.findTop10ByStatusInOrderByCreatedAtDesc(
                Arrays.asList(RequisitionStatus.PENDING_FINANCE_APPROVAL, RequisitionStatus.APPROVED, RequisitionStatus.REJECTED)
            );
        } else {
            // For employees, show only their own requisitions
            requisitions = requisitionRepository.findByCreatedByOrderByCreatedAtDesc(username);
        }
        
        System.out.println("DEBUG: Found " + requisitions.size() + " requisitions for role: " + role);
        return Map.of("success", true, "requisitions", requisitions);
    }

    @GetMapping("/requisitions/pending/it")
    public Map<String, Object> pendingIt(@RequestAttribute("username") String username,
                                         @RequestAttribute("role") String role,
                                         @RequestAttribute("department") String department,
                                         @RequestAttribute("userId") Long userId) {
        System.out.println("DEBUG: IT pending endpoint called for user: " + username);
        
        System.out.println("DEBUG: Current user: " + username + ", Role: " + role);
        
        if (!role.equals("IT_MANAGER") && !role.equals("SUPERADMIN")) {
            System.out.println("DEBUG: User is not IT_MANAGER or SUPERADMIN: " + username);
            return Map.of("success", false, "message", "Access denied");
        }
        
        List<Requisition> requisitions = requisitionRepository.findByStatus(RequisitionStatus.PENDING_IT_APPROVAL);
        System.out.println("DEBUG: Found " + requisitions.size() + " pending IT requisitions");
        return Map.of("success", true, "requisitions", requisitions);
    }

    @GetMapping("/requisitions/pending/department")
    public Map<String, Object> pendingDepartment(@RequestAttribute("username") String username,
                                                 @RequestAttribute("role") String role,
                                                 @RequestAttribute("department") String userDepartment,
                                                 @RequestAttribute("userId") Long userId) {
        System.out.println("DEBUG: Department manager endpoint called for user: " + username);
        
        System.out.println("DEBUG: Current user: " + username + ", Department: " + userDepartment);
        
        // Check if user is department manager or superadmin
        boolean isDepartmentManager = role.equals("DEPARTMENT_MANAGER") || role.equals("SUPERADMIN");
        if (!isDepartmentManager) {
            System.out.println("DEBUG: User is not a department manager or superadmin: " + username);
            return Map.of("success", false, "message", "Access denied");
        }
        
        List<Requisition> requisitions;
        if (role.equals("SUPERADMIN")) {
            // SUPERADMIN can see all pending department approvals from all departments
            requisitions = requisitionRepository.findByStatus(RequisitionStatus.PENDING_DEPARTMENT_APPROVAL);
            System.out.println("DEBUG: SUPERADMIN - Found " + requisitions.size() + " pending department requisitions from all departments");
        } else {
            // Department managers can only see requests from their department
            requisitions = requisitionRepository.findByStatusAndDepartment(
                RequisitionStatus.PENDING_DEPARTMENT_APPROVAL, 
                userDepartment
            );
            System.out.println("DEBUG: Department manager - Found " + requisitions.size() + " pending requisitions for department: " + userDepartment);
        }
        
        for (Requisition req : requisitions) {
            System.out.println("DEBUG: Requisition ID: " + req.getId() + ", Status: " + req.getStatus() + ", Department: " + req.getDepartment());
        }
        return Map.of("success", true, "requisitions", requisitions);
    }

    @GetMapping("/requisitions/pending/finance")
    public Map<String, Object> pendingFinance(@RequestAttribute("username") String username,
                                              @RequestAttribute("role") String role,
                                              @RequestAttribute("department") String department,
                                              @RequestAttribute("userId") Long userId) {
        if (!role.equals("FINANCE_MANAGER") && !role.equals("SUPERADMIN")) {
            return Map.of("success", false, "message", "Access denied");
        }
        List<Requisition> requisitions = requisitionRepository.findByStatus(RequisitionStatus.PENDING_FINANCE_APPROVAL);
        return Map.of("success", true, "requisitions", requisitions);
    }

    @PostMapping("/requisitions/{id}/department-decision")
    public Map<String, Object> departmentDecision(@PathVariable Long id,
                                                  @RequestAttribute("username") String username,
                                                  @RequestAttribute("role") String role,
                                                  @RequestAttribute("department") String department,
                                                  @RequestAttribute("userId") Long userId,
                                                  @RequestParam(required = false) String decision,
                                                  @RequestParam(required = false) String comments,
                                                  @RequestBody(required = false) Map<String, Object> body) {
        // Check if user is department manager or superadmin
        boolean isDepartmentManager = role.equals("DEPARTMENT_MANAGER") || role.equals("SUPERADMIN");
        if (!isDepartmentManager) {
            return Map.of("success", false, "message", "Access denied");
        }
        
        // Debug logging
        System.out.println("DEBUG: Department decision request - ID: " + id + ", User: " + username);
        System.out.println("DEBUG: Query params - decision: " + decision + ", comments: " + comments);
        System.out.println("DEBUG: Request body: " + body);
        if (body != null) {
            System.out.println("DEBUG: Body keys: " + body.keySet());
            for (String key : body.keySet()) {
                System.out.println("DEBUG: Body[" + key + "] = " + body.get(key) + " (type: " + body.get(key).getClass().getSimpleName() + ")");
            }
        }
        
        // Check if this is a GET request (which shouldn't happen for POST endpoints)
        System.out.println("DEBUG: Request method should be POST for department decision");
        
        String decisionValue = decision;
        String commentsValue = comments;
        if ((decisionValue == null || decisionValue.isBlank()) && body != null) {
            Object d = body.get("decision");
            if (d == null) d = body.get("status");
            if (d == null) d = body.get("action");
            if (d == null) d = body.get("decisionStatus");
            if (d == null) {
                // Handle boolean style e.g., {"approved": true}
                Object approved = body.get("approved");
                if (approved == null) approved = body.get("approve");
                if (approved != null) {
                    boolean isApproved = Boolean.parseBoolean(String.valueOf(approved));
                    d = isApproved ? "APPROVE" : "REJECT";
                }
            }
            if (d != null) {
                decisionValue = String.valueOf(d);
            }
            Object c = body.get("comments");
            if (c == null) c = body.get("comment");
            if (c == null) c = body.get("remarks");
            if (c == null) c = body.get("remark");
            if (c == null) c = body.get("note");
            if (c != null) {
                commentsValue = String.valueOf(c);
            }
        }
        // Normalize common values
        if (decisionValue != null) {
            String v = decisionValue.trim().toUpperCase();
            if (v.equals("APPROVED") || v.equals("ACCEPT") || v.equals("ACCEPTED") || v.equals("YES") || v.equals("TRUE") || v.equals("1")) {
                decisionValue = "APPROVE";
            } else if (v.equals("REJECTED") || v.equals("DECLINE") || v.equals("DECLINED") || v.equals("NO") || v.equals("FALSE") || v.equals("0")) {
                decisionValue = "REJECT";
            }
        }
        if (decisionValue == null || decisionValue.isBlank()) {
            return Map.of("success", false, "message", "Missing required field: decision");
        }
        
        Requisition requisition = service.departmentDecision(id, username, decisionValue, commentsValue);
        return Map.of("success", true, "requisition", requisition);
    }

    @PostMapping("/requisitions/{id}/it-decision")
    public Map<String, Object> itDecision(@PathVariable Long id,
                                          @RequestAttribute("username") String username,
                                          @RequestAttribute("role") String role,
                                          @RequestAttribute("department") String department,
                                          @RequestAttribute("userId") Long userId,
                                          @RequestParam(required = false) String decision,
                                          @RequestParam(required = false) String comments,
                                          @RequestBody(required = false) Map<String, Object> body) {
        if (!role.equals("IT_MANAGER") && !role.equals("SUPERADMIN")) {
            return Map.of("success", false, "message", "Access denied");
        }
        
        String decisionValue = decision;
        String commentsValue = comments;
        if ((decisionValue == null || decisionValue.isBlank()) && body != null) {
            Object d = body.get("decision");
            if (d == null) d = body.get("status");
            if (d == null) d = body.get("action");
            if (d == null) d = body.get("decisionStatus");
            if (d == null) {
                Object approved = body.get("approved");
                if (approved == null) approved = body.get("approve");
                if (approved != null) {
                    boolean isApproved = Boolean.parseBoolean(String.valueOf(approved));
                    d = isApproved ? "APPROVE" : "REJECT";
                }
            }
            if (d != null) {
                decisionValue = String.valueOf(d);
            }
            Object c = body.get("comments");
            if (c == null) c = body.get("comment");
            if (c == null) c = body.get("remarks");
            if (c == null) c = body.get("remark");
            if (c == null) c = body.get("note");
            if (c != null) {
                commentsValue = String.valueOf(c);
            }
        }
        if (decisionValue != null) {
            String v = decisionValue.trim().toUpperCase();
            if (v.equals("APPROVED") || v.equals("ACCEPT") || v.equals("ACCEPTED") || v.equals("YES") || v.equals("TRUE") || v.equals("1")) {
                decisionValue = "APPROVE";
            } else if (v.equals("REJECTED") || v.equals("DECLINE") || v.equals("DECLINED") || v.equals("NO") || v.equals("FALSE") || v.equals("0")) {
                decisionValue = "REJECT";
            }
        }
        if (decisionValue == null || decisionValue.isBlank()) {
            return Map.of("success", false, "message", "Missing required field: decision");
        }
        
        Requisition requisition = service.itDecision(id, username, decisionValue, commentsValue);
        return Map.of("success", true, "requisition", requisition);
    }

    @PostMapping("/requisitions/{id}/finance-decision")
    public Map<String, Object> financeDecision(@PathVariable Long id,
                                               @RequestAttribute("username") String username,
                                               @RequestAttribute("role") String role,
                                               @RequestAttribute("department") String department,
                                               @RequestAttribute("userId") Long userId,
                                               @RequestParam(required = false) String decision,
                                               @RequestParam(required = false) String comments,
                                               @RequestBody(required = false) Map<String, Object> body) {
        try {
            if (!role.equals("FINANCE_MANAGER") && !role.equals("SUPERADMIN")) {
                return Map.of("success", false, "message", "Access denied");
            }
            
            String decisionValue = decision;
            String commentsValue = comments;
            if ((decisionValue == null || decisionValue.isBlank()) && body != null) {
                Object d = body.get("decision");
                if (d == null) d = body.get("status");
                if (d == null) d = body.get("action");
                if (d == null) d = body.get("decisionStatus");
                if (d == null) {
                    Object approved = body.get("approved");
                    if (approved == null) approved = body.get("approve");
                    if (approved != null) {
                        boolean isApproved = Boolean.parseBoolean(String.valueOf(approved));
                        d = isApproved ? "APPROVE" : "REJECT";
                    }
                }
                if (d != null) {
                    decisionValue = String.valueOf(d);
                }
                Object c = body.get("comments");
                if (c == null) c = body.get("comment");
                if (c == null) c = body.get("remarks");
                if (c == null) c = body.get("remark");
                if (c == null) c = body.get("note");
                if (c != null) {
                    commentsValue = String.valueOf(c);
                }
            }
            if (decisionValue != null) {
                String v = decisionValue.trim().toUpperCase();
                if (v.equals("APPROVED") || v.equals("ACCEPT") || v.equals("ACCEPTED") || v.equals("YES") || v.equals("TRUE") || v.equals("1")) {
                    decisionValue = "APPROVE";
                } else if (v.equals("REJECTED") || v.equals("DECLINE") || v.equals("DECLINED") || v.equals("NO") || v.equals("FALSE") || v.equals("0")) {
                    decisionValue = "REJECT";
                }
            }
            if (decisionValue == null || decisionValue.isBlank()) {
                return Map.of("success", false, "message", "Missing required field: decision");
            }
            
            Requisition requisition = service.financeDecision(id, username, decisionValue, commentsValue, department);
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
    public Map<String, Object> unread(@RequestAttribute("username") String username,
                                      @RequestAttribute("role") String role,
                                      @RequestAttribute("department") String department,
                                      @RequestAttribute("userId") Long userId) {
        List<Notification> notifications = notificationRepository.findByUserIdAndIsReadFalse(username);
        System.out.println("DEBUG: Fetching notifications for user: " + username + 
                          ", Found " + notifications.size() + " unread notifications");
        
        // Also get all notifications for debugging
        List<Notification> allNotifications = notificationRepository.findByUserIdOrderByTimestampDesc(username);
        System.out.println("DEBUG: Total notifications for user: " + allNotifications.size());
        
        return Map.of("success", true, "notifications", notifications);
    }

    @DeleteMapping("/notifications/{notificationId}")
    public Map<String, Object> deleteNotification(@PathVariable Long notificationId,
                                                  @RequestAttribute("username") String username,
                                                  @RequestAttribute("role") String role,
                                                  @RequestAttribute("department") String department,
                                                  @RequestAttribute("userId") Long userId) {
        try {
            // Check if notification exists and belongs to the current user
            Notification notification = notificationRepository.findById(notificationId).orElse(null);
            if (notification == null) {
                return Map.of("success", false, "message", "Notification not found");
            }
            
            if (!notification.getUserId().equals(username)) {
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
    public Map<String, Object> budgets() {
        System.out.println("DEBUG: Getting budgets (no auth required)");

        try {
            List<Budget> budgets = service.getAllBudgets();
            System.out.println("DEBUG: Found " + budgets.size() + " budgets");

            // If no budgets found, initialize some default budgets
            if (budgets.isEmpty()) {
                System.out.println("DEBUG: No budgets found, initializing default budgets");
                service.initializeDefaultBudgets();
                budgets = service.getAllBudgets();
                System.out.println("DEBUG: After initialization, found " + budgets.size() + " budgets");
            }

            return Map.of("success", true, "budgets", budgets);
        } catch (Exception e) {
            System.err.println("DEBUG: Error getting budgets: " + e.getMessage());
            e.printStackTrace();
            return Map.of("success", false, "message", "Failed to load budgets: " + e.getMessage());
        }
    }

    @GetMapping("/requisitions/approved-this-month")
    public Map<String, Object> approvedThisMonth(@RequestAttribute("username") String username,
                                                 @RequestAttribute("role") String role,
                                                 @RequestAttribute("department") String department,
                                                 @RequestAttribute("userId") Long userId) {
        List<Requisition> approvedRequisitions = service.getApprovedThisMonth();
        return Map.of("success", true, "requisitions", approvedRequisitions);
    }

    @GetMapping("/requisitions/active-orders")
    public Map<String, Object> getActiveOrders(@RequestAttribute("username") String username,
                                               @RequestAttribute("role") String role,
                                               @RequestAttribute("department") String department,
                                               @RequestAttribute("userId") Long userId) {
        // Get requisitions that are approved but not yet completed (active orders)
        List<Requisition> activeOrders = requisitionRepository.findByStatus(RequisitionStatus.APPROVED);
        return Map.of("success", true, "requisitions", activeOrders);
    }

    @GetMapping("/requisitions/recent")
    public Map<String, Object> getRecentRequisitions(@RequestAttribute("username") String username,
                                                     @RequestAttribute("role") String role,
                                                     @RequestAttribute("department") String department,
                                                     @RequestAttribute("userId") Long userId) {
        System.out.println("DEBUG: Getting recent requisitions for user: " + username + ", Role: " + role);
        
        List<Requisition> recentRequisitions;
        
        if (role.equals("SUPERADMIN")) {
            // SUPERADMIN can see all recent requisitions
            recentRequisitions = requisitionRepository.findTop10ByOrderByCreatedAtDesc();
        } else if (role.equals("DEPARTMENT_MANAGER")) {
            // Department managers can see requisitions from their department only
            recentRequisitions = requisitionRepository.findTop10ByDepartmentOrderByCreatedAtDesc(department);
        } else if (role.equals("IT_MANAGER")) {
            // IT managers can only see requisitions that have been approved by department manager
            List<Requisition> allItRequisitions = requisitionRepository.findTop10ByStatusInOrderByCreatedAtDesc(
                Arrays.asList(RequisitionStatus.PENDING_IT_APPROVAL, RequisitionStatus.PENDING_FINANCE_APPROVAL, 
                             RequisitionStatus.APPROVED, RequisitionStatus.REJECTED)
            );
            recentRequisitions = allItRequisitions.stream().limit(10).collect(java.util.stream.Collectors.toList());
        } else if (role.equals("FINANCE_MANAGER")) {
            // Finance managers can only see requisitions that have been approved by both department and IT managers
            List<Requisition> allFinanceRequisitions = requisitionRepository.findTop10ByStatusInOrderByCreatedAtDesc(
                Arrays.asList(RequisitionStatus.PENDING_FINANCE_APPROVAL, RequisitionStatus.APPROVED, RequisitionStatus.REJECTED)
            );
            recentRequisitions = allFinanceRequisitions.stream().limit(10).collect(java.util.stream.Collectors.toList());
        } else {
            // For employees, show only their own requisitions
            recentRequisitions = requisitionRepository.findTop10ByCreatedByOrderByCreatedAtDesc(username);
        }
        
        System.out.println("DEBUG: Found " + recentRequisitions.size() + " recent requisitions for role: " + role);
        return Map.of("success", true, "requisitions", recentRequisitions);
    }
    
    @PutMapping("/requisitions/{id}")
    public Map<String, Object> updateRequisition(@PathVariable Long id,
                                                @RequestAttribute("username") String username,
                                                @RequestAttribute("role") String role,
                                                @RequestAttribute("department") String department,
                                                @RequestAttribute("userId") Long userId,
                                                @RequestBody CreateRequisitionRequest request) {
        try {
            System.out.println("DEBUG: Update requisition request - ID: " + id + ", User: " + username);
            
            System.out.println("DEBUG: Valid session for user: " + username);
            
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
                                                @RequestAttribute("username") String username,
                                                @RequestAttribute("role") String role,
                                                @RequestAttribute("department") String department,
                                                @RequestAttribute("userId") Long userId) {
        try {
            System.out.println("DEBUG: Delete requisition request - ID: " + id + ", User: " + username);
            
            System.out.println("DEBUG: Valid session for user: " + username);
            
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
                                                   @RequestAttribute("username") String username,
                                                   @RequestAttribute("role") String role,
                                                   @RequestAttribute("department") String department,
                                                   @RequestAttribute("userId") Long userId,
                                                   @RequestParam String vendorEmail,
                                                   @RequestParam String vendorName) {
        try {
            // Check if user has IT_MANAGER permission
            if (!role.equals("IT_MANAGER") && !role.equals("SUPERADMIN")) {
                return Map.of("success", false, "message", "Access denied. Only IT Managers can send emails.");
            }
            
            // Find the requisition
            Requisition requisition = requisitionRepository.findById(id).orElse(null);
            if (requisition == null) {
                return Map.of("success", false, "message", "Requisition not found");
            }
            
            // Send email - we need to pass the JWT token for the email service
            // For now, we'll pass the username as sessionId since the email service expects it
            boolean emailSent = emailService.sendRequisitionEmail(
                vendorEmail, 
                vendorName, 
                requisition, 
                username
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


