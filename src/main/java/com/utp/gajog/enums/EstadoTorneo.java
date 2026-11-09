package com.utp.gajog.enums;

/** Ciclo de vida de un torneo. */
public enum EstadoTorneo {
    INSCRIPCION_ABIERTA("Inscripción abierta"),
    EN_CURSO("En curso"),
    FINALIZADO("Finalizado");

    private final String etiqueta;

    EstadoTorneo(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
