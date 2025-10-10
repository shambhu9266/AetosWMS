package com.example.backend.repo;

import com.example.backend.model.VendorPdf;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VendorPdfRepository extends JpaRepository<VendorPdf, Long> {
    
    // Basic queries with pagination
    Page<VendorPdf> findByUploadedByOrderByUploadedAtDesc(String uploadedBy, Pageable pageable);
    List<VendorPdf> findByUploadedByOrderByUploadedAtDesc(String uploadedBy);
    
    Page<VendorPdf> findAllByOrderByUploadedAtDesc(Pageable pageable);
    List<VendorPdf> findAllByOrderByUploadedAtDesc();
    
    Page<VendorPdf> findByIsProcessedFalseOrderByUploadedAtDesc(Pageable pageable);
    List<VendorPdf> findByIsProcessedFalseOrderByUploadedAtDesc();
    
    // Status-based queries
    Page<VendorPdf> findByIsProcessedTrueOrderByUploadedAtDesc(Pageable pageable);
    Page<VendorPdf> findByIsRejectedTrueOrderByUploadedAtDesc(Pageable pageable);
    
    // Requisition-linked queries
    Page<VendorPdf> findByRequisitionIdOrderByUploadedAtDesc(Long requisitionId, Pageable pageable);
    Page<VendorPdf> findByRequisitionIdIsNullOrderByUploadedAtDesc(Pageable pageable);
    
    // Optimized queries for dashboard
    @Query("SELECT v FROM VendorPdf v WHERE v.uploadedBy = :uploadedBy ORDER BY v.uploadedAt DESC")
    Page<VendorPdf> findRecentByUploader(@Param("uploadedBy") String uploadedBy, Pageable pageable);
    
    // Count queries for statistics
    long countByUploadedBy(String uploadedBy);
    long countByIsProcessedTrue();
    long countByIsProcessedFalse();
    long countByIsRejectedTrue();
    
    // Recent PDFs for dashboard (limited)
    @Query("SELECT v FROM VendorPdf v ORDER BY v.uploadedAt DESC")
    Page<VendorPdf> findRecentPdfs(Pageable pageable);
    
    // Approval stage queries
    List<VendorPdf> findByApprovalStageOrderByUploadedAtDesc(String approvalStage);
    Page<VendorPdf> findByApprovalStageOrderByUploadedAtDesc(String approvalStage, Pageable pageable);
    
    // Count by approval stage
    long countByApprovalStage(String approvalStage);
}
