package com.subastas.service;

import com.subastas.exception.BusinessException;
import com.subastas.exception.ErrorCodes;
import com.subastas.exception.ResourceNotFoundException;
import com.subastas.model.dto.request.ConectarSubastaRequest;
import com.subastas.model.dto.response.ConectarSubastaResponse;
import com.subastas.model.dto.response.EstadoPujaResponse;
import com.subastas.model.dto.response.SubastaResponse;
import com.subastas.model.entity.*;
import com.subastas.util.PujaRangeUtil;
import com.subastas.model.enums.Categoria;
import com.subastas.model.enums.EstadoItem;
import com.subastas.model.enums.EstadoSubasta;
import com.subastas.model.enums.Moneda;
import com.subastas.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Lógica de negocio de subastas: listado con control de acceso por categoría,
 * conexión/desconexión de postores y cálculo del estado de puja del ítem activo.
 */
@Service
@RequiredArgsConstructor
public class SubastaService {

    private final SubastaRepository subastaRepository;
    private final ItemRepository itemRepository;
    private final MedioPagoRepository medioPagoRepository;
    private final ParticipacionRepository participacionRepository;
    private final UsuarioService usuarioService;

    /**
     * Filtra las subastas que el usuario puede ver según su categoría.
     * Un usuario PLATA ve subastas COMUN, ESPECIAL y PLATA, pero no ORO ni PLATINO.
     */
    public Page<SubastaResponse> listar(EstadoSubasta estado, Categoria categoria,
                                        Moneda moneda, int page, String email) {
        Usuario usuario = usuarioService.obtenerPorEmail(email);

        List<Categoria> categoriasAccesibles = Arrays.stream(Categoria.values())
                .filter(c -> c.ordinal() <= usuario.getCategoria().ordinal())
                .collect(Collectors.toList());

        return subastaRepository
                .findAccesibles(estado, categoria, moneda, categoriasAccesibles, PageRequest.of(page, 20))
                .map(this::mapToResponse);
    }

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

        Participacion participacion = participacionRepository
                .findByUsuarioAndSubasta(usuario, subasta)
                .orElse(Participacion.builder().usuario(usuario).subasta(subasta).build());

        participacion.setMedioPago(medioPago);
        participacion.setConectado(true);
        participacion.setFechaConexion(LocalDateTime.now());
        participacion.setFechaDesconexion(null);
        participacionRepository.save(participacion);

        List<Item> items = itemRepository.findBySubasta(subasta);
        EstadoPujaResponse itemActual = items.isEmpty() ? null : buildEstadoPuja(items.get(0), subasta);

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
                .mejorPostorAlias(generarAlias(item.getMejorPostor()))
                .pujaMinima(PujaRangeUtil.calcularMinima(item, subasta))
                .pujaMaxima(PujaRangeUtil.calcularMaxima(item, subasta))
                .moneda(subasta.getMoneda())
                .estadoItem(item.getEstado())
                .build();
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
                .totalItems(s.getItems() != null ? s.getItems().size() : 0)
                .build();
    }

    /** Genera un alias anónimo del postor para no exponer su identidad en el historial público. */
    static String generarAlias(Usuario usuario) {
        if (usuario == null) return null;
        String nombre = usuario.getNombre();
        if (nombre == null || nombre.isBlank()) return "postor_***";
        return "postor_" + nombre.substring(0, Math.min(3, nombre.length())) + "***";
    }
}
