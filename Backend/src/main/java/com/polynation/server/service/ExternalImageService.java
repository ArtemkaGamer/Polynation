package com.polynation.server.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class ExternalImageService {

    @Value("${pixabay.api.key}")
    private String pixabayApiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String getPixabayImageUrl(String countryName) {
        List<String> urls = getPixabayImages(countryName, 1);
        return urls.isEmpty() ? null : urls.get(0);
    }

    public List<String> getPixabayImages(String countryName, int count) {
        List<String> result = new ArrayList<>();

        int safeCount = Math.max(3, Math.min(count, 200));

        String url = UriComponentsBuilder
                .fromHttpUrl("https://pixabay.com/api/")
                .queryParam("key", pixabayApiKey)
                .queryParam("q", countryName)
                .queryParam("image_type", "photo")
                .queryParam("orientation", "horizontal")
                .queryParam("per_page", safeCount)
                .queryParam("safesearch", true)
                .toUriString();

        try {
            String response = restTemplate.getForObject(url, String.class);
            JsonNode hits = objectMapper.readTree(response).get("hits");

            if (hits != null && hits.isArray()) {
                for (JsonNode hit : hits) {
                    result.add(hit.get("webformatURL").asText());
                    if (result.size() >= count) break;
                }
            }
            log.info("Pixabay: получено {} фото для '{}'", result.size(), countryName);
        } catch (Exception e) {
            log.error("Pixabay error для '{}': {}", countryName, e.getMessage());
        }
        return result;
    }

    public String getCountryFlagUrl(String countryName) {
        String url = "https://restcountries.com/v3.1/name/"
                + countryName + "?fields=flags";
        try {
            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);
            if (root.isArray() && root.size() > 0) {
                return root.get(0).get("flags").get("png").asText();
            }
            log.warn("REST Countries: '{}' не найдена", countryName);
        } catch (Exception e) {
            log.error("REST Countries error для '{}': {}", countryName, e.getMessage());
        }
        return null;
    }

    public double[] getCapitalCoordinates(String capitalName) {
        if (capitalName == null || capitalName.isBlank()) return null;

        String url = UriComponentsBuilder
                .fromHttpUrl("https://nominatim.openstreetmap.org/search")
                .queryParam("q", capitalName)
                .queryParam("format", "json")
                .queryParam("limit", 1)
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "PolyNationApp/1.0");

        try {
            ResponseEntity<String> response =
                    restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);

            JsonNode root = objectMapper.readTree(response.getBody());
            if (root.isArray() && root.size() > 0) {
                double lat = root.get(0).get("lat").asDouble();
                double lon = root.get(0).get("lon").asDouble();
                log.info("Nominatim: '{}' → [{}, {}]", capitalName, lat, lon);
                return new double[]{lat, lon};
            }
            log.warn("Nominatim: столица '{}' не найдена", capitalName);
        } catch (Exception e) {
            log.error("Nominatim error для '{}': {}", capitalName, e.getMessage());
        }
        return null;
    }
}