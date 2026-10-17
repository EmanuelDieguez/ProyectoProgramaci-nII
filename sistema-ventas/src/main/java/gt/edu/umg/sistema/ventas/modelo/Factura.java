package gt.edu.umg.sistema.ventas.modelo;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Factura {
    private int idFactura;
    private LocalDate fecha;
    private Cliente cliente;
    private Empleado empleado;
    private Bodega bodega;          // bodega de la que sale la mercadería
    private EstadoFactura estado;
    private List<DetalleFactura> detalles = new ArrayList<>();

    public Factura() {}

    public Factura(int idFactura, LocalDate fecha, Cliente cliente, Empleado empleado,
                   Bodega bodega, EstadoFactura estado) {
        this.idFactura = idFactura;
        this.fecha = fecha;
        this.cliente = cliente;
        this.empleado = empleado;
        this.bodega = bodega;
        this.estado = estado;
    }

    public int getIdFactura() { return idFactura; }
    public void setIdFactura(int idFactura) { this.idFactura = idFactura; }
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }
    public Empleado getEmpleado() { return empleado; }
    public void setEmpleado(Empleado empleado) { this.empleado = empleado; }
    public Bodega getBodega() { return bodega; }
    public void setBodega(Bodega bodega) { this.bodega = bodega; }
    public EstadoFactura getEstado() { return estado; }
    public void setEstado(EstadoFactura estado) { this.estado = estado; }
    public List<DetalleFactura> getDetalles() { return detalles; }
    public void setDetalles(List<DetalleFactura> detalles) { this.detalles = detalles; }

    public void agregarDetalle(DetalleFactura detalle) {
        this.detalles.add(detalle);
    }

    public double calcularTotal() {
        double total = 0;
        for (DetalleFactura d : detalles) {
            total += d.calcularSubtotal();
        }
        return total;
    }
}
