package com.polynation.server.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizSessionResponse {

    private Long quizId;
    private String quizTitle;
    private Long userId;
    private int totalQuestions;
    private int correctAnswers;
    private int wrongAnswers;
    private int scorePercent;
    private int ratingEarned;
    private List<QuizCheckResultResponse> details;
}
