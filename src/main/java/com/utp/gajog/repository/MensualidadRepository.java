package com.utp.gajog.repository;

import com.utp.gajog.model.EstadoMensualidad;
import com.utp.gajog.model.Mensualidad;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface MensualidadRepository extends JpaRepository<Mensualidad, Long> {
    List<Mensualidad> findByAlumnoIdOrderByAnioDescMesDesc(Long alumnoId);
    List<Mensualidad> findByAlumnoIdAndEstado(Long alumnoId, EstadoMensualidad estado);
    List<Mensualidad> findByEstado(EstadoMensualidad estado);
    Optional<Mensualidad> findByAlumnoIdAndAnioAndMes(Long alumnoId, Integer anio, Integer mes);
}