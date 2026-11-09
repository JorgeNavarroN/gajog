package com.utp.gajog.model;

import com.utp.gajog.enums.LadoCombate;
import com.utp.gajog.enums.TipoEventoCombate;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Cada punto o falta que el juez anota en el marcador. Se guarda cada
 * evento (no solo el total) para poder deshacer y para tener historial.
 */
@Entity
@Table(name = "eventos_combate")
public class EventoCombate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "combate_id", nullable = false)
    private Combate combate;

    @Column(name = "numero_round", nullable = false)
    private Integer numeroRound;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private LadoCombate lado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoEventoCombate tipo;

    /** Un evento deshecho no se borra: queda anulado. */
    @Column(nullable = false)
    private boolean anulado = false;

    @Column(name = "registrado_en", nullable = false)
    private LocalDateTime registradoEn;

    @PrePersist
    public void antesDeGuardar() {
        if (registradoEn == null) {
            registradoEn = LocalDateTime.now();
        }
    }

    public EventoCombate() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Combate getCombate() { return combate; }
    public void setCombate(Combate combate) { this.combate = combate; }
    public Integer getNumeroRound() { return numeroRound; }
    public void setNumeroRound(Integer numeroRound) { this.numeroRound = numeroRound; }
    public LadoCombate getLado() { return lado; }
    public void setLado(LadoCombate lado) { this.lado = lado; }
    public TipoEventoCombate getTipo() { return tipo; }
    public void setTipo(TipoEventoCombate tipo) { this.tipo = tipo; }
    public boolean isAnulado() { return anulado; }
    public void setAnulado(boolean anulado) { this.anulado = anulado; }
    public LocalDateTime getRegistradoEn() { return registradoEn; }
    public void setRegistradoEn(LocalDateTime registradoEn) { this.registradoEn = registradoEn; }
}
