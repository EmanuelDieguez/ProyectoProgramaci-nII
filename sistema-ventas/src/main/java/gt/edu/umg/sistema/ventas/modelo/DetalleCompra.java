package gt.edu.umg.sistema.ventas.modelo;

public class DetalleCompra {
    private int idDetalle;
    private int idCompra;
    private Producto producto;
    private int cantidad;
    private double costoUnitario;

    public DetalleCompra() {}

    public DetalleCompra(Producto producto, int cantidad, double costoUnitario) {
        this.producto = producto;
        this.cantidad = cantidad;
        this.costoUnitario = costoUnitario;
    }

    public int getIdDetalle() { return idDetalle; }
    public void setIdDetalle(int idDetalle) { this.idDetalle = idDetalle; }
    public int getIdCompra() { return idCompra; }
    public void setIdCompra(int idCompra) { this.idCompra = idCompra; }
    public Producto getProducto() { return producto; }
    public void setProducto(Producto producto) { this.producto = producto; }
    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }
    public double getCostoUnitario() { return costoUnitario; }
    public void setCostoUnitario(double costoUnitario) { this.costoUnitario = costoUnitario; }

    public double calcularSubtotal() {
        return cantidad * costoUnitario;
    }
}
