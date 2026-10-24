package com.utp.gajog.repository;

import com.utp.gajog.model.Asistencia;
import com.utp.gajog.model.EstadoAsistencia;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AsistenciaRepository extends JpaRepository<Asistencia, Long> {

    @Query("""
        SELECT a FROM Asistencia a
        JOIN FETCH a.alumno al
        JOIN FETCH al.persona p
        LEFT JOIN FETCH al.cinturonActual
        WHERE a.sesion.id = :sesionId
        ORDER BY p.apellidos ASC, p.nombres ASC
    """)
    List<Asistencia> findBySesionId(@Param("sesionId") Long sesionId);

    @Query("""
        SELECT a FROM Asistencia a
        JOIN FETCH a.sesion s
        JOIN FETCH s.horario h
        JOIN FETCH h.categoria c
        WHERE a.alumno.id = :alumnoId
        ORDER BY s.fecha DESC, a.fechaRegistro DESC
    """)
    List<Asistencia> findByAlumnoIdOrderByFechaRegistroDesc(@Param("alumnoId") Long alumnoId);

    Optional<Asistencia> findBySesionIdAndAlumnoId(Long sesionId, Long alumnoId);
    long countBySesionIdAndEstadoIn(Long sesionId, Collection<EstadoAsistencia> estados);
    long countBySesionId(Long sesionId);

    @Query(
        value = """
            SELECT a FROM Asistencia a
            JOIN FETCH a.sesion s
            JOIN FETCH s.horario h
            JOIN FETCH h.categoria c
            JOIN FETCH a.alumno al
            JOIN FETCH al.persona p
            LEFT JOIN FETCH al.cinturonActual cin
            WHERE (:fecha IS NULL OR s.fecha = :fecha)
              AND (:categoriaId IS NULL OR c.id = :categoriaId)
              AND (:horarioId IS NULL OR h.id = :horarioId)
              AND (:estado IS NULL OR a.estado = :estado)
              AND (:q IS NULL OR :q = ''
                   OR LOWER(CONCAT(p.nombres, ' ', p.apellidos)) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR p.dni LIKE CONCAT('%', :q, '%'))
            ORDER BY s.fecha DESC, a.fechaRegistro DESC
        """,
        countQuery = """
            SELECT COUNT(a) FROM Asistencia a
            JOIN a.sesion s
            JOIN s.horario h
            JOIN h.categoria c
            JOIN a.alumno al
            JOIN al.persona p
            WHERE (:fecha IS NULL OR s.fecha = :fecha)
              AND (:categoriaId IS NULL OR c.id = :categoriaId)
              AND (:horarioId IS NULL OR h.id = :horarioId)
              AND (:estado IS NULL OR a.estado = :estado)
              AND (:q IS NULL OR :q = ''
                   OR LOWER(CONCAT(p.nombres, ' ', p.apellidos)) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR p.dni LIKE CONCAT('%', :q, '%'))
        """
    )
    Page<Asistencia> buscarHistorialFiltrado(
            @Param("fecha") LocalDate fecha,
            @Param("categoriaId") Long categoriaId,
            @Param("horarioId") Long horarioId,
            @Param("q") String q,
            @Param("estado") EstadoAsistencia estado,
            Pageable pageable
    );
}