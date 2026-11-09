package com.utp.gajog.repository;

import com.utp.gajog.model.IntentoRompimiento;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IntentoRompimientoRepository extends JpaRepository<IntentoRompimiento, Long> {

    List<IntentoRompimiento> findByInscripcion_IdOrderByNumeroAsc(Long inscripcionId);

    List<IntentoRompimiento> findByInscripcion_Torneo_IdOrderByInscripcion_IdAscNumeroAsc(Long torneoId);
}
