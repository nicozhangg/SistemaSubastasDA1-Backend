package com.subastas.scheduler;

import com.subastas.model.entity.Subasta;
import com.subastas.model.enums.EstadoSubasta;
import com.subastas.repository.SubastaRepository;
import com.subastas.service.IncumplimientoService;
import com.subastas.service.SubastaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubastaScheduler {

    private final SubastaRepository subastaRepository;
    private final SubastaService subastaService;
    private final IncumplimientoService incumplimientoService;

    @Scheduled(fixedDelay = 60_000)
    public void cerrarSubastasVencidas() {
        List<Subasta> vencidas = subastaRepository
                .findByEstadoAndFechaFinLessThanEqual(EstadoSubasta.ABIERTA, LocalDateTime.now());

        if (vencidas.isEmpty()) return;

        log.info("Cerrando {} subasta(s) vencida(s)", vencidas.size());
        for (Subasta subasta : vencidas) {
            try {
                subastaService.cerrarSubasta(subasta);
            } catch (Exception e) {
                log.error("Error al cerrar subasta id={}: {}", subasta.getId(), e.getMessage(), e);
            }
        }
    }

    // Cada 5 minutos: detectar compras impagas cuyo plazo de 72 hs venció → multa del 10%
    @Scheduled(fixedDelay = 300_000)
    public void procesarIncumplimientos() {
        try {
            incumplimientoService.procesarComprasVencidas();
        } catch (Exception e) {
            log.error("Error procesando incumplimientos de pago: {}", e.getMessage(), e);
        }
    }

    // Cada 5 minutos: derivar multas impagas a justicia y bloquear usuarios
    @Scheduled(fixedDelay = 300_000, initialDelay = 150_000)
    public void derivarMultasVencidas() {
        try {
            incumplimientoService.derivarMultasVencidas();
        } catch (Exception e) {
            log.error("Error derivando multas vencidas: {}", e.getMessage(), e);
        }
    }
}
