package com.subastas.subastas;

import com.subastas.BaseIntegrationTest;
import com.subastas.model.dto.request.ConectarSubastaRequest;
import com.subastas.model.dto.request.PujaRequest;
import com.subastas.model.dto.response.ConectarSubastaResponse;
import com.subastas.model.dto.response.SubastaResponse;
import com.subastas.model.entity.Item;
import com.subastas.model.entity.Subasta;
import com.subastas.model.enums.EstadoSubasta;
import com.subastas.repository.ItemRepository;
import com.subastas.repository.SubastaRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

class SubastaControllerTest extends BaseIntegrationTest {

    @Autowired private SubastaRepository subastaRepository;
    @Autowired private ItemRepository itemRepository;
    @Autowired private com.subastas.repository.UsuarioRepository usuarioRepository;

    private String jwtJuan;
    private String jwtMaria;

    @BeforeEach
    void setUp() {
        jwtJuan = loginAndGetToken("juan@test.com", "password123");
        jwtMaria = loginAndGetToken("maria@test.com", "password123");
    }

    @AfterEach
    void cleanup() {
        // Garantizar que Juan no quede conectado si el test falla antes del desconectar inline
        if (jwtJuan != null) {
            postWithAuth("/api/v1/subastas/1/desconectar", jwtJuan, null, MAP_TYPE);
        }
    }

    // ---- Listar ----

    @Test
    void listar_subastas_requiere_autenticacion() {
        ResponseEntity<Map<String, Object>> res =getNoAuth("/api/v1/subastas", MAP_TYPE);
        assertThat(res.getStatusCode()).isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
    }

    @Test
    void listar_subastas_autenticado_devuelve_resultados() {
        ResponseEntity<SubastaResponse[]> res = getWithAuth("/api/v1/subastas", jwtJuan, SubastaResponse[].class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isNotEmpty();
    }

    // ---- Catálogo público ----

    @Test
    void catalogo_es_publico_sin_jwt() {
        ResponseEntity<Object[]> res = rest.getForEntity("/api/v1/subastas/1/catalogo", Object[].class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isNotEmpty();
    }

    // ---- Conectar / Desconectar ----

    @Test
    void conectar_a_subasta_abierta_exitoso() {
        ConectarSubastaRequest req = new ConectarSubastaRequest();
        req.setMedioPagoId(1L);

        ResponseEntity<ConectarSubastaResponse> res = postWithAuth(
                "/api/v1/subastas/1/conectar", jwtJuan, req, ConectarSubastaResponse.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody().getSesionId()).isNotNull();

        postWithAuth("/api/v1/subastas/1/desconectar", jwtJuan, null, MAP_TYPE);
    }

    @Test
    void conectar_a_dos_subastas_simultaneamente_falla() {
        ConectarSubastaRequest req = new ConectarSubastaRequest();
        req.setMedioPagoId(1L);
        postWithAuth("/api/v1/subastas/1/conectar", jwtJuan, req, ConectarSubastaResponse.class);

        ResponseEntity<Map<String, Object>> res =postWithAuth("/api/v1/subastas/1/conectar", jwtJuan, req, MAP_TYPE);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        postWithAuth("/api/v1/subastas/1/desconectar", jwtJuan, null, MAP_TYPE);
    }

    @Test
    void desconectar_sin_estar_conectado_falla() {
        ResponseEntity<Map<String, Object>> res =postWithAuth("/api/v1/subastas/1/desconectar", jwtMaria, null, MAP_TYPE);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void estado_puja_sin_estar_conectado_falla() {
        ResponseEntity<Map<String, Object>> res =getWithAuth("/api/v1/subastas/1/pujas/estado", jwtMaria, MAP_TYPE);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void obtener_subasta_inexistente_devuelve_404() {
        ResponseEntity<Map<String, Object>> res =getWithAuth("/api/v1/subastas/9999", jwtJuan, MAP_TYPE);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ---- Puja en subasta no ABIERTA (Problema #4 de auditoría) ----
    // FALLA HOY: PujaService no valida subasta.estado → acepta la puja (201).
    // Después del fix debe devolver 4xx.

    @Test
    void pujar_en_subasta_cerrada_debe_ser_rechazado() {
        // Garantizar que Juan no tenga multas residuales de otros tests
        com.subastas.model.entity.Usuario juan = usuarioRepository.findByEmail("juan@test.com").orElseThrow();
        int multasOriginales = juan.getMultasPendientes();
        juan.setMultasPendientes(0);
        usuarioRepository.save(juan);

        ConectarSubastaRequest conectar = new ConectarSubastaRequest();
        conectar.setMedioPagoId(1L);
        postWithAuth("/api/v1/subastas/1/conectar", jwtJuan, conectar, MAP_TYPE);

        Subasta subasta = subastaRepository.findById(1L).orElseThrow();
        EstadoSubasta estadoOriginal = subasta.getEstado();
        subasta.setEstado(EstadoSubasta.CERRADA);
        subastaRepository.save(subasta);

        Item item = itemRepository.findById(1L).orElseThrow();
        BigDecimal mejorOfertaOriginal = item.getMejorOferta();

        try {
            // Monto siempre dentro del rango: baseEfectiva + 1000 (rango es +500 a +10000 sobre precioBase)
            BigDecimal base = item.getMejorOferta() != null ? item.getMejorOferta() : item.getPrecioBase();
            BigDecimal montoValido = base.add(new BigDecimal("1000.00"));

            PujaRequest req = new PujaRequest();
            req.setItemId(1L);
            req.setMonto(montoValido);
            req.setMedioPagoId(1L);

            ResponseEntity<Map<String, Object>> res = postWithAuth(
                    "/api/v1/subastas/1/pujas", jwtJuan, req, MAP_TYPE);

            // BUG: PujaService no verifica subasta.estado → hoy acepta la puja (201)
            assertThat(res.getStatusCode())
                    .as("No se debe poder pujar en una subasta CERRADA")
                    .isNotEqualTo(HttpStatus.CREATED);
        } finally {
            subasta.setEstado(estadoOriginal);
            subastaRepository.save(subasta);
            // Revertir item si el bug creó la puja y modificó mejorOferta
            Item itemActual = itemRepository.findById(1L).orElseThrow();
            if (!Objects.equals(itemActual.getMejorOferta(), mejorOfertaOriginal)) {
                itemActual.setMejorOferta(mejorOfertaOriginal);
                itemActual.setMejorPostor(null);
                itemRepository.save(itemActual);
            }
            // Restaurar contador de multas de Juan
            com.subastas.model.entity.Usuario juanFinal = usuarioRepository.findByEmail("juan@test.com").orElseThrow();
            juanFinal.setMultasPendientes(multasOriginales);
            usuarioRepository.save(juanFinal);
        }
    }
}
