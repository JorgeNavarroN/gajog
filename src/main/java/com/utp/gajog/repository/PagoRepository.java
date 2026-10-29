/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.utp.gajog.repository;

import com.utp.gajog.enums.EstadoPago;
import com.utp.gajog.enums.TipoPago;
import com.utp.gajog.model.Pago;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 *
 * @author JORGE
 */
public interface PagoRepository extends JpaRepository<Pago, Long> {

    List<Pago> findByAlumno_IdOrderByFechaPagoDesc(Long alumnoId);

    List<Pago> findByMensualidad_IdAndEstado(
            Long mensualidadId, EstadoPago estado);

    List<Pago> findByExamen_IdAndEstado(
            Long examenId, EstadoPago estado);

    @EntityGraph(attributePaths = {
            "alumno.persona",
            "mensualidad"
    })
    List<Pago> findByMensualidad_AnioAndMensualidad_MesAndTipoPagoAndEstadoOrderByFechaPagoDesc(
            Integer anio,
            Integer mes,
            TipoPago tipoPago,
            EstadoPago estado);
}
