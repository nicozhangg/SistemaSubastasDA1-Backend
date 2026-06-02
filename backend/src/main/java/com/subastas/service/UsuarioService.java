package com.subastas.service;

import com.subastas.exception.BusinessException;
import com.subastas.exception.ErrorCodes;
import com.subastas.exception.ResourceNotFoundException;
import com.subastas.model.dto.request.MedioPagoRequest;
import com.subastas.model.dto.response.MedioPagoResponse;
import com.subastas.model.dto.response.MetricasResponse;
import com.subastas.model.dto.response.ParticipacionHistorialResponse;
import com.subastas.model.dto.response.UsuarioResponse;
import com.subastas.model.entity.MedioPago;
import com.subastas.model.entity.Participacion;
import com.subastas.model.entity.Usuario;
import com.subastas.model.enums.Categoria;
import com.subastas.model.enums.EstadoPago;
import com.subastas.model.enums.EstadoSubasta;
import com.subastas.repository.CompraRepository;
import com.subastas.repository.MedioPagoRepository;
import com.subastas.repository.ParticipacionRepository;
import com.subastas.repository.PujaRepository;
import com.subastas.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
    private final PujaRepository pujaRepository;

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

    public MetricasResponse obtenerMetricas(String email) {
        Usuario usuario = obtenerPorEmail(email);

        long totalAsistidas = participacionRepository.countByUsuario(usuario);
        long totalGanadas = compraRepository.countByUsuario(usuario);
        BigDecimal totalOfertado = compraRepository.sumMontoOfertadoByUsuario(usuario);
        BigDecimal totalPagado = compraRepository.sumTotalByUsuarioAndEstadoPago(usuario, EstadoPago.PAGADO);

        double porcentaje = totalAsistidas > 0
                ? Math.round((double) totalGanadas / totalAsistidas * 10000.0) / 100.0
                : 0.0;

        // Participaciones por categoría de subasta
        List<Object[]> rows = participacionRepository.countByUsuarioGroupByCategoria(usuario);
        Map<String, Long> porCategoria = new LinkedHashMap<>();
        String categoriaMas = null;
        long maxCount = 0;
        for (Object[] row : rows) {
            Categoria cat = (Categoria) row[0];
            long count = ((Number) row[1]).longValue();
            porCategoria.put(cat.name(), count);
            if (count > maxCount) {
                maxCount = count;
                categoriaMas = cat.name();
            }
        }

        return MetricasResponse.builder()
                .totalSubastasAsistidas(totalAsistidas)
                .totalGanadas(totalGanadas)
                .totalOfertado(totalOfertado != null ? totalOfertado : BigDecimal.ZERO)
                .totalPagado(totalPagado != null ? totalPagado : BigDecimal.ZERO)
                .porcentajeVictorias(porcentaje)
                .categoriaMasParticipada(categoriaMas)
                .subastasPorCategoria(porCategoria)
                .build();
    }

    public List<ParticipacionHistorialResponse> obtenerParticipaciones(
            String email, String resultado, LocalDateTime desde, LocalDateTime hasta) {
        Usuario usuario = obtenerPorEmail(email);

        List<Participacion> todas;
        if (desde != null && hasta != null) {
            todas = participacionRepository.findByUsuarioAndFechaConexionBetweenOrderByFechaConexionDesc(usuario, desde, hasta);
        } else if (desde != null) {
            todas = participacionRepository.findByUsuarioAndFechaConexionAfterOrderByFechaConexionDesc(usuario, desde);
        } else if (hasta != null) {
            todas = participacionRepository.findByUsuarioAndFechaConexionBeforeOrderByFechaConexionDesc(usuario, hasta);
        } else {
            todas = participacionRepository.findByUsuarioOrderByFechaConexionDesc(usuario);
        }

        List<ParticipacionHistorialResponse> respuestas = todas.stream().map(p -> {
            var subasta = p.getSubasta();
            long itemsPujados = pujaRepository.countItemsDistinctBySubastaAndUsuario(subasta, usuario);
            long itemsGanados = compraRepository.countGanadasByUsuarioAndSubastaId(usuario, subasta.getId());
            BigDecimal monto = pujaRepository.sumMontoBySubastaAndUsuario(subasta, usuario);

            String res;
            if (p.isConectado() || subasta.getEstado() == EstadoSubasta.ABIERTA) {
                res = "en_curso";
            } else if (itemsGanados > 0) {
                res = "ganada";
            } else {
                res = "perdida";
            }

            return ParticipacionHistorialResponse.builder()
                    .subastaId(subasta.getId())
                    .titulo(subasta.getTitulo())
                    .fecha(subasta.getFechaInicio())
                    .categoria(subasta.getCategoria())
                    .itemsPujados(itemsPujados)
                    .itemsGanados(itemsGanados)
                    .montoTotalOfertado(monto != null ? monto : BigDecimal.ZERO)
                    .resultado(res)
                    .build();
        }).collect(Collectors.toList());

        // Filtro por resultado (si se especifica)
        if (resultado != null && !resultado.isBlank()) {
            respuestas = respuestas.stream()
                    .filter(r -> r.getResultado().equalsIgnoreCase(resultado))
                    .collect(Collectors.toList());
        }

        return respuestas;
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
