package com.utp.gajog.repository;

import com.utp.gajog.model.ObservacionProgreso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ObservacionProgresoRepository extends JpaRepository<ObservacionProgreso, Long> {

    @Query("""
        SELECT o FROM ObservacionProgreso o
        JOIN FETCH o.instructor i
        JOIN FETCH i.persona
        WHERE o.alumno.id = :alumnoId
        ORDER BY o.fecha DESC, o.id DESC
    """)
    List<ObservacionProgreso> findByAlumnoIdOrderByFechaDesc(@Param("alumnoId") Long alumnoId);

    List<ObservacionProgreso> findByInstructorIdOrderByFechaDesc(Long instructorId);
}