package com.utp.gajog.service;

import com.utp.gajog.dto.FiltroAlumnoDTO;
import com.utp.gajog.dto.FiltroExamenDTO;
import com.utp.gajog.dto.FiltroHistorialAsistenciaDTO;
import com.utp.gajog.dto.FiltroMensualidadDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ReporteExportServiceTest {

    @Autowired
    private ReporteExportService reporteExportService;

    @Test
    @DisplayName("Exporta los 4 reportes corporativos en Excel (.xlsx) y PDF (.pdf) con cabeceras binarias válidas")
    void debeGenerarExcelYPdfParaLosCuatroModulos() {
        FiltroMensualidadDTO fCobranza = new FiltroMensualidadDTO(2026, 10, null, null, null, null, null, "por-vencer");
        byte[] excelCobranza = reporteExportService.exportarCobranzaExcel(fCobranza);
        byte[] pdfCobranza = reporteExportService.exportarCobranzaPdf(fCobranza);
        validarBinarioExcel(excelCobranza);
        validarBinarioPdf(pdfCobranza);

        FiltroHistorialAsistenciaDTO fAsis = new FiltroHistorialAsistenciaDTO(null, null, null, null, null);
        byte[] excelAsis = reporteExportService.exportarAsistenciaExcel(fAsis, null);
        byte[] pdfAsis = reporteExportService.exportarAsistenciaPdf(fAsis, null);
        validarBinarioExcel(excelAsis);
        validarBinarioPdf(pdfAsis);

        FiltroAlumnoDTO fAlumnos = new FiltroAlumnoDTO(null, null, null, null, null, null, "apellidos", "asc");
        byte[] excelAlumnos = reporteExportService.exportarAlumnosExcel(fAlumnos);
        byte[] pdfAlumnos = reporteExportService.exportarAlumnosPdf(fAlumnos);
        validarBinarioExcel(excelAlumnos);
        validarBinarioPdf(pdfAlumnos);

        FiltroExamenDTO fExam = new FiltroExamenDTO(null, null, null, null, null);
        byte[] excelExam = reporteExportService.exportarExamenesExcel(fExam);
        byte[] pdfExam = reporteExportService.exportarExamenesPdf(fExam);
        validarBinarioExcel(excelExam);
        validarBinarioPdf(pdfExam);
    }

    private void validarBinarioExcel(byte[] bytes) {
        assertNotNull(bytes);
        assertTrue(bytes.length > 100);
        // Todo archivo .xlsx (OOXML ZIP) comienza con 'PK' (0x50, 0x4B)
        assertEquals('P', (char) bytes[0]);
        assertEquals('K', (char) bytes[1]);
    }

    private void validarBinarioPdf(byte[] bytes) {
        assertNotNull(bytes);
        assertTrue(bytes.length > 100);
        String header = new String(bytes, 0, 5, StandardCharsets.US_ASCII);
        assertEquals("%PDF-", header);
    }
}