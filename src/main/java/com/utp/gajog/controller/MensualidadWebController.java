package com.utp.gajog.controller;

import com.utp.gajog.dto.*;
import com.utp.gajog.exception.RecursoNoEncontradoException;
import com.utp.gajog.exception.ReglaNegocioException;
import com.utp.gajog.model.*;
import com.utp.gajog.repository.CategoriaRepository;
import com.utp.gajog.repository.HorarioRepository;
import com.utp.gajog.service.AlumnoService;
import com.utp.gajog.service.PagoService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.*;

@Controller
public class MensualidadWebController {

    private static final Logger log = LoggerFactory.getLogger(MensualidadWebController.class);
    private static final String ERROR_GENERICO = "Ocurrió un error inesperado al procesar la operación.";

    private final PagoService pagoService;
    private final AlumnoService alumnoService;
    private final CategoriaRepository categoriaRepository;
    private final HorarioRepository horarioRepository;
    private final Clock clock;

    public MensualidadWebController(
            PagoService pagoService,
            AlumnoService alumnoService,
            CategoriaRepository categoriaRepository,
            HorarioRepository horarioRepository,
            Clock clock
    ) {
        this.pagoService = pagoService;
        this.alumnoService = alumnoService;
        this.categoriaRepository = categoriaRepository;
        this.horarioRepository = horarioRepository;
        this.clock = clock;
    }

    @GetMapping("/mensualidades")
    public String redirigirMensualidades() {
        return "redirect:/pagos";
    }

    @GetMapping("/pagos")
    public String vistaMensualidades(
            @RequestParam(required = false) Integer anio,
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) Long horarioId,
            @RequestParam(required = false) EstadoMensualidad estado,
            @RequestParam(required = false) MedioPago medioPago,
            @RequestParam(defaultValue = "por-vencer") String tab,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "false") boolean abrirFormulario,
            @RequestParam(required = false) Long alumnoId,
            @RequestParam(required = false) Long mensualidadId,
            @RequestParam(required = false) String returnUrl,
            Authentication auth,
            Model model
    ) {
        pagoService.actualizarMensualidadesVencidas();

        LocalDate hoy = LocalDate.now(clock);
        int anioFiltro = (anio != null) ? anio : hoy.getYear();
        int mesFiltro = (mes != null && mes >= 1 && mes <= 12) ? mes : hoy.getMonthValue();

        FiltroMensualidadDTO filtro = new FiltroMensualidadDTO(
                anioFiltro, mesFiltro, q, categoriaId, horarioId, estado, medioPago, tab
        );

        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(50, Math.max(5, size)));
        Page<Mensualidad> pagePorCobrar = pagoService.buscarMensualidadesPorCobrar(filtro, pageable);
        Page<Pago> pagePagados = pagoService.buscarPagosRealizados(filtro, pageable);
        ResumenMensualidadDTO resumen = pagoService.obtenerResumenPeriodo(anioFiltro, mesFiltro);
        List<Mensualidad> enRiesgoRetiro = pagoService.listarAlumnosEnRiesgoRetiro();
        List<AlumnoPagoAutocompletadoDTO> alumnosAutocompletado = pagoService.listarAlumnosParaFormularioPago();

        Map<Long, String> dniEnmascaradoPorAlumno = new HashMap<>();
        for (AlumnoPagoAutocompletadoDTO dto : alumnosAutocompletado) {
            dniEnmascaradoPorAlumno.put(dto.id(), dto.dniEnmascarado());
        }

        RegistrarPagoFormDTO formPago = construirFormularioInicial(
                hoy, anioFiltro, mesFiltro, alumnoId, mensualidadId, returnUrl
        );

        model.addAttribute("filtro", filtro);
        model.addAttribute("nombreMesSeleccionado", obtenerNombreMes(mesFiltro));
        model.addAttribute("mesesCatalogo", obtenerMapaMeses());
        model.addAttribute("resumen", resumen);
        model.addAttribute("pagePorCobrar", pagePorCobrar);
        model.addAttribute("pagePagados", pagePagados);
        model.addAttribute("enRiesgoRetiro", enRiesgoRetiro);
        model.addAttribute("alumnosAutocompletado", alumnosAutocompletado);
        model.addAttribute("dniEnmascaradoPorAlumno", dniEnmascaradoPorAlumno);
        model.addAttribute("categorias", categoriaRepository.findByActivoTrueOrderByEdadMinimaAsc());
        // CORRECCIÓN CLAVE: usar buscarHorariosConDetalle para traer la Categoría con JOIN FETCH
        model.addAttribute("horarios", horarioRepository.buscarHorariosConDetalle(null, null));
        model.addAttribute("mediosPago", MedioPago.values());
        model.addAttribute("formPago", formPago);
        model.addAttribute("abrirFormulario", abrirFormulario || alumnoId != null || mensualidadId != null);
        model.addAttribute("puedeEditarMonto", tienePermisoEdicionTarifa(auth));

        return "pagos";
    }

    @GetMapping("/pagos/nuevo")
    public String accesoDirectoNuevoPago(
            @RequestParam(required = false) Long alumnoId,
            @RequestParam(required = false) Long mensualidadId,
            @RequestParam(required = false) String returnUrl,
            RedirectAttributes redirectAttributes
    ) {
        redirectAttributes.addAttribute("abrirFormulario", true);
        if (alumnoId != null) {
            redirectAttributes.addAttribute("alumnoId", alumnoId);
        }
        if (mensualidadId != null) {
            Mensualidad m = pagoService.obtenerMensualidadPorId(mensualidadId);
            redirectAttributes.addAttribute("mensualidadId", mensualidadId);
            redirectAttributes.addAttribute("anio", m.getAnio());
            redirectAttributes.addAttribute("mes", m.getMes());
        }
        if (esUrlRetornoSegura(returnUrl)) {
            redirectAttributes.addAttribute("returnUrl", returnUrl);
        }
        return "redirect:/pagos";
    }

    @PostMapping("/pagos/registrar")
    public String registrarPago(
            @Valid @ModelAttribute("formPago") RegistrarPagoFormDTO formPago,
            BindingResult bindingResult,
            Authentication auth,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            String msg = bindingResult.getAllErrors().getFirst().getDefaultMessage();
            redirectAttributes.addFlashAttribute("error", msg);
            return redirigirSegunOrigen(formPago.getReturnUrl(), formPago.getAnio(), formPago.getMes(), "por-vencer");
        }

        try {
            boolean puedeEditarMonto = tienePermisoEdicionTarifa(auth);
            Pago pago = pagoService.registrarPagoMensualidadDesdeFormulario(formPago, puedeEditarMonto);
            String correlativo = pagoService.obtenerNumeroCorrelativoRecibo(pago);
            redirectAttributes.addFlashAttribute("exito",
                    "Pago registrado correctamente (" + correlativo + " · S/ " + pago.getMonto() + ").");
            return redirigirSegunOrigen(formPago.getReturnUrl(), formPago.getAnio(), formPago.getMes(), "pagados");
        } catch (Exception e) {
            manejarExcepcion(e, redirectAttributes);
            return redirigirSegunOrigen(formPago.getReturnUrl(), formPago.getAnio(), formPago.getMes(), "por-vencer");
        }
    }

    @PostMapping("/pagos/mensualidad/pagar")
    public String cobrarRapidoMensualidad(
            @RequestParam Long mensualidadId,
            @RequestParam MedioPago medioPago,
            @RequestParam(required = false) Integer anio,
            @RequestParam(required = false) Integer mes,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Pago pago = pagoService.pagarMensualidad(mensualidadId, medioPago);
            String correlativo = pagoService.obtenerNumeroCorrelativoRecibo(pago);
            redirectAttributes.addFlashAttribute("exito",
                    "Cobro registrado exitosamente (" + correlativo + ").");
        } catch (Exception e) {
            manejarExcepcion(e, redirectAttributes);
        }
        return redirigirSegunOrigen(null, anio, mes, "pagados");
    }

    @PostMapping("/pagos/{id}/editar")
    public String editarPago(
            @PathVariable Long id,
            @Valid @ModelAttribute EditarPagoFormDTO form,
            BindingResult bindingResult,
            @RequestParam(required = false) Integer anio,
            @RequestParam(required = false) Integer mes,
            Authentication auth,
            RedirectAttributes redirectAttributes
    ) {
        form.setPagoId(id);
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", bindingResult.getAllErrors().getFirst().getDefaultMessage());
            return redirigirSegunOrigen(null, anio, mes, "pagados");
        }

        try {
            boolean puedeEditarMonto = tienePermisoEdicionTarifa(auth);
            Pago actualizado = pagoService.editarPago(form, puedeEditarMonto);
            redirectAttributes.addFlashAttribute("exito",
                    "Pago actualizado correctamente. Auditoría registrada por " + actualizado.getModificadoPor() + ".");
        } catch (Exception e) {
            manejarExcepcion(e, redirectAttributes);
        }
        return redirigirSegunOrigen(null, anio, mes, "pagados");
    }

    @PostMapping("/pagos/{id}/anular")
    public String anularPago(
            @PathVariable Long id,
            @RequestParam String motivoAnulacion,
            @RequestParam(required = false) Integer anio,
            @RequestParam(required = false) Integer mes,
            RedirectAttributes redirectAttributes
    ) {
        try {
            pagoService.anularPago(id, motivoAnulacion);
            redirectAttributes.addFlashAttribute("exito",
                    "Pago anulado con éxito. La mensualidad volvió a estado Por vencer / Vencida.");
        } catch (Exception e) {
            manejarExcepcion(e, redirectAttributes);
        }
        return redirigirSegunOrigen(null, anio, mes, "por-vencer");
    }

    @PostMapping("/pagos/mensualidad/masivo")
    public String generarMensualidadesMasivas(
            @RequestParam Integer anio,
            @RequestParam Integer mes,
            @RequestParam(required = false) BigDecimal monto,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaVencimiento,
            RedirectAttributes redirectAttributes
    ) {
        try {
            int cantidad = pagoService.generarMensualidadesMasivas(anio, mes, monto, fechaVencimiento);
            redirectAttributes.addFlashAttribute("exito",
                    "Se generaron " + cantidad + " mensualidades para los alumnos con matrícula activa.");
        } catch (Exception e) {
            manejarExcepcion(e, redirectAttributes);
        }
        return redirigirSegunOrigen(null, anio, mes, "por-vencer");
    }

    @PostMapping("/pagos/mensualidad/generar")
    public String generarMensualidadIndividual(
            @RequestParam Long alumnoId,
            @RequestParam Integer anio,
            @RequestParam Integer mes,
            @RequestParam(required = false) BigDecimal monto,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaVencimiento,
            RedirectAttributes redirectAttributes
    ) {
        try {
            pagoService.generarMensualidad(alumnoId, anio, mes, monto, fechaVencimiento);
            redirectAttributes.addFlashAttribute("exito", "Mensualidad individual generada correctamente.");
        } catch (Exception e) {
            manejarExcepcion(e, redirectAttributes);
        }
        return redirigirSegunOrigen(null, anio, mes, "por-vencer");
    }

    @PostMapping("/pagos/mora/{alumnoId}/retirar")
    public String confirmarRetiroPorMora(
            @PathVariable Long alumnoId,
            @RequestParam String motivo,
            @RequestParam(required = false) Integer anio,
            @RequestParam(required = false) Integer mes,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Alumno retirado = pagoService.confirmarRetiroPorMora(alumnoId, motivo);
            redirectAttributes.addFlashAttribute("exito",
                    "Retiro por mora confirmado para " + retirado.getPersona().getNombres() + " " + retirado.getPersona().getApellidos() + ".");
        } catch (Exception e) {
            manejarExcepcion(e, redirectAttributes);
        }
        return redirigirSegunOrigen(null, anio, mes, "por-vencer");
    }

    @PostMapping("/pagos/mora/{alumnoId}/reincorporar")
    public String reincorporarAlumno(
            @PathVariable Long alumnoId,
            @RequestParam(required = false) String observacion,
            @RequestParam(required = false) String returnUrl,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Alumno activo = pagoService.reincorporarAlumno(alumnoId, observacion);
            redirectAttributes.addFlashAttribute("exito",
                    "Alumno " + activo.getPersona().getNombres() + " reincorporado a estado ACTIVO conservando todo su historial.");
        } catch (Exception e) {
            manejarExcepcion(e, redirectAttributes);
        }
        if (esUrlRetornoSegura(returnUrl)) {
            return "redirect:" + returnUrl;
        }
        return "redirect:/alumnos/" + alumnoId;
    }

    @GetMapping("/pagos/{id}/recibo")
    public String verReciboPago(@PathVariable Long id, Model model) {
        Pago pago = pagoService.obtenerPagoPorId(id);
        boolean esMenor = pagoService.esMenorDeEdad(pago.getAlumno().getPersona().getFechaNacimiento(), LocalDate.now(clock));
        model.addAttribute("pago", pago);
        model.addAttribute("numeroCorrelativo", pagoService.obtenerNumeroCorrelativoRecibo(pago));
        model.addAttribute("dniAlumnoRecibo",
                AlumnoPagoAutocompletadoDTO.enmascararDni(pago.getAlumno().getPersona().getDni(), esMenor));
        return "recibo-pago";
    }

    private RegistrarPagoFormDTO construirFormularioInicial(
            LocalDate hoy,
            int anioFiltro,
            int mesFiltro,
            Long alumnoId,
            Long mensualidadId,
            String returnUrl
    ) {
        RegistrarPagoFormDTO form = new RegistrarPagoFormDTO();
        form.setAnio(anioFiltro);
        form.setMes(mesFiltro);
        form.setFechaPago(hoy);
        form.setMedioPago(MedioPago.EFECTIVO);
        form.setMonto(new BigDecimal("100.00"));

        if (esUrlRetornoSegura(returnUrl)) {
            form.setReturnUrl(returnUrl);
        }

        if (mensualidadId != null) {
            Mensualidad m = pagoService.obtenerMensualidadPorId(mensualidadId);
            form.setMensualidadId(m.getId());
            form.setAlumnoId(m.getAlumno().getId());
            form.setAnio(m.getAnio());
            form.setMes(m.getMes());
            form.setMonto(m.getMonto());
        } else if (alumnoId != null) {
            Alumno a = alumnoService.obtenerPorId(alumnoId);
            form.setAlumnoId(a.getId());
            form.setMonto(pagoService.obtenerTarifaVigenteDeAlumno(a));
        }
        return form;
    }

    private boolean tienePermisoEdicionTarifa(Authentication auth) {
        if (auth == null || auth.getAuthorities() == null) {
            return false;
        }
        return auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMINISTRADOR".equals(a.getAuthority()));
    }

    private boolean esUrlRetornoSegura(String url) {
        return url != null
                && !url.isBlank()
                && url.startsWith("/")
                && !url.startsWith("//")
                && !url.contains("://")
                && !url.contains("\r")
                && !url.contains("\n");
    }

    private String redirigirSegunOrigen(String returnUrl, Integer anio, Integer mes, String tab) {
        if (esUrlRetornoSegura(returnUrl)) {
            return "redirect:" + returnUrl;
        }
        int a = (anio != null) ? anio : LocalDate.now(clock).getYear();
        int m = (mes != null) ? mes : LocalDate.now(clock).getMonthValue();
        return "redirect:/pagos?anio=" + a + "&mes=" + m + "&tab=" + tab;
    }

    private String obtenerNombreMes(int mes) {
        String nombre = Month.of(mes).getDisplayName(TextStyle.FULL, Locale.of("es", "PE"));
        return nombre.substring(0, 1).toUpperCase() + nombre.substring(1);
    }

    private Map<Integer, String> obtenerMapaMeses() {
        Map<Integer, String> mapa = new LinkedHashMap<>();
        for (int i = 1; i <= 12; i++) {
            mapa.put(i, obtenerNombreMes(i));
        }
        return mapa;
    }

    private void manejarExcepcion(Exception e, RedirectAttributes redirectAttributes) {
        if (e instanceof ReglaNegocioException || e instanceof RecursoNoEncontradoException
                || e instanceof IllegalArgumentException || e instanceof IllegalStateException) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } else {
            log.error("Error técnico en MensualidadWebController", e);
            redirectAttributes.addFlashAttribute("error", ERROR_GENERICO);
        }
    }
}