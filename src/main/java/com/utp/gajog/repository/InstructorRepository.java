package com.utp.gajog.repository;

import com.utp.gajog.model.Instructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InstructorRepository extends JpaRepository<Instructor, Long> {

    @Query("SELECT i FROM Instructor i JOIN FETCH i.persona p WHERE p.dni = :dni")
    Optional<Instructor> findByPersonaDni(@Param("dni") String dni);

    @Query("SELECT i FROM Instructor i JOIN FETCH i.persona p ORDER BY p.apellidos ASC")
    List<Instructor> findAllConPersona();
}