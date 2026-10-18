package com.utp.gajog.repository;

import com.utp.gajog.model.ObservacionProgreso;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ObservacionProgresoRepository extends JpaRepository<ObservacionProgreso, Long> {
    List<ObservacionProgreso> findByAlumnoIdOrderByFechaDesc(Long alumnoId);
    List<ObservacionProgreso> findByInstructorIdOrderByFechaDesc(Long instructorId);
}