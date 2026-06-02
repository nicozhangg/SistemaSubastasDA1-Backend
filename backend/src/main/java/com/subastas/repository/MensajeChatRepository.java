package com.subastas.repository;

import com.subastas.model.entity.Compra;
import com.subastas.model.entity.MensajeChat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MensajeChatRepository extends JpaRepository<MensajeChat, Long> {

    List<MensajeChat> findByCompraOrderByTimestampAsc(Compra compra);

    @Modifying
    @Query("UPDATE MensajeChat m SET m.leido = true WHERE m.compra = :compra AND m.remitente = com.subastas.model.enums.RemitenteMensaje.EMPRESA AND m.leido = false")
    void marcarMensajesEmpresaComoLeidos(@Param("compra") Compra compra);
}
