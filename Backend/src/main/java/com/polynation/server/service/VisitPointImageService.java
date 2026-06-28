package com.polynation.server.service;

import com.polynation.server.dto.response.VisitPointImageResponse;
import com.polynation.server.model.VisitPoint;
import com.polynation.server.model.VisitPointImage;
import com.polynation.server.repository.VisitPointImageRepository;
import com.polynation.server.repository.VisitPointRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VisitPointImageService {

    private final VisitPointImageRepository imageRepository;
    private final VisitPointRepository visitPointRepository;
    private final S3Client s3Client;

    @Value("${cloudru.s3.bucket}")
    private String bucket;

    @Value("${app.base-url}")
    private String baseUrl;

    @Transactional
    public VisitPointImageResponse upload(Long visitPointId, MultipartFile file) throws IOException {
        VisitPoint visitPoint = visitPointRepository.findById(visitPointId)
                .orElseThrow(() -> new RuntimeException("Точка визита #" + visitPointId + " не найдена"));

        String originalName = file.getOriginalFilename();
        String extension = "";
        if (originalName != null && originalName.contains(".")) {
            extension = originalName.substring(originalName.lastIndexOf('.'));
        }

        String s3Key = "visit-point/" + visitPointId + "/" + UUID.randomUUID() + extension;

        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(s3Key)
                        .contentType(file.getContentType())
                        .contentLength(file.getSize())
                        .build(),
                RequestBody.fromInputStream(file.getInputStream(), file.getSize())
        );

        log.info("Uploaded to Cloud.ru S3: bucket={} key={}", bucket, s3Key);

        VisitPointImage image = VisitPointImage.builder()
                .visitPoint(visitPoint)
                .filename(s3Key)
                .originalName(originalName)
                .contentType(file.getContentType())
                .build();

        return VisitPointImageResponse.from(imageRepository.save(image), baseUrl);
    }

    public List<VisitPointImageResponse> getByVisitPoint(Long visitPointId) {
        if (!visitPointRepository.existsById(visitPointId)) {
            throw new RuntimeException("Точка визита #" + visitPointId + " не найдена");
        }
        return imageRepository.findByVisitPointId(visitPointId)
                .stream()
                .map(img -> VisitPointImageResponse.from(img, baseUrl))
                .collect(Collectors.toList());
    }

    public Resource loadAsResource(Long imageId) {
        VisitPointImage image = imageRepository.findById(imageId)
                .orElseThrow(() -> new RuntimeException("Картинка #" + imageId + " не найдена"));

        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucket)
                .key(image.getFilename())
                .build();

        var s3Stream = s3Client.getObject(request);
        return new InputStreamResource(s3Stream);
    }

    public VisitPointImage getImageMeta(Long imageId) {
        return imageRepository.findById(imageId)
                .orElseThrow(() -> new RuntimeException("Картинка #" + imageId + " не найдена"));
    }

    @Transactional
    public void delete(Long visitPointId, Long imageId) {
        VisitPointImage image = imageRepository.findById(imageId)
                .orElseThrow(() -> new RuntimeException("Картинка #" + imageId + " не найдена"));

        if (!image.getVisitPoint().getId().equals(visitPointId)) {
            throw new RuntimeException(
                    "Картинка #" + imageId + " не принадлежит точке визита #" + visitPointId);
        }

        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(image.getFilename())
                .build());

        log.info("Deleted from Cloud.ru S3: bucket={} key={}", bucket, image.getFilename());

        imageRepository.deleteById(imageId);
    }
}
