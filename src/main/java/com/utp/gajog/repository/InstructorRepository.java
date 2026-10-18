package com.utp.gajog.repository;

import com.utp.gajog.model.Instructor;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface InstructorRepository extends JpaRepository<Instructor, Long> {
    Optional<Instructor> findByPersonaDni(String dni);
}