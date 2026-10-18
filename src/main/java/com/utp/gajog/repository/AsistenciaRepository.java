package com.utp.gajog.repository;

import com.utp.gajog.model.Asistencia;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AsistenciaRepository extends JpaRepository<Asistencia, Long> {
    List<Asistencia> findBySesionId(Long sesionId);
    List<Asistencia> findByAlumnoIdOrderByFechaRegistroDesc(Long alumnoId);
    Optional<Asistencia> findBySesionIdAndAlumnoId(Long sesionId, Long alumnoId);
}