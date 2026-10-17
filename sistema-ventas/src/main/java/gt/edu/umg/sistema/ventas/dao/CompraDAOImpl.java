package gt.edu.umg.sistema.ventas.dao;

import gt.edu.umg.sistema.ventas.modelo.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CompraDAOImpl implements CompraDAO {

    @Override
    public void insertar(Connection con, Compra c) throws SQLException {
        String sql = "INSERT INTO compra (fecha, proveedor, id_bodega, id_empleado, total) VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setTimestamp(1, Timestamp.valueOf(c.getFecha()));
            ps.setString(2, c.getProveedor());
            ps.setInt(3, c.getBodega().getIdBodega());
            ps.setInt(4, c.getEmpleado().getId());
            ps.setDouble(5, c.calcularTotal());
            ps.executeUpdate();
            c.setIdCompra(JdbcUtil.idGenerado(ps));
        }
        String sqlDetalle = "INSERT INTO detalle_compra (id_compra, id_producto, cantidad, costo_unitario) VALUES (?,?,?,?)";
        try (PreparedStatement ps = con.prepareStatement(sqlDetalle, Statement.RETURN_GENERATED_KEYS)) {
            for (DetalleCompra d : c.getDetalles()) {
                d.setIdCompra(c.getIdCompra());
                ps.setInt(1, c.getIdCompra());
                ps.setInt(2, d.getProducto().getIdProducto());
                ps.setInt(3, d.getCantidad());
                ps.setDouble(4, d.getCostoUnitario());
                ps.executeUpdate();
                d.setIdDetalle(JdbcUtil.idGenerado(ps));
            }
        }
    }

    @Override
    public List<CompraResumen> listarResumen() {
        String sql = "SELECT c.id_compra, c.fecha, c.proveedor, c.total, b.nombre AS bodega, pe.nombre AS empleado "
                + "FROM compra c JOIN bodega b ON b.id_bodega = c.id_bodega "
                + "JOIN persona pe ON pe.id = c.id_empleado ORDER BY c.id_compra DESC";
        return Transaccion.consultar("listar compras", con -> {
            List<CompraResumen> lista = new ArrayList<>();
            try (PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new CompraResumen(rs.getInt("id_compra"),
                            rs.getTimestamp("fecha").toLocalDateTime(), rs.getString("proveedor"),
                            rs.getString("bodega"), rs.getString("empleado"), rs.getDouble("total")));
                }
            }
            return lista;
        });
    }

    @Override
    public List<DetalleCompra> listarDetalles(int idCompra) {
        String sql = "SELECT d.*, p.nombre, p.precio, p.existencia FROM detalle_compra d "
                + "JOIN producto p ON p.id_producto = d.id_producto WHERE d.id_compra = ? ORDER BY d.id_detalle";
        return Transaccion.consultar("listar el detalle de la compra", con -> {
            List<DetalleCompra> lista = new ArrayList<>();
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, idCompra);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Producto p = new Producto(rs.getInt("id_producto"), rs.getString("nombre"),
                                rs.getDouble("precio"), rs.getInt("existencia"), null);
                        DetalleCompra d = new DetalleCompra(p, rs.getInt("cantidad"), rs.getDouble("costo_unitario"));
                        d.setIdDetalle(rs.getInt("id_detalle"));
                        d.setIdCompra(idCompra);
                        lista.add(d);
                    }
                }
            }
            return lista;
        });
    }
}
