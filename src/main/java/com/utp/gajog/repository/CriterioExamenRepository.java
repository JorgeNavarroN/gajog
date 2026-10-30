package com.utp.gajog.repository;

import com.utp.gajog.model.CriterioExamen;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * BASE COMPARTIDA del módulo de exámenes (BPMN 3).
 */
public interface CriterioExamenRepository
        extends JpaRepository<CriterioExamen, Long> {

    List<CriterioExamen> findAllByOrderByIdAsc();
}
