-- =====================================================================
--  MIGRACIÓN: agrega Bodegas, Kardex y Compras a una base EXISTENTE
--  (conserva clientes, productos, facturas y pagos que ya tenga).
--  Ejecutar UNA sola vez en MySQL Workbench.
-- =====================================================================
USE sistema_ventas;

CREATE TABLE IF NOT EXISTS bodega (
    id_bodega  INT AUTO_INCREMENT PRIMARY KEY,
    nombre     VARCHAR(60)  NOT NULL UNIQUE,
    ubicacion  VARCHAR(150),
    activa     TINYINT(1)   NOT NULL DEFAULT 1
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS bodega_producto (
    id_bodega    INT NOT NULL,
    id_producto  INT NOT NULL,
    cantidad     INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id_bodega, id_producto),
    CONSTRAINT fk_bp_bodega   FOREIGN KEY (id_bodega)   REFERENCES bodega(id_bodega),
    CONSTRAINT fk_bp_producto FOREIGN KEY (id_producto) REFERENCES producto(id_producto),
    CONSTRAINT ck_bp_cantidad CHECK (cantidad >= 0)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS movimiento_inventario (
    id_movimiento  INT AUTO_INCREMENT PRIMARY KEY,
    fecha          DATETIME     NOT NULL,
    id_bodega      INT          NOT NULL,
    id_producto    INT          NOT NULL,
    tipo           VARCHAR(10)  NOT NULL,
    cantidad       INT          NOT NULL,
    saldo          INT          NOT NULL,
    referencia     VARCHAR(150),
    CONSTRAINT fk_mov_bodega   FOREIGN KEY (id_bodega)   REFERENCES bodega(id_bodega),
    CONSTRAINT fk_mov_producto FOREIGN KEY (id_producto) REFERENCES producto(id_producto)
) ENGINE=InnoDB;

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

-- 1) Bodega por defecto
INSERT IGNORE INTO bodega (id_bodega, nombre, ubicacion, activa) VALUES (1, 'Bodega Central', 'Oficinas centrales', 1);

-- 2) La factura ahora sabe de qué bodega salió la mercadería
ALTER TABLE factura ADD COLUMN id_bodega INT NULL AFTER id_empleado;
UPDATE factura SET id_bodega = 1 WHERE id_bodega IS NULL;
ALTER TABLE factura ADD CONSTRAINT fk_factura_bodega FOREIGN KEY (id_bodega) REFERENCES bodega(id_bodega);

-- 3) La existencia actual de cada producto pasa a la Bodega Central
INSERT IGNORE INTO bodega_producto (id_bodega, id_producto, cantidad)
SELECT 1, id_producto, GREATEST(existencia, 0) FROM producto;

UPDATE producto SET existencia = GREATEST(existencia, 0);

INSERT INTO movimiento_inventario (fecha, id_bodega, id_producto, tipo, cantidad, saldo, referencia)
SELECT NOW(), 1, id_producto, 'ENTRADA', existencia, existencia, 'Saldo inicial (migración)'
FROM producto WHERE existencia > 0;
