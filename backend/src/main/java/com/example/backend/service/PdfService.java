package com.example.backend.service;

import com.example.backend.model.Notification;
import com.example.backend.model.VendorPdf;
import com.example.backend.model.Requisition;
import com.example.backend.repo.NotificationRepository;
import com.example.backend.repo.VendorPdfRepository;
import com.example.backend.repo.RequisitionRepository;
import com.example.backend.repo.DepartmentRepository;
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
    
    @Autowired
    private DepartmentRepository departmentRepository;
    
    private static final String UPLOAD_DIR = "uploads/pdfs/";
    
    public VendorPdf uploadPdf(MultipartFile file, String uploadedBy, String description, Long requisitionId, String department) throws IOException {
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
        
        // Set department based on requisition's department, not employee's department
        String pdfDepartment = department;
        if (requisitionId != null) {
            Requisition requisition = requisitionRepository.findById(requisitionId).orElse(null);
            if (requisition != null) {
                pdfDepartment = requisition.getDepartment();
                System.out.println("DEBUG: PDF department set to requisition department: " + pdfDepartment);
            }
        }
        vendorPdf.setDepartment(pdfDepartment);
        
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
            System.out.println("DEBUG: PDF linked to requisition ID: " + vendorPdf.getRequisitionId());
            Requisition requisition = requisitionRepository.findById(vendorPdf.getRequisitionId()).orElse(null);
            if (requisition != null) {
                System.out.println("DEBUG: Found requisition with department: " + requisition.getDepartment());
                // Use the department from the requisition (not the employee's department)
                return getDepartmentManager(requisition.getDepartment());
            } else {
                System.out.println("DEBUG: Requisition not found for ID: " + vendorPdf.getRequisitionId());
            }
        } else {
            System.out.println("DEBUG: PDF not linked to any requisition");
        }
        
        // Default to sales manager if no requisition linked
        System.out.println("DEBUG: Using default sales manager for PDF notification");
        return "salesmanager";
    }
    
    private String getDepartmentManager(String department) {
        try {
            // Query the departments table to get the actual manager for this department
            System.out.println("DEBUG: Looking up manager for department: " + department);
            
            return departmentRepository.findByName(department)
                .map(dept -> {
                    String managerUsername = dept.getManagerUsername();
                    if (managerUsername != null && !managerUsername.trim().isEmpty()) {
                        System.out.println("DEBUG: Found manager '" + managerUsername + "' for department '" + department + "'");
                        return managerUsername;
                    } else {
                        System.out.println("DEBUG: No manager assigned for department '" + department + "', using fallback");
                        return "salesmanager"; // Fallback for departments without managers
                    }
                })
                .orElseGet(() -> {
                    System.out.println("DEBUG: Department '" + department + "' not found in database, using fallback");
                    return "salesmanager"; // Fallback for unknown departments
                });
        } catch (Exception e) {
            System.err.println("DEBUG: Error getting department manager for '" + department + "': " + e.getMessage());
            e.printStackTrace();
            return "salesmanager"; // Safe fallback
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
    
    public List<VendorPdf> getPdfsByDepartment(String department) {
        return vendorPdfRepository.findByDepartmentOrderByUploadedAtDesc(department);
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
    
    public List<VendorPdf> getAllFinancePdfs() {
        System.out.println("DEBUG: Getting all Finance PDFs");
        
        try {
            // Get all PDFs that have been through Finance approval (FINANCE, APPROVED)
            List<VendorPdf> allPdfs = vendorPdfRepository.findAllByOrderByUploadedAtDesc();
            System.out.println("DEBUG: Found " + allPdfs.size() + " total PDFs");
            
            // Filter PDFs that are relevant to Finance managers
            List<VendorPdf> financePdfs = allPdfs.stream()
                .filter(pdf -> {
                    String stage = pdf.getApprovalStage();
                    return "FINANCE".equals(stage) || 
                           "APPROVED".equals(stage);
                })
                .collect(java.util.stream.Collectors.toList());
            
            System.out.println("DEBUG: Filtered to " + financePdfs.size() + " Finance-relevant PDFs");
            for (VendorPdf pdf : financePdfs) {
                System.out.println("DEBUG: PDF ID: " + pdf.getId() + ", Stage: " + pdf.getApprovalStage() + ", File: " + pdf.getOriginalFileName());
            }
            
            return financePdfs;
        } catch (Exception e) {
            System.out.println("DEBUG: Error getting all Finance PDFs, falling back to Finance pending: " + e.getMessage());
            // Fallback: get only Finance pending PDFs
            return getFinancePendingPdfs();
        }
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
    
    public List<VendorPdf> getDepartmentPendingPdfsByDepartment(String department) {
        System.out.println("DEBUG: Getting department pending PDFs for department: " + department);
        
        try {
            // Get PDFs by department and approval stage
            List<VendorPdf> pdfs = vendorPdfRepository.findByDepartmentAndApprovalStageOrderByUploadedAtDesc(department, "DEPARTMENT");
            System.out.println("DEBUG: Found " + pdfs.size() + " department pending PDFs for department: " + department);
            
            // Also get PDFs that are linked to requisitions from this department
            List<VendorPdf> requisitionLinkedPdfs = getPdfsLinkedToRequisitionsInDepartment(department, "DEPARTMENT");
            System.out.println("DEBUG: Found " + requisitionLinkedPdfs.size() + " requisition-linked PDFs for department: " + department);
            
            // Combine both lists and remove duplicates
            java.util.Set<Long> existingIds = pdfs.stream().map(VendorPdf::getId).collect(java.util.stream.Collectors.toSet());
            List<VendorPdf> additionalPdfs = requisitionLinkedPdfs.stream()
                .filter(pdf -> !existingIds.contains(pdf.getId()))
                .collect(java.util.stream.Collectors.toList());
            
            pdfs.addAll(additionalPdfs);
            System.out.println("DEBUG: Total department pending PDFs: " + pdfs.size());
            
            return pdfs;
        } catch (Exception e) {
            System.out.println("DEBUG: Error getting department pending PDFs by department, falling back to all department pending: " + e.getMessage());
            // Fallback: get all department pending PDFs
            return getDepartmentPendingPdfs();
        }
    }
    
    public List<VendorPdf> getAllDepartmentPdfsByDepartment(String department) {
        System.out.println("DEBUG: Getting all department PDFs for department: " + department);
        
        try {
            // Get all PDFs for the department that are relevant to department managers
            List<VendorPdf> allPdfs = vendorPdfRepository.findByDepartmentOrderByUploadedAtDesc(department);
            System.out.println("DEBUG: Found " + allPdfs.size() + " total PDFs for department: " + department);
            
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
            
            // Also get PDFs that are linked to requisitions from this department
            List<VendorPdf> requisitionLinkedPdfs = getPdfsLinkedToRequisitionsInDepartment(department, "DEPARTMENT");
            requisitionLinkedPdfs.addAll(getPdfsLinkedToRequisitionsInDepartment(department, "IT"));
            requisitionLinkedPdfs.addAll(getPdfsLinkedToRequisitionsInDepartment(department, "FINANCE"));
            requisitionLinkedPdfs.addAll(getPdfsLinkedToRequisitionsInDepartment(department, "APPROVED"));
            
            System.out.println("DEBUG: Found " + requisitionLinkedPdfs.size() + " requisition-linked PDFs for department: " + department);
            
            // Combine both lists and remove duplicates
            java.util.Set<Long> existingIds = departmentPdfs.stream().map(VendorPdf::getId).collect(java.util.stream.Collectors.toSet());
            List<VendorPdf> additionalPdfs = requisitionLinkedPdfs.stream()
                .filter(pdf -> !existingIds.contains(pdf.getId()))
                .collect(java.util.stream.Collectors.toList());
            
            departmentPdfs.addAll(additionalPdfs);
            
            System.out.println("DEBUG: Total department-relevant PDFs: " + departmentPdfs.size());
            return departmentPdfs;
        } catch (Exception e) {
            System.out.println("DEBUG: Error getting all department PDFs by department, falling back to all department PDFs: " + e.getMessage());
            // Fallback: get all department PDFs
            return getAllDepartmentPdfs();
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
    
    private List<VendorPdf> getPdfsLinkedToRequisitionsInDepartment(String department, String approvalStage) {
        try {
            // Get all PDFs that are linked to requisitions from the specified department
            List<VendorPdf> allPdfs = vendorPdfRepository.findAllByOrderByUploadedAtDesc();
            
            return allPdfs.stream()
                .filter(pdf -> pdf.getRequisitionId() != null && 
                              approvalStage.equals(pdf.getApprovalStage()))
                .filter(pdf -> {
                    Requisition requisition = requisitionRepository.findById(pdf.getRequisitionId()).orElse(null);
                    return requisition != null && department.equals(requisition.getDepartment());
                })
                .collect(java.util.stream.Collectors.toList());
        } catch (Exception e) {
            System.out.println("DEBUG: Error getting PDFs linked to requisitions in department: " + e.getMessage());
            return new java.util.ArrayList<>();
        }
    }
}
