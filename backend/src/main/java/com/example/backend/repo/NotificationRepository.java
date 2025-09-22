package com.example.backend.repo;

import com.example.backend.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserIdAndIsReadFalse(String userId);
    List<Notification> findByUserIdOrderByTimestampDesc(String userId);
}
