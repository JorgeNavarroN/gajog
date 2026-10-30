package com.utp.gajog.repository;

import com.utp.gajog.model.DetalleExamen;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * BASE COMPARTIDA del módulo de exámenes (BPMN 3).
 */
public interface DetalleExamenRepository
        extends JpaRepository<DetalleExamen, Long> {

    @EntityGraph(attributePaths = {"criterio"})
    List<DetalleExamen> findByExamen_IdOrderByCriterio_IdAsc(Long examenId);
}
