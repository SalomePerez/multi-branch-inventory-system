-- ============================================================
-- Sistema de Gestión de Inventario Multi-Sucursal
-- Script de inicialización de base de datos PostgreSQL
-- ============================================================

-- Extensiones
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================================
-- ENUM types
-- ============================================================

CREATE TYPE rol_usuario AS ENUM ('ADMINISTRADOR', 'GERENTE_SUCURSAL', 'OPERADOR_INVENTARIO');
CREATE TYPE tipo_movimiento AS ENUM ('ENTRADA', 'SALIDA', 'AJUSTE', 'TRANSFERENCIA_ENTRADA', 'TRANSFERENCIA_SALIDA', 'DEVOLUCION');
CREATE TYPE estado_transferencia AS ENUM ('PENDIENTE', 'APROBADA', 'EN_TRANSITO', 'COMPLETADA', 'INCOMPLETA');
CREATE TYPE estado_orden AS ENUM ('PENDIENTE', 'APROBADA', 'RECIBIDA', 'CANCELADA');

-- ============================================================
-- Tabla: sucursales
-- ============================================================

CREATE TABLE IF NOT EXISTS sucursales (
    id          BIGSERIAL PRIMARY KEY,
    nombre      VARCHAR(100) NOT NULL,
    direccion   VARCHAR(255) NOT NULL,
    telefono    VARCHAR(20),
    email       VARCHAR(100),
    activa      BOOLEAN NOT NULL DEFAULT TRUE,
    principal   BOOLEAN NOT NULL DEFAULT FALSE,
    motivo_desactivacion TEXT,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ============================================================
-- Tabla: usuarios
-- ============================================================

CREATE TABLE IF NOT EXISTS usuarios (
    id              BIGSERIAL PRIMARY KEY,
    nombre          VARCHAR(100) NOT NULL,
    email           VARCHAR(100) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    rol             VARCHAR(50) NOT NULL,
    sucursal_id     BIGINT REFERENCES sucursales(id) ON DELETE SET NULL,
    activo          BOOLEAN NOT NULL DEFAULT TRUE,
    motivo_desactivacion TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ============================================================
-- Tabla: unidades_medida
-- ============================================================

CREATE TABLE IF NOT EXISTS unidades_medida (
    id          BIGSERIAL PRIMARY KEY,
    nombre      VARCHAR(50) NOT NULL UNIQUE,
    abreviatura VARCHAR(10) NOT NULL UNIQUE
);

-- ============================================================
-- Tabla: categorias
-- ============================================================

CREATE TABLE IF NOT EXISTS categorias (
    id          BIGSERIAL PRIMARY KEY,
    nombre      VARCHAR(100) NOT NULL UNIQUE,
    descripcion TEXT,
    activa      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ============================================================
-- Tabla: productos
-- ============================================================

CREATE TABLE IF NOT EXISTS productos (
    id                  BIGSERIAL PRIMARY KEY,
    sku                 VARCHAR(50) NOT NULL UNIQUE,
    nombre              VARCHAR(200) NOT NULL,
    descripcion         TEXT,
    categoria_id        BIGINT NOT NULL REFERENCES categorias(id),
    precio_costo        DECIMAL(12, 2) NOT NULL CHECK (precio_costo >= 0),
    precio_venta        DECIMAL(12, 2) NOT NULL CHECK (precio_venta >= 0),
    unidad_medida_id    BIGINT REFERENCES unidades_medida(id),
    activo              BOOLEAN NOT NULL DEFAULT TRUE,
    motivo_desactivacion TEXT,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ============================================================
-- Tabla: producto_unidades (Conversiones)
-- ============================================================

CREATE TABLE IF NOT EXISTS producto_unidades (
    id                  BIGSERIAL PRIMARY KEY,
    producto_id         BIGINT NOT NULL REFERENCES productos(id) ON DELETE CASCADE,
    unidad_medida_id    BIGINT NOT NULL REFERENCES unidades_medida(id),
    factor_conversion   DECIMAL(12, 4) NOT NULL DEFAULT 1.0, -- Factor respecto a la unidad base
    es_base             BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE (producto_id, unidad_medida_id)
);

-- ============================================================
-- Tabla: inventario (stock por sucursal)
-- ============================================================

CREATE TABLE IF NOT EXISTS inventario (
    id                  BIGSERIAL PRIMARY KEY,
    producto_id         BIGINT NOT NULL REFERENCES productos(id),
    sucursal_id         BIGINT NOT NULL REFERENCES sucursales(id),
    cantidad            INTEGER NOT NULL DEFAULT 0 CHECK (cantidad >= 0),
    stock_minimo        INTEGER NOT NULL DEFAULT 0 CHECK (stock_minimo >= 0),
    stock_maximo        INTEGER CHECK (stock_maximo >= 0),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (producto_id, sucursal_id)
);

-- ============================================================
-- Tabla: movimientos (registro inmutable de toda operación)
-- ============================================================

CREATE TABLE IF NOT EXISTS movimientos (
    id              BIGSERIAL PRIMARY KEY,
    tipo            VARCHAR(50) NOT NULL,
    producto_id     BIGINT REFERENCES productos(id),
    sucursal_id     BIGINT REFERENCES sucursales(id),
    cantidad        INTEGER NOT NULL,
    cantidad_antes  INTEGER NOT NULL,
    cantidad_despues INTEGER NOT NULL,
    referencia_id   BIGINT,
    referencia_tipo VARCHAR(50),
    usuario_id      BIGINT REFERENCES usuarios(id),
    motivo          VARCHAR(100) NOT NULL, -- Motivo obligatorio (e.g. COMPRA, VENTA, MERMA)
    observaciones   TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ============================================================
-- Tabla: transferencias
-- ============================================================

CREATE TABLE IF NOT EXISTS transferencias (
    id                  BIGSERIAL PRIMARY KEY,
    sucursal_origen_id  BIGINT NOT NULL REFERENCES sucursales(id),
    sucursal_destino_id BIGINT NOT NULL REFERENCES sucursales(id),
    estado              VARCHAR(50) NOT NULL DEFAULT 'PENDIENTE',
    solicitado_por      BIGINT NOT NULL REFERENCES usuarios(id),
    aprobado_por        BIGINT REFERENCES usuarios(id),
    observaciones       TEXT,
    transportista       VARCHAR(100),
    fecha_salida        TIMESTAMP,
    fecha_estimada_llegada TIMESTAMP,
    fecha_real_llegada   TIMESTAMP,
    prioridad           VARCHAR(20) DEFAULT 'NORMAL',
    ruta_nombre         VARCHAR(255),
    costo_envio         DECIMAL(12, 2),
    tiempo_transito     INTEGER,
    tipo_ruta           VARCHAR(50),
    notas_despacho      TEXT,
    operador_despacho   VARCHAR(255),
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT transferencia_distintas_sucursales CHECK (sucursal_origen_id <> sucursal_destino_id)
);

-- ============================================================
-- Tabla: transferencia_items
-- ============================================================

CREATE TABLE IF NOT EXISTS transferencia_items (
    id                  BIGSERIAL PRIMARY KEY,
    transferencia_id    BIGINT NOT NULL REFERENCES transferencias(id) ON DELETE CASCADE,
    producto_id         BIGINT NOT NULL REFERENCES productos(id),
    cantidad_solicitada INTEGER NOT NULL CHECK (cantidad_solicitada > 0),
    cantidad_enviada    INTEGER CHECK (cantidad_enviada >= 0),
    cantidad_recibida   INTEGER CHECK (cantidad_recibida >= 0)
);

-- ============================================================
-- Tabla: ordenes_compra
-- ============================================================

CREATE TABLE IF NOT EXISTS ordenes_compra (
    id                  BIGSERIAL PRIMARY KEY,
    sucursal_id         BIGINT NOT NULL REFERENCES sucursales(id),
    proveedor           VARCHAR(200) NOT NULL,
    estado              VARCHAR(50) NOT NULL DEFAULT 'PENDIENTE',
    total               DECIMAL(14, 2),
    creado_por          BIGINT NOT NULL REFERENCES usuarios(id),
    aprobado_por        BIGINT REFERENCES usuarios(id),
    observaciones       TEXT,
    plazo_pago_dias     INTEGER NOT NULL DEFAULT 0,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ============================================================
-- Tabla: orden_compra_items
-- ============================================================

CREATE TABLE IF NOT EXISTS orden_compra_items (
    id                  BIGSERIAL PRIMARY KEY,
    orden_id            BIGINT NOT NULL REFERENCES ordenes_compra(id) ON DELETE CASCADE,
    producto_id         BIGINT NOT NULL REFERENCES productos(id),
    cantidad            INTEGER NOT NULL CHECK (cantidad > 0),
    precio_unitario     DECIMAL(12, 2) NOT NULL CHECK (precio_unitario >= 0)
);

-- ============================================================
-- Tabla: listas_precio
-- ============================================================

CREATE TABLE IF NOT EXISTS listas_precio (
    id          BIGSERIAL PRIMARY KEY,
    nombre      VARCHAR(100) NOT NULL,
    descripcion TEXT,
    activa      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ============================================================
-- Tabla: producto_precios
-- ============================================================

CREATE TABLE IF NOT EXISTS producto_precios (
    id                  BIGSERIAL PRIMARY KEY,
    lista_precio_id     BIGINT NOT NULL REFERENCES listas_precio(id) ON DELETE CASCADE,
    producto_id         BIGINT NOT NULL REFERENCES productos(id) ON DELETE CASCADE,
    precio_venta        DECIMAL(12, 2) NOT NULL CHECK (precio_venta >= 0),
    UNIQUE (lista_precio_id, producto_id)
);

-- ============================================================
-- Tabla: ventas
-- ============================================================

CREATE TABLE IF NOT EXISTS ventas (
    id              BIGSERIAL PRIMARY KEY,
    sucursal_id     BIGINT NOT NULL REFERENCES sucursales(id),
    vendedor_id     BIGINT NOT NULL REFERENCES usuarios(id),
    lista_precio_id BIGINT REFERENCES listas_precio(id),
    total           DECIMAL(14, 2) NOT NULL,
    descuento_total DECIMAL(14, 2) DEFAULT 0,
    observaciones   TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ============================================================
-- Tabla: venta_items
-- ============================================================

CREATE TABLE IF NOT EXISTS venta_items (
    id              BIGSERIAL PRIMARY KEY,
    venta_id        BIGINT NOT NULL REFERENCES ventas(id) ON DELETE CASCADE,
    producto_id     BIGINT NOT NULL REFERENCES productos(id),
    cantidad        INTEGER NOT NULL CHECK (cantidad > 0),
    precio_unitario DECIMAL(12, 2) NOT NULL CHECK (precio_unitario >= 0),
    descuento_aplicado DECIMAL(14, 2) DEFAULT 0,
    subtotal        DECIMAL(12, 2) NOT NULL
);

-- ============================================================
-- Tabla: alertas
-- ============================================================

CREATE TABLE IF NOT EXISTS alertas (
    id              BIGSERIAL PRIMARY KEY,
    tipo            VARCHAR(50) NOT NULL,
    producto_id     BIGINT REFERENCES productos(id),
    sucursal_id     BIGINT REFERENCES sucursales(id),
    mensaje         TEXT NOT NULL,
    leida           BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ============================================================
-- Índices para performance
-- ============================================================

CREATE INDEX IF NOT EXISTS idx_inventario_sucursal ON inventario(sucursal_id);
CREATE INDEX IF NOT EXISTS idx_inventario_producto ON inventario(producto_id);
CREATE INDEX IF NOT EXISTS idx_movimientos_producto ON movimientos(producto_id);
CREATE INDEX IF NOT EXISTS idx_movimientos_sucursal ON movimientos(sucursal_id);
CREATE INDEX IF NOT EXISTS idx_movimientos_created ON movimientos(created_at);
CREATE INDEX IF NOT EXISTS idx_ventas_sucursal ON ventas(sucursal_id);
CREATE INDEX IF NOT EXISTS idx_ventas_created ON ventas(created_at);
CREATE INDEX IF NOT EXISTS idx_transferencias_origen ON transferencias(sucursal_origen_id);
CREATE INDEX IF NOT EXISTS idx_transferencias_destino ON transferencias(sucursal_destino_id);
CREATE INDEX IF NOT EXISTS idx_alertas_no_leidas ON alertas(leida) WHERE leida = FALSE;

-- ============================================================
-- Datos iniciales (seed)
-- ============================================================

-- Sucursales de ejemplo
INSERT INTO sucursales (nombre, direccion, telefono, email) VALUES
    ('Casa Central', 'Av. Principal 100, Ciudad', '+1234567890', 'central@empresa.com'),
    ('Sucursal Norte', 'Calle Norte 200, Ciudad', '+1234567891', 'norte@empresa.com'),
    ('Sucursal Sur', 'Calle Sur 300, Ciudad', '+1234567892', 'sur@empresa.com')
ON CONFLICT DO NOTHING;

-- Categorías
INSERT INTO categorias (nombre, descripcion) VALUES
    ('Electrónica', 'Dispositivos y componentes electrónicos'),
    ('Ferretería', 'Herramientas y materiales de construcción'),
    ('Oficina', 'Artículos de oficina y papelería')
ON CONFLICT DO NOTHING;
-- Unidades de Medida Básicas
INSERT INTO unidades_medida (nombre, abreviatura) VALUES
    ('Unidad', 'und'),
    ('Kilogramo', 'kg'),
    ('Litro', 'lt'),
    ('Caja (12 und)', 'cj12')
ON CONFLICT DO NOTHING;
