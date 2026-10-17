package gt.edu.umg.sistema.ventas.dao;

import gt.edu.umg.sistema.ventas.modelo.Empleado;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EmpleadoDAOImpl implements EmpleadoDAO {

    private static final String SELECT = "SELECT p.id, p.nombre, p.nit, p.telefono, e.codigo_empleado, e.puesto "
            + "FROM persona p JOIN empleado e ON p.id = e.id ";

    @Override
    public void guardar(Empleado emp) {
        Transaccion.ejecutar("guardar el empleado", con -> {
            String sqlPersona = "INSERT INTO persona (nombre, nit, telefono, tipo) VALUES (?,?,?,'EMPLEADO')";
            try (PreparedStatement ps = con.prepareStatement(sqlPersona, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, emp.getNombre());
                ps.setString(2, emp.getNit());
                ps.setString(3, emp.getTelefono());
                ps.executeUpdate();
                emp.setId(JdbcUtil.idGenerado(ps));
            }
            String sqlEmpleado = "INSERT INTO empleado (id, codigo_empleado, puesto) VALUES (?,?,?)";
            try (PreparedStatement ps = con.prepareStatement(sqlEmpleado)) {
                ps.setInt(1, emp.getId());
                ps.setString(2, emp.getCodigoEmpleado());
                ps.setString(3, emp.getPuesto());
                ps.executeUpdate();
            }
            return null;
        });
    }

    @Override
    public Empleado buscarPorId(int id) {
        return Transaccion.consultar("buscar el empleado", con -> {
            try (PreparedStatement ps = con.prepareStatement(SELECT + "WHERE p.id = ?")) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? mapear(rs) : null;
                }
            }
        });
    }

    @Override
    public List<Empleado> listarTodos() {
        return Transaccion.consultar("listar empleados", con -> {
            List<Empleado> lista = new ArrayList<>();
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
    public void actualizar(Empleado emp) {
        Transaccion.ejecutar("actualizar el empleado", con -> {
            try (PreparedStatement ps = con.prepareStatement("UPDATE persona SET nombre=?, nit=?, telefono=? WHERE id=?")) {
                ps.setString(1, emp.getNombre());
                ps.setString(2, emp.getNit());
                ps.setString(3, emp.getTelefono());
                ps.setInt(4, emp.getId());
                ps.executeUpdate();
            }
            try (PreparedStatement ps = con.prepareStatement("UPDATE empleado SET codigo_empleado=?, puesto=? WHERE id=?")) {
                ps.setString(1, emp.getCodigoEmpleado());
                ps.setString(2, emp.getPuesto());
                ps.setInt(3, emp.getId());
                ps.executeUpdate();
            }
            return null;
        });
    }

    @Override
    public void eliminar(int id) {
        Transaccion.ejecutar("eliminar el empleado", con -> {
            try (PreparedStatement ps = con.prepareStatement("DELETE FROM empleado WHERE id=?")) {
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

    private Empleado mapear(ResultSet rs) throws SQLException {
        return new Empleado(rs.getInt("id"), rs.getString("nombre"), rs.getString("nit"),
                rs.getString("telefono"), rs.getString("codigo_empleado"), rs.getString("puesto"));
    }
}
