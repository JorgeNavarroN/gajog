package com.utp.gajog.repository;

import com.utp.gajog.model.EstadoPago;
import com.utp.gajog.model.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PagoRepository extends JpaRepository<Pago, Long> {
    List<Pago> findByAlumnoIdOrderByFechaPagoDesc(Long alumnoId);
    List<Pago> findByMensualidadId(Long mensualidadId);
    List<Pago> findByEstado(EstadoPago estado);
}