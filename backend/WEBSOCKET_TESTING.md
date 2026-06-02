# Prueba WebSocket — Sistema de Subastas

## Prerequisitos

- Backend corriendo en `http://localhost:8080` (`mvn spring-boot:run`)
- MySQL corriendo localmente con la base `subastas`
- Archivo `test-websocket.html` en la raíz del proyecto

---

## Datos de prueba

| Recurso | ID | Detalle |
|---------|-----|---------|
| **Juan Pérez** | — | `juan@test.com` / `password123` — categoría PLATA |
| **María García** | — | `maria@test.com` / `password123` — categoría ORO |
| **MedioPago Juan** | 1 | Cuenta Bancaria Banco Nación, ARS |
| **MedioPago María** | 2 | Cuenta Corriente Banco Galicia, ARS |
| **Subasta activa** | 1 | "Subasta de Arte Argentino" — ABIERTA, ARS |
| **Item 1** | 1 | "Óleo sobre tela" — Precio base: $50.000 ARS |
| **Item 2** | 2 | "Escultura de bronce" — Precio base: $80.000 ARS |

> Los IDs se asignan en orden al arrancar el servidor. Si la base tiene datos previos, los IDs pueden diferir.

---

## Opción A — Página HTML (recomendado)

Abrir `test-websocket.html` directamente en el browser (doble clic).

### PASO 1 — Login

- Ingresar email y password → **Login**
- El JWT queda guardado automáticamente en el campo de texto

### PASO 2 — Conectar a la subasta (REST)

- Subasta ID: `1` — Medio de pago ID: `1` (Juan) o `2` (María)
- Clic en **Conectar (REST)**
- Esto registra la participación del usuario en la subasta. Es obligatorio antes de pujar.

### PASO 3 — Conectar WebSocket

- Clic en **Conectar WebSocket**
- El log debe mostrar:
  ```
  WebSocket CONECTADO
  Suscripto a /topic/subastas/1
  Suscripto a /user/queue/pujas
  ```

### PASO 4 — Pujar

- Item ID: `1`, Monto: `55000`, Medio de pago ID: `1`
- Clic en **Pujar**

### Para simular dos usuarios

Abrir el mismo HTML en **dos tabs** del browser, hacer login con distintos usuarios y conectar ambos a la subasta 1. Las pujas de uno aparecen en el log del otro.

---

## Opción B — Hoppscotch / cliente STOMP manual

### PASO 1 — Login (REST)

```
POST http://localhost:8080/api/v1/auth/login
Content-Type: application/json

{ "email": "juan@test.com", "password": "password123" }
```

Guardar el campo `token` de la respuesta.

### PASO 2 — Conectar a la subasta (REST)

```
POST http://localhost:8080/api/v1/subastas/1/conectar
Authorization: Bearer <token>
Content-Type: application/json

{ "medioPagoId": 1 }
```

### PASO 3 — Conectar WebSocket

URL:
```
ws://localhost:8080/ws/websocket
```

Frame CONNECT:
```
CONNECT
Authorization:Bearer <token>
accept-version:1.2
heart-beat:0,0

\0
```

### PASO 4 — Suscribirse

Topic público (todos los conectados):
```
SUBSCRIBE
id:sub-0
destination:/topic/subastas/1

\0
```

Cola privada (solo para el usuario autenticado):
```
SUBSCRIBE
id:sub-1
destination:/user/queue/pujas

\0
```

### PASO 5 — Pujar

```
SEND
destination:/app/subastas/1/pujar
content-type:application/json

{"itemId":1,"monto":55000.00,"medioPagoId":1}
\0
```

> Campos en **camelCase** — el servidor espera `itemId` y `medioPagoId`, no `item_id` / `medio_pago_id`.

---

## Flujo esperado

```
Juan (Tab 1)                    Servidor                   María (Tab 2)
     |                              |                             |
     |--- puja $55.000 ------------>|                             |
     |<-- BID_CONFIRMED (privado) --|                             |
     |                              |-- BID_UPDATED (público) --->|
     |                              |                             |
     |                              |<-- puja $60.000 ------------|
     |<-- BID_UPDATED (público) ----|                             |
     |                              |-- BID_CONFIRMED (privado) ->|
     |                              |                             |
     |--- puja $50.000 (inválida) ->|                             |
     |<-- BID_REJECTED (privado) ---|                             |
```

### Rangos de puja para Item 1 (precio base $50.000)

| Situación | Mínimo | Máximo |
|-----------|--------|--------|
| Sin pujas previas | $50.500 | $60.000 |
| Después de puja de $55.000 | $55.550 | $66.000 |
| Después de puja de $60.000 | $60.600 | $72.000 |

> Fórmula: `mín = mejor_oferta + precio_base × 0.01` / `máx = mejor_oferta + precio_base × 0.20`  
> Estos límites **no aplican** para subastas ORO y PLATINO.

---

## Mensajes que emite el servidor

**BID_UPDATED** (topic público — todos los conectados):
```json
{
  "tipo": "BID_UPDATED",
  "item_id": 1,
  "nueva_mejor_oferta": 55000.00,
  "mejor_postor_alias": "postor_***",
  "puja_minima": 55500.00,
  "puja_maxima": 65000.00,
  "timestamp": "..."
}
```

**BID_CONFIRMED** (privado — solo al que pujó):
```json
{
  "tipo": "BID_CONFIRMED",
  "puja_id": 1,
  "monto": 55000.00,
  "timestamp": "..."
}
```

**BID_REJECTED** (privado — solo al que pujó):
```json
{
  "tipo": "BID_REJECTED",
  "motivo": "MONTO_FUERA_DE_RANGO",
  "mensaje": "El monto debe estar entre 55500 y 65000"
}
```

---

## Errores frecuentes

| Error | Causa | Solución |
|-------|-------|----------|
| `Failed to fetch` | CORS bloqueado o servidor caído | Verificar que el servidor esté corriendo y que `cors.allowed-origins` incluya `null` en `application.properties` |
| `Status: 403` en conectar | Token vencido o usuario bloqueado | Hacer login de nuevo |
| `BID_REJECTED: USUARIO_NO_CONECTADO` | Se saltó el PASO 2 (REST) | Llamar primero a `POST /api/v1/subastas/1/conectar` |
| `BID_REJECTED: SIN_MEDIO_PAGO_VERIFICADO` | El medio de pago no está verificado | Usar medioPagoId 1 (Juan) o 2 (María) que ya vienen verificados |
| `BID_REJECTED: PUJA_EN_PROCESO` | Otra puja se está procesando | Esperar un momento y reintentar |
| WebSocket no conecta | JWT inválido en el header CONNECT | Verificar que el token no esté vencido |
