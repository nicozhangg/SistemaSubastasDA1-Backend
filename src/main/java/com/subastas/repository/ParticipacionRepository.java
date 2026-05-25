package com.subastas.repository;

import com.subastas.model.entity.Participacion;
import com.subastas.model.entity.Subasta;
import com.subastas.model.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParticipacionRepository extends JpaRepository<Participacion, Long> {

    Optional<Participacion> findByUsuarioAndSubasta(Usuario usuario, Subasta subasta);

    Optional<Participacion> findByUsuarioAndConectadoTrue(Usuario usuario);

    List<Participacion> findByUsuarioOrderByFechaConexionDesc(Usuario usuario);

    boolean existsByUsuarioAndConectadoTrue(Usuario usuario);

    List<Participacion> findBySubastaAndConectadoTrue(Subasta subasta);
}
