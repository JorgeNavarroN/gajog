package com.utp.gajog.repository;

import com.utp.gajog.model.EstadoMatricula;
import com.utp.gajog.model.Matricula;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MatriculaRepository extends JpaRepository<Matricula, Long> {
    List<Matricula> findByAlumnoId(Long alumnoId);
    List<Matricula> findByAlumnoIdAndEstado(Long alumnoId, EstadoMatricula estado);
    List<Matricula> findByHorarioIdAndEstado(Long horarioId, EstadoMatricula estado);
}