package com.subastas.service;

import com.subastas.event.PujaConfirmadaEvent;
import com.subastas.exception.BusinessException;
import com.subastas.exception.ErrorCodes;
import com.subastas.exception.ResourceNotFoundException;
import com.subastas.model.dto.request.PujaRequest;
import com.subastas.model.dto.response.PujaResponse;
import com.subastas.model.dto.websocket.BidConfirmedMessage;
import com.subastas.model.dto.websocket.BidUpdatedMessage;
import com.subastas.model.entity.*;
import com.subastas.model.enums.EstadoPuja;
import com.subastas.repository.*;
import com.subastas.util.AliasUtil;
import com.subastas.util.PujaRangeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;

/**
 * Lógica de negocio de pujas. Garantiza consistencia de transacciones concurrentes
 * mediante un lock por subasta, para que dos postores no puedan actualizar
 * la mejor oferta de un mismo ítem al mismo tiempo.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PujaService {

    private final PujaRepository pujaRepository;
    private final SubastaRepository subastaRepository;
    private final ItemRepository itemRepository;
    private final MedioPagoRepository medioPagoRepository;
    private final ParticipacionRepository participacionRepository;
    private final UsuarioService usuarioService;
    private final ApplicationEventPublisher eventPublisher;

    // Un lock por subasta para serializar pujas concurrentes sobre el mismo ítem.
    // expireAfterAccess evita acumulación de locks de subastas ya cerradas.
    private final Cache<Long, ReentrantLock> locksPorSubasta = Caffeine.newBuilder()
            .expireAfterAccess(1, TimeUnit.HOURS)
            .build();

    @Transactional
    public PujaResponse realizarPuja(Long subastaId, String email, PujaRequest request) {
        Usuario usuario = usuarioService.obtenerPorEmail(email);
        Subasta subasta = subastaRepository.findById(subastaId)
                .orElseThrow(() -> new ResourceNotFoundException("Subasta", subastaId));

        participacionRepository
                .findByUsuarioAndSubasta(usuario, subasta)
                .filter(Participacion::isConectado)
                .orElseThrow(() -> new BusinessException(ErrorCodes.USUARIO_NO_CONECTADO,
                        "No estás conectado a esta subasta", HttpStatus.FORBIDDEN));

        if (usuario.getMultasPendientes() > 0) {
            throw new BusinessException(ErrorCodes.MULTA_PENDIENTE,
                    "Tenés multas pendientes. Debés pagarlas antes de pujar", HttpStatus.FORBIDDEN);
        }

        // tryLock sin espera: si el lock está tomado, rechazamos la puja inmediatamente
        // en lugar de encolar, porque el cliente debe esperar la confirmación de la puja en curso
        ReentrantLock lock = locksPorSubasta.get(subastaId, id -> new ReentrantLock());

        if (!lock.tryLock()) {
            throw new BusinessException(ErrorCodes.PUJA_EN_PROCESO,
                    "Hay una puja en proceso", HttpStatus.CONFLICT);
        }

        try {
            return procesarPuja(subastaId, subasta, usuario, request);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Núcleo transaccional de la puja: valida rango, persiste la puja,
     * actualiza la mejor oferta del ítem y dispara los mensajes WebSocket.
     * Solo se ejecuta dentro del lock de la subasta correspondiente.
     */
    private PujaResponse procesarPuja(Long subastaId, Subasta subasta, Usuario usuario, PujaRequest request) {
        Item item = itemRepository.findByIdWithLock(request.getItemId())
                .orElseThrow(() -> new ResourceNotFoundException("Item", request.getItemId()));

        MedioPago medioPago = medioPagoRepository.findByIdAndUsuario(request.getMedioPagoId(), usuario)
                .orElseThrow(() -> new ResourceNotFoundException("Medio de pago", request.getMedioPagoId()));

        if (subasta.getMoneda() != medioPago.getMoneda()) {
            throw new BusinessException(ErrorCodes.ESTADO_INVALIDO,
                    "La moneda del medio de pago no coincide con la moneda de la subasta");
        }

        BigDecimal pujaMinima = PujaRangeUtil.calcularMinima(item, subasta);
        BigDecimal pujaMaxima = PujaRangeUtil.calcularMaxima(item, subasta);

        if (pujaMinima != null && pujaMaxima != null &&
                (request.getMonto().compareTo(pujaMinima) < 0 || request.getMonto().compareTo(pujaMaxima) > 0)) {
            throw new BusinessException(ErrorCodes.MONTO_FUERA_DE_RANGO,
                    String.format("El monto debe estar entre %s y %s", pujaMinima, pujaMaxima));
        }

        Puja puja = Puja.builder()
                .monto(request.getMonto())
                .timestamp(LocalDateTime.now())
                .estado(EstadoPuja.CONFIRMADA)
                .usuario(usuario)
                .item(item)
                .subasta(subasta)
                .medioPago(medioPago)
                .build();

        puja = pujaRepository.save(puja);

        item.setMejorOferta(request.getMonto());
        item.setMejorPostor(usuario);
        itemRepository.save(item);

        BigDecimal nuevaMejorOferta = request.getMonto();
        // Recalcular rango sobre la nueva mejor oferta ya guardada en item
        BigDecimal pujaMinimaBroadcast = PujaRangeUtil.calcularMinima(item, subasta);
        BigDecimal pujaMaximaBroadcast = PujaRangeUtil.calcularMaxima(item, subasta);

        String alias = AliasUtil.generarAlias(usuario);

        // Los mensajes WebSocket se envían solo después del commit para evitar
        // que los clientes vean una puja que luego hace rollback en la BD
        eventPublisher.publishEvent(new PujaConfirmadaEvent(this, subastaId, usuario.getEmail(),
                BidUpdatedMessage.builder()
                        .itemId(item.getId())
                        .nuevaMejorOferta(nuevaMejorOferta)
                        .mejorPostorAlias(alias)
                        .pujaMinima(pujaMinimaBroadcast)
                        .pujaMaxima(pujaMaximaBroadcast)
                        .build(),
                BidConfirmedMessage.builder()
                        .pujaId(puja.getId())
                        .monto(puja.getMonto())
                        .build()));

        return PujaResponse.builder()
                .pujaId(puja.getId())
                .monto(puja.getMonto())
                .estado(puja.getEstado())
                .timestamp(puja.getTimestamp())
                .nuevaMejorOferta(nuevaMejorOferta)
                .postorAlias(alias)
                .build();
    }

    public void liberarLock(Long subastaId) {
        locksPorSubasta.invalidate(subastaId);
    }

    public List<PujaResponse> obtenerHistorial(Long subastaId, Long itemId, Boolean soloPropias, String email,
                                               int page, int size) {
        Subasta subasta = subastaRepository.findById(subastaId)
                .orElseThrow(() -> new ResourceNotFoundException("Subasta", subastaId));

        List<Puja> pujas;
        PageRequest pageable = PageRequest.of(page, size);
        if (Boolean.TRUE.equals(soloPropias)) {
            Usuario usuario = usuarioService.obtenerPorEmail(email);
            if (itemId != null) {
                Item item = itemRepository.findById(itemId)
                        .orElseThrow(() -> new ResourceNotFoundException("Item", itemId));
                pujas = pujaRepository.findBySubastaAndItemAndUsuarioOrderByTimestampDesc(subasta, item, usuario, pageable).getContent();
            } else {
                pujas = pujaRepository.findBySubastaAndUsuarioOrderByTimestampDesc(subasta, usuario, pageable).getContent();
            }
        } else if (itemId != null) {
            Item item = itemRepository.findById(itemId)
                    .orElseThrow(() -> new ResourceNotFoundException("Item", itemId));
            pujas = pujaRepository.findBySubastaAndItemOrderByTimestampDesc(subasta, item, pageable).getContent();
        } else {
            pujas = pujaRepository.findBySubastaOrderByTimestampDesc(subasta, pageable).getContent();
        }

        return pujas.stream()
                .map(p -> PujaResponse.builder()
                        .pujaId(p.getId())
                        .monto(p.getMonto())
                        .postorAlias(AliasUtil.generarAlias(p.getUsuario()))
                        .timestamp(p.getTimestamp())
                        .estado(p.getEstado())
                        .build())
                .collect(Collectors.toList());
    }
}
