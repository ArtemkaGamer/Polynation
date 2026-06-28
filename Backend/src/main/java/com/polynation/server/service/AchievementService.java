package com.polynation.server.service;

import com.polynation.server.dto.response.UserAchievementResponse;
import com.polynation.server.model.User;
import com.polynation.server.model.UserAchievement;
import com.polynation.server.repository.UserAchievementRepository;
import com.polynation.server.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AchievementService {

    private final UserAchievementRepository achievementRepository;
    private final UserRepository userRepository;

    public List<UserAchievementResponse> getByUser(Long userId) {
        return achievementRepository.findByUserId(userId)
                .stream()
                .map(UserAchievementResponse::from)
                .collect(Collectors.toList());
    }

    public List<Long> getAchievementIdsByUser(Long userId) {
        return achievementRepository.findByUserId(userId)
                .stream()
                .map(UserAchievement::getAchievementId)
                .collect(Collectors.toList());
    }

    @Transactional
    public UserAchievementResponse addAchievement(Long userId, Long achievementId) {
        if (achievementRepository.existsByUserIdAndAchievementId(userId, achievementId)) {
            throw new RuntimeException(
                    "Ачивка #" + achievementId + " уже есть у пользователя #" + userId);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь #" + userId + " не найден"));

        UserAchievement ua = UserAchievement.builder()
                .user(user)
                .achievementId(achievementId)
                .build();

        return UserAchievementResponse.from(achievementRepository.save(ua));
    }

    @Transactional
    public List<UserAchievementResponse> addAchievements(Long userId, List<Long> achievementIds) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь #" + userId + " не найден"));

        return achievementIds.stream()
                .filter(aid -> !achievementRepository.existsByUserIdAndAchievementId(userId, aid))
                .map(aid -> UserAchievement.builder().user(user).achievementId(aid).build())
                .map(achievementRepository::save)
                .map(UserAchievementResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public void removeAchievement(Long userId, Long achievementId) {
        if (!achievementRepository.existsByUserIdAndAchievementId(userId, achievementId)) {
            throw new RuntimeException(
                    "Ачивка #" + achievementId + " не найдена у пользователя #" + userId);
        }
        achievementRepository.deleteByUserIdAndAchievementId(userId, achievementId);
    }
}
