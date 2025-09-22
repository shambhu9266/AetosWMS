package com.example.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Requisition {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Legacy fields for backward compatibility - will be deprecated
    private String itemName;
    private Integer quantity;
    private BigDecimal price;

    private String createdBy;
    private String department;

    @Enumerated(EnumType.STRING)
    private RequisitionStatus status;

    private String approvedByIt;
    private String approvedByFinance;

    private Instant createdAt = Instant.now();

    // New field for multiple items
    @OneToMany(mappedBy = "requisition", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    private List<RequisitionItem> items = new ArrayList<>();

    public Long getId() { return id; }
    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public RequisitionStatus getStatus() { return status; }
    public void setStatus(RequisitionStatus status) { this.status = status; }
    public String getApprovedByIt() { return approvedByIt; }
    public void setApprovedByIt(String approvedByIt) { this.approvedByIt = approvedByIt; }
    public String getApprovedByFinance() { return approvedByFinance; }
    public void setApprovedByFinance(String approvedByFinance) { this.approvedByFinance = approvedByFinance; }
    public Instant getCreatedAt() { return createdAt; }

    // Items management
    public List<RequisitionItem> getItems() { return items; }
    public void setItems(List<RequisitionItem> items) { this.items = items; }

    public void addItem(RequisitionItem item) {
        items.add(item);
        item.setRequisition(this);
    }

    public void removeItem(RequisitionItem item) {
        items.remove(item);
        item.setRequisition(null);
    }

    // Helper method to calculate total amount
    public BigDecimal getTotalAmount() {
        return items.stream()
                .map(RequisitionItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // Helper method to get total quantity
    public Integer getTotalQuantity() {
        return items.stream()
                .mapToInt(RequisitionItem::getQuantity)
                .sum();
    }

    // Helper method to get item names as comma-separated string (for backward compatibility)
    public String getItemNames() {
        return items.stream()
                .map(RequisitionItem::getItemName)
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
    }
}


