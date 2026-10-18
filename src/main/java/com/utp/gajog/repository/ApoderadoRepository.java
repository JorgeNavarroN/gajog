package com.utp.gajog.repository;

import com.utp.gajog.model.Apoderado;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ApoderadoRepository extends JpaRepository<Apoderado, Long> {
    Optional<Apoderado> findByPersonaDni(String dni);
}