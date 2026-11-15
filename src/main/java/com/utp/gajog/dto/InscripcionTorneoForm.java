package com.utp.gajog.dto;

import com.utp.gajog.enums.MedioPago;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

/** Datos del formulario de inscripción a un torneo. */
public class InscripcionTorneoForm {

    @NotNull(message = "Seleccione un alumno.")
    @Positive(message = "El alumno no es válido.")
    private Long alumnoId;

    @NotNull(message = "Ingrese el peso del competidor.")
    @DecimalMin(value = "15.0", message = "El peso mínimo es 15 kg.")
    @DecimalMax(value = "150.0", message = "El peso máximo es 150 kg.")
    @Digits(integer = 3, fraction = 1, message = "El peso admite un decimal.")
    private BigDecimal pesoKg;

    @NotNull(message = "Seleccione el medio de pago.")
    private MedioPago medioPago = MedioPago.EFECTIVO;

    public Long getAlumnoId() { return alumnoId; }
    public void setAlumnoId(Long alumnoId) { this.alumnoId = alumnoId; }
    public BigDecimal getPesoKg() { return pesoKg; }
    public void setPesoKg(BigDecimal pesoKg) { this.pesoKg = pesoKg; }
    public MedioPago getMedioPago() { return medioPago; }
    public void setMedioPago(MedioPago medioPago) { this.medioPago = medioPago; }
}
