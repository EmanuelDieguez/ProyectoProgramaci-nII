package gt.edu.umg.sistema.ventas.dao;

import gt.edu.umg.sistema.ventas.modelo.Bodega;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BodegaDAOImpl implements BodegaDAO {

    @Override
    public void guardar(Bodega b) {
        Transaccion.consultar("guardar la bodega", con -> {
            String sql = "INSERT INTO bodega (nombre, ubicacion, activa) VALUES (?,?,?)";
            try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, b.getNombre());
                ps.setString(2, b.getUbicacion());
                ps.setBoolean(3, b.isActiva());
                ps.executeUpdate();
                b.setIdBodega(JdbcUtil.idGenerado(ps));
            }
            return null;
        });
    }

    @Override
    public Bodega buscarPorId(int id) {
        return Transaccion.consultar("buscar la bodega", con -> {
            try (PreparedStatement ps = con.prepareStatement("SELECT * FROM bodega WHERE id_bodega=?")) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? mapear(rs) : null;
                }
            }
        });
    }

    @Override
    public List<Bodega> listarTodos() {
        return listar("SELECT * FROM bodega ORDER BY nombre");
    }

    @Override
    public List<Bodega> listarActivas() {
        return listar("SELECT * FROM bodega WHERE activa = 1 ORDER BY nombre");
    }

    private List<Bodega> listar(String sql) {
        return Transaccion.consultar("listar bodegas", con -> {
            List<Bodega> lista = new ArrayList<>();
            try (PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
            return lista;
        });
    }

    @Override
    public void actualizar(Bodega b) {
        Transaccion.consultar("actualizar la bodega", con -> {
            String sql = "UPDATE bodega SET nombre=?, ubicacion=?, activa=? WHERE id_bodega=?";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, b.getNombre());
                ps.setString(2, b.getUbicacion());
                ps.setBoolean(3, b.isActiva());
                ps.setInt(4, b.getIdBodega());
                ps.executeUpdate();
            }
            return null;
        });
    }

    @Override
    public void eliminar(int id) {
        Transaccion.ejecutar("eliminar la bodega", con -> {
            try (PreparedStatement ps = con.prepareStatement(
                    "DELETE FROM bodega_producto WHERE id_bodega=? AND cantidad = 0")) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = con.prepareStatement("DELETE FROM bodega WHERE id_bodega=?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }
            return null;
        });
    }

    private Bodega mapear(ResultSet rs) throws SQLException {
        return new Bodega(rs.getInt("id_bodega"), rs.getString("nombre"),
                rs.getString("ubicacion"), rs.getBoolean("activa"));
    }
}
