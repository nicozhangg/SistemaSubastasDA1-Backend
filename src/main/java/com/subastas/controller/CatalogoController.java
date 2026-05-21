package com.subastas.controller;

import com.subastas.exception.ResourceNotFoundException;
import com.subastas.model.dto.response.ItemResponse;
import com.subastas.model.entity.Item;
import com.subastas.repository.ItemRepository;
import com.subastas.service.SubastaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

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
                .map(item -> ItemResponse.builder()
                        .itemId(item.getId())
                        .numeroPieza(item.getNumeroPieza())
                        .descripcion(item.getDescripcion())
                        .precioBase(autenticado ? item.getPrecioBase() : null)
                        .estado(item.getEstado())
                        .esObraArte(item.isEsObraArte())
                        .imagenes(item.getImagenes() != null
                                ? item.getImagenes().stream()
                                    .map(img -> ItemResponse.ImagenInfo.builder()
                                            .imagenId(img.getId())
                                            .url(img.getUrl())
                                            .orden(img.getOrden())
                                            .descripcion(img.getDescripcion())
                                            .build())
                                    .collect(Collectors.toList())
                                : List.of())
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/v1/items/{itemId}")
    public ResponseEntity<ItemResponse> obtenerItem(@PathVariable Long itemId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item", itemId));

        ItemResponse response = ItemResponse.builder()
                .itemId(item.getId())
                .numeroPieza(item.getNumeroPieza())
                .descripcion(item.getDescripcion())
                .precioBase(item.getPrecioBase())
                .estado(item.getEstado())
                .duenioActual(item.getDuenioActual())
                .esObraArte(item.isEsObraArte())
                .artista(item.getArtista())
                .fechaCreacion(item.getFechaCreacion())
                .historia(item.getHistoria())
                .componentes(item.getComponentes())
                .imagenes(item.getImagenes() != null
                        ? item.getImagenes().stream()
                            .map(img -> ItemResponse.ImagenInfo.builder()
                                    .imagenId(img.getId())
                                    .url(img.getUrl())
                                    .orden(img.getOrden())
                                    .descripcion(img.getDescripcion())
                                    .build())
                            .collect(Collectors.toList())
                        : List.of())
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/v1/items/{itemId}/imagenes")
    public ResponseEntity<List<ItemResponse.ImagenInfo>> obtenerImagenes(@PathVariable Long itemId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item", itemId));

        List<ItemResponse.ImagenInfo> imagenes = item.getImagenes() != null
                ? item.getImagenes().stream()
                    .map(img -> ItemResponse.ImagenInfo.builder()
                            .imagenId(img.getId())
                            .url(img.getUrl())
                            .orden(img.getOrden())
                            .descripcion(img.getDescripcion())
                            .build())
                    .collect(Collectors.toList())
                : List.of();

        return ResponseEntity.ok(imagenes);
    }
}
