package gt.edu.umg.sistema.ventas.vista;

import gt.edu.umg.sistema.ventas.dao.Conexion;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyVetoException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * FORMULARIO PADRE (MDI). Contiene el menú, la barra de herramientas, la barra de estado y un
 * JDesktopPane donde se abren los formularios hijos (JInternalFrame).
 * Cada formulario se abre una sola vez: si ya está abierto, se trae al frente.
 */
public class VentanaPrincipal extends JFrame {

    private final JDesktopPane escritorio = new Escritorio();
    private final Map<Class<?>, FormularioBase> abiertos = new HashMap<>();
    private final JLabel lblConexion = new JLabel("Verificando conexión...");
    private final JLabel lblMensaje = new JLabel(" ");
    private Timer temporizadorMensaje;

    public VentanaPrincipal() {
        super("Sistema de Ventas");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setSize(1280, 800);
        setMinimumSize(new Dimension(1000, 650));
        setLocationRelativeTo(null);
        setExtendedState(MAXIMIZED_BOTH);

        setJMenuBar(crearMenu());
        add(crearBarraHerramientas(), BorderLayout.NORTH);
        add(escritorio, BorderLayout.CENTER);
        add(crearBarraEstado(), BorderLayout.SOUTH);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                salir();
            }
        });
        verificarConexion();
    }

    // ------------------------------------------------------------------ apertura de hijos
    public <T extends FormularioBase> T abrir(Class<T> tipo, Supplier<T> fabrica) {
        FormularioBase existente = abiertos.get(tipo);
        if (existente != null && !existente.isClosed()) {
            enfocar(existente);
            return tipo.cast(existente);
        }
        T nuevo;
        try {
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            nuevo = fabrica.get();
        } catch (RuntimeException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "No se pudo abrir el formulario:\n" + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            return null;
        } finally {
            setCursor(Cursor.getDefaultCursor());
        }
        nuevo.addInternalFrameListener(new javax.swing.event.InternalFrameAdapter() {
            @Override
            public void internalFrameClosed(javax.swing.event.InternalFrameEvent e) {
                abiertos.remove(tipo);
            }
        });
        abiertos.put(tipo, nuevo);
        escritorio.add(nuevo);
        centrar(nuevo);
        nuevo.setVisible(true);
        enfocar(nuevo);
        return nuevo;
    }

    /** Usado por Facturación: después de vender abre Pagos con la factura seleccionada. */
    public void abrirPago(int idFactura) {
        VistaPago v = abrir(VistaPago.class, VistaPago::new);
        if (v != null) {
            v.seleccionarFactura(idFactura);
        }
    }

    private void centrar(JInternalFrame f) {
        Dimension d = escritorio.getSize();
        if (d.width == 0) {
            d = new Dimension(1200, 650);
        }
        int w = Math.min(f.getWidth(), d.width);
        int h = Math.min(f.getHeight(), d.height);
        f.setSize(w, h);
        int desfase = (abiertos.size() - 1) * 24 % 120;   // escalonar ventanas
        f.setLocation(Math.max(0, (d.width - w) / 2 + desfase - 40), Math.max(0, (d.height - h) / 2 + desfase - 40));
    }

    private void enfocar(JInternalFrame f) {
        try {
            if (f.isIcon()) {
                f.setIcon(false);
            }
            f.toFront();
            f.setSelected(true);
        } catch (PropertyVetoException ignorada) {
            // no ocurre en este proyecto
        }
    }

    // ------------------------------------------------------------------ menú y barras
    private JMenuBar crearMenu() {
        JMenuBar barra = new JMenuBar();

        JMenu archivo = new JMenu("Archivo");
        archivo.setMnemonic('A');
        archivo.add(item("Salir", null, e -> salir()));

        JMenu catalogos = new JMenu("Catálogos");
        catalogos.setMnemonic('C');
        catalogos.add(item("Clientes", null, e -> abrir(VistaCliente.class, VistaCliente::new)));
        catalogos.add(item("Empleados", null, e -> abrir(VistaEmpleado.class, VistaEmpleado::new)));
        catalogos.addSeparator();
        catalogos.add(item("Categorías", null, e -> abrir(VistaCategoria.class, VistaCategoria::new)));
        catalogos.add(item("Productos", KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0),
                e -> abrir(VistaProducto.class, VistaProducto::new)));

        JMenu inventario = new JMenu("Inventario");
        inventario.setMnemonic('I');
        inventario.add(item("Bodegas y traslados", KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0),
                e -> abrir(VistaBodega.class, VistaBodega::new)));
        inventario.add(item("Compras a proveedores", KeyStroke.getKeyStroke(KeyEvent.VK_F4, 0),
                e -> abrir(VistaCompra.class, VistaCompra::new)));
        inventario.add(item("Kardex (movimientos)", KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0),
                e -> abrir(VistaKardex.class, VistaKardex::new)));

        JMenu ventas = new JMenu("Ventas");
        ventas.setMnemonic('V');
        ventas.add(item("Facturación", KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0),
                e -> abrir(VistaFactura.class, VistaFactura::new)));
        ventas.add(item("Pagos", KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0),
                e -> abrir(VistaPago.class, VistaPago::new)));

        JMenu ventanas = new JMenu("Ventanas");
        ventanas.setMnemonic('n');
        ventanas.add(item("Cascada", null, e -> cascada()));
        ventanas.add(item("Cerrar todas", null, e -> cerrarTodas()));

        barra.add(archivo);
        barra.add(catalogos);
        barra.add(inventario);
        barra.add(ventas);
        barra.add(ventanas);
        return barra;
    }

    private JMenuItem item(String texto, KeyStroke atajo, java.awt.event.ActionListener accion) {
        JMenuItem i = new JMenuItem(texto);
        if (atajo != null) {
            i.setAccelerator(atajo);
        }
        i.addActionListener(accion);
        return i;
    }

    private JToolBar crearBarraHerramientas() {
        JToolBar tb = new JToolBar();
        tb.setFloatable(false);
        tb.setBorder(new EmptyBorder(4, 6, 4, 6));
        tb.add(herramienta("Facturar", "F", new Color(37, 99, 235), "Facturación (F2)",
                () -> abrir(VistaFactura.class, VistaFactura::new)));
        tb.add(herramienta("Pagos", "$", new Color(22, 163, 74), "Pagos (F3)",
                () -> abrir(VistaPago.class, VistaPago::new)));
        tb.addSeparator();
        tb.add(herramienta("Compras", "C", new Color(234, 88, 12), "Compras a proveedores (F4)",
                () -> abrir(VistaCompra.class, VistaCompra::new)));
        tb.add(herramienta("Bodegas", "B", new Color(124, 58, 237), "Bodegas y traslados (F6)",
                () -> abrir(VistaBodega.class, VistaBodega::new)));
        tb.add(herramienta("Kardex", "K", new Color(71, 85, 105), "Movimientos de inventario (F7)",
                () -> abrir(VistaKardex.class, VistaKardex::new)));
        tb.addSeparator();
        tb.add(herramienta("Productos", "P", new Color(13, 148, 136), "Productos (F5)",
                () -> abrir(VistaProducto.class, VistaProducto::new)));
        tb.add(herramienta("Clientes", "U", new Color(219, 39, 119), "Clientes",
                () -> abrir(VistaCliente.class, VistaCliente::new)));
        return tb;
    }

    private JButton herramienta(String texto, String letra, Color color, String ayuda, Runnable accion) {
        JButton b = new JButton(texto, new UtilVista.IconoLetra(letra, color));
        b.setToolTipText(ayuda);
        b.setFocusable(false);
        b.setHorizontalTextPosition(SwingConstants.RIGHT);
        b.addActionListener(e -> accion.run());
        return b;
    }

    private JPanel crearBarraEstado() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(203, 213, 225)),
                new EmptyBorder(4, 10, 4, 10)));
        JLabel fecha = new JLabel(LocalDate.now().format(UtilVista.FECHA));
        fecha.setForeground(UtilVista.TEXTO_SUAVE);
        p.add(lblConexion, BorderLayout.WEST);
        p.add(lblMensaje, BorderLayout.CENTER);
        p.add(fecha, BorderLayout.EAST);
        lblMensaje.setHorizontalAlignment(SwingConstants.CENTER);
        lblMensaje.setForeground(UtilVista.EXITO);
        return p;
    }

    /** Muestra un mensaje temporal en la barra de estado. */
    public void mensaje(String texto) {
        lblMensaje.setText(texto);
        if (temporizadorMensaje != null) {
            temporizadorMensaje.stop();
        }
        temporizadorMensaje = new Timer(6000, e -> lblMensaje.setText(" "));
        temporizadorMensaje.setRepeats(false);
        temporizadorMensaje.start();
    }

    private void verificarConexion() {
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                return Conexion.probar();
            }

            @Override
            protected void done() {
                try {
                    String error = get();
                    if (error == null) {
                        lblConexion.setText("● Conectado a la base de datos");
                        lblConexion.setForeground(UtilVista.EXITO);
                    } else {
                        lblConexion.setText("● Sin conexión a la base de datos");
                        lblConexion.setForeground(UtilVista.PELIGRO);
                        JOptionPane.showMessageDialog(VentanaPrincipal.this,
                                "No se pudo conectar a MySQL:\n" + error
                                        + "\n\nVerifique que el servicio esté iniciado y los datos de Conexion.java.",
                                "Conexión", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    lblConexion.setText("● Error al verificar la conexión");
                }
            }
        }.execute();
    }

    private void cascada() {
        int i = 0;
        for (JInternalFrame f : escritorio.getAllFrames()) {
            if (!f.isIcon()) {
                f.setLocation(i * 28, i * 28);
                f.toFront();
                i++;
            }
        }
    }

    private void cerrarTodas() {
        for (JInternalFrame f : escritorio.getAllFrames()) {
            f.dispose();
        }
        abiertos.clear();
    }

    private void salir() {
        if (JOptionPane.showConfirmDialog(this, "¿Desea salir del sistema?", "Salir",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            dispose();
            System.exit(0);
        }
    }

    /** Fondo del escritorio con degradado y el nombre del sistema. */
    private static class Escritorio extends JDesktopPane {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setPaint(new GradientPaint(0, 0, new Color(241, 245, 249), 0, getHeight(), new Color(203, 213, 225)));
            g2.fillRect(0, 0, getWidth(), getHeight());

            String titulo = "Sistema de Ventas";
            g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 44));
            g2.setColor(new Color(30, 64, 175, 60));
            FontMetrics fm = g2.getFontMetrics();
            int y = getHeight() / 2;
            g2.drawString(titulo, (getWidth() - fm.stringWidth(titulo)) / 2, y);

            String sub = "F2 Facturar  ·  F3 Pagos  ·  F4 Compras  ·  F5 Productos  ·  F6 Bodegas  ·  F7 Kardex";
            g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));
            g2.setColor(new Color(71, 85, 105, 140));
            fm = g2.getFontMetrics();
            g2.drawString(sub, (getWidth() - fm.stringWidth(sub)) / 2, y + 36);
            g2.dispose();
        }
    }
}
