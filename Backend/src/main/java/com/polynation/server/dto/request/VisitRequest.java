package com.polynation.server.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class VisitRequest {
    @NotNull(message = "userId обязателен")
    private Long userId;

    @NotNull(message = "countryId обязателен")
    private Long countryId;

    private String cityName;
    private String reviewText;
}
