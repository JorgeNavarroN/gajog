/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.utp.gajog.repository;

import com.utp.gajog.enums.DiaSemana;
import com.utp.gajog.model.Horario;
import jakarta.persistence.LockModeType;
import java.time.LocalTime;
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
public interface HorarioRepository extends JpaRepository<Horario, Long> {

    List<Horario> findByGrupo_Id(Long grupoId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT h FROM Horario h WHERE h.id = :id")
    Optional<Horario> findByIdConBloqueo(@Param("id") Long id);

    @EntityGraph(attributePaths = {"grupo.categoria"})
    List<Horario> findByGrupo_ActivoTrueOrderByGrupo_NombreAscDiaSemanaAscHoraInicioAsc();

    @Query("""
    SELECT COUNT(h)
    FROM Horario h
    WHERE h.grupo.activo = true
      AND h.grupo.instructor.id = :instructorId
      AND h.diaSemana = :dia
      AND h.horaInicio < :fin
      AND h.horaFin > :inicio
    """)
    long contarCrucesInstructor(
            @Param("instructorId") Long instructorId,
            @Param("dia") DiaSemana dia,
            @Param("inicio") LocalTime inicio,
            @Param("fin") LocalTime fin);
}
