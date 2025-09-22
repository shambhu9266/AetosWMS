package com.example.backend.model;

import java.math.BigDecimal;

public class LineItem {
    private Integer srNo;
    private String quantity;
    private String unit;
    private String description;
    private BigDecimal unitPrice;
    private BigDecimal amount;

    // Constructors
    public LineItem() {}

    public LineItem(Integer srNo, String quantity, String unit, String description, BigDecimal unitPrice, BigDecimal amount) {
        this.srNo = srNo;
        this.quantity = quantity;
        this.unit = unit;
        this.description = description;
        this.unitPrice = unitPrice;
        this.amount = amount;
    }

    // Getters and Setters
    public Integer getSrNo() { return srNo; }
    public void setSrNo(Integer srNo) { this.srNo = srNo; }

    public String getQuantity() { return quantity; }
    public void setQuantity(String quantity) { this.quantity = quantity; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}
