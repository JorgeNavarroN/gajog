package com.utp.gajog.model;

import com.utp.gajog.enums.TecnicaRompimiento;
import jakarta.persistence.*;
import java.math.BigDecimal;

/** Un intento de rompimiento: una técnica sobre cierta cantidad de tablas. */
@Entity
@Table(name = "intentos_rompimiento",
        uniqueConstraints = @UniqueConstraint(columnNames = {"inscripcion_id", "numero"}))
public class IntentoRompimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inscripcion_id", nullable = false)
    private InscripcionTorneo inscripcion;

    /** Orden del intento del competidor: 1, 2, 3. */
    @Column(nullable = false)
    private Integer numero;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TecnicaRompimiento tecnica;

    @Column(name = "tablas_intentadas", nullable = false)
    private Integer tablasIntentadas;

    @Column(name = "tablas_rotas", nullable = false)
    private Integer tablasRotas;

    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal puntos;

    public IntentoRompimiento() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public InscripcionTorneo getInscripcion() { return inscripcion; }
    public void setInscripcion(InscripcionTorneo inscripcion) { this.inscripcion = inscripcion; }
    public Integer getNumero() { return numero; }
    public void setNumero(Integer numero) { this.numero = numero; }
    public TecnicaRompimiento getTecnica() { return tecnica; }
    public void setTecnica(TecnicaRompimiento tecnica) { this.tecnica = tecnica; }
    public Integer getTablasIntentadas() { return tablasIntentadas; }
    public void setTablasIntentadas(Integer tablasIntentadas) { this.tablasIntentadas = tablasIntentadas; }
    public Integer getTablasRotas() { return tablasRotas; }
    public void setTablasRotas(Integer tablasRotas) { this.tablasRotas = tablasRotas; }
    public BigDecimal getPuntos() { return puntos; }
    public void setPuntos(BigDecimal puntos) { this.puntos = puntos; }
}
