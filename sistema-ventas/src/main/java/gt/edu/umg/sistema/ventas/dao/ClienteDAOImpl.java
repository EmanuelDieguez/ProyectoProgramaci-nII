package gt.edu.umg.sistema.ventas.dao;

import gt.edu.umg.sistema.ventas.modelo.Cliente;
import gt.edu.umg.sistema.ventas.modelo.TelefonoCliente;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ClienteDAOImpl implements ClienteDAO {

    private static final String SELECT = "SELECT p.id, p.nombre, p.nit, p.telefono, c.direccion "
            + "FROM persona p JOIN cliente c ON p.id = c.id ";

    private final TelefonoClienteDAO telefonoDAO = new TelefonoClienteDAOImpl();

    @Override
    public void guardar(Cliente c) {
        // persona + cliente + teléfonos en la misma transacción
        Transaccion.ejecutar("guardar el cliente", con -> {
            String sqlPersona = "INSERT INTO persona (nombre, nit, telefono, tipo) VALUES (?,?,?,'CLIENTE')";
            try (PreparedStatement ps = con.prepareStatement(sqlPersona, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, c.getNombre());
                ps.setString(2, c.getNit());
                ps.setString(3, c.getTelefonoPrincipal());
                ps.executeUpdate();
                c.setId(JdbcUtil.idGenerado(ps));
            }
            try (PreparedStatement ps = con.prepareStatement("INSERT INTO cliente (id, direccion) VALUES (?,?)")) {
                ps.setInt(1, c.getId());
                ps.setString(2, c.getDireccion());
                ps.executeUpdate();
            }
            guardarTelefonos(con, c);
            return null;
        });
    }

    @Override
    public Cliente buscarPorId(int id) {
        return Transaccion.consultar("buscar el cliente", con -> {
            Cliente cliente = null;
            try (PreparedStatement ps = con.prepareStatement(SELECT + "WHERE p.id = ?")) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        cliente = mapear(rs);
                    }
                }
            }
            if (cliente != null) {
                cliente.setTelefonos(telefonoDAO.listarPorCliente(con, id));
            }
            return cliente;
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
            // Una sola consulta para los teléfonos de TODOS los clientes
            Map<Integer, List<TelefonoCliente>> telefonos = telefonoDAO.listarAgrupados(con);
            for (Cliente c : lista) {
                c.setTelefonos(telefonos.getOrDefault(c.getId(), new ArrayList<>()));
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
                ps.setString(3, c.getTelefonoPrincipal());
                ps.setInt(4, c.getId());
                ps.executeUpdate();
            }
            try (PreparedStatement ps = con.prepareStatement("UPDATE cliente SET direccion=? WHERE id=?")) {
                ps.setString(1, c.getDireccion());
                ps.setInt(2, c.getId());
                ps.executeUpdate();
            }
            // Se reemplaza la lista completa: se borran los anteriores y se insertan los actuales
            telefonoDAO.eliminarPorCliente(con, c.getId());
            guardarTelefonos(con, c);
            return null;
        });
    }

    @Override
    public void eliminar(int id) {
        Transaccion.ejecutar("eliminar el cliente", con -> {
            telefonoDAO.eliminarPorCliente(con, id);
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

    private void guardarTelefonos(Connection con, Cliente c) throws SQLException {
        for (TelefonoCliente t : c.getTelefonos()) {
            t.setIdCliente(c.getId());
            telefonoDAO.insertar(con, t);
        }
    }

    private Cliente mapear(ResultSet rs) throws SQLException {
        return new Cliente(rs.getInt("id"), rs.getString("nombre"), rs.getString("nit"),
                rs.getString("telefono"), rs.getString("direccion"));
    }
}
