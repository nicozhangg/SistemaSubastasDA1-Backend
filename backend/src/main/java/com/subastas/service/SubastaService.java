package com.subastas.service;

import com.subastas.exception.BusinessException;
import com.subastas.exception.ErrorCodes;
import com.subastas.exception.ResourceNotFoundException;
import com.subastas.model.dto.request.ConectarSubastaRequest;
import com.subastas.model.dto.response.ConectarSubastaResponse;
import com.subastas.model.dto.response.EstadoPujaResponse;
import com.subastas.model.dto.response.SubastaResponse;
import com.subastas.model.dto.websocket.AuctionClosedMessage;
import com.subastas.model.entity.*;
import com.subastas.repository.MensajeChatRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import com.subastas.model.enums.EstadoPago;
import com.subastas.model.enums.RemitenteMensaje;
import com.subastas.util.AliasUtil;
import com.subastas.util.PujaRangeUtil;
import com.subastas.model.enums.Categoria;
import com.subastas.model.enums.EstadoItem;
import com.subastas.model.enums.EstadoSubasta;
import com.subastas.model.enums.Moneda;
import com.subastas.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Lógica de negocio de subastas: listado con control de acceso por categoría,
 * conexión/desconexión de postores y cálculo del estado de puja del ítem activo.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubastaService {

    private static final BigDecimal PORCENTAJE_COMISION = new BigDecimal("10");
    private static final BigDecimal COSTO_ENVIO = new BigDecimal("1500");

    private final SubastaRepository subastaRepository;
    private final ItemRepository itemRepository;
    private final MedioPagoRepository medioPagoRepository;
    private final ParticipacionRepository participacionRepository;
    private final CompraRepository compraRepository;
    private final MensajeChatRepository mensajeChatRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final PujaService pujaService;
    private final EmailService emailService;
    private final UsuarioService usuarioService;

    /**
     * Filtra las subastas que el usuario puede ver según su categoría.
     * Un usuario PLATA ve subastas COMUN, ESPECIAL y PLATA, pero no ORO ni PLATINO.
     */
    @Transactional(readOnly = true)
    public List<SubastaResponse> listar(EstadoSubasta estado, Categoria categoria,
                                        Moneda moneda, String email) {
        Usuario usuario = usuarioService.obtenerPorEmail(email);

        List<Categoria> categoriasAccesibles = Arrays.stream(Categoria.values())
                .filter(c -> c.ordinal() <= usuario.getCategoria().ordinal())
                .collect(Collectors.toList());

        return subastaRepository
                .findAccesibles(estado, categoria, moneda, categoriasAccesibles)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SubastaResponse obtener(Long id, String email) {
        Usuario usuario = usuarioService.obtenerPorEmail(email);
        Subasta subasta = subastaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subasta", id));

        if (!usuario.getCategoria().puedeAcceder(subasta.getCategoria())) {
            throw new BusinessException(ErrorCodes.CATEGORIA_INSUFICIENTE,
                    "Tu categoría no te permite acceder a esta subasta", HttpStatus.FORBIDDEN);
        }

        return mapToResponse(subasta);
    }

    /**
     * Conecta al usuario a una subasta y registra su participación.
     * Valida: subasta abierta, categoría suficiente, no estar ya conectado a otra
     * subasta, y que el medio de pago seleccionado esté verificado.
     */
    @Transactional
    public ConectarSubastaResponse conectar(Long subastaId, String email, ConectarSubastaRequest request) {
        Usuario usuario = usuarioService.obtenerPorEmail(email);
        Subasta subasta = subastaRepository.findById(subastaId)
                .orElseThrow(() -> new ResourceNotFoundException("Subasta", subastaId));

        if (subasta.getEstado() != EstadoSubasta.ABIERTA) {
            throw new BusinessException(ErrorCodes.SUBASTA_NO_ABIERTA,
                    "La subasta no está abierta", HttpStatus.BAD_REQUEST);
        }

        if (!usuario.getCategoria().puedeAcceder(subasta.getCategoria())) {
            throw new BusinessException(ErrorCodes.CATEGORIA_INSUFICIENTE,
                    "Tu categoría no te permite acceder a esta subasta", HttpStatus.FORBIDDEN);
        }

        if (participacionRepository.existsByUsuarioAndConectadoTrue(usuario)) {
            throw new BusinessException(ErrorCodes.USUARIO_YA_CONECTADO,
                    "Ya estás conectado a una subasta", HttpStatus.CONFLICT);
        }

        MedioPago medioPago = medioPagoRepository.findByIdAndUsuario(request.getMedioPagoId(), usuario)
                .orElseThrow(() -> new ResourceNotFoundException("Medio de pago", request.getMedioPagoId()));

        if (!medioPago.isVerificado()) {
            throw new BusinessException(ErrorCodes.SIN_MEDIO_PAGO_VERIFICADO,
                    "El medio de pago no está verificado", HttpStatus.FORBIDDEN);
        }

        if (subasta.getMoneda() != medioPago.getMoneda()) {
            throw new BusinessException(ErrorCodes.MONEDA_NO_COINCIDE,
                    "La moneda del medio de pago no coincide con la moneda de la subasta", HttpStatus.BAD_REQUEST);
        }

        Participacion participacion = participacionRepository
                .findByUsuarioAndSubasta(usuario, subasta)
                .orElse(Participacion.builder().usuario(usuario).subasta(subasta).build());

        participacion.setMedioPago(medioPago);
        participacion.setConectado(true);
        participacion.setFechaConexion(LocalDateTime.now());
        participacion.setFechaDesconexion(null);
        participacionRepository.save(participacion);

        List<Item> items = itemRepository.findBySubasta(subasta);
        EstadoPujaResponse itemActual = items.stream()
                .filter(i -> i.getEstado() == EstadoItem.EN_SUBASTA)
                .findFirst()
                .map(i -> buildEstadoPuja(i, subasta))
                .orElse(null);

        return ConectarSubastaResponse.builder()
                .sesionId(participacion.getId())
                .streamingUrl("/ws")
                .itemActual(itemActual)
                .build();
    }

    @Transactional
    public void desconectar(Long subastaId, String email) {
        Usuario usuario = usuarioService.obtenerPorEmail(email);
        Subasta subasta = subastaRepository.findById(subastaId)
                .orElseThrow(() -> new ResourceNotFoundException("Subasta", subastaId));

        Participacion participacion = participacionRepository
                .findByUsuarioAndSubasta(usuario, subasta)
                .filter(Participacion::isConectado)
                .orElseThrow(() -> new BusinessException(ErrorCodes.USUARIO_NO_CONECTADO,
                        "No estás conectado a esta subasta", HttpStatus.FORBIDDEN));

        participacion.setConectado(false);
        participacion.setFechaDesconexion(LocalDateTime.now());
        participacionRepository.save(participacion);
    }

    @Transactional(readOnly = true)
    public EstadoPujaResponse obtenerEstadoPuja(Long subastaId, String email) {
        Usuario usuario = usuarioService.obtenerPorEmail(email);
        Subasta subasta = subastaRepository.findById(subastaId)
                .orElseThrow(() -> new ResourceNotFoundException("Subasta", subastaId));

        boolean conectado = participacionRepository
                .findByUsuarioAndSubasta(usuario, subasta)
                .map(Participacion::isConectado)
                .orElse(false);

        if (!conectado) {
            throw new BusinessException(ErrorCodes.USUARIO_NO_CONECTADO,
                    "No estás conectado a esta subasta", HttpStatus.FORBIDDEN);
        }

        List<Item> items = itemRepository.findBySubasta(subasta);
        Item itemActual = items.stream()
                .filter(i -> i.getEstado() == EstadoItem.EN_SUBASTA)
                .findFirst()
                .orElse(items.isEmpty() ? null : items.get(0));

        if (itemActual == null) {
            throw new ResourceNotFoundException("No hay ítems disponibles en esta subasta");
        }

        return buildEstadoPuja(itemActual, subasta);
    }

    @Transactional(readOnly = true)
    public List<Item> obtenerCatalogo(Long subastaId) {
        Subasta subasta = subastaRepository.findById(subastaId)
                .orElseThrow(() -> new ResourceNotFoundException("Subasta", subastaId));
        return itemRepository.findBySubastaWithDetails(subasta);
    }

    /**
     * Construye el estado actual de puja de un ítem.
     * Para categorías ORO y PLATINO los límites de puja son nulos (sin restricción).
     * Para el resto: mínimo = mejorOferta + 1% precio base, máximo = mejorOferta + 20% precio base.
     */
    EstadoPujaResponse buildEstadoPuja(Item item, Subasta subasta) {
        return EstadoPujaResponse.builder()
                .itemId(item.getId())
                .descripcion(item.getDescripcion())
                .precioBase(item.getPrecioBase())
                .mejorOferta(item.getMejorOferta())
                .mejorPostorAlias(AliasUtil.generarAlias(item.getMejorPostor()))
                .pujaMinima(PujaRangeUtil.calcularMinima(item, subasta))
                .pujaMaxima(PujaRangeUtil.calcularMaxima(item, subasta))
                .moneda(subasta.getMoneda())
                .estadoItem(item.getEstado())
                .build();
    }

    @Transactional
    public void cerrarSubasta(Subasta subasta) {
        log.info("Cerrando subasta id={} titulo='{}'", subasta.getId(), subasta.getTitulo());
        subasta.setEstado(EstadoSubasta.CERRADA);
        subastaRepository.save(subasta);

        List<Item> items = itemRepository.findBySubasta(subasta);
        for (Item item : items) {
            if (item.getEstado() != EstadoItem.EN_SUBASTA) continue;

            if (item.getMejorPostor() != null) {
                // Buscar el medio de pago con el que el ganador se conectó a la subasta
                MedioPago medioPagoGanador = participacionRepository
                        .findByUsuarioAndSubasta(item.getMejorPostor(), subasta)
                        .map(Participacion::getMedioPago)
                        .orElse(null);

                BigDecimal comisiones = item.getMejorOferta()
                        .multiply(PORCENTAJE_COMISION)
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                BigDecimal costoEnvio = COSTO_ENVIO;
                BigDecimal total = item.getMejorOferta().add(comisiones).add(costoEnvio);

                Compra compra = Compra.builder()
                        .item(item)
                        .usuario(item.getMejorPostor())
                        .montoOfertado(item.getMejorOferta())
                        .comisiones(comisiones)
                        .costoEnvio(costoEnvio)
                        .total(total)
                        .moneda(subasta.getMoneda())
                        .medioPago(medioPagoGanador)
                        .estadoPago(EstadoPago.PENDIENTE)
                        .fechaLimitePago(LocalDateTime.now().plusHours(72))
                        .build();
                compraRepository.save(compra);

                // Mensaje automático de bienvenida al chat post-subasta
                String msgBienvenida = String.format(
                        "¡Felicitaciones %s! Ganaste el ítem \"%s\" en la subasta.\n\n" +
                        "Resumen de tu compra:\n" +
                        "  • Monto ofertado:  %s %s\n" +
                        "  • Comisiones:      %s %s\n" +
                        "  • Costo de envío:  %s %s\n" +
                        "  • Total a pagar:   %s %s\n\n" +
                        "Tenés 72 horas para completar el pago.\n\n" +
                        "Por este chat podés coordinar:\n" +
                        "  • Modalidad de entrega: envío a domicilio (incluido) o retiro personal\n" +
                        "  • Ampliación de la cobertura del seguro del bien",
                        item.getMejorPostor().getNombre(), item.getDescripcion(),
                        item.getMejorOferta(), subasta.getMoneda(),
                        comisiones, subasta.getMoneda(),
                        costoEnvio, subasta.getMoneda(),
                        total, subasta.getMoneda());
                mensajeChatRepository.save(MensajeChat.builder()
                        .contenido(msgBienvenida)
                        .remitente(RemitenteMensaje.EMPRESA)
                        .compra(compra)
                        .usuario(item.getMejorPostor())
                        .build());

                item.setEstado(EstadoItem.VENDIDO);
                itemRepository.save(item);

                String desglose = String.format(
                        "Monto ofertado: %s %s\nComisiones: %s %s\nCosto de envío: %s %s\nTotal: %s %s",
                        item.getMejorOferta(), subasta.getMoneda(),
                        comisiones, subasta.getMoneda(),
                        costoEnvio, subasta.getMoneda(),
                        total, subasta.getMoneda());
                emailService.enviarNotificacionGanador(
                        item.getMejorPostor().getEmail(),
                        item.getMejorPostor().getNombre(),
                        item.getDescripcion(),
                        desglose);

                messagingTemplate.convertAndSend("/topic/subastas/" + subasta.getId(),
                        AuctionClosedMessage.builder()
                                .itemId(item.getId())
                                .ganadorAlias(AliasUtil.generarAlias(item.getMejorPostor()))
                                .montoFinal(item.getMejorOferta())
                                .build());
            } else {
                // Nadie pujó: la empresa compra el ítem al precio base (consigna)
                Compra compraEmpresa = Compra.builder()
                        .item(item)
                        .usuario(null)
                        .montoOfertado(item.getPrecioBase())
                        .comisiones(BigDecimal.ZERO)
                        .costoEnvio(BigDecimal.ZERO)
                        .total(item.getPrecioBase())
                        .moneda(subasta.getMoneda())
                        .estadoPago(EstadoPago.PAGADO)
                        .build();
                compraRepository.save(compraEmpresa);

                item.setEstado(EstadoItem.VENDIDO);
                itemRepository.save(item);

                messagingTemplate.convertAndSend("/topic/subastas/" + subasta.getId(),
                        AuctionClosedMessage.builder()
                                .itemId(item.getId())
                                .ganadorAlias(null)
                                .montoFinal(null)
                                .build());
            }
        }

        // Desconectar a todos los participantes conectados
        List<Participacion> conectados = participacionRepository.findBySubastaAndConectadoTrue(subasta);
        LocalDateTime ahora = LocalDateTime.now();
        for (Participacion p : conectados) {
            p.setConectado(false);
            p.setFechaDesconexion(ahora);
        }
        participacionRepository.saveAll(conectados);

        pujaService.liberarLock(subasta.getId());
    }

    private SubastaResponse mapToResponse(Subasta s) {
        SubastaResponse.RematadorInfo rematadorInfo = null;
        if (s.getRematador() != null) {
            rematadorInfo = SubastaResponse.RematadorInfo.builder()
                    .id(s.getRematador().getId())
                    .nombre(s.getRematador().getNombre())
                    .apellido(s.getRematador().getApellido())
                    .matricula(s.getRematador().getMatricula())
                    .build();
        }

        return SubastaResponse.builder()
                .id(s.getId())
                .titulo(s.getTitulo())
                .descripcion(s.getDescripcion())
                .fechaInicio(s.getFechaInicio())
                .categoria(s.getCategoria())
                .moneda(s.getMoneda())
                .estado(s.getEstado())
                .ubicacion(s.getUbicacion())
                .rematador(rematadorInfo)
                .totalItems((int) itemRepository.countBySubasta(s))
                .build();
    }

}
