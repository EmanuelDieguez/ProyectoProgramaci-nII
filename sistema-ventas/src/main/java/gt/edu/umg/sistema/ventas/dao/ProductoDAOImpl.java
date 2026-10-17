package gt.edu.umg.sistema.ventas.dao;

import gt.edu.umg.sistema.ventas.modelo.Categoria;
import gt.edu.umg.sistema.ventas.modelo.Producto;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductoDAOImpl implements ProductoDAO {

    // Un solo JOIN en lugar de buscar la categoría producto por producto
    private static final String SELECT = "SELECT p.id_producto, p.nombre, p.precio, p.existencia, "
            + "c.id_categoria, c.nombre AS cat_nombre, c.descripcion AS cat_desc "
            + "FROM producto p LEFT JOIN categoria c ON c.id_categoria = p.id_categoria ";

    @Override
    public void guardar(Producto p) {
        Transaccion.consultar("guardar el producto", con -> {
            insertar(con, p);
            return null;
        });
    }

    @Override
    public void insertar(Connection con, Producto p) throws SQLException {
        // La existencia inicia en 0: solo cambia con movimientos de inventario
        String sql = "INSERT INTO producto (nombre, precio, existencia, id_categoria) VALUES (?,?,0,?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.getNombre());
            ps.setDouble(2, p.getPrecio());
            setCategoria(ps, 3, p.getCategoria());
            ps.executeUpdate();
            p.setIdProducto(JdbcUtil.idGenerado(ps));
            p.setExistencia(0);
        }
    }

    @Override
    public Producto buscarPorId(int id) {
        return Transaccion.consultar("buscar el producto", con -> {
            try (PreparedStatement ps = con.prepareStatement(SELECT + "WHERE p.id_producto = ?")) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? mapear(rs) : null;
                }
            }
        });
    }

    @Override
    public List<Producto> listarTodos() {
        return Transaccion.consultar("listar productos", con -> {
            List<Producto> lista = new ArrayList<>();
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
    public void actualizar(Producto p) {
        // OJO: no se actualiza la existencia aquí; eso lo hace el inventario
        Transaccion.consultar("actualizar el producto", con -> {
            String sql = "UPDATE producto SET nombre=?, precio=?, id_categoria=? WHERE id_producto=?";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, p.getNombre());
                ps.setDouble(2, p.getPrecio());
                setCategoria(ps, 3, p.getCategoria());
                ps.setInt(4, p.getIdProducto());
                ps.executeUpdate();
            }
            return null;
        });
    }

    @Override
    public void ajustarExistencia(Connection con, int idProducto, int delta) throws SQLException {
        String sql = "UPDATE producto SET existencia = existencia + ? WHERE id_producto = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, delta);
            ps.setInt(2, idProducto);
            ps.executeUpdate();
        }
    }

    @Override
    public void eliminar(int id) {
        Transaccion.ejecutar("eliminar el producto", con -> {
            // Si el producto solo tiene existencias en 0 se limpian; si tiene movimientos, la FK lo impide
            try (PreparedStatement ps = con.prepareStatement(
                    "DELETE FROM bodega_producto WHERE id_producto=? AND cantidad = 0")) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = con.prepareStatement("DELETE FROM producto WHERE id_producto=?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }
            return null;
        });
    }

    private void setCategoria(PreparedStatement ps, int indice, Categoria c) throws SQLException {
        if (c == null) {
            ps.setNull(indice, Types.INTEGER);
        } else {
            ps.setInt(indice, c.getIdCategoria());
        }
    }

    private Producto mapear(ResultSet rs) throws SQLException {
        Categoria categoria = null;
        int idCat = rs.getInt("id_categoria");
        if (!rs.wasNull()) {
            categoria = new Categoria(idCat, rs.getString("cat_nombre"), rs.getString("cat_desc"));
        }
        return new Producto(rs.getInt("id_producto"), rs.getString("nombre"),
                rs.getDouble("precio"), rs.getInt("existencia"), categoria);
    }
}
