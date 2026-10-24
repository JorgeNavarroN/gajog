package com.utp.gajog.repository;

import com.utp.gajog.model.AuditoriaEvento;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AuditoriaEventoRepository extends JpaRepository<AuditoriaEvento, Long> {
    List<AuditoriaEvento> findTop50ByOrderByFechaHoraDesc();
}