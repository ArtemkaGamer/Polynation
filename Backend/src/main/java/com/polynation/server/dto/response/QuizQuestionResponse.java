package com.polynation.server.dto.response;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.polynation.server.model.QuizQuestion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizQuestionResponse {

    private Long id;
    private String questionText;
    private List<String> answerOptions;
    private Integer orderIndex;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static QuizQuestionResponse from(QuizQuestion q) {
        List<String> options;
        try {
            options = MAPPER.readValue(q.getAnswerOptions(), new TypeReference<>() {});
        } catch (Exception e) {
            options = List.of();
        }
        return QuizQuestionResponse.builder()
                .id(q.getId())
                .questionText(q.getQuestionText())
                .answerOptions(options)
                .orderIndex(q.getOrderIndex())
                .build();
    }
}
