package com.utp.gajog.dto;

import com.utp.gajog.model.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FilaExamenDTOTest {
    @ParameterizedTest
    @CsvSource({"ana,A", "Luis,L", "' ',?", ",?"})
    void inicialDelAlumnoContemplaNombresVacios(String nombres, String inicial) {
        Persona persona = new Persona(); persona.setNombres(nombres);
        Alumno alumno = new Alumno(); alumno.setPersona(persona);
        assertEquals(inicial, new FilaExamenDTO(alumno, null, false, "Sin examen").getInicial());
    }

    @Test
    void filaConservaAlumnoExamenPagoYEstado() {
        Alumno alumno = new Alumno(); Examen examen = new Examen();
        FilaExamenDTO fila = new FilaExamenDTO(alumno, examen, true, "Pago registrado");
        assertSame(alumno, fila.getAlumno()); assertSame(examen, fila.getExamen());
        assertTrue(fila.isPagado()); assertEquals("Pago registrado", fila.getEstado());
    }
}
