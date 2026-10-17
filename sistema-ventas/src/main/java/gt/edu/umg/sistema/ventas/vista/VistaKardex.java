package gt.edu.umg.sistema.ventas.vista;

import gt.edu.umg.sistema.ventas.dao.BodegaDAO;
import gt.edu.umg.sistema.ventas.dao.BodegaDAOImpl;
import gt.edu.umg.sistema.ventas.dao.ProductoDAO;
import gt.edu.umg.sistema.ventas.dao.ProductoDAOImpl;
import gt.edu.umg.sistema.ventas.evento.EventoSistema;
import gt.edu.umg.sistema.ventas.modelo.*;
import gt.edu.umg.sistema.ventas.servicio.InventarioServicio;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/** Kardex: historial de entradas y salidas por bodega y producto. */
public class VistaKardex extends FormularioBase {

    private static final Bodega TODAS = new Bodega(0, "(Todas las bodegas)", "", true);
    private static final Producto TODOS = new Producto(0, "(Todos los productos)", 0, 0, null);

    private final InventarioServicio inventarioServicio = new InventarioServicio();
    private final BodegaDAO bodegaDAO = new BodegaDAOImpl();
    private final ProductoDAO productoDAO = new ProductoDAOImpl();

    private final JComboBox<Bodega> cbBodega = new JComboBox<>();
    private final JComboBox<Producto> cbProducto = new JComboBox<>();
    private final DefaultTableModel modelo = UtilVista.modelo(
            "Fecha", "Bodega", "Producto", "Tipo", "Cantidad", "Saldo bodega", "Referencia");
    private final JTable tabla = UtilVista.tabla(modelo);
    private final JLabel lblResumen = new JLabel(" ");

    public VistaKardex() {
        super("Kardex - Movimientos de inventario", 980, 560);
        construir();
        cargarFiltros();
        consultar();
        escuchar(this::consultar, EventoSistema.INVENTARIO);
        escuchar(() -> { cargarFiltros(); consultar(); }, EventoSistema.BODEGAS, EventoSistema.PRODUCTOS);
    }

    private void construir() {
        JPanel filtros = UtilVista.botones(FlowLayout.LEFT,
                new JLabel("Bodega:"), cbBodega, new JLabel("   Producto:"), cbProducto);
        filtros.setBorder(UtilVista.seccion("Filtros"));

        UtilVista.anchos(tabla, 120, 130, 200, 80, 70, 90, 260);
        tabla.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
                super.getTableCellRendererComponent(t, v, s, f, r, c);
                setHorizontalAlignment(CENTER);
                setFont(getFont().deriveFont(Font.BOLD));
                if (!s) {
                    setForeground("ENTRADA".equals(v) ? UtilVista.EXITO : UtilVista.PELIGRO);
                }
                return this;
            }
        });

        lblResumen.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        add(filtros, BorderLayout.NORTH);
        add(UtilVista.scroll(tabla, null), BorderLayout.CENTER);
        add(lblResumen, BorderLayout.SOUTH);

        cbBodega.addActionListener(e -> ejecutar(this::consultar));
        cbProducto.addActionListener(e -> ejecutar(this::consultar));
    }

    private void cargarFiltros() {
        List<Bodega> bodegas = new ArrayList<>();
        bodegas.add(TODAS);
        bodegas.addAll(bodegaDAO.listarTodos());
        List<Producto> productos = new ArrayList<>();
        productos.add(TODOS);
        productos.addAll(productoDAO.listarTodos());
        UtilVista.recargarCombo(cbBodega, bodegas, Bodega::getIdBodega);
        UtilVista.recargarCombo(cbProducto, productos, Producto::getIdProducto);
    }

    private void consultar() {
        Bodega b = (Bodega) cbBodega.getSelectedItem();
        Producto p = (Producto) cbProducto.getSelectedItem();
        modelo.setRowCount(0);
        int entradas = 0, salidas = 0;
        for (MovimientoInventario m : inventarioServicio.movimientos(
                b == null ? 0 : b.getIdBodega(), p == null ? 0 : p.getIdProducto())) {
            modelo.addRow(new Object[]{m.getFecha().format(UtilVista.FECHA_HORA), m.getBodega().getNombre(),
                    m.getProducto().getNombre(), m.getTipo().name(), m.getCantidad(), m.getSaldo(), m.getReferencia()});
            if (m.getTipo() == TipoMovimiento.ENTRADA) {
                entradas += m.getCantidad();
            } else {
                salidas += m.getCantidad();
            }
        }
        lblResumen.setText(modelo.getRowCount() + " movimiento(s)   ·   Entradas: " + entradas
                + " und.   ·   Salidas: " + salidas + " und.");
    }
}
