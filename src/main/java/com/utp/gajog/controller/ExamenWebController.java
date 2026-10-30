package com.utp.gajog.controller;

import com.utp.gajog.dto.FilaExamenDTO;
import com.utp.gajog.enums.MedioPago;
import com.utp.gajog.enums.ResultadoExamen;
import com.utp.gajog.model.Alumno;
import com.utp.gajog.model.Examen;
import com.utp.gajog.service.ExamenConsultaService;
import com.utp.gajog.service.ExamenEvaluacionService;
import com.utp.gajog.service.ExamenPagoService;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * BASE COMPARTIDA del módulo de exámenes (BPMN 3).
 * Muestra la pantalla "Exámenes de cinturón" con sus 3 pestañas:
 * 1. Pago (mitad A), 2. Evaluación (mitad B) y 3. Resultado (mitad B).
 */
@Controller
@RequestMapping("/examenes")
public class ExamenWebController {

    private static final ZoneId ZONA = ZoneId.of("America/Lima");

    private final ExamenConsultaService consultaService;
    private final ExamenPagoService pagoService;
    private final ExamenEvaluacionService evaluacionService;

    public ExamenWebController(
            ExamenConsultaService consultaService,
            ExamenPagoService pagoService,
            ExamenEvaluacionService evaluacionService) {
        this.consultaService = consultaService;
        this.pagoService = pagoService;
        this.evaluacionService = evaluacionService;
    }

    @GetMapping
    public String mostrar(
            @RequestParam(name = "alumno", required = false) Long alumnoId,
            @RequestParam(name = "tab", required = false) String tab,
            Model model) {

        List<FilaExamenDTO> filas = consultaService.listarFilas();

        model.addAttribute("filas", filas);
        model.addAttribute("mediosPago", MedioPago.values());
        model.addAttribute("seleccionado", null);

        Optional<FilaExamenDTO> fila = filas.stream()
                .filter(f -> f.getAlumno().getId().equals(alumnoId))
                .findFirst();

        if (fila.isEmpty()) {
            return "examenes";
        }

        Alumno alumno = fila.get().getAlumno();
        Examen examen = fila.get().getExamen();
        boolean pagado = fila.get().isPagado();
        boolean pendiente = examen != null
                && examen.getResultado() == ResultadoExamen.PENDIENTE;

        model.addAttribute("seleccionado", alumno);
        model.addAttribute("examen", examen);
        model.addAttribute("examenPendiente", pendiente);
        model.addAttribute("pagado", pagado);
        model.addAttribute("estadoExamen", fila.get().getEstado());

        // Mitad A: datos del pago.
        model.addAttribute("tarifaExamen", pagoService.tarifaExamen());
        model.addAttribute("hoy", LocalDate.now(ZONA));
        model.addAttribute("requisitos",
                pagoService.consultarRequisitos(alumno.getId()));
        model.addAttribute("proximoCinturon",
                pagoService.proximoCinturon(alumno.getId()).orElse(null));
        model.addAttribute("pagoExamen",
                pendiente
                        ? pagoService.pagoDelExamen(examen.getId()).orElse(null)
                        : null);

        // Mitad B: criterios, detalle del último examen e historial.
        model.addAttribute("criterios", evaluacionService.listarCriterios());
        model.addAttribute("puntajeMinimo", evaluacionService.getPuntajeMinimo());
        model.addAttribute("puntajeMaximoTotal",
                evaluacionService.puntajeMaximoTotal());
        model.addAttribute("detalles",
                examen == null
                        ? List.of()
                        : evaluacionService.listarDetalle(examen.getId()));
        model.addAttribute("historial",
                evaluacionService.listarHistorial(alumno.getId()));
        model.addAttribute("examenesAlumno",
                consultaService.listarExamenesDelAlumno(alumno.getId()));

        model.addAttribute("tab", resolverPestana(tab, examen, pendiente, pagado));

        return "examenes";
    }

    /** Pestaña inicial según el punto del BPMN en el que está el alumno. */
    private String resolverPestana(
            String solicitada,
            Examen examen,
            boolean pendiente,
            boolean pagado) {

        if ("pago".equals(solicitada)
                || "evaluacion".equals(solicitada)
                || "resultado".equals(solicitada)) {
            return solicitada;
        }

        if (examen == null) {
            return "pago";
        }

        if (pendiente) {
            return pagado ? "evaluacion" : "pago";
        }

        return "resultado";
    }
}
