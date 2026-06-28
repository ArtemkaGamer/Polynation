package com.example.polynation.domain.achievement;

import com.example.polynation.R;

public enum AchievementTier {
    BRONZE(R.drawable.bg_ach_bronze, "Бронза"),
    SILVER(R.drawable.bg_ach_silver, "Серебро"),
    GOLD(R.drawable.bg_ach_gold, "Золото"),
    PLATINUM(R.drawable.bg_ach_platinum, "Платина"),
    SPECIAL(R.drawable.bg_ach_special, "Особая");

    public final int medallionRes;
    public final String label;

    AchievementTier(int medallionRes, String label) {
        this.medallionRes = medallionRes;
        this.label = label;
    }
}
