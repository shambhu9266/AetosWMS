package com.example.backend.model;

public enum RequisitionStatus {
    PENDING_DEPARTMENT_APPROVAL,  // First step: Department Manager approval
    PENDING_IT_APPROVAL,          // Second step: IT Manager approval
    PENDING_FINANCE_APPROVAL,     // Third step: Finance Manager approval
    APPROVED,                     // Final approval
    REJECTED,                     // Rejected at any step
    SENT_BACK                     // Sent back for revision
}


