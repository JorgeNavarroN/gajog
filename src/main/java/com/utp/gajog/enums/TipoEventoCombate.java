package com.utp.gajog.enums;

/** Acciones que el juez puede anotar en el marcador. */
public enum TipoEventoCombate {
    PUNTO_1(1),   // puño al tronco
    PUNTO_2(2),   // patada al tronco
    PUNTO_3(3),   // patada a la cabeza
    FALTA(0);     // gam-jeom: suma 1 punto al rival

    private final int puntos;

    TipoEventoCombate(int puntos) {
        this.puntos = puntos;
    }

    public int getPuntos() {
        return puntos;
    }
}
