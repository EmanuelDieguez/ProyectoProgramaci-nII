package gt.edu.umg.sistema.ventas.vista;

import gt.edu.umg.sistema.ventas.dao.BodegaDAO;
import gt.edu.umg.sistema.ventas.dao.BodegaDAOImpl;
import gt.edu.umg.sistema.ventas.dao.CategoriaDAO;
import gt.edu.umg.sistema.ventas.dao.CategoriaDAOImpl;
import gt.edu.umg.sistema.ventas.evento.EventoSistema;
import gt.edu.umg.sistema.ventas.modelo.*;
import gt.edu.umg.sistema.ventas.servicio.InventarioServicio;
import gt.edu.umg.sistema.ventas.servicio.ProductoServicio;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class VistaProducto extends FormularioBase {

    private final ProductoServicio productoServicio = new ProductoServicio();
    private final InventarioServicio inventarioServicio = new InventarioServicio();
    private final CategoriaDAO categoriaDAO = new CategoriaDAOImpl();
    private final BodegaDAO bodegaDAO = new BodegaDAOImpl();

    private final JTextField txtNombre = new JTextField(20);
    private final JTextField txtPrecio = new JTextField(8);
    private final JComboBox<Categoria> cbCategoria = new JComboBox<>();
    private final JComboBox<Bodega> cbBodegaInicial = new JComboBox<>();
    private final JTextField txtExistenciaInicial = new JTextField("0", 6);
    private final JLabel lblModo = new JLabel();

    private final DefaultTableModel modelo = UtilVista.modelo("ID", "Producto", "Categoría", "Precio", "Existencia total");
    private final JTable tabla = UtilVista.tabla(modelo);
    private final DefaultTableModel modeloBodegas = UtilVista.modelo("Bodega", "Cantidad");
    private final JTable tablaBodegas = UtilVista.tabla(modeloBodegas);

    private final Map<Integer, Producto> productos = new HashMap<>();
    private int idSeleccionado = 0;

    public VistaProducto() {
        super("Productos", 900, 600);
        construir();
        cargarCombos();
        cargarTabla();
        limpiar();
        escuchar(this::cargarTabla, EventoSistema.PRODUCTOS, EventoSistema.INVENTARIO);
        escuchar(this::cargarCombos, EventoSistema.CATEGORIAS, EventoSistema.BODEGAS);
    }

    private void construir() {
        lblModo.setFont(lblModo.getFont().deriveFont(Font.BOLD));
        UtilVista.Formulario form = new UtilVista.Formulario()
                .fila("Nombre:", txtNombre, "Precio venta (Q):", txtPrecio)
                .fila("Categoría:", cbCategoria, "", lblModo)
                .fila("Bodega inicial:", cbBodegaInicial, "Existencia inicial:", txtExistenciaInicial);

        JButton btnNuevo = UtilVista.boton("Nuevo", null);
        JButton btnGuardar = UtilVista.boton("Guardar", UtilVista.PRIMARIO);
        JButton btnEliminar = UtilVista.boton("Eliminar", UtilVista.PELIGRO);

        JLabel nota = new JLabel("  La existencia se modifica con Compras, Ventas y Traslados (queda registrada en el Kardex).");
        nota.setForeground(UtilVista.TEXTO_SUAVE);
        JPanel pie = new JPanel(new BorderLayout());
        pie.add(nota, BorderLayout.WEST);
        pie.add(UtilVista.botones(FlowLayout.RIGHT, btnNuevo, btnEliminar, btnGuardar), BorderLayout.EAST);

        JPanel norte = new JPanel(new BorderLayout());
        norte.setBorder(UtilVista.seccion("Datos del producto"));
        norte.add(form, BorderLayout.CENTER);
        norte.add(pie, BorderLayout.SOUTH);

        UtilVista.anchos(tabla, 50, 250, 140, 90, 110);
        UtilVista.columnasMoneda(tabla, 3);
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                UtilVista.scroll(tabla, "Productos"),
                UtilVista.scroll(tablaBodegas, "Existencia por bodega"));
        split.setResizeWeight(0.72);

        add(norte, BorderLayout.NORTH);
        add(split, BorderLayout.CENTER);

        btnNuevo.addActionListener(e -> limpiar());
        btnGuardar.addActionListener(e -> ejecutar(this::guardar));
        btnEliminar.addActionListener(e -> ejecutar(this::eliminar));
        tabla.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                ejecutar(this::cargarSeleccion);
            }
        });
    }

    private void guardar() {
        Producto p = new Producto();
        p.setIdProducto(idSeleccionado);
        p.setNombre(UtilVista.requerido(txtNombre, "Nombre"));
        p.setPrecio(UtilVista.decimal(txtPrecio, "Precio"));
        p.setCategoria((Categoria) cbCategoria.getSelectedItem());
        if (idSeleccionado == 0) {
            int inicial = txtExistenciaInicial.getText().isBlank() ? 0 : UtilVista.entero(txtExistenciaInicial, "Existencia inicial");
            productoServicio.crear(p, (Bodega) cbBodegaInicial.getSelectedItem(), inicial);
        } else {
            productoServicio.actualizar(p);
        }
        limpiar();
        notificar("Producto \"" + p.getNombre() + "\" guardado");
    }

    private void eliminar() {
        if (idSeleccionado == 0) {
            info("Seleccione un producto de la tabla.");
            return;
        }
        if (confirmar("¿Eliminar el producto \"" + txtNombre.getText() + "\"?\n"
                + "Solo es posible si nunca tuvo movimientos.")) {
            productoServicio.eliminar(idSeleccionado);
            limpiar();
            notificar("Producto eliminado");
        }
    }

    private void cargarSeleccion() {
        int fila = UtilVista.filaModelo(tabla);
        if (fila < 0) {
            return;
        }
        Producto p = productos.get((Integer) modelo.getValueAt(fila, 0));
        idSeleccionado = p.getIdProducto();
        txtNombre.setText(p.getNombre());
        txtPrecio.setText(String.valueOf(p.getPrecio()));
        if (p.getCategoria() != null) {
            for (int i = 0; i < cbCategoria.getItemCount(); i++) {
                if (cbCategoria.getItemAt(i).getIdCategoria() == p.getCategoria().getIdCategoria()) {
                    cbCategoria.setSelectedIndex(i);
                }
            }
        }
        modoEdicion(true);
        cargarExistencias(p.getIdProducto());
    }

    private void cargarExistencias(int idProducto) {
        modeloBodegas.setRowCount(0);
        for (Existencia e : inventarioServicio.existenciasPorProducto(idProducto)) {
            modeloBodegas.addRow(new Object[]{e.getBodega().getNombre(), e.getCantidad()});
        }
    }

    private void modoEdicion(boolean editando) {
        cbBodegaInicial.setEnabled(!editando);
        txtExistenciaInicial.setEnabled(!editando);
        lblModo.setText(editando ? "Editando producto #" + idSeleccionado : "Nuevo producto");
        lblModo.setForeground(editando ? UtilVista.ADVERTENCIA : UtilVista.EXITO);
    }

    private void limpiar() {
        idSeleccionado = 0;
        txtNombre.setText("");
        txtPrecio.setText("");
        txtExistenciaInicial.setText("0");
        tabla.clearSelection();
        modeloBodegas.setRowCount(0);
        modoEdicion(false);
        txtNombre.requestFocus();
    }

    private void cargarCombos() {
        UtilVista.recargarCombo(cbCategoria, categoriaDAO.listarTodos(), Categoria::getIdCategoria);
        UtilVista.recargarCombo(cbBodegaInicial, bodegaDAO.listarActivas(), Bodega::getIdBodega);
    }

    private void cargarTabla() {
        modelo.setRowCount(0);
        productos.clear();
        for (Producto p : productoServicio.listar()) {
            productos.put(p.getIdProducto(), p);
            modelo.addRow(new Object[]{p.getIdProducto(), p.getNombre(),
                    p.getCategoria() == null ? "" : p.getCategoria().getNombre(),
                    p.getPrecio(), p.getExistencia()});
        }
        if (idSeleccionado > 0) {
            cargarExistencias(idSeleccionado);
        }
    }
}
