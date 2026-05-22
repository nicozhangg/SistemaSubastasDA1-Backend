package com.subastas.controller;

import com.subastas.exception.ResourceNotFoundException;
import com.subastas.model.dto.response.ItemResponse;
import com.subastas.model.entity.Item;
import com.subastas.model.entity.Poliza;
import com.subastas.model.entity.Subasta;
import com.subastas.repository.ItemRepository;
import com.subastas.service.SubastaService;
import com.subastas.util.PujaRangeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Endpoints públicos del catálogo. No requieren JWT, pero el precio base
 * de cada ítem solo se incluye en la respuesta cuando el token está presente.
 */
@RestController
@RequiredArgsConstructor
public class CatalogoController {

    private final SubastaService subastaService;
    private final ItemRepository itemRepository;

    /** precio_base se omite (null) para usuarios no autenticados (invitados). */
    @GetMapping("/api/v1/subastas/{id}/catalogo")
    public ResponseEntity<List<ItemResponse>> obtenerCatalogo(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        boolean autenticado = userDetails != null;
        List<Item> items = subastaService.obtenerCatalogo(id);

        List<ItemResponse> response = items.stream()
                .map(item -> {
                    ItemResponse.SubastaInfo subastaInfo = buildSubastaInfo(item.getSubasta());
                    return ItemResponse.builder()
                            .itemId(item.getId())
                            .numeroPieza(item.getNumeroPieza())
                            .descripcion(item.getDescripcion())
                            .precioBase(autenticado ? item.getPrecioBase() : null)
                            .estado(item.getEstado())
                            .esObraArte(item.isEsObraArte())
                            .ubicacionFisica(item.getUbicacionFisica())
                            .mejorOferta(autenticado ? item.getMejorOferta() : null)
                            .subasta(subastaInfo)
                            .pujaMinima(autenticado ? calcularPujaMinima(item) : null)
                            .pujaMaxima(autenticado ? calcularPujaMaxima(item) : null)
                            .imagenes(buildImagenes(item))
                            .build();
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/v1/items/{itemId}")
    public ResponseEntity<ItemResponse> obtenerItem(
            @PathVariable Long itemId,
            @AuthenticationPrincipal UserDetails userDetails) {

        boolean autenticado = userDetails != null;
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item", itemId));

        ItemResponse response = ItemResponse.builder()
                .itemId(item.getId())
                .numeroPieza(item.getNumeroPieza())
                .descripcion(item.getDescripcion())
                .precioBase(autenticado ? item.getPrecioBase() : null)
                .estado(item.getEstado())
                .duenioActual(item.getDuenioActual())
                .esObraArte(item.isEsObraArte())
                .artista(item.getArtista())
                .fechaCreacion(item.getFechaCreacion())
                .historia(item.getHistoria())
                .componentes(item.getComponentes())
                .ubicacionFisica(item.getUbicacionFisica())
                .mejorOferta(autenticado ? item.getMejorOferta() : null)
                .pujaMinima(autenticado ? calcularPujaMinima(item) : null)
                .pujaMaxima(autenticado ? calcularPujaMaxima(item) : null)
                .subasta(buildSubastaInfo(item.getSubasta()))
                .poliza(buildPolizaInfo(item.getPoliza()))
                .imagenes(buildImagenes(item))
                .build();

        return ResponseEntity.ok(response);
    }

    private ItemResponse.SubastaInfo buildSubastaInfo(Subasta subasta) {
        if (subasta == null) return null;
        return ItemResponse.SubastaInfo.builder()
                .id(subasta.getId())
                .titulo(subasta.getTitulo())
                .ubicacion(subasta.getUbicacion())
                .build();
    }

    private ItemResponse.PolizaInfo buildPolizaInfo(Poliza poliza) {
        if (poliza == null) return null;
        return ItemResponse.PolizaInfo.builder()
                .polizaId(poliza.getId())
                .aseguradoraNombre(poliza.getAseguradoraNombre())
                .aseguradoraContacto(poliza.getAseguradoraContacto())
                .valorAsegurado(poliza.getValorAsegurado())
                .vigenciaDesde(poliza.getVigenciaDesde())
                .vigenciaHasta(poliza.getVigenciaHasta())
                .build();
    }

    private BigDecimal calcularPujaMinima(Item item) {
        return PujaRangeUtil.calcularMinima(item, item.getSubasta());
    }

    private BigDecimal calcularPujaMaxima(Item item) {
        return PujaRangeUtil.calcularMaxima(item, item.getSubasta());
    }

    @GetMapping("/api/v1/items/{itemId}/imagenes")
    public ResponseEntity<List<ItemResponse.ImagenInfo>> obtenerImagenes(@PathVariable Long itemId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item", itemId));
        return ResponseEntity.ok(buildImagenes(item));
    }

    private List<ItemResponse.ImagenInfo> buildImagenes(Item item) {
        if (item.getImagenes() == null) return List.of();
        return item.getImagenes().stream()
                .map(img -> ItemResponse.ImagenInfo.builder()
                        .imagenId(img.getId())
                        .url(img.getUrl())
                        .orden(img.getOrden())
                        .descripcion(img.getDescripcion())
                        .build())
                .collect(Collectors.toList());
    }
}
