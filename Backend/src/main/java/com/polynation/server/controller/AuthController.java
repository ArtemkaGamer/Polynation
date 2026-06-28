package com.polynation.server.controller;

import com.polynation.server.dto.request.LoginRequest;
import com.polynation.server.dto.request.RegisterRequest;
import com.polynation.server.dto.response.AuthResponse;
import com.polynation.server.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest req) {
        AuthResponse res = authService.register(req);
        return res.isSuccess()
                ? ResponseEntity.ok(res)
                : ResponseEntity.badRequest().body(res);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        AuthResponse res = authService.login(req);
        return res.isSuccess()
                ? ResponseEntity.ok(res)
                : ResponseEntity.status(401).body(res);
    }

    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("Polynation server is alive!");
    }
}
