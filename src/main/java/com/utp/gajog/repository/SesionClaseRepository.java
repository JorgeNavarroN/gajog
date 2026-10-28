/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.utp.gajog.repository;

import com.utp.gajog.model.SesionClase;
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
public interface SesionClaseRepository extends JpaRepository<SesionClase, Long> {

    List<SesionClase> findByFecha(LocalDate fecha);

    List<SesionClase> findByHorario_IdAndFecha(
            Long horarioId, LocalDate fecha);

    List<SesionClase> findByHorario_Grupo_IdAndFechaOrderByHoraInicioAsc(
            Long grupoId,
            LocalDate fecha);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SesionClase s WHERE s.id = :id")
    Optional<SesionClase> findByIdConBloqueo(@Param("id") Long id);

    @EntityGraph(attributePaths = {
        "horario.grupo.categoria",
        "instructor.persona"
    })
    @Query("SELECT s FROM SesionClase s WHERE s.id = :id")
    Optional<SesionClase> buscarParaPantalla(@Param("id") Long id);
}
