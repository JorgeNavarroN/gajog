package com.utp.gajog.repository;

import com.utp.gajog.model.HistorialCinturones;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface HistorialCinturonesRepository extends JpaRepository<HistorialCinturones, Long> {
    List<HistorialCinturones> findByAlumnoIdOrderByFechaObtencionDesc(Long alumnoId);
}