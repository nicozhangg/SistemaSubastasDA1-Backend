package com.subastas.service;

import com.subastas.exception.ResourceNotFoundException;
import com.subastas.model.dto.response.CompraResponse;
import com.subastas.model.entity.Compra;
import com.subastas.model.entity.Usuario;
import com.subastas.repository.CompraRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CompraService {

    private final CompraRepository compraRepository;
    private final UsuarioService usuarioService;

    @Transactional(readOnly = true)
    public CompraResponse obtenerCompra(Long compraId, String email) {
        Usuario usuario = usuarioService.obtenerPorEmail(email);
        Compra compra = compraRepository.findByIdAndUsuario(compraId, usuario)
                .orElseThrow(() -> new ResourceNotFoundException("Compra", compraId));
        return mapToResponse(compra);
    }

    public CompraResponse mapToResponse(Compra compra) {
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
