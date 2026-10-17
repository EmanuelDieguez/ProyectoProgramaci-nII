package gt.edu.umg.sistema.ventas.dao;

import gt.edu.umg.sistema.ventas.modelo.Cliente;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ClienteDAOImpl implements ClienteDAO {

    private static final String SELECT = "SELECT p.id, p.nombre, p.nit, p.telefono, c.direccion "
            + "FROM persona p JOIN cliente c ON p.id = c.id ";

    @Override
    public void guardar(Cliente c) {
        // persona + cliente en la misma transacción
        Transaccion.ejecutar("guardar el cliente", con -> {
            String sqlPersona = "INSERT INTO persona (nombre, nit, telefono, tipo) VALUES (?,?,?,'CLIENTE')";
            try (PreparedStatement ps = con.prepareStatement(sqlPersona, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, c.getNombre());
                ps.setString(2, c.getNit());
                ps.setString(3, c.getTelefono());
                ps.executeUpdate();
                c.setId(JdbcUtil.idGenerado(ps));
            }
            try (PreparedStatement ps = con.prepareStatement("INSERT INTO cliente (id, direccion) VALUES (?,?)")) {
                ps.setInt(1, c.getId());
                ps.setString(2, c.getDireccion());
                ps.executeUpdate();
            }
            return null;
        });
    }

    @Override
    public Cliente buscarPorId(int id) {
        return Transaccion.consultar("buscar el cliente", con -> {
            try (PreparedStatement ps = con.prepareStatement(SELECT + "WHERE p.id = ?")) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? mapear(rs) : null;
                }
            }
        });
    }

    @Override
    public List<Cliente> listarTodos() {
        return Transaccion.consultar("listar clientes", con -> {
            List<Cliente> lista = new ArrayList<>();
            try (PreparedStatement ps = con.prepareStatement(SELECT + "ORDER BY p.nombre");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
            return lista;
        });
    }

    @Override
    public void actualizar(Cliente c) {
        Transaccion.ejecutar("actualizar el cliente", con -> {
            try (PreparedStatement ps = con.prepareStatement("UPDATE persona SET nombre=?, nit=?, telefono=? WHERE id=?")) {
                ps.setString(1, c.getNombre());
                ps.setString(2, c.getNit());
                ps.setString(3, c.getTelefono());
                ps.setInt(4, c.getId());
                ps.executeUpdate();
            }
            try (PreparedStatement ps = con.prepareStatement("UPDATE cliente SET direccion=? WHERE id=?")) {
                ps.setString(1, c.getDireccion());
                ps.setInt(2, c.getId());
                ps.executeUpdate();
            }
            return null;
        });
    }

    @Override
    public void eliminar(int id) {
        Transaccion.ejecutar("eliminar el cliente", con -> {
            try (PreparedStatement ps = con.prepareStatement("DELETE FROM cliente WHERE id=?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = con.prepareStatement("DELETE FROM persona WHERE id=?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }
            return null;
        });
    }

    private Cliente mapear(ResultSet rs) throws SQLException {
        return new Cliente(rs.getInt("id"), rs.getString("nombre"), rs.getString("nit"),
                rs.getString("telefono"), rs.getString("direccion"));
    }
}
