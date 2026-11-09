package com.utp.gajog.repository;

import com.utp.gajog.model.InscripcionTorneo;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InscripcionTorneoRepository extends JpaRepository<InscripcionTorneo, Long> {

    @EntityGraph(attributePaths = {"alumno.persona", "alumno.cinturonActual"})
    List<InscripcionTorneo> findByTorneo_IdOrderByCategoriaAscIdAsc(Long torneoId);

    boolean existsByTorneo_IdAndAlumno_Id(Long torneoId, Long alumnoId);

    @EntityGraph(attributePaths = {"torneo", "alumno.persona"})
    List<InscripcionTorneo> findByPuestoIsNotNull();

    @EntityGraph(attributePaths = {"torneo"})
    List<InscripcionTorneo> findByAlumno_IdOrderByFechaInscripcionDesc(Long alumnoId);
}
