-- ============================================================
-- Esquema actual del Sistema de Subastas DA1
-- Generado a partir de las entidades JPA (MySQL / InnoDB)
-- Hibernate gestiona el DDL automáticamente:
--   dev  → spring.jpa.hibernate.ddl-auto=create-drop  (H2)
--   prod → spring.jpa.hibernate.ddl-auto=validate     (MySQL)
-- Este archivo es referencia documental, no se ejecuta en arranque.
-- ============================================================

-- ------------------------------------------------------------
-- Tablas independientes (sin FK)
-- ------------------------------------------------------------

CREATE TABLE rematadores (
    id          BIGINT          NOT NULL AUTO_INCREMENT,
    nombre      VARCHAR(255)    NOT NULL,
    apellido    VARCHAR(255)    NOT NULL,
    email       VARCHAR(255)    NOT NULL UNIQUE,
    telefono    VARCHAR(255),
    matricula   VARCHAR(255),
    PRIMARY KEY (id)
);

CREATE TABLE usuarios (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    nombre              VARCHAR(255)    NOT NULL,
    apellido            VARCHAR(255)    NOT NULL,
    email               VARCHAR(255)    NOT NULL UNIQUE,
    password            VARCHAR(255),
    domicilio_legal     VARCHAR(255),
    pais_origen         VARCHAR(255),
    categoria           VARCHAR(20)     NOT NULL,   -- COMUN | ESPECIAL | PLATA | ORO | PLATINO
    estado              VARCHAR(30)     NOT NULL,   -- ACTIVO | BLOQUEADO | PENDIENTE_VERIFICACION
    numero_dni          VARCHAR(255)    UNIQUE,
    foto_dni_frente     VARCHAR(255),
    foto_dni_dorso      VARCHAR(255),
    multas_pendientes   INT             NOT NULL DEFAULT 0,
    fecha_registro      DATETIME(6),
    token_email         VARCHAR(255),
    token_expiracion    DATETIME(6),
    PRIMARY KEY (id)
);

CREATE TABLE polizas (
    id                   BIGINT          NOT NULL AUTO_INCREMENT,
    aseguradora_nombre   VARCHAR(255)    NOT NULL,
    aseguradora_contacto VARCHAR(255),
    valor_asegurado      DECIMAL(15,2),
    prima                DECIMAL(15,2),
    vigencia_desde       DATE,
    vigencia_hasta       DATE,
    PRIMARY KEY (id)
);

CREATE TABLE poliza_bienes (
    poliza_id   BIGINT          NOT NULL,
    bien        VARCHAR(255),
    CONSTRAINT fk_poliza_bienes_poliza FOREIGN KEY (poliza_id) REFERENCES polizas (id)
);

CREATE TABLE depositos (
    id            BIGINT          NOT NULL AUTO_INCREMENT,
    nombre        VARCHAR(255)    NOT NULL,
    direccion     VARCHAR(255)    NOT NULL,
    latitud       DOUBLE,
    longitud      DOUBLE,
    fecha_ingreso DATETIME(6),
    estado_fisico VARCHAR(255),
    PRIMARY KEY (id)
);

-- ------------------------------------------------------------
-- Subastas
-- ------------------------------------------------------------

CREATE TABLE subastas (
    id           BIGINT          NOT NULL AUTO_INCREMENT,
    titulo       VARCHAR(255)    NOT NULL,
    descripcion  TEXT,
    fecha_inicio DATETIME(6),
    fecha_fin    DATETIME(6),
    categoria    VARCHAR(20)     NOT NULL,   -- COMUN | ESPECIAL | PLATA | ORO | PLATINO
    moneda       VARCHAR(10)     NOT NULL,   -- ARS | USD
    estado       VARCHAR(20)     NOT NULL DEFAULT 'PROXIMA',  -- PROXIMA | ABIERTA | CERRADA
    ubicacion    VARCHAR(255),
    rematador_id BIGINT,
    PRIMARY KEY (id),
    CONSTRAINT fk_subastas_rematador FOREIGN KEY (rematador_id) REFERENCES rematadores (id)
);

-- ------------------------------------------------------------
-- Items
-- ------------------------------------------------------------

CREATE TABLE items (
    id               BIGINT          NOT NULL AUTO_INCREMENT,
    numero_pieza     VARCHAR(255),
    descripcion      TEXT            NOT NULL,
    precio_base      DECIMAL(15,2)   NOT NULL,
    estado           VARCHAR(20)     NOT NULL DEFAULT 'DISPONIBLE',  -- DISPONIBLE | EN_SUBASTA | VENDIDO
    dueno_actual     VARCHAR(255),
    es_obra_arte     BOOLEAN         NOT NULL DEFAULT FALSE,
    artista          VARCHAR(255),
    fecha_creacion   DATE,
    historia         TEXT,
    ubicacion_fisica VARCHAR(255),
    poliza_id        BIGINT,
    subasta_id       BIGINT,
    mejor_oferta     DECIMAL(15,2),
    mejor_postor_id  BIGINT,
    PRIMARY KEY (id),
    CONSTRAINT fk_items_poliza       FOREIGN KEY (poliza_id)       REFERENCES polizas  (id),
    CONSTRAINT fk_items_subasta      FOREIGN KEY (subasta_id)      REFERENCES subastas (id),
    CONSTRAINT fk_items_mejor_postor FOREIGN KEY (mejor_postor_id) REFERENCES usuarios (id)
);

CREATE TABLE item_componentes (
    item_id     BIGINT          NOT NULL,
    componente  VARCHAR(255),
    CONSTRAINT fk_item_componentes_item FOREIGN KEY (item_id) REFERENCES items (id)
);

CREATE TABLE imagenes_item (
    id           BIGINT          NOT NULL AUTO_INCREMENT,
    url          VARCHAR(255)    NOT NULL,
    orden        INT             NOT NULL DEFAULT 0,
    descripcion  VARCHAR(255),
    item_id      BIGINT          NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_imagenes_item_item FOREIGN KEY (item_id) REFERENCES items (id)
);

-- ------------------------------------------------------------
-- Medios de pago
-- ------------------------------------------------------------

CREATE TABLE medios_pago (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    tipo            VARCHAR(30)     NOT NULL,   -- CHEQUE_CERTIFICADO | CUENTA_BANCARIA | TARJETA_CREDITO
    alias           VARCHAR(255)    NOT NULL,
    moneda          VARCHAR(10)     NOT NULL,   -- ARS | USD
    verificado      BOOLEAN         NOT NULL DEFAULT FALSE,
    monto_limite    DECIMAL(15,2),              -- CHEQUE_CERTIFICADO
    numero_cuenta   VARCHAR(255),               -- CUENTA_BANCARIA
    banco           VARCHAR(255),
    tipo_cuenta     VARCHAR(255),
    cbu             VARCHAR(255),
    numero_tarjeta  VARCHAR(255),               -- TARJETA_CREDITO
    titular         VARCHAR(255),
    vencimiento     VARCHAR(255),
    tipo_tarjeta    VARCHAR(255),
    usuario_id      BIGINT          NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_medios_pago_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id)
);

-- ------------------------------------------------------------
-- Participaciones
-- ------------------------------------------------------------

CREATE TABLE participaciones (
    id                  BIGINT      NOT NULL AUTO_INCREMENT,
    usuario_id          BIGINT      NOT NULL,
    subasta_id          BIGINT      NOT NULL,
    medio_pago_id       BIGINT,
    conectado           BOOLEAN     NOT NULL DEFAULT FALSE,
    fecha_conexion      DATETIME(6),
    fecha_desconexion   DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_participaciones_usuario    FOREIGN KEY (usuario_id)    REFERENCES usuarios    (id),
    CONSTRAINT fk_participaciones_subasta    FOREIGN KEY (subasta_id)    REFERENCES subastas    (id),
    CONSTRAINT fk_participaciones_medio_pago FOREIGN KEY (medio_pago_id) REFERENCES medios_pago (id)
);

-- ------------------------------------------------------------
-- Pujas
-- ------------------------------------------------------------

CREATE TABLE pujas (
    id            BIGINT          NOT NULL AUTO_INCREMENT,
    monto         DECIMAL(15,2)   NOT NULL,
    timestamp     DATETIME(6)     NOT NULL,
    estado        VARCHAR(20)     NOT NULL DEFAULT 'CONFIRMADA',  -- CONFIRMADA | RECHAZADA
    usuario_id    BIGINT          NOT NULL,
    item_id       BIGINT          NOT NULL,
    subasta_id    BIGINT          NOT NULL,
    medio_pago_id BIGINT          NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_pujas_usuario    FOREIGN KEY (usuario_id)    REFERENCES usuarios    (id),
    CONSTRAINT fk_pujas_item       FOREIGN KEY (item_id)       REFERENCES items       (id),
    CONSTRAINT fk_pujas_subasta    FOREIGN KEY (subasta_id)    REFERENCES subastas    (id),
    CONSTRAINT fk_pujas_medio_pago FOREIGN KEY (medio_pago_id) REFERENCES medios_pago (id)
);

-- ------------------------------------------------------------
-- Compras
-- ------------------------------------------------------------

CREATE TABLE compras (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    item_id         BIGINT          NOT NULL,
    usuario_id      BIGINT          NOT NULL,
    monto_ofertado  DECIMAL(15,2)   NOT NULL,
    comisiones      DECIMAL(15,2),
    costo_envio     DECIMAL(15,2),
    total           DECIMAL(15,2)   NOT NULL,
    moneda          VARCHAR(10)     NOT NULL,   -- ARS | USD
    medio_pago_id   BIGINT,
    estado_pago     VARCHAR(20)     NOT NULL DEFAULT 'PENDIENTE',  -- PENDIENTE | PAGADO | FALLIDO
    direccion_envio VARCHAR(255),
    PRIMARY KEY (id),
    CONSTRAINT fk_compras_item       FOREIGN KEY (item_id)       REFERENCES items       (id),
    CONSTRAINT fk_compras_usuario    FOREIGN KEY (usuario_id)    REFERENCES usuarios    (id),
    CONSTRAINT fk_compras_medio_pago FOREIGN KEY (medio_pago_id) REFERENCES medios_pago (id)
);

-- ------------------------------------------------------------
-- Multas
-- ------------------------------------------------------------

CREATE TABLE multas (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    monto               DECIMAL(15,2)   NOT NULL,
    motivo              VARCHAR(255)    NOT NULL,
    fecha_generacion    DATETIME(6)     NOT NULL,
    fecha_limite_pago   DATETIME(6),
    estado              VARCHAR(20)     NOT NULL DEFAULT 'PENDIENTE',  -- PENDIENTE | PAGADA
    usuario_id          BIGINT          NOT NULL,
    puja_id             BIGINT,
    PRIMARY KEY (id),
    CONSTRAINT fk_multas_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id),
    CONSTRAINT fk_multas_puja    FOREIGN KEY (puja_id)    REFERENCES pujas    (id)
);

-- ------------------------------------------------------------
-- Consignaciones
-- ------------------------------------------------------------

CREATE TABLE consignaciones (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    descripcion         TEXT            NOT NULL,
    datos_adicionales   TEXT,
    estado              VARCHAR(30)     NOT NULL DEFAULT 'PENDIENTE_REVISION',
                                                -- PENDIENTE_REVISION | APROBADA | RECHAZADA | ASIGNADA
    acepta_pertenencia  BOOLEAN         NOT NULL,
    motivo_rechazo      TEXT,
    precio_sugerido     DECIMAL(15,2),
    valor_base          DECIMAL(15,2),
    comisiones          DECIMAL(15,2),
    subasta_id          BIGINT,
    cuenta_destino_id   BIGINT,
    usuario_id          BIGINT          NOT NULL,
    poliza_id           BIGINT,
    deposito_id         BIGINT,
    PRIMARY KEY (id),
    CONSTRAINT fk_consignaciones_subasta        FOREIGN KEY (subasta_id)        REFERENCES subastas    (id),
    CONSTRAINT fk_consignaciones_cuenta_destino FOREIGN KEY (cuenta_destino_id) REFERENCES medios_pago (id),
    CONSTRAINT fk_consignaciones_usuario        FOREIGN KEY (usuario_id)        REFERENCES usuarios    (id),
    CONSTRAINT fk_consignaciones_poliza         FOREIGN KEY (poliza_id)         REFERENCES polizas     (id),
    CONSTRAINT fk_consignaciones_deposito       FOREIGN KEY (deposito_id)       REFERENCES depositos   (id)
);

CREATE TABLE fotos_consignacion (
    id               BIGINT          NOT NULL AUTO_INCREMENT,
    url              VARCHAR(255)    NOT NULL,
    orden            INT             NOT NULL DEFAULT 0,
    consignacion_id  BIGINT          NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_fotos_consignacion_consignacion FOREIGN KEY (consignacion_id) REFERENCES consignaciones (id)
);
