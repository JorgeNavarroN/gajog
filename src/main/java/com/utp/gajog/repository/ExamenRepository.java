package com.utp.gajog.repository;

import com.utp.gajog.model.Examen;
import com.utp.gajog.model.ResultadoExamen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ExamenRepository extends JpaRepository<Examen, Long> {

    @Query("""
        SELECT e FROM Examen e
        JOIN FETCH e.alumno a
        JOIN FETCH a.persona
        LEFT JOIN FETCH a.cinturonActual
        JOIN FETCH e.cinturonObjetivo
        WHERE e.id = :id
    """)
    Optional<Examen> findByIdConDetalle(@Param("id") Long id);

    @Query("""
        SELECT e FROM Examen e
        JOIN FETCH e.alumno a
        JOIN FETCH a.persona
        LEFT JOIN FETCH a.cinturonActual
        JOIN FETCH e.cinturonObjetivo
        ORDER BY e.fechaExamen DESC, e.id DESC
    """)
    List<Examen> findAllConAlumnoYCinturon();

    @Query("""
        SELECT e FROM Examen e
        JOIN FETCH e.alumno a
        JOIN FETCH a.persona p
        LEFT JOIN FETCH a.cinturonActual
        JOIN FETCH e.cinturonObjetivo c
        WHERE (:q IS NULL OR :q = ''
               OR LOWER(CONCAT(p.nombres, ' ', p.apellidos)) LIKE LOWER(CONCAT('%', :q, '%'))
               OR p.dni LIKE CONCAT('%', :q, '%'))
          AND (:cinturonObjetivoId IS NULL OR c.id = :cinturonObjetivoId)
          AND (:resultado IS NULL OR e.resultado = :resultado)
          AND (:desde IS NULL OR e.fechaExamen >= :desde)
          AND (:hasta IS NULL OR e.fechaExamen <= :hasta)
        ORDER BY e.fechaExamen DESC, e.id DESC
    """)
    List<Examen> buscarExamenesFiltrados(
            @Param("q") String q,
            @Param("cinturonObjetivoId") Long cinturonObjetivoId,
            @Param("resultado") ResultadoExamen resultado,
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta
    );

    @Query("""
        SELECT e FROM Examen e
        JOIN FETCH e.alumno a
        JOIN FETCH a.persona
        JOIN FETCH e.cinturonObjetivo
        WHERE a.id = :alumnoId
        ORDER BY e.fechaExamen DESC
    """)
    List<Examen> findByAlumnoIdOrderByFechaExamenDesc(@Param("alumnoId") Long alumnoId);

    List<Examen> findByResultado(ResultadoExamen resultado);

    @Query("""
        SELECT e FROM Examen e
        JOIN FETCH e.alumno a
        JOIN FETCH a.persona
        JOIN FETCH e.cinturonObjetivo
        WHERE e.resultado = :resultado
          AND e.fechaExamen >= :desde
        ORDER BY e.fechaExamen ASC
        LIMIT 5
    """)
    List<Examen> findProximosExamenes(
            @Param("resultado") ResultadoExamen resultado,
            @Param("desde") LocalDate desde
    );
}