package com.subastas.service;

import com.subastas.exception.BusinessException;
import com.subastas.exception.ErrorCodes;
import com.subastas.exception.ResourceNotFoundException;
import com.subastas.model.dto.response.ConsignacionResponse;
import com.subastas.model.entity.*;
import com.subastas.model.enums.EstadoConsignacion;
import com.subastas.repository.ConsignacionRepository;
import com.subastas.repository.MedioPagoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Gestión del ciclo de vida de una consignación: solicitud del usuario,
 * revisión por la empresa, aceptación/rechazo de condiciones, y consulta
 * de depósito y póliza de seguro asociados al bien.
 */
@Service
@RequiredArgsConstructor
public class ConsignacionService {

    private final ConsignacionRepository consignacionRepository;
    private final MedioPagoRepository medioPagoRepository;

    public List<ConsignacionResponse> listar(Usuario usuario) {
        return consignacionRepository.findByUsuarioOrderByIdDesc(usuario).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ConsignacionResponse crear(Usuario usuario, String descripcion, String datosAdicionales,
                                      boolean aceptaPertenencia, Long cuentaDestinoId,
                                      List<MultipartFile> fotos) {
        if (!aceptaPertenencia) {
            throw new BusinessException(ErrorCodes.PERTENENCIA_NO_DECLARADA,
                    "Debés declarar que el bien te pertenece");
        }

        if (fotos == null || fotos.size() < 6) {
            throw new BusinessException(ErrorCodes.FOTOS_INSUFICIENTES,
                    "Debés subir al menos 6 fotos del bien");
        }

        MedioPago cuentaDestino = medioPagoRepository.findByIdAndUsuario(cuentaDestinoId, usuario)
                .orElseThrow(() -> new ResourceNotFoundException("Medio de pago", cuentaDestinoId));

        Consignacion consignacion = Consignacion.builder()
                .descripcion(descripcion)
                .datosAdicionales(datosAdicionales)
                .aceptaPertenencia(true)
                .estado(EstadoConsignacion.PENDIENTE_REVISION)
                .cuentaDestino(cuentaDestino)
                .usuario(usuario)
                .build();

        consignacion = consignacionRepository.save(consignacion);

        // Guardar fotos
        List<FotoConsignacion> fotosEntidad = new ArrayList<>();
        for (int i = 0; i < fotos.size(); i++) {
            MultipartFile foto = fotos.get(i);
            FotoConsignacion fotoConsignacion = FotoConsignacion.builder()
                    .url("uploads/consignaciones/" + consignacion.getId() + "/" + foto.getOriginalFilename())
                    .orden(i + 1)
                    .consignacion(consignacion)
                    .build();
            fotosEntidad.add(fotoConsignacion);
        }
        consignacion.setFotos(fotosEntidad);
        consignacion = consignacionRepository.save(consignacion);

        ConsignacionResponse response = mapToResponse(consignacion);
        response.setMensaje("Consignación creada. La empresa revisará tu solicitud.");
        return response;
    }

    @Transactional
    public ConsignacionResponse aceptarCondiciones(Long consignacionId, Usuario usuario) {
        Consignacion consignacion = consignacionRepository.findByIdAndUsuario(consignacionId, usuario)
                .orElseThrow(() -> new ResourceNotFoundException("Consignación", consignacionId));

        if (consignacion.getEstado() != EstadoConsignacion.ACEPTADA) {
            throw new BusinessException(ErrorCodes.ESTADO_INVALIDO,
                    "La consignación no está en estado de aceptación de condiciones");
        }

        consignacion.setEstado(EstadoConsignacion.EN_SUBASTA);
        consignacion = consignacionRepository.save(consignacion);

        ConsignacionResponse response = mapToResponse(consignacion);
        response.setMensaje("Condiciones aceptadas. El bien será incluido en la subasta.");
        if (consignacion.getSubastaAsignada() != null) {
            response.setSubastaId(consignacion.getSubastaAsignada().getId());
            response.setFechaSubasta(consignacion.getSubastaAsignada().getFechaInicio());
        }
        return response;
    }

    @Transactional
    public ConsignacionResponse rechazarCondiciones(Long consignacionId, Usuario usuario) {
        Consignacion consignacion = consignacionRepository.findByIdAndUsuario(consignacionId, usuario)
                .orElseThrow(() -> new ResourceNotFoundException("Consignación", consignacionId));

        if (consignacion.getEstado() != EstadoConsignacion.ACEPTADA) {
            throw new BusinessException(ErrorCodes.ESTADO_INVALIDO,
                    "La consignación no está en estado de aceptación de condiciones");
        }

        consignacion.setEstado(EstadoConsignacion.DEVUELTA);
        consignacion = consignacionRepository.save(consignacion);

        ConsignacionResponse response = mapToResponse(consignacion);
        response.setMensaje("Condiciones rechazadas. El bien será devuelto con cargo de retiro.");
        response.setGastosEstimados(new BigDecimal("5000.00")); // valor fijo hasta integrar cotización real de retiro
        return response;
    }

    public Object obtenerUbicacion(Long consignacionId, Usuario usuario) {
        Consignacion consignacion = consignacionRepository.findByIdAndUsuario(consignacionId, usuario)
                .orElseThrow(() -> new BusinessException(ErrorCodes.ACCESO_DENEGADO,
                        "No autorizado", HttpStatus.FORBIDDEN));

        Deposito deposito = consignacion.getDeposito();
        if (deposito == null) {
            throw new ResourceNotFoundException("El bien aún no fue asignado a un depósito");
        }

        Map<String, Object> coordenadas = new java.util.LinkedHashMap<>();
        coordenadas.put("lat", deposito.getLatitud());
        coordenadas.put("lng", deposito.getLongitud());

        Map<String, Object> depositoMap = new java.util.LinkedHashMap<>();
        depositoMap.put("nombre", deposito.getNombre());
        depositoMap.put("direccion", deposito.getDireccion());
        depositoMap.put("coordenadas", coordenadas);

        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("deposito", depositoMap);
        result.put("fecha_ingreso", deposito.getFechaIngreso());
        result.put("estado_fisico", deposito.getEstadoFisico());
        return result;
    }

    public Object obtenerPoliza(Long consignacionId, Usuario usuario) {
        Consignacion consignacion = consignacionRepository.findByIdAndUsuario(consignacionId, usuario)
                .orElseThrow(() -> new BusinessException(ErrorCodes.ACCESO_DENEGADO,
                        "No autorizado", HttpStatus.FORBIDDEN));

        Poliza poliza = consignacion.getPoliza();
        if (poliza == null) {
            throw new ResourceNotFoundException("No hay póliza asociada a esta consignación");
        }

        Map<String, Object> aseguradora = new java.util.LinkedHashMap<>();
        aseguradora.put("nombre", poliza.getAseguradoraNombre());
        aseguradora.put("contacto", poliza.getAseguradoraContacto());

        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("poliza_id", poliza.getId());
        result.put("aseguradora", aseguradora);
        result.put("valor_asegurado", poliza.getValorAsegurado());
        result.put("prima", poliza.getPrima());
        result.put("vigencia_desde", poliza.getVigenciaDesde());
        result.put("vigencia_hasta", poliza.getVigenciaHasta());
        result.put("bienes_cubiertos", poliza.getBienesCubiertos());
        return result;
    }

    private ConsignacionResponse mapToResponse(Consignacion c) {
        List<String> fotos = c.getFotos() != null
                ? c.getFotos().stream().map(FotoConsignacion::getUrl).collect(Collectors.toList())
                : List.of();

        return ConsignacionResponse.builder()
                .consignacionId(c.getId())
                .descripcion(c.getDescripcion())
                .estado(c.getEstado())
                .aceptaPertenencia(c.isAceptaPertenencia())
                .motivoRechazo(c.getMotivoRechazo())
                .valorBase(c.getValorBase())
                .comisiones(c.getComisiones())
                .subastaId(c.getSubastaAsignada() != null ? c.getSubastaAsignada().getId() : null)
                .fotosUrls(fotos)
                .build();
    }
}
