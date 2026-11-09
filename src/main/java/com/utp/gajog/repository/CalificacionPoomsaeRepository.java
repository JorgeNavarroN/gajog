package com.utp.gajog.repository;

import com.utp.gajog.model.CalificacionPoomsae;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CalificacionPoomsaeRepository extends JpaRepository<CalificacionPoomsae, Long> {

    List<CalificacionPoomsae> findByInscripcion_IdOrderByJuezNumeroAsc(Long inscripcionId);

    Optional<CalificacionPoomsae> findByInscripcion_IdAndJuezNumero(
            Long inscripcionId, Integer juezNumero);
}
