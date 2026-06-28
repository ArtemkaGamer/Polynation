package com.polynation.server.service;

import com.polynation.server.dto.request.LoginRequest;
import com.polynation.server.dto.request.RegisterRequest;
import com.polynation.server.dto.response.AuthResponse;
import com.polynation.server.dto.response.UserResponse;
import com.polynation.server.model.User;
import com.polynation.server.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            return new AuthResponse(false, "Email уже занят");
        }
        if (userRepository.existsByUsername(req.getUsername())) {
            return new AuthResponse(false, "Username уже занят");
        }

        User user = User.builder()
                .username(req.getUsername())
                .email(req.getEmail())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .role("USER")
                .rating(0)
                .quizzesSolved(0)
                .build();

        User saved = userRepository.save(user);
        log.info("Registered new user: {}", saved.getUsername());
        return new AuthResponse(true, "Регистрация успешна!", UserResponse.from(saved));
    }

    public AuthResponse login(LoginRequest req) {
        Optional<User> opt = userRepository.findByEmail(req.getEmail());
        if (opt.isEmpty() || !passwordEncoder.matches(req.getPassword(), opt.get().getPasswordHash())) {
            return new AuthResponse(false, "Неверный email или пароль");
        }
        User user = opt.get();
        log.info("User logged in: {}", user.getUsername());
        return new AuthResponse(true, "Вход выполнен!", UserResponse.from(user));
    }
}
