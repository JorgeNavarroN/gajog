/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.utp.gajog.repository;

import com.utp.gajog.model.Cinturon;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 *
 * @author JORGE
 */
public interface CinturonRepository extends JpaRepository<Cinturon, Long> {

    List<Cinturon> findAllByOrderByOrdenAsc();

    Optional<Cinturon> findByNombreIgnoreCase(String nombre);
}
