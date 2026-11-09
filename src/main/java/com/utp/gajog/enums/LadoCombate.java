package com.utp.gajog.enums;

/** Lados del tatami: CHONG (azul) y HONG (rojo). */
public enum LadoCombate {
    AZUL("CHONG"),
    ROJO("HONG");

    private final String nombre;

    LadoCombate(String nombre) {
        this.nombre = nombre;
    }

    public String getNombre() {
        return nombre;
    }

    public LadoCombate opuesto() {
        return this == AZUL ? ROJO : AZUL;
    }
}
