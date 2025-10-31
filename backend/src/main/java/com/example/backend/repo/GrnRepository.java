package com.example.backend.repo;

import com.example.backend.model.Grn;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GrnRepository extends JpaRepository<Grn, Long> {
    List<Grn> findByPurchaseOrderId(Long purchaseOrderId);
}


