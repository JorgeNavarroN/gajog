/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.utp.gajog.controller;

import com.utp.gajog.dto.AsistenciaForm;
import com.utp.gajog.dto.SesionClaseForm;
import com.utp.gajog.model.Horario;
import com.utp.gajog.model.SesionClase;
import com.utp.gajog.repository.HorarioRepository;
import com.utp.gajog.repository.SesionClaseRepository;
import com.utp.gajog.service.AsistenciaService;
import com.utp.gajog.service.SesionClaseService;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 *
 * @author JORGE
 */
@Controller
@RequestMapping("/asistencias")
public class AsistenciaWebController {

    private final AsistenciaService asistenciaService;
    private final SesionClaseRepository sesionClaseRepository;
    private final SesionClaseService sesionClaseService;
    private final HorarioRepository horarioRepository;

    public AsistenciaWebController(
            AsistenciaService asistenciaService,
            SesionClaseRepository sesionClaseRepository,
            SesionClaseService sesionClaseService,
            HorarioRepository horarioRepository) {

        this.asistenciaService = asistenciaService;
        this.sesionClaseRepository = sesionClaseRepository;
        this.sesionClaseService = sesionClaseService;
        this.horarioRepository = horarioRepository;
    }

    @InitBinder("asistenciaForm")
    public void configurarFormulario(WebDataBinder binder) {
        binder.setAllowedFields(
                "detalles[*].alumnoId",
                "detalles[*].estado");
    }

    @GetMapping("/sesiones/{sesionId}")
    public String mostrarLista(
            @PathVariable("sesionId") Long sesionId,
            Model model) {

        SesionClase sesion = obtenerSesion(sesionId);

        SesionClaseForm formulario = new SesionClaseForm();
        formulario.setHorarioId(sesion.getHorario().getId());
        formulario.setFecha(sesion.getFecha());

        model.addAttribute("sesion", sesion);
        model.addAttribute("sesionForm", formulario);
        model.addAttribute(
                "asistenciaForm",
                asistenciaService.prepararFormulario(sesionId));

        cargarOpcionesSesion(model);

        return "asistencias/pantalla";
    }

    @PostMapping("/sesiones/{sesionId}")
    public String guardarLista(
            @PathVariable("sesionId") Long sesionId,
            @ModelAttribute("asistenciaForm") AsistenciaForm formulario,
            BindingResult resultado,
            RedirectAttributes redirectAttributes) {

        if (resultado.hasErrors()) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "Revisa los valores de asistencia enviados.");

            return "redirect:/asistencias/sesiones/" + sesionId;
        }

        try {
            asistenciaService.guardar(sesionId, formulario);

            redirectAttributes.addFlashAttribute(
                    "exito",
                    "Asistencia guardada correctamente.");

        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute(
                    "error", e.getMessage());
        }

        return "redirect:/asistencias/sesiones/" + sesionId;
    }

    private SesionClase obtenerSesion(Long sesionId) {
        return sesionClaseRepository.buscarParaPantalla(sesionId)
                .orElseThrow(()
                        -> new org.springframework.web.server.ResponseStatusException(
                                org.springframework.http.HttpStatus.NOT_FOUND,
                                "La sesión no existe."));
    }

    @InitBinder("sesionForm")
    public void configurarSesion(WebDataBinder binder) {
        binder.setAllowedFields("horarioId", "fecha");
    }

    @GetMapping
    public String mostrarInicio(
            @RequestParam(name = "horarioId", required = false) Long horarioId,
            @RequestParam(name = "fecha", required = false)
            @org.springframework.format.annotation.DateTimeFormat(
                    iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate fecha,
            Model model) {

        ZoneId zona = ZoneId.of("America/Lima");
        LocalDate fechaSeleccionada = fecha == null
                ? LocalDate.now(zona) : fecha;

        List<Horario> horarios = horarioRepository
                .findByGrupo_ActivoTrueOrderByGrupo_NombreAscDiaSemanaAscHoraInicioAsc();

        Horario seleccionado = null;

        if (horarioId != null) {
            seleccionado = horarios.stream()
                    .filter(h -> h.getId().equals(horarioId))
                    .findFirst()
                    .orElse(null);

        } else if (fechaSeleccionada.equals(LocalDate.now(zona))) {
            LocalTime ahora = LocalTime.now(zona);

            String diaActual = fechaSeleccionada.getDayOfWeek()
                    .getDisplayName(
                            TextStyle.FULL,
                            Locale.forLanguageTag("es-PE"));

            String diaNormalizado = java.text.Normalizer.normalize(
                    diaActual, java.text.Normalizer.Form.NFD)
                    .replaceAll("\\p{M}", "")
                    .toUpperCase(Locale.ROOT);

            seleccionado = horarios.stream()
                    .filter(h -> h.getDiaSemana() != null
                            && h.getHoraInicio() != null
                            && h.getHoraFin() != null)
                    .filter(h -> h.getDiaSemana().name().equals(diaNormalizado))
                    .filter(h -> !ahora.isBefore(h.getHoraInicio())
                            && ahora.isBefore(h.getHoraFin()))
                    .findFirst()
                    .orElse(null);
        }

        if (seleccionado != null) {
            List<SesionClase> existentes = sesionClaseRepository
                    .findByHorario_IdAndFecha(
                            seleccionado.getId(), fechaSeleccionada);

            if (existentes.size() == 1) {
                return "redirect:/asistencias/sesiones/"
                        + existentes.get(0).getId();
            }

            if (existentes.size() > 1) {
                model.addAttribute(
                        "error", "Existen sesiones duplicadas para esta clase.");
            }
        }

        SesionClaseForm formulario = new SesionClaseForm();
        formulario.setFecha(fechaSeleccionada);

        if (seleccionado != null) {
            formulario.setHorarioId(seleccionado.getId());
        }

        model.addAttribute("sesionForm", formulario);
        model.addAttribute("sesion", null);
        model.addAttribute("asistenciaForm", new AsistenciaForm());

        cargarOpcionesSesion(model);

        return "asistencias/pantalla";
    }

    @PostMapping("/sesiones")
    public String abrirSesion(
            @ModelAttribute("sesionForm") SesionClaseForm formulario,
            BindingResult resultado,
            Model model) {

        if (resultado.hasErrors()) {
            model.addAttribute(
                    "error",
                    "Revisa el horario, instructor y fecha.");

            cargarOpcionesSesion(model);

            model.addAttribute("sesion", null);
            model.addAttribute("asistenciaForm", new AsistenciaForm());

            return "asistencias/pantalla";
        }

        try {
            Long sesionId = sesionClaseService.abrirSesion(formulario);

            return "redirect:/asistencias/sesiones/" + sesionId;

        } catch (IllegalArgumentException | IllegalStateException e) {
            model.addAttribute("error", e.getMessage());
            cargarOpcionesSesion(model);

            model.addAttribute("sesion", null);
            model.addAttribute("asistenciaForm", new AsistenciaForm());

            return "asistencias/pantalla";
        }
    }

    private void cargarOpcionesSesion(Model model) {
        model.addAttribute(
                "horarios",
                horarioRepository
                        .findByGrupo_ActivoTrueOrderByGrupo_NombreAscDiaSemanaAscHoraInicioAsc());
    }
}
