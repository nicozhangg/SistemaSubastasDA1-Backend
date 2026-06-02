package com.subastas.repository;

import com.subastas.model.entity.Rematador;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RematadorRepository extends JpaRepository<Rematador, Long> {
}
