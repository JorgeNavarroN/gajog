package com.utp.gajog.dto;

import com.utp.gajog.model.DiaSemana;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

public class MatriculaWizardDTO implements Serializable {

    private Long alumnoExistenteId;
    private Long matriculaPendienteId;

    @NotBlank(message = "El DNI del alumno es obligatorio")
    @Pattern(regexp = "\\d{8}", message = "El DNI del alumno debe tener exactamente 8 dígitos")
    private String dniAlumno;

    @NotBlank(message = "Los nombres del alumno son obligatorios")
    @Size(max = 100)
    private String nombresAlumno;

    @NotBlank(message = "Los apellidos del alumno son obligatorios")
    @Size(max = 100)
    private String apellidosAlumno;

    @NotNull(message = "La fecha de nacimiento es obligatoria")
    @Past(message = "La fecha de nacimiento debe ser anterior a hoy")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaNacimiento;

    @NotBlank(message = "El teléfono de contacto es obligatorio")
    @Size(max = 20)
    private String telefonoContacto;

    @Email(message = "Correo inválido")
    @Size(max = 150)
    private String correoAlumno;

    private Long cinturonInicialId;

    // Datos del Apoderado (obligatorios si edad < 18)
    private Long apoderadoExistenteId;
    private String dniApoderado;
    private String nombresApoderado;
    private String apellidosApoderado;
    private String telefonoApoderado;
    private String parentesco = "Madre / Padre";

    // Paso 2 (Calculados automáticamente en servidor, no editables)
    private Integer edadCalculada;
    private boolean menorDeEdad;
    private boolean personaAlumnoReutilizada;
    private Long categoriaAsignadaId;
    private String categoriaAsignadaNombre;
    private String categoriaRangoTexto;
    private BigDecimal tarifaMensualCategoria;

    // Paso 3 (Selección de Horario y Filtros del Paso 3)
    private Long filtroCategoriaPaso3Id;
    private DiaSemana filtroDiaPaso3;
    private String filtroTurnoPaso3;
    // El identificador seleccionado ahora es del GRUPO (se conserva alias de compatibilidad).
    private Long horarioSeleccionadoId;

    private boolean paso1Completado = false;
    private boolean paso2Completado = false;

    public Long getAlumnoExistenteId() {
        return alumnoExistenteId;
    }

    public void setAlumnoExistenteId(Long alumnoExistenteId) {
        this.alumnoExistenteId = alumnoExistenteId;
    }

    public Long getMatriculaPendienteId() {
        return matriculaPendienteId;
    }

    public void setMatriculaPendienteId(Long matriculaPendienteId) {
        this.matriculaPendienteId = matriculaPendienteId;
    }

    public String getDniAlumno() {
        return dniAlumno;
    }

    public void setDniAlumno(String dniAlumno) {
        this.dniAlumno = dniAlumno;
    }

    public String getNombresAlumno() {
        return nombresAlumno;
    }

    public void setNombresAlumno(String nombresAlumno) {
        this.nombresAlumno = nombresAlumno;
    }

    public String getApellidosAlumno() {
        return apellidosAlumno;
    }

    public void setApellidosAlumno(String apellidosAlumno) {
        this.apellidosAlumno = apellidosAlumno;
    }

    public LocalDate getFechaNacimiento() {
        return fechaNacimiento;
    }

    public void setFechaNacimiento(LocalDate fechaNacimiento) {
        this.fechaNacimiento = fechaNacimiento;
    }

    public String getTelefonoContacto() {
        return telefonoContacto;
    }

    public void setTelefonoContacto(String telefonoContacto) {
        this.telefonoContacto = telefonoContacto;
    }

    public String getCorreoAlumno() {
        return correoAlumno;
    }

    public void setCorreoAlumno(String correoAlumno) {
        this.correoAlumno = correoAlumno;
    }

    public Long getCinturonInicialId() {
        return cinturonInicialId;
    }

    public void setCinturonInicialId(Long cinturonInicialId) {
        this.cinturonInicialId = cinturonInicialId;
    }

    public Long getApoderadoExistenteId() {
        return apoderadoExistenteId;
    }

    public void setApoderadoExistenteId(Long apoderadoExistenteId) {
        this.apoderadoExistenteId = apoderadoExistenteId;
    }

    public String getDniApoderado() {
        return dniApoderado;
    }

    public void setDniApoderado(String dniApoderado) {
        this.dniApoderado = dniApoderado;
    }

    public String getNombresApoderado() {
        return nombresApoderado;
    }

    public void setNombresApoderado(String nombresApoderado) {
        this.nombresApoderado = nombresApoderado;
    }

    public String getApellidosApoderado() {
        return apellidosApoderado;
    }

    public void setApellidosApoderado(String apellidosApoderado) {
        this.apellidosApoderado = apellidosApoderado;
    }

    public String getTelefonoApoderado() {
        return telefonoApoderado;
    }

    public void setTelefonoApoderado(String telefonoApoderado) {
        this.telefonoApoderado = telefonoApoderado;
    }

    public String getParentesco() {
        return parentesco;
    }

    public void setParentesco(String parentesco) {
        this.parentesco = parentesco;
    }

    public Integer getEdadCalculada() {
        return edadCalculada;
    }

    public void setEdadCalculada(Integer edadCalculada) {
        this.edadCalculada = edadCalculada;
    }

    public boolean isMenorDeEdad() {
        return menorDeEdad;
    }

    public void setMenorDeEdad(boolean menorDeEdad) {
        this.menorDeEdad = menorDeEdad;
    }

    public boolean isPersonaAlumnoReutilizada() {
        return personaAlumnoReutilizada;
    }

    public void setPersonaAlumnoReutilizada(boolean personaAlumnoReutilizada) {
        this.personaAlumnoReutilizada = personaAlumnoReutilizada;
    }

    public Long getCategoriaAsignadaId() {
        return categoriaAsignadaId;
    }

    public void setCategoriaAsignadaId(Long categoriaAsignadaId) {
        this.categoriaAsignadaId = categoriaAsignadaId;
    }

    public String getCategoriaAsignadaNombre() {
        return categoriaAsignadaNombre;
    }

    public void setCategoriaAsignadaNombre(String categoriaAsignadaNombre) {
        this.categoriaAsignadaNombre = categoriaAsignadaNombre;
    }

    public String getCategoriaRangoTexto() {
        return categoriaRangoTexto;
    }

    public void setCategoriaRangoTexto(String categoriaRangoTexto) {
        this.categoriaRangoTexto = categoriaRangoTexto;
    }

    public BigDecimal getTarifaMensualCategoria() {
        return tarifaMensualCategoria;
    }

    public void setTarifaMensualCategoria(BigDecimal tarifaMensualCategoria) {
        this.tarifaMensualCategoria = tarifaMensualCategoria;
    }

    public Long getFiltroCategoriaPaso3Id() {
        return filtroCategoriaPaso3Id;
    }

    public void setFiltroCategoriaPaso3Id(Long filtroCategoriaPaso3Id) {
        this.filtroCategoriaPaso3Id = filtroCategoriaPaso3Id;
    }

    public DiaSemana getFiltroDiaPaso3() {
        return filtroDiaPaso3;
    }

    public void setFiltroDiaPaso3(DiaSemana filtroDiaPaso3) {
        this.filtroDiaPaso3 = filtroDiaPaso3;
    }

    public String getFiltroTurnoPaso3() {
        return filtroTurnoPaso3;
    }

    public void setFiltroTurnoPaso3(String filtroTurnoPaso3) {
        this.filtroTurnoPaso3 = filtroTurnoPaso3;
    }

    public Long getGrupoSeleccionadoId() { return horarioSeleccionadoId; }
    public void setGrupoSeleccionadoId(Long grupoId) { this.horarioSeleccionadoId = grupoId; }

    public Long getHorarioSeleccionadoId() {
        return horarioSeleccionadoId;
    }

    public void setHorarioSeleccionadoId(Long horarioSeleccionadoId) {
        this.horarioSeleccionadoId = horarioSeleccionadoId;
    }

    public boolean isPaso1Completado() {
        return paso1Completado;
    }

    public void setPaso1Completado(boolean paso1Completado) {
        this.paso1Completado = paso1Completado;
    }

    public boolean isPaso2Completado() {
        return paso2Completado;
    }

    public void setPaso2Completado(boolean paso2Completado) {
        this.paso2Completado = paso2Completado;
    }

    public String getNombreCompletoAlumno() {
        return ((nombresAlumno != null ? nombresAlumno : "") + " " + (apellidosAlumno != null ? apellidosAlumno : "")).trim();
    }
}