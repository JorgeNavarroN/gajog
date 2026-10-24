package com.utp.gajog.dto;

import com.utp.gajog.model.EstadoAlumno;
import java.math.BigDecimal;

public record AlumnoPagoAutocompletadoDTO(
        Long id,
        String nombreCompleto,
        String dniEnmascarado,
        boolean menorDeEdad,
        EstadoAlumno estado,
        String categoriaNombre,
        BigDecimal tarifaSugerida
) {
    public static String enmascararDni(String dni, boolean esMenor) {
        if (dni == null || dni.isBlank()) {
            return "-";
        }
        if (!esMenor || dni.length() <= 4) {
            return dni;
        }
        return "***" + dni.substring(dni.length() - 4);
    }
}