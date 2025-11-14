package com.example.notification.model;

public class NotificationRequest {
    private String type;
    private String recipient;
    private String subject;
    private String message;
    
    public NotificationRequest() {}
    
    public NotificationRequest(String type, String recipient, String subject, String message) {
        this.type = type;
        this.recipient = recipient;
        this.subject = subject;
        this.message = message;
    }
    
    // Getters and Setters
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public String getRecipient() { return recipient; }
    public void setRecipient(String recipient) { this.recipient = recipient; }
    
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}