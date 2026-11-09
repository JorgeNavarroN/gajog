package com.utp.gajog.service;

import com.utp.gajog.dto.FilaRanking;
import com.utp.gajog.model.InscripcionTorneo;
import com.utp.gajog.repository.InscripcionTorneoRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Ranking de medallas de toda la academia (oro 5, plata 3, bronce 1). */
@Service
public class RankingTorneoService {

    public static final int PUNTOS_ORO = 5;
    public static final int PUNTOS_PLATA = 3;
    public static final int PUNTOS_BRONCE = 1;

    private final InscripcionTorneoRepository inscripcionRepository;

    public RankingTorneoService(InscripcionTorneoRepository inscripcionRepository) {
        this.inscripcionRepository = inscripcionRepository;
    }

    /** Puntos que vale un puesto (1, 2 o 3). */
    public static int puntosPorPuesto(int puesto) {
        return switch (puesto) {
            case 1 -> PUNTOS_ORO;
            case 2 -> PUNTOS_PLATA;
            case 3 -> PUNTOS_BRONCE;
            default -> 0;
        };
    }

    @Transactional(readOnly = true)
    public List<FilaRanking> ranking() {
        // alumno -> [oros, platas, bronces]
        Map<String, int[]> acumulado = new LinkedHashMap<>();
        for (InscripcionTorneo i : inscripcionRepository.findByPuestoIsNotNull()) {
            int[] medallas = acumulado.computeIfAbsent(i.getNombreCompleto(), k -> new int[3]);
            if (i.getPuesto() >= 1 && i.getPuesto() <= 3) {
                medallas[i.getPuesto() - 1]++;
            }
        }

        List<FilaRanking> filas = new ArrayList<>();
        for (Map.Entry<String, int[]> e : acumulado.entrySet()) {
            int[] m = e.getValue();
            int puntos = m[0] * PUNTOS_ORO + m[1] * PUNTOS_PLATA + m[2] * PUNTOS_BRONCE;
            filas.add(new FilaRanking(0, e.getKey(), m[0], m[1], m[2], puntos));
        }
        filas.sort(Comparator.comparingInt(FilaRanking::puntos).reversed()
                .thenComparing(Comparator.comparingInt(FilaRanking::oros).reversed()));

        List<FilaRanking> conPosicion = new ArrayList<>();
        for (int idx = 0; idx < filas.size(); idx++) {
            FilaRanking f = filas.get(idx);
            conPosicion.add(new FilaRanking(
                    idx + 1, f.alumno(), f.oros(), f.platas(), f.bronces(), f.puntos()));
        }
        return conPosicion;
    }
}
