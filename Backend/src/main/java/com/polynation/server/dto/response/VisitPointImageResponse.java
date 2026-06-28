package com.polynation.server.dto.response;

import com.polynation.server.model.VisitPointImage;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class VisitPointImageResponse {
    private Long id;
    private Long visitPointId;
    private String s3Key;
    private String originalName;
    private String contentType;
    private LocalDateTime uploadedAt;
    private String url;

    public static VisitPointImageResponse from(VisitPointImage img, String baseUrl) {
        VisitPointImageResponse r = new VisitPointImageResponse();
        r.id = img.getId();
        r.visitPointId = img.getVisitPoint().getId();
        r.s3Key = img.getFilename();
        r.originalName = img.getOriginalName();
        r.contentType = img.getContentType();
        r.uploadedAt = img.getUploadedAt();
        r.url = baseUrl + "/api/visit-points/" + img.getVisitPoint().getId() + "/images/" + img.getId();
        return r;
    }
}
