package com.example.backend.dto;

import java.math.BigDecimal;
import java.util.List;

public class CreateRequisitionRequest {
    private String createdBy;
    private String department;
    private List<RequisitionItemDto> items;

    // Constructors
    public CreateRequisitionRequest() {}

    public CreateRequisitionRequest(String createdBy, String department, List<RequisitionItemDto> items) {
        this.createdBy = createdBy;
        this.department = department;
        this.items = items;
    }

    // Getters and Setters
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public List<RequisitionItemDto> getItems() { return items; }
    public void setItems(List<RequisitionItemDto> items) { this.items = items; }

    public static class RequisitionItemDto {
        private String itemName;
        private Integer quantity;
        private BigDecimal price;

        // Constructors
        public RequisitionItemDto() {}

        public RequisitionItemDto(String itemName, Integer quantity, BigDecimal price) {
            this.itemName = itemName;
            this.quantity = quantity;
            this.price = price;
        }

        // Getters and Setters
        public String getItemName() { return itemName; }
        public void setItemName(String itemName) { this.itemName = itemName; }

        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }

        public BigDecimal getPrice() { return price; }
        public void setPrice(BigDecimal price) { this.price = price; }
    }
}
