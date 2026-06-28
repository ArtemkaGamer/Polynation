package com.polynation.server.dto.request;

import jakarta.validation.constraints.Email;
import lombok.Data;

@Data
public class UpdateUserRequest {
    private String username;

    @Email(message = "Некорректный email")
    private String email;
}
