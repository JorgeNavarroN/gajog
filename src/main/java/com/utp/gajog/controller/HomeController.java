package com.utp.gajog.controller;

import com.utp.gajog.model.*;
import com.utp.gajog.repository.*;
import com.utp.gajog.service.MatriculaService;
import com.utp.gajog.service.PagoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Controller
public class HomeController {

    private final AlumnoRepository alumnoRepository;
    private final MatriculaRepository matriculaRepository;
    private final MensualidadRepository mensualidadRepository;
    private final PagoRepository pagoRepository;
    private final SesionClaseRepository sesionClaseRepository;
    private final AsistenciaRepository asistenciaRepository;
    private final ExamenRepository examenRepository;
    private final PagoService pagoService;
    private final MatriculaService matriculaService;
    private final Clock clock;

    public HomeController(
            AlumnoRepository alumnoRepository,
            MatriculaRepository matriculaRepository,
            MensualidadRepository mensualidadRepository,
            PagoRepository pagoRepository,
            SesionClaseRepository sesionClaseRepository,
            AsistenciaRepository asistenciaRepository,
            ExamenRepository examenRepository,
            PagoService pagoService,
            MatriculaService matriculaService,
            Clock clock
    ) {
        this.alumnoRepository = alumnoRepository;
        this.matriculaRepository = matriculaRepository;
        this.mensualidadRepository = mensualidadRepository;
        this.pagoRepository = pagoRepository;
        this.sesionClaseRepository = sesionClaseRepository;
        this.asistenciaRepository = asistenciaRepository;
        this.examenRepository = examenRepository;
        this.pagoService = pagoService;
        this.matriculaService = matriculaService;
        this.clock = clock;
    }

    @GetMapping("/")
    public String inicio(Model model) {
        pagoService.actualizarMensualidadesVencidas();

        LocalDate hoy = LocalDate.now(clock);
        YearMonth mesActual = YearMonth.from(hoy);
        LocalDateTime inicioMes = mesActual.atDay(1).atStartOfDay();
        LocalDateTime finMes = mesActual.plusMonths(1).atDay(1).atStartOfDay();

        long alumnosActivos = alumnoRepository.countByEstado(EstadoAlumno.ACTIVO);
        long porVencer = mensualidadRepository.countByEstado(EstadoMensualidad.POR_VENCER);
        long vencidas = mensualidadRepository.countByEstado(EstadoMensualidad.VENCIDA);
        BigDecimal recaudacionMes = pagoRepository.sumarRecaudacionPeriodo(EstadoPago.REGISTRADO, inicioMes, finMes);

        List<SesionClase> sesionesRecientes = sesionClaseRepository.findTop5RecientesConDetalle();
        Map<Long, Long> presentesPorSesion = new HashMap<>();
        Map<Long, Long> totalPorSesion = new HashMap<>();
        for (SesionClase s : sesionesRecientes) {
            presentesPorSesion.put(s.getId(), asistenciaRepository.countBySesionIdAndEstadoIn(
                    s.getId(), List.of(EstadoAsistencia.PRESENTE, EstadoAsistencia.TARDANZA)
            ));
            totalPorSesion.put(s.getId(), asistenciaRepository.countBySesionId(s.getId()));
        }

        String nombreMes = hoy.getMonth().getDisplayName(TextStyle.FULL, Locale.of("es", "PE"));
        nombreMes = nombreMes.substring(0, 1).toUpperCase() + nombreMes.substring(1);

        model.addAttribute("alumnosActivos", alumnosActivos);
        model.addAttribute("alumnosPorCategoria", matriculaRepository.contarAlumnosActivosPorCategoria(EstadoMatricula.ACTIVA));
        model.addAttribute("mensualidadesPorVencer", porVencer);
        model.addAttribute("mensualidadesVencidas", vencidas);
        model.addAttribute("recaudacionMes", recaudacionMes);
        model.addAttribute("nombreMesActual", nombreMes + " " + hoy.getYear());
        model.addAttribute("sesionesRecientes", sesionesRecientes);
        model.addAttribute("presentesPorSesion", presentesPorSesion);
        model.addAttribute("totalPorSesion", totalPorSesion);
        model.addAttribute("proximosExamenes", examenRepository.findProximosExamenes(ResultadoExamen.PENDIENTE, hoy));
        model.addAttribute("cumpleanieros", alumnoRepository.findCumpleanierosDelMes(EstadoAlumno.ACTIVO, hoy.getMonthValue()));
        model.addAttribute("desfasesEdad", matriculaService.detectarAlumnosConDesfaseDeEdad());

        return "index";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }
}