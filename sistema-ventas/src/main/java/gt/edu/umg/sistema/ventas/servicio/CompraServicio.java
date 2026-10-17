package gt.edu.umg.sistema.ventas.servicio;

import gt.edu.umg.sistema.ventas.dao.CompraDAO;
import gt.edu.umg.sistema.ventas.dao.CompraDAOImpl;
import gt.edu.umg.sistema.ventas.dao.Transaccion;
import gt.edu.umg.sistema.ventas.evento.BusEventos;
import gt.edu.umg.sistema.ventas.evento.EventoSistema;
import gt.edu.umg.sistema.ventas.modelo.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Compra a proveedor: registra la compra y su detalle, y por cada línea
 * hace una ENTRADA a la bodega destino (sube existencia por bodega, total y kardex).
 */
public class CompraServicio {

    private final CompraDAO compraDAO = new CompraDAOImpl();
    private final InventarioServicio inventario = new InventarioServicio();

    public void agregarDetalle(Compra compra, Producto producto, int cantidad, double costoUnitario) {
        if (producto == null) throw new ReglaNegocioException("Seleccione un producto.");
        if (cantidad <= 0) throw new ReglaNegocioException("La cantidad debe ser mayor que cero.");
        if (costoUnitario < 0) throw new ReglaNegocioException("El costo no puede ser negativo.");
        compra.agregarDetalle(new DetalleCompra(producto, cantidad, costoUnitario));
    }

    /** @return número de la compra registrada */
    public int registrarCompra(Compra c) {
        if (c.getProveedor() == null || c.getProveedor().isBlank()) {
            throw new ReglaNegocioException("Indique el proveedor.");
        }
        if (c.getBodega() == null) throw new ReglaNegocioException("Seleccione la bodega destino.");
        if (c.getEmpleado() == null) throw new ReglaNegocioException("Seleccione el empleado que recibe.");
        if (c.getDetalles().isEmpty()) throw new ReglaNegocioException("Agregue al menos un producto.");

        c.setFecha(LocalDateTime.now());
        try {
            Transaccion.ejecutar("registrar la compra", con -> {
                compraDAO.insertar(con, c);
                for (DetalleCompra d : c.getDetalles()) {
                    inventario.aplicarMovimiento(con, c.getBodega(), d.getProducto(), TipoMovimiento.ENTRADA,
                            d.getCantidad(), "Compra #" + c.getIdCompra() + " - " + c.getProveedor());
                }
                return null;
            });
        } catch (RuntimeException e) {
            c.setIdCompra(0);
            throw e;
        }
        BusEventos.publicar(EventoSistema.COMPRAS, EventoSistema.INVENTARIO, EventoSistema.PRODUCTOS);
        return c.getIdCompra();
    }

    public List<CompraResumen> listar() {
        return compraDAO.listarResumen();
    }

    public List<DetalleCompra> detalles(int idCompra) {
        return compraDAO.listarDetalles(idCompra);
    }
}
