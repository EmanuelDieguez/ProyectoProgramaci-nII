package gt.edu.umg.sistema.ventas.dao;

import gt.edu.umg.sistema.ventas.modelo.EstadoFactura;
import gt.edu.umg.sistema.ventas.modelo.Factura;
import gt.edu.umg.sistema.ventas.modelo.FacturaResumen;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Las facturas no se editan ni se borran: se registran (FacturaServicio) y se anulan.
 */
public interface FacturaDAO {
    /** Inserta solo el encabezado y asigna el id generado. */
    void insertar(Connection con, Factura f) throws SQLException;

    void actualizarEstado(Connection con, int idFactura, EstadoFactura estado) throws SQLException;

    /** Carga la factura con su detalle. bloquear=true evita que dos usuarios la modifiquen a la vez. */
    Factura buscarPorId(Connection con, int idFactura, boolean bloquear) throws SQLException;

    Factura buscarPorId(int idFactura);

    /** estado = null para listar todas. */
    List<FacturaResumen> listarResumen(EstadoFactura estado);
}
