package com.subastas.incumplimiento;

import com.subastas.BaseIntegrationTest;
import com.subastas.model.entity.*;
import com.subastas.model.enums.*;
import com.subastas.repository.*;
import com.subastas.service.IncumplimientoService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class IncumplimientoServiceTest extends BaseIntegrationTest {

    @Autowired private IncumplimientoService incumplimientoService;
    @Autowired private CompraRepository compraRepository;
    @Autowired private MultaRepository multaRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private ItemRepository itemRepository;

    // Entidades creadas en cada test; el @AfterEach las limpia aunque el test falle
    private Long compraIdCreada;
    private Long multaIdCreada;
    private Usuario usuarioParaRestaurar;

    @AfterEach
    void cleanup() {
        if (compraIdCreada != null) {
            // Eliminar la multa generada como efecto lateral del servicio para esta compra
            compraRepository.findById(compraIdCreada).ifPresent(c -> {
                String clave = "Compra #" + c.getId();
                multaRepository.findByUsuarioOrderByFechaGeneracionDesc(c.getUsuario())
                        .stream()
                        .filter(m -> m.getMotivo().contains(clave))
                        .forEach(multaRepository::delete);
                compraRepository.delete(c);
                // Sincronizar el contador tras borrar multas para no contaminar tests posteriores
                usuarioRepository.findById(c.getUsuario().getId()).ifPresent(u -> {
                    long pendientes = multaRepository.countByUsuarioAndEstado(u, EstadoMulta.PENDIENTE);
                    u.setMultasPendientes((int) pendientes);
                    usuarioRepository.save(u);
                });
            });
            compraIdCreada = null;
        }
        if (multaIdCreada != null) {
            multaRepository.deleteById(multaIdCreada);
            multaIdCreada = null;
        }
        if (usuarioParaRestaurar != null) {
            usuarioParaRestaurar.setEstado(EstadoUsuario.APROBADO);
            usuarioRepository.save(usuarioParaRestaurar);
            usuarioParaRestaurar = null;
        }
    }

    @Test
    void compra_vencida_genera_multa_del_10_porciento() {
        Usuario juan = usuarioRepository.findByEmail("juan@test.com").orElseThrow();
        int multasAntes = juan.getMultasPendientes();
        Item item = itemRepository.findById(1L).orElseThrow();

        Compra compra = compraRepository.save(Compra.builder()
                .item(item)
                .usuario(juan)
                .montoOfertado(new BigDecimal("100000.00"))
                .comisiones(BigDecimal.ZERO)
                .costoEnvio(BigDecimal.ZERO)
                .total(new BigDecimal("100000.00"))
                .moneda(Moneda.ARS)
                .estadoPago(EstadoPago.PENDIENTE)
                .fechaLimitePago(LocalDateTime.now().minusHours(1))
                .build());
        compraIdCreada = compra.getId();

        incumplimientoService.procesarComprasVencidas();

        Compra actualizada = compraRepository.findById(compra.getId()).orElseThrow();
        assertThat(actualizada.getEstadoPago()).isEqualTo(EstadoPago.INCUMPLIDO);

        Usuario juanActualizado = usuarioRepository.findByEmail("juan@test.com").orElseThrow();
        assertThat(juanActualizado.getMultasPendientes()).isGreaterThan(multasAntes);

        // Verificar que la multa es el 10%
        var multas = multaRepository.findByUsuarioOrderByFechaGeneracionDesc(juanActualizado);
        Multa ultimaMulta = multas.get(0);
        assertThat(ultimaMulta.getMonto()).isEqualByComparingTo(new BigDecimal("10000.00"));
        assertThat(ultimaMulta.getEstado()).isEqualTo(EstadoMulta.PENDIENTE);
        assertThat(ultimaMulta.getMotivo()).contains("Incumplimiento");
    }

    @Test
    void compra_no_vencida_no_genera_multa() {
        Usuario maria = usuarioRepository.findByEmail("maria@test.com").orElseThrow();
        Item item = itemRepository.findById(2L).orElseThrow();

        Compra compra = compraRepository.save(Compra.builder()
                .item(item)
                .usuario(maria)
                .montoOfertado(new BigDecimal("50000.00"))
                .comisiones(BigDecimal.ZERO)
                .costoEnvio(BigDecimal.ZERO)
                .total(new BigDecimal("50000.00"))
                .moneda(Moneda.ARS)
                .estadoPago(EstadoPago.PENDIENTE)
                .fechaLimitePago(LocalDateTime.now().plusHours(48))
                .build());
        compraIdCreada = compra.getId();

        incumplimientoService.procesarComprasVencidas();

        Compra noAfectada = compraRepository.findById(compra.getId()).orElseThrow();
        assertThat(noAfectada.getEstadoPago()).isEqualTo(EstadoPago.PENDIENTE);
    }

    @Test
    void multa_vencida_bloquea_usuario_y_deriva_a_justicia() {
        Usuario maria = usuarioRepository.findByEmail("maria@test.com").orElseThrow();
        maria.setEstado(EstadoUsuario.APROBADO);
        usuarioRepository.save(maria);
        usuarioParaRestaurar = maria;

        Multa multa = multaRepository.save(Multa.builder()
                .monto(new BigDecimal("5000.00"))
                .motivo("Multa test para derivar")
                .usuario(maria)
                .fechaLimitePago(LocalDateTime.now().minusHours(1))
                .build());
        multaIdCreada = multa.getId();

        incumplimientoService.derivarMultasVencidas();

        Usuario mariaActualizada = usuarioRepository.findByEmail("maria@test.com").orElseThrow();
        assertThat(mariaActualizada.getEstado()).isEqualTo(EstadoUsuario.BLOQUEADO);

        var multas = multaRepository.findByUsuarioOrderByFechaGeneracionDesc(mariaActualizada);
        boolean hayDerivada = multas.stream()
                .anyMatch(m -> m.getEstado() == EstadoMulta.DERIVADA_JUSTICIA);
        assertThat(hayDerivada).isTrue();
    }

    @Test
    void multa_no_vencida_no_se_deriva() {
        Usuario juan = usuarioRepository.findByEmail("juan@test.com").orElseThrow();

        Multa multa = multaRepository.save(Multa.builder()
                .monto(new BigDecimal("3000.00"))
                .motivo("Multa test NO vencida")
                .usuario(juan)
                .fechaLimitePago(LocalDateTime.now().plusDays(2))
                .build());
        multaIdCreada = multa.getId();

        incumplimientoService.derivarMultasVencidas();

        Multa noAfectada = multaRepository.findById(multa.getId()).orElseThrow();
        assertThat(noAfectada.getEstado()).isEqualTo(EstadoMulta.PENDIENTE);
    }

    // ---- Idempotencia del scheduler (Problema #2 de auditoría) ----
    // FALLA HOY: sin flag de idempotencia, la segunda llamada vuelve a procesar
    // la compra (ya marcada INCUMPLIDO) y genera una multa duplicada.

    @Test
    void procesarComprasVencidas_dos_veces_no_genera_multa_duplicada() {
        Usuario juan = usuarioRepository.findByEmail("juan@test.com").orElseThrow();
        Item item = itemRepository.findById(1L).orElseThrow();

        Compra compra = compraRepository.save(Compra.builder()
                .item(item)
                .usuario(juan)
                .montoOfertado(new BigDecimal("50000.00"))
                .comisiones(BigDecimal.ZERO)
                .costoEnvio(BigDecimal.ZERO)
                .total(new BigDecimal("50000.00"))
                .moneda(Moneda.ARS)
                .estadoPago(EstadoPago.PENDIENTE)
                .fechaLimitePago(LocalDateTime.now().minusHours(1))
                .build());
        compraIdCreada = compra.getId();

        incumplimientoService.procesarComprasVencidas();
        // Segunda llamada simula un scheduler solapado (race condition)
        incumplimientoService.procesarComprasVencidas();

        long multasParaEstaCompra = multaRepository
                .findByUsuarioOrderByFechaGeneracionDesc(juan)
                .stream()
                .filter(m -> m.getMotivo().contains("Compra #" + compra.getId()))
                .count();

        assertThat(multasParaEstaCompra)
                .as("Dos ejecuciones del scheduler no deben generar multas duplicadas")
                .isEqualTo(1L);
    }
}
