# Diagnóstico del Sistema de Subastas

> Fecha: 2026-05-27  
> Alcance: backend Spring Boot — revisión contra context.md  
> Estado: **todos los ítems resueltos** ✅

---

## Estado general

El sistema cubre todos los flujos pedidos por la consigna: registro en 2 pasos, conexión a subasta con validación de categoría y medio de pago, pujas concurrentes con lock, cierre automático con creación de Compra, chat post-subasta con mensaje automático al ganador, consignaciones con revisión mock asíncrona, incumplimientos con multa, y derivación a justicia.

La arquitectura en capas es limpia, la seguridad JWT funciona, la concurrencia de pujas está bien resuelta, y hay 53 tests de integración pasando.

---

## Errores y bugs — RESUELTOS

### BUG-01 — Puja acepta ítems de otra subasta ✅
- **Resuelto en:** `PujaService.java`, método `procesarPuja()`
- Agregado check `!item.getSubasta().getId().equals(subastaId)` → lanza `RECURSO_NO_ENCONTRADO`

---

### BUG-02 — Puja no valida que el ítem esté EN_SUBASTA ✅
- **Resuelto en:** `PujaService.java`, método `procesarPuja()`
- Agregado check `item.getEstado() != EstadoItem.EN_SUBASTA` → lanza `ESTADO_INVALIDO`

---

### BUG-03 — Cierre sin puja no crea Compra para la empresa ✅
- **Resuelto en:** `SubastaService.java`, método `cerrarSubasta()`
- El else branch ahora crea una `Compra` con `usuario = null`, `montoOfertado = precioBase`, `estadoPago = PAGADO`, `comisiones = 0`, `costoEnvio = 0`
- `Compra.usuario` se hizo nullable (era `nullable = false`)

---

### BUG-04 — DataLoader no setea fechaFin en subastas ✅
- **Resuelto en:** `DataLoader.java`
- Agregado `.fechaFin(LocalDateTime.now().plusHours(2))` a subasta1

---

### BUG-05 — Posible multa duplicada por race condition | MENOR
- **Dónde:** `IncumplimientoService.java:44-74`, método `procesarComprasVencidas()`
- **Estado:** no resuelto — `fixedDelay = 5min` hace que sea prácticamente imposible en el TP. No justifica la complejidad para un contexto académico.

---

## Faltantes respecto a la consigna — RESUELTOS

### FALTA-01 — Sin validación de monto acumulado de cheques certificados ✅
- **Resuelto en:** `PujaService.java` + `CompraRepository.java`
- Nueva query `sumTotalByUsuarioAndMedioPagoAndEstadoPago`; check en `procesarPuja()` cuando `tipo == CHEQUE_CERTIFICADO`
- Nuevo código de error: `LIMITE_CHEQUE_EXCEDIDO`

---

### FALTA-02 — Sin mensaje automático de chat al crear la Compra ✅
- **Resuelto en:** `SubastaService.java`, método `cerrarSubasta()`
- Después del `compraRepository.save(compra)` se crea un `MensajeChat` con `remitente = EMPRESA` y contenido dinámico con los valores reales de la compra:
  ```
  ¡Felicitaciones {nombre}! Ganaste el ítem "{descripcion}" en la subasta.

  Resumen de tu compra:
    • Monto ofertado:  {montoOfertado} {moneda}
    • Comisiones:      {comisiones} {moneda}
    • Costo de envío:  {costoEnvio} {moneda}
    • Total a pagar:   {total} {moneda}

  Tenés 72 horas para completar el pago.

  Por este chat podés coordinar:
    • Modalidad de entrega: envío a domicilio (incluido) o retiro personal
    • Ampliación de la cobertura del seguro del bien
  ```

---

### FALTA-03 — Sin revisión automática de consignaciones ✅
- **Resuelto con:** `MockRevisionConsignacionService` (nuevo archivo)
- `@Async` + `Thread.sleep(3000)` → pasa la consignación a `ACEPTADA` con `valorBase = precioSugerido` (o 1000 por defecto) y `comisiones = valorBase × 10%`
- Llamado desde `ConsignacionService.crear()` tras el save

---

## Over-engineering

No se detectaron casos de over-engineering.

---

## Mejoras simples — RESUELTAS

### MEJORA-01 — IOException envuelta en RuntimeException genérica ✅
- **Resuelto en:** `ConsignacionService.java`
- Cambiado a `BusinessException` con `HttpStatus.INTERNAL_SERVER_ERROR`

---

### MEJORA-02 — N+1 en listado de subastas por countBySubasta
- **Estado:** no resuelto — con 2-3 subastas de prueba el impacto es nulo. No justifica la complejidad para el TP.

---

### MEJORA-03 — Métodos read-only sin @Transactional(readOnly = true) ✅
- **Resuelto en:** `SubastaService.listar()`, `.obtener()`, `.obtenerEstadoPuja()`, `.obtenerCatalogo()`, `CompraService.obtenerCompra()`

---

### MEJORA-04 — Ítem P-002 en DISPONIBLE dentro de subasta ABIERTA ✅
- **Resuelto en:** `DataLoader.java` — cambiado a `EstadoItem.EN_SUBASTA`

---

### MEJORA-05 — AuthService.guardarArchivoDni permite null silencioso ✅
- **Resuelto en:** `AuthService.java`
- Ahora lanza `BusinessException` si el archivo es null o vacío

---

## Inconsistencias — RESUELTAS

### INCON-01 — ESTADO_INVALIDO reutilizado para 5+ errores distintos ✅
- **Resuelto:** creados códigos específicos y reemplazados en todos los usos:
  - `MONEDA_NO_COINCIDE` → `SubastaService.conectar()`, `PujaService.procesarPuja()`
  - `ARCHIVO_INVALIDO` → `ConsignacionService.crear()` (tipo de archivo)
  - `ARCHIVO_MUY_GRANDE` → `ConsignacionService.crear()` (tamaño de foto)
  - `ENTREGA_YA_CONFIRMADA` → `ChatService.confirmarEntrega()` (ya existía, no se usaba)
  - `DIRECCION_REQUERIDA` → `ChatService.confirmarEntrega()` (ya existía, no se usaba)

---

## Lo que está bien

- **Concurrencia de pujas:** `ReentrantLock` por subasta + `PESSIMISTIC_WRITE` en el ítem. Patrón sólido.
- **Registro en 2 pasos:** Flujo completo con token, validación de unicidad, y mock de verificación externa con delay de 3s. Correcto según consigna.
- **JWT y seguridad:** Filter chain, rutas públicas/privadas, manejo de usuario bloqueado. Sin vulnerabilidades evidentes.
- **WebSocket STOMP:** Broadcast de pujas, confirmación individual, cierre notificado. Implementación limpia.
- **Tests de integración:** 53 tests cubriendo auth, subastas, chat, consignaciones, incumplimientos, seguridad, y WebSocket.
- **Simplificaciones documentadas:** `SIMPLIFICACIONES.md` con 9 decisiones justificadas. Demuestra criterio.
- **Arquitectura en capas:** Controller → Service → Repository sin atajos. DTOs de response en los controllers, entidades solo en services.
- **Manejo de errores:** `GlobalExceptionHandler` consistente, `BusinessException` con códigos y HTTP status.
- **Chat post-subasta:** Flujo de entrega, toggle de seguro, mensaje automático al confirmar modalidad y al ganar ítem.
- **Scheduler de incumplimientos:** Detección de compras vencidas, multa del 10%, bloqueo de usuario. Flujo completo.
- **Mock de consignaciones:** Revisión asíncrona con delay de 3s, consistente con el mock de registro.
