package gt.edu.umg.sistema.ventas.modelo;

/**
 * La "existencia" de un producto es el TOTAL sumado de todas las bodegas.
 * No se edita a mano: la mantiene el InventarioServicio en cada movimiento.
 */
public class Producto {
    private int idProducto;
    private String nombre;
    private double precio;
    private int existencia;
    private Categoria categoria;

    public Producto() {}

    public Producto(int idProducto, String nombre, double precio, int existencia, Categoria categoria) {
        this.idProducto = idProducto;
        this.nombre = nombre;
        this.precio = precio;
        this.existencia = existencia;
        this.categoria = categoria;
    }

    public int getIdProducto() { return idProducto; }
    public void setIdProducto(int idProducto) { this.idProducto = idProducto; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public double getPrecio() { return precio; }
    public void setPrecio(double precio) { this.precio = precio; }
    public int getExistencia() { return existencia; }
    public void setExistencia(int existencia) { this.existencia = existencia; }
    public Categoria getCategoria() { return categoria; }
    public void setCategoria(Categoria categoria) { this.categoria = categoria; }

    public boolean hayExistencia(int cantidad) {
        return this.existencia >= cantidad;
    }

    @Override
    public String toString() { return nombre; }
}
