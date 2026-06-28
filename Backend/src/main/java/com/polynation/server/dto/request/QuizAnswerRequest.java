package com.polynation.server.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class QuizAnswerRequest {

    @NotNull(message = "questionId обязателен")
    private Long questionId;

    @NotBlank(message = "answer не может быть пустым")
    private String answer;
}
