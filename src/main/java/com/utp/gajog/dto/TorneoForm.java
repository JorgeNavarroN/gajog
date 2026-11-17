package com.utp.gajog.dto;

import com.utp.gajog.enums.ModalidadTorneo;
import java.math.BigDecimal;
import java.time.LocalDate;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * Datos del formulario "Nuevo torneo".
 * A05 Inyección: lista blanca de caracteres permitidos y límites de tamaño,
 * así no se pueden enviar etiquetas HTML, scripts ni textos enormes.
 */
public class TorneoForm {

    /** Letras (con tildes), números, espacios y signos simples. Sin < > " ; */
    public static final String TEXTO_SEGURO = "^[\\p{L}0-9 .,()°#'-]+$";

    @NotBlank(message = "El nombre del torneo es obligatorio.")
    @Size(max = 120, message = "El nombre no puede pasar de 120 caracteres.")
    @Pattern(regexp = TEXTO_SEGURO, message = "El nombre tiene caracteres no permitidos.")
    private String nombre;

    @NotNull(message = "La fecha del torneo es obligatoria.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fecha;

    @NotBlank(message = "La sede es obligatoria.")
    @Size(max = 150, message = "La sede no puede pasar de 150 caracteres.")
    @Pattern(regexp = TEXTO_SEGURO, message = "La sede tiene caracteres no permitidos.")
    private String sede;

    @NotNull(message = "Seleccione la modalidad.")
    private ModalidadTorneo modalidad = ModalidadTorneo.COMBATE;

    @NotNull(message = "La cuota de inscripción es obligatoria.")
    @DecimalMin(value = "0.00", message = "La cuota no puede ser negativa.")
    @DecimalMax(value = "1000.00", message = "La cuota no puede pasar de S/ 1000.")
    @Digits(integer = 4, fraction = 2, message = "La cuota admite hasta 2 decimales.")
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
