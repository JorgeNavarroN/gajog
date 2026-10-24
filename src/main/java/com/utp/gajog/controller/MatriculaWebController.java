package com.utp.gajog.controller;

import com.utp.gajog.dto.FiltroMatriculaDTO;
import com.utp.gajog.dto.HorarioDisponibleDTO;
import com.utp.gajog.dto.MatriculaWizardDTO;
import com.utp.gajog.exception.RecursoNoEncontradoException;
import com.utp.gajog.exception.ReglaNegocioException;
import com.utp.gajog.model.DiaSemana;
import com.utp.gajog.model.EstadoMatricula;
import com.utp.gajog.model.Matricula;
import com.utp.gajog.service.AlumnoService;
import com.utp.gajog.service.MatriculaService;
import com.utp.gajog.service.PersonalService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@SessionAttributes("wizardMatricula")
public class MatriculaWebController {

    private static final Logger log = LoggerFactory.getLogger(MatriculaWebController.class);

    private final MatriculaService matriculaService;
    private final AlumnoService alumnoService;
    private final PersonalService personalService;

    public MatriculaWebController(
            MatriculaService matriculaService,
            AlumnoService alumnoService,
            PersonalService personalService
    ) {
        this.matriculaService = matriculaService;
        this.alumnoService = alumnoService;
        this.personalService = personalService;
    }

    @ModelAttribute("wizardMatricula")
    public MatriculaWizardDTO inicializarWizardSesion() {
        return new MatriculaWizardDTO();
    }

    // ==================== LISTADO DE MATRÍCULAS Y HORARIOS ====================

    @GetMapping("/matriculas")
    public String listarMatriculas(
            @RequestParam(defaultValue = "TODAS") String tab,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) Long horarioId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model
    ) {
        FiltroMatriculaDTO filtro = new FiltroMatriculaDTO(tab, q, categoriaId, horarioId);
        Page<Matricula> pageMatriculas = matriculaService.buscarMatriculasPaginadas(filtro, page, size);

        model.addAttribute("filtro", filtro);
        model.addAttribute("pageMatriculas", pageMatriculas);
        model.addAttribute("conteosPestania", matriculaService.contarMatriculasPorPestania());
        model.addAttribute("categorias", alumnoService.listarCategoriasActivas());
        model.addAttribute("horarios", alumnoService.listarTodosLosHorarios());
        model.addAttribute("desfasesEdad", matriculaService.detectarAlumnosConDesfaseDeEdad());
        return "matriculas";
    }

    @GetMapping("/horarios")
    public String vistaHorarios(
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) DiaSemana diaSemana,
            Model model
    ) {
        List<HorarioDisponibleDTO> horarios = matriculaService.buscarHorariosParaPaso3(categoriaId, diaSemana, null);
        model.addAttribute("horariosDisponibles", horarios);
        model.addAttribute("categorias", alumnoService.listarCategoriasActivas());
        model.addAttribute("diasSemana", DiaSemana.values());
        model.addAttribute("categoriaIdSeleccionada", categoriaId);
        model.addAttribute("diaSeleccionado", diaSemana);
        return "horarios";
    }

    @PostMapping("/matriculas/{id}/estado")
    public String cambiarEstadoMatricula(
            @PathVariable Long id,
            @RequestParam EstadoMatricula nuevoEstado,
            RedirectAttributes redirectAttributes
    ) {
        try {
            matriculaService.cambiarEstado(id, nuevoEstado);
            redirectAttributes.addFlashAttribute("exito", "Estado de la matrícula actualizado a " + nuevoEstado + ".");
        } catch (Exception e) {
            manejarExcepcion(e, redirectAttributes);
        }
        return "redirect:/matriculas";
    }

    // ==================== WIZARD DE MATRÍCULA EN 3 PASOS ====================

    @GetMapping("/matriculas/nueva")
    public String iniciarNuevaMatricula(
            @RequestParam(required = false) Long alumnoId,
            @RequestParam(required = false) Long matriculaId,
            @RequestParam(defaultValue = "false") boolean reiniciar,
            @ModelAttribute("wizardMatricula") MatriculaWizardDTO wizardActual,
            Model model
    ) {
        if (reiniciar || alumnoId != null || matriculaId != null) {
            MatriculaWizardDTO nuevo = matriculaService.inicializarWizardDesdeAlumnoExistente(alumnoId, matriculaId);
            model.addAttribute("wizardMatricula", nuevo);
        }
        return "redirect:/matriculas/nueva/paso-1";
    }

    @GetMapping("/matriculas/nueva/paso-1")
    public String mostrarPaso1(
            @ModelAttribute("wizardMatricula") MatriculaWizardDTO wizard,
            Model model
    ) {
        model.addAttribute("pasoActual", 1);
        model.addAttribute("cinturones", alumnoService.listarCinturonesOrdenados());
        model.addAttribute("apoderados", personalService.listarApoderados());
        return "matricula-wizard";
    }

    @PostMapping("/matriculas/nueva/paso-1")
    public String procesarPaso1(
            @Valid @ModelAttribute("wizardMatricula") MatriculaWizardDTO wizard,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("pasoActual", 1);
            model.addAttribute("cinturones", alumnoService.listarCinturonesOrdenados());
            model.addAttribute("apoderados", personalService.listarApoderados());
            model.addAttribute("error", bindingResult.getAllErrors().getFirst().getDefaultMessage());
            return "matricula-wizard";
        }

        try {
            matriculaService.validarPaso1YCalcularRevision(wizard);
            return "redirect:/matriculas/nueva/paso-2";
        } catch (Exception e) {
            manejarExcepcion(e, redirectAttributes);
            return "redirect:/matriculas/nueva/paso-1";
        }
    }

    @GetMapping("/matriculas/nueva/paso-2")
    public String mostrarPaso2(
            @ModelAttribute("wizardMatricula") MatriculaWizardDTO wizard,
            Model model
    ) {
        if (!wizard.isPaso1Completado()) {
            return "redirect:/matriculas/nueva/paso-1";
        }
        model.addAttribute("pasoActual", 2);
        return "matricula-wizard";
    }

    @PostMapping("/matriculas/nueva/paso-2")
    public String procesarPaso2(@ModelAttribute("wizardMatricula") MatriculaWizardDTO wizard) {
        if (!wizard.isPaso1Completado()) {
            return "redirect:/matriculas/nueva/paso-1";
        }
        wizard.setPaso2Completado(true);
        wizard.setFiltroCategoriaPaso3Id(wizard.getCategoriaAsignadaId());
        return "redirect:/matriculas/nueva/paso-3";
    }

    @GetMapping("/matriculas/nueva/paso-3")
    public String mostrarPaso3(
            @ModelAttribute("wizardMatricula") MatriculaWizardDTO wizard,
            @RequestParam(required = false) Long filtroCategoriaId,
            @RequestParam(required = false) DiaSemana filtroDia,
            @RequestParam(required = false) String filtroTurno,
            Model model
    ) {
        if (!wizard.isPaso1Completado()) {
            return "redirect:/matriculas/nueva/paso-1";
        }

        Long catIdEfectiva = (filtroCategoriaId != null) ? filtroCategoriaId : wizard.getCategoriaAsignadaId();
        wizard.setFiltroCategoriaPaso3Id(catIdEfectiva);
        wizard.setFiltroDiaPaso3(filtroDia);
        wizard.setFiltroTurnoPaso3(filtroTurno);

        List<HorarioDisponibleDTO> horariosDisponibles = matriculaService.buscarHorariosParaPaso3(
                catIdEfectiva, filtroDia, filtroTurno
        );

        model.addAttribute("pasoActual", 3);
        model.addAttribute("horariosDisponibles", horariosDisponibles);
        model.addAttribute("categorias", alumnoService.listarCategoriasActivas());
        model.addAttribute("diasSemana", DiaSemana.values());
        return "matricula-wizard";
    }

    @PostMapping("/matriculas/nueva/confirmar")
    public String confirmarMatricula(
            @ModelAttribute("wizardMatricula") MatriculaWizardDTO wizard,
            SessionStatus sessionStatus,
            RedirectAttributes redirectAttributes
    ) {
        try {
            String nombreAlumno = wizard.getNombreCompletoAlumno();
            String nombreCategoria = wizard.getCategoriaAsignadaNombre();

            matriculaService.confirmarMatriculaWizard(wizard);
            sessionStatus.setComplete();

            redirectAttributes.addFlashAttribute("exito",
                    "Matrícula confirmada exitosamente para " + nombreAlumno
                    + " en la categoría " + nombreCategoria
                    + " y primera mensualidad generada.");
            return "redirect:/matriculas";
        } catch (Exception e) {
            manejarExcepcion(e, redirectAttributes);
            return "redirect:/matriculas/nueva/paso-3";
        }
    }

    private void manejarExcepcion(Exception e, RedirectAttributes redirectAttributes) {
        if (e instanceof ReglaNegocioException || e instanceof RecursoNoEncontradoException
                || e instanceof IllegalArgumentException || e instanceof IllegalStateException) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } else {
            log.error("Error técnico en MatriculaWebController", e);
            redirectAttributes.addFlashAttribute("error", "Ocurrió un error al procesar la matrícula.");
        }
    }
}