package com.utp.gajog.dto;

import com.utp.gajog.model.DiaSemana;
import java.time.LocalTime;

public record OpcionHorarioAsistenciaDTO(
        Long horarioId,
        String categoriaNombre,
        DiaSemana diaSemana,
        LocalTime horaInicio,
        LocalTime horaFin,
        String sala
) {}