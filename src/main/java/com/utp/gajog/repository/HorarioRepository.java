package com.utp.gajog.repository;

import com.utp.gajog.model.DiaSemana;
import com.utp.gajog.model.Horario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface HorarioRepository extends JpaRepository<Horario, Long> {
    @Query("SELECT h FROM Horario h WHERE h.grupo.categoria.id = :categoriaId AND h.activo = true")
    List<Horario> findByCategoriaId(@Param("categoriaId") Long categoriaId);
    List<Horario> findByDiaSemana(DiaSemana diaSemana);

    @Query("""
        SELECT h FROM Horario h
        JOIN FETCH h.grupo g
        JOIN FETCH g.categoria c
        LEFT JOIN FETCH g.instructor i
        LEFT JOIN FETCH i.persona
        WHERE h.id = :id
    """)
    Optional<Horario> findByIdConBloqueoOptimista(@Param("id") Long id);

    @Query("""
        SELECT h FROM Horario h
        JOIN FETCH h.grupo g
        JOIN FETCH g.categoria c
        LEFT JOIN FETCH g.instructor i
        LEFT JOIN FETCH i.persona
        WHERE h.activo = true AND g.activo = true AND (:categoriaId IS NULL OR c.id = :categoriaId)
          AND (:diaSemana IS NULL OR h.diaSemana = :diaSemana)
        ORDER BY c.edadMinima ASC, h.diaSemana ASC, h.horaInicio ASC
    """)
    List<Horario> buscarHorariosConDetalle(
            @Param("categoriaId") Long categoriaId,
            @Param("diaSemana") DiaSemana diaSemana
    );
}