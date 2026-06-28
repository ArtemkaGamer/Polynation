package com.polynation.server.controller;

import com.polynation.server.dto.request.VisitRequest;
import com.polynation.server.dto.response.ApiResponse;
import com.polynation.server.dto.response.VisitResponse;
import com.polynation.server.service.VisitService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/visits")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class VisitController {

    private final VisitService visitService;

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<VisitResponse>>> byUser(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.ok(visitService.getByUser(userId)));
    }

    @GetMapping("/country/{countryId}")
    public ResponseEntity<ApiResponse<List<VisitResponse>>> byCountry(@PathVariable Long countryId) {
        return ResponseEntity.ok(ApiResponse.ok(visitService.getByCountry(countryId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<VisitResponse>> create(@Valid @RequestBody VisitRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Визит добавлен", visitService.create(req)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        visitService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Визит удалён", null));
    }
}
