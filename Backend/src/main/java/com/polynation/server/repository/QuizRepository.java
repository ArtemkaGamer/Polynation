package com.polynation.server.repository;

import com.polynation.server.model.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuizRepository extends JpaRepository<Quiz, Long> {

    List<Quiz> findByCountryId(Long countryId);

    List<Quiz> findByType(String type);

    @Query("SELECT q FROM Quiz q WHERE q.country.id = :countryId AND q.type = :type")
    List<Quiz> findByCountryIdAndType(@Param("countryId") Long countryId,
                                      @Param("type") String type);
}
