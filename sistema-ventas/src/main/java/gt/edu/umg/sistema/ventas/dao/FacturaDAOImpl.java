package gt.edu.umg.sistema.ventas.dao;

import gt.edu.umg.sistema.ventas.modelo.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FacturaDAOImpl implements FacturaDAO {

    private final DetalleFacturaDAO detalleDAO = new DetalleFacturaDAOImpl();

    @Override
    public void insertar(Connection con, Factura f) throws SQLException {
        String sql = "INSERT INTO factura (fecha, id_cliente, id_empleado, id_bodega, estado) VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setDate(1, Date.valueOf(f.getFecha()));
            ps.setInt(2, f.getCliente().getId());
            ps.setInt(3, f.getEmpleado().getId());
            ps.setInt(4, f.getBodega().getIdBodega());
            ps.setString(5, f.getEstado().name());
            ps.executeUpdate();
            f.setIdFactura(JdbcUtil.idGenerado(ps));
        }
        // Los detalles los inserta FacturaServicio (antes se insertaban DOS veces)
    }

    @Override
    public void actualizarEstado(Connection con, int idFactura, EstadoFactura estado) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("UPDATE factura SET estado=? WHERE id_factura=?")) {
            ps.setString(1, estado.name());
            ps.setInt(2, idFactura);
            ps.executeUpdate();
        }
    }

    @Override
    public Factura buscarPorId(Connection con, int idFactura, boolean bloquear) throws SQLException {
        if (bloquear) {
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT id_factura FROM factura WHERE id_factura=? FOR UPDATE")) {
                ps.setInt(1, idFactura);
                ps.executeQuery().close();
            }
        }
        String sql = "SELECT f.*, "
                + "pc.nombre AS c_nombre, pc.nit AS c_nit, pc.telefono AS c_tel, c.direccion AS c_dir, "
                + "pe.nombre AS e_nombre, pe.nit AS e_nit, pe.telefono AS e_tel, e.codigo_empleado, e.puesto, "
                + "b.nombre AS b_nombre, b.ubicacion AS b_ubicacion, b.activa AS b_activa "
                + "FROM factura f "
                + "JOIN persona pc ON pc.id = f.id_cliente LEFT JOIN cliente c ON c.id = f.id_cliente "
                + "JOIN persona pe ON pe.id = f.id_empleado LEFT JOIN empleado e ON e.id = f.id_empleado "
                + "LEFT JOIN bodega b ON b.id_bodega = f.id_bodega "
                + "WHERE f.id_factura = ?";
        Factura f = null;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idFactura);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Cliente cliente = new Cliente(rs.getInt("id_cliente"), rs.getString("c_nombre"),
                            rs.getString("c_nit"), rs.getString("c_tel"), rs.getString("c_dir"));
                    Empleado empleado = new Empleado(rs.getInt("id_empleado"), rs.getString("e_nombre"),
                            rs.getString("e_nit"), rs.getString("e_tel"),
                            rs.getString("codigo_empleado"), rs.getString("puesto"));
                    Bodega bodega = null;
                    int idBodega = rs.getInt("id_bodega");
                    if (!rs.wasNull()) {
                        bodega = new Bodega(idBodega, rs.getString("b_nombre"),
                                rs.getString("b_ubicacion"), rs.getBoolean("b_activa"));
                    }
                    f = new Factura(rs.getInt("id_factura"), rs.getDate("fecha").toLocalDate(),
                            cliente, empleado, bodega, EstadoFactura.valueOf(rs.getString("estado")));
                }
            }
        }
        if (f != null) {
            f.setDetalles(detalleDAO.listarPorFactura(con, idFactura));
        }
        return f;
    }

    @Override
    public Factura buscarPorId(int idFactura) {
        return Transaccion.consultar("buscar la factura", con -> buscarPorId(con, idFactura, false));
    }

    @Override
    public List<FacturaResumen> listarResumen(EstadoFactura estado) {
        String sql = "SELECT f.id_factura, f.fecha, f.estado, pc.nombre AS cliente, pe.nombre AS empleado, "
                + "COALESCE(b.nombre, '-') AS bodega, "
                + "COALESCE((SELECT SUM(d.cantidad * d.precio_unitario) FROM detalle_factura d "
                + "          WHERE d.id_factura = f.id_factura), 0) AS total, "
                + "COALESCE((SELECT SUM(pg.monto) FROM pago pg WHERE pg.id_factura = f.id_factura), 0) AS pagado "
                + "FROM factura f "
                + "JOIN persona pc ON pc.id = f.id_cliente "
                + "JOIN persona pe ON pe.id = f.id_empleado "
                + "LEFT JOIN bodega b ON b.id_bodega = f.id_bodega "
                + (estado != null ? "WHERE f.estado = ? " : "")
                + "ORDER BY f.id_factura DESC";
        return Transaccion.consultar("listar facturas", con -> {
            List<FacturaResumen> lista = new ArrayList<>();
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                if (estado != null) {
                    ps.setString(1, estado.name());
                }
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        lista.add(new FacturaResumen(rs.getInt("id_factura"), rs.getDate("fecha").toLocalDate(),
                                rs.getString("cliente"), rs.getString("empleado"), rs.getString("bodega"),
                                EstadoFactura.valueOf(rs.getString("estado")),
                                rs.getDouble("total"), rs.getDouble("pagado")));
                    }
                }
            }
            return lista;
        });
    }
}
