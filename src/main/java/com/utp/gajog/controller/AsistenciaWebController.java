package com.utp.gajog.controller;

import com.utp.gajog.config.NegocioProperties;
import com.utp.gajog.dto.ConfirmacionIngresoDniDTO;
import com.utp.gajog.dto.FilaPaseListaDTO;
import com.utp.gajog.dto.FiltroHistorialAsistenciaDTO;
import com.utp.gajog.exception.RecursoNoEncontradoException;
import com.utp.gajog.exception.ReglaNegocioException;
import com.utp.gajog.model.*;
import com.utp.gajog.service.AlumnoService;
import com.utp.gajog.service.AsistenciaService;
import com.utp.gajog.service.PersonalService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/asistencias")
public class AsistenciaWebController {

    private static final Logger log = LoggerFactory.getLogger(AsistenciaWebController.class);

    private final AsistenciaService asistenciaService;
    private final AlumnoService alumnoService;
    private final PersonalService personalService;
    private final NegocioProperties negocioProperties;
    private final Clock clock;

    public AsistenciaWebController(
            AsistenciaService asistenciaService,
            AlumnoService alumnoService,
            PersonalService personalService,
            NegocioProperties negocioProperties,
            Clock clock
    ) {
        this.asistenciaService = asistenciaService;
        this.alumnoService = alumnoService;
        this.personalService = personalService;
        this.negocioProperties = negocioProperties;
        this.clock = clock;
    }

    @GetMapping
    public String vistaAsistencias(
            @RequestParam(defaultValue = "dni") String modo,
            @RequestParam(required = false) Long sesionId,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) Long horarioId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) EstadoAsistencia estado,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            Model model
    ) {
        LocalDate hoy = LocalDate.now(clock);
        LocalDate fechaSeleccionada = (fecha != null) ? fecha : hoy;

        String modoActivo = (sesionId != null && "dni".equalsIgnoreCase(modo)) ? "grupo" : modo.toLowerCase();

        model.addAttribute("modo", modoActivo);
        model.addAttribute("fechaSeleccionada", fechaSeleccionada);
        model.addAttribute("categoriaIdSeleccionada", categoriaId);
        model.addAttribute("horarioIdSeleccionado", horarioId);
        model.addAttribute("toleranciaMinutos", negocioProperties.getMinutosToleranciaTardanza());

        model.addAttribute("categorias", alumnoService.listarCategoriasActivas());
        model.addAttribute("horarios", alumnoService.listarTodosLosHorarios());
        model.addAttribute("instructores", personalService.listarInstructores());
        model.addAttribute("estadosAsistencia", EstadoAsistencia.values());
        model.addAttribute("sesionesRecientes", asistenciaService.listarSesionesRecientes(null, categoriaId, horarioId));

        if (sesionId != null) {
            asistenciaService.obtenerSesionConDetalle(sesionId).ifPresent(sesion -> {
                List<FilaPaseListaDTO> filasPaseLista = asistenciaService.construirFilasPaseLista(sesionId);
                model.addAttribute("sesionSeleccionada", sesion);
                model.addAttribute("filasPaseLista", filasPaseLista);
            });
        }

        FiltroHistorialAsistenciaDTO filtroHist = new FiltroHistorialAsistenciaDTO(fecha, categoriaId, horarioId, q, estado);
        Page<Asistencia> pageHistorial = asistenciaService.buscarHistorialPaginado(filtroHist, page, size);
        model.addAttribute("filtroHist", filtroHist);
        model.addAttribute("pageHistorial", pageHistorial);

        return "asistencias";
    }

    @PostMapping("/dni")
    public String registrarPorDni(
            @RequestParam String dni,
            @RequestParam(required = false) Long horarioId,
            RedirectAttributes redirectAttributes
    ) {
        try {
            ConfirmacionIngresoDniDTO confirmacion = asistenciaService.registrarIngresoRapidoPorDni(dni, horarioId);
            redirectAttributes.addFlashAttribute("confirmacionDni", confirmacion);
            if (!confirmacion.requiereSeleccionHorario()) {
                redirectAttributes.addFlashAttribute("exito",
                        "Ingreso registrado: " + confirmacion.nombreCompleto() + " (" + confirmacion.estadoAsistencia() + ").");
            }
        } catch (Exception e) {
            manejarExcepcion(e, redirectAttributes);
        }
        return "redirect:/asistencias?modo=dni";
    }

    @PostMapping("/sesion/guardar")
    public String abrirOCargarSesion(
            @RequestParam Long horarioId,
            @RequestParam(required = false) Long instructorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            RedirectAttributes redirectAttributes
    ) {
        try {
            SesionClase sesion = asistenciaService.abrirOCargarSesionPorGrupoYFecha(horarioId, instructorId, fecha);
            redirectAttributes.addFlashAttribute("exito", "Sesión de entrenamiento lista para pasar asistencia.");
            return "redirect:/asistencias?modo=grupo&sesionId=" + sesion.getId();
        } catch (Exception e) {
            manejarExcepcion(e, redirectAttributes);
            return "redirect:/asistencias?modo=grupo";
        }
    }

    @PostMapping("/guardar-lista")
    public String guardarPaseDeLista(
            @RequestParam Long sesionId,
            @RequestParam(required = false) List<Long> alumnoIds,
            @RequestParam(required = false) List<EstadoAsistencia> estados,
            @RequestParam(required = false) List<String> observaciones,
            RedirectAttributes redirectAttributes
    ) {
        try {
            asistenciaService.guardarPaseListaDetallado(sesionId, alumnoIds, estados, observaciones);
            redirectAttributes.addFlashAttribute("exito", "Lista de asistencia guardada y sesión finalizada correctamente.");
        } catch (Exception e) {
            manejarExcepcion(e, redirectAttributes);
        }
        return "redirect:/asistencias?modo=grupo&sesionId=" + sesionId;
    }

    @PostMapping("/observacion-progreso")
    public String guardarObservacionProgresoEnClase(
            @RequestParam Long sesionId,
            @RequestParam Long alumnoId,
            @RequestParam Long instructorId,
            @RequestParam String observacion,
            RedirectAttributes redirectAttributes
    ) {
        try {
            personalService.registrarObservacion(alumnoId, instructorId, observacion);
            redirectAttributes.addFlashAttribute("exito", "Observación de progreso guardada en el expediente del alumno.");
        } catch (Exception e) {
            manejarExcepcion(e, redirectAttributes);
        }
        return "redirect:/asistencias?modo=grupo&sesionId=" + sesionId;
    }

    @PostMapping("/generar-semana")
    public String generarSesionesDeLaSemanaManual(RedirectAttributes redirectAttributes) {
        try {
            int creadas = asistenciaService.generarSesionesDeLaSemana();
            redirectAttributes.addFlashAttribute("exito",
                    "Se generaron " + creadas + " sesiones programadas para los próximos 7 días.");
        } catch (Exception e) {
            manejarExcepcion(e, redirectAttributes);
        }
        return "redirect:/asistencias?modo=grupo";
    }

    private void manejarExcepcion(Exception e, RedirectAttributes redirectAttributes) {
        if (e instanceof ReglaNegocioException || e instanceof RecursoNoEncontradoException
                || e instanceof IllegalArgumentException || e instanceof IllegalStateException) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } else {
            log.error("Error técnico en AsistenciaWebController", e);
            redirectAttributes.addFlashAttribute("error", "Ocurrió un error al procesar la asistencia.");
        }
    }
}