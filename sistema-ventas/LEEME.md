# Sistema de Ventas v2.0

## Cómo ponerlo a funcionar (NetBeans)

1. **Base de datos** (MySQL Workbench):
   - Base nueva o de pruebas → ejecutar `sql/01_base_de_datos_completa.sql`
   - Ya tiene datos que quiere conservar → ejecutar **una sola vez** `sql/02_migracion_bodega.sql`
     (crea las tablas nuevas y pasa la existencia actual de cada producto a "Bodega Central").
2. Revisar usuario/contraseña en `dao/Conexion.java` (por defecto `root` / `1902`).
3. Abrir la carpeta en NetBeans → **Clean and Build** → **Run**. La clase principal es `SistemaVentas`.

## Estructura (un solo paquete base: gt.edu.umg.sistema.ventas)

| Paquete     | Contenido |
|-------------|-----------|
| `modelo`    | Entidades (Cliente, Producto, Factura...) + nuevas: Bodega, Existencia, MovimientoInventario, Compra, DetalleCompra |
| `dao`       | Acceso a datos. `Transaccion` permite que varias operaciones se guarden "todo o nada" |
| `servicio`  | Reglas de negocio: FacturaServicio, PagoServicio, CompraServicio, InventarioServicio, ProductoServicio |
| `evento`    | `BusEventos`: avisa a las ventanas abiertas que algo cambió (patrón Observador) |
| `vista`     | `VentanaPrincipal` (formulario padre MDI) y los formularios hijos (`JInternalFrame`) |

## Flujo

```
COMPRA  -> compra + detalle -> ENTRADA en bodega -> sube existencia total -> Kardex
VENTA   -> factura + detalle -> SALIDA de bodega -> baja existencia total -> Kardex
PAGO    -> valida saldo -> si saldo = 0 la factura pasa a PAGADA
ANULAR  -> (solo sin pagos) ENTRADA de vuelta a la bodega -> factura ANULADA
TRASLADO-> SALIDA bodega origen + ENTRADA bodega destino (total no cambia)
```
Cada flujo corre en UNA transacción: si algo falla (por ejemplo, no hay existencia) no se guarda nada.
Después del commit se publica un evento y se recargan solas las ventanas abiertas que dependen de ese dato.

## Atajos
F2 Facturación · F3 Pagos · F4 Compras · F5 Productos · F6 Bodegas · F7 Kardex
