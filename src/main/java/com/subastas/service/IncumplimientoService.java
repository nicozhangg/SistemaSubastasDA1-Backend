package com.subastas.service;

import com.subastas.model.entity.Compra;
import com.subastas.model.entity.Multa;
import com.subastas.model.entity.Usuario;
import com.subastas.model.enums.EstadoMulta;
import com.subastas.model.enums.EstadoPago;
import com.subastas.model.enums.EstadoUsuario;
import com.subastas.repository.CompraRepository;
import com.subastas.repository.MultaRepository;
import com.subastas.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Detecta compras impagas cuyo plazo de 72 horas venció, genera multas
 * del 10% del valor ofertado, y bloquea usuarios cuyas multas fueron
 * derivadas a justicia.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IncumplimientoService {

    private static final BigDecimal PORCENTAJE_MULTA = new BigDecimal("0.10");
    private static final int DIAS_LIMITE_MULTA = 3;

    private final CompraRepository compraRepository;
    private final MultaRepository multaRepository;
    private final UsuarioRepository usuarioRepository;

    /**
     * Busca compras con estado PENDIENTE cuyo plazo de pago (72 hs) venció.
     * Para cada una: marca como INCUMPLIDO y genera una multa del 10%.
     */
    @Transactional
    public void procesarComprasVencidas() {
        List<Compra> vencidas = compraRepository
                .findByEstadoPagoAndFechaLimitePagoLessThanEqual(EstadoPago.PENDIENTE, LocalDateTime.now());

        if (vencidas.isEmpty()) return;

        log.info("Procesando {} compra(s) con pago vencido", vencidas.size());

        for (Compra compra : vencidas) {
            // Re-verificar estado para evitar procesar dos veces si el scheduler se solapó
            Compra fresca = compraRepository.findById(compra.getId()).orElse(null);
            if (fresca == null || fresca.getEstadoPago() != EstadoPago.PENDIENTE) {
                continue;
            }
            fresca.setEstadoPago(EstadoPago.INCUMPLIDO);
            compraRepository.save(fresca);
            compra = fresca;

            BigDecimal montoMulta = compra.getMontoOfertado()
                    .multiply(PORCENTAJE_MULTA)
                    .setScale(2, RoundingMode.HALF_UP);

            Multa multa = Multa.builder()
                    .monto(montoMulta)
                    .motivo(String.format("Incumplimiento de pago — Ítem: %s (Compra #%d)",
                            compra.getItem().getDescripcion(), compra.getId()))
                    .usuario(compra.getUsuario())
                    .fechaLimitePago(LocalDateTime.now().plusDays(DIAS_LIMITE_MULTA))
                    .build();
            multaRepository.save(multa);

            // Actualizar cache de multas pendientes del usuario de forma atómica
            Usuario usuario = compra.getUsuario();
            usuarioRepository.actualizarMultasPendientes(usuario.getId());

            log.info("Multa generada: ${} para usuario {} por compra #{}", montoMulta, usuario.getEmail(), compra.getId());
        }
    }

    /**
     * Busca multas PENDIENTES cuyo plazo de pago venció y las deriva a justicia.
     * El usuario queda BLOQUEADO y no puede operar en el sistema.
     */
    @Transactional
    public void derivarMultasVencidas() {
        List<Multa> vencidas = multaRepository.findByEstadoAndFechaLimitePagoLessThanEqual(
                EstadoMulta.PENDIENTE, LocalDateTime.now());

        if (vencidas.isEmpty()) return;

        log.info("Derivando {} multa(s) vencida(s) a justicia", vencidas.size());

        for (Multa multa : vencidas) {
            multa.setEstado(EstadoMulta.DERIVADA_JUSTICIA);
            multaRepository.save(multa);

            Usuario usuario = multa.getUsuario();
            usuario.setEstado(EstadoUsuario.BLOQUEADO);
            usuarioRepository.save(usuario);

            log.warn("Usuario {} bloqueado por multa #{} derivada a justicia", usuario.getEmail(), multa.getId());
        }
    }
}
