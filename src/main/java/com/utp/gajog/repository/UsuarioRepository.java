package com.utp.gajog.repository;

import com.utp.gajog.model.RolUsuario;
import com.utp.gajog.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    @Query("SELECT u FROM Usuario u JOIN FETCH u.persona WHERE u.nombreUsuario = :nombreUsuario")
    Optional<Usuario> findByNombreUsuario(@Param("nombreUsuario") String nombreUsuario);

    @Query("SELECT u FROM Usuario u JOIN FETCH u.persona ORDER BY u.id ASC")
    List<Usuario> findAllConPersona();

    boolean existsByNombreUsuario(String nombreUsuario);
    List<Usuario> findByRol(RolUsuario rol);
}