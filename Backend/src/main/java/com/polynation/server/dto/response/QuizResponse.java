package com.polynation.server.dto.response;

import com.polynation.server.model.Quiz;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizResponse {

    private Long id;
    private Long countryId;
    private String countryName;
    private String title;
    private String type;
    private int questionCount;
    private List<QuizQuestionResponse> questions;

    public static QuizResponse from(Quiz quiz) {
        return QuizResponse.builder()
                .id(quiz.getId())
                .countryId(quiz.getCountry() != null ? quiz.getCountry().getId() : null)
                .countryName(quiz.getCountry() != null ? quiz.getCountry().getName() : null)
                .title(quiz.getTitle())
                .type(quiz.getType())
                .questionCount(quiz.getQuestions().size())
                .questions(quiz.getQuestions().stream()
                        .map(QuizQuestionResponse::from)
                        .collect(Collectors.toList()))
                .build();
    }

    public static QuizResponse summary(Quiz quiz) {
        return QuizResponse.builder()
                .id(quiz.getId())
                .countryId(quiz.getCountry() != null ? quiz.getCountry().getId() : null)
                .countryName(quiz.getCountry() != null ? quiz.getCountry().getName() : null)
                .title(quiz.getTitle())
                .type(quiz.getType())
                .questionCount(quiz.getQuestions().size())
                .build();
    }
}
