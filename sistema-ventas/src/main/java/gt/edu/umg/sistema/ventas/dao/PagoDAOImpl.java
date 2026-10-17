package gt.edu.umg.sistema.ventas.dao;

import gt.edu.umg.sistema.ventas.modelo.MetodoPago;
import gt.edu.umg.sistema.ventas.modelo.Pago;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PagoDAOImpl implements PagoDAO {

    @Override
    public void insertar(Connection con, Pago p) throws SQLException {
        String sql = "INSERT INTO pago (id_factura, monto, fecha_pago, metodo_pago) VALUES (?,?,?,?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, p.getIdFactura());
            ps.setDouble(2, p.getMonto());
            ps.setTimestamp(3, Timestamp.valueOf(p.getFechaPago()));
            ps.setString(4, p.getMetodoPago().name());
            ps.executeUpdate();
            p.setIdPago(JdbcUtil.idGenerado(ps));
        }
    }

    @Override
    public double totalPagado(Connection con, int idFactura) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT COALESCE(SUM(monto), 0) FROM pago WHERE id_factura = ?")) {
            ps.setInt(1, idFactura);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getDouble(1);
            }
        }
    }

    @Override
    public List<Pago> listarPorFactura(int idFactura) {
        return Transaccion.consultar("listar pagos", con -> {
            List<Pago> lista = new ArrayList<>();
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT * FROM pago WHERE id_factura = ? ORDER BY fecha_pago")) {
                ps.setInt(1, idFactura);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        lista.add(new Pago(rs.getInt("id_pago"), rs.getInt("id_factura"), rs.getDouble("monto"),
                                rs.getTimestamp("fecha_pago").toLocalDateTime(),
                                MetodoPago.valueOf(rs.getString("metodo_pago"))));
                    }
                }
            }
            return lista;
        });
    }
}
