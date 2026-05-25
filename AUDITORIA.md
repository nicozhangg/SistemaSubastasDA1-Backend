# Auditoría del Backend — Sistema de Subastas DA1

**Fecha:** 2026-05-25  
**Alcance:** Todos los archivos Java, SQL, configuración y properties del proyecto  
**Rama auditada:** `backend`  
**Nota:** Esta auditoría refleja el estado actual del código tras las correcciones previas.

---

## PROBLEMAS CRÍTICOS

### 1. WebSocket permite conexiones desde cualquier origen

**Archivo:** `src/main/java/com/subastas/config/WebSocketConfig.java:33`  
**Severidad:** CRÍTICO

```java
registry.addEndpoint("/ws")
        .setAllowedOriginPatterns("*")   // acepta cualquier origen
        .withSockJS();
```

**Qué está mal:** El endpoint WebSocket acepta conexiones desde cualquier dominio, mientras que la configuración HTTP CORS en `SecurityConfig.java:94` restringe correctamente los orígenes a `localhost:3000` y `localhost:4200`. Un atacante puede crear una página en su dominio que abra una conexión WebSocket al servidor usando el JWT de la víctima (Cross-Site WebSocket Hijacking). Como la autenticación WebSocket se hace por token en el frame CONNECT y el navegador envía cookies/tokens del dominio, esto permite pujar con la sesión de otro usuario.

**Cómo resolverlo:** Inyectar los mismos orígenes configurados en `SecurityConfig`:

```java
@Value("${cors.allowed-origins:http://localhost:3000,http://localhost:4200}")
private List<String> allowedOrigins;

// En registerStompEndpoints:
registry.addEndpoint("/ws")
        .setAllowedOrigins(allowedOrigins.toArray(new String[0]))
        .withSockJS();
```

---

### 2. JWT secret hardcodeado como valor por defecto

**Archivo:** `src/main/resources/application.properties:17`  
**Severidad:** CRÍTICO

```properties
jwt.secret=${JWT_SECRET:dGhpcy1pcy1hLXZlcnktbG9uZy1zZWNyZXQta2V5LWZvci1zdWJhc3Rhcy1zeXN0ZW0tZGExLTIwMjY=}
```

**Qué está mal:** Si la variable de entorno `JWT_SECRET` no está definida, se usa el secreto visible en el código fuente. El valor Base64 decodifica a `this-is-a-very-long-secret-key-for-subastas-system-da1-2026`. Cualquiera que lea este archivo puede firmar tokens JWT válidos y suplantar a cualquier usuario del sistema.

**Cómo resolverlo:** Eliminar el valor por defecto para que la aplicación falle al iniciar si no se configura:

```properties
jwt.secret=${JWT_SECRET}
```

---

### 3. WebSocket controller pierde los mensajes de error de negocio

**Archivo:** `src/main/java/com/subastas/controller/PujaWebSocketController.java:61-66`  
**Severidad:** CRÍTICO

```java
} catch (Exception e) {
    log.error("Error procesando puja WebSocket de {} en subasta {}: {}", email, subastaId, e.getMessage(), e);
    webSocketService.sendBidRejected(email, BidRejectedMessage.builder()
            .motivo("ERROR_INTERNO")
            .mensaje("No se pudo procesar la puja")
            .build());
}
```

**Qué está mal:** Todos los `BusinessException` que lanza `PujaService.realizarPuja()` son atrapados por el `catch (Exception e)` genérico. Esto incluye errores con feedback crucial para el usuario:
- `MONTO_FUERA_DE_RANGO` → "El monto debe estar entre X y Y"
- `PUJA_EN_PROCESO` → "Hay una puja en proceso"
- `MULTA_PENDIENTE` → "Tenés multas pendientes"
- `USUARIO_NO_CONECTADO` → "No estás conectado a esta subasta"
- `ESTADO_INVALIDO` → "La moneda del medio de pago no coincide"

En todos estos casos el cliente recibe siempre "No se pudo procesar la puja" sin saber por qué. El flujo REST sí devuelve el error correcto gracias al `GlobalExceptionHandler`, pero el flujo WebSocket (que es el principal para pujas en tiempo real) deja al usuario sin feedback útil.

**Cómo resolverlo:**

```java
} catch (BusinessException e) {
    webSocketService.sendBidRejected(email, BidRejectedMessage.builder()
            .motivo(e.getCodigo())
            .mensaje(e.getMessage())
            .build());
} catch (Exception e) {
    log.error("Error inesperado procesando puja WebSocket...", email, subastaId, e.getMessage(), e);
    webSocketService.sendBidRejected(email, BidRejectedMessage.builder()
            .motivo("ERROR_INTERNO")
            .mensaje("No se pudo procesar la puja")
            .build());
}
```

---

### 4. Falta validación de moneda al conectar a subasta

**Archivo:** `src/main/java/com/subastas/service/SubastaService.java:108-114`  
**Severidad:** CRÍTICO

```java
MedioPago medioPago = medioPagoRepository.findByIdAndUsuario(request.getMedioPagoId(), usuario)
        .orElseThrow(() -> new ResourceNotFoundException("Medio de pago", request.getMedioPagoId()));

if (!medioPago.isVerificado()) {
    throw new BusinessException(ErrorCodes.SIN_MEDIO_PAGO_VERIFICADO,
            "El medio de pago no está verificado", HttpStatus.FORBIDDEN);
}
// No se valida que medioPago.getMoneda() == subasta.getMoneda()
```

**Qué está mal:** Al conectarse a una subasta, se valida que el medio de pago exista y esté verificado, pero no se valida que su moneda coincida con la de la subasta. Un usuario puede conectarse a una subasta en USD con un medio de pago en ARS. La validación de moneda sí existe al momento de pujar (`PujaService.java:98-101`), pero el usuario descubre el error recién cuando intenta ofertar.

**Cómo resolverlo:** Agregar después de la validación de verificado:

```java
if (subasta.getMoneda() != medioPago.getMoneda()) {
    throw new BusinessException(ErrorCodes.ESTADO_INVALIDO,
            "La moneda del medio de pago no coincide con la moneda de la subasta");
}
```

---

### 5. Email de registro falla silenciosamente sin mecanismo de recuperación

**Archivo:** `src/main/java/com/subastas/service/EmailService.java:36-38`  
**Severidad:** CRÍTICO

```java
} catch (Exception e) {
    log.error("Error enviando email a {}: {}", emailDestino, e.getMessage());
    // excepción tragada — el usuario nunca recibe el token
}
```

**Qué está mal:** Si el envío del email con el token de registro falla (servidor SMTP caído, error de red, timeout), la excepción se loguea pero se traga. El usuario queda en estado `PENDIENTE_VERIFICACION` sin forma de avanzar al paso 2 porque nunca recibe el token. No hay endpoint de reenvío ni mecanismo de reintento.

El mismo problema existe en `enviarNotificacionGanador()` (línea 57): si falla, el ganador de la subasta nunca se entera de que ganó.

**Cómo resolverlo:**
1. Propagar la excepción o registrar el intento fallido en BD para retry.
2. Agregar un endpoint `POST /api/v1/auth/reenviar-token` que regenere el token y reenvíe el email.
3. Considerar un sistema de reintentos con backoff (Spring Retry).

---

## PROBLEMAS IMPORTANTES

### 6. Comisiones y costo de envío siempre en cero

**Archivo:** `src/main/java/com/subastas/service/SubastaService.java:225-227`  
**Severidad:** IMPORTANTE

```java
Compra compra = Compra.builder()
        .item(item)
        .usuario(item.getMejorPostor())
        .montoOfertado(item.getMejorOferta())
        .comisiones(java.math.BigDecimal.ZERO)   // siempre 0
        .costoEnvio(java.math.BigDecimal.ZERO)   // siempre 0
        .total(item.getMejorOferta())             // total = oferta sin comisiones
```

**Qué está mal:** Al cerrar una subasta y crear las compras, las comisiones y el costo de envío son siempre cero. El sistema no genera ingresos por comisión. La tabla `compras` tiene las columnas preparadas pero nunca se llenan con valores reales.

**Cómo resolverlo:** Implementar un servicio de cálculo de comisiones basado en el tipo de subasta, la categoría del usuario y el monto de la oferta. Calcular el envío según la ubicación del depósito.

---

### 7. Memory leak en el mapa de locks por subasta

**Archivo:** `src/main/java/com/subastas/service/PujaService.java:51`  
**Severidad:** IMPORTANTE

```java
private final Map<Long, ReentrantLock> locksPorSubasta = new ConcurrentHashMap<>();
```

**Qué está mal:** Cada subasta que recibe al menos una puja crea una entrada permanente en el `ConcurrentHashMap`. La limpieza solo ocurre en `liberarLock()` (línea 160-162), llamado desde `SubastaService.cerrarSubasta():272`. Si una subasta recibe pujas pero nunca se cierra correctamente (error en el scheduler, excepción en el cierre), su lock queda en memoria para siempre. En un sistema con miles de subastas a lo largo de meses, esto acumula objetos `ReentrantLock`.

**Cómo resolverlo:** Usar un cache con expiración automática (Caffeine con `expireAfterAccess(1, TimeUnit.HOURS)`) en lugar del `ConcurrentHashMap` plano. Alternativamente, agregar una tarea programada que limpie locks de subastas ya cerradas.

---

### 8. Refresh token generado pero sin endpoint para usarlo

**Archivo:** `src/main/java/com/subastas/service/AuthService.java:176`  
**Severidad:** IMPORTANTE

```java
.tokenRefresh(jwtUtil.generateRefreshToken(request.getEmail()))
```

**Qué está mal:** El login genera un refresh token (con 7 días de expiración, `application.properties:19`) y lo envía al cliente en la respuesta. Pero no existe ningún endpoint `POST /api/v1/auth/refresh` para consumirlo. El método `generateRefreshToken()` existe en `JwtUtil.java:48-55` y funciona correctamente, pero del lado del servidor no hay forma de intercambiar un refresh token por un nuevo access token. Es código muerto funcional: se genera, se envía, pero nunca se puede usar. El usuario debe hacer login completo cada 24 horas.

**Cómo resolverlo:** Implementar un endpoint de refresh en `AuthController`:

```java
@PostMapping("/api/v1/auth/refresh")
public ResponseEntity<Map<String, String>> refresh(@RequestBody RefreshTokenRequest request) {
    // Validar refresh token, extraer email, generar nuevo access token
}
```

Y agregar la ruta a los endpoints públicos en `SecurityConfig`.

---

### 9. Historial de pujas sin paginación en 3 de 4 ramas

**Archivo:** `src/main/java/com/subastas/service/PujaService.java:170-184`  
**Severidad:** IMPORTANTE

```java
if (Boolean.TRUE.equals(soloPropias)) {
    if (itemId != null) {
        pujas = pujaRepository.findBySubastaAndItemAndUsuarioOrderByTimestampDesc(subasta, item, usuario); // sin paginar
    } else {
        pujas = pujaRepository.findBySubastaAndUsuarioOrderByTimestampDesc(subasta, usuario);               // sin paginar
    }
} else if (itemId != null) {
    pujas = pujaRepository.findBySubastaAndItemOrderByTimestampDesc(subasta, item);                         // sin paginar
} else {
    pujas = pujaRepository.findBySubastaOrderByTimestampDesc(subasta, PageRequest.of(page, size));           // OK
}
```

**Qué está mal:** Solo la última rama (todas las pujas de una subasta sin filtro) usa paginación. Las otras tres devuelven TODOS los resultados sin límite. En una subasta activa con miles de pujas, estas consultas cargan todo en memoria y lo serializan completo en la respuesta HTTP. El endpoint acepta parámetros `page` y `size` pero se ignoran en 3 de los 4 casos.

**Cómo resolverlo:** Agregar `Pageable` a los tres métodos del repositorio que faltan (`PujaRepository.java:30-32`) y pasar `PageRequest.of(page, size)` en cada rama.

---

### 10. eliminarMedioPago bloquea eliminación de medios no relacionados

**Archivo:** `src/main/java/com/subastas/service/UsuarioService.java:92-98`  
**Severidad:** IMPORTANTE

```java
boolean enUso = participacionRepository.existsByUsuarioAndConectadoTrue(usuario);
if (enUso) {
    throw new BusinessException(ErrorCodes.MEDIO_PAGO_EN_USO,
            "No podés eliminar un medio de pago mientras estás conectado a una subasta",
            HttpStatus.CONFLICT);
}
```

**Qué está mal:** La validación verifica si el usuario está conectado a **cualquier** subasta, no si el medio de pago **específico** que quiere eliminar es el que está en uso. Si un usuario tiene medio de pago A (en uso en una subasta activa) y medio de pago B (libre), no puede eliminar B mientras esté conectado, a pesar de que B no tiene relación con la participación activa.

**Cómo resolverlo:** Cambiar la query para verificar el medio de pago específico:

```java
boolean enUso = participacionRepository.existsByMedioPagoAndConectadoTrue(medioPago);
```

Y agregar el método correspondiente en `ParticipacionRepository`.

---

### 11. Race condition en cache de multasPendientes

**Archivo:** `src/main/java/com/subastas/service/MultaService.java:59-61`  
**Severidad:** IMPORTANTE

```java
long pendientes = multaRepository.countByUsuarioAndEstado(usuario, EstadoMulta.PENDIENTE);
usuario.setMultasPendientes((int) pendientes);
usuarioRepository.save(usuario);
```

**Qué está mal:** Hay una ventana de race condition entre el `count` y el `save`:
1. Si se generan multas nuevas entre ambas líneas, el conteo queda desactualizado.
2. Si dos multas se pagan simultáneamente en transacciones distintas, ambas cuentan el mismo valor y una sobreescribe el resultado de la otra (lost update).
3. El campo `multasPendientes` en `Usuario` (línea 63-65) es un cache manual que puede quedar inconsistente con la tabla `multas`.

**Cómo resolverlo:** Usar una query atómica: `UPDATE usuarios SET multas_pendientes = (SELECT COUNT(*) FROM multas WHERE usuario_id = ? AND estado = 'PENDIENTE') WHERE id = ?`. O eliminar el cache y consultar siempre la tabla `multas` directamente (el índice `idx_multas_usuario_estado` en `EstructuraActual.sql` ya existe para esto).

---

### 12. H2 console protegida con ROLE_ADMIN que nunca se asigna

**Archivo:** `src/main/java/com/subastas/config/SecurityConfig.java:58` + `src/main/java/com/subastas/security/UserDetailsServiceImpl.java:37`  
**Severidad:** IMPORTANTE

```java
// SecurityConfig.java:58
.requestMatchers("/h2-console/**").hasRole("ADMIN")

// UserDetailsServiceImpl.java:37
.authorities(List.of(new SimpleGrantedAuthority("ROLE_USER")))  // siempre ROLE_USER
```

**Qué está mal:** La consola H2 requiere `ROLE_ADMIN`, pero `UserDetailsServiceImpl` asigna `ROLE_USER` a todos los usuarios sin excepción. Nadie puede acceder. Esto no es un riesgo de seguridad (la consola queda efectivamente bloqueada), pero indica una intención incompleta de tener roles administrativos. En producción, la consola H2 debería estar deshabilitada completamente, no dependiendo de un rol inexistente.

**Cómo resolverlo:** Para desarrollo, cambiar a `permitAll()` condicionalmente con perfil, o mejor, crear un perfil donde se deshabilite la consola. Para producción, confirmar que `spring.h2.console.enabled=false`.

---

### 13. Fotos de DNI sin validación de tipo MIME

**Archivo:** `src/main/java/com/subastas/service/AuthService.java:139-150`  
**Severidad:** IMPORTANTE

```java
private String guardarArchivoDni(MultipartFile archivo) {
    if (archivo == null || archivo.isEmpty()) return null;
    try {
        String nombreArchivo = "uploads/dni/" + FileUtil.uuidFilename(archivo);
        Path destino = Paths.get(nombreArchivo);
        Files.createDirectories(destino.getParent());
        archivo.transferTo(destino.toFile());       // guarda sin validar tipo
        return nombreArchivo;
    } catch (IOException e) {
        throw new RuntimeException("Error al guardar archivo de DNI", e);
    }
}
```

**Qué está mal:** A diferencia del servicio de consignaciones (`ConsignacionService.java:66-78`) que valida el tipo MIME con Apache Tika (magic bytes), el guardado de fotos de DNI no hace ninguna validación de contenido. Un usuario podría subir un archivo ejecutable, un HTML con JavaScript, o cualquier tipo de archivo disfrazado como foto de DNI. La extensión del archivo se preserva en `FileUtil.uuidFilename()` (línea 17), pero solo se extrae del nombre original sin verificar el contenido real.

**Cómo resolverlo:** Aplicar la misma validación con Tika que ya existe en `ConsignacionService`:

```java
private static final Tika TIKA = new Tika();

private String guardarArchivoDni(MultipartFile archivo) {
    if (archivo == null || archivo.isEmpty()) return null;
    try {
        String tipoDetectado = TIKA.detect(archivo.getBytes());
        if (!tipoDetectado.startsWith("image/")) {
            throw new BusinessException(ErrorCodes.ESTADO_INVALIDO, "Solo se permiten imágenes para el DNI");
        }
        // ... resto del guardado
    }
}
```

---

## PROBLEMAS MENORES

### 14. Sin límite máximo de fotos en consignación

**Archivo:** `src/main/java/com/subastas/service/ConsignacionService.java:61`  
**Severidad:** MENOR

```java
if (fotos == null || fotos.size() < 6) {
    throw new BusinessException(ErrorCodes.FOTOS_INSUFICIENTES, "Debés subir al menos 6 fotos del bien");
}
```

**Qué está mal:** Se valida un mínimo de 6 fotos pero no hay máximo. Un usuario podría enviar cientos de fotos. Combinado con el límite de 5 MB por foto (línea 79) y los `foto.getBytes()` que cargan todo en memoria (línea 68), un request con 50 fotos de 5 MB cada una consumiría ~250 MB de heap solo para la validación MIME. El límite de `max-request-size=50MB` en `application.properties:31` mitiga parcialmente, pero no impide enviar muchas fotos pequeñas.

**Cómo resolverlo:** Agregar un límite superior razonable:

```java
if (fotos == null || fotos.size() < 6 || fotos.size() > 20) {
    throw new BusinessException(ErrorCodes.FOTOS_INSUFICIENTES, "Debés subir entre 6 y 20 fotos del bien");
}
```

---

### 15. Rutas relativas para almacenamiento de archivos

**Archivo:** `src/main/java/com/subastas/service/AuthService.java:142` y `src/main/java/com/subastas/service/ConsignacionService.java:105`  
**Severidad:** MENOR

```java
// AuthService.java:142
String nombreArchivo = "uploads/dni/" + FileUtil.uuidFilename(archivo);

// ConsignacionService.java:105
String urlRelativa = "uploads/consignaciones/" + consignacion.getId() + "/" + nombreArchivo;
```

**Qué está mal:** Ambos servicios usan rutas relativas. La ubicación real depende del directorio de trabajo del proceso Java (`user.dir`), que varía entre ejecuciones, IDEs y servidores de aplicaciones. Si la aplicación se reinicia desde otro directorio o se deploya en un contenedor, los archivos previos se vuelven inaccesibles. Las rutas guardadas en la BD apuntan a archivos que ya no se encuentran.

**Cómo resolverlo:** Usar una ruta absoluta configurable:

```properties
app.uploads.base-path=${UPLOADS_PATH:/var/data/subastas/uploads}
```

```java
@Value("${app.uploads.base-path}")
private String uploadsBasePath;
```

---

### 16. Métricas calculadas en memoria en vez de SQL

**Archivo:** `src/main/java/com/subastas/service/UsuarioService.java:116-129`  
**Severidad:** MENOR

```java
var participaciones = participacionRepository.findByUsuarioOrderByFechaConexionDesc(usuario);
var compras = compraRepository.findByUsuarioOrderByIdDesc(usuario);
BigDecimal totalPagado = compras.stream()
        .map(c -> c.getTotal() != null ? c.getTotal() : BigDecimal.ZERO)
        .reduce(BigDecimal.ZERO, BigDecimal::add);

return MetricasResponse.builder()
        .totalSubastasAsistidas(participaciones.size())  // carga TODAS las participaciones para hacer .size()
        .totalGanadas(compras.size())                    // carga TODAS las compras para hacer .size()
        .totalPagado(totalPagado)                        // suma en memoria
```

**Qué está mal:** Para calcular 3 números (count participaciones, count compras, sum total), se cargan TODAS las entidades completas de ambas tablas en memoria. Con un usuario activo que tenga cientos de participaciones y compras, esto genera queries con decenas de filas que se hidratan a objetos Java solo para descartarlos.

**Cómo resolverlo:** Usar queries de agregación en los repositorios:

```java
@Query("SELECT COUNT(p) FROM Participacion p WHERE p.usuario = :usuario")
long countByUsuario(@Param("usuario") Usuario usuario);

@Query("SELECT COALESCE(SUM(c.total), 0) FROM Compra c WHERE c.usuario = :usuario")
BigDecimal sumTotalByUsuario(@Param("usuario") Usuario usuario);
```

---

### 17. Alias parcial filtra parte del nombre real del postor

**Archivo:** `src/main/java/com/subastas/util/AliasUtil.java:9-14`  
**Severidad:** MENOR

```java
public static String generarAlias(Usuario usuario) {
    if (usuario == null) return null;
    String nombre = usuario.getNombre();
    if (nombre == null || nombre.isBlank()) return "postor_***";
    return "postor_" + nombre.substring(0, Math.min(3, nombre.length())) + "***";
}
```

**Qué está mal:** El alias expone las primeras 3 letras del nombre del postor (ej. "postor_Jua***" para Juan). En subastas con pocos participantes, esto puede ser suficiente para identificar a una persona, especialmente si los participantes se conocen entre sí. El propósito del alias es mantener anonimato.

**Cómo resolverlo:** Usar un identificador sin relación con datos personales:

```java
return "postor_" + String.format("%04x", Math.abs(usuario.getId().hashCode() % 0xFFFF));
```

---

### 18. NullPointerException posible en PujaRangeUtil si precioBase es null

**Archivo:** `src/main/java/com/subastas/util/PujaRangeUtil.java:16-24`  
**Severidad:** MENOR

```java
public static BigDecimal calcularMinima(Item item, Subasta subasta) {
    if (subasta == null || subasta.getCategoria().sinLimitesPuja()) return null;
    return baseEfectiva(item).add(item.getPrecioBase().multiply(FACTOR_MIN));
    //                              ^^^^^^^^^^^^^^^^^^^^^ NPE si precioBase es null
}
```

**Qué está mal:** Si `item.getPrecioBase()` fuera `null`, se lanzaría un `NullPointerException`. Aunque el campo está marcado como `nullable = false` en la entidad (`Item.java:34`), no hay protección defensiva. En una migración de datos incorrecta o un bug en la carga de ítems, esto haría que todas las pujas a ese ítem fallen con un 500 sin mensaje útil.

**Cómo resolverlo:** Agregar un null check al inicio:

```java
if (item.getPrecioBase() == null) return null;
```

---

### 19. cerrarSubasta envía emails sincrónicamente en un loop

**Archivo:** `src/main/java/com/subastas/service/SubastaService.java:237-242`  
**Severidad:** MENOR

```java
for (Item item : items) {
    if (item.getEstado() != EstadoItem.EN_SUBASTA) continue;
    if (item.getMejorPostor() != null) {
        // ... crear Compra, guardar Item ...
        emailService.enviarNotificacionGanador(          // síncrono, bloquea el hilo
                item.getMejorPostor().getEmail(),
                item.getMejorPostor().getNombre(),
                item.getDescripcion(),
                desglose
        );
        webSocketService.broadcastAuctionClosed(...);
    }
}
```

**Qué está mal:** Al cerrar una subasta, se itera sobre cada ítem y se envía un email sincrónicamente por cada ganador. Si la subasta tiene 50 ítems vendidos, son 50 llamadas secuenciales al servidor SMTP dentro de una sola transacción `@Transactional`. Cada llamada tiene latencia de red y puede fallar. Esto retrasa el cierre completo, bloquea el hilo del scheduler, y si un email falla a mitad del loop (el `catch` en `EmailService` lo previene, pero si hubiera otra excepción), podría hacer rollback de toda la transacción incluyendo las compras ya creadas.

**Cómo resolverlo:** Marcar `enviarNotificacionGanador()` como `@Async` o usar eventos `@TransactionalEventListener(phase = AFTER_COMMIT)` como ya se hace para las pujas confirmadas. Esto desacopla el envío de emails del cierre transaccional.

---

## TOP 5 — Problemas más urgentes antes de producción

| # | Problema | Archivo | Severidad | Por qué es urgente |
|---|----------|---------|-----------|-------------------|
| 1 | **JWT secret hardcodeado como fallback** | `application.properties:17` | CRÍTICO | Cualquiera que vea el código fuente puede firmar tokens JWT válidos y suplantar a cualquier usuario del sistema. Si el repo es público, el sistema ya está comprometido. |
| 2 | **WebSocket CORS acepta cualquier origen** | `WebSocketConfig.java:33` | CRÍTICO | Permite Cross-Site WebSocket Hijacking: un sitio malicioso puede ejecutar pujas usando la sesión autenticada de la víctima sin que esta lo sepa. |
| 3 | **WebSocket pierde mensajes de error específicos** | `PujaWebSocketController.java:61-66` | CRÍTICO | Los usuarios que pujan por WebSocket (el flujo principal en tiempo real) nunca reciben feedback sobre por qué su puja fue rechazada. Todos los errores de negocio se reemplazan con un genérico "No se pudo procesar la puja". |
| 4 | **Email de registro falla sin recuperación** | `EmailService.java:36-38` | CRÍTICO | Un fallo del SMTP deja usuarios permanentemente bloqueados en PENDIENTE_VERIFICACION sin forma de completar el registro. No hay endpoint de reenvío ni mecanismo de retry. |
| 5 | **Falta validación de moneda al conectar** | `SubastaService.java:108-114` | CRÍTICO | Un usuario puede conectarse a una subasta USD con un medio de pago ARS y recién enterarse del problema al intentar pujar, generando frustración y pérdida de oportunidades de puja. |
