/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.utp.gajog.repository;

import com.utp.gajog.model.SesionClase;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 *
 * @author JORGE
 */
public interface SesionClaseRepository extends JpaRepository<SesionClase, Long> {

    List<SesionClase> findByFecha(LocalDate fecha);

    List<SesionClase> findByHorario_IdAndFecha(
            Long horarioId, LocalDate fecha);
}
