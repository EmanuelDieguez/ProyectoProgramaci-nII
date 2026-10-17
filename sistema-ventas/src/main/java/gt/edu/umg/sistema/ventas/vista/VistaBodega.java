package gt.edu.umg.sistema.ventas.vista;

import gt.edu.umg.sistema.ventas.dao.BodegaDAO;
import gt.edu.umg.sistema.ventas.dao.BodegaDAOImpl;
import gt.edu.umg.sistema.ventas.dao.ProductoDAO;
import gt.edu.umg.sistema.ventas.dao.ProductoDAOImpl;
import gt.edu.umg.sistema.ventas.evento.BusEventos;
import gt.edu.umg.sistema.ventas.evento.EventoSistema;
import gt.edu.umg.sistema.ventas.modelo.Bodega;
import gt.edu.umg.sistema.ventas.modelo.Existencia;
import gt.edu.umg.sistema.ventas.modelo.Producto;
import gt.edu.umg.sistema.ventas.servicio.InventarioServicio;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/** Mantenimiento de bodegas, existencias por bodega y traslados entre bodegas. */
public class VistaBodega extends FormularioBase {

    private final BodegaDAO bodegaDAO = new BodegaDAOImpl();
    private final ProductoDAO productoDAO = new ProductoDAOImpl();
    private final InventarioServicio inventarioServicio = new InventarioServicio();

    // --- pestaña Bodegas
    private final JTextField txtNombre = new JTextField(18);
    private final JTextField txtUbicacion = new JTextField(22);
    private final JCheckBox chkActiva = new JCheckBox("Activa", true);
    private final DefaultTableModel modelo = UtilVista.modelo("ID", "Bodega", "Ubicación", "Estado");
    private final JTable tabla = UtilVista.tabla(modelo);
    private final DefaultTableModel modeloExistencias = UtilVista.modelo("ID", "Producto", "Cantidad");
    private final JTable tablaExistencias = UtilVista.tabla(modeloExistencias);
    private final JLabel lblExistencias = new JLabel(" ");
    private final Map<Integer, Bodega> bodegas = new HashMap<>();
    private int idSeleccionado = 0;

    // --- pestaña Traslados
    private final JComboBox<Bodega> cbOrigen = new JComboBox<>();
    private final JComboBox<Bodega> cbDestino = new JComboBox<>();
    private final JComboBox<Producto> cbProducto = new JComboBox<>();
    private final JTextField txtCantidad = new JTextField(6);
    private final JLabel lblDisponible = UtilVista.etiquetaDato();

    public VistaBodega() {
        super("Bodegas e inventario", 900, 620);
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Bodegas y existencias", construirBodegas());
        tabs.addTab("Traslados entre bodegas", construirTraslados());
        add(tabs);

        cargarBodegas();
        cargarCombos();
        limpiar();
        escuchar(() -> { cargarBodegas(); cargarCombos(); }, EventoSistema.BODEGAS);
        escuchar(() -> { cargarExistencias(); cargarCombos(); }, EventoSistema.INVENTARIO, EventoSistema.PRODUCTOS);
    }

    // ================================================================== BODEGAS
    private JPanel construirBodegas() {
        UtilVista.Formulario form = new UtilVista.Formulario()
                .fila("Nombre:", txtNombre, "Ubicación:", txtUbicacion)
                .fila("", chkActiva);

        JButton btnNuevo = UtilVista.boton("Nueva", null);
        JButton btnGuardar = UtilVista.boton("Guardar", UtilVista.PRIMARIO);
        JButton btnEliminar = UtilVista.boton("Eliminar", UtilVista.PELIGRO);

        JPanel norte = new JPanel(new BorderLayout());
        norte.setBorder(UtilVista.seccion("Datos de la bodega"));
        norte.add(form, BorderLayout.CENTER);
        norte.add(UtilVista.botones(FlowLayout.RIGHT, btnNuevo, btnEliminar, btnGuardar), BorderLayout.SOUTH);

        UtilVista.anchos(tabla, 45, 160, 200, 70);
        UtilVista.anchos(tablaExistencias, 45, 220, 80);
        lblExistencias.setFont(lblExistencias.getFont().deriveFont(Font.BOLD));
        JPanel panelExist = new JPanel(new BorderLayout());
        panelExist.add(UtilVista.scroll(tablaExistencias, "Existencias de la bodega seleccionada"), BorderLayout.CENTER);
        panelExist.add(lblExistencias, BorderLayout.SOUTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, UtilVista.scroll(tabla, "Bodegas"), panelExist);
        split.setResizeWeight(0.5);

        JPanel p = new JPanel(new BorderLayout());
        p.add(norte, BorderLayout.NORTH);
        p.add(split, BorderLayout.CENTER);

        btnNuevo.addActionListener(e -> limpiar());
        btnGuardar.addActionListener(e -> ejecutar(this::guardar));
        btnEliminar.addActionListener(e -> ejecutar(this::eliminar));
        tabla.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                ejecutar(this::cargarSeleccion);
            }
        });
        return p;
    }

    private void guardar() {
        Bodega b = new Bodega(idSeleccionado, UtilVista.requerido(txtNombre, "Nombre"),
                txtUbicacion.getText().trim(), chkActiva.isSelected());
        if (idSeleccionado == 0) {
            bodegaDAO.guardar(b);
        } else {
            bodegaDAO.actualizar(b);
        }
        limpiar();
        BusEventos.publicar(EventoSistema.BODEGAS);
        notificar("Bodega \"" + b.getNombre() + "\" guardada");
    }

    private void eliminar() {
        if (idSeleccionado == 0) {
            info("Seleccione una bodega de la tabla.");
            return;
        }
        if (confirmar("¿Eliminar la bodega \"" + txtNombre.getText() + "\"?\n"
                + "Si tiene movimientos no se podrá eliminar; en ese caso desactívela.")) {
            bodegaDAO.eliminar(idSeleccionado);
            limpiar();
            BusEventos.publicar(EventoSistema.BODEGAS);
            notificar("Bodega eliminada");
        }
    }

    private void cargarSeleccion() {
        int fila = UtilVista.filaModelo(tabla);
        if (fila < 0) {
            return;
        }
        Bodega b = bodegas.get((Integer) modelo.getValueAt(fila, 0));
        idSeleccionado = b.getIdBodega();
        txtNombre.setText(b.getNombre());
        txtUbicacion.setText(b.getUbicacion());
        chkActiva.setSelected(b.isActiva());
        cargarExistencias();
    }

    private void limpiar() {
        idSeleccionado = 0;
        txtNombre.setText("");
        txtUbicacion.setText("");
        chkActiva.setSelected(true);
        tabla.clearSelection();
        modeloExistencias.setRowCount(0);
        lblExistencias.setText(" ");
    }

    private void cargarBodegas() {
        modelo.setRowCount(0);
        bodegas.clear();
        for (Bodega b : bodegaDAO.listarTodos()) {
            bodegas.put(b.getIdBodega(), b);
            modelo.addRow(new Object[]{b.getIdBodega(), b.getNombre(), b.getUbicacion(),
                    b.isActiva() ? "Activa" : "Inactiva"});
        }
    }

    private void cargarExistencias() {
        modeloExistencias.setRowCount(0);
        if (idSeleccionado == 0) {
            return;
        }
        int unidades = 0;
        for (Existencia e : inventarioServicio.existenciasPorBodega(idSeleccionado)) {
            modeloExistencias.addRow(new Object[]{e.getProducto().getIdProducto(),
                    e.getProducto().getNombre(), e.getCantidad()});
            unidades += e.getCantidad();
        }
        lblExistencias.setText("  " + modeloExistencias.getRowCount() + " producto(s) · " + unidades + " unidad(es)");
    }

    // ================================================================== TRASLADOS
    private JPanel construirTraslados() {
        UtilVista.Formulario form = new UtilVista.Formulario()
                .fila("Bodega origen:", cbOrigen)
                .fila("Bodega destino:", cbDestino)
                .fila("Producto:", cbProducto)
                .fila("Disponible en origen:", lblDisponible)
                .fila("Cantidad a trasladar:", txtCantidad);

        JButton btnTrasladar = UtilVista.boton("Realizar traslado", UtilVista.PRIMARIO);
        JPanel caja = new JPanel(new BorderLayout());
        caja.setBorder(UtilVista.seccion("Traslado de mercadería"));
        caja.add(form, BorderLayout.CENTER);
        caja.add(UtilVista.botones(FlowLayout.RIGHT, btnTrasladar), BorderLayout.SOUTH);

        JLabel ayuda = new JLabel("<html><div style='width:420px;color:#475569'>"
                + "El traslado genera una <b>SALIDA</b> en la bodega de origen y una <b>ENTRADA</b> en la de destino "
                + "dentro de la misma transacción. La existencia total del producto no cambia. "
                + "Ambos movimientos quedan registrados en el Kardex.</div></html>");

        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;
        g.gridy = 0;
        g.fill = GridBagConstraints.HORIZONTAL;
        p.add(caja, g);
        g.gridy = 1;
        g.insets = new Insets(12, 4, 0, 4);
        p.add(ayuda, g);

        cbOrigen.addActionListener(e -> ejecutar(this::actualizarDisponible));
        cbProducto.addActionListener(e -> ejecutar(this::actualizarDisponible));
        btnTrasladar.addActionListener(e -> ejecutar(this::trasladar));
        return p;
    }

    private void trasladar() {
        Bodega origen = (Bodega) cbOrigen.getSelectedItem();
        Bodega destino = (Bodega) cbDestino.getSelectedItem();
        Producto producto = (Producto) cbProducto.getSelectedItem();
        int cantidad = UtilVista.entero(txtCantidad, "Cantidad");
        inventarioServicio.trasladar(origen, destino, producto, cantidad);
        txtCantidad.setText("");
        info("Traslado realizado: " + cantidad + " unidad(es) de \"" + producto + "\"\n"
                + origen + "  →  " + destino);
    }

    private void actualizarDisponible() {
        int disp = inventarioServicio.existencia((Bodega) cbOrigen.getSelectedItem(),
                (Producto) cbProducto.getSelectedItem());
        lblDisponible.setText(disp + " unidad(es)");
        lblDisponible.setForeground(disp > 0 ? UtilVista.EXITO : UtilVista.PELIGRO);
    }

    private void cargarCombos() {
        UtilVista.recargarCombo(cbOrigen, bodegaDAO.listarActivas(), Bodega::getIdBodega);
        UtilVista.recargarCombo(cbDestino, bodegaDAO.listarActivas(), Bodega::getIdBodega);
        UtilVista.recargarCombo(cbProducto, productoDAO.listarTodos(), Producto::getIdProducto);
        if (cbDestino.getItemCount() > 1 && cbDestino.getSelectedIndex() == cbOrigen.getSelectedIndex()) {
            cbDestino.setSelectedIndex(cbOrigen.getSelectedIndex() == 0 ? 1 : 0);
        }
        actualizarDisponible();
    }
}
