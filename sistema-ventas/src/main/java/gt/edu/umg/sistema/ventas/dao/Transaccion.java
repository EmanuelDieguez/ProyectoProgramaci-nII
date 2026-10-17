package gt.edu.umg.sistema.ventas.dao;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Ejecuta varias operaciones con UNA sola conexión.
 * - ejecutar(): todo o nada (commit / rollback). Úsese para operaciones que tocan varias tablas.
 * - consultar(): una conexión normal para lecturas u operaciones simples.
 */
public final class Transaccion {

    @FunctionalInterface
    public interface Trabajo<T> {
        T ejecutar(Connection con) throws SQLException;
    }

    private Transaccion() {}

    public static <T> T ejecutar(String accion, Trabajo<T> trabajo) {
        try (Connection con = Conexion.getConexion()) {
            con.setAutoCommit(false);
            try {
                T resultado = trabajo.ejecutar(con);
                con.commit();
                return resultado;
            } catch (SQLException | RuntimeException e) {
                con.rollback();   // si algo falla, no se guarda NADA
                throw e;
            }
        } catch (SQLException e) {
            throw DataAccessException.de(accion, e);
        }
    }

    public static <T> T consultar(String accion, Trabajo<T> trabajo) {
        try (Connection con = Conexion.getConexion()) {
            return trabajo.ejecutar(con);
        } catch (SQLException e) {
            throw DataAccessException.de(accion, e);
        }
    }
}
