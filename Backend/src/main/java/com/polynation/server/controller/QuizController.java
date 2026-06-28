package com.polynation.server.controller;

import com.polynation.server.dto.request.QuizAnswerRequest;
import com.polynation.server.dto.request.QuizSubmitRequest;
import com.polynation.server.dto.response.*;
import com.polynation.server.service.QuizService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/quizzes")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<QuizResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(quizService.getAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<QuizResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(quizService.getById(id)));
    }

    @GetMapping("/country/{countryId}")
    public ResponseEntity<ApiResponse<List<QuizResponse>>> getByCountry(
            @PathVariable Long countryId) {
        return ResponseEntity.ok(ApiResponse.ok(quizService.getByCountry(countryId)));
    }

    @PostMapping("/generate/{countryId}")
    public ResponseEntity<ApiResponse<QuizResponse>> generate(
            @PathVariable Long countryId,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(
                ApiResponse.ok("Квиз сгенерирован", quizService.generate(countryId, size)));
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<ApiResponse<QuizSessionResponse>> submit(
            @PathVariable Long id,
            @Valid @RequestBody QuizSubmitRequest req) {
        return ResponseEntity.ok(
                ApiResponse.ok("Результаты квиза", quizService.submit(id, req)));
    }

    @PostMapping("/check")
    public ResponseEntity<ApiResponse<QuizCheckResultResponse>> checkOne(
            @Valid @RequestBody QuizAnswerRequest req) {
        return ResponseEntity.ok(
                ApiResponse.ok(quizService.checkOne(req.getQuestionId(), req.getAnswer())));
    }
}
