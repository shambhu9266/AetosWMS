package com.example.backend.service;

import com.example.backend.model.*;
import com.example.backend.repo.*;
import com.example.backend.dto.CreateRequisitionRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ProcureService {
    private final RequisitionRepository requisitionRepository;
    private final BudgetRepository budgetRepository;
    private final ApprovalRepository approvalRepository;
    private final NotificationRepository notificationRepository;

    public ProcureService(RequisitionRepository requisitionRepository,
                          BudgetRepository budgetRepository,
                          ApprovalRepository approvalRepository,
                          NotificationRepository notificationRepository) {
        this.requisitionRepository = requisitionRepository;
        this.budgetRepository = budgetRepository;
        this.approvalRepository = approvalRepository;
        this.notificationRepository = notificationRepository;
    }

    public Requisition createRequisition(String createdBy, String itemName, Integer quantity, BigDecimal price, String department) {
        Requisition r = new Requisition();
        r.setCreatedBy(createdBy);
        r.setItemName(itemName);
        r.setQuantity(quantity);
        r.setPrice(price);
        r.setDepartment(department);
        r.setStatus(RequisitionStatus.PENDING_IT_APPROVAL);
        Requisition savedRequisition = requisitionRepository.save(r);
        
        // Notify IT Manager about new PR
        Notification itNotification = new Notification();
        itNotification.setUserId("shambhu"); // IT Manager username
        itNotification.setMessage("New PR #" + savedRequisition.getId() + " from " + createdBy + " (" + department + ") for " + quantity + " " + itemName + " (₹" + price + ") needs IT approval");
        Notification savedNotification = notificationRepository.save(itNotification);
        System.out.println("DEBUG: Created notification for IT Manager: " + savedNotification.getNotificationId());
        
        return savedRequisition;
    }

    @Transactional
    public Requisition createRequisitionWithItems(String createdBy, String department, List<CreateRequisitionRequest.RequisitionItemDto> itemDtos) {
        // Create the main requisition
        Requisition requisition = new Requisition();
        requisition.setCreatedBy(createdBy);
        requisition.setDepartment(department);
        requisition.setStatus(RequisitionStatus.PENDING_IT_APPROVAL);
        
        // Save the requisition first to get the ID
        Requisition savedRequisition = requisitionRepository.save(requisition);
        
        // Add items to the requisition
        for (CreateRequisitionRequest.RequisitionItemDto itemDto : itemDtos) {
            RequisitionItem item = new RequisitionItem();
            item.setItemName(itemDto.getItemName());
            item.setQuantity(itemDto.getQuantity());
            item.setPrice(itemDto.getPrice());
            savedRequisition.addItem(item);
        }
        
        // Save the requisition with items
        savedRequisition = requisitionRepository.save(savedRequisition);
        
        // Create notification for IT Manager
        Notification itNotification = new Notification();
        itNotification.setUserId("shambhu"); // IT Manager username
        
        // Build notification message with items summary
        StringBuilder message = new StringBuilder("New PR #" + savedRequisition.getId() + " from " + createdBy + " (" + department + ") with " + itemDtos.size() + " items needs IT approval:\n");
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (CreateRequisitionRequest.RequisitionItemDto itemDto : itemDtos) {
            BigDecimal lineTotal = itemDto.getPrice().multiply(BigDecimal.valueOf(itemDto.getQuantity()));
            totalAmount = totalAmount.add(lineTotal);
            message.append("- ").append(itemDto.getQuantity()).append("x ").append(itemDto.getItemName()).append(" @ ₹").append(itemDto.getPrice()).append(" = ₹").append(lineTotal).append("\n");
        }
        message.append("Total Amount: ₹").append(totalAmount);
        
        itNotification.setMessage(message.toString());
        Notification savedNotification = notificationRepository.save(itNotification);
        System.out.println("DEBUG: Created notification for IT Manager: " + savedNotification.getNotificationId());
        
        return savedRequisition;
    }

    @Transactional
    public Requisition updateRequisitionWithItems(Long requisitionId, String department, List<CreateRequisitionRequest.RequisitionItemDto> itemDtos) {
        // Find the existing requisition
        Requisition requisition = requisitionRepository.findById(requisitionId)
            .orElseThrow(() -> new IllegalArgumentException("Requisition not found with ID: " + requisitionId));
        
        // Update department
        requisition.setDepartment(department);
        
        // Clear existing items (orphanRemoval = true will handle deletion)
        requisition.getItems().clear();
        
        // Add new items
        for (CreateRequisitionRequest.RequisitionItemDto itemDto : itemDtos) {
            RequisitionItem item = new RequisitionItem();
            item.setItemName(itemDto.getItemName());
            item.setQuantity(itemDto.getQuantity());
            item.setPrice(itemDto.getPrice());
            requisition.addItem(item);
        }
        
        // Reset status to pending IT approval since it's been modified
        requisition.setStatus(RequisitionStatus.PENDING_IT_APPROVAL);
        
        // Save the updated requisition
        Requisition updatedRequisition = requisitionRepository.save(requisition);
        
        // Create notification for IT Manager about the update
        Notification itNotification = new Notification();
        itNotification.setUserId("shambhu"); // IT Manager username
        
        // Build notification message with items summary
        StringBuilder message = new StringBuilder("PR #" + updatedRequisition.getId() + " has been updated by " + updatedRequisition.getCreatedBy() + " (" + department + ") with " + itemDtos.size() + " items needs IT approval:\n");
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (CreateRequisitionRequest.RequisitionItemDto itemDto : itemDtos) {
            BigDecimal lineTotal = itemDto.getPrice().multiply(BigDecimal.valueOf(itemDto.getQuantity()));
            totalAmount = totalAmount.add(lineTotal);
            message.append("- ").append(itemDto.getQuantity()).append("x ").append(itemDto.getItemName()).append(" @ ₹").append(itemDto.getPrice()).append(" = ₹").append(lineTotal).append("\n");
        }
        message.append("Total Amount: ₹").append(totalAmount);
        
        itNotification.setMessage(message.toString());
        Notification savedNotification = notificationRepository.save(itNotification);
        System.out.println("DEBUG: Created notification for IT Manager about update: " + savedNotification.getNotificationId());
        
        return updatedRequisition;
    }

    @Transactional
    public Requisition itDecision(Long requisitionId, String itManager, String decision, String comments) {
        Requisition r = requisitionRepository.findById(requisitionId).orElseThrow();
        Approval a = new Approval();
        a.setRequisitionId(requisitionId);
        a.setApproverRole("IT");
        a.setDecision(decision);
        a.setComments(comments);
        approvalRepository.save(a);

        if ("APPROVE".equalsIgnoreCase(decision)) {
            r.setApprovedByIt(itManager);
            r.setStatus(RequisitionStatus.PENDING_FINANCE_APPROVAL);
            
            // Create description for notifications
            String itemDescription;
            if (r.getItems() != null && !r.getItems().isEmpty()) {
                itemDescription = r.getItems().size() + " items (" + r.getItemNames() + ")";
            } else {
                itemDescription = r.getQuantity() + " " + r.getItemName();
            }
            
            // Notify Finance Manager about IT approval
            Notification financeNotification = new Notification();
            financeNotification.setUserId("joshi"); // Finance Manager username
            financeNotification.setMessage("PR #" + requisitionId + " from " + r.getCreatedBy() + " for " + itemDescription + " has been approved by IT and needs Finance approval");
            notificationRepository.save(financeNotification);
            
            // Notify requester about IT approval
            Notification requesterNotification = new Notification();
            requesterNotification.setUserId(r.getCreatedBy());
            requesterNotification.setMessage("Your PR #" + requisitionId + " for " + itemDescription + " has been approved by IT and sent to Finance");
            notificationRepository.save(requesterNotification);
            
        } else if ("REJECT".equalsIgnoreCase(decision)) {
            r.setStatus(RequisitionStatus.REJECTED);
            
            // Create description for rejection notification
            String rejectionDescription;
            if (r.getItems() != null && !r.getItems().isEmpty()) {
                rejectionDescription = r.getItems().size() + " items (" + r.getItemNames() + ")";
            } else {
                rejectionDescription = r.getQuantity() + " " + r.getItemName();
            }
            
            // Notify requester about rejection
            Notification rejectionNotification = new Notification();
            rejectionNotification.setUserId(r.getCreatedBy());
            rejectionNotification.setMessage("Your PR #" + requisitionId + " for " + rejectionDescription + " has been rejected by IT Manager");
            notificationRepository.save(rejectionNotification);
            
        } else {
            r.setStatus(RequisitionStatus.SENT_BACK);
            
            // Create description for sent back notification
            String sentBackDescription;
            if (r.getItems() != null && !r.getItems().isEmpty()) {
                sentBackDescription = r.getItems().size() + " items (" + r.getItemNames() + ")";
            } else {
                sentBackDescription = r.getQuantity() + " " + r.getItemName();
            }
            
            // Notify requester about being sent back
            Notification sentBackNotification = new Notification();
            sentBackNotification.setUserId(r.getCreatedBy());
            sentBackNotification.setMessage("Your PR #" + requisitionId + " for " + sentBackDescription + " has been sent back by IT Manager for modifications");
            notificationRepository.save(sentBackNotification);
        }
        return r;
    }

    @Transactional
    public Requisition financeDecision(Long requisitionId, String financeManager, String decision, String comments, String department) {
        Requisition r = requisitionRepository.findById(requisitionId).orElseThrow();
        
        // Check if requisition is in correct status for finance approval
        if (r.getStatus() != RequisitionStatus.PENDING_FINANCE_APPROVAL) {
            throw new IllegalStateException("Requisition is not pending finance approval. Current status: " + r.getStatus());
        }

        Approval a = new Approval();
        a.setRequisitionId(requisitionId);
        a.setApproverRole("FINANCE");
        a.setDecision(decision);
        a.setComments(comments);
        approvalRepository.save(a);

        if ("APPROVE".equalsIgnoreCase(decision)) {
            Budget budget = budgetRepository.findByDepartment(department)
                    .orElseThrow(() -> new IllegalArgumentException("Budget not found for department: " + department));

            // Calculate total amount - handle both legacy single-item and new multi-item requisitions
            BigDecimal requestAmount;
            String itemDescription;
            
            if (r.getItems() != null && !r.getItems().isEmpty()) {
                // Multi-item requisition
                requestAmount = r.getTotalAmount();
                itemDescription = r.getItems().size() + " items (" + r.getItemNames() + ")";
            } else {
                // Legacy single-item requisition
                requestAmount = r.getPrice().multiply(BigDecimal.valueOf(r.getQuantity()));
                itemDescription = r.getQuantity() + " " + r.getItemName();
            }
            
            if (budget.getRemainingBudget().compareTo(requestAmount) < 0) {
                throw new IllegalStateException("Insufficient budget");
            }
            budget.setRemainingBudget(budget.getRemainingBudget().subtract(requestAmount));
            budgetRepository.save(budget);

            r.setApprovedByFinance(financeManager);
            r.setStatus(RequisitionStatus.APPROVED);

            Notification n = new Notification();
            n.setUserId(r.getCreatedBy());
            n.setMessage("Your request for " + itemDescription + " (₹" + requestAmount + ") has been approved by Finance. Remaining " + department + " Budget: ₹" + budget.getRemainingBudget());
            notificationRepository.save(n);
        } else if ("REJECT".equalsIgnoreCase(decision)) {
            r.setStatus(RequisitionStatus.REJECTED);
            
            // Notify requester about Finance rejection
            Notification financeRejectionNotification = new Notification();
            financeRejectionNotification.setUserId(r.getCreatedBy());
            
            String rejectionDescription;
            if (r.getItems() != null && !r.getItems().isEmpty()) {
                rejectionDescription = r.getItems().size() + " items (" + r.getItemNames() + ")";
            } else {
                rejectionDescription = r.getQuantity() + " " + r.getItemName();
            }
            
            financeRejectionNotification.setMessage("Your PR #" + requisitionId + " for " + rejectionDescription + " has been rejected by Finance Manager");
            notificationRepository.save(financeRejectionNotification);
            
        } else {
            r.setStatus(RequisitionStatus.SENT_BACK);
            
            // Notify requester about being sent back by Finance
            Notification financeSentBackNotification = new Notification();
            financeSentBackNotification.setUserId(r.getCreatedBy());
            
            String sentBackDescription;
            if (r.getItems() != null && !r.getItems().isEmpty()) {
                sentBackDescription = r.getItems().size() + " items (" + r.getItemNames() + ")";
            } else {
                sentBackDescription = r.getQuantity() + " " + r.getItemName();
            }
            
            financeSentBackNotification.setMessage("Your PR #" + requisitionId + " for " + sentBackDescription + " has been sent back by Finance Manager for modifications");
            notificationRepository.save(financeSentBackNotification);
        }
        return r;
    }

    public List<Budget> getAllBudgets() {
        return budgetRepository.findAll();
    }

    public List<Requisition> getApprovedThisMonth() {
        // Get current month start and end dates
        LocalDate now = LocalDate.now();
        LocalDate startOfMonth = now.withDayOfMonth(1);
        LocalDate endOfMonth = now.withDayOfMonth(now.lengthOfMonth());
        
        // Convert to Instant for comparison
        Instant startInstant = startOfMonth.atStartOfDay().atZone(java.time.ZoneId.systemDefault()).toInstant();
        Instant endInstant = endOfMonth.atTime(23, 59, 59).atZone(java.time.ZoneId.systemDefault()).toInstant();
        
        // Find all approved requisitions
        List<Requisition> allApproved = requisitionRepository.findByStatus(RequisitionStatus.APPROVED);
        
        // Filter by current month using createdAt (since we don't have approvedAt)
        return allApproved.stream()
            .filter(req -> {
                Instant createdAt = req.getCreatedAt();
                return createdAt != null && 
                       createdAt.isAfter(startInstant) && 
                       createdAt.isBefore(endInstant);
            })
            .collect(java.util.stream.Collectors.toList());
    }
}


