package gt.edu.umg.sistema.ventas.vista;

import gt.edu.umg.sistema.ventas.evento.EventoSistema;
import gt.edu.umg.sistema.ventas.modelo.*;
import gt.edu.umg.sistema.ventas.servicio.FacturaServicio;
import gt.edu.umg.sistema.ventas.servicio.PagoServicio;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDateTime;
import java.util.List;

/** Pagos de facturas pendientes. Cuando el saldo llega a cero la factura pasa a PAGADA. */
public class VistaPago extends FormularioBase {

    private final PagoServicio pagoServicio = new PagoServicio();
    private final FacturaServicio facturaServicio = new FacturaServicio();

    private final JComboBox<FacturaResumen> cbFactura = new JComboBox<>();
    private final JLabel lblCliente = UtilVista.etiquetaDato();
    private final JLabel lblTotal = UtilVista.etiquetaDato();
    private final JLabel lblPagado = UtilVista.etiquetaDato();
    private final JLabel lblSaldo = UtilVista.etiquetaTotal();
    private final JTextField txtMonto = new JTextField(10);
    private final JComboBox<MetodoPago> cbMetodo = new JComboBox<>(MetodoPago.values());
    private final DefaultTableModel modeloPagos = UtilVista.modelo("ID", "Fecha", "Método", "Monto");
    private final JTable tablaPagos = UtilVista.tabla(modeloPagos);

    public VistaPago() {
        super("Pagos", 720, 560);
        construir();
        cargarFacturas();
        escuchar(this::cargarFacturas, EventoSistema.FACTURAS, EventoSistema.PAGOS, EventoSistema.CLIENTES);
    }

    private void construir() {
        UtilVista.Formulario datos = new UtilVista.Formulario()
                .fila("Factura pendiente:", cbFactura)
                .fila("Cliente:", lblCliente)
                .fila("Total factura:", lblTotal)
                .fila("Pagado:", lblPagado)
                .fila("Saldo pendiente:", lblSaldo);
        datos.setBorder(UtilVista.seccion("Factura"));

        JButton btnRegistrar = UtilVista.boton("Registrar pago", UtilVista.EXITO);
        JButton btnSaldo = UtilVista.boton("Pagar saldo completo", null);
        UtilVista.Formulario pago = new UtilVista.Formulario()
                .fila("Monto (Q):", txtMonto, "Método:", cbMetodo);
        JPanel panelPago = new JPanel(new BorderLayout());
        panelPago.setBorder(UtilVista.seccion("Nuevo pago"));
        panelPago.add(pago, BorderLayout.CENTER);
        panelPago.add(UtilVista.botones(FlowLayout.RIGHT, btnSaldo, btnRegistrar), BorderLayout.SOUTH);

        JPanel norte = new JPanel(new BorderLayout());
        norte.add(datos, BorderLayout.NORTH);
        norte.add(panelPago, BorderLayout.SOUTH);

        UtilVista.columnasMoneda(tablaPagos, 3);
        UtilVista.anchos(tablaPagos, 50, 150, 130, 110);
        add(norte, BorderLayout.NORTH);
        add(UtilVista.scroll(tablaPagos, "Pagos registrados a esta factura"), BorderLayout.CENTER);

        cbFactura.addActionListener(e -> ejecutar(this::mostrarFactura));
        btnSaldo.addActionListener(e -> {
            if (cbFactura.getSelectedItem() instanceof FacturaResumen f) {
                txtMonto.setText(String.format(java.util.Locale.US, "%.2f", f.saldo()));
            }
        });
        btnRegistrar.addActionListener(e -> ejecutar(this::registrar));
        txtMonto.addActionListener(e -> ejecutar(this::registrar));
    }

    /** Llamado desde Facturación para dejar lista la factura recién emitida. */
    public void seleccionarFactura(int idFactura) {
        for (int i = 0; i < cbFactura.getItemCount(); i++) {
            if (cbFactura.getItemAt(i).idFactura() == idFactura) {
                cbFactura.setSelectedIndex(i);
                mostrarFactura();
                txtMonto.requestFocus();
                return;
            }
        }
        info("La factura #" + idFactura + " no tiene saldo pendiente (está pagada o anulada).");
    }

    private void registrar() {
        FacturaResumen f = (FacturaResumen) cbFactura.getSelectedItem();
        if (f == null) {
            info("No hay facturas pendientes de pago.");
            return;
        }
        Pago p = new Pago();
        p.setIdFactura(f.idFactura());
        p.setMonto(UtilVista.decimal(txtMonto, "Monto"));
        p.setMetodoPago((MetodoPago) cbMetodo.getSelectedItem());
        p.setFechaPago(LocalDateTime.now());
        EstadoFactura estado = pagoServicio.registrarPago(p);
        txtMonto.setText("");
        if (estado == EstadoFactura.PAGADA) {
            info("Pago registrado. La factura #" + f.idFactura() + " quedó PAGADA en su totalidad.");
        } else {
            notificar("Pago de " + UtilVista.q(p.getMonto()) + " registrado a la factura #" + f.idFactura());
        }
    }

    private void mostrarFactura() {
        modeloPagos.setRowCount(0);
        FacturaResumen f = (FacturaResumen) cbFactura.getSelectedItem();
        if (f == null) {
            lblCliente.setText("-");
            lblTotal.setText("-");
            lblPagado.setText("-");
            lblSaldo.setText(UtilVista.q(0));
            return;
        }
        lblCliente.setText(f.cliente());
        lblTotal.setText(UtilVista.q(f.total()));
        lblPagado.setText(UtilVista.q(f.pagado()));
        lblSaldo.setText(UtilVista.q(f.saldo()));
        for (Pago p : pagoServicio.obtenerPagosPorFactura(f.idFactura())) {
            modeloPagos.addRow(new Object[]{p.getIdPago(), p.getFechaPago().format(UtilVista.FECHA_HORA),
                    p.getMetodoPago().name(), p.getMonto()});
        }
    }

    private void cargarFacturas() {
        List<FacturaResumen> pendientes = facturaServicio.listar(EstadoFactura.PENDIENTE);
        UtilVista.recargarCombo(cbFactura, pendientes, FacturaResumen::idFactura);
        mostrarFactura();
    }
}
