package com.utp.gajog.service;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.utp.gajog.dto.*;
import com.utp.gajog.model.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class ReporteExportService {

    private static final DateTimeFormatter FMT_FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter FMT_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FMT_HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final int MAX_REGISTROS_REPORTE = 5000;

    static {
        // Evita fallos de fuentes gráficas AWT en Java 27 al calcular anchos en Excel/PDF
        System.setProperty("java.awt.headless", "true");
    }

    private final PagoService pagoService;
    private final AsistenciaService asistenciaService;
    private final AlumnoService alumnoService;
    private final ExamenService examenService;
    private final AuditoriaService auditoriaService;
    private final Clock clock;

    public ReporteExportService(
            PagoService pagoService,
            AsistenciaService asistenciaService,
            AlumnoService alumnoService,
            ExamenService examenService,
            AuditoriaService auditoriaService,
            Clock clock
    ) {
        this.pagoService = pagoService;
        this.asistenciaService = asistenciaService;
        this.alumnoService = alumnoService;
        this.examenService = examenService;
        this.auditoriaService = auditoriaService;
        this.clock = clock;
    }

    // ==================== 1. REPORTE DE COBRANZA MENSUAL ====================

    @Transactional
    public byte[] exportarCobranzaExcel(FiltroMensualidadDTO filtro) {
        DatosTabla tabla = construirDatosCobranza(filtro);
        auditoriaService.registrarEvento("EXPORTAR_REPORTE", "Mensualidad", null,
                "Exportación Excel de cobranza mensual (" + filtro.mes() + "/" + filtro.anio() + ")");
        return generarExcel("Cobranza Mensual", tabla);
    }

    @Transactional
    public byte[] exportarCobranzaPdf(FiltroMensualidadDTO filtro) {
        DatosTabla tabla = construirDatosCobranza(filtro);
        auditoriaService.registrarEvento("EXPORTAR_REPORTE", "Mensualidad", null,
                "Exportación PDF de cobranza mensual (" + filtro.mes() + "/" + filtro.anio() + ")");
        return generarPdf(tabla);
    }

    private DatosTabla construirDatosCobranza(FiltroMensualidadDTO filtro) {
        Pageable todo = PageRequest.of(0, MAX_REGISTROS_REPORTE);
        ResumenMensualidadDTO resumen = pagoService.obtenerResumenPeriodo(filtro.anio(), filtro.mes());
        LocalDate hoy = LocalDate.now(clock);

        String subtitulo = String.format(
                "Periodo: %02d/%d | Recaudado: S/ %s | Pagados: %d | Por vencer: %d | Vencidas: %d | Efectividad: %d%%",
                filtro.mes(), filtro.anio(), resumen.totalRecaudado(),
                resumen.cantidadPagados(), resumen.cantidadPorVencer(),
                resumen.cantidadVencidas(), resumen.porcentajeCobranza()
        );

        List<String[]> filas = new ArrayList<>();

        if ("pagados".equalsIgnoreCase(filtro.tabActiva())) {
            List<Pago> pagos = pagoService.buscarPagosRealizados(filtro, todo).getContent();
            List<String> columnas = List.of("Recibo", "Alumno", "DNI", "Periodo", "Fecha Pago", "Medio", "Operación", "Monto (S/)", "Estado");
            for (Pago p : pagos) {
                boolean esMenor = pagoService.esMenorDeEdad(p.getAlumno().getPersona().getFechaNacimiento(), hoy);
                String dni = AlumnoPagoAutocompletadoDTO.enmascararDni(p.getAlumno().getPersona().getDni(), esMenor);
                filas.add(new String[]{
                        pagoService.obtenerNumeroCorrelativoRecibo(p),
                        p.getAlumno().getPersona().getNombres() + " " + p.getAlumno().getPersona().getApellidos(),
                        dni,
                        p.getMensualidad() != null ? (p.getMensualidad().getMes() + "/" + p.getMensualidad().getAnio()) : "-",
                        p.getFechaPago() != null ? p.getFechaPago().format(FMT_FECHA_HORA) : "-",
                        p.getMedioPago().name(),
                        p.getNumeroOperacion() != null ? p.getNumeroOperacion() : "-",
                        p.getMonto().toPlainString(),
                        "Pagado"
                });
            }
            return new DatosTabla("Reporte de Cobranza Mensual — Pagos Registrados", subtitulo, columnas, filas);
        } else {
            List<Mensualidad> cuotas = pagoService.buscarMensualidadesPorCobrar(filtro, todo).getContent();
            List<String> columnas = List.of("Alumno", "DNI", "Periodo", "Fecha Vencimiento", "Monto (S/)", "Estado");
            for (Mensualidad m : cuotas) {
                boolean esMenor = pagoService.esMenorDeEdad(m.getAlumno().getPersona().getFechaNacimiento(), hoy);
                String dni = AlumnoPagoAutocompletadoDTO.enmascararDni(m.getAlumno().getPersona().getDni(), esMenor);
                filas.add(new String[]{
                        m.getAlumno().getPersona().getNombres() + " " + m.getAlumno().getPersona().getApellidos(),
                        dni,
                        m.getMes() + "/" + m.getAnio(),
                        m.getFechaVencimiento() != null ? m.getFechaVencimiento().format(FMT_FECHA) : "-",
                        m.getMonto().toPlainString(),
                        m.getEstado().getEtiqueta()
                });
            }
            return new DatosTabla("Reporte de Cobranza Mensual — Por Vencer y Vencidas", subtitulo, columnas, filas);
        }
    }

    // ==================== 2. REPORTE DE ASISTENCIA POR GRUPO ====================

    @Transactional(readOnly = true)
    public byte[] exportarAsistenciaExcel(FiltroHistorialAsistenciaDTO filtro, Long sesionId) {
        DatosTabla tabla = construirDatosAsistencia(filtro, sesionId);
        return generarExcel("Asistencia", tabla);
    }

    @Transactional(readOnly = true)
    public byte[] exportarAsistenciaPdf(FiltroHistorialAsistenciaDTO filtro, Long sesionId) {
        DatosTabla tabla = construirDatosAsistencia(filtro, sesionId);
        return generarPdf(tabla);
    }

    private DatosTabla construirDatosAsistencia(FiltroHistorialAsistenciaDTO filtro, Long sesionId) {
        List<String> columnas = List.of("Fecha", "Alumno", "DNI", "Categoría / Grupo", "Hora Ingreso", "Min. Tarde", "Método", "Estado", "Observación");
        List<String[]> filas = new ArrayList<>();
        LocalDate hoy = LocalDate.now(clock);

        if (sesionId != null) {
            SesionClase sesion = asistenciaService.obtenerSesionConDetalle(sesionId).orElse(null);
            List<FilaPaseListaDTO> listaGrupo = asistenciaService.construirFilasPaseLista(sesionId);
            String descGrupo = sesion != null
                    ? (sesion.getHorario().getCategoria().getNombre() + " · " + sesion.getHorario().getDiaSemana()
                       + " (" + sesion.getHoraInicio() + " - " + sesion.getHoraFin() + ") · Fecha: " + sesion.getFecha())
                    : ("Sesión ID " + sesionId);

            for (FilaPaseListaDTO f : listaGrupo) {
                filas.add(new String[]{
                        sesion != null ? sesion.getFecha().format(FMT_FECHA) : hoy.format(FMT_FECHA),
                        f.nombreCompleto(),
                        f.dniMostrado(),
                        sesion != null ? (sesion.getHorario().getCategoria().getNombre() + " - " + sesion.getHorario().getSala()) : "-",
                        f.horaIngreso() != null ? f.horaIngreso().format(FMT_HORA) : "-",
                        String.valueOf(f.minutosTarde()),
                        f.metodoRegistro() != null ? f.metodoRegistro().name() : "MANUAL",
                        f.estadoActual().name(),
                        f.observacion() != null ? f.observacion() : "-"
                });
            }
            return new DatosTabla("Reporte de Asistencia por Grupo / Sesión", descGrupo, columnas, filas);
        }

        List<Asistencia> registros = asistenciaService.buscarHistorialPaginado(filtro, 0, MAX_REGISTROS_REPORTE).getContent();
        String subtitulo = "Registros filtrados: " + registros.size()
                + (filtro.fecha() != null ? " | Fecha: " + filtro.fecha().format(FMT_FECHA) : " | Todas las fechas");

        for (Asistencia a : registros) {
            LocalDate fn = a.getAlumno().getPersona().getFechaNacimiento();
            boolean esMenor = fn != null && Period.between(fn, hoy).getYears() < 18;
            String dni = AlumnoPagoAutocompletadoDTO.enmascararDni(a.getAlumno().getPersona().getDni(), esMenor);
            Horario h = a.getSesion().getHorario();

            filas.add(new String[]{
                    a.getSesion().getFecha().format(FMT_FECHA),
                    a.getAlumno().getPersona().getNombres() + " " + a.getAlumno().getPersona().getApellidos(),
                    dni,
                    h.getCategoria().getNombre() + " · " + h.getDiaSemana() + " " + h.getHoraInicio(),
                    a.getHoraIngreso() != null ? a.getHoraIngreso().format(FMT_HORA) : "-",
                    String.valueOf(a.getMinutosTarde() != null ? a.getMinutosTarde() : 0),
                    a.getMetodoRegistro().name(),
                    a.getEstado().name(),
                    a.getObservacion() != null ? a.getObservacion() : "-"
            });
        }
        return new DatosTabla("Reporte Histórico de Asistencias por Grupo", subtitulo, columnas, filas);
    }

    // ==================== 3. REPORTE DE ALUMNOS POR CATEGORÍA ====================

    @Transactional(readOnly = true)
    public byte[] exportarAlumnosExcel(FiltroAlumnoDTO filtro) {
        DatosTabla tabla = construirDatosAlumnos(filtro);
        return generarExcel("Alumnos por Categoría", tabla);
    }

    @Transactional(readOnly = true)
    public byte[] exportarAlumnosPdf(FiltroAlumnoDTO filtro) {
        DatosTabla tabla = construirDatosAlumnos(filtro);
        return generarPdf(tabla);
    }

    private DatosTabla construirDatosAlumnos(FiltroAlumnoDTO filtro) {
        List<AlumnoResumenFilaDTO> alumnos = alumnoService.buscarAlumnosPaginados(filtro, 0, MAX_REGISTROS_REPORTE).getContent();
        List<String> columnas = List.of("Alumno", "DNI", "Edad", "Categoría Actual", "Grupo / Horario", "Cinturón", "Mensualidad", "Estado");
        List<String[]> filas = new ArrayList<>();

        for (AlumnoResumenFilaDTO a : alumnos) {
            filas.add(new String[]{
                    a.nombreCompleto(),
                    a.dniMostrado(),
                    a.edad() + " años",
                    a.categoriaActual(),
                    a.horarioActual(),
                    a.cinturonActual() != null ? a.cinturonActual().getNombre() : "-",
                    a.estadoMensualidadEtiqueta(),
                    a.estado().name()
            });
        }

        String subtitulo = "Total de alumnos listados: " + alumnos.size()
                + (filtro.q() != null && !filtro.q().isBlank() ? " | Búsqueda: \"" + filtro.q() + "\"" : "");
        return new DatosTabla("Padrón de Alumnos por Categoría y Grupo", subtitulo, columnas, filas);
    }

    // ==================== 4. REPORTE DE EXÁMENES DE ASCENSO ====================

    @Transactional(readOnly = true)
    public byte[] exportarExamenesExcel(FiltroExamenDTO filtro) {
        DatosTabla tabla = construirDatosExamenes(filtro);
        return generarExcel("Exámenes de Grado", tabla);
    }

    @Transactional(readOnly = true)
    public byte[] exportarExamenesPdf(FiltroExamenDTO filtro) {
        DatosTabla tabla = construirDatosExamenes(filtro);
        return generarPdf(tabla);
    }

    private DatosTabla construirDatosExamenes(FiltroExamenDTO filtro) {
        List<Examen> examenes = examenService.buscarConFiltros(filtro);
        List<String> columnas = List.of("Postulante", "DNI", "Grado Actual", "Cinturón Objetivo", "Fecha Examen", "Puntaje Total", "Resultado");
        List<String[]> filas = new ArrayList<>();
        LocalDate hoy = LocalDate.now(clock);

        for (Examen ex : examenes) {
            LocalDate fn = ex.getAlumno().getPersona().getFechaNacimiento();
            boolean esMenor = fn != null && Period.between(fn, hoy).getYears() < 18;
            String dni = AlumnoPagoAutocompletadoDTO.enmascararDni(ex.getAlumno().getPersona().getDni(), esMenor);

            filas.add(new String[]{
                    ex.getAlumno().getPersona().getNombres() + " " + ex.getAlumno().getPersona().getApellidos(),
                    dni,
                    ex.getAlumno().getCinturonActual() != null ? ex.getAlumno().getCinturonActual().getNombre() : "-",
                    ex.getCinturonObjetivo().getNombre(),
                    ex.getFechaExamen() != null ? ex.getFechaExamen().format(FMT_FECHA) : "-",
                    ex.getPuntajeTotal() != null ? (ex.getPuntajeTotal().toPlainString() + " / 100") : "-",
                    ex.getResultado().name()
            });
        }

        String subtitulo = "Total de evaluaciones incluidas: " + examenes.size();
        return new DatosTabla("Reporte Oficial de Exámenes de Ascenso de Grado", subtitulo, columnas, filas);
    }

    // ==================== MOTORES DE RENDERIZADO EXCEL Y PDF ====================

    private record DatosTabla(String titulo, String subtitulo, List<String> columnas, List<String[]> filas) {}

    private byte[] generarExcel(String nombreHoja, DatosTabla datos) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(nombreHoja);

            org.apache.poi.ss.usermodel.Font fontTitulo = workbook.createFont();
            fontTitulo.setBold(true);
            fontTitulo.setFontHeightInPoints((short) 13);
            CellStyle styleTitulo = workbook.createCellStyle();
            styleTitulo.setFont(fontTitulo);

            org.apache.poi.ss.usermodel.Font fontHeader = workbook.createFont();
            fontHeader.setBold(true);
            fontHeader.setColor(IndexedColors.WHITE.getIndex());
            CellStyle styleHeader = workbook.createCellStyle();
            styleHeader.setFillForegroundColor(IndexedColors.DARK_RED.getIndex());
            styleHeader.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            styleHeader.setFont(fontHeader);
            styleHeader.setBorderBottom(BorderStyle.THIN);

            CellStyle styleCelda = workbook.createCellStyle();
            styleCelda.setBorderBottom(BorderStyle.HAIR);

            Row rowTitulo = sheet.createRow(0);
            Cell cellTitulo = rowTitulo.createCell(0);
            cellTitulo.setCellValue("GAJOG TKD — " + datos.titulo());
            cellTitulo.setCellStyle(styleTitulo);

            Row rowSub = sheet.createRow(1);
            rowSub.createCell(0).setCellValue(datos.subtitulo() + " | Generado: " + LocalDateTime.now(clock).format(FMT_FECHA_HORA));

            int numCols = datos.columnas().size();
            int[] maxCharsPorColumna = new int[numCols];

            Row rowHeader = sheet.createRow(3);
            for (int i = 0; i < numCols; i++) {
                String colName = datos.columnas().get(i);
                Cell cell = rowHeader.createCell(i);
                cell.setCellValue(colName);
                cell.setCellStyle(styleHeader);
                maxCharsPorColumna[i] = colName.length() + 4;
            }

            int rowIdx = 4;
            for (String[] fila : datos.filas()) {
                Row row = sheet.createRow(rowIdx++);
                for (int c = 0; c < fila.length && c < numCols; c++) {
                    String val = fila[c] != null ? fila[c] : "";
                    Cell cell = row.createCell(c);
                    cell.setCellValue(val);
                    cell.setCellStyle(styleCelda);
                    if (val.length() + 4 > maxCharsPorColumna[c]) {
                        maxCharsPorColumna[c] = Math.min(50, val.length() + 4);
                    }
                }
            }

            // Ancho de columna calculado de forma segura sin depender de fuentes nativas del SO
            for (int i = 0; i < numCols; i++) {
                sheet.setColumnWidth(i, Math.max(14, maxCharsPorColumna[i]) * 256);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Error al generar el archivo Excel", e);
        }
    }

    private byte[] generarPdf(DatosTabla datos) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate(), 28, 28, 30, 30);
            PdfWriter.getInstance(document, out);
            document.open();

            Font fontMarca = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, new Color(220, 38, 38));
            Font fontTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15, new Color(15, 23, 42));
            Font fontSub = FontFactory.getFont(FontFactory.HELVETICA, 9, new Color(100, 116, 139));
            Font fontHeader = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
            Font fontCelda = FontFactory.getFont(FontFactory.HELVETICA, 8.5f, new Color(15, 23, 42));

            Paragraph pMarca = new Paragraph("ACADEMIA GAJOG TKD — SISTEMA DE GESTIÓN INTEGRAL", fontMarca);
            document.add(pMarca);

            Paragraph pTitulo = new Paragraph(datos.titulo(), fontTitulo);
            pTitulo.setSpacingAfter(4f);
            document.add(pTitulo);

            Paragraph pSub = new Paragraph(
                    datos.subtitulo() + "  |  Emitido: " + LocalDateTime.now(clock).format(FMT_FECHA_HORA)
                    + "  |  Por: " + auditoriaService.obtenerUsuarioActual(),
                    fontSub
            );
            pSub.setSpacingAfter(14f);
            document.add(pSub);

            PdfPTable table = new PdfPTable(datos.columnas().size());
            table.setWidthPercentage(100);

            Color bgHeader = new Color(185, 28, 28);
            for (String col : datos.columnas()) {
                PdfPCell headerCell = new PdfPCell(new Phrase(col, fontHeader));
                headerCell.setBackgroundColor(bgHeader);
                headerCell.setPadding(6f);
                headerCell.setBorderColor(new Color(226, 232, 240));
                table.addCell(headerCell);
            }

            boolean par = false;
            Color bgPar = new Color(248, 250, 252);
            for (String[] fila : datos.filas()) {
                for (String valor : fila) {
                    PdfPCell cell = new PdfPCell(new Phrase(valor != null ? valor : "", fontCelda));
                    cell.setPadding(5f);
                    cell.setBorderColor(new Color(226, 232, 240));
                    if (par) {
                        cell.setBackgroundColor(bgPar);
                    }
                    table.addCell(cell);
                }
                par = !par;
            }

            if (datos.filas().isEmpty()) {
                PdfPCell emptyCell = new PdfPCell(new Phrase("No se encontraron registros para los filtros seleccionados.", fontSub));
                emptyCell.setColspan(datos.columnas().size());
                emptyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                emptyCell.setPadding(12f);
                table.addCell(emptyCell);
            }

            document.add(table);
            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Error al generar el documento PDF", e);
        }
    }
}