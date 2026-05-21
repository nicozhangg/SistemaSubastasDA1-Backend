package com.subastas.repository;

import com.subastas.model.entity.Item;
import com.subastas.model.entity.Puja;
import com.subastas.model.entity.Subasta;
import com.subastas.model.entity.Usuario;
import com.subastas.model.enums.EstadoPuja;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PujaRepository extends JpaRepository<Puja, Long> {

    // Fetch join en usuario evita N+1 al generar alias en el historial público
    @Query("SELECT p FROM Puja p LEFT JOIN FETCH p.usuario WHERE p.subasta = :subasta ORDER BY p.timestamp DESC")
    List<Puja> findBySubastaOrderByTimestampDesc(@Param("subasta") Subasta subasta);

    @Query("SELECT p FROM Puja p LEFT JOIN FETCH p.usuario WHERE p.subasta = :subasta AND p.item = :item ORDER BY p.timestamp DESC")
    List<Puja> findBySubastaAndItemOrderByTimestampDesc(@Param("subasta") Subasta subasta, @Param("item") Item item);

    List<Puja> findBySubastaAndUsuarioOrderByTimestampDesc(Subasta subasta, Usuario usuario);

    List<Puja> findBySubastaAndItemAndUsuarioOrderByTimestampDesc(Subasta subasta, Item item, Usuario usuario);

    List<Puja> findByUsuarioAndEstado(Usuario usuario, EstadoPuja estado);
}
