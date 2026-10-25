package com.utp.gajog.controller;

import com.utp.gajog.dto.AlumnoResumenFilaDTO;
import com.utp.gajog.dto.ExpedienteAlumnoDTO;
import com.utp.gajog.dto.FiltroAlumnoDTO;
import com.utp.gajog.exception.RecursoNoEncontradoException;
import com.utp.gajog.exception.ReglaNegocioException;
import com.utp.gajog.model.EstadoAlumno;
import com.utp.gajog.model.EstadoMensualidad;
import com.utp.gajog.service.AlumnoService;
import com.utp.gajog.service.MatriculaService;
import com.utp.gajog.service.PersonalService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/alumnos")
public class AlumnoWebController {

    private static final Logger log = LoggerFactory.getLogger(AlumnoWebController.class);

    private final AlumnoService alumnoService;
    private final MatriculaService matriculaService;
    private final PersonalService personalService;

    public AlumnoWebController(
            AlumnoService alumnoService,
            MatriculaService matriculaService,
            PersonalService personalService
    ) {
        this.alumnoService = alumnoService;
        this.matriculaService = matriculaService;
        this.personalService = personalService;
    }

    @GetMapping
    public String listarAlumnos(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) Long horarioId,
            @RequestParam(required = false) EstadoAlumno estado,
            @RequestParam(required = false) Long cinturonId,
            @RequestParam(required = false) EstadoMensualidad estadoMensualidad,
            @RequestParam(defaultValue = "apellidos") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model
    ) {
        FiltroAlumnoDTO filtro = new FiltroAlumnoDTO(
                q, categoriaId, horarioId, estado, cinturonId, estadoMensualidad, sortBy, sortDir
        );

        Page<AlumnoResumenFilaDTO> pageAlumnos = alumnoService.buscarAlumnosPaginados(filtro, page, size);

        model.addAttribute("filtro", filtro);
        model.addAttribute("pageAlumnos", pageAlumnos);
        model.addAttribute("categorias", alumnoService.listarCategoriasActivas());
        model.addAttribute("horarios", alumnoService.listarTodosLosHorarios());
        model.addAttribute("cinturones", alumnoService.listarCinturonesOrdenados());
        model.addAttribute("estadosAlumno", EstadoAlumno.values());
        return "alumnos";
    }

    @GetMapping("/{id}")
    public String verExpediente(@PathVariable Long id, Model model) {
        ExpedienteAlumnoDTO expediente = alumnoService.obtenerExpedienteCompleto(id);
        model.addAttribute("exp", expediente);
        model.addAttribute("alumno", expediente.alumno());
        model.addAttribute("cinturones", alumnoService.listarCinturonesOrdenados());
        return "alumno-detalle";
    }

    @PostMapping("/{id}/estado")
    public String cambiarEstado(
            @PathVariable Long id,
            @RequestParam EstadoAlumno nuevoEstado,
            @RequestParam(required = false) String origen,
            RedirectAttributes redirectAttributes
    ) {
        try {
            alumnoService.cambiarEstado(id, nuevoEstado);
            redirectAttributes.addFlashAttribute("exito", "Estado del alumno actualizado a " + nuevoEstado + ".");
        } catch (Exception e) {
            manejarExcepcion(e, redirectAttributes);
        }
        if ("expediente".equalsIgnoreCase(origen)) {
            return "redirect:/alumnos/" + id;
        }
        return "redirect:/alumnos";
    }

    @PostMapping("/{id}/observacion")
    public String registrarObservacion(
            @PathVariable Long id,
            @RequestParam Long instructorId,
            @RequestParam String observacion,
            RedirectAttributes redirectAttributes
    ) {
        try {
            personalService.registrarObservacion(id, instructorId, observacion);
            redirectAttributes.addFlashAttribute("exito", "Observación técnica registrada en el expediente.");
        } catch (Exception e) {
            manejarExcepcion(e, redirectAttributes);
        }
        return "redirect:/alumnos/" + id;
    }

    @PostMapping("/{id}/reasignar-grupo")
    public String reasignarGrupoPorEdad(
            @PathVariable Long id,
            @RequestParam Long nuevoGrupoId,
            RedirectAttributes redirectAttributes
    ) {
        try {
            matriculaService.reasignarGrupoPorEdad(id, nuevoGrupoId);
            redirectAttributes.addFlashAttribute("exito", "Alumno reasignado exitosamente a su nueva categoría y horario.");
        } catch (Exception e) {
            manejarExcepcion(e, redirectAttributes);
        }
        return "redirect:/alumnos/" + id;
    }

    private void manejarExcepcion(Exception e, RedirectAttributes redirectAttributes) {
        if (e instanceof ReglaNegocioException || e instanceof RecursoNoEncontradoException
                || e instanceof IllegalArgumentException || e instanceof IllegalStateException) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } else {
            log.error("Error técnico en AlumnoWebController", e);
            redirectAttributes.addFlashAttribute("error", "Ocurrió un error al procesar la solicitud.");
        }
    }
}