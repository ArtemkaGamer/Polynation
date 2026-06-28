package com.example.polynation.data.remote.dto;

import java.util.List;

public class VisitPointsResponse {
    private boolean success;
    private String message;
    private List<VisitPoint> data;

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public List<VisitPoint> getData() { return data; }

    public void setSuccess(boolean success) { this.success = success; }
    public void setMessage(String message) { this.message = message; }
    public void setData(List<VisitPoint> data) { this.data = data; }
}
