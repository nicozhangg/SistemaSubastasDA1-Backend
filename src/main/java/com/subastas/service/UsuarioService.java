package com.subastas.service;

import com.subastas.exception.BusinessException;
import com.subastas.exception.ErrorCodes;
import com.subastas.exception.ResourceNotFoundException;
import com.subastas.model.dto.request.MedioPagoRequest;
import com.subastas.model.dto.response.MedioPagoResponse;
import com.subastas.model.dto.response.MetricasResponse;
import com.subastas.model.dto.response.ParticipacionResponse;
import com.subastas.model.dto.response.UsuarioResponse;
import com.subastas.model.entity.MedioPago;
import com.subastas.model.entity.Usuario;
import com.subastas.repository.CompraRepository;
import com.subastas.repository.MedioPagoRepository;
import com.subastas.repository.ParticipacionRepository;
import com.subastas.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Gestión del perfil de usuario: datos personales, medios de pago
 * y validaciones de estado para operaciones críticas (pujar, conectarse).
 */
@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final MedioPagoRepository medioPagoRepository;
    private final ParticipacionRepository participacionRepository;
    private final CompraRepository compraRepository;

    public Usuario obtenerPorEmail(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }

    public UsuarioResponse obtenerPerfil(String email) {
        Usuario usuario = obtenerPorEmail(email);
        return mapToResponse(usuario);
    }

    public List<MedioPagoResponse> listarMediosPago(String email) {
        Usuario usuario = obtenerPorEmail(email);
        return medioPagoRepository.findByUsuario(usuario).stream()
                .map(this::mapMedioPagoToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public MedioPagoResponse agregarMedioPago(String email, MedioPagoRequest request) {
        Usuario usuario = obtenerPorEmail(email);

        MedioPago medioPago = MedioPago.builder()
                .tipo(request.getTipo())
                .alias(request.getAlias())
                .moneda(request.getMoneda())
                .verificado(true)
                .montoLimite(request.getMontoCheque())
                .numeroCuenta(request.getNumeroCuenta())
                .banco(request.getBanco())
                .tipoCuenta(request.getTipoCuenta())
                .cbu(request.getCbu())
                .numeroTarjeta(request.getNumeroTarjeta())
                .titular(request.getTitular())
                .vencimiento(request.getVencimiento())
                .tipoTarjeta(request.getTipoTarjeta())
                .usuario(usuario)
                .build();

        // En producción se integraría con un procesador de pagos; en dev siempre aprueba
        medioPago = medioPagoRepository.save(medioPago);

        MedioPagoResponse response = mapMedioPagoToResponse(medioPago);
        response.setMensaje("Medio de pago agregado y verificado exitosamente");
        return response;
    }

    @Transactional
    public void eliminarMedioPago(String email, Long medioPagoId) {
        Usuario usuario = obtenerPorEmail(email);

        MedioPago medioPago = medioPagoRepository.findByIdAndUsuario(medioPagoId, usuario)
                .orElseThrow(() -> new ResourceNotFoundException("Medio de pago", medioPagoId));

        // Verificar que este medio de pago específico no esté en uso en una subasta activa
        boolean enUso = participacionRepository.existsByMedioPagoAndConectadoTrue(medioPago);
        if (enUso) {
            throw new BusinessException(ErrorCodes.MEDIO_PAGO_EN_USO,
                    "No podés eliminar este medio de pago porque está en uso en una subasta activa",
                    HttpStatus.CONFLICT);
        }

        medioPagoRepository.delete(medioPago);
    }

    public List<ParticipacionResponse> listarParticipaciones(String email) {
        Usuario usuario = obtenerPorEmail(email);
        return participacionRepository.findByUsuarioOrderByFechaConexionDesc(usuario).stream()
                .map(p -> ParticipacionResponse.builder()
                        .subastaId(p.getSubasta().getId())
                        .subastaTitulo(p.getSubasta().getTitulo())
                        .fechaConexion(p.getFechaConexion())
                        .fechaDesconexion(p.getFechaDesconexion())
                        .conectado(p.isConectado())
                        .build())
                .collect(Collectors.toList());
    }

    public MetricasResponse obtenerMetricas(String email) {
        Usuario usuario = obtenerPorEmail(email);

        return MetricasResponse.builder()
                .totalSubastasAsistidas((int) participacionRepository.countByUsuario(usuario))
                .totalGanadas((int) compraRepository.countByUsuario(usuario))
                .totalPagado(compraRepository.sumTotalByUsuario(usuario))
                .multasPendientes(usuario.getMultasPendientes())
                .build();
    }

    private UsuarioResponse mapToResponse(Usuario u) {
        return UsuarioResponse.builder()
                .id(u.getId())
                .nombre(u.getNombre())
                .apellido(u.getApellido())
                .email(u.getEmail())
                .categoria(u.getCategoria())
                .estado(u.getEstado())
                .domicilioLegal(u.getDomicilioLegal())
                .paisOrigen(u.getPaisOrigen())
                .fechaRegistro(u.getFechaRegistro())
                .multasPendientes(u.getMultasPendientes())
                .build();
    }

    private MedioPagoResponse mapMedioPagoToResponse(MedioPago mp) {
        return MedioPagoResponse.builder()
                .id(mp.getId())
                .tipo(mp.getTipo())
                .alias(mp.getAlias())
                .moneda(mp.getMoneda())
                .verificado(mp.isVerificado())
                .montoLimite(mp.getMontoLimite())
                .build();
    }
}
