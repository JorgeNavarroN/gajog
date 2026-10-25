package com.utp.gajog.repository;

import com.utp.gajog.model.Grupo;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface GrupoRepository extends JpaRepository<Grupo, Long> {
    @Query("SELECT g FROM Grupo g WHERE g.activo = true AND g.categoria.id = :categoriaId ORDER BY g.nombre")
    List<Grupo> findActivosByCategoriaId(@Param("categoriaId") Long categoriaId);

    @Query("SELECT g FROM Grupo g WHERE g.activo = true ORDER BY g.categoria.edadMinima, g.nombre")
    List<Grupo> findActivos();

    @Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)
    @Query("SELECT g FROM Grupo g WHERE g.id = :id")
    Optional<Grupo> findByIdParaMatricula(@Param("id") Long id);
}
