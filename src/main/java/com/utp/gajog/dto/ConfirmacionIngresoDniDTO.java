package com.utp.gajog.dto;

import com.utp.gajog.model.EstadoAsistencia;
import java.time.LocalDateTime;
import java.util.List;

public record ConfirmacionIngresoDniDTO(
        boolean requiereSeleccionHorario,
        String dniConsultado,
        Long alumnoId,
        String nombreCompleto,
        String categoriaNombre,
        String cinturonNombre,
        String cinturonColor,
        String grupoHorarioTexto,
        LocalDateTime horaIngreso,
        EstadoAsistencia estadoAsistencia,
        int minutosTarde,
        String estadoMensualidadEtiqueta,
        String estadoMensualidadVariante,
        boolean alertaMensualidad,
        List<OpcionHorarioAsistenciaDTO> horariosCandidatos
) {}