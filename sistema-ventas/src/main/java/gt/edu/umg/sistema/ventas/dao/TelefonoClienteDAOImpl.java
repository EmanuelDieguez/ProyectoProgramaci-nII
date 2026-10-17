package gt.edu.umg.sistema.ventas.dao;

import gt.edu.umg.sistema.ventas.modelo.TelefonoCliente;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TelefonoClienteDAOImpl implements TelefonoClienteDAO {

    @Override
    public void insertar(Connection con, TelefonoCliente t) throws SQLException {
        String sql = "INSERT INTO telefono_cliente (id_cliente, numero, tipo) VALUES (?,?,?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, t.getIdCliente());
            ps.setString(2, t.getNumero());
            ps.setString(3, t.getTipo());
            ps.executeUpdate();
            t.setIdTelefono(JdbcUtil.idGenerado(ps));
        }
    }

    @Override
    public void eliminarPorCliente(Connection con, int idCliente) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("DELETE FROM telefono_cliente WHERE id_cliente = ?")) {
            ps.setInt(1, idCliente);
            ps.executeUpdate();
        }
    }

    @Override
    public List<TelefonoCliente> listarPorCliente(Connection con, int idCliente) throws SQLException {
        List<TelefonoCliente> lista = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT * FROM telefono_cliente WHERE id_cliente = ? ORDER BY id_telefono")) {
            ps.setInt(1, idCliente);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
        }
        return lista;
    }

    @Override
    public Map<Integer, List<TelefonoCliente>> listarAgrupados(Connection con) throws SQLException {
        Map<Integer, List<TelefonoCliente>> mapa = new HashMap<>();
        try (PreparedStatement ps = con.prepareStatement("SELECT * FROM telefono_cliente ORDER BY id_telefono");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                TelefonoCliente t = mapear(rs);
                mapa.computeIfAbsent(t.getIdCliente(), k -> new ArrayList<>()).add(t);
            }
        }
        return mapa;
    }

    private TelefonoCliente mapear(ResultSet rs) throws SQLException {
        return new TelefonoCliente(rs.getInt("id_telefono"), rs.getInt("id_cliente"),
                rs.getString("numero"), rs.getString("tipo"));
    }
}