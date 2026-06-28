package com.example.polynation.data.remote.dto;

public class VisitImageResponse {
    private boolean success;
    private String message;
    private VisitImage data;

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public VisitImage getData() { return data; }

    public void setSuccess(boolean success) { this.success = success; }
    public void setMessage(String message) { this.message = message; }
    public void setData(VisitImage data) { this.data = data; }
}
