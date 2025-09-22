package com.example.backend.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Entity
public class PurchaseOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String poNumber;

    // Bill To Information
    @Column(nullable = false)
    private String billToCompany;
    
    @Column(nullable = false)
    private String billToAddress;
    
    @Column(nullable = false)
    private String billToPAN;
    
    @Column(nullable = false)
    private String billToGSTIN;

    // Vendor Information
    @Column(nullable = false)
    private String vendorName;

    @Column(nullable = false)
    private String vendorAddress;

    @Column(nullable = false)
    private String vendorContactPerson;

    @Column(nullable = false)
    private String vendorMobileNo;

    // Ship To Information
    @Column(nullable = false)
    private String shipToAddress;

    // Order Details
    @Column(nullable = false)
    private String scopeOfOrder;

    @Column(nullable = false)
    private String shippingMethod;

    @Column(nullable = false)
    private String shippingTerms;

    @Column(nullable = false)
    private String dateOfCompletion;

    // Line Items (stored as JSON)
    @Column(columnDefinition = "TEXT")
    private String lineItemsJson;

    // Financial Details
    @Column(nullable = false)
    private BigDecimal subtotalAmount;

    @Column(nullable = false)
    private BigDecimal freightCharges;

    @Column(nullable = false)
    private BigDecimal gstRate; // GST percentage

    @Column(nullable = false)
    private BigDecimal gstAmount;

    @Column(nullable = false)
    private BigDecimal totalAmount;

    // Terms & Conditions
    @Column(length = 1000)
    private String termsAndConditions;

    @Column(length = 1000)
    private String paymentTerms;

    @Column(length = 1000)
    private String warranty;

    // System Fields
    @Column(nullable = false)
    private String createdBy; // Finance Manager username

    @Column(nullable = false)
    private String department;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private POStatus status = POStatus.DRAFT;

    private LocalDate poDate = LocalDate.now();
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    // Constructors
    public PurchaseOrder() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPoNumber() { return poNumber; }
    public void setPoNumber(String poNumber) { this.poNumber = poNumber; }

    // Bill To Getters and Setters
    public String getBillToCompany() { return billToCompany; }
    public void setBillToCompany(String billToCompany) { this.billToCompany = billToCompany; }

    public String getBillToAddress() { return billToAddress; }
    public void setBillToAddress(String billToAddress) { this.billToAddress = billToAddress; }

    public String getBillToPAN() { return billToPAN; }
    public void setBillToPAN(String billToPAN) { this.billToPAN = billToPAN; }

    public String getBillToGSTIN() { return billToGSTIN; }
    public void setBillToGSTIN(String billToGSTIN) { this.billToGSTIN = billToGSTIN; }

    // Vendor Getters and Setters
    public String getVendorName() { return vendorName; }
    public void setVendorName(String vendorName) { this.vendorName = vendorName; }

    public String getVendorAddress() { return vendorAddress; }
    public void setVendorAddress(String vendorAddress) { this.vendorAddress = vendorAddress; }

    public String getVendorContactPerson() { return vendorContactPerson; }
    public void setVendorContactPerson(String vendorContactPerson) { this.vendorContactPerson = vendorContactPerson; }

    public String getVendorMobileNo() { return vendorMobileNo; }
    public void setVendorMobileNo(String vendorMobileNo) { this.vendorMobileNo = vendorMobileNo; }

    // Ship To Getters and Setters
    public String getShipToAddress() { return shipToAddress; }
    public void setShipToAddress(String shipToAddress) { this.shipToAddress = shipToAddress; }

    // Order Details Getters and Setters
    public String getScopeOfOrder() { return scopeOfOrder; }
    public void setScopeOfOrder(String scopeOfOrder) { this.scopeOfOrder = scopeOfOrder; }

    public String getShippingMethod() { return shippingMethod; }
    public void setShippingMethod(String shippingMethod) { this.shippingMethod = shippingMethod; }

    public String getShippingTerms() { return shippingTerms; }
    public void setShippingTerms(String shippingTerms) { this.shippingTerms = shippingTerms; }

    public String getDateOfCompletion() { return dateOfCompletion; }
    public void setDateOfCompletion(String dateOfCompletion) { this.dateOfCompletion = dateOfCompletion; }

    // Line Items Getters and Setters
    public String getLineItemsJson() { return lineItemsJson; }
    public void setLineItemsJson(String lineItemsJson) { this.lineItemsJson = lineItemsJson; }

    // Financial Getters and Setters
    public BigDecimal getSubtotalAmount() { return subtotalAmount; }
    public void setSubtotalAmount(BigDecimal subtotalAmount) { this.subtotalAmount = subtotalAmount; }

    public BigDecimal getFreightCharges() { return freightCharges; }
    public void setFreightCharges(BigDecimal freightCharges) { this.freightCharges = freightCharges; }

    public BigDecimal getGstRate() { return gstRate; }
    public void setGstRate(BigDecimal gstRate) { this.gstRate = gstRate; }

    public BigDecimal getGstAmount() { return gstAmount; }
    public void setGstAmount(BigDecimal gstAmount) { this.gstAmount = gstAmount; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    // Terms Getters and Setters
    public String getTermsAndConditions() { return termsAndConditions; }
    public void setTermsAndConditions(String termsAndConditions) { this.termsAndConditions = termsAndConditions; }

    public String getPaymentTerms() { return paymentTerms; }
    public void setPaymentTerms(String paymentTerms) { this.paymentTerms = paymentTerms; }

    public String getWarranty() { return warranty; }
    public void setWarranty(String warranty) { this.warranty = warranty; }

    // System Getters and Setters
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public POStatus getStatus() { return status; }
    public void setStatus(POStatus status) { this.status = status; }

    public LocalDate getPoDate() { return poDate; }
    public void setPoDate(LocalDate poDate) { this.poDate = poDate; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
