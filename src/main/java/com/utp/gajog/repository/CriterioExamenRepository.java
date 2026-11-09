package com.utp.gajog.repository;

import com.utp.gajog.model.CriterioExamen;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;


public interface CriterioExamenRepository
        extends JpaRepository<CriterioExamen, Long> {

    List<CriterioExamen> findAllByOrderByIdAsc();
}
