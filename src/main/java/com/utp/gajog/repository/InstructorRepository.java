/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.utp.gajog.repository;

import com.utp.gajog.model.Instructor;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 *
 * @author JORGE
 */
public interface InstructorRepository extends JpaRepository<Instructor, Long> {

    @EntityGraph(attributePaths = {"persona"})
    List<Instructor> findAllByOrderByPersona_ApellidosAsc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Instructor i WHERE i.id = :id")
    Optional<Instructor> findByIdConBloqueo(@Param("id") Long id);
}
