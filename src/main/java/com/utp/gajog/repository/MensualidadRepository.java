package com.utp.gajog.repository;

import com.utp.gajog.model.EstadoMensualidad;
import com.utp.gajog.model.Mensualidad;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MensualidadRepository extends JpaRepository<Mensualidad, Long> {

    List<Mensualidad> findByAlumnoIdOrderByAnioDescMesDesc(Long alumnoId);
    List<Mensualidad> findByAlumnoIdAndEstado(Long alumnoId, EstadoMensualidad estado);
    List<Mensualidad> findByEstado(EstadoMensualidad estado);
    List<Mensualidad> findByEstadoIn(Collection<EstadoMensualidad> estados);
    long countByEstado(EstadoMensualidad estado);
    long countByEstadoIn(Collection<EstadoMensualidad> estados);
    long countByAnioAndMesAndEstado(Integer anio, Integer mes, EstadoMensualidad estado);
    Optional<Mensualidad> findByAlumnoIdAndAnioAndMes(Long alumnoId, Integer anio, Integer mes);

    @Query(
        value = """
            SELECT m FROM Mensualidad m
            JOIN FETCH m.alumno a
            JOIN FETCH a.persona p
            WHERE m.anio = :anio
              AND m.mes = :mes
              AND m.estado IN :estados
              AND (:q IS NULL OR :q = ''
                   OR LOWER(CONCAT(p.nombres, ' ', p.apellidos)) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR p.dni LIKE CONCAT('%', :q, '%'))
              AND (:categoriaId IS NULL OR EXISTS (
                   SELECT 1 FROM Matricula mat
                   WHERE mat.alumno.id = a.id
                     AND mat.estado = com.utp.gajog.model.EstadoMatricula.ACTIVA
                     AND mat.grupo.categoria.id = :categoriaId))
              AND (:horarioId IS NULL OR EXISTS (
                   SELECT 1 FROM Matricula mat2
                   WHERE mat2.alumno.id = a.id
                     AND mat2.estado = com.utp.gajog.model.EstadoMatricula.ACTIVA
                     AND EXISTS (SELECT 1 FROM Horario hh WHERE hh.grupo = mat2.grupo AND hh.id = :horarioId)))
            ORDER BY m.estado DESC, m.fechaVencimiento ASC, p.apellidos ASC
        """,
        countQuery = """
            SELECT COUNT(m) FROM Mensualidad m
            JOIN m.alumno a
            JOIN a.persona p
            WHERE m.anio = :anio
              AND m.mes = :mes
              AND m.estado IN :estados
              AND (:q IS NULL OR :q = ''
                   OR LOWER(CONCAT(p.nombres, ' ', p.apellidos)) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR p.dni LIKE CONCAT('%', :q, '%'))
              AND (:categoriaId IS NULL OR EXISTS (
                   SELECT 1 FROM Matricula mat
                   WHERE mat.alumno.id = a.id
                     AND mat.estado = com.utp.gajog.model.EstadoMatricula.ACTIVA
                     AND mat.grupo.categoria.id = :categoriaId))
              AND (:horarioId IS NULL OR EXISTS (
                   SELECT 1 FROM Matricula mat2
                   WHERE mat2.alumno.id = a.id
                     AND mat2.estado = com.utp.gajog.model.EstadoMatricula.ACTIVA
                     AND EXISTS (SELECT 1 FROM Horario hh WHERE hh.grupo = mat2.grupo AND hh.id = :horarioId)))
        """
    )
    Page<Mensualidad> buscarMensualidadesFiltradas(
            @Param("anio") Integer anio,
            @Param("mes") Integer mes,
            @Param("estados") Collection<EstadoMensualidad> estados,
            @Param("q") String q,
            @Param("categoriaId") Long categoriaId,
            @Param("horarioId") Long horarioId,
            Pageable pageable
    );

    @Query("""
        SELECT m FROM Mensualidad m
        JOIN FETCH m.alumno a
        JOIN FETCH a.persona p
        WHERE m.estado = com.utp.gajog.model.EstadoMensualidad.VENCIDA
          AND a.estado = com.utp.gajog.model.EstadoAlumno.ACTIVO
          AND m.fechaVencimiento <= :fechaLimiteGracia
        ORDER BY m.fechaVencimiento ASC
    """)
    List<Mensualidad> findEnRiesgoDeRetiro(@Param("fechaLimiteGracia") LocalDate fechaLimiteGracia);
}