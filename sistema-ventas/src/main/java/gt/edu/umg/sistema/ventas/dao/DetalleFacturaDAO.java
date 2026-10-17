package gt.edu.umg.sistema.ventas.dao;

import gt.edu.umg.sistema.ventas.modelo.DetalleFactura;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public interface DetalleFacturaDAO {
    void insertar(Connection con, DetalleFactura d) throws SQLException;

    List<DetalleFactura> listarPorFactura(Connection con, int idFactura) throws SQLException;
}
