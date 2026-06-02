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
    private final CompraService compraService;

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
            throw new BusinessException(ErrorCodes.ENTREGA_YA_CONFIRMADA,
                    "La modalidad de entrega ya fue confirmada");
        }

        compra.setModalidadEntrega(request.getModalidadEntrega());

        if (request.getModalidadEntrega() == ModalidadEntrega.ENVIO_DOMICILIO) {
            if (request.getDireccionEnvio() == null || request.getDireccionEnvio().isBlank()) {
                throw new BusinessException(ErrorCodes.DIRECCION_REQUERIDA,
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

        return compraService.mapToResponse(compra);
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
}
