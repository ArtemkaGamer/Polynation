package com.example.polynation.data.remote.dto;

import java.util.List;

public class UsersResponse {
    private boolean success;
    private String message;
    private List<UserProfile> data;

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public List<UserProfile> getData() { return data; }

    public void setSuccess(boolean success) { this.success = success; }
    public void setMessage(String message) { this.message = message; }
    public void setData(List<UserProfile> data) { this.data = data; }
}
