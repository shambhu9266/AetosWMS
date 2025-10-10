package com.example.backend.service;

import com.example.backend.model.Notification;
import com.example.backend.model.VendorPdf;
import com.example.backend.model.Requisition;
import com.example.backend.repo.NotificationRepository;
import com.example.backend.repo.VendorPdfRepository;
import com.example.backend.repo.RequisitionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class PdfService {
    
    @Autowired
    private VendorPdfRepository vendorPdfRepository;
    
    @Autowired
    private NotificationRepository notificationRepository;
    
    @Autowired
    private RequisitionRepository requisitionRepository;
    
    private static final String UPLOAD_DIR = "uploads/pdfs/";
    
    public VendorPdf uploadPdf(MultipartFile file, String uploadedBy, String description, Long requisitionId) throws IOException {
        // Create upload directory if it doesn't exist
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        // Generate unique filename
        String originalFileName = file.getOriginalFilename();
        String fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
        String uniqueFileName = UUID.randomUUID().toString() + fileExtension;
        
        // Save file to disk
        Path filePath = uploadPath.resolve(uniqueFileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        
        // Save file info to database
        VendorPdf vendorPdf = new VendorPdf();
        vendorPdf.setFileName(uniqueFileName);
        vendorPdf.setOriginalFileName(originalFileName);
        vendorPdf.setFilePath(filePath.toString());
        vendorPdf.setUploadedBy(uploadedBy);
        vendorPdf.setDescription(description);
        vendorPdf.setRequisitionId(requisitionId);
        vendorPdf.setUploadedAt(Instant.now());
        vendorPdf.setProcessed(false);
        
        // Set initial approval stage
        try {
            vendorPdf.setApprovalStage("DEPARTMENT");
        } catch (Exception e) {
            System.out.println("DEBUG: Approval stage field not available, using processed flag: " + e.getMessage());
        }
        
        VendorPdf savedPdf = vendorPdfRepository.save(vendorPdf);
        
        // Notify Department Manager first (employees upload PDFs to Department Manager for approval)
        System.out.println("DEBUG: ===== PDF UPLOAD: About to notify department manager ======");
        System.out.println("DEBUG: PDF uploaded by: " + uploadedBy + ", requisitionId: " + requisitionId + ", filename: " + originalFileName);
        notifyDepartmentManager(savedPdf);
        
        return savedPdf;
    }
    
    private void notifyDepartmentManager(VendorPdf vendorPdf) {
        // Get the department manager based on the requisition's department
        String departmentManager = getDepartmentManagerForPdf(vendorPdf);
        
        // Check if notification already exists for this requisition to prevent duplicates
        String expectedMessageStart = "New vendor PDF uploaded by " + vendorPdf.getUploadedBy();
        boolean notificationExists = false;
        
        if (vendorPdf.getRequisitionId() != null) {
            // For PDFs linked to requisitions, check if any notification exists for this requisition
            notificationExists = notificationRepository.findAll().stream()
                .anyMatch(n -> n.getUserId().equals(departmentManager) && 
                              n.getMessage().contains("Requisition #" + vendorPdf.getRequisitionId()));
        } else {
            // For standalone PDFs, check if notification exists for this user
            notificationExists = notificationRepository.findAll().stream()
                .anyMatch(n -> n.getUserId().equals(departmentManager) && 
                              n.getMessage().startsWith(expectedMessageStart));
        }
        
        if (notificationExists) {
            System.out.println("DEBUG: PDF notification already exists for requisition #" + vendorPdf.getRequisitionId() + ", skipping duplicate creation");
        } else {
            Notification notification = new Notification();
            notification.setUserId(departmentManager);
            
            String message = "New vendor PDF uploaded by " + vendorPdf.getUploadedBy() + 
                            ": " + vendorPdf.getOriginalFileName();
            
            // Add requisition details if linked
            if (vendorPdf.getRequisitionId() != null) {
                Requisition requisition = requisitionRepository.findById(vendorPdf.getRequisitionId()).orElse(null);
                if (requisition != null) {
                    message += " for Requisition #" + requisition.getId() + 
                              " (" + requisition.getItemName() + " - " + requisition.getQuantity() + " units)";
                }
            }
            
            if (vendorPdf.getDescription() != null && !vendorPdf.getDescription().trim().isEmpty()) {
                message += " - " + vendorPdf.getDescription();
            }
            
            notification.setMessage(message);
            notification.setRead(false);
            notificationRepository.save(notification);
            
            System.out.println("DEBUG: Created PDF notification for Department Manager: " + departmentManager);
        }
    }
    
    private String getDepartmentManagerForPdf(VendorPdf vendorPdf) {
        // If PDF is linked to a requisition, get the department manager for that requisition's department
        if (vendorPdf.getRequisitionId() != null) {
            Requisition requisition = requisitionRepository.findById(vendorPdf.getRequisitionId()).orElse(null);
            if (requisition != null) {
                return getDepartmentManager(requisition.getDepartment());
            }
        }
        
        // Default to sales manager if no requisition linked
        return "salesmanager";
    }
    
    private String getDepartmentManager(String department) {
        // Map departments to their managers
        switch (department.toLowerCase()) {
            case "sales":
                return "salesmanager";
            case "it":
                return "itmanager";
            default:
                return "salesmanager"; // Default fallback
        }
    }
    
    private void notifyITTeam(VendorPdf vendorPdf) {
        Notification notification = new Notification();
        notification.setUserId("itmanager"); // IT Manager username
        
        String message = "Department-approved vendor PDF uploaded by " + vendorPdf.getUploadedBy() + 
                        ": " + vendorPdf.getOriginalFileName();
        
        // Add requisition details if linked
        if (vendorPdf.getRequisitionId() != null) {
            Requisition requisition = requisitionRepository.findById(vendorPdf.getRequisitionId()).orElse(null);
            if (requisition != null) {
                message += " for Requisition #" + requisition.getId() + 
                          " (" + requisition.getItemName() + " - " + requisition.getQuantity() + " units)";
            }
        }
        
        if (vendorPdf.getDescription() != null && !vendorPdf.getDescription().trim().isEmpty()) {
            message += " - " + vendorPdf.getDescription();
        }
        
        notification.setMessage(message);
        notification.setRead(false);
        notificationRepository.save(notification);
        
        System.out.println("DEBUG: Created PDF notification for IT Manager: itmanager");
    }
    
    private void notifyFinanceTeam(VendorPdf vendorPdf) {
        Notification notification = new Notification();
        notification.setUserId("joshi"); // Finance Manager username
        
        String message = "IT-approved vendor PDF uploaded by " + vendorPdf.getUploadedBy() + 
                        ": " + vendorPdf.getOriginalFileName();
        
        // Add requisition details if linked
        if (vendorPdf.getRequisitionId() != null) {
            Requisition requisition = requisitionRepository.findById(vendorPdf.getRequisitionId()).orElse(null);
            if (requisition != null) {
                message += " for Requisition #" + requisition.getId() + 
                          " (" + requisition.getItemName() + " - " + requisition.getQuantity() + " units)";
            }
        }
        
        if (vendorPdf.getDescription() != null && !vendorPdf.getDescription().trim().isEmpty()) {
            message += " - " + vendorPdf.getDescription();
        }
        
        notification.setMessage(message);
        notification.setRead(false);
        notificationRepository.save(notification);
    }
    
    public List<VendorPdf> getAllPdfs() {
        return vendorPdfRepository.findAllByOrderByUploadedAtDesc();
    }
    
    public List<VendorPdf> getPdfsByUploader(String uploadedBy) {
        return vendorPdfRepository.findByUploadedByOrderByUploadedAtDesc(uploadedBy);
    }
    
    public List<VendorPdf> getUnprocessedPdfs() {
        return vendorPdfRepository.findByIsProcessedFalseOrderByUploadedAtDesc();
    }
    
    public List<VendorPdf> getPdfsByApprovalStage(String approvalStage) {
        return vendorPdfRepository.findByApprovalStageOrderByUploadedAtDesc(approvalStage);
    }
    
    public List<VendorPdf> getDepartmentPendingPdfs() {
        System.out.println("DEBUG: Getting department pending PDFs");
        
        try {
            // Try to get PDFs by approval stage
            List<VendorPdf> pdfs = vendorPdfRepository.findByApprovalStageOrderByUploadedAtDesc("DEPARTMENT");
            System.out.println("DEBUG: Found " + pdfs.size() + " department pending PDFs by approval stage");
            for (VendorPdf pdf : pdfs) {
                System.out.println("DEBUG: PDF ID: " + pdf.getId() + ", Stage: " + pdf.getApprovalStage() + ", File: " + pdf.getOriginalFileName());
            }
            return pdfs;
        } catch (Exception e) {
            System.out.println("DEBUG: Error getting PDFs by approval stage, falling back to unprocessed: " + e.getMessage());
            // Fallback: get unprocessed PDFs if approval stage field doesn't exist
            List<VendorPdf> pdfs = vendorPdfRepository.findByIsProcessedFalseOrderByUploadedAtDesc();
            System.out.println("DEBUG: Found " + pdfs.size() + " unprocessed PDFs as fallback");
            return pdfs;
        }
    }
    
    public List<VendorPdf> getItPendingPdfs() {
        return vendorPdfRepository.findByApprovalStageOrderByUploadedAtDesc("IT");
    }

    public List<VendorPdf> getAllItPdfs() {
        System.out.println("DEBUG: Getting all IT PDFs");

        try {
            // Get all PDFs that have been through IT approval (IT, FINANCE, APPROVED)
            List<VendorPdf> allPdfs = vendorPdfRepository.findAllByOrderByUploadedAtDesc();
            System.out.println("DEBUG: Found " + allPdfs.size() + " total PDFs");

            // Filter PDFs that are relevant to IT managers
            List<VendorPdf> itPdfs = allPdfs.stream()
                .filter(pdf -> {
                    String stage = pdf.getApprovalStage();
                    return "IT".equals(stage) ||
                           "FINANCE".equals(stage) ||
                           "APPROVED".equals(stage);
                })
                .collect(java.util.stream.Collectors.toList());

            System.out.println("DEBUG: Filtered to " + itPdfs.size() + " IT-relevant PDFs");
            for (VendorPdf pdf : itPdfs) {
                System.out.println("DEBUG: PDF ID: " + pdf.getId() + ", Stage: " + pdf.getApprovalStage() + ", File: " + pdf.getOriginalFileName());
            }

            return itPdfs;
        } catch (Exception e) {
            System.out.println("DEBUG: Error getting all IT PDFs, falling back to IT pending: " + e.getMessage());
            // Fallback: get only IT pending PDFs
            return getItPendingPdfs();
        }
    }
    
    public List<VendorPdf> getFinancePendingPdfs() {
        return vendorPdfRepository.findByApprovalStageOrderByUploadedAtDesc("FINANCE");
    }
    
    public List<VendorPdf> getAllDepartmentPdfs() {
        System.out.println("DEBUG: Getting all department PDFs");
        
        try {
            // Get all PDFs that have been through department approval (DEPARTMENT, IT, FINANCE, APPROVED)
            List<VendorPdf> allPdfs = vendorPdfRepository.findAllByOrderByUploadedAtDesc();
            System.out.println("DEBUG: Found " + allPdfs.size() + " total PDFs");
            
            // Filter PDFs that are relevant to department managers
            List<VendorPdf> departmentPdfs = allPdfs.stream()
                .filter(pdf -> {
                    String stage = pdf.getApprovalStage();
                    return "DEPARTMENT".equals(stage) || 
                           "IT".equals(stage) || 
                           "FINANCE".equals(stage) || 
                           "APPROVED".equals(stage);
                })
                .collect(java.util.stream.Collectors.toList());
            
            System.out.println("DEBUG: Filtered to " + departmentPdfs.size() + " department-relevant PDFs");
            for (VendorPdf pdf : departmentPdfs) {
                System.out.println("DEBUG: PDF ID: " + pdf.getId() + ", Stage: " + pdf.getApprovalStage() + ", File: " + pdf.getOriginalFileName());
            }
            
            return departmentPdfs;
        } catch (Exception e) {
            System.out.println("DEBUG: Error getting all department PDFs, falling back to department pending: " + e.getMessage());
            // Fallback: get only department pending PDFs
            return getDepartmentPendingPdfs();
        }
    }
    
    public VendorPdf markAsProcessed(Long pdfId) {
        VendorPdf pdf = vendorPdfRepository.findById(pdfId).orElseThrow();
        pdf.setProcessed(true);
        pdf.setRejected(false); // Clear rejection status if it was rejected
        pdf.setRejectionReason(null);
        
        // Notify the employee who uploaded the PDF about approval
        notifyPdfApproval(pdf);
        
        // Notify finance team about IT-approved PDF
        notifyFinanceTeam(pdf);
        
        return vendorPdfRepository.save(pdf);
    }
    
    public VendorPdf departmentApprovePdf(Long pdfId) {
        System.out.println("DEBUG: departmentApprovePdf called with pdfId: " + pdfId);
        
        try {
            VendorPdf pdf = vendorPdfRepository.findById(pdfId).orElseThrow();
            System.out.println("DEBUG: Found PDF: " + pdf.getOriginalFileName() + ", current stage: " + pdf.getApprovalStage());
            
            // Check if approval stage field exists, if not, use processed flag as fallback
            try {
                pdf.setApprovalStage("IT");
                System.out.println("DEBUG: Set approval stage to IT");
            } catch (Exception e) {
                System.out.println("DEBUG: Error setting approval stage, using processed flag: " + e.getMessage());
                // Fallback: use processed flag if approval stage doesn't exist
                pdf.setProcessed(false); // Keep it unprocessed for IT Manager
            }
            
            pdf.setRejected(false);
            pdf.setRejectionReason(null);
            
            System.out.println("DEBUG: Updated PDF stage to: " + pdf.getApprovalStage());
            
            // Notify IT Manager about department-approved PDF
            notifyITTeam(pdf);
            
            // Notify the employee who uploaded the PDF about department approval
            notifyDepartmentPdfApproval(pdf);
            
            VendorPdf savedPdf = vendorPdfRepository.save(pdf);
            System.out.println("DEBUG: PDF saved successfully, final stage: " + savedPdf.getApprovalStage());
            return savedPdf;
            
        } catch (Exception e) {
            System.out.println("DEBUG: Exception in departmentApprovePdf: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to approve PDF: " + e.getMessage());
        }
    }
    
    public VendorPdf itApprovePdf(Long pdfId) {
        VendorPdf pdf = vendorPdfRepository.findById(pdfId).orElseThrow();
        pdf.setApprovalStage("FINANCE");
        pdf.setRejected(false);
        pdf.setRejectionReason(null);
        
        // Notify Finance Manager about IT-approved PDF
        notifyFinanceTeam(pdf);
        
        // Notify the employee who uploaded the PDF about IT approval
        notifyItPdfApproval(pdf);
        
        return vendorPdfRepository.save(pdf);
    }
    
    public VendorPdf financeApprovePdf(Long pdfId) {
        VendorPdf pdf = vendorPdfRepository.findById(pdfId).orElseThrow();
        pdf.setApprovalStage("APPROVED");
        pdf.setProcessed(true);
        pdf.setRejected(false);
        pdf.setRejectionReason(null);
        
        // Notify the employee who uploaded the PDF about final approval
        notifyFinalPdfApproval(pdf);
        
        return vendorPdfRepository.save(pdf);
    }
    
    public VendorPdf rejectPdf(Long pdfId, String rejectionReason) {
        VendorPdf pdf = vendorPdfRepository.findById(pdfId).orElseThrow();
        pdf.setRejected(true);
        pdf.setProcessed(false); // Clear processed status if it was processed
        pdf.setRejectionReason(rejectionReason);
        
        // Notify the IT Manager about the rejection
        notifyPdfRejection(pdf);
        
        return vendorPdfRepository.save(pdf);
    }

    public VendorPdf getPdfById(Long pdfId) {
        return vendorPdfRepository.findById(pdfId).orElse(null);
    }

    public boolean deletePdf(Long pdfId) {
        try {
            vendorPdfRepository.deleteById(pdfId);
            return true;
        } catch (Exception e) {
            System.err.println("Error deleting PDF: " + e.getMessage());
            return false;
        }
    }
    
    private void notifyPdfRejection(VendorPdf vendorPdf) {
        Notification notification = new Notification();
        notification.setUserId(vendorPdf.getUploadedBy()); // Notify the employee who uploaded it
        
        String message = "Your PDF '" + vendorPdf.getOriginalFileName() + "' has been rejected by IT Manager.";
        if (vendorPdf.getRejectionReason() != null && !vendorPdf.getRejectionReason().trim().isEmpty()) {
            message += " Reason: " + vendorPdf.getRejectionReason();
        }
        
        // Add requisition details if linked
        if (vendorPdf.getRequisitionId() != null) {
            Requisition requisition = requisitionRepository.findById(vendorPdf.getRequisitionId()).orElse(null);
            if (requisition != null) {
                message += " for Requisition #" + requisition.getId();
            }
        }
        
        notification.setMessage(message);
        notification.setRead(false);
        Notification savedNotification = notificationRepository.save(notification);
        System.out.println("DEBUG: Created PDF rejection notification for user: " + vendorPdf.getUploadedBy() + 
                          ", Message: " + message + ", Notification ID: " + savedNotification.getNotificationId());
    }
    
    private void notifyPdfApproval(VendorPdf vendorPdf) {
        Notification notification = new Notification();
        notification.setUserId(vendorPdf.getUploadedBy()); // Notify the employee who uploaded it
        
        String message = "Your PDF '" + vendorPdf.getOriginalFileName() + "' has been approved by IT Manager.";
        
        // Add requisition details if linked
        if (vendorPdf.getRequisitionId() != null) {
            Requisition requisition = requisitionRepository.findById(vendorPdf.getRequisitionId()).orElse(null);
            if (requisition != null) {
                message += " for Requisition #" + requisition.getId();
            }
        }
        
        notification.setMessage(message);
        notification.setRead(false);
        Notification savedNotification = notificationRepository.save(notification);
        System.out.println("DEBUG: Created PDF approval notification for user: " + vendorPdf.getUploadedBy() + 
                          ", Message: " + message + ", Notification ID: " + savedNotification.getNotificationId());
    }
    
    private void notifyDepartmentPdfApproval(VendorPdf vendorPdf) {
        Notification notification = new Notification();
        notification.setUserId(vendorPdf.getUploadedBy()); // Notify the employee who uploaded it
        
        String message = "Your PDF '" + vendorPdf.getOriginalFileName() + "' has been approved by Department Manager and sent to IT Manager.";
        
        // Add requisition details if linked
        if (vendorPdf.getRequisitionId() != null) {
            Requisition requisition = requisitionRepository.findById(vendorPdf.getRequisitionId()).orElse(null);
            if (requisition != null) {
                message += " for Requisition #" + requisition.getId();
            }
        }
        
        notification.setMessage(message);
        notification.setRead(false);
        Notification savedNotification = notificationRepository.save(notification);
        System.out.println("DEBUG: Created Department PDF approval notification for user: " + vendorPdf.getUploadedBy() + 
                          ", Message: " + message + ", Notification ID: " + savedNotification.getNotificationId());
    }
    
    private void notifyItPdfApproval(VendorPdf vendorPdf) {
        Notification notification = new Notification();
        notification.setUserId(vendorPdf.getUploadedBy()); // Notify the employee who uploaded it
        
        String message = "Your PDF '" + vendorPdf.getOriginalFileName() + "' has been approved by IT Manager and sent to Finance Manager.";
        
        // Add requisition details if linked
        if (vendorPdf.getRequisitionId() != null) {
            Requisition requisition = requisitionRepository.findById(vendorPdf.getRequisitionId()).orElse(null);
            if (requisition != null) {
                message += " for Requisition #" + requisition.getId();
            }
        }
        
        notification.setMessage(message);
        notification.setRead(false);
        Notification savedNotification = notificationRepository.save(notification);
        System.out.println("DEBUG: Created IT PDF approval notification for user: " + vendorPdf.getUploadedBy() + 
                          ", Message: " + message + ", Notification ID: " + savedNotification.getNotificationId());
    }
    
    private void notifyFinalPdfApproval(VendorPdf vendorPdf) {
        Notification notification = new Notification();
        notification.setUserId(vendorPdf.getUploadedBy()); // Notify the employee who uploaded it
        
        String message = "Your PDF '" + vendorPdf.getOriginalFileName() + "' has been fully approved by Finance Manager.";
        
        // Add requisition details if linked
        if (vendorPdf.getRequisitionId() != null) {
            Requisition requisition = requisitionRepository.findById(vendorPdf.getRequisitionId()).orElse(null);
            if (requisition != null) {
                message += " for Requisition #" + requisition.getId();
            }
        }
        
        notification.setMessage(message);
        notification.setRead(false);
        Notification savedNotification = notificationRepository.save(notification);
        System.out.println("DEBUG: Created Final PDF approval notification for user: " + vendorPdf.getUploadedBy() + 
                          ", Message: " + message + ", Notification ID: " + savedNotification.getNotificationId());
    }
    
    public byte[] getPdfContent(Long pdfId) throws IOException {
        VendorPdf pdf = vendorPdfRepository.findById(pdfId).orElseThrow();
        Path filePath = Paths.get(pdf.getFilePath());
        return Files.readAllBytes(filePath);
    }
}
