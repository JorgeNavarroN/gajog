package com.utp.gajog.enums;

import java.math.BigDecimal;

/** Técnicas de rompimiento con su coeficiente de dificultad. */
public enum TecnicaRompimiento {
    PUNO("Puño (Jirugi)", "1.0"),
    CANTO_MANO("Canto de mano (Sonkal)", "1.2"),
    PATADA_FRONTAL("Patada frontal (Ap chagi)", "1.5"),
    PATADA_LATERAL("Patada lateral (Yeop chagi)", "1.5"),
    PATADA_SALTO("Patada con salto (Twimyo chagi)", "2.0"),
    PATADA_GIRO_SALTO("Patada giratoria con salto", "2.5");

    private final String etiqueta;
    private final BigDecimal coeficiente;

    TecnicaRompimiento(String etiqueta, String coeficiente) {
        this.etiqueta = etiqueta;
        this.coeficiente = new BigDecimal(coeficiente);
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public BigDecimal getCoeficiente() {
        return coeficiente;
    }
}
