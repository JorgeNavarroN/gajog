package com.utp.gajog.repository;

import com.utp.gajog.enums.ResultadoExamen;
import com.utp.gajog.model.Examen;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


public interface ExamenRepository extends JpaRepository<Examen, Long> {

    Optional<Examen> findFirstByAlumno_IdOrderByIdDesc(Long alumnoId);

    Optional<Examen> findFirstByAlumno_IdAndResultadoOrderByIdDesc(
            Long alumnoId, ResultadoExamen resultado);

    @EntityGraph(attributePaths = {"cinturonObjetivo"})
    List<Examen> findByAlumno_IdOrderByIdDesc(Long alumnoId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Examen e WHERE e.id = :id")
    Optional<Examen> findByIdConBloqueo(@Param("id") Long id);
}
