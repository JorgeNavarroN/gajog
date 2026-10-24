package com.utp.gajog.model;

public enum EstadoMensualidad {
    POR_VENCER("Por vencer"),
    PAGADA("Pagado"),
    VENCIDA("Vencida – en riesgo de retiro");

    private final String etiqueta;

    EstadoMensualidad(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}