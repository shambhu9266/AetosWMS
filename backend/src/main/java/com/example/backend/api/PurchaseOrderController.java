package com.example.backend.api;

import com.example.backend.model.PurchaseOrder;
import com.example.backend.model.POStatus;
import com.example.backend.model.User;
import com.example.backend.service.PurchaseOrderService;
import com.example.backend.service.AuthService;
import com.example.backend.service.PurchaseOrderPdfService;
import com.example.backend.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/po")
@CrossOrigin(origins = "http://localhost:4200")
public class PurchaseOrderController {
    
    @Autowired
    private PurchaseOrderService purchaseOrderService;
    
    @Autowired
    private AuthService authService;
    
    @Autowired
    private PurchaseOrderPdfService pdfService;
    
    @Autowired
    private EmailService emailService;
    
    @PostMapping("/create")
    public Map<String, Object> createPurchaseOrder(@RequestParam String sessionId,
                                                  @RequestParam String billToCompany,
                                                  @RequestParam String billToAddress,
                                                  @RequestParam(required = false) String billToPAN,
                                                  @RequestParam(required = false) String billToGSTIN,
                                                  @RequestParam String vendorName,
                                                  @RequestParam String vendorAddress,
                                                  @RequestParam String vendorContactPerson,
                                                  @RequestParam String vendorMobileNo,
                                                  @RequestParam String shipToAddress,
                                                  @RequestParam String scopeOfOrder,
                                                  @RequestParam String shippingMethod,
                                                  @RequestParam String shippingTerms,
                                                  @RequestParam String dateOfCompletion,
                                                  @RequestParam String lineItemsJson,
                                                  @RequestParam BigDecimal subtotalAmount,
                                                  @RequestParam BigDecimal freightCharges,
                                                  @RequestParam BigDecimal gstRate,
                                                  @RequestParam BigDecimal gstAmount,
                                                  @RequestParam BigDecimal totalAmount,
                                                  @RequestParam(required = false) String termsAndConditions,
                                                  @RequestParam(required = false) String paymentTerms,
                                                  @RequestParam(required = false) String warranty,
                                                  @RequestParam String department) {
        try {
            // Check if user has FINANCE_MANAGER permission
            User currentUser = authService.getCurrentUser(sessionId);
            System.out.println("DEBUG: PO Create - SessionId: " + sessionId);
            System.out.println("DEBUG: PO Create - CurrentUser: " + (currentUser != null ? currentUser.getUsername() : "null"));
            System.out.println("DEBUG: PO Create - User Role: " + (currentUser != null ? currentUser.getRole().name() : "null"));
            
            if (currentUser == null || !"FINANCE_MANAGER".equals(currentUser.getRole().name())) {
                System.out.println("DEBUG: PO Create - Access denied for user: " + (currentUser != null ? currentUser.getUsername() : "null"));
                return Map.of("success", false, "message", "Access denied. Only Finance Managers can create Purchase Orders.");
            }
            
            PurchaseOrder po = purchaseOrderService.createPurchaseOrder(
                billToCompany, billToAddress, billToPAN, billToGSTIN,
                vendorName, vendorAddress, vendorContactPerson, vendorMobileNo,
                shipToAddress, scopeOfOrder, shippingMethod, shippingTerms,
                dateOfCompletion, lineItemsJson, subtotalAmount, freightCharges,
                gstRate, gstAmount, totalAmount, termsAndConditions,
                paymentTerms, warranty, currentUser.getUsername(), department
            );
            
            return Map.of("success", true, "message", "Purchase Order created successfully", "po", po);
            
        } catch (Exception e) {
            return Map.of("success", false, "message", "Failed to create Purchase Order: " + e.getMessage());
        }
    }
    
    @GetMapping("/list")
    public Map<String, Object> getAllPurchaseOrders(@RequestParam String sessionId) {
        try {
            User currentUser = authService.getCurrentUser(sessionId);
            if (currentUser == null) {
                return Map.of("success", false, "message", "Invalid session");
            }
            
            List<PurchaseOrder> pos;
            if ("FINANCE_MANAGER".equals(currentUser.getRole().name())) {
                // Finance Managers see all POs
                pos = purchaseOrderService.getAllPurchaseOrders();
            } else {
                // Other users see only POs from their department
                pos = purchaseOrderService.getPurchaseOrdersByDepartment(currentUser.getDepartment());
            }
            
            return Map.of("success", true, "pos", pos);
            
        } catch (Exception e) {
            return Map.of("success", false, "message", "An error occurred: " + e.getMessage());
        }
    }
    
    @GetMapping("/{id}")
    public Map<String, Object> getPurchaseOrderById(@PathVariable Long id, @RequestParam String sessionId) {
        try {
            User currentUser = authService.getCurrentUser(sessionId);
            if (currentUser == null) {
                return Map.of("success", false, "message", "Invalid session");
            }
            
            Optional<PurchaseOrder> po = purchaseOrderService.getPurchaseOrderById(id);
            if (po.isPresent()) {
                return Map.of("success", true, "po", po.get());
            } else {
                return Map.of("success", false, "message", "Purchase Order not found");
            }
            
        } catch (Exception e) {
            return Map.of("success", false, "message", "An error occurred: " + e.getMessage());
        }
    }
    
    @PutMapping("/{id}")
    public Map<String, Object> updatePurchaseOrder(@PathVariable Long id,
                                                  @RequestParam String sessionId,
                                                  @RequestParam String billToCompany,
                                                  @RequestParam String billToAddress,
                                                  @RequestParam(required = false) String billToPAN,
                                                  @RequestParam(required = false) String billToGSTIN,
                                                  @RequestParam String vendorName,
                                                  @RequestParam String vendorAddress,
                                                  @RequestParam String vendorContactPerson,
                                                  @RequestParam String vendorMobileNo,
                                                  @RequestParam String shipToAddress,
                                                  @RequestParam String scopeOfOrder,
                                                  @RequestParam String shippingMethod,
                                                  @RequestParam String shippingTerms,
                                                  @RequestParam String dateOfCompletion,
                                                  @RequestParam String lineItemsJson,
                                                  @RequestParam BigDecimal subtotalAmount,
                                                  @RequestParam BigDecimal freightCharges,
                                                  @RequestParam BigDecimal gstRate,
                                                  @RequestParam BigDecimal gstAmount,
                                                  @RequestParam BigDecimal totalAmount,
                                                  @RequestParam(required = false) String termsAndConditions,
                                                  @RequestParam(required = false) String paymentTerms,
                                                  @RequestParam(required = false) String warranty) {
        try {
            // Check if user has FINANCE_MANAGER permission
            User currentUser = authService.getCurrentUser(sessionId);
            if (currentUser == null || !"FINANCE_MANAGER".equals(currentUser.getRole().name())) {
                return Map.of("success", false, "message", "Access denied. Only Finance Managers can update Purchase Orders.");
            }
            
            PurchaseOrder po = purchaseOrderService.updatePurchaseOrder(
                id, billToCompany, billToAddress, billToPAN, billToGSTIN,
                vendorName, vendorAddress, vendorContactPerson, vendorMobileNo,
                shipToAddress, scopeOfOrder, shippingMethod, shippingTerms,
                dateOfCompletion, lineItemsJson, subtotalAmount, freightCharges,
                gstRate, gstAmount, totalAmount, termsAndConditions,
                paymentTerms, warranty
            );
            
            return Map.of("success", true, "message", "Purchase Order updated successfully", "po", po);
            
        } catch (Exception e) {
            return Map.of("success", false, "message", "Failed to update Purchase Order: " + e.getMessage());
        }
    }
    
    @PutMapping("/{id}/status")
    public Map<String, Object> updatePOStatus(@PathVariable Long id,
                                             @RequestParam String sessionId,
                                             @RequestParam String status) {
        try {
            // Check if user has FINANCE_MANAGER permission
            User currentUser = authService.getCurrentUser(sessionId);
            if (currentUser == null || !"FINANCE_MANAGER".equals(currentUser.getRole().name())) {
                return Map.of("success", false, "message", "Access denied. Only Finance Managers can update PO status.");
            }
            
            POStatus poStatus = POStatus.valueOf(status.toUpperCase());
            PurchaseOrder po = purchaseOrderService.updatePOStatus(id, poStatus);
            
            return Map.of("success", true, "message", "PO status updated successfully", "po", po);
            
        } catch (Exception e) {
            return Map.of("success", false, "message", "Failed to update PO status: " + e.getMessage());
        }
    }
    
    @DeleteMapping("/{id}")
    public Map<String, Object> deletePurchaseOrder(@PathVariable Long id, @RequestParam String sessionId) {
        try {
            // Check if user has FINANCE_MANAGER permission
            User currentUser = authService.getCurrentUser(sessionId);
            if (currentUser == null || !"FINANCE_MANAGER".equals(currentUser.getRole().name())) {
                return Map.of("success", false, "message", "Access denied. Only Finance Managers can delete Purchase Orders.");
            }
            
            purchaseOrderService.deletePurchaseOrder(id);
            return Map.of("success", true, "message", "Purchase Order deleted successfully");
            
        } catch (Exception e) {
            return Map.of("success", false, "message", "Failed to delete Purchase Order: " + e.getMessage());
        }
    }
    
    @GetMapping("/{id}/pdf")
    public ResponseEntity<ByteArrayResource> downloadPurchaseOrderPdf(@PathVariable Long id, @RequestParam String sessionId) {
        try {
            // Check if user has permission (FINANCE_MANAGER or SUPERADMIN)
            User currentUser = authService.getCurrentUser(sessionId);
            if (currentUser == null || (!"FINANCE_MANAGER".equals(currentUser.getRole().name()) && !"SUPERADMIN".equals(currentUser.getRole().name()))) {
                return ResponseEntity.status(403).build();
            }
            
            Optional<PurchaseOrder> poOpt = purchaseOrderService.getPurchaseOrderById(id);
            if (poOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            PurchaseOrder po = poOpt.get();
            byte[] pdfBytes = pdfService.generatePurchaseOrderPdf(po);
            
            ByteArrayResource resource = new ByteArrayResource(pdfBytes);
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"PO_" + po.getPoNumber() + ".pdf\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .contentLength(pdfBytes.length)
                    .body(resource);
                    
        } catch (Exception e) {
            System.err.println("Error generating PDF: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }
    
    @PostMapping("/{id}/send-email")
    public Map<String, Object> sendPurchaseOrderEmail(@PathVariable Long id,
                                                     @RequestParam String sessionId,
                                                     @RequestParam String vendorEmail) {
        try {
            // Check if user has FINANCE_MANAGER permission
            User currentUser = authService.getCurrentUser(sessionId);
            if (currentUser == null || !"FINANCE_MANAGER".equals(currentUser.getRole().name())) {
                return Map.of("success", false, "message", "Access denied. Only Finance Managers can send emails.");
            }
            
            Optional<PurchaseOrder> poOpt = purchaseOrderService.getPurchaseOrderById(id);
            if (poOpt.isEmpty()) {
                return Map.of("success", false, "message", "Purchase Order not found");
            }
            
            PurchaseOrder po = poOpt.get();
            
            boolean emailSent = emailService.sendPurchaseOrderEmail(
                vendorEmail, 
                po.getVendorName(), 
                po, 
                sessionId
            );
            
            if (emailSent) {
                // Update PO status to SENT_TO_VENDOR
                purchaseOrderService.updatePOStatus(id, POStatus.SENT_TO_VENDOR);
                return Map.of("success", true, "message", "Purchase Order sent to vendor successfully");
            } else {
                return Map.of("success", false, "message", "Failed to send email. Please check email configuration.");
            }
            
        } catch (Exception e) {
            System.err.println("Error sending email: " + e.getMessage());
            e.printStackTrace();
            return Map.of("success", false, "message", "Failed to send email: " + e.getMessage());
        }
    }
}
