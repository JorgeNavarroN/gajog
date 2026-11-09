package com.utp.gajog.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

/** Nota de un juez para un competidor de poomsae. */
@Entity
@Table(name = "calificaciones_poomsae",
        uniqueConstraints = @UniqueConstraint(columnNames = {"inscripcion_id", "juez_numero"}))
public class CalificacionPoomsae {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inscripcion_id", nullable = false)
    private InscripcionTorneo inscripcion;

    @Column(name = "juez_numero", nullable = false)
    private Integer juezNumero;

    @Column(nullable = false, precision = 4, scale = 2)
    private BigDecimal exactitud;

    @Column(nullable = false, precision = 4, scale = 2)
    private BigDecimal presentacion;

    public CalificacionPoomsae() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public InscripcionTorneo getInscripcion() { return inscripcion; }
    public void setInscripcion(InscripcionTorneo inscripcion) { this.inscripcion = inscripcion; }
    public Integer getJuezNumero() { return juezNumero; }
    public void setJuezNumero(Integer juezNumero) { this.juezNumero = juezNumero; }
    public BigDecimal getExactitud() { return exactitud; }
    public void setExactitud(BigDecimal exactitud) { this.exactitud = exactitud; }
    public BigDecimal getPresentacion() { return presentacion; }
    public void setPresentacion(BigDecimal presentacion) { this.presentacion = presentacion; }

    public BigDecimal getTotal() {
        return exactitud.add(presentacion);
    }
}
