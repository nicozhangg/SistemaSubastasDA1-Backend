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

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class IncumplimientoServiceTest extends BaseIntegrationTest {

    @Autowired private IncumplimientoService incumplimientoService;
    @Autowired private CompraRepository compraRepository;
    @Autowired private MultaRepository multaRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private ItemRepository itemRepository;

    @Test
    @Order(1)
    void compra_vencida_genera_multa_del_10_porciento() {
        Usuario juan = usuarioRepository.findByEmail("juan@test.com").orElseThrow();
        int multasAntes = juan.getMultasPendientes();
        Item item = itemRepository.findAll().get(0);

        // Crear compra con fecha límite ya vencida
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
    @Order(2)
    void compra_no_vencida_no_genera_multa() {
        Usuario maria = usuarioRepository.findByEmail("maria@test.com").orElseThrow();
        Item item = itemRepository.findAll().get(1);

        // Compra con plazo aún vigente
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

        incumplimientoService.procesarComprasVencidas();

        Compra noAfectada = compraRepository.findById(compra.getId()).orElseThrow();
        assertThat(noAfectada.getEstadoPago()).isEqualTo(EstadoPago.PENDIENTE);
    }

    @Test
    @Order(3)
    void multa_vencida_bloquea_usuario_y_deriva_a_justicia() {
        Usuario maria = usuarioRepository.findByEmail("maria@test.com").orElseThrow();
        maria.setEstado(EstadoUsuario.APROBADO);
        usuarioRepository.save(maria);

        // Crear multa con fecha límite ya vencida
        multaRepository.save(Multa.builder()
                .monto(new BigDecimal("5000.00"))
                .motivo("Multa test para derivar")
                .usuario(maria)
                .fechaLimitePago(LocalDateTime.now().minusHours(1))
                .build());

        incumplimientoService.derivarMultasVencidas();

        Usuario mariaActualizada = usuarioRepository.findByEmail("maria@test.com").orElseThrow();
        assertThat(mariaActualizada.getEstado()).isEqualTo(EstadoUsuario.BLOQUEADO);

        var multas = multaRepository.findByUsuarioOrderByFechaGeneracionDesc(mariaActualizada);
        boolean hayDerivada = multas.stream()
                .anyMatch(m -> m.getEstado() == EstadoMulta.DERIVADA_JUSTICIA);
        assertThat(hayDerivada).isTrue();

        // Restaurar estado para no romper otros tests
        mariaActualizada.setEstado(EstadoUsuario.APROBADO);
        usuarioRepository.save(mariaActualizada);
    }

    @Test
    @Order(4)
    void multa_no_vencida_no_se_deriva() {
        Usuario juan = usuarioRepository.findByEmail("juan@test.com").orElseThrow();

        Multa multa = multaRepository.save(Multa.builder()
                .monto(new BigDecimal("3000.00"))
                .motivo("Multa test NO vencida")
                .usuario(juan)
                .fechaLimitePago(LocalDateTime.now().plusDays(2))
                .build());

        incumplimientoService.derivarMultasVencidas();

        Multa noAfectada = multaRepository.findById(multa.getId()).orElseThrow();
        assertThat(noAfectada.getEstado()).isEqualTo(EstadoMulta.PENDIENTE);
    }
}
