package gt.edu.umg.sistema.ventas.modelo;

import java.time.LocalDate;

/** Vista de una factura para listados: incluye total y lo pagado sin cargar todo el detalle. */
public record FacturaResumen(int idFactura, LocalDate fecha, String cliente, String empleado,
                             String bodega, EstadoFactura estado, double total, double pagado) {

    public double saldo() {
        return Math.max(0, Math.round((total - pagado) * 100) / 100.0);
    }

    @Override
    public String toString() {
        return String.format(java.util.Locale.US, "#%d - %s - Saldo Q %,.2f", idFactura, cliente, saldo());
    }
}
