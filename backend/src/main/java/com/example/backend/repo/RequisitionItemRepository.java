package com.example.backend.repo;

import com.example.backend.model.RequisitionItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RequisitionItemRepository extends JpaRepository<RequisitionItem, Long> {
    
    // Find all items for a specific requisition
    List<RequisitionItem> findByRequisitionId(Long requisitionId);
    
    // Delete all items for a specific requisition
    void deleteByRequisitionId(Long requisitionId);
}
