# Endpoints REST — Sistema de Subastas

Base URL: `/api/v1`  
Auth: JWT Bearer (excepto los marcados con 🔓)

---

## AUTENTICACIÓN

### 🔓 POST /auth/registro/paso1
Registro inicial. Recibe datos personales + fotos DNI (multipart/form-data).  
Simula verificación externa (delay 3s, siempre exitoso). Envía email con token.
- Body: `nombre, apellido, email, domicilio_legal, pais_origen, foto_dni_frente, foto_dni_dorso`
- 201: `{ usuario_id, estado: "pendiente_verificacion", mensaje }`
- 400: datos inválidos
- 409: email ya registrado

### 🔓 POST /auth/registro/paso2
Completa el registro usando el token recibido por email. Define la contraseña.
- Body: `{ token_email, email, password }`
- 200: `{ usuario_id, email, categoria, token_acceso }`
- 400: token inválido o expirado
- 403: cuenta aún no aprobada

### 🔓 POST /auth/login
- Body: `{ email, password }`
- 200: `{ token_acceso, token_refresh, usuario }`
- 401: credenciales inválidas
- 403: cuenta bloqueada

### POST /auth/logout
- 200: `{ mensaje }`
- 401: no autenticado

---

## USUARIOS

### GET /usuarios/perfil
Retorna el perfil del usuario autenticado.
- 200: `Usuario { id, nombre, apellido, email, categoria, estado, domicilio_legal, pais_origen, fecha_registro, multas_pendientes }`
- 401: no autenticado

---

## MEDIOS DE PAGO

### GET /usuarios/medios-pago
Lista los medios de pago del usuario.
- 200: `[ MedioPago ]`

### POST /usuarios/medios-pago
Agrega un nuevo medio de pago.
- Body: `{ tipo, alias, moneda, datos_banco? | datos_tarjeta? | monto_cheque? }`
- tipos: `cuenta_bancaria | tarjeta_credito | cheque_certificado`
- 201: `{ id, tipo, alias, verificado: false, mensaje }`
- 400: datos inválidos

### DELETE /usuarios/medios-pago/{id}
Elimina un medio de pago.
- 200: `{ mensaje }`
- 404: no encontrado
- 409: en uso en subasta activa

---

## SUBASTAS

### GET /subastas
Lista subastas accesibles según la categoría del usuario.
- Query params: `estado (proxima|abierta|cerrada)`, `categoria`, `moneda`, `page`
- 200: `{ data: [ Subasta ], total, page }`

### GET /subastas/{id}
Detalle de una subasta.
- 200: `Subasta`
- 403: categoría insuficiente
- 404: no encontrada

### POST /subastas/{id}/conectar
Ingresa a una subasta abierta. Solo una subasta a la vez por usuario.
- Body: `{ medio_pago_id }`
- 200: `{ sesion_id, item_actual: EstadoPuja, streaming_url }`
- 400: ya conectado a otra subasta
- 403: sin medio de pago verificado o categoría insuficiente
- 404: subasta no encontrada o no está abierta

### POST /subastas/{id}/desconectar
Desconecta al usuario de la subasta.
- 200: `{ mensaje }`
- 400: no estaba conectado

---

## PUJAS

### GET /subastas/{id}/pujas/estado
Estado actual del ítem en subasta: mejor oferta, límites de puja.
- Puja mínima = mejor_oferta + (precio_base × 0.01)
- Puja máxima = mejor_oferta + (precio_base × 0.20)
- ⚠️ Límites NO aplican en subastas ORO y PLATINO
- 200: `EstadoPuja { item_id, descripcion, precio_base, mejor_oferta, mejor_postor_alias, puja_minima, puja_maxima, moneda, estado_item }`
- 403: usuario no conectado a esta subasta

### POST /subastas/{id}/pujas
Realiza una puja. No se acepta otra hasta confirmar la anterior.
- Body: `{ item_id, monto, medio_pago_id }`
- 201: `{ puja_id, monto, estado: "confirmada", timestamp, nueva_mejor_oferta }`
- 400: monto fuera de rango
- 403: sin fondos, multa pendiente, o no conectado
- 409: hay una puja en proceso (esperar confirmación)

### GET /subastas/{id}/pujas/historial
Historial de pujas en orden cronológico.
- Query params: `item_id?`, `solo_propias (boolean)?`
- 200: `[ Puja { puja_id, monto, postor_alias, timestamp, estado } ]`

### GET /usuarios/compras/{compra_id}
Detalle de una compra ganada: monto + comisiones + envío.
- 200: `{ compra_id, item, monto_ofertado, comisiones, costo_envio, total, moneda, medio_pago, estado_pago, direccion_envio }`
- 403: no es tu compra
- 404: no encontrada

---

## CATÁLOGO

### 🔓 GET /subastas/{id}/catalogo
Lista los ítems de la subasta. El precio_base solo se muestra a usuarios registrados.
- 200: `[ { item_id, numero_pieza, descripcion, imagenes[], precio_base (nullable), estado, es_obra_arte } ]`

### GET /items/{item_id}
Detalle completo de un ítem.
- 200: `Item { item_id, numero_pieza, descripcion, precio_base, estado, dueno_actual, imagenes[], componentes[], es_obra_arte, artista?, fecha_creacion?, historia? }`
- 404: no encontrado

### GET /items/{item_id}/imagenes
Imágenes de un ítem.
- 200: `[ { imagen_id, url, orden, descripcion } ]`

---

## CONSIGNACIÓN

### GET /consignaciones
Lista las consignaciones del usuario autenticado.
- 200: `[ Consignacion ]`

### POST /consignaciones
Solicita incluir un bien en subasta (multipart/form-data).
- Body: `descripcion, fotos[] (mín. 6), datos_adicionales?, acepta_pertenencia: true, cuenta_destino_id`
- 201: `{ consignacion_id, estado: "pendiente_revision", mensaje }`
- 400: menos de 6 fotos, falta declaración de pertenencia

### POST /consignaciones/{id}/aceptar-condiciones
Acepta el valor base y comisiones propuestos por la empresa.
- 200: `{ mensaje, subasta_id, fecha_subasta }`
- 400: estado no permite esta acción
- 404: no encontrada

### POST /consignaciones/{id}/rechazar-condiciones
Rechaza las condiciones. El bien se devuelve con cargo al usuario.
- 200: `{ mensaje, gastos_estimados }`

### GET /consignaciones/{id}/ubicacion
Retorna el depósito donde se encuentra el bien.
- 200: `{ deposito: { nombre, direccion, coordenadas: { lat, lng } }, fecha_ingreso, estado_fisico }`
- 403: el bien no le pertenece

### GET /consignaciones/{id}/poliza
Datos de la póliza de seguro del bien.
- 200: `{ poliza_id, aseguradora: { nombre, contacto }, valor_asegurado, prima, vigencia_desde, vigencia_hasta, bienes_cubiertos[] }`
- 403: no autorizado

---

## MÉTRICAS E HISTORIAL

### GET /usuarios/metricas
Estadísticas del usuario.
- 200: `Metrica { total_subastas_asistidas, total_ganadas, total_ofertado, total_pagado, categoria_mas_participada, subastas_por_categoria, porcentaje_victorias }`

### GET /usuarios/participaciones
Historial de participaciones en subastas.
- Query params: `resultado (ganada|perdida|en_curso)`, `desde`, `hasta`
- 200: `[ { subasta_id, titulo, fecha, categoria, items_pujados, items_ganados, monto_total_ofertado } ]`

### GET /usuarios/multas
Lista de multas del usuario (10% del valor ofertado al no pagar).
- 200: `[ Multa { multa_id, monto, motivo, fecha_generacion, fecha_limite_pago, estado } ]`

### POST /usuarios/multas/{id}/pagar
Paga una multa pendiente.
- Body: `{ medio_pago_id }`
- 200: `{ multa_id, estado: "pagada", puede_participar_nuevamente: true }`
- 400: pago fallido
- 404: multa no encontrada

---

## WEBSOCKET — Pujas en tiempo real

**Protocolo:** STOMP sobre WebSocket  
**Endpoint de conexión:** `ws://host/ws` (con JWT en header)

### Suscripciones del cliente
- `/topic/subastas/{subastaId}` → actualizaciones de la subasta (todos los conectados)
- `/user/queue/pujas` → confirmación/rechazo de la propia puja

### Para pujar (cliente → servidor)
**Destino:** `/app/subastas/{subastaId}/pujar`
```json
{ "item_id": 501, "monto": 15100.00, "medio_pago_id": 10 }
```

### Mensajes del servidor

**BID_UPDATED** (broadcast a todos):
```json
{ "tipo": "BID_UPDATED", "item_id": 501, "nueva_mejor_oferta": 15100.00,
  "mejor_postor_alias": "postor_***", "puja_minima": 15201.00, "puja_maxima": 17100.00, "timestamp": "..." }
```

**BID_CONFIRMED** (solo al postor):
```json
{ "tipo": "BID_CONFIRMED", "puja_id": 99, "monto": 15100.00, "timestamp": "..." }
```

**BID_REJECTED** (solo al postor):
```json
{ "tipo": "BID_REJECTED", "motivo": "MONTO_FUERA_DE_RANGO", "mensaje": "El monto debe estar entre 15100 y 17000" }
```

**AUCTION_CLOSED** (broadcast a todos):
```json
{ "tipo": "AUCTION_CLOSED", "item_id": 501, "ganador_alias": "postor_***", "monto_final": 15100.00 }
```
