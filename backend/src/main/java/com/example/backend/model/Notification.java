package com.example.backend.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long notificationId;

    private String userId;

    @Column(length = 1000)
    private String message;

    private boolean isRead = false;

    private Instant timestamp = Instant.now();

    public Long getNotificationId() { return notificationId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }
    public Instant getTimestamp() { return timestamp; }
}


