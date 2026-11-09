package com.utp.gajog.model;

import com.utp.gajog.enums.EstadoCombate;
import com.utp.gajog.enums.TipoVictoria;
import jakarta.persistence.*;

/**
 * Un combate de la llave. Los rojos y azules pueden ser nulos mientras
 * la llave espera al ganador del combate anterior.
 */
@Entity
@Table(name = "combates")
public class Combate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "torneo_id", nullable = false)
    private Torneo torneo;

    @Column(nullable = false, length = 80)
    private String categoria;

    /** 1 = primera ronda, 2 = siguiente, etc. */
    @Column(nullable = false)
    private Integer ronda;

    /** Nombre visible de la ronda: Cuartos, Semifinal, Final... */
    @Column(name = "nombre_ronda", nullable = false, length = 30)
    private String nombreRonda;

    /** Posición del combate dentro de su ronda (0, 1, 2...). */
    @Column(nullable = false)
    private Integer posicion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "azul_id")
    private InscripcionTorneo azul;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rojo_id")
    private InscripcionTorneo rojo;

    @Column(name = "round_actual", nullable = false)
    private Integer roundActual = 1;

    @Column(name = "rounds_azul", nullable = false)
    private Integer roundsAzul = 0;

    @Column(name = "rounds_rojo", nullable = false)
    private Integer roundsRojo = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private EstadoCombate estado = EstadoCombate.PENDIENTE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ganador_id")
    private InscripcionTorneo ganador;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_victoria", length = 20)
    private TipoVictoria tipoVictoria;

    public Combate() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Torneo getTorneo() { return torneo; }
    public void setTorneo(Torneo torneo) { this.torneo = torneo; }
    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    public Integer getRonda() { return ronda; }
    public void setRonda(Integer ronda) { this.ronda = ronda; }
    public String getNombreRonda() { return nombreRonda; }
    public void setNombreRonda(String nombreRonda) { this.nombreRonda = nombreRonda; }
    public Integer getPosicion() { return posicion; }
    public void setPosicion(Integer posicion) { this.posicion = posicion; }
    public InscripcionTorneo getAzul() { return azul; }
    public void setAzul(InscripcionTorneo azul) { this.azul = azul; }
    public InscripcionTorneo getRojo() { return rojo; }
    public void setRojo(InscripcionTorneo rojo) { this.rojo = rojo; }
    public Integer getRoundActual() { return roundActual; }
    public void setRoundActual(Integer roundActual) { this.roundActual = roundActual; }
    public Integer getRoundsAzul() { return roundsAzul; }
    public void setRoundsAzul(Integer roundsAzul) { this.roundsAzul = roundsAzul; }
    public Integer getRoundsRojo() { return roundsRojo; }
    public void setRoundsRojo(Integer roundsRojo) { this.roundsRojo = roundsRojo; }
    public EstadoCombate getEstado() { return estado; }
    public void setEstado(EstadoCombate estado) { this.estado = estado; }
    public InscripcionTorneo getGanador() { return ganador; }
    public void setGanador(InscripcionTorneo ganador) { this.ganador = ganador; }
    public TipoVictoria getTipoVictoria() { return tipoVictoria; }
    public void setTipoVictoria(TipoVictoria tipoVictoria) { this.tipoVictoria = tipoVictoria; }
}
