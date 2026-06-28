package com.polynation.server.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class LoginRequest {
    @Email(message = "Некорректный email")
    @NotBlank(message = "email обязателен")
    private String email;

    @NotBlank(message = "Пароль обязателен")
    private String password;
}
