/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Record.java to edit this template
 */
package com.utp.gajog.dto;

import java.time.LocalTime;
import java.util.List;

/**
 *
 * @author JORGE
 */
public record GrupoDisponibleDTO(Long id,
        String nombre,
        long vacantes,
        List<HorarioDTO> horarios) {

    public record HorarioDTO(
            String dia,
            LocalTime inicio,
            LocalTime fin
            ) {

    }
}
