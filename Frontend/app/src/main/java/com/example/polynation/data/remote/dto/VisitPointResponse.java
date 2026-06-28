package com.example.polynation.data.remote.dto;

public class VisitPointResponse {
    private boolean success;
    private String message;
    private VisitPoint data;

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public VisitPoint getData() { return data; }

    public void setSuccess(boolean success) { this.success = success; }
    public void setMessage(String message) { this.message = message; }
    public void setData(VisitPoint data) { this.data = data; }
}
