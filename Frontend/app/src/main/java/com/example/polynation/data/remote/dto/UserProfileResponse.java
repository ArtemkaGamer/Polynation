package com.example.polynation.data.remote.dto;

public class UserProfileResponse {
    private boolean success;
    private String message;
    private UserProfile data;

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public UserProfile getData() { return data; }

    public void setSuccess(boolean success) { this.success = success; }
    public void setMessage(String message) { this.message = message; }
    public void setData(UserProfile data) { this.data = data; }
}
