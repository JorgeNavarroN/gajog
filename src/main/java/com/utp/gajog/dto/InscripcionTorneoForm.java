package com.utp.gajog.dto;

import com.utp.gajog.enums.MedioPago;
import java.math.BigDecimal;

/** Datos del formulario de inscripción a un torneo. */
public class InscripcionTorneoForm {

    private Long alumnoId;
    private BigDecimal pesoKg;
    private MedioPago medioPago = MedioPago.EFECTIVO;

    public Long getAlumnoId() { return alumnoId; }
    public void setAlumnoId(Long alumnoId) { this.alumnoId = alumnoId; }
    public BigDecimal getPesoKg() { return pesoKg; }
    public void setPesoKg(BigDecimal pesoKg) { this.pesoKg = pesoKg; }
    public MedioPago getMedioPago() { return medioPago; }
    public void setMedioPago(MedioPago medioPago) { this.medioPago = medioPago; }
}
