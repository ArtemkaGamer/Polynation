package com.polynation.server.dto.response;

import com.polynation.server.model.Visit;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class VisitResponse {
    private Long id;
    private Long userId;
    private String username;
    private Long countryId;
    private String countryName;
    private String cityName;
    private String reviewText;
    private LocalDateTime visitedAt;

    public static VisitResponse from(Visit v) {
        VisitResponse r = new VisitResponse();
        r.id = v.getId();
        r.userId = v.getUser().getId();
        r.username = v.getUser().getUsername();
        r.countryId = v.getCountry().getId();
        r.countryName = v.getCountry().getName();
        r.cityName = v.getCityName();
        r.reviewText = v.getReviewText();
        r.visitedAt = v.getVisitedAt();
        return r;
    }
}
