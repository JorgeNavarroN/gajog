/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.utp.gajog.controller;

import com.utp.gajog.dto.PagoMensualidadForm;
import com.utp.gajog.enums.EstadoMensualidad;
import com.utp.gajog.enums.MedioPago;
import com.utp.gajog.model.Mensualidad;
import com.utp.gajog.model.Pago;
import com.utp.gajog.service.MensualidadService;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
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
 * @author JOAQUIN
 */
@Controller
@RequestMapping("/mensualidades")
public class MensualidadWebController {

    private static final ZoneId ZONA = ZoneId.of("America/Lima");

    private final MensualidadService mensualidadService;

    public MensualidadWebController(
            MensualidadService mensualidadService) {
        this.mensualidadService = mensualidadService;
    }

    @InitBinder("pagoForm")
    public void configurarFormulario(WebDataBinder binder) {
        binder.setAllowedFields(
                "mensualidadId",
                "monto",
                "medioPago",
                "fechaPago");
    }

    @GetMapping
    public String mostrar(
            @RequestParam(name = "anio", required = false) Integer anio,
            @RequestParam(name = "mes", required = false) Integer mes,
            Model model) {

        YearMonth periodo = resolverPeriodo(anio, mes);

        PagoMensualidadForm formulario = new PagoMensualidadForm();
        formulario.setFechaPago(LocalDate.now(ZONA));

        model.addAttribute("pagoForm", formulario);
        model.addAttribute("mostrarFormulario", false);

        cargarPantalla(model, periodo);

        return "mensualidades";
    }

    @PostMapping("/pagos")
    public String registrarPago(
            @ModelAttribute("pagoForm") PagoMensualidadForm formulario,
            BindingResult resultado,
            @RequestParam(name = "anio") Integer anio,
            @RequestParam(name = "mes") Integer mes,
            Model model,
            RedirectAttributes redirectAttributes) {

        YearMonth periodo = resolverPeriodo(anio, mes);

        if (resultado.hasErrors()) {
            model.addAttribute(
                    "error",
                    "Revisa el monto, la fecha y el medio de pago.");

            return mostrarConError(model, periodo);
        }

        try {
            mensualidadService.registrarPago(
                    formulario.getMensualidadId(),
                    formulario.getMonto(),
                    formulario.getMedioPago(),
                    formulario.getFechaPago());

        } catch (IllegalArgumentException | IllegalStateException e) {
            model.addAttribute("error", e.getMessage());

            return mostrarConError(model, periodo);
        }

        redirectAttributes.addFlashAttribute(
                "exito",
                "Pago registrado correctamente.");

        redirectAttributes.addAttribute("anio", periodo.getYear());
        redirectAttributes.addAttribute("mes", periodo.getMonthValue());

        return "redirect:/mensualidades";
    }

    private String mostrarConError(Model model, YearMonth periodo) {
        model.addAttribute("mostrarFormulario", true);
        cargarPantalla(model, periodo);

        return "mensualidades";
    }

    private void cargarPantalla(Model model, YearMonth periodo) {
        List<Mensualidad> mensualidades
                = mensualidadService.listarMensualidades(
                        periodo.getYear(),
                        periodo.getMonthValue());

        List<Mensualidad> pendientes = mensualidades.stream()
                .filter(mensualidad
                        -> mensualidad.getEstado() != EstadoMensualidad.PAGADA)
                .toList();

        long pagadas = mensualidades.stream()
                .filter(mensualidad
                        -> mensualidad.getEstado() == EstadoMensualidad.PAGADA)
                .count();

        model.addAttribute("anio", periodo.getYear());
        model.addAttribute("mes", periodo.getMonthValue());
        model.addAttribute("meses", Month.values());

        model.addAttribute("mensualidades", mensualidades);
        model.addAttribute("pendientes", pendientes);

        model.addAttribute(
                "pagos",
                mensualidadService.listarPagos(
                        periodo.getYear(),
                        periodo.getMonthValue()));

        model.addAttribute("totalPagadas", pagadas);
        model.addAttribute("totalPendientes", pendientes.size());

        model.addAttribute("mediosPago", MedioPago.values());
        model.addAttribute("fechaActual", LocalDate.now(ZONA));
    }

    private YearMonth resolverPeriodo(Integer anio, Integer mes) {
        YearMonth actual = YearMonth.now(ZONA);

        int anioSeleccionado = anio == null ? actual.getYear() : anio;
        int mesSeleccionado = mes == null ? actual.getMonthValue() : mes;

        if (anioSeleccionado < 1900
                || anioSeleccionado > 9999
                || mesSeleccionado < 1
                || mesSeleccionado > 12) {
            return actual;
        }

        return YearMonth.of(anioSeleccionado, mesSeleccionado);
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
