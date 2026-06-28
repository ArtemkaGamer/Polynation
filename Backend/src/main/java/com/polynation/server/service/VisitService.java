package com.polynation.server.service;

import com.polynation.server.dto.request.VisitRequest;
import com.polynation.server.dto.response.VisitResponse;
import com.polynation.server.model.Country;
import com.polynation.server.model.User;
import com.polynation.server.model.Visit;
import com.polynation.server.repository.CountryRepository;
import com.polynation.server.repository.UserRepository;
import com.polynation.server.repository.VisitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VisitService {

    private final VisitRepository visitRepository;
    private final UserRepository userRepository;
    private final CountryRepository countryRepository;

    public List<VisitResponse> getByUser(Long userId) {
        return visitRepository.findByUserId(userId)
                .stream().map(VisitResponse::from).collect(Collectors.toList());
    }

    public List<VisitResponse> getByCountry(Long countryId) {
        return visitRepository.findByCountryId(countryId)
                .stream().map(VisitResponse::from).collect(Collectors.toList());
    }

    public VisitResponse create(VisitRequest req) {
        User user = userRepository.findById(req.getUserId())
                .orElseThrow(() -> new RuntimeException("Пользователь #" + req.getUserId() + " не найден"));
        Country country = countryRepository.findById(req.getCountryId())
                .orElseThrow(() -> new RuntimeException("Страна #" + req.getCountryId() + " не найдена"));

        if (visitRepository.existsByUserIdAndCountryId(req.getUserId(), req.getCountryId())) {
            throw new RuntimeException("Пользователь #" + req.getUserId()
                    + " уже добавил визит в страну #" + req.getCountryId());
        }

        Visit visit = Visit.builder()
                .user(user)
                .country(country)
                .cityName(req.getCityName())
                .reviewText(req.getReviewText())
                .build();

        return VisitResponse.from(visitRepository.save(visit));
    }

    public void delete(Long id) {
        if (!visitRepository.existsById(id)) {
            throw new RuntimeException("Визит #" + id + " не найден");
        }
        visitRepository.deleteById(id);
    }
}
