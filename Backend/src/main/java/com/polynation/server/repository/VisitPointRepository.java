package com.polynation.server.repository;

import com.polynation.server.model.VisitPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VisitPointRepository extends JpaRepository<VisitPoint, Long> {
    List<VisitPoint> findByUserId(Long userId);
}
