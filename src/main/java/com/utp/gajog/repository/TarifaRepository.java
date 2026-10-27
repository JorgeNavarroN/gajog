/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.utp.gajog.repository;

import com.utp.gajog.enums.TipoPago;
import com.utp.gajog.model.Tarifa;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 *
 * @author JORGE
 */
public interface TarifaRepository extends JpaRepository<Tarifa, Long> {

    List<Tarifa> findByTipoPagoAndActivoTrue(TipoPago tipoPago);
}
