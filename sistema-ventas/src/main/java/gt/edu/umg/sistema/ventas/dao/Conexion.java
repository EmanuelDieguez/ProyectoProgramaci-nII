package gt.edu.umg.sistema.ventas.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class Conexion {
    // Se pueden cambiar sin recompilar con -Ddb.url=... -Ddb.user=... -Ddb.password=...
    private static final String URL = System.getProperty("db.url",
            "jdbc:mysql://localhost:3306/sistema_ventas?useSSL=false&allowPublicKeyRetrieval=true");
    private static final String USUARIO = System.getProperty("db.user", "root");
    private static final String CLAVE = System.getProperty("db.password", "m6666");

    private Conexion() {}

    public static Connection getConexion() throws SQLException {
        return DriverManager.getConnection(URL, USUARIO, CLAVE);
    }

    /** @return null si la conexión funciona; si no, el mensaje de error. */
    public static String probar() {
        try (Connection con = getConexion()) {
            return con.isValid(3) ? null : "La conexión no respondió";
        } catch (SQLException e) {
            return e.getMessage();
        }
    }
}
