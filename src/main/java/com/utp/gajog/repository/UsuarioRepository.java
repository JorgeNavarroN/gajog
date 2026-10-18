package com.utp.gajog.repository;

import com.utp.gajog.model.RolUsuario;
import com.utp.gajog.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByNombreUsuario(String nombreUsuario);
    boolean existsByNombreUsuario(String nombreUsuario);
    List<Usuario> findByRol(RolUsuario rol);
}