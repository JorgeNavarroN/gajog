package com.utp.gajog.repository;

import com.utp.gajog.model.HistorialCinturones;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;


public interface HistorialCinturonesRepository
        extends JpaRepository<HistorialCinturones, Long> {

    @EntityGraph(attributePaths = {"cinturon", "examen"})
    List<HistorialCinturones> findByAlumno_IdOrderByFechaObtencionDescIdDesc(
            Long alumnoId);
}
