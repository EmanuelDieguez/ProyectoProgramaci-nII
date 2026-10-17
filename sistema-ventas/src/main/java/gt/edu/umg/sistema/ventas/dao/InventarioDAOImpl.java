package gt.edu.umg.sistema.ventas.dao;

import gt.edu.umg.sistema.ventas.modelo.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class InventarioDAOImpl implements InventarioDAO {

    @Override
    public int existencia(Connection con, int idBodega, int idProducto, boolean bloquear) throws SQLException {
        String sql = "SELECT cantidad FROM bodega_producto WHERE id_bodega=? AND id_producto=?"
                + (bloquear ? " FOR UPDATE" : "");
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idBodega);
            ps.setInt(2, idProducto);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    @Override
    public int existencia(int idBodega, int idProducto) {
        return Transaccion.consultar("consultar existencia",
                con -> existencia(con, idBodega, idProducto, false));
    }

    @Override
    public void sumarExistencia(Connection con, int idBodega, int idProducto, int delta) throws SQLException {
        if (delta < 0) {
            // Salida: la fila ya existe (se validó la existencia antes)
            String sql = "UPDATE bodega_producto SET cantidad = cantidad + ? WHERE id_bodega=? AND id_producto=?";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, delta);
                ps.setInt(2, idBodega);
                ps.setInt(3, idProducto);
                ps.executeUpdate();
            }
            return;
        }
        // Entrada: si el producto aún no existe en la bodega se crea la fila; si existe se suma
        String sql = "INSERT INTO bodega_producto (id_bodega, id_producto, cantidad) VALUES (?,?,?) "
                + "ON DUPLICATE KEY UPDATE cantidad = cantidad + ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idBodega);
            ps.setInt(2, idProducto);
            ps.setInt(3, delta);
            ps.setInt(4, delta);
            ps.executeUpdate();
        }
    }

    @Override
    public void insertarMovimiento(Connection con, MovimientoInventario m) throws SQLException {
        String sql = "INSERT INTO movimiento_inventario "
                + "(fecha, id_bodega, id_producto, tipo, cantidad, saldo, referencia) VALUES (?,?,?,?,?,?,?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setTimestamp(1, Timestamp.valueOf(m.getFecha()));
            ps.setInt(2, m.getBodega().getIdBodega());
            ps.setInt(3, m.getProducto().getIdProducto());
            ps.setString(4, m.getTipo().name());
            ps.setInt(5, m.getCantidad());
            ps.setInt(6, m.getSaldo());
            ps.setString(7, m.getReferencia());
            ps.executeUpdate();
            m.setIdMovimiento(JdbcUtil.idGenerado(ps));
        }
    }

    private static final String SELECT_EXISTENCIA = "SELECT bp.cantidad, b.id_bodega, b.nombre AS bodega, "
            + "b.ubicacion, b.activa, p.id_producto, p.nombre AS producto, p.precio, p.existencia "
            + "FROM bodega_producto bp "
            + "JOIN bodega b ON b.id_bodega = bp.id_bodega "
            + "JOIN producto p ON p.id_producto = bp.id_producto ";

    @Override
    public List<Existencia> listarPorBodega(int idBodega) {
        return listarExistencias(SELECT_EXISTENCIA + "WHERE bp.id_bodega = ? ORDER BY p.nombre", idBodega);
    }

    @Override
    public List<Existencia> listarPorProducto(int idProducto) {
        return listarExistencias(SELECT_EXISTENCIA + "WHERE bp.id_producto = ? ORDER BY b.nombre", idProducto);
    }

    private List<Existencia> listarExistencias(String sql, int parametro) {
        return Transaccion.consultar("listar existencias", con -> {
            List<Existencia> lista = new ArrayList<>();
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, parametro);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Bodega b = new Bodega(rs.getInt("id_bodega"), rs.getString("bodega"),
                                rs.getString("ubicacion"), rs.getBoolean("activa"));
                        Producto p = new Producto(rs.getInt("id_producto"), rs.getString("producto"),
                                rs.getDouble("precio"), rs.getInt("existencia"), null);
                        lista.add(new Existencia(b, p, rs.getInt("cantidad")));
                    }
                }
            }
            return lista;
        });
    }

    @Override
    public List<MovimientoInventario> listarMovimientos(int idBodega, int idProducto) {
        StringBuilder sql = new StringBuilder("SELECT m.*, b.nombre AS bodega, p.nombre AS producto "
                + "FROM movimiento_inventario m "
                + "JOIN bodega b ON b.id_bodega = m.id_bodega "
                + "JOIN producto p ON p.id_producto = m.id_producto WHERE 1=1 ");
        List<Integer> params = new ArrayList<>();
        if (idBodega > 0) {
            sql.append("AND m.id_bodega = ? ");
            params.add(idBodega);
        }
        if (idProducto > 0) {
            sql.append("AND m.id_producto = ? ");
            params.add(idProducto);
        }
        sql.append("ORDER BY m.fecha DESC, m.id_movimiento DESC");

        return Transaccion.consultar("listar movimientos", con -> {
            List<MovimientoInventario> lista = new ArrayList<>();
            try (PreparedStatement ps = con.prepareStatement(sql.toString())) {
                for (int i = 0; i < params.size(); i++) {
                    ps.setInt(i + 1, params.get(i));
                }
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        MovimientoInventario m = new MovimientoInventario();
                        m.setIdMovimiento(rs.getInt("id_movimiento"));
                        m.setFecha(rs.getTimestamp("fecha").toLocalDateTime());
                        m.setBodega(new Bodega(rs.getInt("id_bodega"), rs.getString("bodega"), null, true));
                        m.setProducto(new Producto(rs.getInt("id_producto"), rs.getString("producto"), 0, 0, null));
                        m.setTipo(TipoMovimiento.valueOf(rs.getString("tipo")));
                        m.setCantidad(rs.getInt("cantidad"));
                        m.setSaldo(rs.getInt("saldo"));
                        m.setReferencia(rs.getString("referencia"));
                        lista.add(m);
                    }
                }
            }
            return lista;
        });
    }
}
