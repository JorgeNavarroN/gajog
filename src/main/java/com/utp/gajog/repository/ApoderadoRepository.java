package com.utp.gajog.repository;

import com.utp.gajog.model.Apoderado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ApoderadoRepository extends JpaRepository<Apoderado, Long> {

    @Query("SELECT ap FROM Apoderado ap JOIN FETCH ap.persona p WHERE p.dni = :dni")
    Optional<Apoderado> findByPersonaDni(@Param("dni") String dni);

    @Query("SELECT ap FROM Apoderado ap JOIN FETCH ap.persona p ORDER BY p.apellidos ASC")
    List<Apoderado> findAllConPersona();
}