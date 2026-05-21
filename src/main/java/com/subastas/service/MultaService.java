package com.subastas.service;

import com.subastas.exception.BusinessException;
import com.subastas.exception.ErrorCodes;
import com.subastas.exception.ResourceNotFoundException;
import com.subastas.model.dto.request.PagarMultaRequest;
import com.subastas.model.dto.response.MultaResponse;
import com.subastas.model.entity.Multa;
import com.subastas.model.entity.Usuario;
import com.subastas.model.enums.EstadoMulta;
import com.subastas.repository.MedioPagoRepository;
import com.subastas.repository.MultaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Gestión de multas por incumplimiento de pago.
 * Un usuario con multas pendientes no puede realizar nuevas pujas hasta saldarlas.
 */
@Service
@RequiredArgsConstructor
public class MultaService {

    private final MultaRepository multaRepository;
    private final MedioPagoRepository medioPagoRepository;
    private final UsuarioService usuarioService;

    public List<MultaResponse> listarMultas(String email) {
        Usuario usuario = usuarioService.obtenerPorEmail(email);
        return multaRepository.findByUsuarioOrderByFechaGeneracionDesc(usuario).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public MultaResponse pagarMulta(Long multaId, String email, PagarMultaRequest request) {
        Usuario usuario = usuarioService.obtenerPorEmail(email);

        Multa multa = multaRepository.findByIdAndUsuario(multaId, usuario)
                .orElseThrow(() -> new ResourceNotFoundException("Multa", multaId));

        if (multa.getEstado() != EstadoMulta.PENDIENTE) {
            throw new BusinessException(ErrorCodes.ESTADO_INVALIDO,
                    "Esta multa no está pendiente de pago");
        }

        medioPagoRepository.findByIdAndUsuario(request.getMedioPagoId(), usuario)
                .orElseThrow(() -> new ResourceNotFoundException("Medio de pago", request.getMedioPagoId()));

        multa.setEstado(EstadoMulta.PAGADA);
        multaRepository.save(multa);

        long pendientes = multaRepository.countByUsuarioAndEstado(usuario, EstadoMulta.PENDIENTE);
        usuario.setMultasPendientes((int) pendientes);

        MultaResponse response = mapToResponse(multa);
        response.setPuedeParticiparNuevamente(pendientes == 0);
        return response;
    }

    private MultaResponse mapToResponse(Multa m) {
        return MultaResponse.builder()
                .multaId(m.getId())
                .monto(m.getMonto())
                .motivo(m.getMotivo())
                .fechaGeneracion(m.getFechaGeneracion())
                .fechaLimitePago(m.getFechaLimitePago())
                .estado(m.getEstado())
                .build();
    }
}
