package com.utp.gajog.repository;

import com.utp.gajog.model.DiaSemana;
import com.utp.gajog.model.Horario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface HorarioRepository extends JpaRepository<Horario, Long> {
    List<Horario> findByCategoriaId(Long categoriaId);
    List<Horario> findByDiaSemana(DiaSemana diaSemana);
}