# Simplificaciones del backend

Este documento describe los cambios aplicados para alinear el proyecto con la consigna, eliminando complejidad innecesaria para un TP universitario.

---

## 1. Eliminación del sistema de eventos Spring

**Qué había:** Tres clases en el package `event/` (`PujaConfirmadaEvent`, `RegistroCompletadoEvent`, `SubastaCerradaItemEvent`) que se publicaban con `ApplicationEventPublisher` y se escuchaban con `@TransactionalEventListener` en distintos servicios. El objetivo era desacoplar el envío de WebSocket y emails del commit de la transacción.

**Por qué se eliminó:** Para un TP, este nivel de desacoplamiento transaccional es innecesario. El flujo se vuelve difícil de seguir: un método publica un evento, otro lo escucha en otro servicio, y la conexión entre ambos no es evidente. Tres clases extras y tres listeners para algo que se puede hacer con una llamada directa.

**Cómo quedó:**
- `PujaService` llama directamente a `WebSocketService.broadcastBidUpdated()` y `sendBidConfirmed()`.
- `AuthService` llama directamente a `MockVerificacionService.verificarYEnviarEmail()` (que sigue siendo `@Async`).
- `SubastaService` llama directamente a `EmailService.enviarNotificacionGanador()` (que sigue siendo `@Async`).
- Package `event/` eliminado.

---

## 2. Eliminación de Apache Tika

**Qué había:** Dependencia `tika-core` (~2 MB) usada en `AuthService` y `ConsignacionService` para detectar el tipo real de archivo por magic bytes, en lugar de confiar en el `Content-Type` del cliente.

**Por qué se eliminó:** La validación por magic bytes es una buena práctica de seguridad en producción, pero es excesiva para un TP donde los archivos los sube el propio usuario de prueba. Agrega una dependencia pesada para un beneficio que la consigna no pide.

**Cómo quedó:** Validación con `archivo.getContentType().startsWith("image/")` en ambos servicios. Mismo comportamiento funcional, sin dependencia extra.

---

## 3. Reemplazo de Caffeine por ConcurrentHashMap

**Qué había:** Dependencia `caffeine` usada en `PujaService` para mantener un `Cache<Long, ReentrantLock>` con expiración automática de locks por subasta (1 hora de inactividad).

**Por qué se eliminó:** La expiración automática es un nice-to-have irrelevante para un TP. El lock se libera explícitamente al cerrar la subasta via `liberarLock()`, que es el único momento que importa.

**Cómo quedó:** `ConcurrentHashMap<Long, ReentrantLock>` de la JDK estándar. Misma semántica de concurrencia, cero dependencias extra. El método `liberarLock()` ahora llama a `remove()` en lugar de `invalidate()`.

---

## 4. Eliminación del refresh token

**Qué había:** Un segundo token JWT de larga duración (`jwt.refresh-expiration = 7 días`) con su endpoint `POST /auth/refresh`, lógica en `AuthService.refreshToken()`, método `JwtUtil.generateRefreshToken()`, y el campo `tokenRefresh` en `LoginResponse`.

**Por qué se eliminó:** La consigna solo pide JWT para autenticación. El mecanismo de refresh token es una feature de producción (renovar sesión sin re-login) que el TP no requiere y agrega superficie de ataque y código sin propósito claro.

**Cómo quedó:** Un solo token de acceso de 24 horas. `LoginResponse` solo devuelve `tokenAcceso`. `JwtUtil` solo tiene `generateToken()`.

---

## 5. Eliminación del endpoint de reenvío de token

**Qué había:** `POST /auth/reenviar-token` que permitía solicitar un nuevo token de activación para completar el registro.

**Por qué se eliminó:** La consigna describe un flujo de dos pasos sin reenvío: el usuario recibe el token, lo usa, listo. El reenvío es una feature UX no pedida.

**Cómo quedó:** El método `reenviarToken()` fue eliminado de `AuthService` y el endpoint del `AuthController`.

---

## 6. Eliminación de DTOs huérfanos y sus endpoints

**Qué había:** Cuatro DTOs de response que respondían a endpoints no pedidos por la consigna:

| DTO | Endpoint | Descripción |
|-----|----------|-------------|
| `MetricasResponse` | `GET /usuarios/metricas` | Total subastas asistidas, ganadas, monto pagado |
| `ParticipacionResponse` | `GET /usuarios/participaciones` | Historial de conexiones a subastas |
| `UbicacionResponse` | `GET /consignaciones/{id}/ubicacion` | Depósito con lat/lng del bien consignado |
| `PolizaResponse` | `GET /consignaciones/{id}/poliza` | Datos de la aseguradora, prima, vigencia |

**Por qué se eliminó:** La consigna menciona que el usuario "puede ver la póliza y la ubicación" pero no especifica un endpoint dedicado con todos esos campos. Los DTOs de métricas y participaciones directamente no están en la consigna. Son features extras que agregan código sin cubrir requisitos.

**Cómo quedó:** Los cuatro DTOs, sus métodos en los servicios (`listarParticipaciones`, `obtenerMetricas`, `obtenerUbicacion`, `obtenerPoliza`) y los endpoints correspondientes fueron eliminados. `UsuarioService` también dejó de depender de `CompraRepository` al no necesitarlo para las métricas.

---

## 7. Aplanamiento de ComisionService

**Qué había:** Un `@Service` separado (`ComisionService`) con dos valores inyectados desde `application.properties` (`app.comision.porcentaje` y `app.envio.costo-fijo`) para calcular comisión, costo de envío y total al cerrar una subasta.

**Por qué se eliminó:** Son tres líneas de aritmética usadas en un único lugar (`SubastaService.cerrarSubasta()`). Extraerlas a un servicio con configuración externalizada es over-engineering para valores que son constantes de negocio del TP.

**Cómo quedó:** Dos constantes directas en `SubastaService`:
```java
private static final BigDecimal PORCENTAJE_COMISION = new BigDecimal("10");
private static final BigDecimal COSTO_ENVIO = new BigDecimal("1500");
```
El cálculo se hace inline. `ComisionService.java` fue eliminado.

---

## 8. Eliminación de WebSocketService wrapper

**Qué había:** Un `@Service` (`WebSocketService`) que envolvía `SimpMessagingTemplate` con 4 métodos de una línea cada uno, solo para nombrar los destinos STOMP.

**Por qué se eliminó:** No agregaba lógica ni mejoraba la testabilidad. Era un intermediario vacío entre los servicios y Spring Messaging.

**Cómo quedó:** `PujaService`, `SubastaService` y `PujaWebSocketController` inyectan `SimpMessagingTemplate` directamente y llaman `convertAndSend`/`convertAndSendToUser` con el destino inline. `WebSocketService.java` eliminado.

---

## 9. Eliminación de paginación en listado de subastas

**Qué había:** `SubastaService.listar()` devolvía `Page<SubastaResponse>` con `PageRequest.of(page, 20)`. El controller respondía `{"data": [...], "total": N, "page": 0}`.

**Por qué se eliminó:** El TP tiene 2-3 subastas de prueba. La paginación agregaba complejidad en el repositorio, el servicio, el controller y los tests sin ningún beneficio real.

**Cómo quedó:** `listar()` devuelve `List<SubastaResponse>` directamente. El controller retorna el array sin wrapper. `SubastaRepository.findAccesibles()` sin `Pageable`.

---

## Resultado acumulado

- **Archivos eliminados:** 10 (3 eventos, 4 DTOs, 1 `ComisionService`, 1 `WebSocketService`, endpoints varios)
- **Dependencias eliminadas:** 2 (`tika-core`, `caffeine`)
- **Endpoints eliminados:** 6 (`/refresh`, `/reenviar-token`, `/metricas`, `/participaciones`, `/consignaciones/{id}/ubicacion`, `/consignaciones/{id}/poliza`)
- **Tests:** 53 pasan, 0 fallos
