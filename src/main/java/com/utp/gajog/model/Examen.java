/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.utp.gajog.model;

import com.utp.gajog.enums.ResultadoExamen;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;


@Entity
@Table(name = "examenes")
public class Examen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "alumno_id", nullable = false)
    private Alumno alumno;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cinturon_objetivo_id", nullable = false)
    private Cinturon cinturonObjetivo;

    @Column(name = "fecha_examen", nullable = false)
    private LocalDate fechaExamen;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private ResultadoExamen resultado
            = ResultadoExamen.PENDIENTE;

    @Column(name = "puntaje_total", precision = 6, scale = 2)
    private BigDecimal puntajeTotal;

    public Examen() {
    }

    // Generar getters y setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Alumno getAlumno() {
        return alumno;
    }

    public void setAlumno(Alumno alumno) {
        this.alumno = alumno;
    }

    public Cinturon getCinturonObjetivo() {
        return cinturonObjetivo;
    }

    public void setCinturonObjetivo(Cinturon cinturonObjetivo) {
        this.cinturonObjetivo = cinturonObjetivo;
    }

    public LocalDate getFechaExamen() {
        return fechaExamen;
    }

    public void setFechaExamen(LocalDate fechaExamen) {
        this.fechaExamen = fechaExamen;
    }

    public ResultadoExamen getResultado() {
        return resultado;
    }

    public void setResultado(ResultadoExamen resultado) {
        this.resultado = resultado;
    }

    public BigDecimal getPuntajeTotal() {
        return puntajeTotal;
    }

    public void setPuntajeTotal(BigDecimal puntajeTotal) {
        this.puntajeTotal = puntajeTotal;
    }
}
