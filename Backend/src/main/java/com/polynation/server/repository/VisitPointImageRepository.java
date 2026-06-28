package com.polynation.server.repository;

import com.polynation.server.model.VisitPointImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VisitPointImageRepository extends JpaRepository<VisitPointImage, Long> {

    List<VisitPointImage> findByVisitPointId(Long visitPointId);
}
