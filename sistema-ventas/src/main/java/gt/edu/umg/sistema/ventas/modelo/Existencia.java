package gt.edu.umg.sistema.ventas.modelo;

/** Cantidad de un producto dentro de una bodega específica (tabla bodega_producto). */
public class Existencia {
    private final Bodega bodega;
    private final Producto producto;
    private final int cantidad;

    public Existencia(Bodega bodega, Producto producto, int cantidad) {
        this.bodega = bodega;
        this.producto = producto;
        this.cantidad = cantidad;
    }

    public Bodega getBodega() { return bodega; }
    public Producto getProducto() { return producto; }
    public int getCantidad() { return cantidad; }
}
