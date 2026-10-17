package gt.edu.umg.sistema.ventas.vista;

import gt.edu.umg.sistema.ventas.dao.*;
import gt.edu.umg.sistema.ventas.evento.EventoSistema;
import gt.edu.umg.sistema.ventas.modelo.*;
import gt.edu.umg.sistema.ventas.servicio.FacturaServicio;
import gt.edu.umg.sistema.ventas.servicio.InventarioServicio;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;

/**
 * Facturación. Al registrar una venta (todo en una transacción):
 *   factura + detalle -> SALIDA de la bodega -> baja existencia total -> Kardex
 * Luego se ofrece registrar el pago. Desde "Facturas emitidas" se puede pagar o anular.
 */
public class VistaFactura extends FormularioBase {

    private final FacturaServicio facturaServicio = new FacturaServicio();
    private final InventarioServicio inventarioServicio = new InventarioServicio();
    private final ClienteDAO clienteDAO = new ClienteDAOImpl();
    private final EmpleadoDAO empleadoDAO = new EmpleadoDAOImpl();
    private final ProductoDAO productoDAO = new ProductoDAOImpl();
    private final BodegaDAO bodegaDAO = new BodegaDAOImpl();

    // nueva venta
    private final JComboBox<Cliente> cbCliente = new JComboBox<>();
    private final JComboBox<Empleado> cbEmpleado = new JComboBox<>();
    private final JComboBox<Bodega> cbBodega = new JComboBox<>();
    private final JComboBox<Producto> cbProducto = new JComboBox<>();
    private final JTextField txtCantidad = new JTextField("1", 5);
    private final JLabel lblPrecio = UtilVista.etiquetaDato();
    private final JLabel lblDisponible = UtilVista.etiquetaDato();
    private final DefaultTableModel modeloDetalle = UtilVista.modelo("Producto", "Cantidad", "Precio", "Subtotal");
    private final JTable tablaDetalle = UtilVista.tabla(modeloDetalle);
    private final JLabel lblTotal = UtilVista.etiquetaTotal();
    private Factura facturaActual;
    private int idBodegaActual = 0;

    // facturas emitidas
    private final JComboBox<String> cbFiltro = new JComboBox<>(new String[]{"TODAS", "PENDIENTE", "PAGADA", "ANULADA"});
    private final DefaultTableModel modeloFacturas = UtilVista.modelo(
            "No.", "Fecha", "Cliente", "Vendedor", "Bodega", "Total", "Pagado", "Saldo", "Estado");
    private final JTable tablaFacturas = UtilVista.tabla(modeloFacturas);
    private final DefaultTableModel modeloVerDetalle = UtilVista.modelo("Producto", "Cantidad", "Precio", "Subtotal");
    private final JTable tablaVerDetalle = UtilVista.tabla(modeloVerDetalle);

    public VistaFactura() {
        super("Facturación", 980, 640);
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Nueva venta", construirNueva());
        tabs.addTab("Facturas emitidas", construirEmitidas());
        add(tabs);

        nuevaFactura();
        cargarCombos();
        cargarFacturas();
        escuchar(this::cargarCombos, EventoSistema.CLIENTES, EventoSistema.EMPLEADOS,
                EventoSistema.BODEGAS, EventoSistema.PRODUCTOS, EventoSistema.INVENTARIO);
        escuchar(this::cargarFacturas, EventoSistema.FACTURAS, EventoSistema.PAGOS);
    }

    // ================================================================== NUEVA VENTA
    private JPanel construirNueva() {
        JLabel lblFecha = UtilVista.etiquetaDato();
        lblFecha.setText(LocalDate.now().format(UtilVista.FECHA));
        UtilVista.Formulario encabezado = new UtilVista.Formulario()
                .fila("Cliente:", cbCliente, "Fecha:", lblFecha)
                .fila("Vendedor:", cbEmpleado, "Despachar de:", cbBodega);
        encabezado.setBorder(UtilVista.seccion("Encabezado"));

        JButton btnAgregar = UtilVista.boton("Agregar", UtilVista.PRIMARIO);
        UtilVista.Formulario linea = new UtilVista.Formulario()
                .fila("Producto:", cbProducto, "Precio:", lblPrecio, "Disponible:", lblDisponible, "Cantidad:", txtCantidad);
        JPanel panelLinea = new JPanel(new BorderLayout());
        panelLinea.setBorder(UtilVista.seccion("Agregar producto"));
        panelLinea.add(linea, BorderLayout.CENTER);
        panelLinea.add(UtilVista.botones(FlowLayout.RIGHT, btnAgregar), BorderLayout.EAST);

        JPanel norte = new JPanel(new BorderLayout());
        norte.add(encabezado, BorderLayout.NORTH);
        norte.add(panelLinea, BorderLayout.SOUTH);

        UtilVista.columnasMoneda(tablaDetalle, 2, 3);
        UtilVista.anchos(tablaDetalle, 320, 80, 100, 110);

        JButton btnQuitar = UtilVista.boton("Quitar línea", UtilVista.PELIGRO);
        JButton btnCancelar = UtilVista.boton("Cancelar venta", null);
        JButton btnRegistrar = UtilVista.boton("Registrar venta", UtilVista.EXITO);
        JPanel sur = new JPanel(new BorderLayout());
        sur.add(UtilVista.botones(FlowLayout.LEFT, new JLabel("Total:"), lblTotal), BorderLayout.WEST);
        sur.add(UtilVista.botones(FlowLayout.RIGHT, btnQuitar, btnCancelar, btnRegistrar), BorderLayout.EAST);

        JPanel p = new JPanel(new BorderLayout());
        p.add(norte, BorderLayout.NORTH);
        p.add(UtilVista.scroll(tablaDetalle, "Detalle de la factura"), BorderLayout.CENTER);
        p.add(sur, BorderLayout.SOUTH);

        cbProducto.addActionListener(e -> ejecutar(this::mostrarDatosProducto));
        cbBodega.addActionListener(e -> ejecutar(this::cambioDeBodega));
        btnAgregar.addActionListener(e -> ejecutar(this::agregar));
        txtCantidad.addActionListener(e -> ejecutar(this::agregar));   // Enter agrega
        btnQuitar.addActionListener(e -> quitarLinea());
        btnCancelar.addActionListener(e -> {
            if (facturaActual.getDetalles().isEmpty() || confirmar("¿Descartar la venta en curso?")) {
                nuevaFactura();
            }
        });
        btnRegistrar.addActionListener(e -> ejecutar(this::registrar));
        return p;
    }

    private void cambioDeBodega() {
        Bodega b = (Bodega) cbBodega.getSelectedItem();
        int nuevo = b == null ? 0 : b.getIdBodega();
        if (nuevo == idBodegaActual) {
            return;
        }
        if (!facturaActual.getDetalles().isEmpty()) {
            if (confirmar("Cambiar de bodega vaciará el detalle actual. ¿Continuar?")) {
                facturaActual.getDetalles().clear();
                refrescarDetalle();
            } else {
                // regresar a la bodega anterior
                for (int i = 0; i < cbBodega.getItemCount(); i++) {
                    if (cbBodega.getItemAt(i).getIdBodega() == idBodegaActual) {
                        cbBodega.setSelectedIndex(i);
                    }
                }
                return;
            }
        }
        idBodegaActual = nuevo;
        mostrarDatosProducto();
    }

    private int disponibleActual() {
        return inventarioServicio.existencia((Bodega) cbBodega.getSelectedItem(), (Producto) cbProducto.getSelectedItem());
    }

    private void mostrarDatosProducto() {
        Producto p = (Producto) cbProducto.getSelectedItem();
        if (p == null || cbBodega.getSelectedItem() == null) {
            lblPrecio.setText("-");
            lblDisponible.setText("-");
            return;
        }
        int disp = disponibleActual();
        lblPrecio.setText(UtilVista.q(p.getPrecio()));
        lblDisponible.setText(disp + " und.");
        lblDisponible.setForeground(disp > 0 ? UtilVista.EXITO : UtilVista.PELIGRO);
    }

    private void agregar() {
        facturaServicio.agregarDetalle(facturaActual, (Producto) cbProducto.getSelectedItem(),
                UtilVista.entero(txtCantidad, "Cantidad"), disponibleActual());
        txtCantidad.setText("1");
        refrescarDetalle();
        cbProducto.requestFocus();
    }

    private void quitarLinea() {
        int fila = UtilVista.filaModelo(tablaDetalle);
        if (fila >= 0) {
            facturaActual.getDetalles().remove(fila);
            refrescarDetalle();
        }
    }

    private void registrar() {
        facturaActual.setCliente((Cliente) cbCliente.getSelectedItem());
        facturaActual.setEmpleado((Empleado) cbEmpleado.getSelectedItem());
        facturaActual.setBodega((Bodega) cbBodega.getSelectedItem());
        int id = facturaServicio.registrarVenta(facturaActual);
        String total = UtilVista.q(facturaActual.calcularTotal());
        nuevaFactura();
        int r = JOptionPane.showConfirmDialog(this,
                "Factura #" + id + " registrada por " + total + ".\n"
                        + "El inventario de la bodega ya fue descontado.\n\n¿Desea registrar el pago ahora?",
                "Venta registrada", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
        if (r == JOptionPane.YES_OPTION && principal() != null) {
            principal().abrirPago(id);
        } else {
            notificar("Factura #" + id + " registrada (pendiente de pago)");
        }
    }

    private void nuevaFactura() {
        facturaActual = new Factura();
        txtCantidad.setText("1");
        refrescarDetalle();
    }

    private void refrescarDetalle() {
        modeloDetalle.setRowCount(0);
        for (DetalleFactura d : facturaActual.getDetalles()) {
            modeloDetalle.addRow(new Object[]{d.getProducto().getNombre(), d.getCantidad(),
                    d.getPrecioUnitario(), d.calcularSubtotal()});
        }
        lblTotal.setText(UtilVista.q(facturaActual.calcularTotal()));
    }

    private void cargarCombos() {
        UtilVista.recargarCombo(cbCliente, clienteDAO.listarTodos(), Cliente::getId);
        UtilVista.recargarCombo(cbEmpleado, empleadoDAO.listarTodos(), Empleado::getId);
        UtilVista.recargarCombo(cbBodega, bodegaDAO.listarActivas(), Bodega::getIdBodega);
        UtilVista.recargarCombo(cbProducto, productoDAO.listarTodos(), Producto::getIdProducto);
        Bodega b = (Bodega) cbBodega.getSelectedItem();
        idBodegaActual = b == null ? 0 : b.getIdBodega();
        mostrarDatosProducto();
    }

    // ================================================================== EMITIDAS
    private JPanel construirEmitidas() {
        UtilVista.columnasMoneda(tablaFacturas, 5, 6, 7);
        UtilVista.columnasMoneda(tablaVerDetalle, 2, 3);
        UtilVista.anchos(tablaFacturas, 50, 85, 170, 120, 120, 90, 90, 90, 90);
        tablaFacturas.getColumnModel().getColumn(8).setCellRenderer(new RenderEstado());

        JButton btnPagar = UtilVista.boton("Registrar pago", UtilVista.EXITO);
        JButton btnAnular = UtilVista.boton("Anular factura", UtilVista.PELIGRO);
        JPanel barra = new JPanel(new BorderLayout());
        barra.add(UtilVista.botones(FlowLayout.LEFT, new JLabel("Mostrar:"), cbFiltro), BorderLayout.WEST);
        barra.add(UtilVista.botones(FlowLayout.RIGHT, btnAnular, btnPagar), BorderLayout.EAST);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                UtilVista.scroll(tablaFacturas, "Facturas"),
                UtilVista.scroll(tablaVerDetalle, "Detalle de la factura seleccionada"));
        split.setResizeWeight(0.65);

        JPanel p = new JPanel(new BorderLayout());
        p.add(barra, BorderLayout.NORTH);
        p.add(split, BorderLayout.CENTER);

        cbFiltro.addActionListener(e -> ejecutar(this::cargarFacturas));
        tablaFacturas.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                ejecutar(this::verDetalle);
            }
        });
        btnPagar.addActionListener(e -> {
            int id = idFacturaSeleccionada();
            if (id > 0 && principal() != null) {
                principal().abrirPago(id);
            }
        });
        btnAnular.addActionListener(e -> ejecutar(this::anular));
        return p;
    }

    private int idFacturaSeleccionada() {
        int fila = UtilVista.filaModelo(tablaFacturas);
        if (fila < 0) {
            info("Seleccione una factura de la tabla.");
            return 0;
        }
        return (Integer) modeloFacturas.getValueAt(fila, 0);
    }

    private void anular() {
        int id = idFacturaSeleccionada();
        if (id > 0 && confirmar("¿Anular la factura #" + id + "?\n"
                + "La mercadería regresará a la bodega de donde salió.")) {
            facturaServicio.anular(id);
            info("Factura #" + id + " anulada. El inventario fue devuelto a la bodega.");
        }
    }

    private void verDetalle() {
        modeloVerDetalle.setRowCount(0);
        int fila = UtilVista.filaModelo(tablaFacturas);
        if (fila < 0) {
            return;
        }
        Factura f = facturaServicio.buscar((Integer) modeloFacturas.getValueAt(fila, 0));
        if (f != null) {
            for (DetalleFactura d : f.getDetalles()) {
                modeloVerDetalle.addRow(new Object[]{d.getProducto().getNombre(), d.getCantidad(),
                        d.getPrecioUnitario(), d.calcularSubtotal()});
            }
        }
    }

    private void cargarFacturas() {
        String filtro = (String) cbFiltro.getSelectedItem();
        EstadoFactura estado = "TODAS".equals(filtro) ? null : EstadoFactura.valueOf(filtro);
        modeloFacturas.setRowCount(0);
        modeloVerDetalle.setRowCount(0);
        for (FacturaResumen f : facturaServicio.listar(estado)) {
            modeloFacturas.addRow(new Object[]{f.idFactura(), f.fecha().format(UtilVista.FECHA), f.cliente(),
                    f.empleado(), f.bodega(), f.total(), f.pagado(),
                    f.estado() == EstadoFactura.ANULADA ? 0.0 : f.saldo(), f.estado().name()});
        }
    }

    /** Colorea la columna Estado. */
    static class RenderEstado extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foco, int f, int c) {
            super.getTableCellRendererComponent(t, v, sel, foco, f, c);
            setHorizontalAlignment(CENTER);
            setFont(getFont().deriveFont(Font.BOLD));
            if (!sel) {
                switch (String.valueOf(v)) {
                    case "PAGADA" -> setForeground(UtilVista.EXITO);
                    case "ANULADA" -> setForeground(UtilVista.PELIGRO);
                    default -> setForeground(UtilVista.ADVERTENCIA);
                }
            }
            return this;
        }
    }
}
