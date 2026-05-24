package com.subastas.repository;

import com.subastas.model.entity.Consignacion;
import com.subastas.model.entity.Usuario;
import com.subastas.model.enums.EstadoConsignacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConsignacionRepository extends JpaRepository<Consignacion, Long> {

    List<Consignacion> findByUsuarioOrderByIdDesc(Usuario usuario);

    Optional<Consignacion> findByIdAndUsuario(Long id, Usuario usuario);
}
