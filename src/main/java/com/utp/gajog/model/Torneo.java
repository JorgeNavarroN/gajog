package com.utp.gajog.model;

import com.utp.gajog.enums.EstadoTorneo;
import com.utp.gajog.enums.ModalidadTorneo;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Un torneo de taekwondo (combate o poomsae). */
@Entity
@Table(name = "torneos")
public class Torneo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false, length = 150)
    private String sede;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private ModalidadTorneo modalidad;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoTorneo estado = EstadoTorneo.INSCRIPCION_ABIERTA;

    @Column(name = "cuota_inscripcion", nullable = false, precision = 10, scale = 2)
    private BigDecimal cuotaInscripcion = BigDecimal.ZERO;

    public Torneo() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    public String getSede() { return sede; }
    public void setSede(String sede) { this.sede = sede; }
    public ModalidadTorneo getModalidad() { return modalidad; }
    public void setModalidad(ModalidadTorneo modalidad) { this.modalidad = modalidad; }
    public EstadoTorneo getEstado() { return estado; }
    public void setEstado(EstadoTorneo estado) { this.estado = estado; }
    public BigDecimal getCuotaInscripcion() { return cuotaInscripcion; }
    public void setCuotaInscripcion(BigDecimal cuotaInscripcion) { this.cuotaInscripcion = cuotaInscripcion; }
}
