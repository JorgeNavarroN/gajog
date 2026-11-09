package com.utp.gajog.dto;

import com.utp.gajog.enums.ModalidadTorneo;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

/** Datos del formulario "Nuevo torneo". */
public class TorneoForm {

    private String nombre;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fecha;

    private String sede;
    private ModalidadTorneo modalidad = ModalidadTorneo.COMBATE;
    private BigDecimal cuotaInscripcion = new BigDecimal("30.00");

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    public String getSede() { return sede; }
    public void setSede(String sede) { this.sede = sede; }
    public ModalidadTorneo getModalidad() { return modalidad; }
    public void setModalidad(ModalidadTorneo modalidad) { this.modalidad = modalidad; }
    public BigDecimal getCuotaInscripcion() { return cuotaInscripcion; }
    public void setCuotaInscripcion(BigDecimal cuotaInscripcion) { this.cuotaInscripcion = cuotaInscripcion; }
}
