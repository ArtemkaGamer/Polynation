package com.example.polynation.data.remote.dto;

import java.util.List;

public class AchievementBatchRequest {
    private final List<Long> achievementIds;

    public AchievementBatchRequest(List<Long> achievementIds) {
        this.achievementIds = achievementIds;
    }

    public List<Long> getAchievementIds() { return achievementIds; }
}
