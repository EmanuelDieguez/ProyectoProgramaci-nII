package gt.edu.umg.sistema.ventas.servicio;

import gt.edu.umg.sistema.ventas.dao.*;
import gt.edu.umg.sistema.ventas.evento.BusEventos;
import gt.edu.umg.sistema.ventas.evento.EventoSistema;
import gt.edu.umg.sistema.ventas.modelo.*;
import java.time.LocalDate;
import java.util.List;

/**
 * Flujo de una venta:
 *   registrarVenta() -> factura + detalle + SALIDA de la bodega + baja existencia total   (todo o nada)
 *   PagoServicio     -> pago(s) hasta saldar -> estado PAGADA
 *   anular()         -> ENTRADA de vuelta a la bodega + estado ANULADA
 */
public class FacturaServicio {

    private final FacturaDAO facturaDAO = new FacturaDAOImpl();
    private final DetalleFacturaDAO detalleDAO = new DetalleFacturaDAOImpl();
    private final PagoDAO pagoDAO = new PagoDAOImpl();
    private final InventarioServicio inventario = new InventarioServicio();

    /**
     * Agrega una línea en memoria (aún no se guarda).
     * Si el producto ya está en la factura, suma la cantidad.
     */
    public void agregarDetalle(Factura factura, Producto producto, int cantidad, int disponibleEnBodega) {
        if (producto == null) {
            throw new ReglaNegocioException("Seleccione un producto.");
        }
        if (cantidad <= 0) {
            throw new ReglaNegocioException("La cantidad debe ser mayor que cero.");
        }
        DetalleFactura existente = null;
        for (DetalleFactura d : factura.getDetalles()) {
            if (d.getProducto().getIdProducto() == producto.getIdProducto()) {
                existente = d;
            }
        }
        int yaAgregado = existente == null ? 0 : existente.getCantidad();
        if (yaAgregado + cantidad > disponibleEnBodega) {
            throw new ReglaNegocioException(String.format(
                    "Solo hay %d unidad(es) de \"%s\" en la bodega seleccionada%s.",
                    disponibleEnBodega, producto.getNombre(),
                    yaAgregado > 0 ? " (ya agregó " + yaAgregado + ")" : ""));
        }
        if (existente != null) {
            existente.setCantidad(yaAgregado + cantidad);
        } else {
            factura.agregarDetalle(new DetalleFactura(producto, cantidad, producto.getPrecio()));
        }
    }

    /** @return número de la factura registrada */
    public int registrarVenta(Factura f) {
        if (f.getCliente() == null) throw new ReglaNegocioException("Seleccione el cliente.");
        if (f.getEmpleado() == null) throw new ReglaNegocioException("Seleccione el empleado.");
        if (f.getBodega() == null) throw new ReglaNegocioException("Seleccione la bodega de despacho.");
        if (f.getDetalles().isEmpty()) throw new ReglaNegocioException("Agregue al menos un producto.");

        f.setFecha(LocalDate.now());
        f.setEstado(EstadoFactura.PENDIENTE);
        try {
            Transaccion.ejecutar("registrar la factura", con -> {
                facturaDAO.insertar(con, f);
                for (DetalleFactura d : f.getDetalles()) {
                    d.setIdFactura(f.getIdFactura());
                    detalleDAO.insertar(con, d);
                    // La validación de existencia se repite aquí con la fila bloqueada:
                    // si otro usuario vendió mientras tanto, la venta completa se revierte.
                    inventario.aplicarMovimiento(con, f.getBodega(), d.getProducto(), TipoMovimiento.SALIDA,
                            d.getCantidad(), "Factura #" + f.getIdFactura());
                }
                return null;
            });
        } catch (RuntimeException e) {
            f.setIdFactura(0);
            for (DetalleFactura d : f.getDetalles()) {
                d.setIdFactura(0);
                d.setIdDetalle(0);
            }
            throw e;
        }
        BusEventos.publicar(EventoSistema.FACTURAS, EventoSistema.INVENTARIO, EventoSistema.PRODUCTOS);
        return f.getIdFactura();
    }

    public void anular(int idFactura) {
        Transaccion.ejecutar("anular la factura", con -> {
            Factura f = facturaDAO.buscarPorId(con, idFactura, true);
            if (f == null) {
                throw new ReglaNegocioException("La factura #" + idFactura + " no existe.");
            }
            if (f.getEstado() == EstadoFactura.ANULADA) {
                throw new ReglaNegocioException("La factura #" + idFactura + " ya está anulada.");
            }
            double pagado = pagoDAO.totalPagado(con, idFactura);
            if (pagado > 0) {
                throw new ReglaNegocioException(String.format(java.util.Locale.US,
                        "La factura #%d tiene pagos registrados (Q %,.2f) y no se puede anular.", idFactura, pagado));
            }
            if (f.getBodega() == null) {
                throw new ReglaNegocioException("La factura no tiene bodega asociada; no se puede devolver el inventario.");
            }
            for (DetalleFactura d : f.getDetalles()) {
                inventario.aplicarMovimiento(con, f.getBodega(), d.getProducto(), TipoMovimiento.ENTRADA,
                        d.getCantidad(), "Anulación factura #" + idFactura);
            }
            facturaDAO.actualizarEstado(con, idFactura, EstadoFactura.ANULADA);
            return null;
        });
        BusEventos.publicar(EventoSistema.FACTURAS, EventoSistema.INVENTARIO,
                EventoSistema.PRODUCTOS, EventoSistema.PAGOS);
    }

    public Factura buscar(int idFactura) {
        return facturaDAO.buscarPorId(idFactura);
    }

    /** estado = null para todas */
    public List<FacturaResumen> listar(EstadoFactura estado) {
        return facturaDAO.listarResumen(estado);
    }
}
