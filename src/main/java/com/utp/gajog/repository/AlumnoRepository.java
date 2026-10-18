package com.utp.gajog.repository;

import com.utp.gajog.model.Alumno;
import com.utp.gajog.model.EstadoAlumno;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AlumnoRepository extends JpaRepository<Alumno, Long> {
    Optional<Alumno> findByPersonaDni(String dni);
    List<Alumno> findByEstado(EstadoAlumno estado);
    List<Alumno> findByApoderadoId(Long apoderadoId);
    List<Alumno> findByCinturonActualId(Long cinturonId);
}