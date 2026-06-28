package com.polynation.server.service;

import com.polynation.server.dto.request.VisitPointRequest;
import com.polynation.server.dto.response.VisitPointResponse;
import com.polynation.server.model.User;
import com.polynation.server.model.VisitPoint;
import com.polynation.server.repository.UserRepository;
import com.polynation.server.repository.VisitPointRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VisitPointService {

    private final VisitPointRepository visitPointRepository;
    private final UserRepository userRepository;

    public List<VisitPointResponse> getByUser(Long userId) {
        return visitPointRepository.findByUserId(userId)
                .stream().map(VisitPointResponse::from).collect(Collectors.toList());
    }

    public VisitPointResponse create(VisitPointRequest req) {
        User user = userRepository.findById(req.getUserId())
                .orElseThrow(() -> new RuntimeException("Пользователь #" + req.getUserId() + " не найден"));

        VisitPoint point = VisitPoint.builder()
                .user(user)
                .lat(req.getLat())
                .lon(req.getLon())
                .label(req.getLabel())
                .build();

        return VisitPointResponse.from(visitPointRepository.save(point));
    }

    public void delete(Long id) {
        if (!visitPointRepository.existsById(id)) {
            throw new RuntimeException("Точка визита #" + id + " не найдена");
        }
        visitPointRepository.deleteById(id);
    }
}
