package com.utp.gajog.repository;

import com.utp.gajog.model.Alumno;
import com.utp.gajog.model.EstadoAlumno;
import com.utp.gajog.model.EstadoMensualidad;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AlumnoRepository extends JpaRepository<Alumno, Long> {

    @Query("""
        SELECT a FROM Alumno a
        JOIN FETCH a.persona p
        LEFT JOIN FETCH a.cinturonActual c
        LEFT JOIN FETCH a.apoderado ap
        LEFT JOIN FETCH ap.persona
        WHERE a.id = :id
    """)
    Optional<Alumno> findByIdConDetalle(@Param("id") Long id);

    @Query("""
        SELECT a FROM Alumno a
        JOIN FETCH a.persona p
        LEFT JOIN FETCH a.cinturonActual c
        LEFT JOIN FETCH a.apoderado ap
        LEFT JOIN FETCH ap.persona
        WHERE p.dni = :dni
    """)
    Optional<Alumno> findByPersonaDni(@Param("dni") String dni);

    @Query("""
        SELECT a FROM Alumno a
        JOIN FETCH a.persona p
        LEFT JOIN FETCH a.cinturonActual c
        LEFT JOIN FETCH a.apoderado ap
        LEFT JOIN FETCH ap.persona
        WHERE a.estado = :estado
        ORDER BY p.apellidos ASC, p.nombres ASC
    """)
    List<Alumno> findByEstado(@Param("estado") EstadoAlumno estado);

    @Query("""
        SELECT a FROM Alumno a
        JOIN FETCH a.persona p
        LEFT JOIN FETCH a.cinturonActual c
        LEFT JOIN FETCH a.apoderado ap
        LEFT JOIN FETCH ap.persona
        ORDER BY p.apellidos ASC, p.nombres ASC
    """)
    List<Alumno> findAllConDetalle();

    long countByEstado(EstadoAlumno estado);

    @Query("""
        SELECT a FROM Alumno a
        JOIN FETCH a.persona p
        LEFT JOIN FETCH a.apoderado ap
        WHERE ap.id = :apoderadoId
    """)
    List<Alumno> findByApoderadoId(@Param("apoderadoId") Long apoderadoId);

    @Query("""
        SELECT a FROM Alumno a
        JOIN FETCH a.persona p
        JOIN FETCH a.apoderado ap
        ORDER BY p.nombres ASC
    """)
    List<Alumno> findAllConApoderadoYPersona();

    List<Alumno> findByCinturonActualId(Long cinturonId);

    @Query("""
        SELECT a FROM Alumno a
        JOIN FETCH a.persona p
        WHERE a.estado = :estado
          AND p.fechaNacimiento IS NOT NULL
          AND MONTH(p.fechaNacimiento) = :mes
        ORDER BY DAY(p.fechaNacimiento) ASC
    """)
    List<Alumno> findCumpleanierosDelMes(@Param("estado") EstadoAlumno estado, @Param("mes") int mes);

    @Query(
        value = """
            SELECT a FROM Alumno a
            JOIN FETCH a.persona p
            LEFT JOIN FETCH a.cinturonActual c
            LEFT JOIN FETCH a.apoderado ap
            LEFT JOIN FETCH ap.persona
            WHERE (:q IS NULL OR :q = ''
                   OR LOWER(p.nombres) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(p.apellidos) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(CONCAT(p.nombres, ' ', p.apellidos)) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR p.dni LIKE CONCAT('%', :q, '%'))
              AND (:estado IS NULL OR a.estado = :estado)
              AND (:cinturonId IS NULL OR c.id = :cinturonId)
              AND (:categoriaId IS NULL OR EXISTS (
                   SELECT 1 FROM Matricula m
                   WHERE m.alumno.id = a.id
                     AND m.estado = com.utp.gajog.model.EstadoMatricula.ACTIVA
                     AND m.categoria.id = :categoriaId))
              AND (:horarioId IS NULL OR EXISTS (
                   SELECT 1 FROM Matricula m2
                   WHERE m2.alumno.id = a.id
                     AND m2.estado = com.utp.gajog.model.EstadoMatricula.ACTIVA
                     AND m2.horario.id = :horarioId))
              AND (:estadoMensualidad IS NULL OR EXISTS (
                   SELECT 1 FROM Mensualidad men
                   WHERE men.alumno.id = a.id
                     AND men.anio = :anioActual
                     AND men.mes = :mesActual
                     AND men.estado = :estadoMensualidad))
        """,
        countQuery = """
            SELECT COUNT(a) FROM Alumno a
            JOIN a.persona p
            LEFT JOIN a.cinturonActual c
            WHERE (:q IS NULL OR :q = ''
                   OR LOWER(p.nombres) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(p.apellidos) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(CONCAT(p.nombres, ' ', p.apellidos)) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR p.dni LIKE CONCAT('%', :q, '%'))
              AND (:estado IS NULL OR a.estado = :estado)
              AND (:cinturonId IS NULL OR c.id = :cinturonId)
              AND (:categoriaId IS NULL OR EXISTS (
                   SELECT 1 FROM Matricula m
                   WHERE m.alumno.id = a.id
                     AND m.estado = com.utp.gajog.model.EstadoMatricula.ACTIVA
                     AND m.categoria.id = :categoriaId))
              AND (:horarioId IS NULL OR EXISTS (
                   SELECT 1 FROM Matricula m2
                   WHERE m2.alumno.id = a.id
                     AND m2.estado = com.utp.gajog.model.EstadoMatricula.ACTIVA
                     AND m2.horario.id = :horarioId))
              AND (:estadoMensualidad IS NULL OR EXISTS (
                   SELECT 1 FROM Mensualidad men
                   WHERE men.alumno.id = a.id
                     AND men.anio = :anioActual
                     AND men.mes = :mesActual
                     AND men.estado = :estadoMensualidad))
        """
    )
    Page<Alumno> buscarAlumnosFiltrados(
            @Param("q") String q,
            @Param("categoriaId") Long categoriaId,
            @Param("horarioId") Long horarioId,
            @Param("estado") EstadoAlumno estado,
            @Param("cinturonId") Long cinturonId,
            @Param("estadoMensualidad") EstadoMensualidad estadoMensualidad,
            @Param("anioActual") int anioActual,
            @Param("mesActual") int mesActual,
            Pageable pageable
    );
}