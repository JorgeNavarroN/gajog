package com.utp.gajog.service;

import com.utp.gajog.dto.CategoriaLlave;
import com.utp.gajog.dto.MarcadorDTO;
import com.utp.gajog.enums.EstadoCombate;
import com.utp.gajog.enums.EstadoTorneo;
import com.utp.gajog.enums.LadoCombate;
import com.utp.gajog.enums.ModalidadTorneo;
import com.utp.gajog.enums.TipoEventoCombate;
import com.utp.gajog.enums.TipoVictoria;
import com.utp.gajog.model.Combate;
import com.utp.gajog.model.EventoCombate;
import com.utp.gajog.model.InscripcionTorneo;
import com.utp.gajog.model.Torneo;
import com.utp.gajog.repository.CombateRepository;
import com.utp.gajog.repository.EventoCombateRepository;
import com.utp.gajog.repository.InscripcionTorneoRepository;
import com.utp.gajog.repository.TorneoRepository;
import com.utp.gajog.util.ReglasCombate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lógica de los combates: sorteo de la llave, marcador (puntos, faltas,
 * rounds), avance de ganadores y medallas.
 */
@Service
public class CompetenciaService {

    private static final Logger log = LoggerFactory.getLogger(CompetenciaService.class);

    private final TorneoRepository torneoRepository;
    private final InscripcionTorneoRepository inscripcionRepository;
    private final CombateRepository combateRepository;
    private final EventoCombateRepository eventoRepository;

    private final int diferenciaSuperioridad;
    private final int maximoFaltas;

    public CompetenciaService(
            TorneoRepository torneoRepository,
            InscripcionTorneoRepository inscripcionRepository,
            CombateRepository combateRepository,
            EventoCombateRepository eventoRepository,
            @Value("${gajog.torneo.diferencia-superioridad:12}") int diferenciaSuperioridad,
            @Value("${gajog.torneo.maximo-faltas:10}") int maximoFaltas) {
        this.torneoRepository = torneoRepository;
        this.inscripcionRepository = inscripcionRepository;
        this.combateRepository = combateRepository;
        this.eventoRepository = eventoRepository;
        this.diferenciaSuperioridad = diferenciaSuperioridad;
        this.maximoFaltas = maximoFaltas;
    }

    // ------------------------------------------------------------------
    // LLAVE
    // ------------------------------------------------------------------

    /** Llaves del torneo agrupadas por categoría y por ronda (para la pantalla). */
    @Transactional(readOnly = true)
    public List<CategoriaLlave> obtenerLlaves(Long torneoId) {
        List<Combate> combates = combateRepository
                .findByTorneo_IdOrderByCategoriaAscRondaAscPosicionAsc(torneoId);

        Map<String, Map<String, List<Combate>>> agrupado = new LinkedHashMap<>();
        for (Combate c : combates) {
            agrupado.computeIfAbsent(c.getCategoria(), k -> new LinkedHashMap<>())
                    .computeIfAbsent(c.getNombreRonda(), k -> new ArrayList<>())
                    .add(c);
        }

        List<CategoriaLlave> resultado = new ArrayList<>();
        for (Map.Entry<String, Map<String, List<Combate>>> cat : agrupado.entrySet()) {
            List<CategoriaLlave.Ronda> rondas = new ArrayList<>();
            for (Map.Entry<String, List<Combate>> r : cat.getValue().entrySet()) {
                rondas.add(new CategoriaLlave.Ronda(r.getKey(), r.getValue()));
            }
            resultado.add(new CategoriaLlave(cat.getKey(), rondas));
        }
        return resultado;
    }

    /**
     * Cierra las inscripciones, sortea a los competidores de cada categoría
     * y crea todos los combates de la llave (eliminación directa).
     *
     * @return resumen para mostrar al usuario
     */
    @Transactional
    public String sortear(Long torneoId) {
        Torneo torneo = torneoRepository.findById(torneoId)
                .orElseThrow(() -> new IllegalArgumentException("El torneo no existe."));

        if (torneo.getModalidad() != ModalidadTorneo.COMBATE) {
            throw new IllegalArgumentException("El sorteo de llaves es solo para torneos de combate.");
        }
        if (torneo.getEstado() != EstadoTorneo.INSCRIPCION_ABIERTA) {
            throw new IllegalArgumentException("El torneo ya fue sorteado o finalizó.");
        }

        List<InscripcionTorneo> inscritos = inscripcionRepository
                .findByTorneo_IdOrderByCategoriaAscIdAsc(torneoId);

        Map<String, List<InscripcionTorneo>> porCategoria = new LinkedHashMap<>();
        for (InscripcionTorneo i : inscritos) {
            porCategoria.computeIfAbsent(i.getCategoria(), k -> new ArrayList<>()).add(i);
        }

        int categoriasConLlave = 0;
        List<String> omitidas = new ArrayList<>();

        for (Map.Entry<String, List<InscripcionTorneo>> entrada : porCategoria.entrySet()) {
            List<InscripcionTorneo> lista = new ArrayList<>(entrada.getValue());
            if (lista.size() < 2) {
                omitidas.add(entrada.getKey());
                continue;
            }
            Collections.shuffle(lista);
            crearLlave(torneo, entrada.getKey(), lista);
            categoriasConLlave++;
        }

        if (categoriasConLlave == 0) {
            throw new IllegalArgumentException(
                    "Se necesitan al menos 2 competidores en una misma categoría para sortear.");
        }

        torneo.setEstado(EstadoTorneo.EN_CURSO);
        torneoRepository.save(torneo);
        log.info("Sorteo realizado: torneo={}, categorias={}, omitidas={}",
                torneoId, categoriasConLlave, omitidas);

        String resumen = "Sorteo listo: " + categoriasConLlave + " categoría(s) con llave.";
        if (!omitidas.isEmpty()) {
            resumen += " Sin rival (no compiten): " + String.join(", ", omitidas) + ".";
        }
        return resumen;
    }

    /** Crea todos los combates de una categoría y resuelve los pases directos. */
    private void crearLlave(Torneo torneo, String categoria, List<InscripcionTorneo> competidores) {
        int n = competidores.size();
        int tamano = 2;
        while (tamano < n) {
            tamano *= 2;
        }
        int totalRondas = Integer.numberOfTrailingZeros(tamano); // log2

        List<List<Combate>> rondas = new ArrayList<>();
        for (int r = 1; r <= totalRondas; r++) {
            int combatesEnRonda = tamano / (1 << r);
            int participantes = combatesEnRonda * 2;
            List<Combate> lista = new ArrayList<>();
            for (int p = 0; p < combatesEnRonda; p++) {
                Combate c = new Combate();
                c.setTorneo(torneo);
                c.setCategoria(categoria);
                c.setRonda(r);
                c.setNombreRonda(nombreRonda(participantes));
                c.setPosicion(p);
                lista.add(combateRepository.save(c));
            }
            rondas.add(lista);
        }

        // Primera ronda: el azul de cada combate sale de la primera mitad,
        // el rojo de la segunda; así nunca se enfrentan dos "pases directos".
        List<Combate> primera = rondas.get(0);
        int mitad = tamano / 2;
        for (int p = 0; p < primera.size(); p++) {
            Combate c = primera.get(p);
            c.setAzul(competidores.get(p));
            int indiceRojo = mitad + p;
            if (indiceRojo < n) {
                c.setRojo(competidores.get(indiceRojo));
            }
            combateRepository.save(c);
        }

        // Pase directo: si no hay rojo, el azul avanza sin pelear.
        for (Combate c : primera) {
            if (c.getRojo() == null) {
                finalizarCombate(c, c.getAzul(), TipoVictoria.BYE);
            }
        }
    }

    private String nombreRonda(int participantes) {
        return switch (participantes) {
            case 2 -> "Final";
            case 4 -> "Semifinal";
            case 8 -> "Cuartos de final";
            default -> "Ronda de " + participantes;
        };
    }

    // ------------------------------------------------------------------
    // MARCADOR
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Combate obtenerCombate(Long combateId) {
        return combateRepository.findById(combateId)
                .orElseThrow(() -> new IllegalArgumentException("El combate no existe."));
    }

    @Transactional(readOnly = true)
    public MarcadorDTO estado(Long combateId) {
        return construirEstado(obtenerCombate(combateId), null);
    }

    /** Anota un punto o una falta para un lado en el round actual. */
    @Transactional
    public MarcadorDTO registrarEvento(Long combateId, LadoCombate lado, TipoEventoCombate tipo) {
        Combate combate = obtenerCombate(combateId);
        validarEnJuego(combate);

        EventoCombate evento = new EventoCombate();
        evento.setCombate(combate);
        evento.setNumeroRound(combate.getRoundActual());
        evento.setLado(lado);
        evento.setTipo(tipo);
        eventoRepository.save(evento);

        if (combate.getEstado() == EstadoCombate.PENDIENTE) {
            combate.setEstado(EstadoCombate.EN_CURSO);
        }

        String mensaje = null;
        int[] pr = puntosRoundActual(combate);
        int[] faltas = faltasTotales(combate);

        // 1) Descalificación por faltas
        if (ReglasCombate.descalificadoPorFaltas(faltas[0], maximoFaltas)) {
            finalizarCombate(combate, combate.getRojo(), TipoVictoria.FALTAS);
            mensaje = "CHONG descalificado por faltas.";
        } else if (ReglasCombate.descalificadoPorFaltas(faltas[1], maximoFaltas)) {
            finalizarCombate(combate, combate.getAzul(), TipoVictoria.FALTAS);
            mensaje = "HONG descalificado por faltas.";
        }
        // 2) Punto de oro: gana el primero que anota
        else if (combate.getRoundActual() == ReglasCombate.ROUND_DE_ORO && pr[0] != pr[1]) {
            finalizarCombate(combate, pr[0] > pr[1] ? combate.getAzul() : combate.getRojo(),
                    TipoVictoria.PUNTO_DE_ORO);
            mensaje = "Punto de oro.";
        }
        // 3) Superioridad técnica
        else if (ReglasCombate.haySuperioridad(pr[0], pr[1], diferenciaSuperioridad)) {
            finalizarCombate(combate, pr[0] > pr[1] ? combate.getAzul() : combate.getRojo(),
                    TipoVictoria.SUPERIORIDAD);
            mensaje = "Superioridad técnica.";
        } else {
            combateRepository.save(combate);
        }

        log.info("Evento de combate: combate={}, round={}, lado={}, tipo={}",
                combateId, evento.getNumeroRound(), lado, tipo);
        return construirEstado(combate, mensaje);
    }

    /** Anula el último punto o falta del round actual. */
    @Transactional
    public MarcadorDTO deshacer(Long combateId) {
        Combate combate = obtenerCombate(combateId);
        if (combate.getEstado() == EstadoCombate.FINALIZADO) {
            throw new IllegalArgumentException("El combate ya finalizó, no se puede deshacer.");
        }
        List<EventoCombate> eventos = eventoRepository
                .findByCombate_IdAndNumeroRoundAndAnuladoFalseOrderByIdAsc(
                        combateId, combate.getRoundActual());
        if (eventos.isEmpty()) {
            throw new IllegalArgumentException("No hay nada que deshacer en este round.");
        }
        EventoCombate ultimo = eventos.get(eventos.size() - 1);
        ultimo.setAnulado(true);
        eventoRepository.save(ultimo);
        log.info("Evento deshecho: combate={}, evento={}", combateId, ultimo.getId());
        return construirEstado(combate, "Se deshizo la última anotación.");
    }

    /** Termina el round actual y decide si el combate sigue o ya hay ganador. */
    @Transactional
    public MarcadorDTO cerrarRound(Long combateId) {
        Combate combate = obtenerCombate(combateId);
        validarEnJuego(combate);

        if (combate.getRoundActual() == ReglasCombate.ROUND_DE_ORO) {
            throw new IllegalArgumentException(
                    "En el round de oro gana quien anote primero; no se cierra por tiempo.");
        }

        int[] pr = puntosRoundActual(combate);
        int ganadorRound = ReglasCombate.ganadorRound(pr[0], pr[1]);
        if (ganadorRound == 1) {
            combate.setRoundsAzul(combate.getRoundsAzul() + 1);
        } else if (ganadorRound == 2) {
            combate.setRoundsRojo(combate.getRoundsRojo() + 1);
        }

        String mensaje;
        int porRounds = ReglasCombate.ganadorPorRounds(
                combate.getRoundsAzul(), combate.getRoundsRojo());

        if (porRounds != 0) {
            finalizarCombate(combate, porRounds == 1 ? combate.getAzul() : combate.getRojo(),
                    TipoVictoria.PUNTOS);
            mensaje = "Ganó por rounds.";
        } else if (combate.getRoundActual() < ReglasCombate.ROUNDS_TOTALES) {
            combate.setRoundActual(combate.getRoundActual() + 1);
            combateRepository.save(combate);
            mensaje = ganadorRound == 0 ? "Round empatado. Siguiente round."
                    : "Fin del round. Siguiente round.";
        } else if (combate.getRoundsAzul() > combate.getRoundsRojo()) {
            finalizarCombate(combate, combate.getAzul(), TipoVictoria.PUNTOS);
            mensaje = "Ganó por puntos.";
        } else if (combate.getRoundsRojo() > combate.getRoundsAzul()) {
            finalizarCombate(combate, combate.getRojo(), TipoVictoria.PUNTOS);
            mensaje = "Ganó por puntos.";
        } else {
            combate.setRoundActual(ReglasCombate.ROUND_DE_ORO);
            combateRepository.save(combate);
            mensaje = "Empate: round de oro. Gana quien anote primero.";
        }

        log.info("Round cerrado: combate={}, resultadoRound={}, roundsAzul={}, roundsRojo={}",
                combateId, ganadorRound, combate.getRoundsAzul(), combate.getRoundsRojo());
        return construirEstado(combate, mensaje);
    }

    /** El lado indicado no se presentó: gana el rival por W.O. */
    @Transactional
    public MarcadorDTO walkover(Long combateId, LadoCombate ladoAusente) {
        Combate combate = obtenerCombate(combateId);
        validarEnJuego(combate);
        InscripcionTorneo ganador = ladoAusente == LadoCombate.AZUL
                ? combate.getRojo() : combate.getAzul();
        finalizarCombate(combate, ganador, TipoVictoria.WO);
        log.info("W.O.: combate={}, ausente={}", combateId, ladoAusente);
        return construirEstado(combate, "Ganó por W.O.");
    }

    // ------------------------------------------------------------------
    // APOYO
    // ------------------------------------------------------------------

    private void validarEnJuego(Combate combate) {
        if (combate.getEstado() == EstadoCombate.FINALIZADO) {
            throw new IllegalArgumentException("El combate ya finalizó.");
        }
        if (combate.getAzul() == null || combate.getRojo() == null) {
            throw new IllegalArgumentException(
                    "Este combate espera al ganador del combate anterior.");
        }
    }

    /** Puntos del round actual: [azul, rojo]. */
    private int[] puntosRoundActual(Combate combate) {
        List<EventoCombate> eventos = eventoRepository
                .findByCombate_IdAndNumeroRoundAndAnuladoFalseOrderByIdAsc(
                        combate.getId(), combate.getRoundActual());
        int anotadosAzul = 0;
        int anotadosRojo = 0;
        int faltasAzul = 0;
        int faltasRojo = 0;
        for (EventoCombate e : eventos) {
            boolean azul = e.getLado() == LadoCombate.AZUL;
            if (e.getTipo() == TipoEventoCombate.FALTA) {
                if (azul) { faltasAzul++; } else { faltasRojo++; }
            } else if (azul) {
                anotadosAzul += e.getTipo().getPuntos();
            } else {
                anotadosRojo += e.getTipo().getPuntos();
            }
        }
        return new int[]{
            ReglasCombate.puntosRound(anotadosAzul, faltasRojo),
            ReglasCombate.puntosRound(anotadosRojo, faltasAzul)};
    }

    /** Faltas acumuladas en todo el combate: [azul, rojo]. */
    private int[] faltasTotales(Combate combate) {
        int azul = 0;
        int rojo = 0;
        for (EventoCombate e : eventoRepository
                .findByCombate_IdAndAnuladoFalseOrderByIdAsc(combate.getId())) {
            if (e.getTipo() == TipoEventoCombate.FALTA) {
                if (e.getLado() == LadoCombate.AZUL) { azul++; } else { rojo++; }
            }
        }
        return new int[]{azul, rojo};
    }

    /** Marca el combate como terminado y hace avanzar al ganador en la llave. */
    private void finalizarCombate(Combate combate, InscripcionTorneo ganador, TipoVictoria tipo) {
        combate.setGanador(ganador);
        combate.setTipoVictoria(tipo);
        combate.setEstado(EstadoCombate.FINALIZADO);
        combateRepository.save(combate);

        List<Combate> categoria = combateRepository
                .findByTorneo_IdAndCategoriaOrderByRondaAscPosicionAsc(
                        combate.getTorneo().getId(), combate.getCategoria());
        int ultimaRonda = categoria.get(categoria.size() - 1).getRonda();

        if (combate.getRonda() < ultimaRonda) {
            // Avanza: el ganador de la posición p va al combate p/2 de la ronda siguiente.
            for (Combate siguiente : categoria) {
                if (siguiente.getRonda() == combate.getRonda() + 1
                        && siguiente.getPosicion() == combate.getPosicion() / 2) {
                    if (combate.getPosicion() % 2 == 0) {
                        siguiente.setAzul(ganador);
                    } else {
                        siguiente.setRojo(ganador);
                    }
                    combateRepository.save(siguiente);
                    break;
                }
            }
        } else {
            asignarMedallas(categoria, combate, ganador);
            cerrarTorneoSiTermino(combate.getTorneo());
        }
    }

    /** Oro: ganador de la final. Plata: perdedor. Bronce: perdedores de semifinales. */
    private void asignarMedallas(List<Combate> categoria, Combate fin, InscripcionTorneo oro) {
        oro.setPuesto(1);
        inscripcionRepository.save(oro);

        InscripcionTorneo plata = fin.getAzul().getId().equals(oro.getId())
                ? fin.getRojo() : fin.getAzul();
        plata.setPuesto(2);
        inscripcionRepository.save(plata);

        for (Combate c : categoria) {
            boolean esSemifinal = c.getRonda() == fin.getRonda() - 1;
            if (esSemifinal && c.getTipoVictoria() != TipoVictoria.BYE
                    && c.getGanador() != null && c.getAzul() != null && c.getRojo() != null) {
                InscripcionTorneo perdedor = c.getAzul().getId().equals(c.getGanador().getId())
                        ? c.getRojo() : c.getAzul();
                perdedor.setPuesto(3);
                inscripcionRepository.save(perdedor);
            }
        }
    }

    private void cerrarTorneoSiTermino(Torneo torneo) {
        List<Combate> todos = combateRepository
                .findByTorneo_IdOrderByCategoriaAscRondaAscPosicionAsc(torneo.getId());
        boolean pendiente = todos.stream()
                .anyMatch(c -> c.getEstado() != EstadoCombate.FINALIZADO);
        if (!pendiente) {
            torneo.setEstado(EstadoTorneo.FINALIZADO);
            torneoRepository.save(torneo);
            log.info("Torneo finalizado: id={}", torneo.getId());
        }
    }

    private MarcadorDTO construirEstado(Combate c, String mensaje) {
        int[] pr = c.getAzul() != null && c.getRojo() != null && c.getId() != null
                ? puntosRoundActual(c) : new int[]{0, 0};
        int[] faltas = c.getId() != null ? faltasTotales(c) : new int[]{0, 0};

        String nombreAzul = c.getAzul() != null ? c.getAzul().getNombreCompleto() : "Por definir";
        String nombreRojo = c.getRojo() != null ? c.getRojo().getNombreCompleto() : "Por definir";

        return new MarcadorDTO(
                c.getId(),
                c.getCategoria(),
                c.getNombreRonda(),
                c.getEstado().name(),
                nombreAzul,
                nombreRojo,
                c.getAzul() != null && c.getRojo() != null,
                c.getRoundActual(),
                c.getRoundActual() == ReglasCombate.ROUND_DE_ORO,
                pr[0],
                pr[1],
                faltas[0],
                faltas[1],
                c.getRoundsAzul(),
                c.getRoundsRojo(),
                c.getGanador() != null ? c.getGanador().getNombreCompleto() : null,
                c.getTipoVictoria() != null ? c.getTipoVictoria().getEtiqueta() : null,
                mensaje);
    }
}
