package com.subastas.repository;

import com.subastas.model.entity.Item;
import com.subastas.model.entity.Subasta;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {

    List<Item> findBySubasta(Subasta subasta);

    // Evita N+1 al renderizar el catálogo completo con imágenes y póliza
    @Query("SELECT DISTINCT i FROM Item i LEFT JOIN FETCH i.imagenes LEFT JOIN FETCH i.poliza WHERE i.subasta = :subasta")
    List<Item> findBySubastaWithDetails(@Param("subasta") Subasta subasta);

    @Query("SELECT i FROM Item i LEFT JOIN FETCH i.imagenes LEFT JOIN FETCH i.poliza LEFT JOIN FETCH i.subasta WHERE i.id = :id")
    Optional<Item> findByIdWithDetails(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Item i WHERE i.id = :id")
    Optional<Item> findByIdWithLock(@Param("id") Long id);
}
