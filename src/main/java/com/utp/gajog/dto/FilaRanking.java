package com.utp.gajog.dto;

/** Una fila del ranking anual de medallas. */
public record FilaRanking(
        int posicion,
        String alumno,
        int oros,
        int platas,
        int bronces,
        int puntos) {
}
