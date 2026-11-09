package com.utp.gajog.repository;

import com.utp.gajog.model.Combate;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CombateRepository extends JpaRepository<Combate, Long> {

    @EntityGraph(attributePaths = {"azul.alumno.persona", "rojo.alumno.persona",
        "ganador.alumno.persona"})
    List<Combate> findByTorneo_IdOrderByCategoriaAscRondaAscPosicionAsc(Long torneoId);

    @EntityGraph(attributePaths = {"azul.alumno.persona", "rojo.alumno.persona",
        "ganador.alumno.persona", "torneo"})
    List<Combate> findByTorneo_IdAndCategoriaOrderByRondaAscPosicionAsc(
            Long torneoId, String categoria);

    long countByTorneo_Id(Long torneoId);
}
