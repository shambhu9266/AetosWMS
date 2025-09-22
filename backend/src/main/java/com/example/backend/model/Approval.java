package com.example.backend.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
public class Approval {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long approvalId;

    private Long requisitionId;

    private String approverRole; // IT or FINANCE

    private String decision; // APPROVE / REJECT / SEND_BACK

    private String comments;

    private Instant timestamp = Instant.now();

    public Long getApprovalId() { return approvalId; }
    public Long getRequisitionId() { return requisitionId; }
    public void setRequisitionId(Long requisitionId) { this.requisitionId = requisitionId; }
    public String getApproverRole() { return approverRole; }
    public void setApproverRole(String approverRole) { this.approverRole = approverRole; }
    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }
    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }
    public Instant getTimestamp() { return timestamp; }
}


