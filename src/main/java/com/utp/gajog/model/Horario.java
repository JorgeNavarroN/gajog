package com.utp.gajog.model;

import jakarta.persistence.*;
import java.time.LocalTime;

@Entity
@Table(name = "horarios")
public class Horario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "grupo_id", nullable = false)
    private Grupo grupo;

    @Column(nullable = false)
    private Boolean activo = true;

    @Transient private Categoria categoriaLegacy;
    @Transient private Instructor instructorLegacy;
    @Transient private Integer cupoLegacy = 20;

    @Enumerated(EnumType.STRING)
    @Column(name = "dia_semana", nullable = false, length = 15)
    private DiaSemana diaSemana;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

@NotBlank
    @Column(name = "sala", nullable = false, length = 60)
    private String sala = "Sala Principal";

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    public Horario() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Grupo getGrupo() { return grupo; }
    public void setGrupo(Grupo grupo) { this.grupo = grupo; }
    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }
    @Transient
    public Categoria getCategoria() { return grupo != null ? grupo.getCategoria() : categoriaLegacy; }
    public void setCategoria(Categoria categoria) { this.categoriaLegacy = categoria; }

    @Transient
    public Instructor getInstructor() { return grupo != null ? grupo.getInstructor() : instructorLegacy; }
    public void setInstructor(Instructor instructor) { this.instructorLegacy = instructor; }

    public DiaSemana getDiaSemana() {
        return diaSemana;
    }

    public void setDiaSemana(DiaSemana diaSemana) {
        this.diaSemana = diaSemana;
    }

    public LocalTime getHoraInicio() {
        return horaInicio;
    }

    public void setHoraInicio(LocalTime horaInicio) {
        this.horaInicio = horaInicio;
    }

    public LocalTime getHoraFin() {
        return horaFin;
    }

    public void setHoraFin(LocalTime horaFin) {
        this.horaFin = horaFin;
    }

    @Transient
    public Integer getCupoMaximo() { return grupo != null ? grupo.getCupoMaximo() : cupoLegacy; }
    public void setCupoMaximo(Integer cupoMaximo) { this.cupoLegacy = cupoMaximo; }

    public String getSala() {
        return sala;
    }

    public void setSala(String sala) {
        this.sala = sala;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}