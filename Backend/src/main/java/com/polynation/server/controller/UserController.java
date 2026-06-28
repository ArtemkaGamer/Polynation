package com.polynation.server.controller;

import com.polynation.server.dto.request.UpdateUserRequest;
import com.polynation.server.dto.response.ApiResponse;
import com.polynation.server.dto.response.UserResponse;
import com.polynation.server.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(userService.getAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getById(id)));
    }

    @GetMapping("/by-username/{username}")
    public ResponseEntity<ApiResponse<UserResponse>> getByUsername(@PathVariable String username) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getByUsername(username)));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<UserResponse>>> search(@RequestParam String q) {
        return ResponseEntity.ok(ApiResponse.ok(userService.search(q)));
    }

    @GetMapping("/leaderboard")
    public ResponseEntity<ApiResponse<List<UserResponse>>> leaderboard() {
        return ResponseEntity.ok(ApiResponse.ok(userService.getLeaderboard()));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Пользователь обновлён", userService.update(id, req)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Пользователь удалён", null));
    }

    @PostMapping("/{id}/rating")
    public ResponseEntity<ApiResponse<UserResponse>> addRating(
            @PathVariable Long id,
            @RequestBody Map<String, Integer> body) {
        int points = body.getOrDefault("points", 0);
        return ResponseEntity.ok(ApiResponse.ok(
                "Рейтинг обновлён (+%d)".formatted(points),
                userService.addRating(id, points)
        ));
    }
}
