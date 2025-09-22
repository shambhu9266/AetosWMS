package com.example.backend.service;

import com.example.backend.model.PurchaseOrder;
import com.example.backend.model.POStatus;
import com.example.backend.repo.PurchaseOrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class PurchaseOrderService {
    
    @Autowired
    private PurchaseOrderRepository purchaseOrderRepository;
    
    public PurchaseOrder createPurchaseOrder(String billToCompany, String billToAddress, String billToPAN, String billToGSTIN,
                                           String vendorName, String vendorAddress, String vendorContactPerson, String vendorMobileNo,
                                           String shipToAddress, String scopeOfOrder, String shippingMethod, String shippingTerms,
                                           String dateOfCompletion, String lineItemsJson, BigDecimal subtotalAmount, BigDecimal freightCharges,
                                           BigDecimal gstRate, BigDecimal gstAmount, BigDecimal totalAmount, String termsAndConditions,
                                           String paymentTerms, String warranty, String createdBy, String department) {
        
        // Generate PO Number
        String poNumber = generatePONumber();
        
        PurchaseOrder po = new PurchaseOrder();
        po.setPoNumber(poNumber);
        
        // Bill To Information
        po.setBillToCompany(billToCompany);
        po.setBillToAddress(billToAddress);
        po.setBillToPAN(billToPAN);
        po.setBillToGSTIN(billToGSTIN);
        
        // Vendor Information
        po.setVendorName(vendorName);
        po.setVendorAddress(vendorAddress);
        po.setVendorContactPerson(vendorContactPerson);
        po.setVendorMobileNo(vendorMobileNo);
        
        // Ship To Information
        po.setShipToAddress(shipToAddress);
        
        // Order Details
        po.setScopeOfOrder(scopeOfOrder);
        po.setShippingMethod(shippingMethod);
        po.setShippingTerms(shippingTerms);
        po.setDateOfCompletion(dateOfCompletion);
        
        // Line Items
        po.setLineItemsJson(lineItemsJson);
        
        // Financial Details
        po.setSubtotalAmount(subtotalAmount);
        po.setFreightCharges(freightCharges);
        po.setGstRate(gstRate);
        po.setGstAmount(gstAmount);
        po.setTotalAmount(totalAmount);
        
        // Terms & Conditions
        po.setTermsAndConditions(termsAndConditions);
        po.setPaymentTerms(paymentTerms);
        po.setWarranty(warranty);
        
        // System Fields
        po.setCreatedBy(createdBy);
        po.setDepartment(department);
        po.setStatus(POStatus.DRAFT);
        po.setCreatedAt(Instant.now());
        po.setUpdatedAt(Instant.now());
        
        return purchaseOrderRepository.save(po);
    }
    
    public PurchaseOrder updatePurchaseOrder(Long id, String billToCompany, String billToAddress, String billToPAN, String billToGSTIN,
                                           String vendorName, String vendorAddress, String vendorContactPerson, String vendorMobileNo,
                                           String shipToAddress, String scopeOfOrder, String shippingMethod, String shippingTerms,
                                           String dateOfCompletion, String lineItemsJson, BigDecimal subtotalAmount, BigDecimal freightCharges,
                                           BigDecimal gstRate, BigDecimal gstAmount, BigDecimal totalAmount, String termsAndConditions,
                                           String paymentTerms, String warranty) {
        
        PurchaseOrder po = purchaseOrderRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Purchase Order not found"));
        
        // Bill To Information
        po.setBillToCompany(billToCompany);
        po.setBillToAddress(billToAddress);
        po.setBillToPAN(billToPAN);
        po.setBillToGSTIN(billToGSTIN);
        
        // Vendor Information
        po.setVendorName(vendorName);
        po.setVendorAddress(vendorAddress);
        po.setVendorContactPerson(vendorContactPerson);
        po.setVendorMobileNo(vendorMobileNo);
        
        // Ship To Information
        po.setShipToAddress(shipToAddress);
        
        // Order Details
        po.setScopeOfOrder(scopeOfOrder);
        po.setShippingMethod(shippingMethod);
        po.setShippingTerms(shippingTerms);
        po.setDateOfCompletion(dateOfCompletion);
        
        // Line Items
        po.setLineItemsJson(lineItemsJson);
        
        // Financial Details
        po.setSubtotalAmount(subtotalAmount);
        po.setFreightCharges(freightCharges);
        po.setGstRate(gstRate);
        po.setGstAmount(gstAmount);
        po.setTotalAmount(totalAmount);
        
        // Terms & Conditions
        po.setTermsAndConditions(termsAndConditions);
        po.setPaymentTerms(paymentTerms);
        po.setWarranty(warranty);
        
        po.setUpdatedAt(Instant.now());
        
        return purchaseOrderRepository.save(po);
    }
    
    public PurchaseOrder updatePOStatus(Long id, POStatus status) {
        PurchaseOrder po = purchaseOrderRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Purchase Order not found"));
        
        po.setStatus(status);
        po.setUpdatedAt(Instant.now());
        
        return purchaseOrderRepository.save(po);
    }
    
    public List<PurchaseOrder> getAllPurchaseOrders() {
        return purchaseOrderRepository.findAllByOrderByCreatedAtDesc();
    }
    
    public List<PurchaseOrder> getPurchaseOrdersByCreator(String createdBy) {
        return purchaseOrderRepository.findByCreatedByOrderByCreatedAtDesc(createdBy);
    }
    
    public List<PurchaseOrder> getPurchaseOrdersByStatus(POStatus status) {
        return purchaseOrderRepository.findByStatusOrderByCreatedAtDesc(status);
    }
    
    public List<PurchaseOrder> getPurchaseOrdersByDepartment(String department) {
        return purchaseOrderRepository.findByDepartmentOrderByCreatedAtDesc(department);
    }
    
    public Optional<PurchaseOrder> getPurchaseOrderById(Long id) {
        return purchaseOrderRepository.findById(id);
    }
    
    public Optional<PurchaseOrder> getPurchaseOrderByNumber(String poNumber) {
        return purchaseOrderRepository.findByPoNumber(poNumber);
    }
    
    public void deletePurchaseOrder(Long id) {
        PurchaseOrder po = purchaseOrderRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Purchase Order not found"));
        
        // Only allow deletion of DRAFT status POs
        if (po.getStatus() == POStatus.DRAFT) {
            purchaseOrderRepository.delete(po);
        } else {
            throw new RuntimeException("Cannot delete Purchase Order that is not in DRAFT status");
        }
    }
    
    private String generatePONumber() {
        // Generate PO number in format: PO-YYYY-MM-XXXX
        String year = String.valueOf(Instant.now().atZone(java.time.ZoneId.systemDefault()).getYear());
        String month = String.format("%02d", Instant.now().atZone(java.time.ZoneId.systemDefault()).getMonthValue());
        
        // Get count of POs created this month
        long count = purchaseOrderRepository.count();
        String sequence = String.format("%04d", count + 1);
        
        return "PO-" + year + "-" + month + "-" + sequence;
    }
}
