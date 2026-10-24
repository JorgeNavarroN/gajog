package com.utp.gajog.repository;

import com.utp.gajog.model.Tarifa;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TarifaRepository extends JpaRepository<Tarifa, Long> {
    Optional<Tarifa> findFirstByCategoriaIdAndActivoTrueOrderByFechaDesdeDesc(Long categoriaId);
    List<Tarifa> findByActivoTrue();
}