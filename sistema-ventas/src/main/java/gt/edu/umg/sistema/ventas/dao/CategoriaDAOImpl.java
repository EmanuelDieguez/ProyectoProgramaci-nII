package gt.edu.umg.sistema.ventas.dao;

import gt.edu.umg.sistema.ventas.modelo.Categoria;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CategoriaDAOImpl implements CategoriaDAO {

    @Override
    public void guardar(Categoria c) {
        Transaccion.consultar("guardar la categoría", con -> {
            String sql = "INSERT INTO categoria (nombre, descripcion) VALUES (?,?)";
            try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, c.getNombre());
                ps.setString(2, c.getDescripcion());
                ps.executeUpdate();
                c.setIdCategoria(JdbcUtil.idGenerado(ps));
            }
            return null;
        });
    }

    @Override
    public Categoria buscarPorId(int id) {
        return Transaccion.consultar("buscar la categoría", con -> {
            try (PreparedStatement ps = con.prepareStatement("SELECT * FROM categoria WHERE id_categoria = ?")) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? mapear(rs) : null;
                }
            }
        });
    }

    @Override
    public List<Categoria> listarTodos() {
        return Transaccion.consultar("listar categorías", con -> {
            List<Categoria> lista = new ArrayList<>();
            try (PreparedStatement ps = con.prepareStatement("SELECT * FROM categoria ORDER BY nombre");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
            return lista;
        });
    }

    @Override
    public void actualizar(Categoria c) {
        Transaccion.consultar("actualizar la categoría", con -> {
            String sql = "UPDATE categoria SET nombre=?, descripcion=? WHERE id_categoria=?";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, c.getNombre());
                ps.setString(2, c.getDescripcion());
                ps.setInt(3, c.getIdCategoria());
                ps.executeUpdate();
            }
            return null;
        });
    }

    @Override
    public void eliminar(int id) {
        Transaccion.consultar("eliminar la categoría", con -> {
            try (PreparedStatement ps = con.prepareStatement("DELETE FROM categoria WHERE id_categoria=?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }
            return null;
        });
    }

    private Categoria mapear(ResultSet rs) throws SQLException {
        return new Categoria(rs.getInt("id_categoria"), rs.getString("nombre"), rs.getString("descripcion"));
    }
}
