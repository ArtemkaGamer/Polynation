package com.example.polynation.data.repository;

import java.util.List;

public class AchievementSyncResult {
    public final List<Long> allIds;
    public final List<Long> newlyUnlocked;

    public AchievementSyncResult(List<Long> allIds, List<Long> newlyUnlocked) {
        this.allIds = allIds;
        this.newlyUnlocked = newlyUnlocked;
    }
}
