package com.polynation.server.dto.response;

import com.polynation.server.model.UserAchievement;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserAchievementResponse {
    private Long id;
    private Long userId;
    private Long achievementId;
    private LocalDateTime awardedAt;

    public static UserAchievementResponse from(UserAchievement ua) {
        UserAchievementResponse r = new UserAchievementResponse();
        r.id = ua.getId();
        r.userId = ua.getUser().getId();
        r.achievementId = ua.getAchievementId();
        r.awardedAt = ua.getAwardedAt();
        return r;
    }
}
