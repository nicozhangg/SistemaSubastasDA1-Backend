package com.subastas.config;

import com.subastas.model.entity.*;
import com.subastas.model.enums.*;
import com.subastas.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Carga datos de prueba al inicio de la aplicación en todos los perfiles excepto prod.
 * Popula dos usuarios (PLATA y ORO), sus medios de pago, un rematador,
 * una subasta ABIERTA con dos ítems y una subasta PROXIMA en USD.
 */
@Slf4j
@Component
@Profile("!prod")
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final MedioPagoRepository medioPagoRepository;
    private final SubastaRepository subastaRepository;
    private final ItemRepository itemRepository;
    private final RematadorRepository rematadorRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (usuarioRepository.count() > 0) return;

        log.info("Cargando datos de prueba...");

        // Usuarios de prueba
        Usuario postor1 = Usuario.builder()
                .nombre("Juan")
                .apellido("Pérez")
                .email("juan@test.com")
                .password(passwordEncoder.encode("password123"))
                .domicilioLegal("Av. Corrientes 1234, CABA")
                .paisOrigen("Argentina")
                .categoria(Categoria.PLATA)
                .estado(EstadoUsuario.APROBADO)
                .build();
        postor1 = usuarioRepository.save(postor1);

        Usuario postor2 = Usuario.builder()
                .nombre("María")
                .apellido("García")
                .email("maria@test.com")
                .password(passwordEncoder.encode("password123"))
                .domicilioLegal("Palermo 567, CABA")
                .paisOrigen("Argentina")
                .categoria(Categoria.ORO)
                .estado(EstadoUsuario.APROBADO)
                .build();
        postor2 = usuarioRepository.save(postor2);

        // Medios de pago
        MedioPago mp1 = MedioPago.builder()
                .tipo(TipoMedioPago.CUENTA_BANCARIA)
                .alias("Cuenta Principal")
                .moneda(Moneda.ARS)
                .verificado(true)
                .banco("Banco Nación")
                .numeroCuenta("001234567890")
                .tipoCuenta("nacional")
                .cbu("0110001000012345678902")
                .usuario(postor1)
                .build();
        medioPagoRepository.save(mp1);

        MedioPago mp2 = MedioPago.builder()
                .tipo(TipoMedioPago.TARJETA_CREDITO)
                .alias("Visa Personal")
                .moneda(Moneda.USD)
                .verificado(true)
                .numeroTarjeta("4111111111111111")
                .titular("MARIA GARCIA")
                .vencimiento("12/27")
                .tipoTarjeta("internacional")
                .usuario(postor2)
                .build();
        medioPagoRepository.save(mp2);

        // Subasta de prueba - ABIERTA
        Rematador rematador = Rematador.builder()
                .nombre("Carlos")
                .apellido("Martini")
                .email("rematador@subastas.com")
                .matricula("MAT-001")
                .build();
        rematador = rematadorRepository.save(rematador);

        Subasta subasta1 = Subasta.builder()
                .titulo("Subasta de Arte Argentino - Lote 01")
                .descripcion("Colección de obras de artistas contemporáneos argentinos")
                .fechaInicio(LocalDateTime.now().minusHours(1))
                .categoria(Categoria.COMUN)
                .moneda(Moneda.ARS)
                .estado(EstadoSubasta.ABIERTA)
                .ubicacion("Centro Cultural Borges, CABA")
                .rematador(rematador)
                .build();
        subasta1 = subastaRepository.save(subasta1);

        // Ítems
        Item item1 = Item.builder()
                .numeroPieza("P-001")
                .descripcion("Óleo sobre tela - Paisaje pampeano - 80x60cm")
                .precioBase(new BigDecimal("50000.00"))
                .estado(EstadoItem.EN_SUBASTA)
                .duenioActual("Colección Privada")
                .esObraArte(true)
                .artista("Luis Fontana")
                .historia("Obra del período 1990-2000, técnica mixta")
                .subasta(subasta1)
                .build();
        itemRepository.save(item1);

        Item item2 = Item.builder()
                .numeroPieza("P-002")
                .descripcion("Escultura de bronce - Figura abstracta - 30cm")
                .precioBase(new BigDecimal("80000.00"))
                .estado(EstadoItem.DISPONIBLE)
                .duenioActual("Galería Norte")
                .esObraArte(true)
                .artista("Ana Berlotti")
                .subasta(subasta1)
                .build();
        itemRepository.save(item2);

        // Subasta próxima
        Subasta subasta2 = Subasta.builder()
                .titulo("Subasta Especial USD - Colección Internacional")
                .descripcion("Obras y antigüedades de artistas internacionales")
                .fechaInicio(LocalDateTime.now().plusDays(7))
                .categoria(Categoria.ESPECIAL)
                .moneda(Moneda.USD)
                .estado(EstadoSubasta.PROXIMA)
                .ubicacion("Hotel Alvear, CABA")
                .build();
        subastaRepository.save(subasta2);

        log.info("Datos de prueba cargados: 2 usuarios, 2 medios de pago, 2 subastas, 2 ítems");
        log.info("Login de prueba: juan@test.com / password123 (PLATA)");
        log.info("Login de prueba: maria@test.com / password123 (ORO)");
    }
}
