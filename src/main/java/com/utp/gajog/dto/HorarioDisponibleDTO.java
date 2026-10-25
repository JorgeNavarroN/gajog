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
        int cuposDisponibles,
        String grupoNombre,
        String horariosTexto
) {
    public HorarioDisponibleDTO(Long id, Long categoriaId, String categoriaNombre,
            DiaSemana diaSemana, LocalTime horaInicio, LocalTime horaFin, String sala,
            String instructorNombre, int cupoMaximo, long inscritosActivos, int cuposDisponibles) {
        this(id, categoriaId, categoriaNombre, diaSemana, horaInicio, horaFin, sala,
                instructorNombre, cupoMaximo, inscritosActivos, cuposDisponibles,
                categoriaNombre, diaSemana + " " + horaInicio + " - " + horaFin);
    }
    public boolean tieneCupo() {
        return cuposDisponibles > 0;
    }
}