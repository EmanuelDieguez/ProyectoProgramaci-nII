package gt.edu.umg.sistema.ventas.modelo;

import java.time.LocalDateTime;

public class Pago {
    private int idPago;
    private int idFactura;
    private double monto;
    private LocalDateTime fechaPago;
    private MetodoPago metodoPago;

    public Pago() {}

    public Pago(int idPago, int idFactura, double monto, LocalDateTime fechaPago, MetodoPago metodoPago) {
        this.idPago = idPago;
        this.idFactura = idFactura;
        this.monto = monto;
        this.fechaPago = fechaPago;
        this.metodoPago = metodoPago;
    }

    public int getIdPago() { return idPago; }
    public void setIdPago(int idPago) { this.idPago = idPago; }
    public int getIdFactura() { return idFactura; }
    public void setIdFactura(int idFactura) { this.idFactura = idFactura; }
    public double getMonto() { return monto; }
    public void setMonto(double monto) { this.monto = monto; }
    public LocalDateTime getFechaPago() { return fechaPago; }
    public void setFechaPago(LocalDateTime fechaPago) { this.fechaPago = fechaPago; }
    public MetodoPago getMetodoPago() { return metodoPago; }
    public void setMetodoPago(MetodoPago metodoPago) { this.metodoPago = metodoPago; }
}
