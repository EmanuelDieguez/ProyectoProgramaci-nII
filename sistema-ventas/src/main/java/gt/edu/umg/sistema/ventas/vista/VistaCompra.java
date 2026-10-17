package gt.edu.umg.sistema.ventas.vista;

import gt.edu.umg.sistema.ventas.dao.*;
import gt.edu.umg.sistema.ventas.evento.EventoSistema;
import gt.edu.umg.sistema.ventas.modelo.*;
import gt.edu.umg.sistema.ventas.servicio.CompraServicio;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/**
 * Compras a proveedores. Al registrar una compra:
 *   compra + detalle  ->  ENTRADA a la bodega  ->  sube existencia total  ->  Kardex
 * y se refrescan Productos, Bodegas, Kardex y Facturación si están abiertos.
 */
public class VistaCompra extends FormularioBase {

    private final CompraServicio compraServicio = new CompraServicio();
    private final BodegaDAO bodegaDAO = new BodegaDAOImpl();
    private final EmpleadoDAO empleadoDAO = new EmpleadoDAOImpl();
    private final ProductoDAO productoDAO = new ProductoDAOImpl();

    // nueva compra
    private final JTextField txtProveedor = new JTextField(22);
    private final JComboBox<Bodega> cbBodega = new JComboBox<>();
    private final JComboBox<Empleado> cbEmpleado = new JComboBox<>();
    private final JComboBox<Producto> cbProducto = new JComboBox<>();
    private final JTextField txtCantidad = new JTextField(6);
    private final JTextField txtCosto = new JTextField(8);
    private final DefaultTableModel modeloDetalle = UtilVista.modelo("Producto", "Cantidad", "Costo unit.", "Subtotal");
    private final JTable tablaDetalle = UtilVista.tabla(modeloDetalle);
    private final JLabel lblTotal = UtilVista.etiquetaTotal();
    private Compra compraActual = new Compra();

    // historial
    private final DefaultTableModel modeloHist = UtilVista.modelo("No.", "Fecha", "Proveedor", "Bodega", "Recibió", "Total");
    private final JTable tablaHist = UtilVista.tabla(modeloHist);
    private final DefaultTableModel modeloHistDet = UtilVista.modelo("Producto", "Cantidad", "Costo unit.", "Subtotal");
    private final JTable tablaHistDet = UtilVista.tabla(modeloHistDet);

    public VistaCompra() {
        super("Compras a proveedores", 920, 620);
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Nueva compra", construirNueva());
        tabs.addTab("Historial de compras", construirHistorial());
        add(tabs);
        cargarCombos();
        cargarHistorial();
        escuchar(this::cargarCombos, EventoSistema.BODEGAS, EventoSistema.EMPLEADOS, EventoSistema.PRODUCTOS);
        escuchar(this::cargarHistorial, EventoSistema.COMPRAS);
    }

    private JPanel construirNueva() {
        UtilVista.Formulario encabezado = new UtilVista.Formulario()
                .fila("Proveedor:", txtProveedor, "Bodega destino:", cbBodega)
                .fila("Recibe:", cbEmpleado);
        encabezado.setBorder(UtilVista.seccion("Encabezado"));

        JButton btnAgregar = UtilVista.boton("Agregar", UtilVista.PRIMARIO);
        UtilVista.Formulario linea = new UtilVista.Formulario()
                .fila("Producto:", cbProducto, "Cantidad:", txtCantidad, "Costo unit. (Q):", txtCosto);
        JPanel panelLinea = new JPanel(new BorderLayout());
        panelLinea.setBorder(UtilVista.seccion("Agregar producto"));
        panelLinea.add(linea, BorderLayout.CENTER);
        panelLinea.add(UtilVista.botones(FlowLayout.RIGHT, btnAgregar), BorderLayout.EAST);

        JPanel norte = new JPanel(new BorderLayout());
        norte.add(encabezado, BorderLayout.NORTH);
        norte.add(panelLinea, BorderLayout.SOUTH);

        UtilVista.columnasMoneda(tablaDetalle, 2, 3);
        UtilVista.anchos(tablaDetalle, 300, 80, 100, 110);

        JButton btnQuitar = UtilVista.boton("Quitar línea", UtilVista.PELIGRO);
        JButton btnLimpiar = UtilVista.boton("Limpiar", null);
        JButton btnRegistrar = UtilVista.boton("Registrar compra", UtilVista.EXITO);
        JPanel sur = new JPanel(new BorderLayout());
        JPanel total = UtilVista.botones(FlowLayout.LEFT, new JLabel("Total:"), lblTotal);
        sur.add(total, BorderLayout.WEST);
        sur.add(UtilVista.botones(FlowLayout.RIGHT, btnQuitar, btnLimpiar, btnRegistrar), BorderLayout.EAST);

        JPanel p = new JPanel(new BorderLayout());
        p.add(norte, BorderLayout.NORTH);
        p.add(UtilVista.scroll(tablaDetalle, "Detalle de la compra"), BorderLayout.CENTER);
        p.add(sur, BorderLayout.SOUTH);

        cbProducto.addActionListener(e -> sugerirCosto());
        btnAgregar.addActionListener(e -> ejecutar(this::agregar));
        txtCosto.addActionListener(e -> ejecutar(this::agregar));
        btnQuitar.addActionListener(e -> quitarLinea());
        btnLimpiar.addActionListener(e -> nuevaCompra());
        btnRegistrar.addActionListener(e -> ejecutar(this::registrar));
        return p;
    }

    private void sugerirCosto() {
        // Solo como ayuda: propone el precio actual; el usuario escribe el costo real
        if (txtCosto.getText().isBlank() && cbProducto.getSelectedItem() instanceof Producto p) {
            txtCosto.setText(String.valueOf(p.getPrecio()));
        }
    }

    private void agregar() {
        compraServicio.agregarDetalle(compraActual, (Producto) cbProducto.getSelectedItem(),
                UtilVista.entero(txtCantidad, "Cantidad"), UtilVista.decimal(txtCosto, "Costo"));
        txtCantidad.setText("");
        txtCosto.setText("");
        refrescarDetalle();
        cbProducto.requestFocus();
    }

    private void quitarLinea() {
        int fila = UtilVista.filaModelo(tablaDetalle);
        if (fila >= 0) {
            compraActual.getDetalles().remove(fila);
            refrescarDetalle();
        }
    }

    private void registrar() {
        compraActual.setProveedor(txtProveedor.getText().trim());
        compraActual.setBodega((Bodega) cbBodega.getSelectedItem());
        compraActual.setEmpleado((Empleado) cbEmpleado.getSelectedItem());
        if (!confirmar(String.format("¿Registrar la compra por %s?%nIngresará a: %s",
                UtilVista.q(compraActual.calcularTotal()), compraActual.getBodega()))) {
            return;
        }
        int id = compraServicio.registrarCompra(compraActual);
        info("Compra #" + id + " registrada.\nLas existencias de " + compraActual.getBodega() + " fueron actualizadas.");
        nuevaCompra();
    }

    private void nuevaCompra() {
        compraActual = new Compra();
        txtProveedor.setText("");
        txtCantidad.setText("");
        txtCosto.setText("");
        refrescarDetalle();
    }

    private void refrescarDetalle() {
        modeloDetalle.setRowCount(0);
        for (DetalleCompra d : compraActual.getDetalles()) {
            modeloDetalle.addRow(new Object[]{d.getProducto().getNombre(), d.getCantidad(),
                    d.getCostoUnitario(), d.calcularSubtotal()});
        }
        lblTotal.setText(UtilVista.q(compraActual.calcularTotal()));
    }

    private void cargarCombos() {
        UtilVista.recargarCombo(cbBodega, bodegaDAO.listarActivas(), Bodega::getIdBodega);
        UtilVista.recargarCombo(cbEmpleado, empleadoDAO.listarTodos(), Empleado::getId);
        UtilVista.recargarCombo(cbProducto, productoDAO.listarTodos(), Producto::getIdProducto);
    }

    // ------------------------------------------------------------------ historial
    private JPanel construirHistorial() {
        UtilVista.columnasMoneda(tablaHist, 5);
        UtilVista.columnasMoneda(tablaHistDet, 2, 3);
        UtilVista.anchos(tablaHist, 50, 120, 200, 130, 130, 100);
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                UtilVista.scroll(tablaHist, "Compras registradas"),
                UtilVista.scroll(tablaHistDet, "Detalle de la compra seleccionada"));
        split.setResizeWeight(0.6);
        tablaHist.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                ejecutar(this::cargarDetalleHistorial);
            }
        });
        JPanel p = new JPanel(new BorderLayout());
        p.add(split);
        return p;
    }

    private void cargarHistorial() {
        modeloHist.setRowCount(0);
        modeloHistDet.setRowCount(0);
        for (CompraResumen c : compraServicio.listar()) {
            modeloHist.addRow(new Object[]{c.idCompra(), c.fecha().format(UtilVista.FECHA_HORA),
                    c.proveedor(), c.bodega(), c.empleado(), c.total()});
        }
    }

    private void cargarDetalleHistorial() {
        modeloHistDet.setRowCount(0);
        int fila = UtilVista.filaModelo(tablaHist);
        if (fila < 0) {
            return;
        }
        for (DetalleCompra d : compraServicio.detalles((Integer) modeloHist.getValueAt(fila, 0))) {
            modeloHistDet.addRow(new Object[]{d.getProducto().getNombre(), d.getCantidad(),
                    d.getCostoUnitario(), d.calcularSubtotal()});
        }
    }
}
