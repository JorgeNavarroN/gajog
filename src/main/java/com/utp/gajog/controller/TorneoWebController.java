package com.utp.gajog.controller;

import com.utp.gajog.dto.InscripcionTorneoForm;
import com.utp.gajog.dto.MarcadorDTO;
import com.utp.gajog.dto.TorneoForm;
import com.utp.gajog.enums.EstadoTorneo;
import com.utp.gajog.enums.LadoCombate;
import com.utp.gajog.enums.MedioPago;
import com.utp.gajog.enums.TecnicaRompimiento;
import com.utp.gajog.enums.ModalidadTorneo;
import com.utp.gajog.enums.TipoEventoCombate;
import com.utp.gajog.model.InscripcionTorneo;
import com.utp.gajog.model.Torneo;
import com.utp.gajog.service.CompetenciaService;
import com.utp.gajog.service.ExportarTorneoService;
import com.utp.gajog.service.InscripcionTorneoService;
import com.utp.gajog.service.PoomsaeService;
import com.utp.gajog.service.RankingTorneoService;
import com.utp.gajog.service.RompimientoService;
import com.utp.gajog.service.TorneoService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Pantallas del módulo de torneos: lista, nuevo, detalle con inscripciones
 * y llaves, marcador de combate, calificación de poomsae, resultados y ranking.
 */
@Controller
@RequestMapping("/torneos")
public class TorneoWebController {

    private final TorneoService torneoService;
    private final InscripcionTorneoService inscripcionService;
    private final CompetenciaService competenciaService;
    private final PoomsaeService poomsaeService;
    private final RompimientoService rompimientoService;
    private final RankingTorneoService rankingService;
    private final ExportarTorneoService exportarService;

    public TorneoWebController(
            TorneoService torneoService,
            InscripcionTorneoService inscripcionService,
            CompetenciaService competenciaService,
            PoomsaeService poomsaeService,
            RompimientoService rompimientoService,
            RankingTorneoService rankingService,
            ExportarTorneoService exportarService) {
        this.torneoService = torneoService;
        this.inscripcionService = inscripcionService;
        this.competenciaService = competenciaService;
        this.poomsaeService = poomsaeService;
        this.rompimientoService = rompimientoService;
        this.rankingService = rankingService;
        this.exportarService = exportarService;
    }

    // ----------------------------- LISTA Y NUEVO -----------------------------

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("torneos", torneoService.listar());
        model.addAttribute("seccion", "torneos");
        return "torneos";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        if (!model.containsAttribute("torneoForm")) {
            model.addAttribute("torneoForm", new TorneoForm());
        }
        model.addAttribute("modalidades", ModalidadTorneo.values());
        model.addAttribute("seccion", "torneos");
        return "torneos/nuevo";
    }

    @PostMapping("/nuevo")
    public String crear(@ModelAttribute("torneoForm") TorneoForm form,
            RedirectAttributes redirect) {
        try {
            Torneo torneo = torneoService.crear(form);
            redirect.addFlashAttribute("exito", "Torneo creado correctamente.");
            return "redirect:/torneos/" + torneo.getId();
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            redirect.addFlashAttribute("torneoForm", form);
            return "redirect:/torneos/nuevo";
        }
    }

    // ----------------------------- DETALLE -----------------------------------

    @GetMapping("/{id}")
    public String detalle(@PathVariable Long id, Model model) {
        Torneo torneo = torneoService.obtener(id);
        model.addAttribute("torneo", torneo);
        model.addAttribute("inscritos", inscripcionService.listar(id));
        model.addAttribute("alumnos", inscripcionService.alumnosActivos());
        model.addAttribute("mediosPago", MedioPago.values());
        model.addAttribute("llaves", competenciaService.obtenerLlaves(id));
        model.addAttribute("abierto", torneo.getEstado() == EstadoTorneo.INSCRIPCION_ABIERTA);
        model.addAttribute("esCombate", torneo.getModalidad() == ModalidadTorneo.COMBATE);
        model.addAttribute("esPoomsae", torneo.getModalidad() == ModalidadTorneo.POOMSAE);
        model.addAttribute("esRompimiento", torneo.getModalidad() == ModalidadTorneo.ROMPIMIENTO);

        // Resumen del torneo: totales para mostrar en tarjetas
        List<InscripcionTorneo> inscritos = inscripcionService.listar(id);
        BigDecimal recaudado = BigDecimal.ZERO;
        java.util.Set<String> categorias = new java.util.LinkedHashSet<>();
        int medallas = 0;
        for (InscripcionTorneo i : inscritos) {
            recaudado = recaudado.add(i.getMontoPagado());
            categorias.add(i.getCategoria());
            if (i.getPuesto() != null) {
                medallas++;
            }
        }
        model.addAttribute("totalInscritos", inscritos.size());
        model.addAttribute("totalCategorias", categorias.size());
        model.addAttribute("recaudado", recaudado);
        model.addAttribute("totalMedallas", medallas);
        if (!model.containsAttribute("inscripcionForm")) {
            model.addAttribute("inscripcionForm", new InscripcionTorneoForm());
        }
        model.addAttribute("seccion", "torneos");
        return "torneos/detalle";
    }

    @PostMapping("/{id}/inscribir")
    public String inscribir(@PathVariable Long id,
            @ModelAttribute("inscripcionForm") InscripcionTorneoForm form,
            RedirectAttributes redirect) {
        try {
            InscripcionTorneo i = inscripcionService.inscribir(id, form);
            redirect.addFlashAttribute("exito",
                    "Inscrito: " + i.getNombreCompleto() + " en la categoría " + i.getCategoria() + ".");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            redirect.addFlashAttribute("inscripcionForm", form);
        }
        return "redirect:/torneos/" + id;
    }

    @PostMapping("/{id}/sortear")
    public String sortear(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            redirect.addFlashAttribute("exito", competenciaService.sortear(id));
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/torneos/" + id;
    }

    @PostMapping("/{id}/iniciar-poomsae")
    public String iniciarPoomsae(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            poomsaeService.iniciar(id);
            redirect.addFlashAttribute("exito", "Inscripciones cerradas. Ya puede calificar.");
            return "redirect:/torneos/" + id + "/poomsae";
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/torneos/" + id;
        }
    }

    // ----------------------------- MARCADOR ----------------------------------

    @GetMapping("/combates/{combateId}")
    public String marcador(@PathVariable Long combateId, Model model) {
        MarcadorDTO estado = competenciaService.estado(combateId);
        model.addAttribute("combateId", combateId);
        model.addAttribute("estadoInicial", estado);
        model.addAttribute("torneoId", competenciaService.obtenerCombate(combateId)
                .getTorneo().getId());
        model.addAttribute("seccion", "torneos");
        return "torneos/marcador";
    }

    @GetMapping("/combates/{combateId}/estado")
    @ResponseBody
    public ResponseEntity<?> estado(@PathVariable Long combateId) {
        return responder(() -> competenciaService.estado(combateId));
    }

    @PostMapping("/combates/{combateId}/evento")
    @ResponseBody
    public ResponseEntity<?> evento(@PathVariable Long combateId,
            @RequestParam LadoCombate lado, @RequestParam TipoEventoCombate tipo) {
        return responder(() -> competenciaService.registrarEvento(combateId, lado, tipo));
    }

    @PostMapping("/combates/{combateId}/deshacer")
    @ResponseBody
    public ResponseEntity<?> deshacer(@PathVariable Long combateId) {
        return responder(() -> competenciaService.deshacer(combateId));
    }

    @PostMapping("/combates/{combateId}/round")
    @ResponseBody
    public ResponseEntity<?> cerrarRound(@PathVariable Long combateId) {
        return responder(() -> competenciaService.cerrarRound(combateId));
    }

    @PostMapping("/combates/{combateId}/wo")
    @ResponseBody
    public ResponseEntity<?> walkover(@PathVariable Long combateId,
            @RequestParam LadoCombate ausente) {
        return responder(() -> competenciaService.walkover(combateId, ausente));
    }

    /** Ejecuta la acción y devuelve JSON; si hay error de regla, devuelve 400 con el mensaje. */
    private <T> ResponseEntity<?> responder(java.util.function.Supplier<T> accion) {
        try {
            return ResponseEntity.ok(accion.get());
        } catch (IllegalArgumentException e) {
            Map<String, String> cuerpo = new LinkedHashMap<>();
            cuerpo.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(cuerpo);
        }
    }

    // ----------------------------- POOMSAE -----------------------------------

    @GetMapping("/{id}/poomsae")
    public String poomsae(@PathVariable Long id, Model model) {
        Torneo torneo = torneoService.obtener(id);
        List<InscripcionTorneo> inscritos = inscripcionService.listar(id);

        // notas ya guardadas por competidor: inscripcionId -> lista de 5 totales
        Map<Long, List<com.utp.gajog.model.CalificacionPoomsae>> notas = new LinkedHashMap<>();
        for (InscripcionTorneo i : inscritos) {
            notas.put(i.getId(), poomsaeService.calificaciones(i.getId()));
        }

        // Tablero de clasificación: solo los ya calificados, por categoría y nota
        List<InscripcionTorneo> clasificacion = new ArrayList<>();
        for (InscripcionTorneo i : inscritos) {
            if (i.getNotaFinal() != null) {
                clasificacion.add(i);
            }
        }
        clasificacion.sort(java.util.Comparator
                .comparing(InscripcionTorneo::getCategoria)
                .thenComparing(InscripcionTorneo::getNotaFinal, java.util.Comparator.reverseOrder()));

        model.addAttribute("torneo", torneo);
        model.addAttribute("inscritos", inscritos);
        model.addAttribute("clasificacion", clasificacion);
        model.addAttribute("notas", notas);
        model.addAttribute("jueces", PoomsaeService.JUECES);
        model.addAttribute("enCurso", torneo.getEstado() == EstadoTorneo.EN_CURSO);
        model.addAttribute("seccion", "torneos");
        return "torneos/poomsae";
    }

    @PostMapping("/{id}/poomsae/{inscripcionId}")
    public String calificar(@PathVariable Long id, @PathVariable Long inscripcionId,
            @RequestParam("exactitud") List<BigDecimal> exactitud,
            @RequestParam("presentacion") List<BigDecimal> presentacion,
            RedirectAttributes redirect) {
        try {
            BigDecimal nota = poomsaeService.calificar(id, inscripcionId, exactitud, presentacion);
            redirect.addFlashAttribute("exito", "Nota final registrada: " + nota);
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/torneos/" + id + "/poomsae";
    }

    @PostMapping("/{id}/poomsae/cerrar")
    public String cerrarPoomsae(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            poomsaeService.cerrar(id);
            redirect.addFlashAttribute("exito", "Competencia finalizada. Medallas asignadas.");
            return "redirect:/torneos/" + id + "/resultados";
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/torneos/" + id + "/poomsae";
        }
    }

    // ----------------------------- ROMPIMIENTO -------------------------------

    @PostMapping("/{id}/iniciar-rompimiento")
    public String iniciarRompimiento(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            rompimientoService.iniciar(id);
            redirect.addFlashAttribute("exito", "Inscripciones cerradas. Ya puede registrar intentos.");
            return "redirect:/torneos/" + id + "/rompimiento";
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/torneos/" + id;
        }
    }

    @GetMapping("/{id}/rompimiento")
    public String rompimiento(@PathVariable Long id, Model model) {
        Torneo torneo = torneoService.obtener(id);
        model.addAttribute("torneo", torneo);
        model.addAttribute("tecnicas", TecnicaRompimiento.values());
        model.addAttribute("enCurso", torneo.getEstado() == EstadoTorneo.EN_CURSO);
        model.addAttribute("seccion", "torneos");
        return "torneos/rompimiento";
    }

    @GetMapping("/{id}/rompimiento/estado")
    @ResponseBody
    public ResponseEntity<?> estadoRompimiento(@PathVariable Long id) {
        return responder(() -> rompimientoService.listar(id));
    }

    @PostMapping("/{id}/rompimiento/{inscripcionId}/intento")
    @ResponseBody
    public ResponseEntity<?> intentoRompimiento(@PathVariable Long id, @PathVariable Long inscripcionId,
            @RequestParam TecnicaRompimiento tecnica,
            @RequestParam int intentadas, @RequestParam int rotas) {
        return responder(() -> rompimientoService.registrar(id, inscripcionId, tecnica, intentadas, rotas));
    }

    @PostMapping("/{id}/rompimiento/{inscripcionId}/deshacer")
    @ResponseBody
    public ResponseEntity<?> deshacerRompimiento(@PathVariable Long id, @PathVariable Long inscripcionId) {
        return responder(() -> rompimientoService.deshacer(id, inscripcionId));
    }

    @PostMapping("/{id}/rompimiento/cerrar")
    public String cerrarRompimiento(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            rompimientoService.cerrar(id);
            redirect.addFlashAttribute("exito", "Competencia finalizada. Medallas asignadas.");
            return "redirect:/torneos/" + id + "/resultados";
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/torneos/" + id + "/rompimiento";
        }
    }

    // ----------------------------- RESULTADOS --------------------------------

    @GetMapping("/{id}/resultados")
    public String resultados(@PathVariable Long id, Model model) {
        List<InscripcionTorneo> medallistas = new ArrayList<>();
        for (InscripcionTorneo i : inscripcionService.listar(id)) {
            if (i.getPuesto() != null) {
                medallistas.add(i);
            }
        }
        medallistas.sort(java.util.Comparator
                .comparing(InscripcionTorneo::getCategoria)
                .thenComparing(InscripcionTorneo::getPuesto));
        model.addAttribute("torneo", torneoService.obtener(id));
        model.addAttribute("medallistas", medallistas);
        model.addAttribute("seccion", "torneos");
        return "torneos/resultados";
    }

    @GetMapping("/{id}/resultados.xlsx")
    public ResponseEntity<byte[]> descargarExcel(@PathVariable Long id) {
        byte[] contenido = exportarService.generarExcel(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=resultados-torneo-" + id + ".xlsx")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(contenido);
    }

    @GetMapping("/{id}/constancia/{inscripcionId}")
    public String constancia(@PathVariable Long id, @PathVariable Long inscripcionId, Model model) {
        InscripcionTorneo inscripcion = inscripcionService.listar(id).stream()
                .filter(i -> i.getId().equals(inscripcionId) && i.getPuesto() != null)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No hay constancia para este competidor."));
        model.addAttribute("torneo", torneoService.obtener(id));
        model.addAttribute("inscripcion", inscripcion);
        return "torneos/constancia";
    }

    // ----------------------------- RANKING -----------------------------------

    @GetMapping("/ranking")
    public String ranking(Model model) {
        model.addAttribute("filas", rankingService.ranking());
        model.addAttribute("seccion", "torneos");
        return "torneos/ranking";
    }
}
