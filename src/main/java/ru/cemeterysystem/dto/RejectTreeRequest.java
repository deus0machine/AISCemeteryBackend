package ru.cemeterysystem.dto;

public class RejectTreeRequest {
    private String reason;
    
    public RejectTreeRequest() {}
    
    public RejectTreeRequest(String reason) {
        this.reason = reason;
    }
    
    public String getReason() {
        return reason;
    }
    
    public void setReason(String reason) {
        this.reason = reason;
    }
} 