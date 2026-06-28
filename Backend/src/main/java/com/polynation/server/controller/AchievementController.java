package com.polynation.server.controller;

import com.polynation.server.dto.response.ApiResponse;
import com.polynation.server.dto.response.UserAchievementResponse;
import com.polynation.server.service.AchievementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users/{userId}/achievements")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AchievementController {

    private final AchievementService achievementService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<UserAchievementResponse>>> getAll(
            @PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.ok(achievementService.getByUser(userId)));
    }

    @GetMapping("/ids")
    public ResponseEntity<ApiResponse<List<Long>>> getIds(
            @PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.ok(achievementService.getAchievementIdsByUser(userId)));
    }

    @PostMapping("/{achievementId}")
    public ResponseEntity<ApiResponse<UserAchievementResponse>> add(
            @PathVariable Long userId,
            @PathVariable Long achievementId) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Ачивка добавлена",
                achievementService.addAchievement(userId, achievementId)));
    }

    @PostMapping("/batch")
    public ResponseEntity<ApiResponse<List<UserAchievementResponse>>> addBatch(
            @PathVariable Long userId,
            @RequestBody Map<String, List<Long>> body) {
        List<Long> ids = body.get("achievementIds");
        if (ids == null || ids.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.ok("achievementIds не должен быть пустым", List.of()));
        }
        return ResponseEntity.ok(ApiResponse.ok(
                "Ачивки добавлены",
                achievementService.addAchievements(userId, ids)));
    }

    @DeleteMapping("/{achievementId}")
    public ResponseEntity<ApiResponse<Void>> remove(
            @PathVariable Long userId,
            @PathVariable Long achievementId) {
        achievementService.removeAchievement(userId, achievementId);
        return ResponseEntity.ok(ApiResponse.ok("Ачивка удалена", null));
    }
}
