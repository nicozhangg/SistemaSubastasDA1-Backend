package com.subastas.model.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ConectarSubastaResponse {
    private Long sesionId;
    private String streamingUrl;
    private EstadoPujaResponse itemActual;
}
