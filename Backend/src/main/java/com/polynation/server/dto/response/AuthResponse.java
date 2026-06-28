package com.polynation.server.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
    private boolean success;
    private String message;
    private UserResponse user;

    public AuthResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
}
