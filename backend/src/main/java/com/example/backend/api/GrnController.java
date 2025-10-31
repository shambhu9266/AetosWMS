package com.example.backend.api;

import com.example.backend.model.*;
import com.example.backend.repo.GrnRepository;
import com.example.backend.repo.PurchaseOrderRepository;
import com.example.backend.service.AuthService;
import com.example.backend.service.GrnPdfService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/grn")
@CrossOrigin(origins = "http://localhost:4200")
public class GrnController {

    @Autowired
    private GrnRepository grnRepository;

    @Autowired
    private PurchaseOrderRepository purchaseOrderRepository;

    @Autowired
    private AuthService authService;

    @Autowired
    private GrnPdfService grnPdfService;

    public static class ReceiveItemDto {
        public String description;
        public Integer orderedQty;
        public Integer receiveQty;
        public String unit;
        public Boolean received;
        public String status; // OK/DAMAGED/SHORT/REJECTED
        public String remarks;
    }

    public static class ReceiveRequest {
        public Long poId;
        public String receivedBy;
        public String receivedDate; // yyyy-MM-dd
        public String overallRemarks;
        public List<ReceiveItemDto> items;
    }

    @PostMapping("/receive")
    public Map<String, Object> receive(@RequestParam String sessionId, @RequestBody ReceiveRequest request) {
        try {
            User currentUser = authService.getCurrentUser(sessionId);
            if (currentUser == null) {
                return Map.of("success", false, "message", "Invalid session");
            }

            PurchaseOrder po = purchaseOrderRepository.findById(request.poId).orElse(null);
            if (po == null) {
                return Map.of("success", false, "message", "Purchase Order not found");
            }

            // Create GRN
            Grn grn = new Grn();
            grn.setPurchaseOrderId(po.getId());
            grn.setPoNumber(po.getPoNumber());
            grn.setReceivedBy(request.receivedBy != null && !request.receivedBy.isBlank() ? request.receivedBy : currentUser.getFullName());
            grn.setReceivedDate(request.receivedDate != null ? LocalDate.parse(request.receivedDate) : LocalDate.now());
            grn.setOverallRemarks(request.overallRemarks);

            boolean allFullyReceived = true;

            for (ReceiveItemDto dto : request.items) {
                GrnItem item = new GrnItem();
                item.setDescription(dto.description);
                item.setOrderedQty(dto.orderedQty != null ? dto.orderedQty : 0);
                item.setReceivedQty(dto.receiveQty != null ? dto.receiveQty : 0);
                item.setUnit(dto.unit);
                item.setReceived(Boolean.TRUE.equals(dto.received));
                item.setStatus(dto.status != null ? dto.status : (Boolean.TRUE.equals(dto.received) ? "OK" : "REJECTED"));
                item.setRemarks(dto.remarks);
                grn.addItem(item);

                // Determine if fully received
                if (!(Boolean.TRUE.equals(dto.received) && dto.receiveQty != null && dto.orderedQty != null && dto.receiveQty >= dto.orderedQty)) {
                    allFullyReceived = false;
                }
            }

            grnRepository.save(grn);

            // Update PO status if fully received
            if (allFullyReceived) {
                po.setStatus(POStatus.DELIVERED);
                purchaseOrderRepository.save(po);
            }

            return Map.of("success", true, "message", "GRN recorded successfully", "grnId", grn.getId(), "poStatus", po.getStatus().name());
        } catch (Exception e) {
            return Map.of("success", false, "message", "Failed to record GRN: " + e.getMessage());
        }
    }

    @GetMapping("/history")
    public Map<String, Object> history(@RequestParam String sessionId, @RequestParam(required = false) Long poId) {
        try {
            User currentUser = authService.getCurrentUser(sessionId);
            if (currentUser == null) {
                return Map.of("success", false, "message", "Invalid session");
            }
            List<Grn> list = (poId != null) ? grnRepository.findByPurchaseOrderId(poId) : grnRepository.findAll();
            return Map.of("success", true, "grns", list);
        } catch (Exception e) {
            return Map.of("success", false, "message", "Failed to load GRN history: " + e.getMessage());
        }
    }

    @GetMapping("/{id}/pdf")
    public @ResponseBody byte[] downloadPdf(@PathVariable Long id, @RequestParam String sessionId, @RequestHeader(value = "Accept", required = false) String accept, jakarta.servlet.http.HttpServletResponse response) {
        User currentUser = authService.getCurrentUser(sessionId);
        if (currentUser == null) {
            response.setStatus(401);
            return new byte[0];
        }
        byte[] pdf = grnPdfService.generateGrnPdf(id);
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=GRN_" + id + ".pdf");
        return pdf;
    }
}


