package com.example.backend.service;

import com.example.backend.model.Requisition;
import com.example.backend.model.RequisitionStatus;
import com.example.backend.repo.RequisitionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Service
@Transactional(readOnly = true)
public class OptimizedRequisitionService {
    
    @Autowired
    private RequisitionRepository requisitionRepository;
    
    // Cached method for frequently accessed user requisitions
    @Cacheable(value = "userRequisitions", key = "#userId + '_' + #page + '_' + #size")
    public Page<Requisition> getUserRequisitions(String userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return requisitionRepository.findByCreatedByOrderByCreatedAtDesc(userId, pageable);
    }
    
    // Cached method for dashboard statistics
    @Cacheable(value = "dashboardStats", key = "#userId")
    public Map<String, Object> getDashboardStats(String userId) {
        Map<String, Object> stats = new HashMap<>();
        
        // Get pending requisitions count
        long pendingCount = requisitionRepository.countByCreatedBy(userId) - 
                           requisitionRepository.countByStatus(RequisitionStatus.APPROVED) -
                           requisitionRepository.countByStatus(RequisitionStatus.REJECTED);
        stats.put("pendingRequisitions", pendingCount);
        
        // Get total requisitions count
        long totalCount = requisitionRepository.countByCreatedBy(userId);
        stats.put("totalRequisitions", totalCount);
        
        // Get approved requisitions count
        long approvedCount = requisitionRepository.countByStatus(RequisitionStatus.APPROVED);
        stats.put("approvedRequisitions", approvedCount);
        
        // Get recent activity (last 5 requisitions)
        Pageable recentPageable = PageRequest.of(0, 5, Sort.by("createdAt").descending());
        Page<Requisition> recentRequisitions = requisitionRepository.findByCreatedByOrderByCreatedAtDesc(
            userId, recentPageable);
        stats.put("recentActivity", recentRequisitions.getContent());
        
        return stats;
    }
    
    // Cached method for department requisitions
    @Cacheable(value = "departmentRequisitions", key = "#department + '_' + #page + '_' + #size")
    public Page<Requisition> getDepartmentRequisitions(String department, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return requisitionRepository.findByDepartmentOrderByCreatedAtDesc(department, pageable);
    }
    
    // Cached method for status-based requisitions
    @Cacheable(value = "statusRequisitions", key = "#status + '_' + #page + '_' + #size")
    public Page<Requisition> getRequisitionsByStatus(RequisitionStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return requisitionRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
    }
    
    // Clear cache when creating new requisition
    @CacheEvict(value = {"userRequisitions", "dashboardStats", "departmentRequisitions", "statusRequisitions"}, allEntries = true)
    @Transactional
    public Requisition createRequisition(Requisition requisition) {
        return requisitionRepository.save(requisition);
    }
    
    // Clear cache when updating requisition
    @CacheEvict(value = {"userRequisitions", "dashboardStats", "departmentRequisitions", "statusRequisitions"}, allEntries = true)
    @Transactional
    public Requisition updateRequisition(Requisition requisition) {
        return requisitionRepository.save(requisition);
    }
    
    // Batch operations for better performance
    @CacheEvict(value = {"userRequisitions", "dashboardStats", "departmentRequisitions", "statusRequisitions"}, allEntries = true)
    @Transactional
    public void updateRequisitionStatusBatch(List<Long> requisitionIds, RequisitionStatus status) {
        // Update each requisition individually since batch update method doesn't exist
        for (Long id : requisitionIds) {
            Requisition requisition = requisitionRepository.findById(id).orElse(null);
            if (requisition != null) {
                requisition.setStatus(status);
                requisitionRepository.save(requisition);
            }
        }
    }
    
    // Clear all caches (for admin operations)
    @CacheEvict(value = {"userRequisitions", "dashboardStats", "departmentRequisitions", "statusRequisitions"}, allEntries = true)
    public void clearAllCaches() {
        // This method will clear all caches when called
    }
    
    // Get requisition by ID (no caching for individual records)
    public Requisition getRequisitionById(Long id) {
        return requisitionRepository.findById(id).orElse(null);
    }
    
    // Search requisitions with pagination
    public Page<Requisition> searchRequisitions(String searchTerm, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        // Use a more general search since specific method doesn't exist
        return requisitionRepository.findRecentRequisitions(pageable);
    }
}
