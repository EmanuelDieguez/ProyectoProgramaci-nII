package gt.edu.umg.sistema.ventas.dao;

import gt.edu.umg.sistema.ventas.modelo.Pago;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public interface PagoDAO {
    void insertar(Connection con, Pago p) throws SQLException;

    double totalPagado(Connection con, int idFactura) throws SQLException;

    List<Pago> listarPorFactura(int idFactura);
}
