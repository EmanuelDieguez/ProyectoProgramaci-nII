package gt.edu.umg.sistema.ventas.modelo;

import java.time.LocalDateTime;

/** Una línea del kardex: cada entrada o salida de producto en una bodega. */
public class MovimientoInventario {
    private int idMovimiento;
    private LocalDateTime fecha;
    private Bodega bodega;
    private Producto producto;
    private TipoMovimiento tipo;
    private int cantidad;
    private int saldo;
    private String referencia;

    public MovimientoInventario() {}

    public int getIdMovimiento() { return idMovimiento; }
    public void setIdMovimiento(int idMovimiento) { this.idMovimiento = idMovimiento; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
    public Bodega getBodega() { return bodega; }
    public void setBodega(Bodega bodega) { this.bodega = bodega; }
    public Producto getProducto() { return producto; }
    public void setProducto(Producto producto) { this.producto = producto; }
    public TipoMovimiento getTipo() { return tipo; }
    public void setTipo(TipoMovimiento tipo) { this.tipo = tipo; }
    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }
    public int getSaldo() { return saldo; }
    public void setSaldo(int saldo) { this.saldo = saldo; }
    public String getReferencia() { return referencia; }
    public void setReferencia(String referencia) { this.referencia = referencia; }
}
