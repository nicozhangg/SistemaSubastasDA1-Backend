package com.subastas.repository;

import com.subastas.model.entity.Subasta;
import com.subastas.model.enums.Categoria;
import com.subastas.model.enums.EstadoSubasta;
import com.subastas.model.enums.Moneda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SubastaRepository extends JpaRepository<Subasta, Long> {

    List<Subasta> findByEstadoAndFechaFinLessThanEqual(EstadoSubasta estado, LocalDateTime ahora);

    // Fetch join en rematador para evitar N+1 al mapear la lista
    @Query("SELECT s FROM Subasta s LEFT JOIN FETCH s.rematador WHERE " +
           "(:estado IS NULL OR s.estado = :estado) AND " +
           "(:categoria IS NULL OR s.categoria = :categoria) AND " +
           "(:moneda IS NULL OR s.moneda = :moneda) AND " +
           "s.categoria IN :categoriasAccesibles")
    List<Subasta> findAccesibles(
            @Param("estado") EstadoSubasta estado,
            @Param("categoria") Categoria categoria,
            @Param("moneda") Moneda moneda,
            @Param("categoriasAccesibles") List<Categoria> categoriasAccesibles);
}
