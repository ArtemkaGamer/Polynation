package com.polynation.server.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class VisitPointRequest {

    @NotNull(message = "userId обязателен")
    private Long userId;

    @NotNull(message = "lat обязателен")
    private Double lat;

    @NotNull(message = "lon обязателен")
    private Double lon;

    private String label;
}
