package com.utp.gajog.repository;

import com.utp.gajog.model.DetalleExamen;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;


public interface DetalleExamenRepository
        extends JpaRepository<DetalleExamen, Long> {

    @EntityGraph(attributePaths = {"criterio"})
    List<DetalleExamen> findByExamen_IdOrderByCriterio_IdAsc(Long examenId);
}
