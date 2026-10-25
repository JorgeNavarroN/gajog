package com.utp.gajog.repository;

import com.utp.gajog.model.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface MatriculaRepository extends JpaRepository<Matricula, Long> {
    interface ConteoCategoriaProjection { String getCategoria(); long getTotal(); }

    @Query("SELECT m FROM Matricula m JOIN FETCH m.alumno a JOIN FETCH a.persona JOIN FETCH m.grupo g JOIN FETCH g.categoria WHERE a.id=:alumnoId")
    List<Matricula> findByAlumnoId(@Param("alumnoId") Long alumnoId);

    @Query("SELECT m FROM Matricula m JOIN FETCH m.alumno a JOIN FETCH a.persona JOIN FETCH m.grupo g JOIN FETCH g.categoria WHERE a.id=:alumnoId AND m.estado=:estado")
    List<Matricula> findByAlumnoIdAndEstado(@Param("alumnoId") Long alumnoId, @Param("estado") EstadoMatricula estado);

    @Query("""
        SELECT m FROM Matricula m JOIN FETCH m.alumno a JOIN FETCH a.persona p
        LEFT JOIN FETCH a.cinturonActual JOIN FETCH m.grupo g JOIN FETCH g.categoria
        WHERE m.estado=:estado AND EXISTS
          (SELECT 1 FROM Horario h WHERE h.grupo=g AND h.id=:horarioId)
        ORDER BY p.apellidos, p.nombres
    """)
    List<Matricula> findByHorarioIdAndEstado(@Param("horarioId") Long horarioId, @Param("estado") EstadoMatricula estado);

    @Query("SELECT COUNT(m) FROM Matricula m WHERE m.estado=:estado AND EXISTS (SELECT 1 FROM Horario h WHERE h.grupo=m.grupo AND h.id=:horarioId)")
    long countByHorarioIdAndEstado(@Param("horarioId") Long horarioId, @Param("estado") EstadoMatricula estado);

    long countByGrupoIdAndEstado(Long grupoId, EstadoMatricula estado);
    long countByEstado(EstadoMatricula estado);

    @Query("SELECT m FROM Matricula m JOIN FETCH m.alumno a JOIN FETCH a.persona JOIN FETCH m.grupo g JOIN FETCH g.categoria WHERE m.estado=:estado AND a.estado=com.utp.gajog.model.EstadoAlumno.ACTIVO")
    List<Matricula> findMatriculasActivasConAlumnoYCategoria(@Param("estado") EstadoMatricula estado);

    @Query(value="""
        SELECT m FROM Matricula m JOIN FETCH m.alumno a JOIN FETCH a.persona p
        LEFT JOIN FETCH a.apoderado ap LEFT JOIN FETCH ap.persona
        JOIN FETCH m.grupo g JOIN FETCH g.categoria c
        WHERE (:estado IS NULL OR m.estado=:estado)
          AND (:categoriaId IS NULL OR c.id=:categoriaId)
          AND (:horarioId IS NULL OR EXISTS (SELECT 1 FROM Horario h WHERE h.grupo=g AND h.id=:horarioId))
          AND (:q IS NULL OR :q='' OR LOWER(CONCAT(p.nombres,' ',p.apellidos)) LIKE LOWER(CONCAT('%',:q,'%')) OR p.dni LIKE CONCAT('%',:q,'%'))
        ORDER BY m.fechaMatricula DESC, m.id DESC
    """, countQuery="""
        SELECT COUNT(m) FROM Matricula m JOIN m.alumno a JOIN a.persona p
        JOIN m.grupo g JOIN g.categoria c
        WHERE (:estado IS NULL OR m.estado=:estado)
          AND (:categoriaId IS NULL OR c.id=:categoriaId)
          AND (:horarioId IS NULL OR EXISTS (SELECT 1 FROM Horario h WHERE h.grupo=g AND h.id=:horarioId))
          AND (:q IS NULL OR :q='' OR LOWER(CONCAT(p.nombres,' ',p.apellidos)) LIKE LOWER(CONCAT('%',:q,'%')) OR p.dni LIKE CONCAT('%',:q,'%'))
    """)
    Page<Matricula> buscarMatriculasFiltradas(@Param("estado") EstadoMatricula estado,
        @Param("categoriaId") Long categoriaId, @Param("horarioId") Long horarioId,
        @Param("q") String q, Pageable pageable);

    @Query("""
        SELECT c.nombre AS categoria, COUNT(DISTINCT m.alumno.id) AS total FROM Matricula m
        JOIN m.grupo g JOIN g.categoria c WHERE m.estado=:estado
        GROUP BY c.id,c.nombre ORDER BY c.edadMinima
    """)
    List<ConteoCategoriaProjection> contarAlumnosActivosPorCategoria(@Param("estado") EstadoMatricula estado);
}