package com.polynation.server.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class QuizSubmitRequest {

    @NotNull(message = "userId обязателен")
    private Long userId;

    @NotEmpty(message = "Список ответов не может быть пустым")
    @Valid
    private List<QuizAnswerRequest> answers;
}
