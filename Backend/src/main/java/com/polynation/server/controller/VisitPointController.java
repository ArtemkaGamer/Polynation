package com.polynation.server.controller;

import com.polynation.server.dto.request.VisitPointRequest;
import com.polynation.server.dto.response.ApiResponse;
import com.polynation.server.dto.response.VisitPointResponse;
import com.polynation.server.service.VisitPointService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/visit-points")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class VisitPointController {

    private final VisitPointService visitPointService;

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<VisitPointResponse>>> getByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.ok(visitPointService.getByUser(userId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<VisitPointResponse>> create(@Valid @RequestBody VisitPointRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Точка визита добавлена", visitPointService.create(req)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        visitPointService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Точка визита удалена", null));
    }
}
