package com.polynation.server.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizCheckResultResponse {

    private Long questionId;
    private String questionText;
    private String userAnswer;
    private String correctAnswer;
    private boolean correct;
}
