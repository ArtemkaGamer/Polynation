package com.polynation.server.controller;

import com.polynation.server.dto.response.ApiResponse;
import com.polynation.server.dto.response.VisitPointImageResponse;
import com.polynation.server.model.VisitPointImage;
import com.polynation.server.service.VisitPointImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/visit-points/{visitPointId}/images")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class VisitPointImageController {

    private final VisitPointImageService imageService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<VisitPointImageResponse>> upload(
            @PathVariable Long visitPointId,
            @RequestParam("file") MultipartFile file) throws IOException {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.ok("Файл не может быть пустым", null));
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.ok("Можно загружать только изображения", null));
        }

        VisitPointImageResponse response = imageService.upload(visitPointId, file);
        return ResponseEntity.ok(ApiResponse.ok("Картинка загружена", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<VisitPointImageResponse>>> list(
            @PathVariable Long visitPointId) {
        return ResponseEntity.ok(ApiResponse.ok(imageService.getByVisitPoint(visitPointId)));
    }

    @GetMapping("/{imageId}")
    public ResponseEntity<Resource> getImage(
            @PathVariable Long visitPointId,
            @PathVariable Long imageId) throws IOException {

        Resource resource = imageService.loadAsResource(imageId);
        VisitPointImage meta = imageService.getImageMeta(imageId);

        String contentType = meta.getContentType() != null
                ? meta.getContentType()
                : MediaType.APPLICATION_OCTET_STREAM_VALUE;

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + meta.getOriginalName() + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }

    @DeleteMapping("/{imageId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long visitPointId,
            @PathVariable Long imageId) throws IOException {

        imageService.delete(visitPointId, imageId);
        return ResponseEntity.ok(ApiResponse.ok("Картинка удалена", null));
    }
}
