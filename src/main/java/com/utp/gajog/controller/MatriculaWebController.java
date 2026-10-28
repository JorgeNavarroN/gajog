/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.utp.gajog.controller;

import com.utp.gajog.dto.PagoMensualidadForm;
import com.utp.gajog.enums.EstadoMatricula;
import com.utp.gajog.enums.MedioPago;
import com.utp.gajog.model.Pago;
import com.utp.gajog.repository.CategoriaRepository;
import com.utp.gajog.repository.GrupoRepository;
import com.utp.gajog.service.MatriculaService;
import com.utp.gajog.service.MensualidadService;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
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
@RequestMapping("/mensualidades")
public class MatriculaWebController {

    private final MatriculaService matriculaService;
    private final CategoriaRepository categoriaRepository;
    private final GrupoRepository grupoRepository;
    private static final ZoneId ZONA = ZoneId.of("America/Lima");
    private final MensualidadService mensualidadService;

    public MatriculaWebController(
            MatriculaService matriculaService,
            CategoriaRepository categoriaRepository,
            GrupoRepository grupoRepository,
            MensualidadService mensualidadService) {

        this.matriculaService = matriculaService;
        this.categoriaRepository = categoriaRepository;
        this.grupoRepository = grupoRepository;
        this.mensualidadService = mensualidadService;
    }

    @GetMapping("/matriculas")
    public String mostrarMatriculas(
            @RequestParam(name = "busqueda", defaultValue = "") String busqueda,
            @RequestParam(name = "categoriaId", required = false) Long categoriaId,
            @RequestParam(name = "grupoId", required = false) Long grupoId,
            @RequestParam(name = "estado", required = false) EstadoMatricula estado,
            Model model) {

        model.addAttribute(
                "matriculas",
                matriculaService.listar(
                        busqueda, categoriaId, grupoId, estado)
        );

        model.addAttribute("categorias", categoriaRepository.findAll());
        model.addAttribute("grupos", grupoRepository.findAll());
        model.addAttribute("estados", EstadoMatricula.values());

        // Conservan los filtros seleccionados al recargar la pantalla.
        model.addAttribute("busqueda", busqueda);
        model.addAttribute("categoriaId", categoriaId);
        model.addAttribute("grupoId", grupoId);
        model.addAttribute("estadoSeleccionado", estado);

        return "matriculas";
    }

    @GetMapping("/pagos/{pagoId}/editar")
    public String mostrarEdicion(
            @PathVariable("pagoId") Long pagoId,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {
            Pago pago = mensualidadService.obtenerPagoMensualidad(pagoId);

            PagoMensualidadForm formulario = new PagoMensualidadForm();
            formulario.setMedioPago(pago.getMedioPago());
            formulario.setFechaPago(pago.getFechaPago().toLocalDate());

            model.addAttribute("pagoForm", formulario);
            cargarEdicion(model, pago);

            return "mensualidades/editar-pago";

        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/mensualidades";
        }
    }

    @PostMapping("/pagos/{pagoId}/editar")
    public String guardarEdicion(
            @PathVariable("pagoId") Long pagoId,
            @ModelAttribute("pagoForm") PagoMensualidadForm formulario,
            BindingResult resultado,
            Model model,
            RedirectAttributes redirectAttributes) {

        Pago pago;

        try {
            pago = mensualidadService.obtenerPagoMensualidad(pagoId);
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/mensualidades";
        }

        if (resultado.hasErrors()) {
            model.addAttribute(
                    "error",
                    "Revisa la fecha y el medio de pago.");

            cargarEdicion(model, pago);
            return "mensualidades/editar-pago";
        }

        try {
            mensualidadService.editarPago(
                    pagoId,
                    formulario.getMedioPago(),
                    formulario.getFechaPago());

        } catch (IllegalArgumentException | IllegalStateException e) {
            model.addAttribute("error", e.getMessage());
            cargarEdicion(model, pago);

            return "mensualidades/editar-pago";
        }

        redirectAttributes.addFlashAttribute(
                "exito", "Pago actualizado correctamente.");

        redirectAttributes.addAttribute(
                "anio", pago.getMensualidad().getAnio());

        redirectAttributes.addAttribute(
                "mes", pago.getMensualidad().getMes());

        return "redirect:/mensualidades";
    }

    private void cargarEdicion(Model model, Pago pago) {
        model.addAttribute("pago", pago);
        model.addAttribute("mediosPago", MedioPago.values());
        model.addAttribute("fechaActual", LocalDate.now(ZONA));
    }
}
