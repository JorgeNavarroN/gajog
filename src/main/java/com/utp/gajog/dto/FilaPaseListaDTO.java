package com.utp.gajog.dto;

import com.utp.gajog.model.EstadoAsistencia;
import com.utp.gajog.model.MetodoRegistroAsistencia;
import java.time.LocalDateTime;

public record FilaPaseListaDTO(
        Long alumnoId,
        String nombreCompleto,
        String dniMostrado,
        String cinturonNombre,
        String cinturonColor,
        String estadoMensualidadEtiqueta,
        String estadoMensualidadVariante,
        EstadoAsistencia estadoActual,
        LocalDateTime horaIngreso,
        int minutosTarde,
        MetodoRegistroAsistencia metodoRegistro,
        String observacion
) {
    public boolean registradoPorDni() {
        return metodoRegistro == MetodoRegistroAsistencia.DNI && horaIngreso != null;
    }
}