package com.example.backend.repo;

import com.example.backend.model.Approval;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApprovalRepository extends JpaRepository<Approval, Long> {
    List<Approval> findByRequisitionId(Long requisitionId);
}
