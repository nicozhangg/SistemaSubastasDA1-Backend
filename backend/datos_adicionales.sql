-- ============================================================
-- Datos de prueba adicionales — Sistema de Subastas
-- Ejecutar en MySQL Workbench sobre la base 'subastas'
-- Requiere que el DataLoader ya haya corrido (juan y maria existen)
-- ============================================================

USE subastas;

-- Reutilizamos el hash BCrypt de juan (password123)
SET @pass = (SELECT `password` FROM usuarios WHERE email = 'juan@test.com' LIMIT 1);

-- ── USUARIOS ─────────────────────────────────────────────────────────────────

INSERT INTO usuarios (nombre, apellido, email, `password`, domicilio_legal, pais_origen,
                      categoria, estado, numero_dni, multas_pendientes, fecha_registro)
VALUES
  ('Pedro', 'Ramírez', 'pedro@test.com', @pass,
   'San Martín 890, Rosario',  'Argentina', 'ESPECIAL', 'APROBADO',  '55512345', 1, NOW()),
  ('Laura', 'Mendez',  'laura@test.com', @pass,
   'Puerto Madero 100, CABA', 'Argentina', 'PLATINO',  'APROBADO',  '33398765', 0, NOW()),
  ('Diego', 'Torres',  'diego@test.com', @pass,
   'Quilmes 300, GBA',        'Argentina', 'COMUN',    'BLOQUEADO', '11122233', 2, NOW());

-- ── MEDIOS DE PAGO ───────────────────────────────────────────────────────────

-- Pedro: cheque certificado ARS ($500k límite)
INSERT INTO medios_pago (tipo, alias, moneda, verificado, monto_limite, usuario_id)
VALUES ('CHEQUE_CERTIFICADO', 'Cheque Certificado BBVA', 'ARS', 1, 500000.00,
        (SELECT id FROM usuarios WHERE email = 'pedro@test.com'));

-- Laura: cuenta bancaria USD
INSERT INTO medios_pago (tipo, alias, moneda, verificado, banco, numero_cuenta, tipo_cuenta, cbu, usuario_id)
VALUES ('CUENTA_BANCARIA', 'Cuenta USD Internacional', 'USD', 1,
        'HSBC Argentina', '00800012345', 'extranjera', '1500008000080001234501',
        (SELECT id FROM usuarios WHERE email = 'laura@test.com'));

-- Laura: tarjeta crédito ARS
INSERT INTO medios_pago (tipo, alias, moneda, verificado, numero_tarjeta, titular, vencimiento, tipo_tarjeta, usuario_id)
VALUES ('TARJETA_CREDITO', 'Mastercard Platinum', 'ARS', 1,
        '5500005555555559', 'LAURA MENDEZ', '09/28', 'nacional',
        (SELECT id FROM usuarios WHERE email = 'laura@test.com'));

-- ── REMATADORES ──────────────────────────────────────────────────────────────

INSERT INTO rematadores (nombre, apellido, email, matricula)
VALUES ('Sofía', 'Vidal', 'svidal@subastas.com', 'MAT-002');

-- ── SUBASTA 3: ABIERTA — Exclusiva ORO (solo ORO y PLATINO pueden acceder) ───

INSERT INTO subastas (titulo, descripcion, fecha_inicio, fecha_fin, categoria, moneda, estado, ubicacion, rematador_id)
VALUES ('Subasta Exclusiva ORO - Joyas y Reliquias',
        'Piezas únicas de joyería fina y antigüedades de alto valor',
        DATE_SUB(NOW(), INTERVAL 30 MINUTE),
        DATE_ADD(NOW(), INTERVAL 3 HOUR),
        'ORO', 'ARS', 'ABIERTA',
        'Palacio Sans Souci, CABA',
        (SELECT id FROM rematadores WHERE matricula = 'MAT-002'));
SET @sub3 = LAST_INSERT_ID();

-- ── SUBASTA 4: CERRADA — con compra y chat de ejemplo ────────────────────────

INSERT INTO subastas (titulo, descripcion, fecha_inicio, fecha_fin, categoria, moneda, estado, ubicacion, rematador_id)
VALUES ('Subasta Cerrada - Muebles Clásicos',
        'Muebles y objetos de época, estilos inglés y francés',
        DATE_SUB(NOW(), INTERVAL 3 DAY),
        DATE_SUB(NOW(), INTERVAL 2 DAY),
        'COMUN', 'ARS', 'CERRADA',
        'Salón Borges, Palermo',
        (SELECT id FROM rematadores WHERE matricula = 'MAT-001'));
SET @sub4 = LAST_INSERT_ID();

-- ── PÓLIZAS ───────────────────────────────────────────────────────────────────

INSERT INTO polizas (aseguradora_nombre, aseguradora_contacto, valor_asegurado, prima, vigencia_desde, vigencia_hasta)
VALUES ('Zurich Argentina', '0800-333-9874', 500000.00, 8000.00,
        DATE_SUB(CURDATE(), INTERVAL 1 MONTH), DATE_ADD(CURDATE(), INTERVAL 11 MONTH));
SET @pol_item3 = LAST_INSERT_ID();

INSERT INTO polizas (aseguradora_nombre, aseguradora_contacto, valor_asegurado, prima, vigencia_desde, vigencia_hasta)
VALUES ('Seguros Clásicos S.A.', '0800-111-2222', 200000.00, 3500.00,
        DATE_SUB(CURDATE(), INTERVAL 1 MONTH), DATE_ADD(CURDATE(), INTERVAL 11 MONTH));
SET @pol_item5 = LAST_INSERT_ID();

-- ── ÍTEMS ─────────────────────────────────────────────────────────────────────

-- Subasta ORO — Collar de diamantes
INSERT INTO items (numero_pieza, descripcion, precio_base, estado, dueno_actual, es_obra_arte, ubicacion_fisica, poliza_id, subasta_id)
VALUES ('J-001', 'Collar de diamantes y rubíes - Oro 18k - circa 1920', 350000.00, 'EN_SUBASTA',
        'Sucesión Beltrán', 0, 'Caja Fuerte - Palacio Sans Souci', @pol_item3, @sub3);
SET @item3 = LAST_INSERT_ID();

-- Subasta ORO — Reloj Rolex
INSERT INTO items (numero_pieza, descripcion, precio_base, estado, dueno_actual, es_obra_arte, ubicacion_fisica, subasta_id)
VALUES ('J-002', 'Reloj Rolex Datejust 1968 - Acero y Oro', 280000.00, 'EN_SUBASTA',
        'Colección privada', 0, 'Caja Fuerte - Palacio Sans Souci', @sub3);

-- Subasta CERRADA — Escritorio victoriano (ya vendido)
INSERT INTO items (numero_pieza, descripcion, precio_base, estado, dueno_actual, es_obra_arte, ubicacion_fisica, poliza_id, subasta_id)
VALUES ('M-001', 'Escritorio inglés estilo victoriano - Caoba - 1890', 120000.00, 'VENDIDO',
        'Juan Pérez', 0, 'Depósito Central - Av. Industria 4500, Dock Sud', @pol_item5, @sub4);
SET @item5 = LAST_INSERT_ID();

-- ── VARIABLES DE REFERENCIA A DATOS EXISTENTES ───────────────────────────────

SET @sub1  = (SELECT id FROM subastas WHERE titulo LIKE 'Subasta de Arte Argentino%' LIMIT 1);
SET @item1 = (SELECT id FROM items WHERE numero_pieza = 'P-001' LIMIT 1);
SET @juan  = (SELECT id FROM usuarios WHERE email = 'juan@test.com' LIMIT 1);
SET @maria = (SELECT id FROM usuarios WHERE email = 'maria@test.com' LIMIT 1);
SET @pedro = (SELECT id FROM usuarios WHERE email = 'pedro@test.com' LIMIT 1);
SET @mp1   = (SELECT id FROM medios_pago WHERE alias = 'Cuenta Principal' LIMIT 1);
SET @mp2   = (SELECT id FROM medios_pago WHERE alias = 'Cuenta Corriente ARS' LIMIT 1);
SET @mp4   = (SELECT id FROM medios_pago WHERE alias = 'Cheque Certificado BBVA' LIMIT 1);

-- ── PUJAS EN SUBASTA 1 — historial escalando precio en P-001 ─────────────────

INSERT INTO pujas (monto, `timestamp`, estado, usuario_id, item_id, subasta_id, medio_pago_id)
VALUES
  (51000.00, DATE_SUB(NOW(), INTERVAL 45 MINUTE), 'CONFIRMADA', @juan,  @item1, @sub1, @mp1),
  (52500.00, DATE_SUB(NOW(), INTERVAL 30 MINUTE), 'CONFIRMADA', @maria, @item1, @sub1, @mp2),
  (54000.00, DATE_SUB(NOW(), INTERVAL 15 MINUTE), 'CONFIRMADA', @juan,  @item1, @sub1, @mp1),
  (49000.00, DATE_SUB(NOW(), INTERVAL 10 MINUTE), 'RECHAZADA',  @maria, @item1, @sub1, @mp2);

-- Reflejar mejor oferta en el item
UPDATE items SET mejor_oferta = 54000.00, mejor_postor_id = @juan WHERE id = @item1;

-- ── PUJAS EN SUBASTA 3 — ORO ──────────────────────────────────────────────────

INSERT INTO pujas (monto, `timestamp`, estado, usuario_id, item_id, subasta_id, medio_pago_id)
VALUES (353500.00, DATE_SUB(NOW(), INTERVAL 20 MINUTE), 'CONFIRMADA', @maria, @item3, @sub3, @mp2);

UPDATE items SET mejor_oferta = 353500.00, mejor_postor_id = @maria WHERE id = @item3;

-- ── PUJAS EN SUBASTA 4 — CERRADA ─────────────────────────────────────────────

INSERT INTO pujas (monto, `timestamp`, estado, usuario_id, item_id, subasta_id, medio_pago_id)
VALUES
  (132000.00, DATE_ADD(DATE_SUB(NOW(), INTERVAL 2 DAY), INTERVAL 10 MINUTE), 'CONFIRMADA', @maria, @item5, @sub4, @mp2),
  (145000.00, DATE_ADD(DATE_SUB(NOW(), INTERVAL 2 DAY), INTERVAL 25 MINUTE), 'CONFIRMADA', @juan,  @item5, @sub4, @mp1);

-- ── PARTICIPACIONES ───────────────────────────────────────────────────────────

INSERT INTO participaciones (usuario_id, subasta_id, medio_pago_id, conectado, fecha_conexion)
VALUES
  (@juan,  @sub1, @mp1, 1, DATE_SUB(NOW(), INTERVAL 60 MINUTE)),
  (@maria, @sub1, @mp2, 1, DATE_SUB(NOW(), INTERVAL 50 MINUTE));

-- ── COMPRA — Juan ganó el escritorio victoriano ───────────────────────────────

INSERT INTO compras (item_id, usuario_id, monto_ofertado, comisiones, costo_envio, total,
                     moneda, medio_pago_id, estado_pago, direccion_envio,
                     modalidad_entrega, cobertura_seguro_activa, fecha_limite_pago)
VALUES (@item5, @juan, 145000.00, 14500.00, 3500.00, 163000.00,
        'ARS', @mp1, 'PENDIENTE',
        'Av. Corrientes 1234, CABA',
        'ENVIO_DOMICILIO', 1,
        DATE_ADD(NOW(), INTERVAL 1 DAY));
SET @compra1 = LAST_INSERT_ID();

-- ── CHAT POST-SUBASTA ─────────────────────────────────────────────────────────

INSERT INTO mensajes_chat (contenido, `timestamp`, remitente, leido, compra_id, usuario_id)
VALUES
  ('¡Felicitaciones por ganar el escritorio victoriano! El total a pagar es $163.000 (monto $145.000 + comisión $14.500 + envío $3.500). ¿Prefiere envío a domicilio o retiro personal?',
   DATE_SUB(NOW(), INTERVAL 2 DAY),
   'EMPRESA', 1, @compra1, @juan),

  ('Prefiero envío a domicilio. La dirección es Av. Corrientes 1234, CABA.',
   DATE_ADD(DATE_SUB(NOW(), INTERVAL 2 DAY), INTERVAL 30 MINUTE),
   'USUARIO', 1, @compra1, @juan),

  ('Confirmamos envío a domicilio. La cobertura del seguro está activa por $200.000. ¿Desea ampliar la cobertura? La diferencia de prima sería de $1.800 adicionales.',
   DATE_SUB(NOW(), INTERVAL 20 HOUR),
   'EMPRESA', 1, @compra1, @juan),

  ('No, la cobertura actual está bien. ¿Cuándo estiman que llega el envío?',
   DATE_SUB(NOW(), INTERVAL 18 HOUR),
   'USUARIO', 0, @compra1, @juan);

-- ── MULTA — Pedro no pagó una compra anterior ────────────────────────────────

INSERT INTO multas (monto, motivo, fecha_generacion, fecha_limite_pago, estado, usuario_id)
VALUES (13200.00,
        'Incumplimiento de pago — compra no abonada en plazo de 72hs',
        DATE_SUB(NOW(), INTERVAL 5 DAY),
        DATE_ADD(NOW(), INTERVAL 2 DAY),
        'PENDIENTE', @pedro);

-- ── CONSIGNACIÓN RECHAZADA — Pedro (para testear flujo de rechazo) ────────────

INSERT INTO consignaciones (descripcion, datos_adicionales, estado, acepta_pertenencia,
                             motivo_rechazo, precio_sugerido, usuario_id, cuenta_destino_id)
VALUES ('Cuadro sin firma - Acuarela sobre papel - 40x30cm',
        'Adquirido en feria de antigüedades, sin procedencia documentada',
        'RECHAZADA', 1,
        'No se pudo verificar la autenticidad ni la procedencia de la obra',
        15000.00, @pedro, @mp4);
SET @cons3 = LAST_INSERT_ID();

INSERT INTO fotos_consignacion (url, orden, consignacion_id) VALUES
  ('uploads/consignaciones/cuadro-sin-firma-foto-1.jpg', 1, @cons3),
  ('uploads/consignaciones/cuadro-sin-firma-foto-2.jpg', 2, @cons3),
  ('uploads/consignaciones/cuadro-sin-firma-foto-3.jpg', 3, @cons3),
  ('uploads/consignaciones/cuadro-sin-firma-foto-4.jpg', 4, @cons3),
  ('uploads/consignaciones/cuadro-sin-firma-foto-5.jpg', 5, @cons3),
  ('uploads/consignaciones/cuadro-sin-firma-foto-6.jpg', 6, @cons3);

-- ── RESUMEN ───────────────────────────────────────────────────────────────────
SELECT 'Datos cargados correctamente' AS resultado;

SELECT email, categoria, estado, multas_pendientes FROM usuarios ORDER BY id;
