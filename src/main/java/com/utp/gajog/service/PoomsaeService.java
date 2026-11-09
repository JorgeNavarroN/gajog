package com.utp.gajog.service;

import com.utp.gajog.enums.EstadoTorneo;
import com.utp.gajog.enums.ModalidadTorneo;
import com.utp.gajog.model.CalificacionPoomsae;
import com.utp.gajog.model.InscripcionTorneo;
import com.utp.gajog.model.Torneo;
import com.utp.gajog.repository.CalificacionPoomsaeRepository;
import com.utp.gajog.repository.InscripcionTorneoRepository;
import com.utp.gajog.repository.TorneoRepository;
import com.utp.gajog.util.CalculadoraPoomsae;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Calificación de poomsae con 5 jueces y cierre con medallas. */
@Service
public class PoomsaeService {

    public static final int JUECES = 5;

    private static final Logger log = LoggerFactory.getLogger(PoomsaeService.class);

    private final TorneoRepository torneoRepository;
    private final InscripcionTorneoRepository inscripcionRepository;
    private final CalificacionPoomsaeRepository calificacionRepository;

    public PoomsaeService(
            TorneoRepository torneoRepository,
            InscripcionTorneoRepository inscripcionRepository,
            CalificacionPoomsaeRepository calificacionRepository) {
        this.torneoRepository = torneoRepository;
        this.inscripcionRepository = inscripcionRepository;
        this.calificacionRepository = calificacionRepository;
    }

    /** Cierra inscripciones y abre la competencia de poomsae. */
    @Transactional
    public void iniciar(Long torneoId) {
        Torneo torneo = obtenerTorneoPoomsae(torneoId);
        if (torneo.getEstado() != EstadoTorneo.INSCRIPCION_ABIERTA) {
            throw new IllegalArgumentException("La competencia ya fue iniciada.");
        }
        if (inscripcionRepository.findByTorneo_IdOrderByCategoriaAscIdAsc(torneoId).isEmpty()) {
            throw new IllegalArgumentException("No hay competidores inscritos.");
        }
        torneo.setEstado(EstadoTorneo.EN_CURSO);
        torneoRepository.save(torneo);
        log.info("Competencia de poomsae iniciada: torneo={}", torneoId);
    }

    @Transactional(readOnly = true)
    public List<CalificacionPoomsae> calificaciones(Long inscripcionId) {
        return calificacionRepository.findByInscripcion_IdOrderByJuezNumeroAsc(inscripcionId);
    }

    /**
     * Guarda las notas de los 5 jueces para un competidor y calcula la nota final.
     *
     * @param exactitud   lista de 5 notas (0 a 4)
     * @param presentacion lista de 5 notas (0 a 6)
     */
    @Transactional
    public BigDecimal calificar(Long torneoId, Long inscripcionId,
            List<BigDecimal> exactitud, List<BigDecimal> presentacion) {

        Torneo torneo = obtenerTorneoPoomsae(torneoId);
        if (torneo.getEstado() != EstadoTorneo.EN_CURSO) {
            throw new IllegalArgumentException("La competencia no está en curso.");
        }
        InscripcionTorneo inscripcion = inscripcionRepository.findById(inscripcionId)
                .orElseThrow(() -> new IllegalArgumentException("La inscripción no existe."));
        if (!inscripcion.getTorneo().getId().equals(torneoId)) {
            throw new IllegalArgumentException("El competidor no pertenece a este torneo.");
        }
        if (exactitud == null || presentacion == null
                || exactitud.size() != JUECES || presentacion.size() != JUECES) {
            throw new IllegalArgumentException("Se deben ingresar las notas de los " + JUECES + " jueces.");
        }

        List<BigDecimal> totales = new ArrayList<>();
        for (int j = 0; j < JUECES; j++) {
            // totalJuez valida los rangos (exactitud 0-4, presentación 0-6)
            totales.add(CalculadoraPoomsae.totalJuez(exactitud.get(j), presentacion.get(j)));
        }

        for (int j = 0; j < JUECES; j++) {
            CalificacionPoomsae nota = calificacionRepository
                    .findByInscripcion_IdAndJuezNumero(inscripcionId, j + 1)
                    .orElseGet(CalificacionPoomsae::new);
            nota.setInscripcion(inscripcion);
            nota.setJuezNumero(j + 1);
            nota.setExactitud(exactitud.get(j));
            nota.setPresentacion(presentacion.get(j));
            calificacionRepository.save(nota);
        }

        BigDecimal notaFinal = CalculadoraPoomsae.notaFinal(totales);
        inscripcion.setNotaFinal(notaFinal);
        inscripcionRepository.save(inscripcion);
        log.info("Poomsae calificado: torneo={}, inscripcion={}, notaFinal={}",
                torneoId, inscripcionId, notaFinal);
        return notaFinal;
    }

    /** Asigna oro, plata y bronce por categoría y finaliza el torneo. */
    @Transactional
    public void cerrar(Long torneoId) {
        Torneo torneo = obtenerTorneoPoomsae(torneoId);
        if (torneo.getEstado() != EstadoTorneo.EN_CURSO) {
            throw new IllegalArgumentException("La competencia no está en curso.");
        }

        List<InscripcionTorneo> inscritos = inscripcionRepository
                .findByTorneo_IdOrderByCategoriaAscIdAsc(torneoId);

        for (InscripcionTorneo i : inscritos) {
            if (i.getNotaFinal() == null) {
                throw new IllegalArgumentException(
                        "Falta calificar a " + i.getNombreCompleto() + ".");
            }
        }

        Map<String, List<InscripcionTorneo>> porCategoria = new LinkedHashMap<>();
        for (InscripcionTorneo i : inscritos) {
            porCategoria.computeIfAbsent(i.getCategoria(), k -> new ArrayList<>()).add(i);
        }

        for (List<InscripcionTorneo> lista : porCategoria.values()) {
            lista.sort(Comparator.comparing(InscripcionTorneo::getNotaFinal).reversed());
            int puesto = 0;
            BigDecimal anterior = null;
            for (int idx = 0; idx < lista.size(); idx++) {
                InscripcionTorneo i = lista.get(idx);
                // Empates comparten puesto
                if (anterior == null || i.getNotaFinal().compareTo(anterior) != 0) {
                    puesto = idx + 1;
                    anterior = i.getNotaFinal();
                }
                i.setPuesto(puesto <= 3 ? puesto : null);
                inscripcionRepository.save(i);
            }
        }

        torneo.setEstado(EstadoTorneo.FINALIZADO);
        torneoRepository.save(torneo);
        log.info("Poomsae finalizado: torneo={}", torneoId);
    }

    private Torneo obtenerTorneoPoomsae(Long torneoId) {
        Torneo torneo = torneoRepository.findById(torneoId)
                .orElseThrow(() -> new IllegalArgumentException("El torneo no existe."));
        if (torneo.getModalidad() != ModalidadTorneo.POOMSAE) {
            throw new IllegalArgumentException("Este torneo no es de poomsae.");
        }
        return torneo;
    }
}
