/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.utp.gajog.repository;

import com.utp.gajog.enums.EstadoMatricula;
import com.utp.gajog.model.Matricula;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 *
 * @author JORGE
 */
public interface MatriculaRepository extends JpaRepository<Matricula, Long> {

    List<Matricula> findByAlumno_IdAndEstado(
            Long alumnoId, EstadoMatricula estado);

    List<Matricula> findByGrupo_IdAndEstado(
            Long grupoId, EstadoMatricula estado);

    long countByGrupo_IdAndEstado(
            Long grupoId, EstadoMatricula estado);

    boolean existsByAlumno_IdAndGrupo_IdAndEstado(
            Long alumnoId, Long grupoId, EstadoMatricula estado);

    @EntityGraph(attributePaths = {
        "alumno.persona",
        "alumno.apoderado.persona",
        "grupo.categoria",
        "grupo.horarios"
    })
    @Query("""
    SELECT DISTINCT m
    FROM Matricula m
    JOIN m.alumno a
    JOIN a.persona p
    JOIN m.grupo g
    WHERE (
        :busqueda = ''
        OR LOWER(CONCAT(p.nombres, CONCAT(' ', p.apellidos)))
            LIKE LOWER(CONCAT('%', CONCAT(:busqueda, '%')))
        OR p.dni LIKE CONCAT('%', CONCAT(:busqueda, '%'))
    )
    AND (:categoriaId IS NULL OR g.categoria.id = :categoriaId)
    AND (:grupoId IS NULL OR g.id = :grupoId)
    AND (:estado IS NULL OR m.estado = :estado)
    ORDER BY m.fechaMatricula DESC, m.id DESC
    """)
    List<Matricula> buscarParaListado(
            @Param("busqueda") String busqueda,
            @Param("categoriaId") Long categoriaId,
            @Param("grupoId") Long grupoId,
            @Param("estado") EstadoMatricula estado);
}
