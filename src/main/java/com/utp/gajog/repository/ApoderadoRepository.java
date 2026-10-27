package com.utp.gajog.repository;


import com.utp.gajog.model.Apoderado;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
/**
 *
 * @author JORGE
 */
public interface ApoderadoRepository extends JpaRepository<Apoderado, Long> {

    Optional<Apoderado> findByPersona_Dni(String dni);
}
