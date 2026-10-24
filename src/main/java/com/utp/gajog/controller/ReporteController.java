package com.utp.gajog.controller;

import com.utp.gajog.dto.*;
import com.utp.gajog.model.EstadoAlumno;
import com.utp.gajog.model.EstadoAsistencia;
import com.utp.gajog.model.EstadoMensualidad;
import com.utp.gajog.model.MedioPago;
import com.utp.gajog.model.ResultadoExamen;
import com.utp.gajog.service.ReporteExportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;

@Controller
@RequestMapping("/reportes")
public class ReporteController {

    private static final MediaType MEDIA_XLSX = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    );

    private final ReporteExportService reporteExportService;
    private final Clock clock;

    public ReporteController(ReporteExportService reporteExportService, Clock clock) {
        this.reporteExportService = reporteExportService;
        this.clock = clock;
    }

    // 1. Reporte de Cobranza Mensual
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCION')")
    @GetMapping("/cobranza/{formato}")
    public ResponseEntity<byte[]> exportarCobranza(
            @PathVariable String formato,
            @RequestParam(required = false) Integer anio,
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) Long horarioId,
            @RequestParam(required = false) EstadoMensualidad estado,
            @RequestParam(required = false) MedioPago medioPago,
            @RequestParam(defaultValue = "por-vencer") String tab
    ) {
        LocalDate hoy = LocalDate.now(clock);
        int anioEf = (anio != null) ? anio : hoy.getYear();
        int mesEf = (mes != null && mes >= 1 && mes <= 12) ? mes : hoy.getMonthValue();

        FiltroMensualidadDTO filtro = new FiltroMensualidadDTO(
                anioEf, mesEf, q, categoriaId, horarioId, estado, medioPago, tab
        );

        boolean esPdf = "pdf".equalsIgnoreCase(formato);
        byte[] contenido = esPdf
                ? reporteExportService.exportarCobranzaPdf(filtro)
                : reporteExportService.exportarCobranzaExcel(filtro);

        String nombreArchivo = String.format("reporte_cobranza_%d_%02d_%s.%s",
                anioEf, mesEf, filtro.tabActiva(), esPdf ? "pdf" : "xlsx");

        return construirRespuestaDescarga(contenido, nombreArchivo, esPdf);
    }

    // 2. Reporte de Asistencia por Grupo / Historial
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCION', 'INSTRUCTOR')")
    @GetMapping("/asistencias/{formato}")
    public ResponseEntity<byte[]> exportarAsistencias(
            @PathVariable String formato,
            @RequestParam(required = false) Long sesionId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) Long horarioId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) EstadoAsistencia estado
    ) {
        FiltroHistorialAsistenciaDTO filtro = new FiltroHistorialAsistenciaDTO(
                fecha, categoriaId, horarioId, q, estado
        );

        boolean esPdf = "pdf".equalsIgnoreCase(formato);
        byte[] contenido = esPdf
                ? reporteExportService.exportarAsistenciaPdf(filtro, sesionId)
                : reporteExportService.exportarAsistenciaExcel(filtro, sesionId);

        String nombreArchivo = "reporte_asistencia_" + LocalDate.now(clock) + (esPdf ? ".pdf" : ".xlsx");
        return construirRespuestaDescarga(contenido, nombreArchivo, esPdf);
    }

    // 3. Reporte de Alumnos por Categoría
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCION', 'INSTRUCTOR')")
    @GetMapping("/alumnos/{formato}")
    public ResponseEntity<byte[]> exportarAlumnos(
            @PathVariable String formato,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) Long horarioId,
            @RequestParam(required = false) EstadoAlumno estado,
            @RequestParam(required = false) Long cinturonId,
            @RequestParam(required = false) EstadoMensualidad estadoMensualidad,
            @RequestParam(defaultValue = "apellidos") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        FiltroAlumnoDTO filtro = new FiltroAlumnoDTO(
                q, categoriaId, horarioId, estado, cinturonId, estadoMensualidad, sortBy, sortDir
        );

        boolean esPdf = "pdf".equalsIgnoreCase(formato);
        byte[] contenido = esPdf
                ? reporteExportService.exportarAlumnosPdf(filtro)
                : reporteExportService.exportarAlumnosExcel(filtro);

        String nombreArchivo = "reporte_alumnos_" + LocalDate.now(clock) + (esPdf ? ".pdf" : ".xlsx");
        return construirRespuestaDescarga(contenido, nombreArchivo, esPdf);
    }

    // 4. Reporte de Exámenes de Ascenso
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'INSTRUCTOR')")
    @GetMapping("/examenes/{formato}")
    public ResponseEntity<byte[]> exportarExamenes(
            @PathVariable String formato,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long cinturonObjetivoId,
            @RequestParam(required = false) ResultadoExamen resultado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta
    ) {
        FiltroExamenDTO filtro = new FiltroExamenDTO(q, cinturonObjetivoId, resultado, desde, hasta);

        boolean esPdf = "pdf".equalsIgnoreCase(formato);
        byte[] contenido = esPdf
                ? reporteExportService.exportarExamenesPdf(filtro)
                : reporteExportService.exportarExamenesExcel(filtro);

        String nombreArchivo = "reporte_examenes_" + LocalDate.now(clock) + (esPdf ? ".pdf" : ".xlsx");
        return construirRespuestaDescarga(contenido, nombreArchivo, esPdf);
    }

    private ResponseEntity<byte[]> construirRespuestaDescarga(byte[] contenido, String nombreArchivo, boolean esPdf) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(esPdf ? MediaType.APPLICATION_PDF : MEDIA_XLSX);
        headers.setContentDisposition(
                ContentDisposition.attachment().filename(nombreArchivo, StandardCharsets.UTF_8).build()
        );
        return ResponseEntity.ok()
                .headers(headers)
                .body(contenido);
    }
}