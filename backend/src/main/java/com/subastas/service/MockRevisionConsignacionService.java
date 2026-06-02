package com.subastas.service;

import com.subastas.model.entity.Consignacion;
import com.subastas.model.enums.EstadoConsignacion;
import com.subastas.repository.ConsignacionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Simula la revisión de consignaciones por la empresa.
 * Se ejecuta de forma asíncrona con un delay de 3 segundos para imitar
 * el proceso de evaluación, igual que el mock de verificación de identidad.
 * Resultado siempre: ACEPTADA, con valorBase = precioSugerido (o 1000 por defecto)
 * y comisiones = valorBase × 10%.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MockRevisionConsignacionService {

    private static final BigDecimal COMISION_PORCENTAJE = new BigDecimal("0.10");
    private static final BigDecimal VALOR_BASE_DEFAULT  = new BigDecimal("1000.00");

    private final ConsignacionRepository consignacionRepository;

    @Async
    public void revisarYAceptar(Long consignacionId) {
        try {
            log.debug("Iniciando revisión mock para consignación {}", consignacionId);
            Thread.sleep(3000);

            Consignacion consignacion = consignacionRepository.findById(consignacionId).orElse(null);
            if (consignacion == null) {
                log.warn("Consignación {} no encontrada tras revisión mock", consignacionId);
                return;
            }

            BigDecimal valorBase = consignacion.getPrecioSugerido() != null
                    ? consignacion.getPrecioSugerido()
                    : VALOR_BASE_DEFAULT;

            BigDecimal comisiones = valorBase.multiply(COMISION_PORCENTAJE)
                    .setScale(2, RoundingMode.HALF_UP);

            consignacion.setValorBase(valorBase);
            consignacion.setComisiones(comisiones);
            consignacion.setEstado(EstadoConsignacion.ACEPTADA);
            consignacionRepository.save(consignacion);

            log.debug("Revisión mock completada para consignación {}. Estado: ACEPTADA, valorBase: {}, comisiones: {}",
                    consignacionId, valorBase, comisiones);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Revisión mock interrumpida para consignación {}", consignacionId);
        } catch (Exception e) {
            log.error("Error en revisión mock de consignación {}: {}", consignacionId, e.getMessage());
        }
    }
}
