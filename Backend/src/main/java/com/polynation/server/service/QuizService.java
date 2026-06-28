package com.polynation.server.service;

import com.polynation.server.dto.request.QuizAnswerRequest;
import com.polynation.server.dto.request.QuizSubmitRequest;
import com.polynation.server.dto.response.QuizCheckResultResponse;
import com.polynation.server.dto.response.QuizResponse;
import com.polynation.server.dto.response.QuizSessionResponse;
import com.polynation.server.model.*;
import com.polynation.server.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuizService {

    private final QuizRepository quizRepository;
    private final QuizQuestionRepository questionRepository;
    private final QuizResultRepository resultRepository;
    private final UserRepository userRepository;
    private final CountryRepository countryRepository;

    public List<QuizResponse> getAll() {
        return quizRepository.findAll().stream()
                .map(QuizResponse::summary)
                .collect(Collectors.toList());
    }

    public List<QuizResponse> getByCountry(Long countryId) {
        return quizRepository.findByCountryId(countryId).stream()
                .map(QuizResponse::summary)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public QuizResponse getById(Long quizId) {
        Quiz quiz = findQuizOrThrow(quizId);
        return QuizResponse.from(quiz);
    }

    @Transactional
    public QuizResponse generate(Long countryId, int size) {
        Country country = countryRepository.findById(countryId)
                .orElseThrow(() -> new RuntimeException("Страна с id=" + countryId + " не найдена"));

        List<QuizQuestion> pool = quizRepository.findByCountryId(countryId).stream()
                .flatMap(q -> questionRepository.findByQuizIdOrderByOrderIndexAsc(q.getId()).stream())
                .collect(Collectors.toList());

        if (pool.isEmpty()) {
            throw new RuntimeException("Для этой страны пока нет вопросов");
        }

        Collections.shuffle(pool);
        List<QuizQuestion> selected = pool.subList(0, Math.min(size, pool.size()));

        Quiz generated = Quiz.builder()
                .country(country)
                .title("Квиз по стране: " + country.getName())
                .type("GENERATED")
                .build();
        Quiz saved = quizRepository.save(generated);

        List<QuizQuestion> cloned = new ArrayList<>();
        for (int i = 0; i < selected.size(); i++) {
            QuizQuestion src = selected.get(i);
            QuizQuestion q = QuizQuestion.builder()
                    .quiz(saved)
                    .questionText(src.getQuestionText())
                    .answerOptions(src.getAnswerOptions())
                    .correctAnswer(src.getCorrectAnswer())
                    .orderIndex(i + 1)
                    .build();
            cloned.add(q);
        }
        questionRepository.saveAll(cloned);
        saved.setQuestions(cloned);

        log.info("Generated quiz id={} for country={} with {} questions", saved.getId(), country.getName(), cloned.size());
        return QuizResponse.from(saved);
    }

    @Transactional
    public QuizSessionResponse submit(Long quizId, QuizSubmitRequest req) {
        Quiz quiz = findQuizOrThrow(quizId);
        User user = userRepository.findById(req.getUserId())
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        // Build a map questionId → question for fast lookup
        Map<Long, QuizQuestion> questionMap = questionRepository
                .findByQuizIdOrderByOrderIndexAsc(quizId).stream()
                .collect(Collectors.toMap(QuizQuestion::getId, q -> q));

        List<QuizCheckResultResponse> details = new ArrayList<>();
        int correct = 0;

        for (QuizAnswerRequest ans : req.getAnswers()) {
            QuizQuestion question = questionMap.get(ans.getQuestionId());
            if (question == null) {
                log.warn("Question id={} not found in quiz id={}, skipping", ans.getQuestionId(), quizId);
                continue;
            }

            boolean isCorrect = question.getCorrectAnswer().equalsIgnoreCase(ans.getAnswer().trim());
            if (isCorrect) correct++;

            // Persist result
            QuizResult result = QuizResult.builder()
                    .user(user)
                    .question(question)
                    .userAnswer(ans.getAnswer())
                    .isCorrect(isCorrect)
                    .build();
            resultRepository.save(result);

            details.add(QuizCheckResultResponse.builder()
                    .questionId(question.getId())
                    .questionText(question.getQuestionText())
                    .userAnswer(ans.getAnswer())
                    .correctAnswer(question.getCorrectAnswer())
                    .correct(isCorrect)
                    .build());
        }

        int total = details.size();
        int wrong = total - correct;
        int scorePercent = total > 0 ? (correct * 100 / total) : 0;

        int ratingEarned = correct * 10;
        user.setRating(user.getRating() + ratingEarned);
        user.setQuizzesSolved(user.getQuizzesSolved() + 1);
        userRepository.save(user);

        log.info("User {} submitted quiz {}: {}/{} correct, +{} rating",
                user.getUsername(), quizId, correct, total, ratingEarned);

        return QuizSessionResponse.builder()
                .quizId(quizId)
                .quizTitle(quiz.getTitle())
                .userId(user.getId())
                .totalQuestions(total)
                .correctAnswers(correct)
                .wrongAnswers(wrong)
                .scorePercent(scorePercent)
                .ratingEarned(ratingEarned)
                .details(details)
                .build();
    }

    public QuizCheckResultResponse checkOne(Long questionId, String userAnswer) {
        QuizQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Вопрос с id=" + questionId + " не найден"));

        boolean correct = question.getCorrectAnswer().equalsIgnoreCase(userAnswer.trim());
        return QuizCheckResultResponse.builder()
                .questionId(question.getId())
                .questionText(question.getQuestionText())
                .userAnswer(userAnswer)
                .correctAnswer(question.getCorrectAnswer())
                .correct(correct)
                .build();
    }

    private Quiz findQuizOrThrow(Long id) {
        return quizRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Квиз с id=" + id + " не найден"));
    }
}
