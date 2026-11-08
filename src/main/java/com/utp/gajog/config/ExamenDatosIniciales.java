package com.utp.gajog.config;

import com.utp.gajog.model.Cinturon;
import com.utp.gajog.model.CriterioExamen;
import com.utp.gajog.repository.CinturonRepository;
import com.utp.gajog.repository.CriterioExamenRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;


@Component
public class ExamenDatosIniciales implements ApplicationRunner {

    private record CriterioBase(String nombre, String maximo) {

    }

    private record CinturonBase(String nombre, String color) {

    }

    private static final List<CriterioBase> CRITERIOS = List.of(
            new CriterioBase("Básicos y técnicas de mano/patada", "25"),
            new CriterioBase("Poomsae", "30"),
            new CriterioBase("Kyorugi", "25"),
            new CriterioBase("Rompimiento y teoría", "20"));

    private static final List<CinturonBase> CINTURONES = List.of(
            new CinturonBase("Blanco", "BLANCO"),
            new CinturonBase("Amarillo", "AMARILLO"),
            new CinturonBase("Verde", "VERDE"),
            new CinturonBase("Azul", "AZUL"),
            new CinturonBase("Rojo", "ROJO"),
            new CinturonBase("Negro", "NEGRO"));

    private final CriterioExamenRepository criterioRepository;
    private final CinturonRepository cinturonRepository;

    public ExamenDatosIniciales(
            CriterioExamenRepository criterioRepository,
            CinturonRepository cinturonRepository) {
        this.criterioRepository = criterioRepository;
        this.cinturonRepository = cinturonRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (criterioRepository.count() == 0) {
            for (CriterioBase base : CRITERIOS) {
                CriterioExamen criterio = new CriterioExamen();
                criterio.setNombre(base.nombre());
                criterio.setPuntajeMaximo(new BigDecimal(base.maximo()));
                criterioRepository.save(criterio);
            }
        }

        List<Cinturon> existentes = cinturonRepository.findAllByOrderByOrdenAsc();
        int siguienteOrden = existentes.stream()
                .map(Cinturon::getOrden)
                .max(Integer::compareTo)
                .orElse(0) + 1;

        for (CinturonBase base : CINTURONES) {
            boolean existe = existentes.stream().anyMatch(c
                    -> c.getNombre().equalsIgnoreCase(base.nombre()));

            if (!existe) {
                Cinturon cinturon = new Cinturon();
                cinturon.setNombre(base.nombre());
                cinturon.setColor(base.color());
                cinturon.setOrden(siguienteOrden++);
                cinturonRepository.save(cinturon);
            }
        }
    }
}
