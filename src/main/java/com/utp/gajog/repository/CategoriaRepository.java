/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.utp.gajog.repository;

import com.utp.gajog.model.Categoria;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 *
 * @author JORGE
 */
public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    List<Categoria> findByActivoTrue();
}
