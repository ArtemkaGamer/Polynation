package com.polynation.server.service;

import com.polynation.server.dto.request.UpdateUserRequest;
import com.polynation.server.dto.response.UserResponse;
import com.polynation.server.model.User;
import com.polynation.server.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public List<UserResponse> getAll() {
        return userRepository.findAll()
                .stream()
                .map(UserResponse::from)
                .collect(Collectors.toList());
    }

    public UserResponse getById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Пользователь #" + id + " не найден"));
        return UserResponse.from(user);
    }

    public UserResponse getByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Пользователь '" + username + "' не найден"));
        return UserResponse.from(user);
    }

    public List<UserResponse> search(String query) {
        return userRepository.searchByUsername(query)
                .stream()
                .map(UserResponse::from)
                .collect(Collectors.toList());
    }

    public List<UserResponse> getLeaderboard() {
        return userRepository.findTop10ByOrderByRatingDesc()
                .stream()
                .map(UserResponse::from)
                .collect(Collectors.toList());
    }

    public UserResponse update(Long id, UpdateUserRequest req) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Пользователь #" + id + " не найден"));

        if (req.getUsername() != null && !req.getUsername().isBlank()) {
            if (userRepository.existsByUsername(req.getUsername())) {
                throw new RuntimeException("Username '" + req.getUsername() + "' уже занят");
            }
            user.setUsername(req.getUsername());
        }
        if (req.getEmail() != null && !req.getEmail().isBlank()) {
            if (userRepository.existsByEmail(req.getEmail())) {
                throw new RuntimeException("Email '" + req.getEmail() + "' уже занят");
            }
            user.setEmail(req.getEmail());
        }

        return UserResponse.from(userRepository.save(user));
    }

    public void delete(Long id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("Пользователь #" + id + " не найден");
        }
        userRepository.deleteById(id);
        log.info("Deleted user #{}", id);
    }

    public UserResponse addRating(Long id, int points) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Пользователь #" + id + " не найден"));
        user.setRating(user.getRating() + points);
        return UserResponse.from(userRepository.save(user));
    }
}
