package gt.edu.umg.sistema.ventas.servicio;

import gt.edu.umg.sistema.ventas.dao.*;
import gt.edu.umg.sistema.ventas.evento.BusEventos;
import gt.edu.umg.sistema.ventas.evento.EventoSistema;
import gt.edu.umg.sistema.ventas.modelo.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * ÚNICO punto por donde cambian las existencias.
 * Cada movimiento actualiza en la misma transacción:
 *   1) la existencia del producto en la bodega (bodega_producto)
 *   2) la existencia total del producto (producto.existencia)
 *   3) el kardex (movimiento_inventario)
 */
public class InventarioServicio {

    private final InventarioDAO inventarioDAO = new InventarioDAOImpl();
    private final ProductoDAO productoDAO = new ProductoDAOImpl();

    public void aplicarMovimiento(Connection con, Bodega bodega, Producto producto, TipoMovimiento tipo,
                                  int cantidad, String referencia) throws SQLException {
        if (bodega == null) {
            throw new ReglaNegocioException("Debe indicar la bodega.");
        }
        if (cantidad <= 0) {
            throw new ReglaNegocioException("La cantidad debe ser mayor que cero.");
        }
        int actual = inventarioDAO.existencia(con, bodega.getIdBodega(), producto.getIdProducto(), true);
        int delta = (tipo == TipoMovimiento.ENTRADA) ? cantidad : -cantidad;
        if (actual + delta < 0) {
            throw new ReglaNegocioException(String.format(
                    "Existencia insuficiente de \"%s\" en %s.%nDisponible: %d   Solicitado: %d",
                    producto.getNombre(), bodega.getNombre(), actual, cantidad));
        }

        inventarioDAO.sumarExistencia(con, bodega.getIdBodega(), producto.getIdProducto(), delta);
        productoDAO.ajustarExistencia(con, producto.getIdProducto(), delta);

        MovimientoInventario m = new MovimientoInventario();
        m.setFecha(LocalDateTime.now());
        m.setBodega(bodega);
        m.setProducto(producto);
        m.setTipo(tipo);
        m.setCantidad(cantidad);
        m.setSaldo(actual + delta);
        m.setReferencia(referencia);
        inventarioDAO.insertarMovimiento(con, m);
    }

    public void trasladar(Bodega origen, Bodega destino, Producto producto, int cantidad) {
        if (origen == null || destino == null || producto == null) {
            throw new ReglaNegocioException("Seleccione bodega de origen, destino y producto.");
        }
        if (origen.getIdBodega() == destino.getIdBodega()) {
            throw new ReglaNegocioException("La bodega de origen y destino deben ser distintas.");
        }
        Transaccion.ejecutar("realizar el traslado", con -> {
            aplicarMovimiento(con, origen, producto, TipoMovimiento.SALIDA, cantidad,
                    "Traslado hacia " + destino.getNombre());
            aplicarMovimiento(con, destino, producto, TipoMovimiento.ENTRADA, cantidad,
                    "Traslado desde " + origen.getNombre());
            return null;
        });
        BusEventos.publicar(EventoSistema.INVENTARIO, EventoSistema.PRODUCTOS);
    }

    public int existencia(Bodega bodega, Producto producto) {
        if (bodega == null || producto == null) {
            return 0;
        }
        return inventarioDAO.existencia(bodega.getIdBodega(), producto.getIdProducto());
    }

    public List<Existencia> existenciasPorBodega(int idBodega) {
        return inventarioDAO.listarPorBodega(idBodega);
    }

    public List<Existencia> existenciasPorProducto(int idProducto) {
        return inventarioDAO.listarPorProducto(idProducto);
    }

    public List<MovimientoInventario> movimientos(int idBodega, int idProducto) {
        return inventarioDAO.listarMovimientos(idBodega, idProducto);
    }
}
