package com.polynation.server.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank(message = "username обязателен")
    private String username;

    @Email(message = "Некорректный email")
    @NotBlank(message = "email обязателен")
    private String email;

    @NotBlank(message = "Пароль обязателен")
    @Size(min = 4, message = "Пароль минимум 4 символа")
    private String password;
}
