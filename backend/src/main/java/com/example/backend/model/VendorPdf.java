package com.example.backend.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
public class VendorPdf {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fileName;
    
    private String originalFileName;
    
    private String filePath;
    
    private String uploadedBy; // IT Manager username
    
    private String description;
    
    private Long requisitionId; // Link to specific requisition
    
    private Instant uploadedAt = Instant.now();
    
    private boolean isProcessed = false;
    
    private boolean isRejected = false;
    
    private String rejectionReason;
    
    private String approvalStage = "DEPARTMENT"; // DEPARTMENT, IT, FINANCE, APPROVED

    // Constructors
    public VendorPdf() {}

    public VendorPdf(String fileName, String originalFileName, String filePath, String uploadedBy, String description) {
        this.fileName = fileName;
        this.originalFileName = originalFileName;
        this.filePath = filePath;
        this.uploadedBy = uploadedBy;
        this.description = description;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getOriginalFileName() { return originalFileName; }
    public void setOriginalFileName(String originalFileName) { this.originalFileName = originalFileName; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public String getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(String uploadedBy) { this.uploadedBy = uploadedBy; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Long getRequisitionId() { return requisitionId; }
    public void setRequisitionId(Long requisitionId) { this.requisitionId = requisitionId; }

    public Instant getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(Instant uploadedAt) { this.uploadedAt = uploadedAt; }

    public boolean isProcessed() { return isProcessed; }
    public void setProcessed(boolean processed) { isProcessed = processed; }

    public boolean isRejected() { return isRejected; }
    public void setRejected(boolean rejected) { isRejected = rejected; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public String getApprovalStage() { return approvalStage; }
    public void setApprovalStage(String approvalStage) { this.approvalStage = approvalStage; }
}
