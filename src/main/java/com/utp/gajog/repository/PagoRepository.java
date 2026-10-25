package com.utp.gajog.repository;

import com.utp.gajog.model.EstadoPago;
import com.utp.gajog.model.MedioPago;
import com.utp.gajog.model.Pago;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PagoRepository extends JpaRepository<Pago, Long> {

    @Query("""
        SELECT p FROM Pago p
        JOIN FETCH p.alumno a
        JOIN FETCH a.persona
        LEFT JOIN FETCH p.mensualidad
        WHERE p.id = :id
    """)
    Optional<Pago> findByIdConDetalle(@Param("id") Long id);

    @Query("""
        SELECT p FROM Pago p
        JOIN FETCH p.alumno a
        JOIN FETCH a.persona
        LEFT JOIN FETCH p.mensualidad
        WHERE a.id = :alumnoId
        ORDER BY p.fechaPago DESC
    """)
    List<Pago> findByAlumnoIdOrderByFechaPagoDesc(@Param("alumnoId") Long alumnoId);

    List<Pago> findByMensualidadId(Long mensualidadId);
    Optional<Pago> findFirstByMensualidadIdAndEstadoOrderByIdDesc(Long mensualidadId, EstadoPago estado);
    List<Pago> findByEstado(EstadoPago estado);

    @Query("""
        SELECT COALESCE(SUM(p.monto), 0)
        FROM Pago p
        WHERE p.estado = :estado
          AND p.fechaPago >= :inicio
          AND p.fechaPago < :fin
    """)
    BigDecimal sumarRecaudacionPeriodo(
            @Param("estado") EstadoPago estado,
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin
    );

    @Query("""
        SELECT COALESCE(SUM(p.monto), 0)
        FROM Pago p
        JOIN p.mensualidad m
        WHERE p.estado = com.utp.gajog.model.EstadoPago.REGISTRADO
          AND m.anio = :anio
          AND m.mes = :mes
    """)
    BigDecimal sumarRecaudacionPorPeriodoMensualidad(
            @Param("anio") Integer anio,
            @Param("mes") Integer mes
    );

    @Query(
        value = """
            SELECT p FROM Pago p
            JOIN FETCH p.alumno a
            JOIN FETCH a.persona per
            LEFT JOIN FETCH p.mensualidad m
            WHERE m.anio = :anio
              AND m.mes = :mes
              AND p.estado = com.utp.gajog.model.EstadoPago.REGISTRADO
              AND (:medioPago IS NULL OR p.medioPago = :medioPago)
              AND (:q IS NULL OR :q = ''
                   OR LOWER(CONCAT(per.nombres, ' ', per.apellidos)) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR per.dni LIKE CONCAT('%', :q, '%'))
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
            ORDER BY p.fechaPago DESC
        """,
        countQuery = """
            SELECT COUNT(p) FROM Pago p
            JOIN p.alumno a
            JOIN a.persona per
            LEFT JOIN p.mensualidad m
            WHERE m.anio = :anio
              AND m.mes = :mes
              AND p.estado = com.utp.gajog.model.EstadoPago.REGISTRADO
              AND (:medioPago IS NULL OR p.medioPago = :medioPago)
              AND (:q IS NULL OR :q = ''
                   OR LOWER(CONCAT(per.nombres, ' ', per.apellidos)) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR per.dni LIKE CONCAT('%', :q, '%'))
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
    Page<Pago> buscarPagosDelPeriodo(
            @Param("anio") Integer anio,
            @Param("mes") Integer mes,
            @Param("medioPago") MedioPago medioPago,
            @Param("q") String q,
            @Param("categoriaId") Long categoriaId,
            @Param("horarioId") Long horarioId,
            Pageable pageable
    );
}