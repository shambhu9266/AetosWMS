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
        
        VendorPdf savedPdf = vendorPdfRepository.save(vendorPdf);
        
        // Notify IT team (employees upload PDFs to IT team for approval)
        notifyITTeam(savedPdf);
        
        return savedPdf;
    }
    
    private void notifyITTeam(VendorPdf vendorPdf) {
        Notification notification = new Notification();
        notification.setUserId("itmanager"); // IT Manager username
        
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
    
    public byte[] getPdfContent(Long pdfId) throws IOException {
        VendorPdf pdf = vendorPdfRepository.findById(pdfId).orElseThrow();
        Path filePath = Paths.get(pdf.getFilePath());
        return Files.readAllBytes(filePath);
    }
}
