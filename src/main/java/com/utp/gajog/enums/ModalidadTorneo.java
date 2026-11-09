package com.utp.gajog.enums;

/** Modalidad de competencia: combate (kyorugi) o formas (poomsae). */
public enum ModalidadTorneo {
    COMBATE("Combate (Kyorugi)"),
    POOMSAE("Formas (Poomsae)"),
    ROMPIMIENTO("Rompimiento de tablas (Kyokpa)");

    private final String etiqueta;

    ModalidadTorneo(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
