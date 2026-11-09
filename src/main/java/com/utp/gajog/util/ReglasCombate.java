package com.utp.gajog.util;

/**
 * Reglas puras del marcador de combate (sin base de datos).
 * Los puntos de un lado en un round son: los puntos que anotó
 * más 1 por cada falta (gam-jeom) del rival.
 */
public final class ReglasCombate {

    public static final int ROUNDS_TOTALES = 3;
    public static final int ROUNDS_PARA_GANAR = 2;
    /** Número que se usa para el round extra de punto de oro. */
    public static final int ROUND_DE_ORO = ROUNDS_TOTALES + 1;

    private ReglasCombate() {
    }

    /** Puntos de un lado en un round. */
    public static int puntosRound(int puntosAnotados, int faltasDelRival) {
        return puntosAnotados + faltasDelRival;
    }

    /**
     * Ganador de un round: 1 si gana el azul, 2 si gana el rojo,
     * 0 si hay empate.
     */
    public static int ganadorRound(int puntosAzul, int puntosRojo) {
        return Integer.compare(puntosAzul, puntosRojo) == 0
                ? 0 : (puntosAzul > puntosRojo ? 1 : 2);
    }

    /** ¿Hay superioridad técnica (diferencia suficiente para terminar)? */
    public static boolean haySuperioridad(int puntosAzul, int puntosRojo, int diferenciaMinima) {
        return Math.abs(puntosAzul - puntosRojo) >= diferenciaMinima;
    }

    /** ¿Se descalifica por acumular demasiadas faltas? */
    public static boolean descalificadoPorFaltas(int faltasTotales, int maximoFaltas) {
        return faltasTotales >= maximoFaltas;
    }

    /**
     * Decide si el combate ya tiene ganador por rounds.
     *
     * @return 1 si gana el azul, 2 si gana el rojo, 0 si aún no se define
     */
    public static int ganadorPorRounds(int roundsAzul, int roundsRojo) {
        if (roundsAzul >= ROUNDS_PARA_GANAR) {
            return 1;
        }
        if (roundsRojo >= ROUNDS_PARA_GANAR) {
            return 2;
        }
        return 0;
    }
}
