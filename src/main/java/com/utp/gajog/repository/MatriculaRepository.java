package com.utp.gajog.repository;

import com.utp.gajog.model.EstadoMatricula;
import com.utp.gajog.model.Matricula;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MatriculaRepository extends JpaRepository<Matricula, Long> {

    public interface ConteoCategoriaProjection {
        String getCategoria();
        long getTotal();
    }

    @Query("""
        SELECT m FROM Matricula m
        JOIN FETCH m.alumno a
        JOIN FETCH a.persona
        JOIN FETCH m.categoria
        JOIN FETCH m.horario
        WHERE a.id = :alumnoId
    """)
    List<Matricula> findByAlumnoId(@Param("alumnoId") Long alumnoId);

    @Query("""
        SELECT m FROM Matricula m
        JOIN FETCH m.alumno a
        JOIN FETCH a.persona
        JOIN FETCH m.categoria
        JOIN FETCH m.horario
        WHERE a.id = :alumnoId AND m.estado = :estado
    """)
    List<Matricula> findByAlumnoIdAndEstado(@Param("alumnoId") Long alumnoId, @Param("estado") EstadoMatricula estado);

    @Query("""
        SELECT m FROM Matricula m
        JOIN FETCH m.alumno a
        JOIN FETCH a.persona p
        LEFT JOIN FETCH a.cinturonActual
        JOIN FETCH m.categoria
        JOIN FETCH m.horario
        WHERE m.horario.id = :horarioId AND m.estado = :estado
        ORDER BY p.apellidos ASC, p.nombres ASC
    """)
    List<Matricula> findByHorarioIdAndEstado(@Param("horarioId") Long horarioId, @Param("estado") EstadoMatricula estado);

    long countByHorarioIdAndEstado(Long horarioId, EstadoMatricula estado);
    long countByEstado(EstadoMatricula estado);

    @Query("""
        SELECT m FROM Matricula m
        JOIN FETCH m.alumno a
        JOIN FETCH a.persona p
        JOIN FETCH m.categoria c
        JOIN FETCH m.horario h
        WHERE m.estado = :estado
          AND a.estado = com.utp.gajog.model.EstadoAlumno.ACTIVO
    """)
    List<Matricula> findMatriculasActivasConAlumnoYCategoria(@Param("estado") EstadoMatricula estado);

    @Query(
        value = """
            SELECT m FROM Matricula m
            JOIN FETCH m.alumno a
            JOIN FETCH a.persona p
            LEFT JOIN FETCH a.apoderado ap
            LEFT JOIN FETCH ap.persona apPer
            JOIN FETCH m.categoria c
            JOIN FETCH m.horario h
            WHERE (:estado IS NULL OR m.estado = :estado)
              AND (:categoriaId IS NULL OR c.id = :categoriaId)
              AND (:horarioId IS NULL OR h.id = :horarioId)
              AND (:q IS NULL OR :q = ''
                   OR LOWER(CONCAT(p.nombres, ' ', p.apellidos)) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR p.dni LIKE CONCAT('%', :q, '%'))
            ORDER BY m.fechaMatricula DESC, m.id DESC
        """,
        countQuery = """
            SELECT COUNT(m) FROM Matricula m
            JOIN m.alumno a
            JOIN a.persona p
            JOIN m.categoria c
            JOIN m.horario h
            WHERE (:estado IS NULL OR m.estado = :estado)
              AND (:categoriaId IS NULL OR c.id = :categoriaId)
              AND (:horarioId IS NULL OR h.id = :horarioId)
              AND (:q IS NULL OR :q = ''
                   OR LOWER(CONCAT(p.nombres, ' ', p.apellidos)) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR p.dni LIKE CONCAT('%', :q, '%'))
        """
    )
    Page<Matricula> buscarMatriculasFiltradas(
            @Param("estado") EstadoMatricula estado,
            @Param("categoriaId") Long categoriaId,
            @Param("horarioId") Long horarioId,
            @Param("q") String q,
            Pageable pageable
    );

    @Query("""
        SELECT c.nombre AS categoria, COUNT(DISTINCT m.alumno.id) AS total
        FROM Matricula m
        JOIN m.categoria c
        WHERE m.estado = :estado
        GROUP BY c.id, c.nombre
        ORDER BY c.edadMinima ASC
    """)
    List<ConteoCategoriaProjection> contarAlumnosActivosPorCategoria(@Param("estado") EstadoMatricula estado);
}