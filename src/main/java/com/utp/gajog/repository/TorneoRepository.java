package com.utp.gajog.repository;

import com.utp.gajog.model.Torneo;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TorneoRepository extends JpaRepository<Torneo, Long> {

    List<Torneo> findAllByOrderByFechaDesc();
}
