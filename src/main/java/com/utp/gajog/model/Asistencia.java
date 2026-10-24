package com.utp.gajog.model;

import com.utp.gajog.config.TimeConfig;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "asistencias",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_asistencia_sesion_alumno",
                    columnNames = {"sesion_id", "alumno_id"}
            )
        }
)
public class Asistencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sesion_id", nullable = false)
    private SesionClase sesion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "alumno_id", nullable = false)
    private Alumno alumno;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoAsistencia estado;

    @Column(name = "hora_ingreso")
    private LocalDateTime horaIngreso;

    @Column(name = "minutos_tarde", nullable = false)
    private Integer minutosTarde = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_registro", nullable = false, length = 15)
    private MetodoRegistroAsistencia metodoRegistro = MetodoRegistroAsistencia.MANUAL;

    @Column(length = 500)
    private String observacion;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;

    public Asistencia() {
    }

    @PrePersist
    public void antesDeGuardar() {
        if (fechaRegistro == null) {
            fechaRegistro = LocalDateTime.now(TimeConfig.ZONA_LIMA);
        }
        if (minutosTarde == null) {
            minutosTarde = 0;
        }
        if (metodoRegistro == null) {
            metodoRegistro = MetodoRegistroAsistencia.MANUAL;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public SesionClase getSesion() {
        return sesion;
    }

    public void setSesion(SesionClase sesion) {
        this.sesion = sesion;
    }

    public Alumno getAlumno() {
        return alumno;
    }

    public void setAlumno(Alumno alumno) {
        this.alumno = alumno;
    }

    public EstadoAsistencia getEstado() {
        return estado;
    }

    public void setEstado(EstadoAsistencia estado) {
        this.estado = estado;
    }

    public LocalDateTime getHoraIngreso() {
        return horaIngreso;
    }

    public void setHoraIngreso(LocalDateTime horaIngreso) {
        this.horaIngreso = horaIngreso;
    }

    public Integer getMinutosTarde() {
        return minutosTarde;
    }

    public void setMinutosTarde(Integer minutosTarde) {
        this.minutosTarde = minutosTarde;
    }

    public MetodoRegistroAsistencia getMetodoRegistro() {
        return metodoRegistro;
    }

    public void setMetodoRegistro(MetodoRegistroAsistencia metodoRegistro) {
        this.metodoRegistro = metodoRegistro;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }
}