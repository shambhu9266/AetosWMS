package com.example.backend.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Grn {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long purchaseOrderId;
    private String poNumber;

    private String receivedBy;
    private LocalDate receivedDate;
    private String overallRemarks;

    private Instant createdAt = Instant.now();

    @OneToMany(mappedBy = "grn", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<GrnItem> items = new ArrayList<>();

    public Long getId() { return id; }
    public Long getPurchaseOrderId() { return purchaseOrderId; }
    public void setPurchaseOrderId(Long purchaseOrderId) { this.purchaseOrderId = purchaseOrderId; }
    public String getPoNumber() { return poNumber; }
    public void setPoNumber(String poNumber) { this.poNumber = poNumber; }
    public String getReceivedBy() { return receivedBy; }
    public void setReceivedBy(String receivedBy) { this.receivedBy = receivedBy; }
    public LocalDate getReceivedDate() { return receivedDate; }
    public void setReceivedDate(LocalDate receivedDate) { this.receivedDate = receivedDate; }
    public String getOverallRemarks() { return overallRemarks; }
    public void setOverallRemarks(String overallRemarks) { this.overallRemarks = overallRemarks; }
    public Instant getCreatedAt() { return createdAt; }
    public List<GrnItem> getItems() { return items; }
    public void addItem(GrnItem item) { item.setGrn(this); this.items.add(item); }
}


