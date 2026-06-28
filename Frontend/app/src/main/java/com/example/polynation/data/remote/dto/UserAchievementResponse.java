package com.example.polynation.data.remote.dto;

public class UserAchievementResponse {
    private long id;
    private long userId;
    private long achievementId;
    private String awardedAt;

    public long getId() { return id; }
    public long getUserId() { return userId; }
    public long getAchievementId() { return achievementId; }
    public String getAwardedAt() { return awardedAt; }
}
