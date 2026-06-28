package com.polynation.server.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.List;

@Data
public class CountryRequest {
    @NotBlank(message = "Название страны обязательно")
    private String name;

    private String capital;
    private List<Double> capitalInfoLatlng;
    private String historyInfo;
    private String cultureInfo;
    private String musicInfo;
    private String moviesInfo;
    private String sportsInfo;
}