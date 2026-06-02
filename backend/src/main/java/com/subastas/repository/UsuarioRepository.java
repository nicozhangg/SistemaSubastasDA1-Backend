package com.subastas.repository;

import com.subastas.model.entity.Usuario;
import com.subastas.model.enums.EstadoUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByNumeroDni(String numeroDni);

    Optional<Usuario> findByTokenEmail(String tokenEmail);

    long countByEstado(EstadoUsuario estado);

    @Modifying
    @Query("UPDATE Usuario u SET u.multasPendientes = " +
           "(SELECT COUNT(m) FROM Multa m WHERE m.usuario = u AND m.estado = 'PENDIENTE') " +
           "WHERE u.id = :usuarioId")
    void actualizarMultasPendientes(@Param("usuarioId") Long usuarioId);
}
