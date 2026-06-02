# Auditoría del Backend — ¿Listo para MySQL + Frontend?

## Veredicto: NO LISTO (faltan 4 endpoints + 1 bug)

Lo que está hecho es sólido, pero hay gaps que la consigna pide explícitamente en `endpoints.md`.

---

## Lo que SÍ está listo

- **31 de 35 endpoints** implementados y funcionales
- **15 entidades JPA** alineadas al 100% con `EstructuraActual.sql`
- **Toda la lógica de negocio crítica**: registro 2 pasos, pujas con límites, concurrencia con `ReentrantLock`, consignación con mock 3s, cierre de subasta, multas, chat post-subasta
- **Seguridad**: JWT, CORS, endpoints públicos correctos, usuarios bloqueados filtrados
- **WebSocket STOMP**: configurado con autenticación JWT, topics correctos
- **55+ tests de integración** que cubren auth, subastas, pujas, chat, consignación, seguridad
- **Config MySQL**: perfil `prod` listo en `application-prod.properties` con `ddl-auto=validate`
- **DataLoader** con datos de prueba

---

## Lo que FALTA

### 4 endpoints faltantes (definidos en `endpoints.md`, no implementados)

| # | Endpoint | Descripción | Dificultad |
|---|----------|-------------|------------|
| 1 | `GET /consignaciones/{id}/ubicacion` | Ubicación del depósito del bien | Fácil — la entidad `Deposito` ya existe |
| 2 | `GET /consignaciones/{id}/poliza` | Póliza de seguro del bien | Fácil — la entidad `Poliza` ya existe |
| 3 | `GET /usuarios/metricas` | Estadísticas del usuario (subastas, victorias, montos) | Media — requiere queries de agregación |
| 4 | `GET /usuarios/participaciones` | Historial de participaciones con filtros | Media — requiere queries con filtros dinámicos |

### 1 bug: idempotencia del scheduler de incumplimientos

- **Archivo:** `IncumplimientoService.java`
- **Problema:** El test `procesarComprasVencidas_dos_veces_no_genera_multa_duplicada` falla — probablemente por un issue de flush/cache de JPA dentro de la misma transacción del test.
- **Impacto:** Bajo en producción (single instance, scheduler cada 5min), pero el test documenta el problema.
- **Fix:** Agregar un check explícito antes de generar la multa, o corregir el manejo transaccional del test.

---

## Plan de acción para completar

### 1. Endpoints de consignación (~30 min)
- Agregar `GET /{id}/ubicacion` y `GET /{id}/poliza` en `ConsignacionController`
- Crear DTOs `UbicacionResponse` y `PolizaResponse`
- Agregar métodos en `ConsignacionService`

### 2. Endpoints de métricas/participaciones (~1 hora)
- Agregar `GET /usuarios/metricas` y `GET /usuarios/participaciones` en `UsuarioController`
- Crear DTOs `MetricasResponse` y `ParticipacionHistorialResponse`
- Agregar queries de agregación en repositorios
- Agregar filtros (resultado, fecha desde/hasta)

### 3. Fix del bug de idempotencia (~15 min)
- Opción simple: el query ya filtra por PENDIENTE, así que en producción no hay race condition real. Ajustar el test o agregar `entityManager.flush()` entre las dos llamadas.

### 4. Verificación
- Correr `mvn test` y confirmar que todos los tests pasan
- Verificar que la app levanta limpia con perfil H2 (dev)

**Estimación total: ~2 horas**

---

## Sobre MySQL

La configuración ya está lista. Solo se necesita:
1. Tener MySQL corriendo con la base creada
2. Ejecutar con `-Dspring.profiles.active=prod` (o configurar las env vars `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`)
3. El esquema se valida automáticamente (`ddl-auto=validate`) — hay que ejecutar el DDL de `EstructuraActual.sql` una vez

No hay bloqueantes para la conexión a MySQL más allá de completar los endpoints faltantes.
