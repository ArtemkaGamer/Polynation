package com.example.polynation.data.remote.dto;

import java.util.List;

public class AchievementsResponse {
    private boolean success;
    private String message;
    private List<UserAchievementResponse> data;

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public List<UserAchievementResponse> getData() { return data; }
}
