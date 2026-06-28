package com.polynation.server.repository;

import com.polynation.server.model.QuizResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuizResultRepository extends JpaRepository<QuizResult, Long> {

    List<QuizResult> findByUserId(Long userId);

    List<QuizResult> findByUserIdAndQuestionQuizId(Long userId, Long quizId);

    @Query("SELECT COUNT(r) FROM QuizResult r WHERE r.user.id = :userId AND r.isCorrect = true")
    long countCorrectByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(DISTINCT r.question.quiz.id) FROM QuizResult r WHERE r.user.id = :userId")
    long countDistinctQuizzesByUserId(@Param("userId") Long userId);
}
