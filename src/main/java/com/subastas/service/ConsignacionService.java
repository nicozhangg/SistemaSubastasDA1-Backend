package com.subastas.service;

import com.subastas.exception.BusinessException;
import com.subastas.exception.ErrorCodes;
import com.subastas.exception.ResourceNotFoundException;
import com.subastas.model.dto.response.ConsignacionResponse;
import com.subastas.model.dto.response.PolizaResponse;
import com.subastas.model.dto.response.UbicacionResponse;
import com.subastas.model.entity.*;
import com.subastas.model.enums.EstadoConsignacion;
import com.subastas.repository.ConsignacionRepository;
import com.subastas.repository.MedioPagoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.subastas.util.FileUtil;
import org.apache.tika.Tika;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Gestión del ciclo de vida de una consignación: solicitud del usuario,
 * revisión por la empresa, aceptación/rechazo de condiciones, y consulta
 * de depósito y póliza de seguro asociados al bien.
 */
@Service
@RequiredArgsConstructor
public class ConsignacionService {

    private static final BigDecimal GASTOS_RETIRO = new BigDecimal("5000.00");
    private static final Tika TIKA = new Tika();

    private final ConsignacionRepository consignacionRepository;
    private final MedioPagoRepository medioPagoRepository;

    @Value("${app.uploads.base-path:uploads}")
    private String uploadsBasePath;

    public List<ConsignacionResponse> listar(Usuario usuario) {
        return consignacionRepository.findByUsuarioOrderByIdDesc(usuario).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ConsignacionResponse crear(Usuario usuario, String descripcion, String datosAdicionales,
                                      boolean aceptaPertenencia, Long cuentaDestinoId,
                                      BigDecimal precioSugerido, List<MultipartFile> fotos) {
        if (!aceptaPertenencia) {
            throw new BusinessException(ErrorCodes.PERTENENCIA_NO_DECLARADA,
                    "Debés declarar que el bien te pertenece");
        }

        if (fotos == null || fotos.size() < 6 || fotos.size() > 20) {
            throw new BusinessException(ErrorCodes.FOTOS_INSUFICIENTES,
                    "Debés subir entre 6 y 20 fotos del bien");
        }

        for (MultipartFile foto : fotos) {
            try {
                String tipoDetectado = TIKA.detect(foto.getBytes());
                if (!tipoDetectado.startsWith("image/")) {
                    throw new BusinessException(ErrorCodes.ESTADO_INVALIDO,
                            "Solo se permiten imágenes (JPEG, PNG, GIF, WebP)");
                }
            } catch (BusinessException e) {
                throw e;
            } catch (IOException e) {
                throw new BusinessException(ErrorCodes.ESTADO_INVALIDO,
                        "No se pudo leer el archivo");
            }
            if (foto.getSize() > 5_242_880L) {
                throw new BusinessException(ErrorCodes.ESTADO_INVALIDO,
                        "Cada foto no debe superar 5 MB");
            }
        }

        MedioPago cuentaDestino = medioPagoRepository.findByIdAndUsuario(cuentaDestinoId, usuario)
                .orElseThrow(() -> new ResourceNotFoundException("Medio de pago", cuentaDestinoId));

        Consignacion consignacion = Consignacion.builder()
                .descripcion(descripcion)
                .datosAdicionales(datosAdicionales)
                .aceptaPertenencia(aceptaPertenencia)
                .estado(EstadoConsignacion.PENDIENTE_REVISION)
                .precioSugerido(precioSugerido)
                .cuentaDestino(cuentaDestino)
                .usuario(usuario)
                .build();

        consignacion = consignacionRepository.save(consignacion);

        // Guardar fotos con nombre UUID para evitar path traversal
        List<FotoConsignacion> fotosEntidad = new ArrayList<>();
        for (int i = 0; i < fotos.size(); i++) {
            MultipartFile foto = fotos.get(i);
            String nombreArchivo = FileUtil.uuidFilename(foto);
            String urlRelativa = uploadsBasePath + "/consignaciones/" + consignacion.getId() + "/" + nombreArchivo;
            try {
                Path destino = Paths.get(urlRelativa);
                Files.createDirectories(destino.getParent());
                foto.transferTo(destino.toFile());
            } catch (IOException e) {
                throw new RuntimeException("Error al guardar foto de consignación", e);
            }
            FotoConsignacion fotoConsignacion = FotoConsignacion.builder()
                    .url(urlRelativa)
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
        Consignacion consignacion = obtenerConsignacionParaDecision(consignacionId, usuario);

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
        Consignacion consignacion = obtenerConsignacionParaDecision(consignacionId, usuario);

        consignacion.setEstado(EstadoConsignacion.DEVUELTA);
        consignacion = consignacionRepository.save(consignacion);

        ConsignacionResponse response = mapToResponse(consignacion);
        response.setMensaje("Condiciones rechazadas. El bien será devuelto con cargo de retiro.");
        response.setGastosEstimados(GASTOS_RETIRO);
        return response;
    }

    public UbicacionResponse obtenerUbicacion(Long consignacionId, Usuario usuario) {
        Consignacion consignacion = consignacionRepository.findByIdAndUsuario(consignacionId, usuario)
                .orElseThrow(() -> new BusinessException(ErrorCodes.ACCESO_DENEGADO,
                        "No autorizado", HttpStatus.FORBIDDEN));

        Deposito deposito = consignacion.getDeposito();
        if (deposito == null) {
            throw new ResourceNotFoundException("El bien aún no fue asignado a un depósito");
        }

        return UbicacionResponse.builder()
                .depositoNombre(deposito.getNombre())
                .depositoDireccion(deposito.getDireccion())
                .lat(deposito.getLatitud())
                .lng(deposito.getLongitud())
                .fechaIngreso(deposito.getFechaIngreso())
                .estadoFisico(deposito.getEstadoFisico())
                .build();
    }

    public PolizaResponse obtenerPoliza(Long consignacionId, Usuario usuario) {
        Consignacion consignacion = consignacionRepository.findByIdAndUsuario(consignacionId, usuario)
                .orElseThrow(() -> new BusinessException(ErrorCodes.ACCESO_DENEGADO,
                        "No autorizado", HttpStatus.FORBIDDEN));

        Poliza poliza = consignacion.getPoliza();
        if (poliza == null) {
            throw new ResourceNotFoundException("No hay póliza asociada a esta consignación");
        }

        return PolizaResponse.builder()
                .polizaId(poliza.getId())
                .aseguradoraNombre(poliza.getAseguradoraNombre())
                .aseguradoraContacto(poliza.getAseguradoraContacto())
                .valorAsegurado(poliza.getValorAsegurado())
                .prima(poliza.getPrima())
                .vigenciaDesde(poliza.getVigenciaDesde())
                .vigenciaHasta(poliza.getVigenciaHasta())
                .bienesCubiertos(poliza.getBienesCubiertos())
                .build();
    }

    private Consignacion obtenerConsignacionParaDecision(Long consignacionId, Usuario usuario) {
        Consignacion consignacion = consignacionRepository.findByIdAndUsuario(consignacionId, usuario)
                .orElseThrow(() -> new ResourceNotFoundException("Consignación", consignacionId));
        if (consignacion.getEstado() != EstadoConsignacion.ACEPTADA) {
            throw new BusinessException(ErrorCodes.ESTADO_INVALIDO,
                    "La consignación no está en estado de aceptación de condiciones");
        }
        return consignacion;
    }

    private ConsignacionResponse mapToResponse(Consignacion c) {
        List<String> fotos = c.getFotos() != null
                ? c.getFotos().stream().map(FotoConsignacion::getUrl).collect(Collectors.toList())
                : List.of();

        return ConsignacionResponse.builder()
                .consignacionId(c.getId())
                .descripcion(c.getDescripcion())
                .datosAdicionales(c.getDatosAdicionales())
                .estado(c.getEstado())
                .aceptaPertenencia(c.isAceptaPertenencia())
                .motivoRechazo(c.getMotivoRechazo())
                .precioSugerido(c.getPrecioSugerido())
                .valorBase(c.getValorBase())
                .comisiones(c.getComisiones())
                .subastaId(c.getSubastaAsignada() != null ? c.getSubastaAsignada().getId() : null)
                .fotosUrls(fotos)
                .build();
    }
}
