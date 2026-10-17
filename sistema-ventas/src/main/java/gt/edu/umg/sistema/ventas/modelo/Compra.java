package gt.edu.umg.sistema.ventas.modelo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Compra a proveedor: ingresa mercadería a una bodega. */
public class Compra {
    private int idCompra;
    private LocalDateTime fecha;
    private String proveedor;
    private Bodega bodega;
    private Empleado empleado;
    private List<DetalleCompra> detalles = new ArrayList<>();

    public Compra() {}

    public int getIdCompra() { return idCompra; }
    public void setIdCompra(int idCompra) { this.idCompra = idCompra; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
    public String getProveedor() { return proveedor; }
    public void setProveedor(String proveedor) { this.proveedor = proveedor; }
    public Bodega getBodega() { return bodega; }
    public void setBodega(Bodega bodega) { this.bodega = bodega; }
    public Empleado getEmpleado() { return empleado; }
    public void setEmpleado(Empleado empleado) { this.empleado = empleado; }
    public List<DetalleCompra> getDetalles() { return detalles; }
    public void setDetalles(List<DetalleCompra> detalles) { this.detalles = detalles; }

    public void agregarDetalle(DetalleCompra d) { detalles.add(d); }

    public double calcularTotal() {
        double total = 0;
        for (DetalleCompra d : detalles) {
            total += d.calcularSubtotal();
        }
        return total;
    }
}
