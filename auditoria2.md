# Auditoría del Backend — Sistema de Subastas DA1 (Segunda Revisión)

**Fecha:** 2026-05-25  
**Alcance:** Todos los archivos Java, SQL, configuración y properties del proyecto  
**Rama auditada:** `backend`  
**Nota:** Esta auditoría es posterior a las correcciones de la primera auditoría (19 problemas resueltos).

---

## PROBLEMAS CRÍTICOS

### 1. Endpoints `/reenviar-token` y `/refresh` sin validación de input

**Archivo:** `src/main/java/com/subastas/controller/AuthController.java:48-57`  
**Severidad:** CRÍTICO

```java
@PostMapping("/reenviar-token")
public ResponseEntity<Map<String, String>> reenviarToken(@RequestBody Map<String, String> request) {
    authService.reenviarToken(request.get("email"));  // null si la key no existe
}

@PostMapping("/refresh")
public ResponseEntity<Map<String, String>> refresh(@RequestBody Map<String, String> request) {
    return ResponseEntity.ok(authService.refreshToken(request.get("refreshToken")));  // idem
}
```

**Qué está mal:** Ambos endpoints usan `Map<String, String>` sin validación. Si el body no incluye la key esperada, `request.get()` devuelve `null`, que se propaga al servicio y puede causar NPE o comportamiento indefinido. No hay `@Valid`, no hay DTO tipado, no hay validación de formato de email.

**Cómo resolverlo:** Crear DTOs con `@NotBlank` y `@Email` donde corresponda, y usar `@Valid @RequestBody`.

---

### 2. Scheduler puede generar multas duplicadas (race condition)

**Archivo:** `src/main/java/com/subastas/service/IncumplimientoService.java:44-75`  
**Severidad:** CRÍTICO

```java
@Transactional
public void procesarComprasVencidas() {
    List<Compra> vencidas = compraRepository
            .findByEstadoPagoAndFechaLimitePagoLessThanEqual(EstadoPago.PENDIENTE, LocalDateTime.now());
    for (Compra compra : vencidas) {
        compra.setEstadoPago(EstadoPago.INCUMPLIDO);
        // ...genera multa...
    }
}
```

**Qué está mal:** Si el scheduler ejecuta dos instancias solapadas (por deploy con múltiples nodos, o retraso en ejecución), ambas queries leerán las mismas compras PENDIENTES antes de que la primera haga commit. Resultado: multas duplicadas por la misma compra.

**Cómo resolverlo:** Usar `SELECT ... FOR UPDATE SKIP LOCKED` en la query, o agregar un flag `multaGenerada` en `Compra` para idempotencia. Alternativamente, un lock distribuido o `@SchedulerLock` (ShedLock).

---

### 3. `cerrarSubasta()` puede leer estado desactualizado de ítems

**Archivo:** `src/main/java/com/subastas/service/SubastaService.java:224-285`  
**Severidad:** CRÍTICO

```java
List<Item> items = itemRepository.findBySubasta(subasta);
for (Item item : items) {
    if (item.getMejorPostor() != null) {
        // crea Compra con mejorOferta y mejorPostor
```

**Qué está mal:** `cerrarSubasta()` lee los ítems sin lock. Mientras tanto, `PujaService.realizarPuja()` podría estar procesando una puja con `PESSIMISTIC_WRITE` en el mismo ítem. El cierre podría leer un `mejorPostor` / `mejorOferta` desactualizado, creando la Compra con el ganador incorrecto.

**Cómo resolverlo:** Usar `findBySubastaWithLock()` con `@Lock(PESSIMISTIC_WRITE)` para que el cierre espere a que la puja en curso termine, o validar que no haya pujas activas antes de cerrar.

---

### 4. `PujaService` no valida que la subasta esté ABIERTA

**Archivo:** `src/main/java/com/subastas/service/PujaService.java:54-84`  
**Severidad:** CRÍTICO

```java
public PujaResponse realizarPuja(Long subastaId, String email, PujaRequest request) {
    Usuario usuario = usuarioService.obtenerPorEmail(email);
    Subasta subasta = subastaRepository.findById(subastaId)...;
    // Valida: usuario conectado, sin multas, lock
    // NUNCA valida: subasta.getEstado() == EstadoSubasta.ABIERTA
```

**Qué está mal:** No se verifica que la subasta esté en estado `ABIERTA`. Si el scheduler cierra la subasta (cambia estado a `CERRADA`) pero la desconexión de participantes aún no se procesó, un usuario todavía "conectado" puede seguir pujando en una subasta cerrada. La puja se persiste, actualiza `mejorOferta`, pero la Compra ya fue creada con otro ganador.

**Cómo resolverlo:** Agregar validación al inicio de `realizarPuja()`:

```java
if (subasta.getEstado() != EstadoSubasta.ABIERTA) {
    throw new BusinessException(ErrorCodes.SUBASTA_NO_ABIERTA, "La subasta no está abierta");
}
```

---

### 5. Medio de pago siempre verificado automáticamente

**Archivo:** `src/main/java/com/subastas/service/UsuarioService.java:64`  
**Severidad:** CRÍTICO

```java
MedioPago medioPago = MedioPago.builder()
        .verificado(true)  // siempre true
```

**Qué está mal:** Todo medio de pago se marca como verificado inmediatamente, sin integración con procesador de pagos. Un usuario puede registrar datos de tarjeta/cuenta bancaria ficticios y pujar con ellos. En producción esto permite que cualquiera gane subastas con medios de pago inválidos.

**Cómo resolverlo:** Dejar `verificado(false)` por defecto y agregar un flujo de verificación (webhook de procesador de pagos, o aprobación manual). Para dev, crear un endpoint admin `/api/v1/admin/medios-pago/{id}/verificar`.

---

### 6. Falta constraint UNIQUE en `participaciones(usuario_id, subasta_id)`

**Archivo:** `EstructuraActual.sql:161-173`  
**Severidad:** CRÍTICO

**Qué está mal:** El código en `SubastaService.conectar()` hace `findByUsuarioAndSubasta().orElse(new Participacion())` para evitar duplicados, pero no hay constraint UNIQUE en la BD. Bajo requests concurrentes, dos threads pueden ambos obtener `empty()` del `findByUsuarioAndSubasta` y crear dos participaciones duplicadas para el mismo par (usuario, subasta).

**Cómo resolverlo:**

```sql
ALTER TABLE participaciones ADD UNIQUE INDEX uk_participacion_usuario_subasta (usuario_id, subasta_id);
```

---

## PROBLEMAS IMPORTANTES

### 7. Log con placeholder SLF4J incorrecto

**Archivo:** `src/main/java/com/subastas/service/IncumplimientoService.java:73`  
**Severidad:** IMPORTANTE

```java
log.info("Multa generada: ${} para usuario {} por compra #{}", montoMulta, ...);
```

**Qué está mal:** `${}` no es un placeholder SLF4J estándar. SLF4J solo reconoce `{}`. El `$` y `#` se imprimen como literales y `{}` se reemplaza por el argumento. El output se lee bien por accidente (`$5500.00`), pero es un patrón confuso y frágil.

**Cómo resolverlo:** Cambiar a `"Multa generada: {} para usuario {} por compra {}"` y formatear el monto con currency symbol en el argumento si se necesita.

---

### 8. `mapCompraToResponse()` duplicado en CompraService y ChatService

**Archivo:** `src/main/java/com/subastas/service/CompraService.java:24-49` y `src/main/java/com/subastas/service/ChatService.java:121-147`  
**Severidad:** IMPORTANTE

**Qué está mal:** Exactamente el mismo bloque de 25 líneas de mapping `Compra → CompraResponse` está copiado en dos servicios. Si se agrega un campo a `CompraResponse`, hay que actualizar los dos lugares.

**Cómo resolverlo:** Extraer a un método estático en `CompraResponse` o a un mapper compartido, e invocarlo desde ambos servicios.

---

### 9. N+1 en queries de pujas privadas (sin fetch join en usuario)

**Archivo:** `src/main/java/com/subastas/repository/PujaRepository.java:36-40`  
**Severidad:** IMPORTANTE

```java
Page<Puja> findBySubastaAndUsuarioOrderByTimestampDesc(Subasta subasta, Usuario usuario, Pageable pageable);
Page<Puja> findBySubastaAndItemAndUsuarioOrderByTimestampDesc(Subasta subasta, Item item, Usuario usuario, Pageable pageable);
```

**Qué está mal:** Estos métodos (pujas propias) no tienen `LEFT JOIN FETCH p.usuario` como sí tienen los métodos públicos (líneas 20, 27). Cuando `PujaService.obtenerHistorial()` genera alias con `AliasUtil.generarAlias(p.getUsuario())`, cada puja dispara un SELECT adicional a `usuarios`.

**Cómo resolverlo:** Agregar `@Query` con `LEFT JOIN FETCH p.usuario` a los métodos paginados privados.

---

### 10. Paginación sin límite máximo en controllers

**Archivo:** `src/main/java/com/subastas/controller/SubastaController.java:100-105`  
**Severidad:** IMPORTANTE

```java
@RequestParam(defaultValue = "0") int page,
@RequestParam(defaultValue = "50") int size
```

**Qué está mal:** Un cliente puede enviar `?size=999999` y cargar toda la tabla en memoria. No hay `@Max` ni cap en el servicio. Mismo patrón en el endpoint de listado de subastas (sin límite de página).

**Cómo resolverlo:** Agregar `@Min(0)` y `@Max(100)` en los parámetros de paginación, y un `Math.min(size, 100)` defensivo en el servicio.

---

### 11. CORS permite headers wildcard con credentials

**Archivo:** `src/main/java/com/subastas/config/SecurityConfig.java:98`  
**Severidad:** IMPORTANTE

```java
config.setAllowedHeaders(List.of("*"));
config.setAllowCredentials(true);
```

**Qué está mal:** `allowedHeaders("*")` combinado con `allowCredentials(true)` es un patrón que OWASP desaconseja. Permite que cualquier header custom sea enviado en requests CORS con credenciales. Además falta `config.setMaxAge()`, forzando preflight en cada request.

**Cómo resolverlo:** Restringir headers a `"Content-Type", "Authorization", "Accept"` y agregar `config.setMaxAge(3600L)`.

---

### 12. `enviarNotificacionGanador` sigue tragando excepciones

**Archivo:** `src/main/java/com/subastas/service/EmailService.java:52-55`  
**Severidad:** IMPORTANTE

```java
public void enviarNotificacionGanador(...) {
    try {
        // ...enviar email...
    } catch (Exception e) {
        log.error("Error enviando notificación de ganador a {}: {}", emailDestino, e.getMessage());
    }
}
```

**Qué está mal:** A diferencia de `enviarTokenRegistro()` (que ahora propaga la excepción, fix #5 de la primera auditoría), `enviarNotificacionGanador()` sigue tragando excepciones. Si falla el SMTP, el ganador nunca se entera de que ganó. Como ahora se ejecuta vía `@Async @TransactionalEventListener`, la excepción no rompe nada, pero el usuario queda sin notificación sin mecanismo de retry.

**Cómo resolverlo:** Propagar la excepción (el caller `onSubastaCerradaItem` la logueará como error del hilo async), y registrar intentos fallidos en BD para retry posterior.

---

### 13. `Thread.sleep(3000)` hardcodeado en mock de verificación

**Archivo:** `src/main/java/com/subastas/service/MockVerificacionService.java:32`  
**Severidad:** IMPORTANTE

**Qué está mal:** Cada registro tiene un delay fijo de 3 segundos que bloquea un hilo del pool async. En producción con muchos registros simultáneos, esto agota el pool de hilos. No hay forma de desactivar el sleep sin recompilar.

**Cómo resolverlo:** Mover a un `@Value("${app.mock.verificacion.delay-ms:0}")` y setear a 0 en producción. O anotar la clase con `@Profile("!prod")` para excluirla completamente en producción.

---

### 14. Timing attack posible en validación de token de registro

**Archivo:** `src/main/java/com/subastas/service/AuthService.java:117`  
**Severidad:** IMPORTANTE

```java
if (!request.getTokenEmail().equals(usuario.getTokenEmail())) {
```

**Qué está mal:** `String.equals()` hace comparación byte a byte y retorna en el primer mismatch, lo que filtra información de cuántos caracteres iniciales coinciden. Un atacante puede inferir el token UUID carácter por carácter midiendo tiempos de respuesta.

**Cómo resolverlo:** Usar `MessageDigest.isEqual()` para comparación en tiempo constante:

```java
if (!MessageDigest.isEqual(
        request.getTokenEmail().getBytes(StandardCharsets.UTF_8),
        usuario.getTokenEmail().getBytes(StandardCharsets.UTF_8))) {
```

---

### 15. Archivos subidos parcialmente dejan huérfanos en disco

**Archivo:** `src/main/java/com/subastas/service/ConsignacionService.java:98-125`  
**Severidad:** IMPORTANTE

**Qué está mal:** La consignación se guarda primero en BD (línea 98), luego se suben fotos al disco en un loop (líneas 102-119). Si la foto 5 de 6 falla con IOException, las 4 anteriores quedan en disco pero la entidad referencia un set incompleto. No hay cleanup de archivos ya escritos en caso de error.

**Cómo resolverlo:** Subir los archivos al disco primero (en un directorio temporal), y solo persistir en BD después de que todos los archivos se escribieron correctamente. En caso de error, limpiar el directorio temporal.

---

## PROBLEMAS MENORES

### 16. DELETE de medio de pago devuelve 200 en vez de 204

**Archivo:** `src/main/java/com/subastas/controller/UsuarioController.java:58`  
**Severidad:** MENOR

**Qué está mal:** `ResponseEntity.ok(Map.of("mensaje", "..."))` devuelve 200 con body. La convención REST para DELETE exitoso es 204 No Content.

**Cómo resolverlo:** `return ResponseEntity.noContent().build();`

---

### 17. Respuesta de listado de subastas usa `Map<String, Object>` sin tipar

**Archivo:** `src/main/java/com/subastas/controller/SubastaController.java:48-53`  
**Severidad:** MENOR

```java
return ResponseEntity.ok(Map.of(
    "data", result.getContent(),
    "total", result.getTotalElements(),
    "page", result.getNumber()
));
```

**Qué está mal:** Swagger/OpenAPI no puede documentar la estructura de la respuesta. Los clientes no tienen type safety. Otros endpoints usan DTOs tipados — este es inconsistente.

**Cómo resolverlo:** Crear un DTO `SubastaListResponse` con campos `data`, `total`, `page`.

---

### 18. Credenciales de test en logs de DataLoader

**Archivo:** `src/main/java/com/subastas/config/DataLoader.java:255-256`  
**Severidad:** MENOR

```java
log.debug("Login de prueba: juan@test.com / password123 (PLATA)");
```

**Qué está mal:** Aunque está en nivel DEBUG y excluido de producción (`@Profile("!prod")`), loguear contraseñas en claro es una mala práctica. Stack traces o auditorías de logs podrían capturarlas.

**Cómo resolverlo:** Loguear solo el email y la categoría, sin la contraseña.

---

### 19. Tamaño máximo de foto hardcodeado en ConsignacionService

**Archivo:** `src/main/java/com/subastas/service/ConsignacionService.java:79`  
**Severidad:** MENOR

```java
if (foto.getSize() > 5_242_880L) {
```

**Qué está mal:** El límite de 5 MB está hardcodeado. Cambiarlo requiere recompilar.

**Cómo resolverlo:** Mover a `@Value("${app.uploads.max-foto-size:5242880}")`.

---

### 20. Falta `@Column(length=...)` en campos URL de entidades

**Archivo:** `src/main/java/com/subastas/model/entity/ImagenItem.java:18`, `FotoConsignacion.java:18`  
**Severidad:** MENOR

**Qué está mal:** Los campos `url` no especifican `@Column(length=255)`. Si una URL excede 255 caracteres, la BD trunca silenciosamente el valor y la imagen queda irrecuperable.

**Cómo resolverlo:** Agregar `@Column(nullable = false, length = 512)` y actualizar el VARCHAR en `EstructuraActual.sql`.

---

### 21. Faltan anotaciones OpenAPI en controllers

**Archivo:** Todos los controllers  
**Severidad:** MENOR

**Qué está mal:** Ningún controller tiene `@Operation`, `@ApiResponse`, o `@Tag` de Swagger. La documentación auto-generada en `/swagger-ui.html` muestra endpoints sin descripción, sin códigos de error posibles, y sin ejemplos.

**Cómo resolverlo:** Agregar anotaciones `@Operation(summary = "...")` y `@ApiResponse` a cada endpoint.

---

## TOP 5 — Problemas más urgentes antes de producción

| # | Problema | Archivo | Severidad | Por qué es urgente |
|---|----------|---------|-----------|-------------------|
| 1 | **Puja sin validar estado de subasta** | `PujaService.java:54-84` | CRÍTICO | Pujas en subastas cerradas crean inconsistencia de datos irrecuperable: la Compra ya se generó con un ganador, pero la puja actualiza `mejorOferta` y `mejorPostor` del ítem. |
| 2 | **Scheduler genera multas duplicadas** | `IncumplimientoService.java:44-75` | CRÍTICO | Dos ejecuciones solapadas del scheduler leen las mismas compras PENDIENTES y generan multas duplicadas. Los usuarios reciben multas dobles y se bloquean injustamente. |
| 3 | **Cierre de subasta sin lock en ítems** | `SubastaService.java:224-285` | CRÍTICO | El cierre lee `mejorPostor` sin lock mientras una puja concurrente puede estar actualizándolo. Puede asignar el ganador incorrecto y generar la Compra con datos desactualizados. |
| 4 | **Endpoints auth sin validación de input** | `AuthController.java:48-57` | CRÍTICO | `request.get("email")` devuelve `null` si la key no existe en el Map, propagando null al servicio y causando NPE en producción. |
| 5 | **Medios de pago sin verificación real** | `UsuarioService.java:64` | CRÍTICO | Cualquier usuario puede registrar datos ficticios de tarjeta/cuenta y pujar inmediatamente con ellos. No hay validación externa de solvencia. |
