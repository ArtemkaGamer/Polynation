package com.example.polynation.data.remote.dto;

import java.util.List;

public class AchievementIdsResponse {
    private boolean success;
    private String message;
    private List<Long> data;

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public List<Long> getData() { return data; }
}
