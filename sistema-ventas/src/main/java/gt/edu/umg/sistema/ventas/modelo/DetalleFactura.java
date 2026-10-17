package gt.edu.umg.sistema.ventas.modelo;

public class DetalleFactura {
    private int idDetalle;
    private Producto producto;
    private int cantidad;
    private double precioUnitario;
    private int idFactura;

    public DetalleFactura() {}

    public DetalleFactura(Producto producto, int cantidad, double precioUnitario) {
        this.producto = producto;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
    }

    public DetalleFactura(Producto producto, int cantidad, double precioUnitario, int idFactura) {
        this(producto, cantidad, precioUnitario);
        this.idFactura = idFactura;
    }

    public int getIdDetalle() { return idDetalle; }
    public void setIdDetalle(int idDetalle) { this.idDetalle = idDetalle; }
    public Producto getProducto() { return producto; }
    public void setProducto(Producto producto) { this.producto = producto; }
    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }
    public double getPrecioUnitario() { return precioUnitario; }
    public void setPrecioUnitario(double precioUnitario) { this.precioUnitario = precioUnitario; }
    public int getIdFactura() { return idFactura; }
    public void setIdFactura(int idFactura) { this.idFactura = idFactura; }

    public double calcularSubtotal() {
        return cantidad * precioUnitario;
    }
}
