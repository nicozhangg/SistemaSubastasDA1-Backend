# Sistema de Subastas — Contexto del Backend

## Stack tecnológico
- **Framework:** Spring Boot (Java)
- **Base de datos:** [COMPLETAR: PostgreSQL / MySQL / H2 para dev]
- **ORM:** Spring Data JPA + Hibernate
- **Seguridad:** Spring Security + JWT
- **WebSocket:** Spring WebSocket (STOMP sobre SockJS)
- **Build:** Maven / Gradle [COMPLETAR]

---

## Arquitectura del proyecto

Usar arquitectura en capas estándar de Spring Boot:
```
controller/     → REST controllers + WebSocket handler
service/        → Lógica de negocio
repository/     → Spring Data JPA repositories
model/entity/   → Entidades JPA
model/dto/      → DTOs de request y response
model/enums/    → Enumeraciones
exception/      → Excepciones custom + GlobalExceptionHandler
config/         → SecurityConfig, WebSocketConfig, JwtConfig
util/           → Helpers (JwtUtil, etc.)
```

---

## Actores del sistema
- **Postor:** usuario registrado que participa en subastas
- **Rematador / Martillero:** asignado a cada subasta
- **Dueño de ítem:** quien consigna bienes para subastar
- **Empresa:** gestiona verificaciones, categorías y subastas (sistema externo)

---

## Entidades principales y relaciones

### Usuario
- id, nombre, apellido, email, password (hash), domicilio_legal, pais_origen
- categoria: ENUM(COMUN, ESPECIAL, PLATA, ORO, PLATINO)
- estado: ENUM(PENDIENTE_VERIFICACION, APROBADO, BLOQUEADO)
- foto_dni_frente (url/path), foto_dni_dorso (url/path)
- multas_pendientes: int
- relaciones: List<MedioPago>, List<Participacion>, List<Consignacion>

### MedioPago
- id, tipo: ENUM(CUENTA_BANCARIA, TARJETA_CREDITO, CHEQUE_CERTIFICADO)
- alias, moneda: ENUM(ARS, USD)
- verificado: boolean
- monto_limite (para cheques certificados)
- datos en tabla separada o JSON según tipo
- usuario (ManyToOne)

### Subasta
- id, titulo, descripcion, fecha_inicio
- categoria: ENUM(COMUN, ESPECIAL, PLATA, ORO, PLATINO)
- moneda: ENUM(ARS, USD)
- estado: ENUM(PROXIMA, ABIERTA, CERRADA)
- ubicacion, rematador (ManyToOne → Rematador)
- relaciones: List<Item> (catálogo)

### Item
- id, numero_pieza, descripcion, precio_base
- estado: ENUM(DISPONIBLE, EN_SUBASTA, VENDIDO)
- dueno_actual (string o relación)
- es_obra_arte: boolean
- artista, fecha_creacion, historia (nullable)
- relaciones: List<ImagenItem>, List<String> componentes
- subasta (ManyToOne)

### Puja
- id, monto, timestamp
- estado: ENUM(CONFIRMADA, RECHAZADA, PENDIENTE)
- usuario (ManyToOne), item (ManyToOne), subasta (ManyToOne)
- medio_pago (ManyToOne)

### Participacion (sesión del usuario en una subasta)
- id, usuario, subasta, medio_pago_seleccionado
- conectado: boolean, fecha_conexion, fecha_desconexion

### Consignacion
- id, descripcion, datos_adicionales
- estado: ENUM(PENDIENTE_REVISION, ACEPTADA, RECHAZADA, EN_SUBASTA, VENDIDA, DEVUELTA)
- acepta_pertenencia: boolean
- motivo_rechazo (nullable)
- valor_base, comisiones (nullable, asignados por empresa)
- subasta_asignada (ManyToOne, nullable)
- cuenta_destino (ManyToOne → MedioPago)
- usuario (ManyToOne)
- relaciones: List<FotoConsignacion>, Poliza, Deposito

### Multa
- id, monto (10% del valor ofertado), motivo
- fecha_generacion, fecha_limite_pago
- estado: ENUM(PENDIENTE, PAGADA, DERIVADA_JUSTICIA)
- usuario (ManyToOne), puja (ManyToOne)

### Compra
- id, item, usuario, monto_ofertado, comisiones, costo_envio, total
- moneda, medio_pago, estado_pago: ENUM(PENDIENTE, PAGADO, INCUMPLIDO)
- direccion_envio
- modalidad_entrega: ENUM(ENVIO_DOMICILIO, RETIRO_PERSONAL) — se define por chat con la empresa post-subasta
- cobertura_seguro_activa: boolean — se pone en `false` cuando el usuario retira personalmente el bien

### MensajeChat
- id, contenido, timestamp
- remitente: ENUM(USUARIO, EMPRESA)
- leido: boolean
- compra (ManyToOne) — el chat está asociado a una compra específica
- usuario (ManyToOne)

> El chat se usa post-subasta para coordinar dos cosas:
> 1. Si el ganador quiere envío a domicilio o retiro personal (si retira, pierde la cobertura del seguro)
> 2. Si el ganador quiere ampliar la cobertura del seguro pagando la diferencia del premio

---

## Reglas de negocio críticas

### Registro de usuarios (2 etapas)
1. **Paso 1:** El postor envía datos personales + fotos del DNI
   - Se simula verificación externa con un **delay fijo de 3 segundos** (mock que siempre retorna éxito)
   - Se asigna categoría automáticamente (mock: asignar COMUN por defecto)
   - Se envía email con token para completar el registro
2. **Paso 2:** El usuario usa el token del email para definir su contraseña y queda APROBADO

### Categorías
- Orden ascendente: COMUN < ESPECIAL < PLATA < ORO < PLATINO
- Para acceder a una subasta: `categoria_subasta <= categoria_usuario`
- La diversidad de medios de pago y actividad pueden mejorar la categoría (lógica a implementar)

### Medios de pago
- Un usuario debe tener **al menos un medio de pago verificado** para poder pujar
- Sin medio verificado: puede ver la subasta pero no pujar
- Tipos: cuenta bancaria (nacional/extranjera), tarjeta de crédito (nacional/extranjera), cheque certificado
- Los cheques tienen un monto máximo; las compras del usuario no pueden superar ese monto acumulado
- La verificación de medios de pago la realiza la empresa (mock: verificado = true automáticamente)

### Reglas de puja
- `puja_minima = mejor_oferta_actual + (precio_base × 0.01)`
- `puja_maxima = mejor_oferta_actual + (precio_base × 0.20)`
- **Estos límites NO aplican para subastas de categoría ORO y PLATINO**
- Las pujas son **secuenciales**: no se acepta una nueva puja hasta confirmar la anterior (usar lock/mutex por subasta)
- Un usuario **no puede estar conectado a más de una subasta a la vez**
- Si el ganador no paga → multa del 10% del valor ofertado + debe presentar fondos en 72hs
- Si no cumple → caso derivado a justicia, usuario bloqueado de la app

### Cierre de subasta
- Cuando nadie supera la mejor oferta, el último postor gana el ítem
- Si nadie puja → la empresa compra el ítem al precio base
- Al cerrar: se registra la venta, se notifica al ganador (mensaje privado con desglose: monto + comisiones + envío)

### Chat post-subasta
- Al ganar un ítem, se abre automáticamente un chat entre el ganador y la empresa vinculado a esa `Compra`
- A través del chat el usuario coordina:
  - **Modalidad de entrega:** envío a domicilio (default, costo incluido en la factura) o retiro personal
  - **Ampliación del seguro:** si el bien aún está en depósito, el usuario puede solicitar aumentar el valor de la póliza; la empresa le informa la diferencia del premio y el usuario la abona
- Si el usuario elige retiro personal: `cobertura_seguro_activa = false` al momento de confirmar el retiro
- El chat queda registrado como historial de la compra
- Endpoints necesarios:
  - `GET /compras/{id}/chat` — obtener mensajes del chat
  - `POST /compras/{id}/chat` — enviar mensaje (usuario o empresa)
  - `PATCH /compras/{id}/entrega` — confirmar modalidad de entrega (`ENVIO_DOMICILIO` / `RETIRO_PERSONAL`)

### Moneda
- Cada subasta es exclusivamente ARS o USD (no bimonetaria)
- Subastas en USD deben cancelarse en USD (transferencia o tarjeta internacional)

### Consignación de bienes
- El usuario declara que el bien le pertenece (checkbox obligatorio)
- Requiere mínimo 6 fotos
- La empresa acepta o rechaza; si rechaza → devolución con cargo al usuario
- Si acepta → el usuario decide aceptar o rechazar valor base + comisiones
- Si hay muchos objetos del mismo dueño → puede crearse una subasta "colección"
- Se contrata un seguro por bien (agrupable por dueño)
- El usuario puede ver la ubicación del bien (depósito) y la póliza de seguro
- **Mock de revisión:** igual que el mock de verificación de identidad, al crear una consignación se dispara un proceso asíncrono con delay de 3 segundos que la pasa automáticamente de `PENDIENTE_REVISION` a `ACEPTADA`, con `valorBase = precioSugerido` (o un valor por defecto si no se proporcionó) y `comisiones = valorBase × 0.10`. Esto simula la revisión de la empresa y permite demo sin llamar endpoints adicionales.

---

## WebSocket — Subastas en tiempo real

**Protocolo:** STOMP sobre WebSocket  
**Librería cliente (mobile):** a definir por el front

### Flujo de conexión
1. Usuario se autentica por REST (`/auth/login`) y obtiene JWT
2. Llama a REST `/subastas/{id}/conectar` para registrar su sesión
3. Se conecta al WebSocket enviando el JWT en el header
4. Se suscribe al topic de la subasta

### Topics y mensajes

**Suscripción del cliente:**
```
/topic/subastas/{subastaId}          → recibe actualizaciones de la subasta
/user/queue/pujas                    → recibe confirmación/rechazo de SU puja
```

**Mensajes que envía el servidor:**

`BID_UPDATED` → a todos los conectados cuando hay una nueva mejor oferta:
```json
{
  "tipo": "BID_UPDATED",
  "item_id": 501,
  "nueva_mejor_oferta": 15100.00,
  "mejor_postor_alias": "postor_***",
  "puja_minima": 15201.00,
  "puja_maxima": 17100.00,
  "timestamp": "2026-05-20T10:30:00Z"
}
```

`BID_CONFIRMED` → solo al postor que hizo la puja:
```json
{
  "tipo": "BID_CONFIRMED",
  "puja_id": 99,
  "monto": 15100.00,
  "timestamp": "2026-05-20T10:30:00Z"
}
```

`BID_REJECTED` → solo al postor que hizo la puja:
```json
{
  "tipo": "BID_REJECTED",
  "motivo": "MONTO_FUERA_DE_RANGO",
  "mensaje": "El monto debe estar entre 15100 y 17000"
}
```

`AUCTION_CLOSED` → a todos los conectados:
```json
{
  "tipo": "AUCTION_CLOSED",
  "item_id": 501,
  "ganador_alias": "postor_***",
  "monto_final": 15100.00
}
```

**Mensaje que envía el cliente para pujar:**
```
Destino: /app/subastas/{subastaId}/pujar
```
```json
{
  "item_id": 501,
  "monto": 15100.00,
  "medio_pago_id": 10
}
```

### Control de concurrencia
- Usar `synchronized` o `ReentrantLock` por subasta para garantizar que solo se procese una puja a la vez
- Mientras se procesa una puja, rechazar cualquier otra con `BID_REJECTED` + motivo `PUJA_EN_PROCESO`

---

## Integración con sistema externo

La app consume y sincroniza datos con el sistema local de la empresa mediante su propia API REST.

**Base URL:** [COMPLETAR cuando se tenga]  
**Autenticación:** [COMPLETAR]  
**Estrategia:** llamadas HTTP desde Spring (usar `RestTemplate` o `WebClient`)

Datos que vienen del sistema externo:
- Subastas, ítems, catálogos
- Rematadores
- Historial de ventas

Datos que se actualizan en el sistema externo al cerrar una subasta:
- Nuevo dueño del ítem
- Importes, comisiones
- Estado del ítem (vendido)

**Por ahora implementar con datos locales (base de datos propia). La integración con el sistema externo se agrega después.**

---

## Seguridad

- Todos los endpoints requieren JWT excepto: `POST /auth/registro/paso1`, `POST /auth/registro/paso2`, `POST /auth/login`, `GET /subastas/{id}/catalogo`
- JWT en header: `Authorization: Bearer <token>`
- Los usuarios BLOQUEADOS (deuda derivada a justicia) no pueden acceder a ningún servicio
- Los usuarios con multas PENDIENTES no pueden pujar hasta pagarlas

---

## Manejo de errores

Usar un `GlobalExceptionHandler` con `@ControllerAdvice`.  
Todas las respuestas de error siguen el schema:
```json
{
  "codigo": "ERROR_CODIGO",
  "mensaje": "Descripción legible del error"
}
```

Códigos de error de negocio a definir como constantes:
- `CATEGORIA_INSUFICIENTE`
- `SIN_MEDIO_PAGO_VERIFICADO`
- `USUARIO_YA_CONECTADO`
- `MONTO_FUERA_DE_RANGO`
- `PUJA_EN_PROCESO`
- `MULTA_PENDIENTE`
- `EMAIL_DUPLICADO`
- `TOKEN_INVALIDO`
- `USUARIO_BLOQUEADO`

---

## Lo que se construye en esta etapa (solo backend)

- [ ] Configuración inicial del proyecto Spring Boot
- [ ] Entidades JPA y relaciones
- [ ] Repositorios Spring Data
- [ ] Servicios con lógica de negocio
- [ ] Controllers REST según endpoints definidos en `endpoints.md`
- [ ] WebSocket con STOMP para pujas en tiempo real
- [ ] JWT (generación y validación)
- [ ] GlobalExceptionHandler
- [ ] Mock de verificación externa (delay 3 seg, siempre exitoso)
- [ ] Chat post-subasta (entidad MensajeChat, endpoints GET/POST, lógica de retiro personal y seguro)
- [ ] Datos de prueba (data.sql o @Bean CommandLineRunner)

**No trabajar el frontend en esta etapa.**