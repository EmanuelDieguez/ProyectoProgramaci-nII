package gt.edu.umg.sistema.ventas.dao;

import gt.edu.umg.sistema.ventas.modelo.Existencia;
import gt.edu.umg.sistema.ventas.modelo.MovimientoInventario;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public interface InventarioDAO {
    /** Existencia de un producto en una bodega. bloquear=true reserva la fila hasta el commit. */
    int existencia(Connection con, int idBodega, int idProducto, boolean bloquear) throws SQLException;

    int existencia(int idBodega, int idProducto);

    void sumarExistencia(Connection con, int idBodega, int idProducto, int delta) throws SQLException;

    void insertarMovimiento(Connection con, MovimientoInventario m) throws SQLException;

    List<Existencia> listarPorBodega(int idBodega);

    List<Existencia> listarPorProducto(int idProducto);

    /** Filtros opcionales: pasar 0 para "todas"/"todos". */
    List<MovimientoInventario> listarMovimientos(int idBodega, int idProducto);
}
