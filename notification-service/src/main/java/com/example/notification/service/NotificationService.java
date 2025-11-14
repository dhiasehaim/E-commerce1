package com.example.notification.service;

import com.example.notification.model.Notification;
import com.example.notification.model.NotificationRequest;
import com.example.notification.model.NotificationResponse;
import com.example.notification.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {
    
    @Autowired
    private NotificationRepository notificationRepository;
    
    public NotificationResponse sendNotification(NotificationRequest request) {
        // Simulate sending notification
        System.out.println("=== SENDING NOTIFICATION ===");
        System.out.println("Type: " + request.getType());
        System.out.println("To: " + request.getRecipient());
        System.out.println("Subject: " + request.getSubject());
        System.out.println("Message: " + request.getMessage());
        System.out.println("============================");
        
        Notification notification = new Notification(
            request.getType(),
            request.getRecipient(),
            request.getSubject(),
            request.getMessage(),
            "SENT"
        );
        
        Notification savedNotification = notificationRepository.save(notification);
        return NotificationResponse.fromEntity(savedNotification);
    }
}