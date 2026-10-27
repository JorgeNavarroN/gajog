/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.utp.gajog.repository;

import com.utp.gajog.model.Persona;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 *
 * @author JORGE
 */
public interface PersonaRepository extends JpaRepository<Persona, Long> {

    Optional<Persona> findByDni(String dni);

    boolean existsByDni(String dni);
}
