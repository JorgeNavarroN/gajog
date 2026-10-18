package com.utp.gajog.repository;

import com.utp.gajog.model.Cinturon;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CinturonRepository extends JpaRepository<Cinturon, Long> {
    List<Cinturon> findAllByOrderByOrdenAsc();
    Optional<Cinturon> findByOrden(Integer orden);
}