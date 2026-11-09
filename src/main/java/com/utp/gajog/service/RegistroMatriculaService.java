/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.utp.gajog.service;

import com.utp.gajog.dto.RegistroMatriculaForm;
import com.utp.gajog.enums.EstadoAlumno;
import com.utp.gajog.enums.EstadoMensualidad;
import com.utp.gajog.enums.EstadoPago;
import com.utp.gajog.enums.MedioPago;
import com.utp.gajog.enums.TipoPago;
import com.utp.gajog.model.Alumno;
import com.utp.gajog.model.Apoderado;
import com.utp.gajog.model.Cinturon;
import com.utp.gajog.model.Matricula;
import com.utp.gajog.model.Mensualidad;
import com.utp.gajog.model.Pago;
import com.utp.gajog.model.Persona;
import com.utp.gajog.model.Tarifa;
import com.utp.gajog.repository.AlumnoRepository;
import com.utp.gajog.repository.ApoderadoRepository;
import com.utp.gajog.repository.CinturonRepository;
import com.utp.gajog.repository.MensualidadRepository;
import com.utp.gajog.repository.PagoRepository;
import com.utp.gajog.repository.PersonaRepository;
import com.utp.gajog.repository.TarifaRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.temporal.TemporalAdjusters;
import java.time.ZoneId;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author JORGE
 */
@Service
public class RegistroMatriculaService {

    private final PersonaRepository personaRepository;
    private final AlumnoRepository alumnoRepository;
    private final ApoderadoRepository apoderadoRepository;
    private final CinturonRepository cinturonRepository;
    private final TarifaRepository tarifaRepository;
    private final MensualidadRepository mensualidadRepository;
    private final MatriculaService matriculaService;
    private final PagoRepository pagoRepository;

    public RegistroMatriculaService(
            PersonaRepository personaRepository,
            AlumnoRepository alumnoRepository,
            ApoderadoRepository apoderadoRepository,
            CinturonRepository cinturonRepository,
            TarifaRepository tarifaRepository,
            MensualidadRepository mensualidadRepository,
            MatriculaService matriculaService,
            PagoRepository pagoRepository) {

        this.personaRepository = personaRepository;
        this.alumnoRepository = alumnoRepository;
        this.apoderadoRepository = apoderadoRepository;
        this.cinturonRepository = cinturonRepository;
        this.tarifaRepository = tarifaRepository;
        this.mensualidadRepository = mensualidadRepository;
        this.matriculaService = matriculaService;
        this.pagoRepository = pagoRepository;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Matricula confirmar(
            RegistroMatriculaForm formulario,
            Long grupoId,
            MedioPago medioPago,
            boolean pagoRecibido) {

        validarFormulario(formulario);

        if (medioPago == null) {
            throw new IllegalArgumentException(
                    "Selecciona el medio de pago.");
        }

        if (!pagoRecibido) {
            throw new IllegalArgumentException(
                    "Confirma que recibiste el pago de matrícula.");
        }

        if (grupoId == null) {
            throw new IllegalArgumentException(
                    "Selecciona un grupo.");
        }

        String dni = texto(formulario.getDni());

        if (alumnoRepository.findByPersona_Dni(dni).isPresent()) {
            throw new IllegalArgumentException(
                    "Ya existe un alumno con ese DNI.");
        }

        LocalDate hoy = LocalDate.now(ZoneId.of("America/Lima"));

        Cinturon cinturon = cinturonRepository
                .findByNombreIgnoreCase("Blanco")
                .orElseThrow(()
                        -> new IllegalStateException(
                                "Configura el cinturón Blanco antes de registrar alumnos."));

        Tarifa tarifa = obtenerTarifaMensual(hoy);

        Tarifa tarifaMatricula = obtenerTarifaVigente(
                TipoPago.MATRICULA, hoy);

        if (formulario.getImporteMatricula() == null
                || formulario.getImporteMatricula()
                        .compareTo(tarifaMatricula.getMonto()) != 0) {

            throw new IllegalStateException(
                    "El importe de matrícula cambió. "
                    + "Revisa el importe actualizado y confirma nuevamente.");
        }

        // Persona del alumno.
        Persona persona = obtenerPersona(
                dni,
                formulario.getNombres(),
                formulario.getApellidos()
        );

        if (persona.getFechaNacimiento() != null
                && !persona.getFechaNacimiento()
                        .equals(formulario.getFechaNacimiento())) {

            throw new IllegalArgumentException(
                    "El DNI ya pertenece a una persona con otra fecha de nacimiento.");
        }

        persona.setFechaNacimiento(
                formulario.getFechaNacimiento());

        persona.setTelefono(texto(formulario.getTelefono()));

        if (!texto(formulario.getCorreo()).isEmpty()) {
            persona.setCorreo(texto(formulario.getCorreo()));
        }

        personaRepository.save(persona);

        // Apoderado: se reutiliza si ya existe.
        Apoderado apoderado = null;

        if (!texto(formulario.getApoderadoNombre()).isEmpty()) {

            Persona personaApoderado = obtenerPersona(
                    texto(formulario.getApoderadoDni()),
                    formulario.getApoderadoNombre(),
                    formulario.getApoderadoApellidos()
            );

            apoderado = apoderadoRepository
                    .findByPersona_Dni(personaApoderado.getDni())
                    .orElseGet(() -> {
                        Apoderado nuevo = new Apoderado();
                        nuevo.setPersona(personaApoderado);
                        return apoderadoRepository.save(nuevo);
                    });
        }

        // Alumno.
        Alumno alumno = new Alumno();
        alumno.setPersona(persona);
        alumno.setApoderado(apoderado);

        alumno.setParentescoApoderado(
                apoderado == null
                        ? null
                        : formulario.getParentesco()
        );

        alumno.setCinturonActual(cinturon);
        alumno.setFechaIngreso(hoy);
        alumno.setEstado(EstadoAlumno.ACTIVO);

        alumnoRepository.save(alumno);

        // Matrícula: valida el grupo y controla el cupo con bloqueo.
        Matricula matricula = matriculaService.registrar(
                alumno.getId(),
                grupoId
        );

        // Primera mensualidad: queda pendiente.
        Mensualidad mensualidad = new Mensualidad();
        mensualidad.setAlumno(alumno);
        mensualidad.setAnio(hoy.getYear());
        mensualidad.setMes(hoy.getMonthValue());
        mensualidad.setMonto(tarifa.getMonto());

        mensualidad.setFechaVencimiento(
                hoy.with(TemporalAdjusters.lastDayOfMonth())
        );

        mensualidad.setEstado(EstadoMensualidad.PENDIENTE);

        mensualidadRepository.save(mensualidad);

        Pago pago = new Pago();
        pago.setAlumno(alumno);
        pago.setMatricula(matricula);
        pago.setTipoPago(TipoPago.MATRICULA);
        pago.setMonto(tarifaMatricula.getMonto());
        pago.setMedioPago(medioPago);
        pago.setFechaPago(
                LocalDateTime.now(ZoneId.of("America/Lima")));
        pago.setEstado(EstadoPago.REGISTRADO);

        pagoRepository.save(pago);

        return matricula;
    }

    private Persona obtenerPersona(
            String dni,
            String nombres,
            String apellidos) {

        Persona persona = personaRepository
                .findByDni(dni)
                .orElse(null);

        if (persona != null) {

            boolean coincideNombre
                    = texto(persona.getNombres())
                            .equalsIgnoreCase(texto(nombres))
                    && texto(persona.getApellidos())
                            .equalsIgnoreCase(texto(apellidos));

            if (!coincideNombre) {
                throw new IllegalArgumentException(
                        "El DNI " + dni
                        + " ya está registrado con otros nombres o apellidos.");
            }

            return persona;
        }

        Persona nueva = new Persona();
        nueva.setDni(dni);
        nueva.setNombres(texto(nombres));
        nueva.setApellidos(texto(apellidos));

        return personaRepository.save(nueva);
    }

    @Transactional(readOnly = true)
    public BigDecimal consultarImporteMatricula() {
        return obtenerTarifaVigente(
                TipoPago.MATRICULA,
                LocalDate.now(ZoneId.of("America/Lima"))
        ).getMonto();
    }

    private Tarifa obtenerTarifaMensual(LocalDate fecha) {
        return obtenerTarifaVigente(TipoPago.MENSUALIDAD, fecha);
    }

    private Tarifa obtenerTarifaVigente(
            TipoPago tipo,
            LocalDate fecha) {

        List<Tarifa> vigentes = tarifaRepository
                .findByTipoPagoAndActivoTrue(tipo)
                .stream()
                .filter(t -> !t.getFechaInicio().isAfter(fecha))
                .filter(t -> t.getFechaFin() == null
                        || !t.getFechaFin().isBefore(fecha))
                .toList();

        if (vigentes.size() != 1) {
            throw new IllegalStateException(
                    "Debe existir exactamente una tarifa vigente de "
                    + tipo + ".");
        }

        Tarifa tarifa = vigentes.get(0);

        if (tarifa.getMonto() == null
                || tarifa.getMonto().signum() <= 0) {
            throw new IllegalStateException(
                    "La tarifa de " + tipo + " debe ser positiva.");
        }

        return tarifa;
    }

    private void validarFormulario(RegistroMatriculaForm formulario) {

        validarNombre(formulario.getNombres(), "Nombres del alumno");
        validarNombre(formulario.getApellidos(), "Apellidos del alumno");

        if (!texto(formulario.getDni()).matches("\\d{8}")) {
            throw new IllegalArgumentException(
                    "El DNI del alumno debe tener 8 dígitos.");
        }

        if (!texto(formulario.getTelefono()).matches("\\d{9}")) {
            throw new IllegalArgumentException(
                    "El teléfono debe tener 9 dígitos.");
        }

        if (texto(formulario.getCorreo()).length() > 150) {
            throw new IllegalArgumentException(
                    "El correo supera los 150 caracteres.");
        }

        LocalDate nacimiento = formulario.getFechaNacimiento();

        if (nacimiento == null
                || nacimiento.isAfter(LocalDate.now())) {

            throw new IllegalArgumentException(
                    "La fecha de nacimiento no es válida.");
        }

        int edad = Period.between(
                nacimiento, LocalDate.now()).getYears();

        boolean tieneDatosApoderado
                = !texto(formulario.getApoderadoNombre()).isEmpty()
                || !texto(formulario.getApoderadoApellidos()).isEmpty()
                || !texto(formulario.getApoderadoDni()).isEmpty()
                || formulario.getParentesco() != null;

        if (edad < 17 || tieneDatosApoderado) {

            validarNombre(
                    formulario.getApoderadoNombre(),
                    "Nombres del apoderado");

            validarNombre(
                    formulario.getApoderadoApellidos(),
                    "Apellidos del apoderado");

            if (!texto(formulario.getApoderadoDni()).matches("\\d{8}")
                    || formulario.getParentesco() == null) {

                throw new IllegalArgumentException(
                        "Completa el DNI y parentesco del apoderado.");
            }

            if (texto(formulario.getDni())
                    .equals(texto(formulario.getApoderadoDni()))) {

                throw new IllegalArgumentException(
                        "El alumno y su apoderado deben tener DNI diferentes.");
            }
        }
    }

    private void validarNombre(String valor, String campo) {
        String contenido = texto(valor);

        if (contenido.isEmpty() || contenido.length() > 100) {
            throw new IllegalArgumentException(
                    campo + ": ingresa entre 1 y 100 caracteres.");
        }
    }

    private String texto(String valor) {
        return valor == null ? "" : valor.trim();
    }
}
