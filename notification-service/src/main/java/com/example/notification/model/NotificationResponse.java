package com.example.notification.model;

import java.util.Date;

public class NotificationResponse {
    private String notificationId;
    private String type;
    private String recipient;
    private String subject;
    private String message;
    private String status;
    private Date sentAt;
    
    public static NotificationResponse fromEntity(Notification notification) {
        NotificationResponse response = new NotificationResponse();
        response.setNotificationId(notification.getId());
        response.setType(notification.getType());
        response.setRecipient(notification.getRecipient());
        response.setSubject(notification.getSubject());
        response.setMessage(notification.getMessage());
        response.setStatus(notification.getStatus());
        response.setSentAt(notification.getSentAt());
        return response;
    }
    
    // Getters and Setters
    public String getNotificationId() { return notificationId; }
    public void setNotificationId(String notificationId) { this.notificationId = notificationId; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public String getRecipient() { return recipient; }
    public void setRecipient(String recipient) { this.recipient = recipient; }
    
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public Date getSentAt() { return sentAt; }
    public void setSentAt(Date sentAt) { this.sentAt = sentAt; }
}