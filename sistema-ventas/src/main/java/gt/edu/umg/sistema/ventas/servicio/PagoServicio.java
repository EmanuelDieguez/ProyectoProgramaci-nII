package gt.edu.umg.sistema.ventas.servicio;

import gt.edu.umg.sistema.ventas.dao.*;
import gt.edu.umg.sistema.ventas.evento.BusEventos;
import gt.edu.umg.sistema.ventas.evento.EventoSistema;
import gt.edu.umg.sistema.ventas.modelo.EstadoFactura;
import gt.edu.umg.sistema.ventas.modelo.Factura;
import gt.edu.umg.sistema.ventas.modelo.Pago;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

public class PagoServicio {

    private static final double CENTAVO = 0.005;

    private final PagoDAO pagoDAO = new PagoDAOImpl();
    private final FacturaDAO facturaDAO = new FacturaDAOImpl();

    /**
     * Registra el pago validando el saldo. Si con este pago se cubre el total,
     * la factura pasa automáticamente a PAGADA.
     * @return el estado en que queda la factura
     */
    public EstadoFactura registrarPago(Pago p) {
        if (p.getMonto() <= 0) {
            throw new ReglaNegocioException("El monto debe ser mayor que cero.");
        }
        if (p.getMetodoPago() == null) {
            throw new ReglaNegocioException("Seleccione el método de pago.");
        }
        if (p.getFechaPago() == null) {
            p.setFechaPago(LocalDateTime.now());
        }
        EstadoFactura estado = Transaccion.ejecutar("registrar el pago", con -> {
            Factura f = facturaDAO.buscarPorId(con, p.getIdFactura(), true);
            if (f == null) {
                throw new ReglaNegocioException("No existe la factura #" + p.getIdFactura() + ".");
            }
            if (f.getEstado() == EstadoFactura.ANULADA) {
                throw new ReglaNegocioException("La factura está anulada; no admite pagos.");
            }
            if (f.getEstado() == EstadoFactura.PAGADA) {
                throw new ReglaNegocioException("La factura ya está pagada.");
            }
            double total = f.calcularTotal();
            double pagado = pagoDAO.totalPagado(con, f.getIdFactura());
            double saldo = total - pagado;
            if (p.getMonto() > saldo + CENTAVO) {
                throw new ReglaNegocioException(String.format(Locale.US,
                        "El monto (Q %,.2f) excede el saldo pendiente (Q %,.2f).", p.getMonto(), saldo));
            }
            pagoDAO.insertar(con, p);
            if (pagado + p.getMonto() >= total - CENTAVO) {
                facturaDAO.actualizarEstado(con, f.getIdFactura(), EstadoFactura.PAGADA);
                return EstadoFactura.PAGADA;
            }
            return EstadoFactura.PENDIENTE;
        });
        BusEventos.publicar(EventoSistema.PAGOS, EventoSistema.FACTURAS);
        return estado;
    }

    public List<Pago> obtenerPagosPorFactura(int idFactura) {
        return pagoDAO.listarPorFactura(idFactura);
    }
}
