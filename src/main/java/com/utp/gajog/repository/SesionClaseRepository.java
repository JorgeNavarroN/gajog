package com.utp.gajog.repository;

import com.utp.gajog.model.EstadoSesionClase;
import com.utp.gajog.model.SesionClase;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface SesionClaseRepository extends JpaRepository<SesionClase, Long> {
    List<SesionClase> findByFecha(LocalDate fecha);
    List<SesionClase> findByInstructorIdAndFecha(Long instructorId, LocalDate fecha);
    List<SesionClase> findByEstado(EstadoSesionClase estado);
}