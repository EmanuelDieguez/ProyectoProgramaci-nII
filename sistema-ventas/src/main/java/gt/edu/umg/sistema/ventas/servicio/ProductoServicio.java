package gt.edu.umg.sistema.ventas.servicio;

import gt.edu.umg.sistema.ventas.dao.ProductoDAO;
import gt.edu.umg.sistema.ventas.dao.ProductoDAOImpl;
import gt.edu.umg.sistema.ventas.dao.Transaccion;
import gt.edu.umg.sistema.ventas.evento.BusEventos;
import gt.edu.umg.sistema.ventas.evento.EventoSistema;
import gt.edu.umg.sistema.ventas.modelo.Bodega;
import gt.edu.umg.sistema.ventas.modelo.Producto;
import gt.edu.umg.sistema.ventas.modelo.TipoMovimiento;
import java.util.List;

public class ProductoServicio {

    private final ProductoDAO productoDAO = new ProductoDAOImpl();
    private final InventarioServicio inventario = new InventarioServicio();

    /** Crea el producto y, si se indica, ingresa su existencia inicial a una bodega. */
    public void crear(Producto p, Bodega bodegaInicial, int existenciaInicial) {
        validar(p);
        if (existenciaInicial < 0) {
            throw new ReglaNegocioException("La existencia inicial no puede ser negativa.");
        }
        if (existenciaInicial > 0 && bodegaInicial == null) {
            throw new ReglaNegocioException("Seleccione la bodega donde ingresará la existencia inicial.");
        }
        Transaccion.ejecutar("guardar el producto", con -> {
            productoDAO.insertar(con, p);
            if (existenciaInicial > 0) {
                inventario.aplicarMovimiento(con, bodegaInicial, p, TipoMovimiento.ENTRADA,
                        existenciaInicial, "Inventario inicial");
            }
            return null;
        });
        BusEventos.publicar(EventoSistema.PRODUCTOS, EventoSistema.INVENTARIO);
    }

    public void actualizar(Producto p) {
        validar(p);
        productoDAO.actualizar(p);
        BusEventos.publicar(EventoSistema.PRODUCTOS);
    }

    public void eliminar(int idProducto) {
        productoDAO.eliminar(idProducto);
        BusEventos.publicar(EventoSistema.PRODUCTOS, EventoSistema.INVENTARIO);
    }

    public List<Producto> listar() {
        return productoDAO.listarTodos();
    }

    private void validar(Producto p) {
        if (p.getNombre() == null || p.getNombre().isBlank()) {
            throw new ReglaNegocioException("El nombre del producto es obligatorio.");
        }
        if (p.getPrecio() <= 0) {
            throw new ReglaNegocioException("El precio debe ser mayor que cero.");
        }
        if (p.getCategoria() == null) {
            throw new ReglaNegocioException("Seleccione una categoría (créela primero si no existe).");
        }
    }
}
