-- =====================================================================
--  SISTEMA DE VENTAS - Base de datos completa (instalación NUEVA)
--  MySQL 8.x
--  Si ya tiene la base con datos, use 02_migracion_bodega.sql en su lugar.
-- =====================================================================
CREATE DATABASE IF NOT EXISTS sistema_ventas CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE sistema_ventas;

CREATE TABLE IF NOT EXISTS persona (
    id        INT AUTO_INCREMENT PRIMARY KEY,
    nombre    VARCHAR(100) NOT NULL,
    nit       VARCHAR(20),
    telefono  VARCHAR(20),
    tipo      VARCHAR(10)  NOT NULL            -- CLIENTE / EMPLEADO
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS cliente (
    id         INT PRIMARY KEY,
    direccion  VARCHAR(150),
    CONSTRAINT fk_cliente_persona FOREIGN KEY (id) REFERENCES persona(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS empleado (
    id               INT PRIMARY KEY,
    codigo_empleado  VARCHAR(20),
    puesto           VARCHAR(50),
    CONSTRAINT fk_empleado_persona FOREIGN KEY (id) REFERENCES persona(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS categoria (
    id_categoria  INT AUTO_INCREMENT PRIMARY KEY,
    nombre        VARCHAR(60) NOT NULL,
    descripcion   VARCHAR(200)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS producto (
    id_producto   INT AUTO_INCREMENT PRIMARY KEY,
    nombre        VARCHAR(100)  NOT NULL,
    precio        DECIMAL(10,2) NOT NULL,
    existencia    INT           NOT NULL DEFAULT 0,   -- total de todas las bodegas
    id_categoria  INT,
    CONSTRAINT fk_producto_categoria FOREIGN KEY (id_categoria) REFERENCES categoria(id_categoria)
) ENGINE=InnoDB;

-- ---------------------------- BODEGA ---------------------------------
CREATE TABLE IF NOT EXISTS bodega (
    id_bodega  INT AUTO_INCREMENT PRIMARY KEY,
    nombre     VARCHAR(60)  NOT NULL UNIQUE,
    ubicacion  VARCHAR(150),
    activa     TINYINT(1)   NOT NULL DEFAULT 1
) ENGINE=InnoDB;

-- Existencia de cada producto en cada bodega
CREATE TABLE IF NOT EXISTS bodega_producto (
    id_bodega    INT NOT NULL,
    id_producto  INT NOT NULL,
    cantidad     INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id_bodega, id_producto),
    CONSTRAINT fk_bp_bodega   FOREIGN KEY (id_bodega)   REFERENCES bodega(id_bodega),
    CONSTRAINT fk_bp_producto FOREIGN KEY (id_producto) REFERENCES producto(id_producto),
    CONSTRAINT ck_bp_cantidad CHECK (cantidad >= 0)
) ENGINE=InnoDB;

-- Kardex: historial de entradas y salidas
CREATE TABLE IF NOT EXISTS movimiento_inventario (
    id_movimiento  INT AUTO_INCREMENT PRIMARY KEY,
    fecha          DATETIME     NOT NULL,
    id_bodega      INT          NOT NULL,
    id_producto    INT          NOT NULL,
    tipo           VARCHAR(10)  NOT NULL,           -- ENTRADA / SALIDA
    cantidad       INT          NOT NULL,
    saldo          INT          NOT NULL,           -- existencia en la bodega después del movimiento
    referencia     VARCHAR(150),
    CONSTRAINT fk_mov_bodega   FOREIGN KEY (id_bodega)   REFERENCES bodega(id_bodega),
    CONSTRAINT fk_mov_producto FOREIGN KEY (id_producto) REFERENCES producto(id_producto)
) ENGINE=InnoDB;

-- ---------------------------- VENTAS ---------------------------------
CREATE TABLE IF NOT EXISTS factura (
    id_factura   INT AUTO_INCREMENT PRIMARY KEY,
    fecha        DATE        NOT NULL,
    id_cliente   INT         NOT NULL,
    id_empleado  INT         NOT NULL,
    id_bodega    INT,
    estado       VARCHAR(15) NOT NULL,              -- PENDIENTE / PAGADA / ANULADA
    CONSTRAINT fk_factura_cliente  FOREIGN KEY (id_cliente)  REFERENCES cliente(id),
    CONSTRAINT fk_factura_empleado FOREIGN KEY (id_empleado) REFERENCES empleado(id),
    CONSTRAINT fk_factura_bodega   FOREIGN KEY (id_bodega)   REFERENCES bodega(id_bodega)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS detalle_factura (
    id_detalle       INT AUTO_INCREMENT PRIMARY KEY,
    id_factura       INT           NOT NULL,
    id_producto      INT           NOT NULL,
    cantidad         INT           NOT NULL,
    precio_unitario  DECIMAL(10,2) NOT NULL,
    CONSTRAINT fk_detalle_factura  FOREIGN KEY (id_factura)  REFERENCES factura(id_factura),
    CONSTRAINT fk_detalle_producto FOREIGN KEY (id_producto) REFERENCES producto(id_producto)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS pago (
    id_pago      INT AUTO_INCREMENT PRIMARY KEY,
    id_factura   INT           NOT NULL,
    monto        DECIMAL(10,2) NOT NULL,
    fecha_pago   DATETIME      NOT NULL,
    metodo_pago  VARCHAR(15)   NOT NULL,            -- EFECTIVO / TARJETA / TRANSFERENCIA
    CONSTRAINT fk_pago_factura FOREIGN KEY (id_factura) REFERENCES factura(id_factura)
) ENGINE=InnoDB;

-- ---------------------------- COMPRAS --------------------------------
CREATE TABLE IF NOT EXISTS compra (
    id_compra    INT AUTO_INCREMENT PRIMARY KEY,
    fecha        DATETIME      NOT NULL,
    proveedor    VARCHAR(100)  NOT NULL,
    id_bodega    INT           NOT NULL,
    id_empleado  INT           NOT NULL,
    total        DECIMAL(12,2) NOT NULL,
    CONSTRAINT fk_compra_bodega   FOREIGN KEY (id_bodega)   REFERENCES bodega(id_bodega),
    CONSTRAINT fk_compra_empleado FOREIGN KEY (id_empleado) REFERENCES empleado(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS detalle_compra (
    id_detalle      INT AUTO_INCREMENT PRIMARY KEY,
    id_compra       INT           NOT NULL,
    id_producto     INT           NOT NULL,
    cantidad        INT           NOT NULL,
    costo_unitario  DECIMAL(10,2) NOT NULL,
    CONSTRAINT fk_dcompra_compra   FOREIGN KEY (id_compra)   REFERENCES compra(id_compra),
    CONSTRAINT fk_dcompra_producto FOREIGN KEY (id_producto) REFERENCES producto(id_producto)
) ENGINE=InnoDB;

-- Datos mínimos
INSERT IGNORE INTO bodega (id_bodega, nombre, ubicacion, activa) VALUES (1, 'Bodega Central', 'Oficinas centrales', 1);
