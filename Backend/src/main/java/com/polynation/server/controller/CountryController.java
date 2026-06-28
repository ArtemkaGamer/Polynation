package com.polynation.server.controller;

import com.polynation.server.dto.request.CountryRequest;
import com.polynation.server.dto.response.ApiResponse;
import com.polynation.server.dto.response.CountryResponse;
import com.polynation.server.service.CountryService;
import com.polynation.server.service.ExternalImageService;  // ← НОВЫЙ ИМПОРТ
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/countries")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class CountryController {

    private final CountryService countryService;
    private final ExternalImageService externalImageService; // ← НОВАЯ ЗАВИСИМОСТЬ

    @GetMapping
    public ResponseEntity<ApiResponse<List<CountryResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(countryService.getAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CountryResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(countryService.getById(id)));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<CountryResponse>>> search(@RequestParam String q) {
        return ResponseEntity.ok(ApiResponse.ok(countryService.search(q)));
    }

    @GetMapping("/by-name/{name}")
    public ResponseEntity<ApiResponse<CountryResponse>> getByName(@PathVariable String name) {
        return ResponseEntity.ok(ApiResponse.ok(countryService.getByName(name)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CountryResponse>> create(
            @Valid @RequestBody CountryRequest req) {
        CountryResponse created = countryService.create(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Страна создана", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CountryResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody CountryRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Страна обновлена", countryService.update(id, req)));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<CountryResponse>> patch(
            @PathVariable Long id,
            @RequestBody CountryRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Страна обновлена", countryService.patch(id, req)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        countryService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Страна удалена", null));
    }

    @GetMapping("/image/{name}")
    public ResponseEntity<ApiResponse<String>> getPixabayImage(@PathVariable String name) {
        String imageUrl = externalImageService.getPixabayImageUrl(name);
        if (imageUrl == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Изображение для страны '" + name + "' не найдено"));
        }
        return ResponseEntity.ok(ApiResponse.ok(imageUrl));
    }

    @GetMapping("/flag/{name}")
    public ResponseEntity<ApiResponse<String>> getCountryFlag(@PathVariable String name) {
        String flagUrl = externalImageService.getCountryFlagUrl(name);
        if (flagUrl == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Флаг для страны '" + name + "' не найден"));
        }
        return ResponseEntity.ok(ApiResponse.ok(flagUrl));
    }

    @GetMapping("/details/{name}")
    public ResponseEntity<ApiResponse<CountryResponse>> getDetails(
            @PathVariable String name,
            @RequestParam(required = false) String nameForExternal) {
        CountryResponse details = countryService.getDetailsByName(name, nameForExternal);
        return ResponseEntity.ok(ApiResponse.ok(details));
    }

}