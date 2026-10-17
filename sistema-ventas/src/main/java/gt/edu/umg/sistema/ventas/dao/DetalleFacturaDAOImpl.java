package gt.edu.umg.sistema.ventas.dao;

import gt.edu.umg.sistema.ventas.modelo.DetalleFactura;
import gt.edu.umg.sistema.ventas.modelo.Producto;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DetalleFacturaDAOImpl implements DetalleFacturaDAO {

    @Override
    public void insertar(Connection con, DetalleFactura d) throws SQLException {
        String sql = "INSERT INTO detalle_factura (id_factura, id_producto, cantidad, precio_unitario) VALUES (?,?,?,?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, d.getIdFactura());
            ps.setInt(2, d.getProducto().getIdProducto());
            ps.setInt(3, d.getCantidad());
            ps.setDouble(4, d.getPrecioUnitario());
            ps.executeUpdate();
            d.setIdDetalle(JdbcUtil.idGenerado(ps));
        }
    }

    @Override
    public List<DetalleFactura> listarPorFactura(Connection con, int idFactura) throws SQLException {
        String sql = "SELECT d.*, p.nombre, p.precio, p.existencia FROM detalle_factura d "
                + "JOIN producto p ON p.id_producto = d.id_producto WHERE d.id_factura = ? ORDER BY d.id_detalle";
        List<DetalleFactura> lista = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idFactura);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Producto p = new Producto(rs.getInt("id_producto"), rs.getString("nombre"),
                            rs.getDouble("precio"), rs.getInt("existencia"), null);
                    DetalleFactura d = new DetalleFactura(p, rs.getInt("cantidad"),
                            rs.getDouble("precio_unitario"), rs.getInt("id_factura"));
                    d.setIdDetalle(rs.getInt("id_detalle"));
                    lista.add(d);
                }
            }
        }
        return lista;
    }
}
