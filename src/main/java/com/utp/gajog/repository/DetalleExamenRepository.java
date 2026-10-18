package com.utp.gajog.repository;

import com.utp.gajog.model.DetalleExamen;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DetalleExamenRepository extends JpaRepository<DetalleExamen, Long> {
    List<DetalleExamen> findByExamenId(Long examenId);
}