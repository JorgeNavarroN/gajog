/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.utp.gajog.repository;

import com.utp.gajog.model.Asistencia;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 *
 * @author JORGE
 */
public interface AsistenciaRepository extends JpaRepository<Asistencia, Long> {

    Optional<Asistencia> findBySesion_IdAndAlumno_Id(
            Long sesionId, Long alumnoId);

    List<Asistencia> findBySesion_Id(Long sesionId);

    List<Asistencia> findByAlumno_IdOrderByFechaRegistroDesc(
            Long alumnoId);
}
