package com.utp.gajog.repository;

import com.utp.gajog.model.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
    List<Categoria> findByActivoTrue();
    List<Categoria> findByActivoTrueOrderByEdadMinimaAsc();

    @Query("""
        SELECT c FROM Categoria c
        WHERE c.activo = true
          AND :edad >= c.edadMinima
          AND :edad <= c.edadMaxima
        ORDER BY c.edadMinima ASC
    """)
    Optional<Categoria> findActivaPorEdad(@Param("edad") int edad);
}