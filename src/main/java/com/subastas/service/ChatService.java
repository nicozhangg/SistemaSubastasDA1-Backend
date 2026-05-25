package com.subastas.service;

import com.subastas.exception.BusinessException;
import com.subastas.exception.ErrorCodes;
import com.subastas.exception.ResourceNotFoundException;
import com.subastas.model.dto.request.EntregaRequest;
import com.subastas.model.dto.request.MensajeChatRequest;
import com.subastas.model.dto.response.CompraResponse;
import com.subastas.model.dto.response.MensajeChatResponse;
import com.subastas.model.entity.Compra;
import com.subastas.model.entity.MensajeChat;
import com.subastas.model.entity.Usuario;
import com.subastas.model.enums.ModalidadEntrega;
import com.subastas.model.enums.RemitenteMensaje;
import com.subastas.repository.CompraRepository;
import com.subastas.repository.MensajeChatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Chat post-subasta entre el ganador y la empresa, vinculado a una Compra.
 * Permite coordinar modalidad de entrega y cobertura del seguro.
 */
@Service
@RequiredArgsConstructor
public class ChatService {

    private final CompraRepository compraRepository;
    private final MensajeChatRepository mensajeChatRepository;
    private final UsuarioService usuarioService;

    @Transactional(readOnly = true)
    public List<MensajeChatResponse> obtenerMensajes(Long compraId, String email) {
        Usuario usuario = usuarioService.obtenerPorEmail(email);
        Compra compra = obtenerCompraDelUsuario(compraId, usuario);

        return mensajeChatRepository.findByCompraOrderByTimestampAsc(compra).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public MensajeChatResponse enviarMensaje(Long compraId, String email, MensajeChatRequest request) {
        Usuario usuario = usuarioService.obtenerPorEmail(email);
        Compra compra = obtenerCompraDelUsuario(compraId, usuario);

        MensajeChat mensaje = MensajeChat.builder()
                .contenido(request.getContenido())
                .remitente(RemitenteMensaje.USUARIO)
                .compra(compra)
                .usuario(usuario)
                .build();

        mensaje = mensajeChatRepository.save(mensaje);

        // Marcar como leídos los mensajes de la empresa al responder
        mensajeChatRepository.marcarMensajesEmpresaComoLeidos(compra);

        return mapToResponse(mensaje);
    }

    @Transactional
    public CompraResponse confirmarEntrega(Long compraId, String email, EntregaRequest request) {
        Usuario usuario = usuarioService.obtenerPorEmail(email);
        Compra compra = obtenerCompraDelUsuario(compraId, usuario);

        if (compra.getModalidadEntrega() != null) {
            throw new BusinessException(ErrorCodes.ESTADO_INVALIDO,
                    "La modalidad de entrega ya fue confirmada");
        }

        compra.setModalidadEntrega(request.getModalidadEntrega());

        if (request.getModalidadEntrega() == ModalidadEntrega.ENVIO_DOMICILIO) {
            if (request.getDireccionEnvio() == null || request.getDireccionEnvio().isBlank()) {
                throw new BusinessException(ErrorCodes.ESTADO_INVALIDO,
                        "La dirección de envío es obligatoria para envío a domicilio");
            }
            compra.setDireccionEnvio(request.getDireccionEnvio());
        } else {
            // Retiro personal: se pierde la cobertura del seguro
            compra.setCoberturaSeguroActiva(false);
        }

        compra = compraRepository.save(compra);

        // Registrar la decisión como mensaje automático del sistema
        String textoAutomatico = request.getModalidadEntrega() == ModalidadEntrega.ENVIO_DOMICILIO
                ? "Modalidad confirmada: envío a domicilio a " + request.getDireccionEnvio()
                : "Modalidad confirmada: retiro personal. La cobertura del seguro queda inactiva.";

        mensajeChatRepository.save(MensajeChat.builder()
                .contenido(textoAutomatico)
                .remitente(RemitenteMensaje.EMPRESA)
                .compra(compra)
                .usuario(usuario)
                .build());

        return mapCompraToResponse(compra);
    }

    private Compra obtenerCompraDelUsuario(Long compraId, Usuario usuario) {
        return compraRepository.findByIdAndUsuario(compraId, usuario)
                .orElseThrow(() -> new ResourceNotFoundException("Compra", compraId));
    }

    private MensajeChatResponse mapToResponse(MensajeChat m) {
        return MensajeChatResponse.builder()
                .mensajeId(m.getId())
                .contenido(m.getContenido())
                .timestamp(m.getTimestamp())
                .remitente(m.getRemitente())
                .leido(m.isLeido())
                .build();
    }

    private CompraResponse mapCompraToResponse(Compra compra) {
        return CompraResponse.builder()
                .compraId(compra.getId())
                .item(CompraResponse.ItemInfo.builder()
                        .id(compra.getItem().getId())
                        .descripcion(compra.getItem().getDescripcion())
                        .numeroPieza(compra.getItem().getNumeroPieza())
                        .build())
                .montoOfertado(compra.getMontoOfertado())
                .comisiones(compra.getComisiones())
                .costoEnvio(compra.getCostoEnvio())
                .total(compra.getTotal())
                .moneda(compra.getMoneda())
                .medioPago(compra.getMedioPago() != null
                        ? CompraResponse.MedioPagoInfo.builder()
                            .id(compra.getMedioPago().getId())
                            .alias(compra.getMedioPago().getAlias())
                            .tipo(compra.getMedioPago().getTipo().name())
                            .build()
                        : null)
                .estadoPago(compra.getEstadoPago())
                .direccionEnvio(compra.getDireccionEnvio())
                .modalidadEntrega(compra.getModalidadEntrega())
                .coberturaSeguroActiva(compra.isCoberturaSeguroActiva())
                .fechaLimitePago(compra.getFechaLimitePago())
                .build();
    }
}
