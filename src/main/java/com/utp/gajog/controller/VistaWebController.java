package com.utp.gajog.controller;

import com.utp.gajog.model.*;
import com.utp.gajog.repository.*;
import com.utp.gajog.service.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
public class VistaWebController {

    private final AlumnoService alumnoService;
    private final MatriculaService matriculaService;
    private final PagoService pagoService;
    private final AsistenciaService asistenciaService;
    private final ExamenService examenService;
    private final PersonalService personalService;

    private final CinturonRepository cinturonRepository;
    private final CategoriaRepository categoriaRepository;
    private final HorarioRepository horarioRepository;
    private final MatriculaRepository matriculaRepository;
    private final MensualidadRepository mensualidadRepository;
    private final PagoRepository pagoRepository;
    private final InstructorRepository instructorRepository;
    private final SesionClaseRepository sesionClaseRepository;
    private final ExamenRepository examenRepository;
    private final CriterioExamenRepository criterioExamenRepository;
    private final HistorialCinturonesRepository historialCinturonesRepository;
    private final ObservacionProgresoRepository observacionProgresoRepository;

    public VistaWebController(
            AlumnoService alumnoService,
            MatriculaService matriculaService,
            PagoService pagoService,
            AsistenciaService asistenciaService,
            ExamenService examenService,
            PersonalService personalService,
            CinturonRepository cinturonRepository,
            CategoriaRepository categoriaRepository,
            HorarioRepository horarioRepository,
            MatriculaRepository matriculaRepository,
            MensualidadRepository mensualidadRepository,
            PagoRepository pagoRepository,
            InstructorRepository instructorRepository,
            SesionClaseRepository sesionClaseRepository,
            ExamenRepository examenRepository,
            CriterioExamenRepository criterioExamenRepository,
            HistorialCinturonesRepository historialCinturonesRepository,
            ObservacionProgresoRepository observacionProgresoRepository
    ) {
        this.alumnoService = alumnoService;
        this.matriculaService = matriculaService;
        this.pagoService = pagoService;
        this.asistenciaService = asistenciaService;
        this.examenService = examenService;
        this.personalService = personalService;
        this.cinturonRepository = cinturonRepository;
        this.categoriaRepository = categoriaRepository;
        this.horarioRepository = horarioRepository;
        this.matriculaRepository = matriculaRepository;
        this.mensualidadRepository = mensualidadRepository;
        this.pagoRepository = pagoRepository;
        this.instructorRepository = instructorRepository;
        this.sesionClaseRepository = sesionClaseRepository;
        this.examenRepository = examenRepository;
        this.criterioExamenRepository = criterioExamenRepository;
        this.historialCinturonesRepository = historialCinturonesRepository;
        this.observacionProgresoRepository = observacionProgresoRepository;
    }

    // ==================== 1. ALUMNOS Y PERFIL ====================
    @GetMapping("/alumnos")
    public String vistaAlumnos(Model model) {
        model.addAttribute("alumnos", alumnoService.listarTodos());
        model.addAttribute("cinturones", cinturonRepository.findAllByOrderByOrdenAsc());
        model.addAttribute("apoderados", personalService.listarApoderados());
        return "alumnos";
    }

    @PostMapping("/alumnos/guardar")
    public String guardarAlumno(
            @RequestParam String dni,
            @RequestParam String nombres,
            @RequestParam String apellidos,
            @RequestParam(required = false) String correo,
            @RequestParam(required = false) String telefono,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaNacimiento,
            @RequestParam Long cinturonInicialId,
            @RequestParam(required = false) Long apoderadoId,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Persona persona = new Persona();
            persona.setDni(dni);
            persona.setNombres(nombres);
            persona.setApellidos(apellidos);
            persona.setCorreo(correo);
            persona.setTelefono(telefono);
            persona.setFechaNacimiento(fechaNacimiento);

            alumnoService.registrarAlumno(persona, cinturonInicialId, apoderadoId);
            redirectAttributes.addFlashAttribute("exito", "Alumno registrado correctamente.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/alumnos";
    }

    @PostMapping("/alumnos/{id}/estado")
    public String cambiarEstadoAlumno(
            @PathVariable Long id,
            @RequestParam EstadoAlumno nuevoEstado,
            RedirectAttributes redirectAttributes
    ) {
        try {
            alumnoService.cambiarEstado(id, nuevoEstado);
            redirectAttributes.addFlashAttribute("exito", "El estado del alumno fue actualizado a: " + nuevoEstado);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "No se pudo actualizar el estado: " + e.getMessage());
        }
        return "redirect:/alumnos";
    }

    @GetMapping("/alumnos/{id}")
    public String verPerfilAlumno(@PathVariable Long id, Model model) {
        Alumno alumno = alumnoService.obtenerPorId(id);
        model.addAttribute("alumno", alumno);
        model.addAttribute("historialCinturones", historialCinturonesRepository.findByAlumnoIdOrderByFechaObtencionDesc(id));
        model.addAttribute("observaciones", observacionProgresoRepository.findByAlumnoIdOrderByFechaDesc(id));
        model.addAttribute("matriculas", matriculaRepository.findByAlumnoId(id));
        model.addAttribute("instructores", instructorRepository.findAll());
        return "alumno-detalle";
    }

    @PostMapping("/alumnos/{id}/observacion")
    public String guardarObservacion(
            @PathVariable Long id,
            @RequestParam Long instructorId,
            @RequestParam String observacion,
            RedirectAttributes redirectAttributes
    ) {
        try {
            personalService.registrarObservacion(id, instructorId, observacion);
            redirectAttributes.addFlashAttribute("exito", "Observación de progreso añadida.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/alumnos/" + id;
    }
    

    // ==================== 2. INSTRUCTORES Y APODERADOS ====================
    @GetMapping("/personal")
    public String vistaPersonal(Model model) {
        model.addAttribute("instructores", personalService.listarInstructores());
        model.addAttribute("apoderados", personalService.listarApoderados());
        return "personal";
    }

    @PostMapping("/personal/instructor/guardar")
    public String guardarInstructor(
            @RequestParam String dni,
            @RequestParam String nombres,
            @RequestParam String apellidos,
            @RequestParam(required = false) String telefono,
            @RequestParam(required = false) String correo,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Persona p = new Persona();
            p.setDni(dni);
            p.setNombres(nombres);
            p.setApellidos(apellidos);
            p.setTelefono(telefono);
            p.setCorreo(correo);
            personalService.registrarInstructor(p);
            redirectAttributes.addFlashAttribute("exito", "Instructor registrado correctamente.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/personal";
    }

    @PostMapping("/personal/apoderado/guardar")
    public String guardarApoderado(
            @RequestParam String dni,
            @RequestParam String nombres,
            @RequestParam String apellidos,
            @RequestParam(required = false) String telefono,
            @RequestParam(required = false) String correo,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Persona p = new Persona();
            p.setDni(dni);
            p.setNombres(nombres);
            p.setApellidos(apellidos);
            p.setTelefono(telefono);
            p.setCorreo(correo);
            personalService.registrarApoderado(p);
            redirectAttributes.addFlashAttribute("exito", "Apoderado registrado correctamente.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/personal";
    }

    // ==================== 3. MATRÍCULAS ====================
    @GetMapping("/matriculas")
    public String vistaMatriculas(Model model) {
        model.addAttribute("matriculas", matriculaRepository.findAll());
        model.addAttribute("alumnos", alumnoService.listarPorEstado(EstadoAlumno.ACTIVO));
        model.addAttribute("categorias", categoriaRepository.findByActivoTrue());
        model.addAttribute("horarios", horarioRepository.findAll());
        return "matriculas";
    }

    @PostMapping("/matriculas/guardar")
    public String guardarMatricula(
            @RequestParam Long alumnoId,
            @RequestParam Long categoriaId,
            @RequestParam Long horarioId,
            RedirectAttributes redirectAttributes
    ) {
        try {
            matriculaService.registrarMatricula(alumnoId, categoriaId, horarioId);
            redirectAttributes.addFlashAttribute("exito", "Matrícula registrada con éxito.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/matriculas";
    }

    // ==================== 4. MENSUALIDADES Y PAGOS ====================
    @GetMapping("/pagos")
    public String vistaPagos(Model model) {
        pagoService.actualizarMensualidadesVencidas();
        model.addAttribute("alumnos", alumnoService.listarPorEstado(EstadoAlumno.ACTIVO));
        model.addAttribute("mensualidades", mensualidadRepository.findAll());
        model.addAttribute("pagos", pagoRepository.findAll());
        model.addAttribute("mediosPago", MedioPago.values());
        return "pagos";
    }

    @PostMapping("/pagos/mensualidad/generar")
    public String generarMensualidad(
            @RequestParam Long alumnoId,
            @RequestParam Integer anio,
            @RequestParam Integer mes,
            @RequestParam BigDecimal monto,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaVencimiento,
            RedirectAttributes redirectAttributes
    ) {
        try {
            pagoService.generarMensualidad(alumnoId, anio, mes, monto, fechaVencimiento);
            redirectAttributes.addFlashAttribute("exito", "Mensualidad individual generada correctamente.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/pagos";
    }

    @PostMapping("/pagos/mensualidad/masivo")
    public String generarMensualidadesMasivas(
            @RequestParam Integer anio,
            @RequestParam Integer mes,
            @RequestParam BigDecimal monto,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaVencimiento,
            RedirectAttributes redirectAttributes
    ) {
        try {
            int cantidad = pagoService.generarMensualidadesMasivas(anio, mes, monto, fechaVencimiento);
            redirectAttributes.addFlashAttribute("exito",
                    "Se generaron " + cantidad + " mensualidades nuevas para los alumnos activos.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/pagos";
    }

    @PostMapping("/pagos/mensualidad/pagar")
    public String pagarMensualidad(
            @RequestParam Long mensualidadId,
            @RequestParam MedioPago medioPago,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Pago pago = pagoService.pagarMensualidad(mensualidadId, medioPago);
            redirectAttributes.addFlashAttribute("exito",
                    "Pago registrado exitosamente (Recibo N° " + pago.getId() + ").");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/pagos";
    }

    @PostMapping("/pagos/{id}/anular")
    public String anularPago(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            pagoService.anularPago(id);
            redirectAttributes.addFlashAttribute("exito", "Pago anulado y cuota devuelta a pendiente/vencida.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/pagos";
    }

    @GetMapping("/pagos/{id}/recibo")
    public String verReciboPago(@PathVariable Long id, Model model) {
        model.addAttribute("pago", pagoService.obtenerPagoPorId(id));
        return "recibo-pago";
    }

    // ==================== 5. ASISTENCIAS ====================
    @GetMapping("/asistencias")
    public String vistaAsistencias(
            @RequestParam(required = false) Long sesionId,
            Model model
    ) {
        model.addAttribute("horarios", horarioRepository.findAll());
        model.addAttribute("instructores", instructorRepository.findAll());
        model.addAttribute("sesiones", sesionClaseRepository.findAll());

        if (sesionId != null) {
            SesionClase sesion = sesionClaseRepository.findById(sesionId).orElse(null);
            if (sesion != null) {
                model.addAttribute("sesionSeleccionada", sesion);
                List<Matricula> matriculados = matriculaRepository.findByHorarioIdAndEstado(
                        sesion.getHorario().getId(), EstadoMatricula.ACTIVA
                );
                model.addAttribute("matriculados", matriculados);

                Map<Long, String> mapaAsistencia = asistenciaService.listarAsistenciasPorSesion(sesionId)
                        .stream()
                        .collect(Collectors.toMap(
                                a -> a.getAlumno().getId(),
                                a -> a.getEstado().name()
                        ));
                model.addAttribute("mapaAsistencia", mapaAsistencia);
            }
        }
        return "asistencias";
    }

    @PostMapping("/asistencias/sesion/guardar")
    public String guardarSesion(
            @RequestParam Long horarioId,
            @RequestParam Long instructorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            RedirectAttributes redirectAttributes
    ) {
        try {
            SesionClase nueva = asistenciaService.programarSesion(horarioId, instructorId, fecha);
            redirectAttributes.addFlashAttribute("exito", "Sesión creada. Ahora puedes pasar lista.");
            return "redirect:/asistencias?sesionId=" + nueva.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/asistencias";
        }
    }

    @PostMapping("/asistencias/guardar-lista")
    public String guardarListaAsistencia(
            @RequestParam Long sesionId,
            @RequestParam(required = false) Set<Long> presentesIds,
            RedirectAttributes redirectAttributes
    ) {
        try {
            asistenciaService.registrarAsistenciaMasiva(sesionId, presentesIds);
            redirectAttributes.addFlashAttribute("exito", "Lista de asistencia guardada correctamente.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/asistencias?sesionId=" + sesionId;
    }

    // ==================== 6. EXÁMENES Y ASCENSOS ====================
    @GetMapping("/examenes")
    public String vistaExamenes(Model model) {
        model.addAttribute("alumnos", alumnoService.listarTodos());
        model.addAttribute("cinturones", cinturonRepository.findAllByOrderByOrdenAsc());
        model.addAttribute("examenes", examenRepository.findAll());
        model.addAttribute("criterios", criterioExamenRepository.findAll());
        return "examenes";
    }

    @PostMapping("/examenes/guardar")
    public String programarExamen(
            @RequestParam Long alumnoId,
            @RequestParam Long cinturonObjetivoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaExamen,
            RedirectAttributes redirectAttributes
    ) {
        try {
            examenService.programarExamen(alumnoId, cinturonObjetivoId, fechaExamen);
            redirectAttributes.addFlashAttribute("exito", "Examen programado correctamente.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/examenes";
    }

    @PostMapping("/examenes/evaluar-completo")
    public String evaluarExamenCompleto(
            @RequestParam Long examenId,
            @RequestParam List<Long> criterioIds,
            @RequestParam List<BigDecimal> puntajes,
            @RequestParam(required = false) List<String> observaciones,
            @RequestParam(defaultValue = "70.00") BigDecimal puntajeMinimo,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Examen evaluado = examenService.calificarYFinalizarExamen(
                    examenId, criterioIds, puntajes, observaciones, puntajeMinimo
            );
            redirectAttributes.addFlashAttribute("exito",
                    "Evaluación completada. Puntaje total: " + evaluado.getPuntajeTotal()
                    + " (" + evaluado.getResultado() + ")");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/examenes";
    }
}