package com.subastas.repository;

import com.subastas.model.entity.Compra;
import com.subastas.model.entity.MedioPago;
import com.subastas.model.entity.Usuario;
import com.subastas.model.enums.EstadoPago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CompraRepository extends JpaRepository<Compra, Long> {

    List<Compra> findByUsuarioOrderByIdDesc(Usuario usuario);

    Optional<Compra> findByIdAndUsuario(Long id, Usuario usuario);

    List<Compra> findByEstadoPagoAndFechaLimitePagoLessThanEqual(EstadoPago estadoPago, LocalDateTime ahora);

    long countByUsuario(Usuario usuario);

    @Query("SELECT COALESCE(SUM(c.total), 0) FROM Compra c WHERE c.usuario = :usuario")
    BigDecimal sumTotalByUsuario(@Param("usuario") Usuario usuario);

    @Query("SELECT COALESCE(SUM(c.total), 0) FROM Compra c WHERE c.usuario = :usuario AND c.medioPago = :medioPago AND c.estadoPago = :estadoPago")
    BigDecimal sumTotalByUsuarioAndMedioPagoAndEstadoPago(@Param("usuario") Usuario usuario,
                                                          @Param("medioPago") MedioPago medioPago,
                                                          @Param("estadoPago") EstadoPago estadoPago);

    @Query("SELECT COALESCE(SUM(c.montoOfertado), 0) FROM Compra c WHERE c.usuario = :usuario")
    BigDecimal sumMontoOfertadoByUsuario(@Param("usuario") Usuario usuario);

    @Query("SELECT COALESCE(SUM(c.total), 0) FROM Compra c WHERE c.usuario = :usuario AND c.estadoPago = :estadoPago")
    BigDecimal sumTotalByUsuarioAndEstadoPago(@Param("usuario") Usuario usuario,
                                             @Param("estadoPago") EstadoPago estadoPago);

    @Query("SELECT COUNT(c) FROM Compra c WHERE c.usuario = :usuario AND c.item.subasta.id = :subastaId")
    long countGanadasByUsuarioAndSubastaId(@Param("usuario") Usuario usuario,
                                           @Param("subastaId") Long subastaId);
}
