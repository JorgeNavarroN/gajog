package com.utp.gajog.service;

import com.utp.gajog.model.*;
import com.utp.gajog.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class ExamenService {

    private final ExamenRepository examenRepository;
    private final CriterioExamenRepository criterioExamenRepository;
    private final DetalleExamenRepository detalleExamenRepository;
    private final AlumnoRepository alumnoRepository;
    private final CinturonRepository cinturonRepository;
    private final HistorialCinturonesRepository historialCinturonesRepository;

    public ExamenService(
            ExamenRepository examenRepository,
            CriterioExamenRepository criterioExamenRepository,
            DetalleExamenRepository detalleExamenRepository,
            AlumnoRepository alumnoRepository,
            CinturonRepository cinturonRepository,
            HistorialCinturonesRepository historialCinturonesRepository
    ) {
        this.examenRepository = examenRepository;
        this.criterioExamenRepository = criterioExamenRepository;
        this.detalleExamenRepository = detalleExamenRepository;
        this.alumnoRepository = alumnoRepository;
        this.cinturonRepository = cinturonRepository;
        this.historialCinturonesRepository = historialCinturonesRepository;
    }

    @Transactional
    public Examen programarExamen(Long alumnoId, Long cinturonObjetivoId, LocalDate fechaExamen) {
        Alumno alumno = alumnoRepository.findById(alumnoId)
                .orElseThrow(() -> new IllegalArgumentException("Alumno no encontrado"));

        Cinturon cinturonObjetivo = cinturonRepository.findById(cinturonObjetivoId)
                .orElseThrow(() -> new IllegalArgumentException("Cinturón objetivo no encontrado"));

        if (alumno.getCinturonActual() != null
                && cinturonObjetivo.getOrden() <= alumno.getCinturonActual().getOrden()) {
            throw new IllegalArgumentException("El cinturón objetivo debe ser superior al cinturón actual del alumno");
        }

        Examen examen = new Examen();
        examen.setAlumno(alumno);
        examen.setCinturonObjetivo(cinturonObjetivo);
        examen.setFechaExamen(fechaExamen);
        examen.setResultado(ResultadoExamen.PENDIENTE);
        examen.setPuntajeTotal(BigDecimal.ZERO);

        return examenRepository.save(examen);
    }

    @Transactional
    public DetalleExamen calificarCriterio(Long examenId, Long criterioId, BigDecimal puntaje, String observacion) {
        Examen examen = examenRepository.findById(examenId)
                .orElseThrow(() -> new IllegalArgumentException("Examen no encontrado"));

        if (examen.getResultado() != ResultadoExamen.PENDIENTE) {
            throw new IllegalStateException("El examen ya fue cerrado y no admite más modificaciones");
        }

        CriterioExamen criterio = criterioExamenRepository.findById(criterioId)
                .orElseThrow(() -> new IllegalArgumentException("Criterio de evaluación no encontrado"));

        if (puntaje.compareTo(BigDecimal.ZERO) < 0 || puntaje.compareTo(criterio.getPuntajeMaximo()) > 0) {
            throw new IllegalArgumentException(
                    "El puntaje para " + criterio.getNombre() + " debe estar entre 0 y " + criterio.getPuntajeMaximo()
            );
        }

        DetalleExamen detalle = new DetalleExamen();
        detalle.setExamen(examen);
        detalle.setCriterio(criterio);
        detalle.setPuntaje(puntaje);
        detalle.setObservacion(observacion);

        return detalleExamenRepository.save(detalle);
    }

    @Transactional
    public Examen finalizarEvaluacion(Long examenId, BigDecimal puntajeMinimoAprobatorio) {
        Examen examen = examenRepository.findById(examenId)
                .orElseThrow(() -> new IllegalArgumentException("Examen no encontrado"));

        if (examen.getResultado() != ResultadoExamen.PENDIENTE) {
            throw new IllegalStateException("Este examen ya fue evaluado anteriormente");
        }

        List<DetalleExamen> detalles = detalleExamenRepository.findByExamenId(examenId);
        if (detalles.isEmpty()) {
            throw new IllegalStateException("No se han registrado calificaciones en el detalle del examen");
        }

        BigDecimal sumaTotal = detalles.stream()
        .map(d -> d.getPuntaje() != null ? d.getPuntaje() : BigDecimal.ZERO)
        .reduce(BigDecimal.ZERO, (a, b) -> a.add(b));

        examen.setPuntajeTotal(sumaTotal);

        if (sumaTotal.compareTo(puntajeMinimoAprobatorio) >= 0) {
            examen.setResultado(ResultadoExamen.APROBADO);

            Alumno alumno = examen.getAlumno();
            alumno.setCinturonActual(examen.getCinturonObjetivo());
            alumnoRepository.save(alumno);

            HistorialCinturones historial = new HistorialCinturones();
            historial.setAlumno(alumno);
            historial.setCinturon(examen.getCinturonObjetivo());
            historial.setExamen(examen);
            historial.setFechaObtencion(examen.getFechaExamen());
            historialCinturonesRepository.save(historial);
        } else {
            examen.setResultado(ResultadoExamen.REPROBADO);
        }

        return examenRepository.save(examen);
    }

    @Transactional
    public Examen calificarYFinalizarExamen(
            Long examenId,
            List<Long> criterioIds,
            List<BigDecimal> puntajes,
            List<String> observaciones,
            BigDecimal puntajeMinimo
    ) {
        for (int i = 0; i < criterioIds.size(); i++) {
            String obs = (observaciones != null && i < observaciones.size()) ? observaciones.get(i) : "";
            calificarCriterio(examenId, criterioIds.get(i), puntajes.get(i), obs);
        }
        return finalizarEvaluacion(examenId, puntajeMinimo);
    }

    @Transactional(readOnly = true)
    public List<Examen> listarPorAlumno(Long alumnoId) {
        return examenRepository.findByAlumnoIdOrderByFechaExamenDesc(alumnoId);
    }
}