package com.utp.gajog.dto;

/** Estado del marcador que se envía a la pantalla (formato JSON). */
public record MarcadorDTO(
        Long id,
        String categoria,
        String nombreRonda,
        String estado,
        String nombreAzul,
        String nombreRojo,
        boolean listo,
        int roundActual,
        boolean roundDeOro,
        int puntosAzul,
        int puntosRojo,
        int faltasAzul,
        int faltasRojo,
        int roundsAzul,
        int roundsRojo,
        String ganador,
        String tipoVictoria,
        String mensaje) {
}
