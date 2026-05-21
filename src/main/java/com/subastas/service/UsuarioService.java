package com.subastas.service;

import com.subastas.exception.BusinessException;
import com.subastas.exception.ErrorCodes;
import com.subastas.exception.ResourceNotFoundException;
import com.subastas.model.dto.request.MedioPagoRequest;
import com.subastas.model.dto.response.MedioPagoResponse;
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

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
                .verificado(false)
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

        medioPago = medioPagoRepository.save(medioPago);

        // En producción se integraría con un procesador de pagos; en dev siempre aprueba
        medioPago.setVerificado(true);
        medioPagoRepository.save(medioPago);

        MedioPagoResponse response = mapMedioPagoToResponse(medioPago);
        response.setMensaje("Medio de pago agregado y verificado exitosamente");
        return response;
    }

    @Transactional
    public void eliminarMedioPago(String email, Long medioPagoId) {
        Usuario usuario = obtenerPorEmail(email);

        MedioPago medioPago = medioPagoRepository.findByIdAndUsuario(medioPagoId, usuario)
                .orElseThrow(() -> new ResourceNotFoundException("Medio de pago", medioPagoId));

        // Verificar que no esté en uso en una subasta activa
        boolean enUso = participacionRepository.existsByUsuarioAndConectadoTrue(usuario);
        if (enUso) {
            throw new BusinessException(ErrorCodes.MEDIO_PAGO_EN_USO,
                    "No podés eliminar un medio de pago mientras estás conectado a una subasta",
                    HttpStatus.CONFLICT);
        }

        medioPagoRepository.delete(medioPago);
    }

    public List<Map<String, Object>> listarParticipaciones(String email) {
        Usuario usuario = obtenerPorEmail(email);
        return participacionRepository.findByUsuarioOrderByFechaConexionDesc(usuario).stream()
                .map(p -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("subastaId", p.getSubasta().getId());
                    m.put("subastaTitulo", p.getSubasta().getTitulo());
                    m.put("fechaConexion", p.getFechaConexion());
                    m.put("fechaDesconexion", p.getFechaDesconexion());
                    m.put("conectado", p.isConectado());
                    return m;
                })
                .collect(Collectors.toList());
    }

    public Map<String, Object> obtenerMetricas(String email) {
        Usuario usuario = obtenerPorEmail(email);
        var participaciones = participacionRepository.findByUsuarioOrderByFechaConexionDesc(usuario);
        var compras = compraRepository.findByUsuarioOrderByIdDesc(usuario);
        BigDecimal totalPagado = compras.stream()
                .map(c -> c.getTotal() != null ? c.getTotal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("total_subastas_asistidas", participaciones.size());
        m.put("total_ganadas", compras.size());
        m.put("total_pagado", totalPagado);
        m.put("multas_pendientes", usuario.getMultasPendientes());
        return m;
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
