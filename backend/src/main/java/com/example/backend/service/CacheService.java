package com.example.backend.service;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import com.example.backend.repo.RequisitionRepository;
import com.example.backend.repo.VendorPdfRepository;
import com.example.backend.repo.BudgetRepository;
import com.example.backend.model.RequisitionStatus;
import com.example.backend.model.Budget;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Service
public class CacheService {

    @Autowired
    private RequisitionRepository requisitionRepository;
    
    @Autowired
    private VendorPdfRepository vendorPdfRepository;
    
    @Autowired
    private BudgetRepository budgetRepository;

    // Cache dashboard statistics
    @Cacheable(value = "dashboardStats", key = "#userId")
    public Map<String, Object> getDashboardStats(String userId) {
        Map<String, Object> stats = new HashMap<>();
        
        // Get counts for different statuses
        stats.put("pendingItApprovals", requisitionRepository.countByStatus(RequisitionStatus.PENDING_IT_APPROVAL));
        stats.put("pendingFinanceApprovals", requisitionRepository.countByStatus(RequisitionStatus.PENDING_FINANCE_APPROVAL));
        stats.put("approvedThisMonth", requisitionRepository.countByStatus(RequisitionStatus.APPROVED));
        stats.put("myRequisitions", requisitionRepository.countByCreatedBy(userId));
        stats.put("myPdfs", vendorPdfRepository.countByUploadedBy(userId));
        
        return stats;
    }

    // Cache budget information
    @Cacheable(value = "budgets")
    public List<Budget> getAllBudgets() {
        return budgetRepository.findAll();
    }

    // Cache user-specific requisitions
    @Cacheable(value = "userRequisitions", key = "#userId")
    public List<Map<String, Object>> getUserRequisitions(String userId) {
        return requisitionRepository.findByCreatedByOrderByCreatedAtDesc(userId)
                .stream()
                .limit(10) // Limit to recent 10 for performance
                .map(req -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", req.getId());
                    map.put("itemName", req.getItemName());
                    map.put("status", req.getStatus());
                    map.put("createdAt", req.getCreatedAt());
                    return map;
                })
                .toList();
    }

    // Cache user-specific PDFs
    @Cacheable(value = "userPdfs", key = "#userId")
    public List<Map<String, Object>> getUserPdfs(String userId) {
        return vendorPdfRepository.findByUploadedByOrderByUploadedAtDesc(userId)
                .stream()
                .limit(10) // Limit to recent 10 for performance
                .map(pdf -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", pdf.getId());
                    map.put("originalFileName", pdf.getOriginalFileName());
                    map.put("processed", pdf.isProcessed());
                    map.put("uploadedAt", pdf.getUploadedAt());
                    return map;
                })
                .toList();
    }

    // Evict cache when data changes
    @CacheEvict(value = {"dashboardStats", "userRequisitions", "userPdfs"}, allEntries = true)
    public void evictUserCache() {
        // This method will be called when user data changes
    }

    @CacheEvict(value = "budgets", allEntries = true)
    public void evictBudgetCache() {
        // This method will be called when budget data changes
    }

    // Update cache when new data is added
    @CachePut(value = "dashboardStats", key = "#userId")
    public Map<String, Object> updateDashboardStats(String userId) {
        return getDashboardStats(userId);
    }
}
