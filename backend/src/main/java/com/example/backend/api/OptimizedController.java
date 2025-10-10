package com.example.backend.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import com.example.backend.service.PaginationService;
import com.example.backend.service.CacheService;
import com.example.backend.service.ProcureService;
import com.example.backend.service.PdfService;
import com.example.backend.repo.RequisitionRepository;
import com.example.backend.repo.VendorPdfRepository;
import com.example.backend.model.RequisitionStatus;
import com.example.backend.model.UserRole;
import com.example.backend.service.AuthService;

import java.util.Map;

@RestController
@RequestMapping("/api/optimized")
public class OptimizedController {

    @Autowired
    private PaginationService paginationService;
    
    @Autowired
    private CacheService cacheService;
    
    @Autowired
    private ProcureService procureService;
    
    @Autowired
    private PdfService pdfService;
    
    @Autowired
    private RequisitionRepository requisitionRepository;
    
    @Autowired
    private VendorPdfRepository vendorPdfRepository;
    
    @Autowired
    private AuthService authService;

    // Optimized dashboard data with caching
    @GetMapping("/dashboard")
    public Map<String, Object> getDashboardData(@RequestParam String sessionId) {
        try {
            var currentUser = authService.getCurrentUser(sessionId);
            if (currentUser == null) {
                return Map.of("success", false, "message", "Invalid session");
            }

            // Get cached dashboard stats
            Map<String, Object> stats = cacheService.getDashboardStats(currentUser.getUsername());
            
            // Get recent data with pagination
            Pageable pageable = paginationService.createDashboardPageable(0);
            
            Map<String, Object> response = Map.of(
                "success", true,
                "stats", stats,
                "recentRequisitions", paginationService.createPaginatedResponse(
                    requisitionRepository.findRecentRequisitions(pageable)
                ),
                "recentPdfs", paginationService.createPaginatedResponse(
                    vendorPdfRepository.findRecentPdfs(pageable)
                )
            );
            
            return response;
        } catch (Exception e) {
            return Map.of("success", false, "message", "Error loading dashboard data: " + e.getMessage());
        }
    }

    // Optimized requisitions with pagination
    @GetMapping("/requisitions")
    public Map<String, Object> getRequisitions(
            @RequestParam String sessionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        try {
            var currentUser = authService.getCurrentUser(sessionId);
            if (currentUser == null) {
                return Map.of("success", false, "message", "Invalid session");
            }

            Pageable pageable = paginationService.createPageable(page, size, sortBy, sortDir);
            Page<?> requisitions;
            
            if (authService.isEmployee(currentUser)) {
                // Employee sees only their requisitions
                requisitions = requisitionRepository.findByCreatedByOrderByCreatedAtDesc(currentUser.getUsername(), pageable);
            } else {
                // Managers see all requisitions
                requisitions = requisitionRepository.findRecentRequisitions(pageable);
            }
            
            return Map.of(
                "success", true,
                "data", paginationService.createPaginatedResponse(requisitions)
            );
        } catch (Exception e) {
            return Map.of("success", false, "message", "Error loading requisitions: " + e.getMessage());
        }
    }

    // Optimized PDFs with pagination
    @GetMapping("/pdfs")
    public Map<String, Object> getPdfs(
            @RequestParam String sessionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "uploadedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        try {
            var currentUser = authService.getCurrentUser(sessionId);
            if (currentUser == null) {
                return Map.of("success", false, "message", "Invalid session");
            }

            Pageable pageable = paginationService.createPageable(page, size, sortBy, sortDir);
            Page<?> pdfs;
            
            if (authService.isEmployee(currentUser)) {
                // Employee sees only their PDFs
                pdfs = vendorPdfRepository.findByUploadedByOrderByUploadedAtDesc(currentUser.getUsername(), pageable);
            } else {
                // Managers see all PDFs
                pdfs = vendorPdfRepository.findAllByOrderByUploadedAtDesc(pageable);
            }
            
            return Map.of(
                "success", true,
                "data", paginationService.createPaginatedResponse(pdfs)
            );
        } catch (Exception e) {
            return Map.of("success", false, "message", "Error loading PDFs: " + e.getMessage());
        }
    }

    // Optimized pending approvals
    @GetMapping("/pending-approvals")
    public Map<String, Object> getPendingApprovals(
            @RequestParam String sessionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        try {
            var currentUser = authService.getCurrentUser(sessionId);
            if (currentUser == null) {
                return Map.of("success", false, "message", "Invalid session");
            }

            Pageable pageable = paginationService.createPageable(page, size, "createdAt", "desc");
            Page<?> approvals;
            
            if (currentUser.getRole() == UserRole.SUPERADMIN) {
                // SUPERADMIN sees all pending approvals (both IT and Finance)
                // For now, return IT approvals - the frontend will handle combining both
                approvals = requisitionRepository.findByStatusOrderByCreatedAtDesc(RequisitionStatus.PENDING_IT_APPROVAL, pageable);
            } else if (authService.hasRole(currentUser, "IT_MANAGER")) {
                approvals = requisitionRepository.findByStatusOrderByCreatedAtDesc(RequisitionStatus.PENDING_IT_APPROVAL, pageable);
            } else if (authService.hasRole(currentUser, "FINANCE_MANAGER")) {
                approvals = requisitionRepository.findByStatusOrderByCreatedAtDesc(RequisitionStatus.PENDING_FINANCE_APPROVAL, pageable);
            } else {
                return Map.of("success", false, "message", "Access denied");
            }
            
            return Map.of(
                "success", true,
                "data", paginationService.createPaginatedResponse(approvals)
            );
        } catch (Exception e) {
            return Map.of("success", false, "message", "Error loading pending approvals: " + e.getMessage());
        }
    }

    // Clear cache endpoint (for admin use)
    @PostMapping("/clear-cache")
    public Map<String, Object> clearCache(@RequestParam String sessionId) {
        try {
            var currentUser = authService.getCurrentUser(sessionId);
            if (currentUser == null || !authService.hasRole(currentUser, "SUPERADMIN")) {
                return Map.of("success", false, "message", "Access denied");
            }

            cacheService.evictUserCache();
            cacheService.evictBudgetCache();
            
            return Map.of("success", true, "message", "Cache cleared successfully");
        } catch (Exception e) {
            return Map.of("success", false, "message", "Error clearing cache: " + e.getMessage());
        }
    }
}
