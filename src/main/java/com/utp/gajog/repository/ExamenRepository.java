package com.utp.gajog.repository;

import com.utp.gajog.model.Examen;
import com.utp.gajog.model.ResultadoExamen;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ExamenRepository extends JpaRepository<Examen, Long> {
    List<Examen> findByAlumnoIdOrderByFechaExamenDesc(Long alumnoId);
    List<Examen> findByResultado(ResultadoExamen resultado);
}