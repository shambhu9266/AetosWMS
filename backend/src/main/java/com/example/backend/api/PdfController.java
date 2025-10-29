package com.example.backend.api;

import com.example.backend.model.VendorPdf;
import com.example.backend.service.PdfService;
import com.example.backend.service.AuthService;
import com.example.backend.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pdf")
@CrossOrigin(origins = "http://localhost:4200")
public class PdfController {
    
    @Autowired
    private PdfService pdfService;
    
    @Autowired
    private AuthService authService;
    
    @PostMapping("/upload")
    public Map<String, Object> uploadPdf(@RequestParam String sessionId,
                                        @RequestParam("file") MultipartFile file,
                                        @RequestParam(required = false) String description,
                                        @RequestParam(required = false) Long requisitionId) {
        try {
        // Check if user is authenticated (allow all authenticated users to upload PDFs)
        User currentUser = authService.getCurrentUser(sessionId);
        if (currentUser == null) {
            return Map.of("success", false, "message", "Access denied. Please log in to upload PDFs.");
        }
            
            // Validate file type
            if (!file.getContentType().equals("application/pdf")) {
                return Map.of("success", false, "message", "Only PDF files are allowed.");
            }
            
            // Validate file size (max 10MB)
            if (file.getSize() > 10 * 1024 * 1024) {
                return Map.of("success", false, "message", "File size must be less than 10MB.");
            }
            
            VendorPdf uploadedPdf = pdfService.uploadPdf(file, currentUser.getUsername(), description, requisitionId, currentUser.getDepartment());
            
            return Map.of("success", true, "message", "PDF uploaded successfully", "pdf", uploadedPdf);
            
        } catch (IOException e) {
            return Map.of("success", false, "message", "Failed to upload PDF: " + e.getMessage());
        } catch (Exception e) {
            return Map.of("success", false, "message", "An error occurred: " + e.getMessage());
        }
    }
    
    @GetMapping("/list")
    public Map<String, Object> getAllPdfs(@RequestParam String sessionId) {
        try {
            User currentUser = authService.getCurrentUser(sessionId);
            if (currentUser == null) {
                return Map.of("success", false, "message", "Invalid session");
            }
            
            List<VendorPdf> pdfs;
            if ("SUPERADMIN".equals(currentUser.getRole().name())) {
                // Super Admins see all PDFs
                pdfs = pdfService.getAllPdfs();
            } else if ("DEPARTMENT_MANAGER".equals(currentUser.getRole().name())) {
                // Department Managers see only PDFs from their department (following approval workflow)
                pdfs = pdfService.getPdfsByDepartment(currentUser.getDepartment());
            } else if ("IT_MANAGER".equals(currentUser.getRole().name())) {
                // IT Managers see PDFs that have been approved by department managers
                pdfs = pdfService.getAllItPdfs();
            } else if ("FINANCE_MANAGER".equals(currentUser.getRole().name())) {
                // Finance Managers see PDFs that have been approved by IT managers
                pdfs = pdfService.getAllFinancePdfs();
            } else {
                // All other users (employees) see only their uploaded PDFs
                pdfs = pdfService.getPdfsByUploader(currentUser.getUsername());
            }
            
            return Map.of("success", true, "pdfs", pdfs);
            
        } catch (Exception e) {
            return Map.of("success", false, "message", "An error occurred: " + e.getMessage());
        }
    }
    
    @GetMapping("/download/{pdfId}")
    public ResponseEntity<Resource> downloadPdf(@PathVariable Long pdfId, @RequestParam String sessionId) {
        try {
            // Check if user has permission to download PDFs
            User currentUser = authService.getCurrentUser(sessionId);
            if (currentUser == null || 
                (!"IT_MANAGER".equals(currentUser.getRole().name()) && 
                 !"FINANCE_MANAGER".equals(currentUser.getRole().name()) &&
                 !"DEPARTMENT_MANAGER".equals(currentUser.getRole().name()) &&
                 !"SUPERADMIN".equals(currentUser.getRole().name()))) {
                return ResponseEntity.status(403).build();
            }
            
            byte[] pdfContent = pdfService.getPdfContent(pdfId);
            VendorPdf pdf = pdfService.getAllPdfs().stream()
                .filter(p -> p.getId().equals(pdfId))
                .findFirst()
                .orElseThrow();
            
            ByteArrayResource resource = new ByteArrayResource(pdfContent);
            
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + pdf.getOriginalFileName() + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdfContent.length)
                .body(resource);
                
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }
    
    @PostMapping("/mark-processed/{pdfId}")
    public Map<String, Object> markAsProcessed(@PathVariable Long pdfId, @RequestParam String sessionId) {
        try {
            // Finance Managers and IT Managers can mark PDFs as processed
            User currentUser = authService.getCurrentUser(sessionId);
            if (currentUser == null || 
                (!"FINANCE_MANAGER".equals(currentUser.getRole().name()) && 
                 !"IT_MANAGER".equals(currentUser.getRole().name()))) {
                return Map.of("success", false, "message", "Access denied. Only Finance Managers and IT Managers can mark PDFs as processed.");
            }
            
            VendorPdf processedPdf = pdfService.markAsProcessed(pdfId);
            return Map.of("success", true, "message", "PDF marked as processed", "pdf", processedPdf);
            
        } catch (Exception e) {
            return Map.of("success", false, "message", "An error occurred: " + e.getMessage());
        }
    }
    
    @PostMapping("/reject/{pdfId}")
    public Map<String, Object> rejectPdf(@PathVariable Long pdfId, 
                                        @RequestParam String sessionId,
                                        @RequestParam String rejectionReason) {
        try {
            // Finance Managers, IT Managers, and Department Managers can reject PDFs
            User currentUser = authService.getCurrentUser(sessionId);
            if (currentUser == null || 
                (!"FINANCE_MANAGER".equals(currentUser.getRole().name()) && 
                 !"IT_MANAGER".equals(currentUser.getRole().name()) &&
                 !"DEPARTMENT_MANAGER".equals(currentUser.getRole().name()))) {
                return Map.of("success", false, "message", "Access denied. Only Finance Managers, IT Managers, and Department Managers can reject PDFs.");
            }
            
            VendorPdf rejectedPdf = pdfService.rejectPdf(pdfId, rejectionReason);
            return Map.of("success", true, "message", "PDF rejected", "pdf", rejectedPdf);
            
        } catch (Exception e) {
            return Map.of("success", false, "message", "An error occurred: " + e.getMessage());
        }
    }
    
    @PostMapping("/department-approve/{pdfId}")
    public Map<String, Object> departmentApprovePdf(@PathVariable Long pdfId, 
                                                   @RequestParam String sessionId) {
        try {
            System.out.println("DEBUG: Department approve PDF called with pdfId: " + pdfId + ", sessionId: " + sessionId);
            
            // Only Department Managers can approve PDFs at department level
            User currentUser = authService.getCurrentUser(sessionId);
            System.out.println("DEBUG: Current user: " + (currentUser != null ? currentUser.getUsername() : "null"));
            System.out.println("DEBUG: User role: " + (currentUser != null ? currentUser.getRole().name() : "null"));
            
            if (currentUser == null) {
                System.out.println("DEBUG: User is null");
                return Map.of("success", false, "message", "Invalid session");
            }
            
            if (!"DEPARTMENT_MANAGER".equals(currentUser.getRole().name())) {
                System.out.println("DEBUG: User is not DEPARTMENT_MANAGER, role is: " + currentUser.getRole().name());
                return Map.of("success", false, "message", "Access denied. Only Department Managers can approve PDFs at department level.");
            }
            
            System.out.println("DEBUG: User is DEPARTMENT_MANAGER, proceeding with approval");
            VendorPdf approvedPdf = pdfService.departmentApprovePdf(pdfId);
            System.out.println("DEBUG: PDF approved successfully, new approval stage: " + approvedPdf.getApprovalStage());
            return Map.of("success", true, "message", "PDF approved by Department Manager", "pdf", approvedPdf);
            
        } catch (Exception e) {
            System.out.println("DEBUG: Exception in department approve: " + e.getMessage());
            e.printStackTrace();
            return Map.of("success", false, "message", "An error occurred: " + e.getMessage());
        }
    }
    
    @PostMapping("/it-approve/{pdfId}")
    public Map<String, Object> itApprovePdf(@PathVariable Long pdfId, 
                                           @RequestParam String sessionId) {
        try {
            // Only IT Managers can approve PDFs at IT level
            User currentUser = authService.getCurrentUser(sessionId);
            if (currentUser == null || !"IT_MANAGER".equals(currentUser.getRole().name())) {
                return Map.of("success", false, "message", "Access denied. Only IT Managers can approve PDFs at IT level.");
            }
            
            VendorPdf approvedPdf = pdfService.itApprovePdf(pdfId);
            return Map.of("success", true, "message", "PDF approved by IT Manager", "pdf", approvedPdf);
            
        } catch (Exception e) {
            return Map.of("success", false, "message", "An error occurred: " + e.getMessage());
        }
    }
    
    @PostMapping("/finance-approve/{pdfId}")
    public Map<String, Object> financeApprovePdf(@PathVariable Long pdfId, 
                                                @RequestParam String sessionId) {
        try {
            // Only Finance Managers can approve PDFs at finance level
            User currentUser = authService.getCurrentUser(sessionId);
            if (currentUser == null || !"FINANCE_MANAGER".equals(currentUser.getRole().name())) {
                return Map.of("success", false, "message", "Access denied. Only Finance Managers can approve PDFs at finance level.");
            }
            
            VendorPdf approvedPdf = pdfService.financeApprovePdf(pdfId);
            return Map.of("success", true, "message", "PDF approved by Finance Manager", "pdf", approvedPdf);
            
        } catch (Exception e) {
            return Map.of("success", false, "message", "An error occurred: " + e.getMessage());
        }
    }

    @DeleteMapping("/{pdfId}")
    public Map<String, Object> deletePdf(@PathVariable Long pdfId, 
                                        @RequestParam String sessionId) {
        try {
            User currentUser = authService.getCurrentUser(sessionId);
            if (currentUser == null) {
                return Map.of("success", false, "message", "Invalid session");
            }
            
            // Check if user can delete this PDF (only the uploader or Finance Manager)
            VendorPdf pdf = pdfService.getPdfById(pdfId);
            if (pdf == null) {
                return Map.of("success", false, "message", "PDF not found");
            }
            
            // Allow deletion if user is the uploader or is a Finance Manager, IT Manager, Department Manager, or Super Admin
            if (!pdf.getUploadedBy().equals(currentUser.getUsername()) && 
                !"FINANCE_MANAGER".equals(currentUser.getRole().name()) &&
                !"IT_MANAGER".equals(currentUser.getRole().name()) &&
                !"DEPARTMENT_MANAGER".equals(currentUser.getRole().name()) &&
                !"SUPERADMIN".equals(currentUser.getRole().name())) {
                return Map.of("success", false, "message", "Access denied. You can only delete your own PDFs.");
            }
            
            boolean deleted = pdfService.deletePdf(pdfId);
            if (deleted) {
                return Map.of("success", true, "message", "PDF deleted successfully");
            } else {
                return Map.of("success", false, "message", "Failed to delete PDF");
            }
            
        } catch (Exception e) {
            return Map.of("success", false, "message", "An error occurred: " + e.getMessage());
        }
    }
    
    @GetMapping("/department-pending")
    public Map<String, Object> getDepartmentPendingPdfs(@RequestParam String sessionId) {
        try {
            User currentUser = authService.getCurrentUser(sessionId);
            if (currentUser == null || !"DEPARTMENT_MANAGER".equals(currentUser.getRole().name())) {
                return Map.of("success", false, "message", "Access denied. Only Department Managers can access department pending PDFs.");
            }
            
            // Department managers can only see PDFs from requisitions in their department
            List<VendorPdf> pdfs = pdfService.getDepartmentPendingPdfsByDepartment(currentUser.getDepartment());
            return Map.of("success", true, "pdfs", pdfs);
            
        } catch (Exception e) {
            return Map.of("success", false, "message", "An error occurred: " + e.getMessage());
        }
    }

    @GetMapping("/department-all")
    public Map<String, Object> getAllDepartmentPdfs(@RequestParam String sessionId) {
        try {
            User currentUser = authService.getCurrentUser(sessionId);
            if (currentUser == null || !"DEPARTMENT_MANAGER".equals(currentUser.getRole().name())) {
                return Map.of("success", false, "message", "Access denied. Only Department Managers can access department PDFs.");
            }
            
            List<VendorPdf> pdfs = pdfService.getAllDepartmentPdfsByDepartment(currentUser.getDepartment());
            return Map.of("success", true, "pdfs", pdfs);
            
        } catch (Exception e) {
            return Map.of("success", false, "message", "An error occurred: " + e.getMessage());
        }
    }
    
    @GetMapping("/it-pending")
    public Map<String, Object> getItPendingPdfs(@RequestParam String sessionId) {
        try {
            User currentUser = authService.getCurrentUser(sessionId);
            if (currentUser == null || !"IT_MANAGER".equals(currentUser.getRole().name())) {
                return Map.of("success", false, "message", "Access denied. Only IT Managers can access IT pending PDFs.");
            }

            // IT Managers see PDFs that have been approved by department managers
            List<VendorPdf> pdfs = pdfService.getItPendingPdfs();
            return Map.of("success", true, "pdfs", pdfs);

        } catch (Exception e) {
            return Map.of("success", false, "message", "An error occurred: " + e.getMessage());
        }
    }

    @GetMapping("/it-all")
    public Map<String, Object> getAllItPdfs(@RequestParam String sessionId) {
        try {
            User currentUser = authService.getCurrentUser(sessionId);
            if (currentUser == null || !"IT_MANAGER".equals(currentUser.getRole().name())) {
                return Map.of("success", false, "message", "Access denied. Only IT Managers can access IT PDFs.");
            }

            List<VendorPdf> pdfs = pdfService.getAllItPdfs();
            return Map.of("success", true, "pdfs", pdfs);

        } catch (Exception e) {
            return Map.of("success", false, "message", "An error occurred: " + e.getMessage());
        }
    }
    
    @GetMapping("/finance-pending")
    public Map<String, Object> getFinancePendingPdfs(@RequestParam String sessionId) {
        try {
            User currentUser = authService.getCurrentUser(sessionId);
            if (currentUser == null || !"FINANCE_MANAGER".equals(currentUser.getRole().name())) {
                return Map.of("success", false, "message", "Access denied. Only Finance Managers can access Finance pending PDFs.");
            }
            
            // Finance Managers see PDFs that have been approved by IT managers
            List<VendorPdf> pdfs = pdfService.getFinancePendingPdfs();
            return Map.of("success", true, "pdfs", pdfs);
            
        } catch (Exception e) {
            return Map.of("success", false, "message", "An error occurred: " + e.getMessage());
        }
    }
}
