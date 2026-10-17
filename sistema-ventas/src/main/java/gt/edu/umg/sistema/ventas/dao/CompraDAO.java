package gt.edu.umg.sistema.ventas.dao;

import gt.edu.umg.sistema.ventas.modelo.Compra;
import gt.edu.umg.sistema.ventas.modelo.CompraResumen;
import gt.edu.umg.sistema.ventas.modelo.DetalleCompra;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public interface CompraDAO {
    /** Inserta encabezado y detalle (dentro de la transacción del servicio). */
    void insertar(Connection con, Compra c) throws SQLException;

    List<CompraResumen> listarResumen();

    List<DetalleCompra> listarDetalles(int idCompra);
}
