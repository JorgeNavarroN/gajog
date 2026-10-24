package com.utp.gajog.dto;

import com.utp.gajog.model.DiaSemana;
import java.time.LocalTime;

public record HorarioDisponibleDTO(
        Long id,
        Long categoriaId,
        String categoriaNombre,
        DiaSemana diaSemana,
        LocalTime horaInicio,
        LocalTime horaFin,
        String sala,
        String instructorNombre,
        int cupoMaximo,
        long inscritosActivos,
        int cuposDisponibles
) {
    public boolean tieneCupo() {
        return cuposDisponibles > 0;
    }
}