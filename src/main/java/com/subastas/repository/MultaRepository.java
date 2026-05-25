package com.subastas.repository;

import com.subastas.model.entity.Multa;
import com.subastas.model.entity.Usuario;
import com.subastas.model.enums.EstadoMulta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MultaRepository extends JpaRepository<Multa, Long> {

    List<Multa> findByUsuarioOrderByFechaGeneracionDesc(Usuario usuario);

    Optional<Multa> findByIdAndUsuario(Long id, Usuario usuario);

    long countByUsuarioAndEstado(Usuario usuario, EstadoMulta estado);

    List<Multa> findByEstadoAndFechaLimitePagoLessThanEqual(EstadoMulta estado, LocalDateTime ahora);
}
