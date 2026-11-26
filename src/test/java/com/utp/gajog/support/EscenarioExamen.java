package com.utp.gajog.support;

import com.utp.gajog.enums.*;
import com.utp.gajog.model.*;
import com.utp.gajog.service.*;
import java.math.BigDecimal;
import java.time.*;

/** Objetos nuevos por prueba, sin contexto Spring ni conexión de base de datos. */
public class EscenarioExamen {
    public final RepositoriosExamen.Alumnos alumnos = new RepositoriosExamen.Alumnos();
    public final RepositoriosExamen.Cinturones cinturones = new RepositoriosExamen.Cinturones();
    public final RepositoriosExamen.Examenes examenes = new RepositoriosExamen.Examenes();
    public final RepositoriosExamen.Pagos pagos = new RepositoriosExamen.Pagos();
    public final RepositoriosExamen.Tarifas tarifas = new RepositoriosExamen.Tarifas();
    public final RepositoriosExamen.Matriculas matriculas = new RepositoriosExamen.Matriculas();
    public final RepositoriosExamen.Asistencias asistencias = new RepositoriosExamen.Asistencias();
    public final RepositoriosExamen.Criterios criterios = new RepositoriosExamen.Criterios();
    public final RepositoriosExamen.Detalles detalles = new RepositoriosExamen.Detalles();
    public final RepositoriosExamen.Historial historial = new RepositoriosExamen.Historial();
    public final ExamenPagoService pagoService = new ExamenPagoService(
        alumnos, cinturones, examenes, pagos, tarifas, matriculas, asistencias);
    public final ExamenConsultaService consultaService = new ExamenConsultaService(matriculas, examenes, pagoService);
    public final ExamenEvaluacionService evaluacionService = new ExamenEvaluacionService(
        examenes, criterios, detalles, historial, alumnos, pagoService, new BigDecimal("70"));
    public final LocalDate hoy = LocalDate.now(ZoneId.of("America/Lima"));

    public Alumno agregarAlumno(Long id, String nombres, String apellidos) {
        Persona persona = new Persona();
        persona.setNombres(nombres);
        persona.setApellidos(apellidos);
        Alumno alumno = new Alumno();
        alumno.setId(id);
        alumno.setPersona(persona);
        alumno.setEstado(EstadoAlumno.ACTIVO);
        alumnos.agregar(alumno);
        return alumno;
    }

    public void matricular(Alumno alumno) {
        Matricula matricula = new Matricula();
        matricula.setAlumno(alumno);
        matricula.setEstado(EstadoMatricula.ACTIVA);
        matriculas.agregar(matricula);
    }

    public Examen agregarExamen(Long id, Alumno alumno, Cinturon objetivo) {
        Examen examen = new Examen();
        examen.setId(id);
        examen.setAlumno(alumno);
        examen.setCinturonObjetivo(objetivo);
        examen.setResultado(ResultadoExamen.PENDIENTE);
        examenes.agregar(examen);
        return examen;
    }

    public Pago agregarPago(Examen examen) {
        Pago pago = new Pago();
        pago.setAlumno(examen.getAlumno());
        pago.setExamen(examen);
        pago.setEstado(EstadoPago.REGISTRADO);
        pagos.agregar(pago);
        return pago;
    }

    public CriterioExamen agregarCriterio(Long id, String nombre, String maximo) {
        CriterioExamen criterio = new CriterioExamen();
        criterio.setId(id);
        criterio.setNombre(nombre);
        criterio.setPuntajeMaximo(new BigDecimal(maximo));
        criterios.agregar(criterio);
        return criterio;
    }
}
