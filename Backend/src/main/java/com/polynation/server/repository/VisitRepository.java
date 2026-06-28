package com.polynation.server.repository;

import com.polynation.server.model.Visit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VisitRepository extends JpaRepository<Visit, Long> {
    List<Visit> findByUserId(Long userId);

    List<Visit> findByCountryId(Long countryId);

    boolean existsByUserIdAndCountryId(Long userId, Long countryId);
}
