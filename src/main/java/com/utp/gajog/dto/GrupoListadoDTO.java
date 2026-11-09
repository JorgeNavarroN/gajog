/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Record.java to edit this template
 */
package com.utp.gajog.dto;

import java.util.List;

/**
 *
 * @author JORGE
 */
public record GrupoListadoDTO(
        Long id,
        String nombre,
        String categoria,
        String instructor,
        int cupoMaximo,
        long ocupados,
        long vacantes,
        boolean activo,
        List<String> horarios) {

}
