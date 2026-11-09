package com.utp.gajog.repository;

import com.utp.gajog.model.EventoCombate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventoCombateRepository extends JpaRepository<EventoCombate, Long> {

    List<EventoCombate> findByCombate_IdAndAnuladoFalseOrderByIdAsc(Long combateId);

    List<EventoCombate> findByCombate_IdAndNumeroRoundAndAnuladoFalseOrderByIdAsc(
            Long combateId, Integer numeroRound);
}
