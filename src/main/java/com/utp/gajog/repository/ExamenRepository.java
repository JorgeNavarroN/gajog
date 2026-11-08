package com.utp.gajog.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.utp.gajog.enums.ResultadoExamen;
import com.utp.gajog.model.Examen;

public interface ExamenRepository extends JpaRepository<Examen, Long> {

    Optional<Examen> findFirstByAlumno_IdOrderByIdDesc(Long alumnoId);

    Optional<Examen> findFirstByAlumno_IdAndResultadoOrderByIdDesc(
            Long alumnoId, ResultadoExamen resultado);

    @EntityGraph(attributePaths = {"cinturonObjetivo"})
    List<Examen> findByAlumno_IdOrderByIdDesc(Long alumnoId);

    @Query(
        value = "SELECT * FROM examenes WHERE id = :id FOR UPDATE",
        nativeQuery = true
    )
    Optional<Examen> findByIdConBloqueo(@Param("id") Long id);
}