package com.example.backend.repo;

import com.example.backend.model.Requisition;
import com.example.backend.model.RequisitionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface RequisitionRepository extends JpaRepository<Requisition, Long> {
    
    // Basic queries with pagination
    Page<Requisition> findByStatus(RequisitionStatus status, Pageable pageable);
    List<Requisition> findByStatus(RequisitionStatus status);
    
    // Optimized queries for dashboard
    @Query("SELECT r FROM Requisition r WHERE r.status = :status ORDER BY r.createdAt DESC")
    Page<Requisition> findByStatusOrderByCreatedAtDesc(@Param("status") RequisitionStatus status, Pageable pageable);
    
    // User-specific queries
    Page<Requisition> findByCreatedByOrderByCreatedAtDesc(String createdBy, Pageable pageable);
    List<Requisition> findByCreatedByOrderByCreatedAtDesc(String createdBy);
    
    // Department queries
    Page<Requisition> findByDepartmentOrderByCreatedAtDesc(String department, Pageable pageable);
    
    // Date range queries for reporting
    @Query("SELECT r FROM Requisition r WHERE r.status = 'APPROVED' AND r.createdAt >= :startDate AND r.createdAt <= :endDate")
    List<Requisition> findApprovedInDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    // Count queries for statistics
    long countByStatus(RequisitionStatus status);
    long countByCreatedBy(String createdBy);
    long countByDepartment(String department);
    
    // Recent requisitions for dashboard
    @Query("SELECT r FROM Requisition r ORDER BY r.createdAt DESC")
    Page<Requisition> findRecentRequisitions(Pageable pageable);
}
