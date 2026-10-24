package com.utp.gajog.repository;

import com.utp.gajog.model.HistorialCinturones;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface HistorialCinturonesRepository extends JpaRepository<HistorialCinturones, Long> {

    @Query("""
        SELECT h FROM HistorialCinturones h
        JOIN FETCH h.cinturon c
        LEFT JOIN FETCH h.examen e
        WHERE h.alumno.id = :alumnoId
        ORDER BY h.fechaObtencion DESC, h.id DESC
    """)
    List<HistorialCinturones> findByAlumnoIdOrderByFechaObtencionDesc(@Param("alumnoId") Long alumnoId);
}