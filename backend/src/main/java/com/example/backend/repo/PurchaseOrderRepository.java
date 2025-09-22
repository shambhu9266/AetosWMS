package com.example.backend.repo;

import com.example.backend.model.PurchaseOrder;
import com.example.backend.model.POStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {
    List<PurchaseOrder> findByCreatedByOrderByCreatedAtDesc(String createdBy);
    List<PurchaseOrder> findAllByOrderByCreatedAtDesc();
    List<PurchaseOrder> findByStatusOrderByCreatedAtDesc(POStatus status);
    List<PurchaseOrder> findByDepartmentOrderByCreatedAtDesc(String department);
    Optional<PurchaseOrder> findByPoNumber(String poNumber);
}
