package gt.edu.umg.sistema.ventas.dao;

import gt.edu.umg.sistema.ventas.modelo.TelefonoCliente;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/** Todos los métodos reciben la conexión: se usan dentro de las transacciones de ClienteDAOImpl. */
public interface TelefonoClienteDAO {
    void insertar(Connection con, TelefonoCliente t) throws SQLException;

    void eliminarPorCliente(Connection con, int idCliente) throws SQLException;

    List<TelefonoCliente> listarPorCliente(Connection con, int idCliente) throws SQLException;

    /** Todos los teléfonos en una sola consulta, agrupados por id de cliente. */
    Map<Integer, List<TelefonoCliente>> listarAgrupados(Connection con) throws SQLException;
}