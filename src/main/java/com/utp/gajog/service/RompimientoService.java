package com.utp.gajog.service;

import com.utp.gajog.dto.RompimientoDTO;
import com.utp.gajog.enums.EstadoTorneo;
import com.utp.gajog.enums.ModalidadTorneo;
import com.utp.gajog.enums.TecnicaRompimiento;
import com.utp.gajog.model.IntentoRompimiento;
import com.utp.gajog.model.InscripcionTorneo;
import com.utp.gajog.model.Torneo;
import com.utp.gajog.repository.InscripcionTorneoRepository;
import com.utp.gajog.repository.IntentoRompimientoRepository;
import com.utp.gajog.repository.TorneoRepository;
import com.utp.gajog.util.CalculadoraRompimiento;
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

/** Panel de rompimiento de tablas: intentos, puntaje, ranking y medallas. */
@Service
public class RompimientoService {

    private static final Logger log = LoggerFactory.getLogger(RompimientoService.class);

    private final TorneoRepository torneoRepository;
    private final InscripcionTorneoRepository inscripcionRepository;
    private final IntentoRompimientoRepository intentoRepository;

    public RompimientoService(
            TorneoRepository torneoRepository,
            InscripcionTorneoRepository inscripcionRepository,
            IntentoRompimientoRepository intentoRepository) {
        this.torneoRepository = torneoRepository;
        this.inscripcionRepository = inscripcionRepository;
        this.intentoRepository = intentoRepository;
    }

    /** Cierra inscripciones y abre la competencia. */
    @Transactional
    public void iniciar(Long torneoId) {
        Torneo torneo = obtenerTorneo(torneoId);
        if (torneo.getEstado() != EstadoTorneo.INSCRIPCION_ABIERTA) {
            throw new IllegalArgumentException("La competencia ya fue iniciada.");
        }
        if (inscripcionRepository.findByTorneo_IdOrderByCategoriaAscIdAsc(torneoId).isEmpty()) {
            throw new IllegalArgumentException("No hay competidores inscritos.");
        }
        torneo.setEstado(EstadoTorneo.EN_CURSO);
        torneoRepository.save(torneo);
        log.info("Competencia de rompimiento iniciada: torneo={}", torneoId);
    }

    /** Estado de todos los competidores del torneo, ordenado por puntaje. */
    @Transactional(readOnly = true)
    public List<RompimientoDTO> listar(Long torneoId) {
        obtenerTorneo(torneoId);
        List<InscripcionTorneo> inscritos = inscripcionRepository
                .findByTorneo_IdOrderByCategoriaAscIdAsc(torneoId);

        Map<Long, List<IntentoRompimiento>> porInscripcion = new LinkedHashMap<>();
        for (IntentoRompimiento i : intentoRepository
                .findByInscripcion_Torneo_IdOrderByInscripcion_IdAscNumeroAsc(torneoId)) {
            porInscripcion.computeIfAbsent(i.getInscripcion().getId(), k -> new ArrayList<>()).add(i);
        }

        List<RompimientoDTO> lista = new ArrayList<>();
        for (InscripcionTorneo i : inscritos) {
            lista.add(construir(i, porInscripcion.getOrDefault(i.getId(), new ArrayList<>())));
        }
        lista.sort(Comparator.comparing(RompimientoDTO::categoria)
                .thenComparing(Comparator.comparing(RompimientoDTO::total).reversed()));
        return lista;
    }

    /** Registra un intento del competidor (técnica, tablas intentadas y rotas). */
    @Transactional
    public RompimientoDTO registrar(Long torneoId, Long inscripcionId,
            TecnicaRompimiento tecnica, int tablasIntentadas, int tablasRotas) {

        InscripcionTorneo inscripcion = obtenerInscripcionEnCurso(torneoId, inscripcionId);
        if (tecnica == null) {
            throw new IllegalArgumentException("Seleccione la técnica.");
        }
        List<IntentoRompimiento> previos = intentoRepository
                .findByInscripcion_IdOrderByNumeroAsc(inscripcionId);
        if (previos.size() >= CalculadoraRompimiento.MAX_INTENTOS) {
            throw new IllegalArgumentException("El competidor ya realizó sus "
                    + CalculadoraRompimiento.MAX_INTENTOS + " intentos.");
        }

        BigDecimal puntos = CalculadoraRompimiento.puntosIntento(
                tecnica.getCoeficiente(), tablasIntentadas, tablasRotas);

        IntentoRompimiento intento = new IntentoRompimiento();
        intento.setInscripcion(inscripcion);
        intento.setNumero(previos.size() + 1);
        intento.setTecnica(tecnica);
        intento.setTablasIntentadas(tablasIntentadas);
        intento.setTablasRotas(tablasRotas);
        intento.setPuntos(puntos);
        intentoRepository.save(intento);
        previos.add(intento);

        actualizarTotal(inscripcion, previos);
        log.info("Intento de rompimiento: torneo={}, inscripcion={}, tecnica={}, rotas={}/{}, puntos={}",
                torneoId, inscripcionId, tecnica, tablasRotas, tablasIntentadas, puntos);
        return construir(inscripcion, previos);
    }

    /** Elimina el último intento registrado del competidor. */
    @Transactional
    public RompimientoDTO deshacer(Long torneoId, Long inscripcionId) {
        InscripcionTorneo inscripcion = obtenerInscripcionEnCurso(torneoId, inscripcionId);
        List<IntentoRompimiento> intentos = new ArrayList<>(intentoRepository
                .findByInscripcion_IdOrderByNumeroAsc(inscripcionId));
        if (intentos.isEmpty()) {
            throw new IllegalArgumentException("No hay intentos para deshacer.");
        }
        IntentoRompimiento ultimo = intentos.remove(intentos.size() - 1);
        intentoRepository.delete(ultimo);
        actualizarTotal(inscripcion, intentos);
        log.info("Intento de rompimiento deshecho: torneo={}, inscripcion={}", torneoId, inscripcionId);
        return construir(inscripcion, intentos);
    }

    /** Ordena por categoría, asigna oro/plata/bronce y finaliza el torneo. */
    @Transactional
    public void cerrar(Long torneoId) {
        Torneo torneo = obtenerTorneo(torneoId);
        if (torneo.getEstado() != EstadoTorneo.EN_CURSO) {
            throw new IllegalArgumentException("La competencia no está en curso.");
        }

        Map<Long, List<IntentoRompimiento>> porInscripcion = new LinkedHashMap<>();
        for (IntentoRompimiento i : intentoRepository
                .findByInscripcion_Torneo_IdOrderByInscripcion_IdAscNumeroAsc(torneoId)) {
            porInscripcion.computeIfAbsent(i.getInscripcion().getId(), k -> new ArrayList<>()).add(i);
        }

        List<InscripcionTorneo> inscritos = inscripcionRepository
                .findByTorneo_IdOrderByCategoriaAscIdAsc(torneoId);
        for (InscripcionTorneo i : inscritos) {
            if (!porInscripcion.containsKey(i.getId())) {
                throw new IllegalArgumentException("Falta registrar intentos de " + i.getNombreCompleto() + ".");
            }
        }

        Map<String, List<InscripcionTorneo>> porCategoria = new LinkedHashMap<>();
        for (InscripcionTorneo i : inscritos) {
            porCategoria.computeIfAbsent(i.getCategoria(), k -> new ArrayList<>()).add(i);
        }

        for (List<InscripcionTorneo> lista : porCategoria.values()) {
            // Orden: más puntos primero; si empatan, más tablas rotas
            lista.sort((a, b) -> {
                int c = b.getNotaFinal().compareTo(a.getNotaFinal());
                if (c != 0) {
                    return c;
                }
                return Integer.compare(
                        tablasRotas(porInscripcion.get(b.getId())),
                        tablasRotas(porInscripcion.get(a.getId())));
            });
            int puesto = 0;
            InscripcionTorneo anterior = null;
            for (int idx = 0; idx < lista.size(); idx++) {
                InscripcionTorneo i = lista.get(idx);
                boolean empata = anterior != null
                        && i.getNotaFinal().compareTo(anterior.getNotaFinal()) == 0
                        && tablasRotas(porInscripcion.get(i.getId()))
                        == tablasRotas(porInscripcion.get(anterior.getId()));
                if (!empata) {
                    puesto = idx + 1;
                }
                i.setPuesto(puesto <= 3 ? puesto : null);
                inscripcionRepository.save(i);
                anterior = i;
            }
        }

        torneo.setEstado(EstadoTorneo.FINALIZADO);
        torneoRepository.save(torneo);
        log.info("Rompimiento finalizado: torneo={}", torneoId);
    }

    // ------------------------------------------------------------------

    private void actualizarTotal(InscripcionTorneo inscripcion, List<IntentoRompimiento> intentos) {
        List<BigDecimal> puntos = new ArrayList<>();
        for (IntentoRompimiento i : intentos) {
            puntos.add(i.getPuntos());
        }
        inscripcion.setNotaFinal(intentos.isEmpty() ? null : CalculadoraRompimiento.total(puntos));
        inscripcionRepository.save(inscripcion);
    }

    private int tablasRotas(List<IntentoRompimiento> intentos) {
        int suma = 0;
        for (IntentoRompimiento i : intentos) {
            suma += i.getTablasRotas();
        }
        return suma;
    }

    private RompimientoDTO construir(InscripcionTorneo i, List<IntentoRompimiento> intentos) {
        List<RompimientoDTO.IntentoDTO> filas = new ArrayList<>();
        List<BigDecimal> puntos = new ArrayList<>();
        for (IntentoRompimiento t : intentos) {
            filas.add(new RompimientoDTO.IntentoDTO(
                    t.getNumero(),
                    t.getTecnica().getEtiqueta(),
                    t.getTablasIntentadas(),
                    t.getTablasRotas(),
                    CalculadoraRompimiento.esLimpio(t.getTablasIntentadas(), t.getTablasRotas()),
                    t.getPuntos()));
            puntos.add(t.getPuntos());
        }
        return new RompimientoDTO(
                i.getId(),
                i.getNombreCompleto(),
                i.getCategoria(),
                filas,
                CalculadoraRompimiento.total(puntos),
                tablasRotas(intentos),
                CalculadoraRompimiento.MAX_INTENTOS - intentos.size(),
                i.getPuesto());
    }

    private Torneo obtenerTorneo(Long torneoId) {
        Torneo torneo = torneoRepository.findById(torneoId)
                .orElseThrow(() -> new IllegalArgumentException("El torneo no existe."));
        if (torneo.getModalidad() != ModalidadTorneo.ROMPIMIENTO) {
            throw new IllegalArgumentException("Este torneo no es de rompimiento.");
        }
        return torneo;
    }

    private InscripcionTorneo obtenerInscripcionEnCurso(Long torneoId, Long inscripcionId) {
        Torneo torneo = obtenerTorneo(torneoId);
        if (torneo.getEstado() != EstadoTorneo.EN_CURSO) {
            throw new IllegalArgumentException("La competencia no está en curso.");
        }
        InscripcionTorneo inscripcion = inscripcionRepository.findById(inscripcionId)
                .orElseThrow(() -> new IllegalArgumentException("La inscripción no existe."));
        if (!inscripcion.getTorneo().getId().equals(torneoId)) {
            throw new IllegalArgumentException("El competidor no pertenece a este torneo.");
        }
        return inscripcion;
    }
}
