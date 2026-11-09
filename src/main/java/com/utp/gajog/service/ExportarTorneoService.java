package com.utp.gajog.service;

import com.utp.gajog.enums.ModalidadTorneo;
import com.utp.gajog.model.Combate;
import com.utp.gajog.model.InscripcionTorneo;
import com.utp.gajog.model.Torneo;
import com.utp.gajog.repository.CombateRepository;
import com.utp.gajog.repository.InscripcionTorneoRepository;
import com.utp.gajog.repository.IntentoRompimientoRepository;
import com.utp.gajog.model.IntentoRompimiento;
import com.utp.gajog.repository.TorneoRepository;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Genera el reporte de resultados del torneo en Excel con Apache POI. */
@Service
public class ExportarTorneoService {

    private static final Logger log = LoggerFactory.getLogger(ExportarTorneoService.class);

    private final TorneoRepository torneoRepository;
    private final InscripcionTorneoRepository inscripcionRepository;
    private final CombateRepository combateRepository;
    private final IntentoRompimientoRepository intentoRepository;

    public ExportarTorneoService(
            TorneoRepository torneoRepository,
            InscripcionTorneoRepository inscripcionRepository,
            CombateRepository combateRepository,
            IntentoRompimientoRepository intentoRepository) {
        this.torneoRepository = torneoRepository;
        this.inscripcionRepository = inscripcionRepository;
        this.combateRepository = combateRepository;
        this.intentoRepository = intentoRepository;
    }

    @Transactional(readOnly = true)
    public byte[] generarExcel(Long torneoId) {
        Torneo torneo = torneoRepository.findById(torneoId)
                .orElseThrow(() -> new IllegalArgumentException("El torneo no existe."));

        List<InscripcionTorneo> inscritos = new ArrayList<>(inscripcionRepository
                .findByTorneo_IdOrderByCategoriaAscIdAsc(torneoId));
        inscritos.sort(Comparator.comparing(InscripcionTorneo::getCategoria)
                .thenComparing(i -> i.getPuesto() == null ? 99 : i.getPuesto()));

        try (Workbook libro = new XSSFWorkbook(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {

            Font negrita = libro.createFont();
            negrita.setBold(true);
            CellStyle estiloCabecera = libro.createCellStyle();
            estiloCabecera.setFont(negrita);

            Sheet hoja = libro.createSheet("Resultados");
            Row titulo = hoja.createRow(0);
            titulo.createCell(0).setCellValue(torneo.getNombre() + " - " + torneo.getFecha()
                    + " - " + torneo.getSede());

            String[] columnas = {"Categoría", "Competidor", "DNI", "Peso (kg)", "Puesto", "Medalla", "Nota / puntaje"};
            Row cabecera = hoja.createRow(2);
            for (int c = 0; c < columnas.length; c++) {
                Cell celda = cabecera.createCell(c);
                celda.setCellValue(columnas[c]);
                celda.setCellStyle(estiloCabecera);
            }

            int fila = 3;
            for (InscripcionTorneo i : inscritos) {
                Row r = hoja.createRow(fila++);
                r.createCell(0).setCellValue(i.getCategoria());
                r.createCell(1).setCellValue(i.getNombreCompleto());
                r.createCell(2).setCellValue(i.getAlumno().getPersona().getDni());
                r.createCell(3).setCellValue(i.getPesoKg().doubleValue());
                if (i.getPuesto() != null) {
                    r.createCell(4).setCellValue(i.getPuesto());
                }
                r.createCell(5).setCellValue(medalla(i.getPuesto()));
                if (i.getNotaFinal() != null) {
                    r.createCell(6).setCellValue(i.getNotaFinal().doubleValue());
                }
            }
            for (int c = 0; c < columnas.length; c++) {
                hoja.autoSizeColumn(c);
            }

            if (torneo.getModalidad() == ModalidadTorneo.COMBATE) {
                Sheet hojaCombates = libro.createSheet("Combates");
                String[] cols = {"Categoría", "Ronda", "CHONG (azul)", "HONG (rojo)", "Ganador", "Tipo de victoria"};
                Row cab = hojaCombates.createRow(0);
                for (int c = 0; c < cols.length; c++) {
                    Cell celda = cab.createCell(c);
                    celda.setCellValue(cols[c]);
                    celda.setCellStyle(estiloCabecera);
                }
                int f = 1;
                for (Combate c : combateRepository
                        .findByTorneo_IdOrderByCategoriaAscRondaAscPosicionAsc(torneoId)) {
                    Row r = hojaCombates.createRow(f++);
                    r.createCell(0).setCellValue(c.getCategoria());
                    r.createCell(1).setCellValue(c.getNombreRonda());
                    r.createCell(2).setCellValue(c.getAzul() != null ? c.getAzul().getNombreCompleto() : "-");
                    r.createCell(3).setCellValue(c.getRojo() != null ? c.getRojo().getNombreCompleto() : "-");
                    r.createCell(4).setCellValue(c.getGanador() != null ? c.getGanador().getNombreCompleto() : "-");
                    r.createCell(5).setCellValue(c.getTipoVictoria() != null ? c.getTipoVictoria().getEtiqueta() : "-");
                }
                for (int c = 0; c < cols.length; c++) {
                    hojaCombates.autoSizeColumn(c);
                }
            }

            if (torneo.getModalidad() == ModalidadTorneo.ROMPIMIENTO) {
                Sheet hojaIntentos = libro.createSheet("Intentos");
                String[] cols = {"Competidor", "Intento", "Técnica", "Tablas intentadas", "Tablas rotas", "Puntos"};
                Row cab = hojaIntentos.createRow(0);
                for (int c = 0; c < cols.length; c++) {
                    Cell celda = cab.createCell(c);
                    celda.setCellValue(cols[c]);
                    celda.setCellStyle(estiloCabecera);
                }
                int f = 1;
                for (IntentoRompimiento t : intentoRepository
                        .findByInscripcion_Torneo_IdOrderByInscripcion_IdAscNumeroAsc(torneoId)) {
                    Row r = hojaIntentos.createRow(f++);
                    r.createCell(0).setCellValue(t.getInscripcion().getNombreCompleto());
                    r.createCell(1).setCellValue(t.getNumero());
                    r.createCell(2).setCellValue(t.getTecnica().getEtiqueta());
                    r.createCell(3).setCellValue(t.getTablasIntentadas());
                    r.createCell(4).setCellValue(t.getTablasRotas());
                    r.createCell(5).setCellValue(t.getPuntos().doubleValue());
                }
                for (int c = 0; c < cols.length; c++) {
                    hojaIntentos.autoSizeColumn(c);
                }
            }

            libro.write(salida);
            log.info("Excel de resultados generado: torneo={}, filas={}", torneoId, inscritos.size());
            return salida.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo generar el archivo Excel.", e);
        }
    }

    private String medalla(Integer puesto) {
        if (puesto == null) {
            return "-";
        }
        return switch (puesto) {
            case 1 -> "Oro";
            case 2 -> "Plata";
            case 3 -> "Bronce";
            default -> "-";
        };
    }
}
