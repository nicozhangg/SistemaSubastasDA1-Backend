package com.subastas.repository;

import com.subastas.model.entity.MedioPago;
import com.subastas.model.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MedioPagoRepository extends JpaRepository<MedioPago, Long> {

    List<MedioPago> findByUsuario(Usuario usuario);

    Optional<MedioPago> findByIdAndUsuario(Long id, Usuario usuario);
}
