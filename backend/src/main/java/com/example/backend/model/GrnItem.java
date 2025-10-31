package com.example.backend.model;

import jakarta.persistence.*;

@Entity
public class GrnItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grn_id")
    private Grn grn;

    private String description;
    private Integer orderedQty;
    private Integer receivedQty;
    private String unit;
    private Boolean received;
    private String status; // OK, DAMAGED, SHORT, REJECTED
    private String remarks;

    public Long getId() { return id; }
    public Grn getGrn() { return grn; }
    public void setGrn(Grn grn) { this.grn = grn; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getOrderedQty() { return orderedQty; }
    public void setOrderedQty(Integer orderedQty) { this.orderedQty = orderedQty; }
    public Integer getReceivedQty() { return receivedQty; }
    public void setReceivedQty(Integer receivedQty) { this.receivedQty = receivedQty; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public Boolean getReceived() { return received; }
    public void setReceived(Boolean received) { this.received = received; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
}


