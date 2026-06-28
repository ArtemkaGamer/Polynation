package com.polynation.server.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "quiz_questions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Quiz quiz;

    @Column(name = "question_text", nullable = false)
    private String questionText;

    @Column(name = "answer_options", columnDefinition = "TEXT", nullable = false)
    private String answerOptions;

    @Column(name = "correct_answer", nullable = false)
    private String correctAnswer;

    @Column(name = "order_index")
    private Integer orderIndex;
}
