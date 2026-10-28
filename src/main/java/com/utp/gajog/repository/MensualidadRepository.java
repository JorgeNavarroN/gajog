/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.utp.gajog.repository;

import com.utp.gajog.enums.EstadoMensualidad;
import com.utp.gajog.model.Mensualidad;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 *
 * @author JORGE
 */
public interface MensualidadRepository extends JpaRepository<Mensualidad, Long> {

    Optional<Mensualidad> findByAlumno_IdAndAnioAndMes(
            Long alumnoId, Integer anio, Integer mes);

    List<Mensualidad> findByAlumno_IdOrderByAnioDescMesDesc(
            Long alumnoId);

    List<Mensualidad> findByEstadoAndFechaVencimientoBefore(
            EstadoMensualidad estado, LocalDate fecha);

    @EntityGraph(attributePaths = {"alumno.persona"})
    List<Mensualidad> findByAnioAndMesOrderByAlumno_Persona_ApellidosAsc(
            Integer anio, Integer mes);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM Mensualidad m WHERE m.id = :id")
    Optional<Mensualidad> findByIdConBloqueo(@Param("id") Long id);
}
