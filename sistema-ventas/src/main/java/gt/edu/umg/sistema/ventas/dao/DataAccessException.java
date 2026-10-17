package gt.edu.umg.sistema.ventas.dao;

import java.sql.SQLException;

/** Error de base de datos con un mensaje entendible para el usuario. */
public class DataAccessException extends RuntimeException {

    public DataAccessException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }

    public static DataAccessException de(String accion, SQLException e) {
        String estado = e.getSQLState() == null ? "" : e.getSQLState();
        if (estado.startsWith("23")) {
            return new DataAccessException("No se pudo " + accion
                    + ": el registro está duplicado o está siendo usado por otros datos "
                    + "(facturas, compras, movimientos...).", e);
        }
        if (estado.startsWith("08")) {
            return new DataAccessException("No hay conexión con la base de datos. "
                    + "Verifique que MySQL esté iniciado.", e);
        }
        return new DataAccessException("No se pudo " + accion + ": " + e.getMessage(), e);
    }
}
