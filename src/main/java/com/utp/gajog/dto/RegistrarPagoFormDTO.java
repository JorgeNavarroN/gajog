package com.utp.gajog.dto;

import com.utp.gajog.model.MedioPago;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

public class RegistrarPagoFormDTO {

    private Long mensualidadId;

    @NotNull(message = "Debe seleccionar un alumno")
    private Long alumnoId;

    @NotNull(message = "El año del periodo es obligatorio")
    @Min(value = 2020, message = "Año inválido")
    @Max(value = 2100, message = "Año inválido")
    private Integer anio;

    @NotNull(message = "El mes del periodo es obligatorio")
    @Min(value = 1, message = "El mes debe estar entre 1 y 12")
    @Max(value = 12, message = "El mes debe estar entre 1 y 12")
    private Integer mes;

    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a S/ 0.00")
    private BigDecimal monto;

    @NotNull(message = "La fecha de pago es obligatoria")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaPago;

    @NotNull(message = "Seleccione el medio de pago")
    private MedioPago medioPago = MedioPago.EFECTIVO;

    @Size(max = 60, message = "El número de operación no puede exceder 60 caracteres")
    private String numeroOperacion;

    @Size(max = 500, message = "La observación no puede exceder 500 caracteres")
    private String observacion;

    private boolean reincorporarAlumno = false;

    private String returnUrl;

    public Long getMensualidadId() {
        return mensualidadId;
    }

    public void setMensualidadId(Long mensualidadId) {
        this.mensualidadId = mensualidadId;
    }

    public Long getAlumnoId() {
        return alumnoId;
    }

    public void setAlumnoId(Long alumnoId) {
        this.alumnoId = alumnoId;
    }

    public Integer getAnio() {
        return anio;
    }

    public void setAnio(Integer anio) {
        this.anio = anio;
    }

    public Integer getMes() {
        return mes;
    }

    public void setMes(Integer mes) {
        this.mes = mes;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }

    public LocalDate getFechaPago() {
        return fechaPago;
    }

    public void setFechaPago(LocalDate fechaPago) {
        this.fechaPago = fechaPago;
    }

    public MedioPago getMedioPago() {
        return medioPago;
    }

    public void setMedioPago(MedioPago medioPago) {
        this.medioPago = medioPago;
    }

    public String getNumeroOperacion() {
        return numeroOperacion;
    }

    public void setNumeroOperacion(String numeroOperacion) {
        this.numeroOperacion = numeroOperacion;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    public boolean isReincorporarAlumno() {
        return reincorporarAlumno;
    }

    public void setReincorporarAlumno(boolean reincorporarAlumno) {
        this.reincorporarAlumno = reincorporarAlumno;
    }

    public String getReturnUrl() {
        return returnUrl;
    }

    public void setReturnUrl(String returnUrl) {
        this.returnUrl = returnUrl;
    }
}