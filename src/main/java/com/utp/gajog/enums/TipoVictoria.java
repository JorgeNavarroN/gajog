package com.utp.gajog.enums;

public enum TipoVictoria {
    PUNTOS("Por puntos"),
    SUPERIORIDAD("Superioridad técnica"),
    PUNTO_DE_ORO("Punto de oro"),
    FALTAS("Descalificación por faltas"),
    WO("W.O. (no se presentó)"),
    BYE("Pase directo");

    private final String etiqueta;

    TipoVictoria(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
