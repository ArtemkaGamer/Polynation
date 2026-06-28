package com.polynation.server.service;

import com.polynation.server.dto.request.CountryRequest;
import com.polynation.server.dto.response.CountryResponse;
import com.polynation.server.model.Country;
import com.polynation.server.repository.CountryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CountryService {

    private final CountryRepository countryRepository;
    private final ExternalImageService externalImageService;

    public List<CountryResponse> getAll() {
        return countryRepository.findAll()
                .stream()
                .map(CountryResponse::from)
                .collect(Collectors.toList());
    }

    public CountryResponse getById(Long id) {
        return CountryResponse.from(findOrThrow(id));
    }


    public CountryResponse getByName(String name) {
        Country c = countryRepository.findByName(name)
                .orElseThrow(() -> new RuntimeException("Страна '" + name + "' не найдена"));
        return CountryResponse.from(c);
    }

    public List<CountryResponse> search(String query) {
        return countryRepository.searchByName(query)
                .stream()
                .map(CountryResponse::from)
                .collect(Collectors.toList());
    }

    public CountryResponse create(CountryRequest req) {
        if (countryRepository.existsByName(req.getName())) {
            throw new RuntimeException("Страна '" + req.getName() + "' уже существует");
        }
        Country c = buildFromRequest(new Country(), req);
        Country saved = countryRepository.save(c);
        log.info("Created country: {}", saved.getName());
        return CountryResponse.from(saved);
    }

    public CountryResponse update(Long id, CountryRequest req) {
        Country c = findOrThrow(id);
        buildFromRequest(c, req);
        return CountryResponse.from(countryRepository.save(c));
    }

    public CountryResponse patch(Long id, CountryRequest req) {
        Country c = findOrThrow(id);
        if (req.getName() != null) c.setName(req.getName());
        if (req.getCapital() != null) c.setCapital(req.getCapital());
        if (req.getCapitalInfoLatlng() != null && req.getCapitalInfoLatlng().size() == 2) {
            c.setCapitalLat(req.getCapitalInfoLatlng().get(0));
            c.setCapitalLng(req.getCapitalInfoLatlng().get(1));
        }
        if (req.getHistoryInfo() != null) c.setHistoryInfo(req.getHistoryInfo());
        if (req.getCultureInfo() != null) c.setCultureInfo(req.getCultureInfo());
        if (req.getMusicInfo() != null) c.setMusicInfo(req.getMusicInfo());
        if (req.getMoviesInfo() != null) c.setMoviesInfo(req.getMoviesInfo());
        if (req.getSportsInfo() != null) c.setSportsInfo(req.getSportsInfo());
        return CountryResponse.from(countryRepository.save(c));
    }

    public void delete(Long id) {
        if (!countryRepository.existsById(id)) {
            throw new RuntimeException("Страна #" + id + " не найдена");
        }
        countryRepository.deleteById(id);
        log.info("Deleted country #{}", id);
    }

    private Country findOrThrow(Long id) {
        return countryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Страна #" + id + " не найдена"));
    }

    private Country buildFromRequest(Country c, CountryRequest req) {
        c.setName(req.getName());
        c.setCapital(req.getCapital());
        if (req.getCapitalInfoLatlng() != null && req.getCapitalInfoLatlng().size() == 2) {
            c.setCapitalLat(req.getCapitalInfoLatlng().get(0));
            c.setCapitalLng(req.getCapitalInfoLatlng().get(1));
        }
        c.setHistoryInfo(req.getHistoryInfo());
        c.setCultureInfo(req.getCultureInfo());
        c.setMusicInfo(req.getMusicInfo());
        c.setMoviesInfo(req.getMoviesInfo());
        c.setSportsInfo(req.getSportsInfo());
        return c;
    }

    public CountryResponse getDetailsByName(String name, String nameForExternal) {
        Country c = countryRepository.findByName(name)
                .orElseThrow(() -> new RuntimeException("Страна '" + name + "' не найдена"));

        CountryResponse response = CountryResponse.from(c);

        String externalName = (nameForExternal != null && !nameForExternal.isBlank())
                ? nameForExternal : name;

        try {
            String flagUrl = externalImageService.getCountryFlagUrl(externalName);
            response.setFlagUrl(flagUrl);
        } catch (Exception e) {
            log.warn("Не удалось получить флаг для '{}': {}", externalName, e.getMessage());
        }

        try {
            List<String> photos = externalImageService.getPixabayImages(externalName, 3);
            response.setPhotos(photos);
        } catch (Exception e) {
            log.warn("Не удалось получить фото для '{}': {}", externalName, e.getMessage());
        }

        return response;
    }
}
