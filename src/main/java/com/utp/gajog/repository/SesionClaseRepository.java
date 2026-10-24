package com.utp.gajog.repository;

import com.utp.gajog.model.EstadoSesionClase;
import com.utp.gajog.model.SesionClase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SesionClaseRepository extends JpaRepository<SesionClase, Long> {
    List<SesionClase> findByFecha(LocalDate fecha);
    List<SesionClase> findByInstructorIdAndFecha(Long instructorId, LocalDate fecha);
    List<SesionClase> findByEstado(EstadoSesionClase estado);

    Optional<SesionClase> findFirstByHorarioIdAndFechaOrderByIdDesc(Long horarioId, LocalDate fecha);

    @Query("""
        SELECT s FROM SesionClase s
        JOIN FETCH s.horario h
        JOIN FETCH h.categoria c
        JOIN FETCH s.instructor i
        JOIN FETCH i.persona
        WHERE s.id = :id
    """)
    Optional<SesionClase> findByIdConDetalle(@Param("id") Long id);

    @Query("""
        SELECT s FROM SesionClase s
        JOIN FETCH s.horario h
        JOIN FETCH h.categoria c
        JOIN FETCH s.instructor i
        JOIN FETCH i.persona
        WHERE (:fecha IS NULL OR s.fecha = :fecha)
          AND (:categoriaId IS NULL OR c.id = :categoriaId)
          AND (:horarioId IS NULL OR h.id = :horarioId)
        ORDER BY s.fecha DESC, s.horaInicio DESC
    """)
    List<SesionClase> buscarSesionesPorFiltro(
            @Param("fecha") LocalDate fecha,
            @Param("categoriaId") Long categoriaId,
            @Param("horarioId") Long horarioId
    );

    @Query("""
        SELECT s FROM SesionClase s
        JOIN FETCH s.horario h
        WHERE s.fecha < :fechaLimite
          AND s.estado IN :estadosAbiertos
    """)
    List<SesionClase> findSesionesPasadasAbiertas(
            @Param("fechaLimite") LocalDate fechaLimite,
            @Param("estadosAbiertos") Collection<EstadoSesionClase> estadosAbiertos
    );

    @Query("""
        SELECT s FROM SesionClase s
        JOIN FETCH s.horario h
        JOIN FETCH h.categoria
        JOIN FETCH s.instructor i
        JOIN FETCH i.persona
        ORDER BY s.fecha DESC, s.horaInicio DESC
        LIMIT 5
    """)
    List<SesionClase> findTop5RecientesConDetalle();
}