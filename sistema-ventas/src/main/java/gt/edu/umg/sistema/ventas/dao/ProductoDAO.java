package gt.edu.umg.sistema.ventas.dao;

import gt.edu.umg.sistema.ventas.modelo.Producto;
import java.sql.Connection;
import java.sql.SQLException;

public interface ProductoDAO extends DAO<Producto> {
    /** Inserta usando una conexión existente (dentro de una transacción). */
    void insertar(Connection con, Producto p) throws SQLException;

    /** Suma (o resta, si delta es negativo) a la existencia total del producto. */
    void ajustarExistencia(Connection con, int idProducto, int delta) throws SQLException;
}
