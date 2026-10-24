package com.utp.gajog.controller;

import com.utp.gajog.dto.FiltroExamenDTO;
import com.utp.gajog.exception.RecursoNoEncontradoException;
import com.utp.gajog.exception.ReglaNegocioException;
import com.utp.gajog.model.EstadoAlumno;
import com.utp.gajog.model.Examen;
import com.utp.gajog.model.ResultadoExamen;
import com.utp.gajog.service.AlumnoService;
import com.utp.gajog.service.ExamenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/examenes")
@PreAuthorize("hasAnyRole('ADMINISTRADOR', 'INSTRUCTOR')")
public class ExamenWebController {

    private static final Logger log = LoggerFactory.getLogger(ExamenWebController.class);

    private final AlumnoService alumnoService;
    private final ExamenService examenService;

    public ExamenWebController(AlumnoService alumnoService, ExamenService examenService) {
        this.alumnoService = alumnoService;
        this.examenService = examenService;
    }

    @GetMapping
    public String vistaExamenes(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long cinturonObjetivoId,
            @RequestParam(required = false) ResultadoExamen resultado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            Model model
    ) {
        FiltroExamenDTO filtro = new FiltroExamenDTO(q, cinturonObjetivoId, resultado, desde, hasta);
        model.addAttribute("filtro", filtro);
        model.addAttribute("alumnos", alumnoService.listarPorEstado(EstadoAlumno.ACTIVO));
        model.addAttribute("cinturones", alumnoService.listarCinturonesOrdenados());
        model.addAttribute("resultadosExamen", ResultadoExamen.values());
        model.addAttribute("examenes", examenService.buscarConFiltros(filtro));
        model.addAttribute("criterios", examenService.listarCriterios());
        return "examenes";
    }

    @PostMapping("/guardar")
    public String programarExamen(
            @RequestParam Long alumnoId,
            @RequestParam Long cinturonObjetivoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaExamen,
            RedirectAttributes redirectAttributes
    ) {
        try {
            examenService.programarExamen(alumnoId, cinturonObjetivoId, fechaExamen);
            redirectAttributes.addFlashAttribute("exito", "Examen de ascenso programado correctamente.");
        } catch (ReglaNegocioException | RecursoNoEncontradoException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            log.error("Error al programar examen", e);
            redirectAttributes.addFlashAttribute("error", "Ocurrió un error al programar el examen.");
        }
        return "redirect:/examenes";
    }

    @PostMapping("/evaluar-completo")
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
                    + " (" + evaluado.getResultado() + ").");
        } catch (ReglaNegocioException | RecursoNoEncontradoException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            log.error("Error al evaluar examen", e);
            redirectAttributes.addFlashAttribute("error", "Ocurrió un error al registrar las calificaciones.");
        }
        return "redirect:/examenes";
    }
}