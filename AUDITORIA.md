# Auditoría — Backend Sistema de Subastas DA1

> Perspectiva: senior developer preparando para producción.
> No se modificó ningún archivo durante esta auditoría.

---

## Estado de implementación

| Ítem | Severidad | Estado |
|------|-----------|--------|
| tokenRefresh real con expiración propia | CRÍTICO | ✅ Implementado |
| Secreto JWT hardcodeado | CRÍTICO | ✅ Implementado |
| Password de BD en prod hardcodeada | CRÍTICO | ✅ Implementado |
| Archivos de DNI nunca guardados en disco | CRÍTICO | ✅ Implementado |
| Sin mecanismo de cierre de subasta | CRÍTICO | ✅ Implementado |
| Orden validaciones en login (estado antes de contraseña) | IMPORTANTE | ✅ Implementado |
| `conectar()` devuelve ítem en posición 0 en vez de EN_SUBASTA | IMPORTANTE | ✅ Implementado |
| Pujas WebSocket sin validaciones `@Valid` | IMPORTANTE | ✅ Implementado |
| Email enviado antes del commit de transacción | IMPORTANTE | ✅ Implementado |
| WebSocket broadcast antes del commit de transacción | IMPORTANTE | ✅ Implementado |
| H2 Console sin autenticación | IMPORTANTE | ✅ Implementado |
| Validación MIME por Content-Type del cliente | IMPORTANTE | ✅ Implementado |
| Sin límite de tamaño en multipart | IMPORTANTE | ✅ Implementado |
| N+1 en `items.size()` por subasta | IMPORTANTE | ✅ Implementado |
| Query innecesaria a multas en cada puja | IMPORTANTE | ✅ Implementado |
| N+1 en `GET /items/{id}` | IMPORTANTE | ✅ Implementado |
| Historial de pujas sin paginación | IMPORTANTE | ✅ Implementado |
| `ConsignacionService`: retornos `Object` / `Map` | IMPORTANTE | ✅ Implementado |
| `UsuarioService`: retornos `Map<String,Object>` | IMPORTANTE | ✅ Implementado |
| Sin configuración CORS | IMPORTANTE | ✅ Implementado |
| `EstructuraActual.sql` desactualizado | IMPORTANTE | ✅ Implementado |
| `aceptaPertenencia` hardcodeado a `true` | MENOR | ✅ Implementado |
| Código de error `TOKEN_INVALIDO` para registro incompleto | MENOR | ✅ Implementado |
| `sendBidRejected` disparado en llamadas REST | MENOR | ✅ Implementado |
| Mensaje de excepción interna expuesto al cliente WS | MENOR | ✅ Implementado |
| Token WS inválido no rechaza la conexión | MENOR | ✅ Implementado |
| Credenciales en log nivel INFO | MENOR | ✅ Implementado |
| `generarAlias()` en service incorrecto | MENOR | ✅ Implementado |
| `CompraService` lazy load innecesario en pertenencia | MENOR | ✅ Implementado |
| `EstadoPuja.PENDIENTE` nunca asignado | MENOR | ✅ Implementado |
| Métodos de repositorio sin invocaciones | MENOR | ✅ Implementado |
| `locksPorSubasta` crece indefinidamente | MENOR | ✅ Implementado (`liberarLock` en cierre) |
| Rate limiting en endpoints de auth | IMPORTANTE | ⏸ Diferido |
| Patrón de resolución de usuario inconsistente | IMPORTANTE | ⏸ Diferido |
| Multas nunca generadas automáticamente | IMPORTANTE | ⏸ Diferido |

---

## 1. Bugs y errores de lógica

### `AuthService.java:149–153` — CRÍTICO
**tokenAcceso y tokenRefresh son el mismo token**

```java
String jwt = jwtUtil.generateToken(request.getEmail());  // token A

return LoginResponse.builder()
        .tokenAcceso(jwt)
        .tokenRefresh(jwtUtil.generateToken(request.getEmail()))  // token B — idéntico
```

`JwtUtil` tiene un solo `@Value("${jwt.expiration}")`. La propiedad `jwt.refresh-expiration=604800000` definida en `application.properties:19` **nunca es inyectada** en ningún archivo Java. Ambos tokens tienen el mismo subject, la misma firma y el mismo vencimiento. El cliente cree que tiene un refresh de 7 días cuando en realidad tiene dos tokens de 24 hs idénticos.

**Corrección:** agregar `@Value("${jwt.refresh-expiration}") private long refreshExpiration;` en `JwtUtil` y un método `generateRefreshToken(String email)` con esa expiración.

---

### `AuthService.java:131–143` — IMPORTANTE
**Estado de la cuenta revelado antes de verificar la contraseña**

```java
Usuario usuario = usuarioRepository.findByEmail(request.getEmail())...

if (estado == BLOQUEADO)              → "La cuenta está bloqueada"     // 403
if (estado == PENDIENTE_VERIFICACION) → "Debés completar el registro"  // 403

authenticationManager.authenticate(...)  // recién acá se valida la contraseña
```

Con contraseña incorrecta: si el email existe pero está bloqueado, el sistema devuelve `USUARIO_BLOQUEADO` antes de verificar credenciales. Un atacante puede enumerar emails válidos y sus estados sin conocer la contraseña.

**Corrección:** llamar a `authenticationManager.authenticate()` primero. Si la contraseña es incorrecta lanza `BadCredentialsException` (ya manejado). Solo verificar el estado después de autenticar con éxito.

---

### `AuthService.java:141` — MENOR
**Código de error semánticamente incorrecto**

```java
throw new BusinessException(ErrorCodes.TOKEN_INVALIDO,
        "Debés completar el registro primero", HttpStatus.FORBIDDEN);
```

`TOKEN_INVALIDO` no describe "registro incompleto". Un frontend que evalúa el código de error mostrará el mensaje equivocado.

**Corrección:** agregar `ErrorCodes.REGISTRO_INCOMPLETO = "REGISTRO_INCOMPLETO"`.

---

### `SubastaService.java:117–118` — IMPORTANTE
**`conectar()` devuelve el ítem en posición 0, no el ítem EN_SUBASTA**

```java
List<Item> items = itemRepository.findBySubasta(subasta);
EstadoPujaResponse itemActual = items.isEmpty() ? null : buildEstadoPuja(items.get(0), subasta);
```

`obtenerEstadoPuja()` en la misma clase (línea 160–163) sí filtra por `EstadoItem.EN_SUBASTA`. El ítem que devuelve `conectar()` al usuario recién conectado puede ser el incorrecto si el EN_SUBASTA no está en índice 0.

**Corrección:** aplicar el mismo `.filter(i -> i.getEstado() == EstadoItem.EN_SUBASTA).findFirst()` que usa `obtenerEstadoPuja()`.

---

### `PujaWebSocketController.java:44–48` — IMPORTANTE
**Las pujas por WebSocket evitan todas las validaciones `@Valid`**

```java
PujaRequest request = new PujaRequest();
request.setItemId(wsRequest.getItemId());      // puede ser null
request.setMonto(wsRequest.getMonto());        // puede ser null
request.setMedioPagoId(wsRequest.getMedioPagoId()); // puede ser null
```

`PujaWebSocketRequest` no tiene `@NotNull`, `@Positive` ni `@DecimalMin`. Un cliente WebSocket puede enviar `monto: null` y el sistema lanza `NullPointerException` en `PujaRangeUtil.calcularMinima()`.

**Corrección:** validar manualmente los campos en `PujaWebSocketController` antes de construir el `PujaRequest`, o unificar en un único DTO con validaciones.

---

### `PujaService.java:74–81` y `:113–117` — MENOR
**`sendBidRejected()` se dispara siempre, incluso en llamadas REST**

```java
if (!lock.tryLock()) {
    webSocketService.sendBidRejected(email, ...);  // mensaje WS emitido
    throw new BusinessException(...);              // error HTTP también lanzado
}
```

Cuando la llamada llega por REST el cliente no tiene suscripción a `/user/queue/pujas`, el mensaje se pierde silenciosamente pero se genera tráfico innecesario. El `PujaWebSocketController` ya envía el rechazo desde el `catch`.

**Corrección:** mover el `sendBidRejected` al `PujaWebSocketController`; el service solo lanza la excepción.

---

### `MockVerificacionService.java` (invocado en `AuthService.java:86`) — IMPORTANTE
**Email de activación enviado aunque la transacción principal haga rollback**

```java
// AuthService.registroPaso1() — método @Transactional
usuario = usuarioRepository.save(usuario);
mockVerificacionService.verificarYEnviarEmail(usuario.getId(), token); // @Async: se despacha ahora
// → transacción todavía no commitó
```

El método `@Async` se encola dentro de la transacción abierta. Si ocurre un error después del dispatch y la transacción hace rollback, el usuario no queda persistido pero el email con el token ya fue enviado.

**Corrección:** usar `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` para publicar el evento de email solo cuando la transacción commitó exitosamente.

---

### `PujaService.java:143–153` — IMPORTANTE
**Mensajes WebSocket enviados dentro de la transacción, antes del commit**

```java
@Transactional
public PujaResponse realizarPuja(...) {
    return procesarPuja(...);  // la transacción commitea al retornar este método
}

private PujaResponse procesarPuja(...) {
    pujaRepository.save(puja);
    itemRepository.save(item);
    webSocketService.broadcastBidUpdated(...);  // ← broadcast ANTES del commit
    webSocketService.sendBidConfirmed(...);     // ← ídem
}
```

Si la BD falla al hacer el commit después de que los mensajes ya salieron por WebSocket, todos los clientes tienen información de una puja que nunca existió.

**Corrección:** mismo patrón que el punto anterior: `@TransactionalEventListener(AFTER_COMMIT)`.

---

### `ConsignacionService.java:78` — MENOR
**`aceptaPertenencia` hardcodeado en el builder**

```java
Consignacion consignacion = Consignacion.builder()
        .aceptaPertenencia(true)   // ← siempre true, ignora el parámetro recibido
```

El parámetro se valida arriba pero no se usa al construir el objeto.

**Corrección:** `.aceptaPertenencia(aceptaPertenencia)`.

---

## 2. Código muerto

### `application.properties:19` — IMPORTANTE
```properties
jwt.refresh-expiration=604800000
```
Propiedad definida, nunca inyectada. Ningún `@Value("${jwt.refresh-expiration}")` existe en todo el proyecto.

---

### `EmailService.java:41–59` — IMPORTANTE
`enviarNotificacionGanador()` implementado pero nunca invocado por ningún servicio. El ganador de la subasta no recibe ningún email. Feature silenciosamente ausente.

---

### `WebSocketService.java:45–49` — IMPORTANTE
`broadcastAuctionClosed()` implementado pero nunca llamado. No existe ningún mecanismo para cerrar una subasta (ver sección de funcionalidades incompletas).

---

### Métodos de repositorio sin invocaciones — MENOR

| Archivo | Método | Situación |
|---------|--------|-----------|
| `MedioPagoRepository.java:17` | `findByUsuarioAndVerificadoTrue` | Nunca invocado |
| `MedioPagoRepository.java:20` | `existsByIdAndUsuario` | Nunca invocado |
| `MultaRepository.java:17` | `findByUsuarioAndEstado` | Nunca invocado — se usa `countBy` |
| `PujaRepository.java:29` | `findByUsuarioAndEstado` | Nunca invocado |
| `ConsignacionRepository.java:17` | `findByUsuarioAndEstado` | Nunca invocado |
| `CompraRepository.java:16` | `findByIdAndUsuario` | Nunca invocado — `CompraService` usa `findById` + check manual |

---

### `EstadoPuja.PENDIENTE` — MENOR
El propio `Puja.java:13` lo documenta: *"en la práctica no persisten pujas en estado PENDIENTE"*. `PujaService` crea todas las pujas directamente en `CONFIRMADA`. El valor del enum existe pero nunca se asigna.

---

### `AuctionClosedMessage.java` — MENOR
DTO completo, nunca instanciado ni enviado desde ningún lugar del código.

---

## 3. Problemas de seguridad

### `application.properties:17` — CRÍTICO
**Secreto JWT hardcodeado en el repositorio**

```properties
jwt.secret=dGhpcy1pcy1hLXZlcnktbG9uZy1zZWNyZXQta2V5LWZvci1zdWJhc3Rhcy1zeXN0ZW0tZGExLTIwMjY=
```

Cualquier persona con acceso al repo puede decodificar este Base64, obtener la clave HMAC-SHA256 y firmar tokens JWT arbitrarios haciéndose pasar por cualquier usuario del sistema.

**Corrección:** reemplazar por `${JWT_SECRET}` (igual que en `application-prod.properties`). Rotar el secreto inmediatamente si el repo es o fue público.

---

### `application-prod.properties:4` — CRÍTICO
**Password de base de datos de producción con valor placeholder hardcodeado**

```properties
spring.datasource.password=changeme
```

Si alguien despliega con el perfil `prod` sin configurar la variable de entorno correspondiente, la app se conecta a la base de datos con la contraseña literal `changeme`.

**Corrección:** reemplazar por `${DB_PASSWORD}` igual que se hace con las credenciales de mail.

---

### `SecurityConfig.java:48` — IMPORTANTE
**H2 Console completamente abierta, sin autenticación**

```java
.requestMatchers("/h2-console/**").permitAll()
```

Si el servidor de desarrollo es alcanzable remotamente, cualquier persona tiene acceso completo a la base de datos en memoria.

**Corrección:** restringir solo a localhost o proteger con autenticación básica. En producción ya está deshabilitada (`spring.h2.console.enabled=false` en `application-prod.properties`).

---

### `ConsignacionService.java:61–65` — IMPORTANTE
**Validación de tipo de archivo por MIME declarado por el cliente**

```java
String contentType = foto.getContentType();
if (contentType == null || !contentType.startsWith("image/")) { throw... }
```

El `Content-Type` lo envía el cliente y puede ser falsificado trivialmente. Un archivo `.php`, `.html` o `.exe` con header `Content-Type: image/jpeg` pasa esta validación.

**Corrección:** validar los magic bytes del archivo. Opciones: Apache Tika (`tika.detect(inputStream)`), o leer los primeros bytes y comparar con las firmas conocidas de JPEG (`FF D8 FF`), PNG (`89 50 4E 47`), etc.

---

### Endpoints de auth sin rate limiting — IMPORTANTE
`POST /auth/login` y `POST /auth/registro/paso1` no tienen ningún mecanismo de limitación de intentos. Un atacante puede realizar miles de intentos de login por segundo sin consecuencias.

**Corrección:** agregar Bucket4j o un interceptor con contador en memoria. Como mínimo, registrar intentos fallidos y bloquear IPs con más de N intentos en X minutos.

---

### Sin configuración CORS — IMPORTANTE
No hay ninguna configuración `CorsConfigurationSource` en `SecurityConfig`. Con `SessionCreationPolicy.STATELESS` en Spring Security 6, el comportamiento CORS depende de la configuración por defecto de Spring MVC, que puede ser permisivo o bloquear requests legítimos según el entorno.

**Corrección:**
```java
.cors(cors -> cors.configurationSource(corsConfigurationSource()))
```

---

### Sin límite de tamaño en multipart — IMPORTANTE
No hay `spring.servlet.multipart.max-file-size` ni `spring.servlet.multipart.max-request-size` en `application.properties`. La validación de 5 MB por foto en `ConsignacionService.java:66` se ejecuta después de que Spring ya recibió y parseó el multipart completo en memoria.

**Corrección:**
```properties
spring.servlet.multipart.max-file-size=6MB
spring.servlet.multipart.max-request-size=50MB
```

---

### `PujaWebSocketController.java:53` — MENOR
**Mensaje de excepción interna expuesto al cliente**

```java
.mensaje("No se pudo procesar la puja: " + e.getMessage())
```

El mensaje crudo de cualquier excepción (incluyendo mensajes de Hibernate, nombres de columnas, stacktraces) llega al cliente WebSocket.

**Corrección:** usar un mensaje genérico y loguear el detalle internamente.

---

### `WebSocketAuthInterceptor.java:43–49` — MENOR
**Token WebSocket inválido no rechaza la conexión**

```java
if (jwtUtil.isTokenValid(token, userDetails)) {
    accessor.setUser(auth);
}
// si inválido: continúa sin user — conexión permitida
```

Un cliente con token expirado o manipulado puede conectarse al WebSocket y suscribirse a topics públicos sin recibir ninguna notificación de rechazo.

---

### `DataLoader.java:255–256` — MENOR
```java
log.info("Login de prueba: juan@test.com / password123 (PLATA)");
log.info("Login de prueba: maria@test.com / password123 (ORO)");
```
Credenciales en nivel INFO. Mala práctica que puede propagarse a otros archivos.

---

## 4. Problemas de rendimiento

### `SubastaService.java:218` — IMPORTANTE
**N+1: `items.size()` dispara una query por cada subasta en la página**

```java
.totalItems(s.getItems() != null ? s.getItems().size() : 0)
```

`Subasta.items` es `FetchType.LAZY`. La query de `findAccesibles` hace `FETCH JOIN` del `rematador` pero no de `items`. Con 20 subastas por página: 1 query principal + 20 queries lazy = 21 queries totales.

**Corrección:** agregar un campo calculado en la query JPQL, o usar `@Formula("(SELECT COUNT(*) FROM items i WHERE i.subasta_id = id)")` en la entidad.

---

### `PujaService.java:64` — IMPORTANTE
**Query innecesaria: campo desnormalizado ignorado**

```java
long multasPendientes = multaRepository.countByUsuarioAndEstado(usuario, EstadoMulta.PENDIENTE);
```

La entidad `Usuario` tiene `multasPendientes` (campo desnormalizado en `Usuario.java:65`) mantenido sincronizado en `MultaService.pagarMulta()`. Se hace una query extra a la BD en cada intento de puja.

**Corrección:** `if (usuario.getMultasPendientes() > 0) throw...`.

---

### `CatalogoController.java:70` y `:128` — IMPORTANTE
**N+1 en `GET /items/{itemId}` y `GET /items/{itemId}/imagenes`**

```java
Item item = itemRepository.findById(itemId)...  // sin fetch joins
// luego accede a:
item.getImagenes()   // lazy → query adicional
item.getPoliza()     // lazy → query adicional
item.getSubasta()    // lazy → query adicional
```

Tres queries lazy para un único `GET /items/{id}`. En `findBySubastaWithDetails` ya hay un fetch join correcto para el catálogo completo, pero no se reutiliza aquí.

**Corrección:** agregar en `ItemRepository` un `findByIdWithDetails` con `LEFT JOIN FETCH i.imagenes LEFT JOIN FETCH i.poliza LEFT JOIN FETCH i.subasta`.

---

### `PujaService.java:185` — IMPORTANTE
**Historial de pujas sin paginación ni límite**

```java
pujas = pujaRepository.findBySubastaOrderByTimestampDesc(subasta);
```

Una subasta activa con muchos participantes puede tener miles de pujas que se cargan completamente en memoria y se serializan de una vez.

**Corrección:** agregar `Pageable` al método del repositorio y un parámetro `?page=&size=` al endpoint.

---

### `PujaService.java:50` — MENOR
**`locksPorSubasta` crece indefinidamente**

```java
private final Map<Long, ReentrantLock> locksPorSubasta = new ConcurrentHashMap<>();
```

Los locks de subastas cerradas nunca se eliminan. Memory leak de larga duración.

**Corrección:** usar Guava `CacheBuilder.newBuilder().expireAfterAccess(1, HOURS).build()` como mapa de locks.

---

## 5. Inconsistencias con el esquema SQL

### `EstructuraActual.sql` — IMPORTANTE
**El esquema SQL no tiene ninguna correspondencia con el modelo JPA actual**

| SQL | JPA | Estado |
|-----|-----|--------|
| `paises`, `personas`, `empleados`, `sectores` | No existen | Solo en SQL |
| `registroDeSubasta` | No existe | Solo en SQL |
| `asistentes` | `participaciones` | Renombrado y rediseñado |
| `productos` + `itemsCatalogo` | `items` | Unificado |
| `subastadores` | `rematadores` | Renombrado |
| `usuarios`, `medios_pago`, `multas`, `compras`, etc. | Existen en JPA | Solo en JPA |

Hibernate genera el DDL desde las entidades (`create-drop` en dev, `validate` en prod). Si alguien usa el SQL para crear la BD manualmente e intenta conectar la app, falla en el arranque.

**Corrección:** actualizar `EstructuraActual.sql` exportando el esquema que genera Hibernate, o eliminarlo del repo y documentar que el DDL lo maneja Hibernate.

---

## 6. Duplicación y responsabilidades mal separadas

### `ConsignacionService.java:135,161` — IMPORTANTE
**Métodos con tipo de retorno `Object`**

```java
public Object obtenerUbicacion(Long consignacionId, Usuario usuario)
public Object obtenerPoliza(Long consignacionId, Usuario usuario)
```

Construyen `Map<String, Object>` manualmente. Rompe type-safety, no se puede documentar en Swagger y no hay validación de compilación sobre los campos. Los controllers heredan el problema con `ResponseEntity<Object>`.

**Corrección:** crear `UbicacionResponse` y `PolizaResponse` como records o clases `@Builder`.

---

### `UsuarioService.java:103,118` — IMPORTANTE
**Mismo problema: `Map<String, Object>` como tipo de retorno**

```java
public List<Map<String, Object>> listarParticipaciones(String email)
public Map<String, Object> obtenerMetricas(String email)
```

**Corrección:** `ParticipacionResponse` y `MetricasResponse`.

---

### Patrón de resolución de usuario inconsistente — IMPORTANTE

Algunos controllers resuelven el `Usuario` y lo pasan al service:
```java
// ConsignacionController
Usuario usuario = usuarioService.obtenerPorEmail(userDetails.getUsername()); // query en controller
consignacionService.crear(usuario, ...);
```

Otros services lo resuelven internamente:
```java
// SubastaService, PujaService, MultaService
public void desconectar(Long subastaId, String email) {
    Usuario usuario = usuarioService.obtenerPorEmail(email); // query en service
```

La inconsistencia causa queries dobles en algunos flujos y hace difícil razonar sobre cuántas veces se accede a la BD por request.

---

### `CompraService.java:27–28` — MENOR
**Lazy load innecesario en verificación de pertenencia**

```java
Compra compra = compraRepository.findById(compraId)...
if (!compra.getUsuario().getId().equals(usuario.getId())) { throw... }
```

`compra.getUsuario()` dispara un lazy load para obtener el `Usuario` completo solo para comparar el ID. `CompraRepository` ya tiene `findByIdAndUsuario` definido pero no se usa.

**Corrección:** `compraRepository.findByIdAndUsuario(compraId, usuario).orElseThrow(...)`.

---

### `SubastaService.java:223` — MENOR
**`generarAlias()` vive en el service incorrecto**

Método `static` en `SubastaService` invocado desde `PujaService`. Un método de utilidad estático no debería vivir en un service.

**Corrección:** mover a `com.subastas.util.AliasUtil`.

---

## 7. Funcionalidades incompletas que bloquean producción

### Sin mecanismo de cierre de subasta — CRÍTICO
No existe ningún scheduler, endpoint de admin ni lógica que haga transicionar una subasta de `ABIERTA` a `CERRADA`. Consecuencias en cascada:

- `Item.estado` nunca transiciona a `VENDIDO`
- `Compra` nunca es creada por el sistema (solo existe en el `DataLoader`)
- `broadcastAuctionClosed()` y `AuctionClosedMessage` son código muerto
- `EmailService.enviarNotificacionGanador()` nunca se llama

Este es el flujo de negocio más crítico del sistema y no tiene implementación.

---

### `AuthService.java:75–76` — CRÍTICO
**Archivos de DNI nunca se guardan en disco**

```java
.fotoDniFrente(fotoDniFrente != null ? "uploads/dni/" + FileUtil.uuidFilename(fotoDniFrente) : null)
```

`FileUtil.uuidFilename()` solo genera un nombre de archivo. El `MultipartFile` **nunca se escribe**. La BD guarda una ruta, pero el archivo no existe. Cada reinicio del servidor deja referencias huérfanas. El mismo problema ocurre en `ConsignacionService.java:92`.

**Corrección:**
```java
Path destino = Paths.get("uploads/dni/", FileUtil.uuidFilename(fotoDniFrente));
Files.createDirectories(destino.getParent());
fotoDniFrente.transferTo(destino);
```

---

### Multas nunca generadas automáticamente — IMPORTANTE
`MultaService` permite pagarlas y listarlas, pero no hay ningún código que las cree. En el modelo de negocio se generan cuando un ganador incumple el pago. Sin el mecanismo de cierre de subasta esto nunca ocurre, y tampoco existe el trigger aunque existiera el cierre.

---

## Resumen: top 5 antes de producción

| # | Problema | Archivo | Por qué es urgente |
|---|----------|---------|-------------------|
| **1** | JWT secret hardcodeado en el repo | `application.properties:17` | Compromiso total del sistema de autenticación. Cualquiera puede forjar tokens. |
| **2** | Archivos subidos nunca se guardan en disco | `AuthService.java:75–76`, `ConsignacionService.java:92` | El registro y las consignaciones están completamente rotos. Cada reinicio destruye las referencias. |
| **3** | Sin mecanismo de cierre de subasta | (ausente en todo el proyecto) | El flujo de negocio principal no puede completarse: ningún ganador, ninguna compra, ningún email. |
| **4** | `tokenRefresh` = `tokenAcceso` + `jwt.refresh-expiration` ignorada | `AuthService.java:153`, `JwtUtil.java` | La renovación de sesión no funciona. Los clientes quedan deslogueados cada 24 hs sin aviso. |
| **5** | DB password `changeme` en prod config | `application-prod.properties:4` | Un deploy sin configurar la variable de entorno conecta la app con contraseña literal. |
