package gt.edu.umg.sistema.ventas.vista;

import gt.edu.umg.sistema.ventas.servicio.ReglaNegocioException;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.function.ToIntFunction;

/** Utilidades de interfaz compartidas por todas las ventanas. */
public final class UtilVista {

    public static final Color PRIMARIO = new Color(30, 64, 175);
    public static final Color EXITO = new Color(21, 128, 61);
    public static final Color PELIGRO = new Color(185, 28, 28);
    public static final Color ADVERTENCIA = new Color(180, 83, 9);
    public static final Color TEXTO_SUAVE = new Color(100, 116, 139);

    public static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    public static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private UtilVista() {}

    public static String q(double valor) {
        return String.format(Locale.US, "Q %,.2f", valor);
    }

    // ------------------------------------------------------------ tablas
    /** Modelo de tabla de solo lectura que ordena números como números. */
    public static DefaultTableModel modelo(String... columnas) {
        return new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int columna) {
                for (int fila = 0; fila < getRowCount(); fila++) {
                    Object v = getValueAt(fila, columna);
                    if (v != null) {
                        return v.getClass();
                    }
                }
                return Object.class;
            }
        };
    }

    public static JTable tabla(DefaultTableModel modelo) {
        JTable t = new JTable(modelo);
        t.setRowHeight(26);
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.setAutoCreateRowSorter(true);
        t.setFillsViewportHeight(true);
        t.getTableHeader().setReorderingAllowed(false);
        t.getTableHeader().setFont(t.getTableHeader().getFont().deriveFont(Font.BOLD));
        return t;
    }

    /** Muestra las columnas indicadas como moneda (Q) alineadas a la derecha. */
    public static void columnasMoneda(JTable tabla, int... columnas) {
        DefaultTableCellRenderer r = new DefaultTableCellRenderer() {
            @Override
            protected void setValue(Object value) {
                setText(value instanceof Number n ? q(n.doubleValue()) : (value == null ? "" : value.toString()));
            }
        };
        r.setHorizontalAlignment(SwingConstants.RIGHT);
        for (int c : columnas) {
            tabla.getColumnModel().getColumn(c).setCellRenderer(r);
        }
    }

    public static void anchos(JTable tabla, int... anchos) {
        for (int i = 0; i < anchos.length && i < tabla.getColumnCount(); i++) {
            if (anchos[i] > 0) {
                tabla.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
                if (anchos[i] <= 70) {
                    tabla.getColumnModel().getColumn(i).setMaxWidth(anchos[i] + 30);
                }
            }
        }
    }

    /** Fila seleccionada convertida al índice del modelo (respeta el ordenamiento). -1 si no hay. */
    public static int filaModelo(JTable tabla) {
        int fila = tabla.getSelectedRow();
        return fila < 0 ? -1 : tabla.convertRowIndexToModel(fila);
    }

    // ------------------------------------------------------------ combos
    /**
     * Recarga un combo conservando el elemento seleccionado (comparando por id).
     * No dispara eventos de selección, así que después se debe refrescar lo que dependa del combo.
     */
    public static <T> void recargarCombo(JComboBox<T> combo, List<T> items, ToIntFunction<T> id) {
        @SuppressWarnings("unchecked")
        T anterior = (T) combo.getSelectedItem();
        DefaultComboBoxModel<T> m = new DefaultComboBoxModel<>();
        for (T item : items) {
            m.addElement(item);
        }
        if (anterior != null) {
            int idAnterior = id.applyAsInt(anterior);
            for (T item : items) {
                if (id.applyAsInt(item) == idAnterior) {
                    m.setSelectedItem(item);
                }
            }
        }
        combo.setModel(m);
    }

    // ------------------------------------------------------------ lectura de campos
    public static String requerido(JTextField campo, String nombre) {
        String v = campo.getText().trim();
        if (v.isEmpty()) {
            campo.requestFocus();
            throw new ReglaNegocioException("El campo \"" + nombre + "\" es obligatorio.");
        }
        return v;
    }

    public static int entero(JTextField campo, String nombre) {
        try {
            return Integer.parseInt(campo.getText().trim());
        } catch (NumberFormatException e) {
            campo.requestFocus();
            throw new ReglaNegocioException("\"" + nombre + "\" debe ser un número entero.");
        }
    }

    public static double decimal(JTextField campo, String nombre) {
        try {
            return Double.parseDouble(campo.getText().trim().replace(",", ""));
        } catch (NumberFormatException e) {
            campo.requestFocus();
            throw new ReglaNegocioException("\"" + nombre + "\" debe ser un número válido.");
        }
    }

    // ------------------------------------------------------------ componentes
    public static JButton boton(String texto, Color color) {
        JButton b = new JButton(texto);
        if (color != null) {
            b.setForeground(color);
            b.setFont(b.getFont().deriveFont(Font.BOLD));
        }
        b.setFocusPainted(false);
        return b;
    }

    public static JPanel botones(int alineacion, JComponent... componentes) {
        JPanel p = new JPanel(new FlowLayout(alineacion, 8, 6));
        for (JComponent c : componentes) {
            p.add(c);
        }
        return p;
    }

    public static JLabel etiquetaTotal() {
        JLabel l = new JLabel(q(0));
        l.setFont(l.getFont().deriveFont(Font.BOLD, 20f));
        l.setForeground(PRIMARIO);
        return l;
    }

    public static JLabel etiquetaDato() {
        JLabel l = new JLabel("-");
        l.setFont(l.getFont().deriveFont(Font.BOLD));
        return l;
    }

    public static TitledBorder seccion(String titulo) {
        TitledBorder b = BorderFactory.createTitledBorder(titulo);
        b.setTitleFont(new JLabel().getFont().deriveFont(Font.BOLD));
        b.setTitleColor(PRIMARIO);
        return b;
    }

    public static JScrollPane scroll(JComponent c, String titulo) {
        JScrollPane s = new JScrollPane(c);
        if (titulo != null) {
            s.setBorder(BorderFactory.createCompoundBorder(seccion(titulo), new EmptyBorder(2, 2, 2, 2)));
        }
        return s;
    }

    /**
     * Panel de formulario con GridBagLayout. Uso:
     *   new Formulario().fila("Nombre:", txtNombre, "NIT:", txtNit)
     */
    public static class Formulario extends JPanel {
        private int fila = 0;

        public Formulario() {
            super(new GridBagLayout());
            setBorder(new EmptyBorder(8, 8, 4, 8));
        }

        /** Recibe pares etiqueta/componente en la misma fila. */
        public Formulario fila(Object... pares) {
            for (int i = 0; i < pares.length; i += 2) {
                GridBagConstraints g = new GridBagConstraints();
                g.gridy = fila;
                g.insets = new Insets(4, i == 0 ? 0 : 16, 4, 6);
                g.anchor = GridBagConstraints.LINE_END;
                g.gridx = i;
                add(new JLabel(String.valueOf(pares[i])), g);

                g = new GridBagConstraints();
                g.gridy = fila;
                g.gridx = i + 1;
                g.insets = new Insets(4, 0, 4, 0);
                g.fill = GridBagConstraints.HORIZONTAL;
                g.weightx = 1;
                add((Component) pares[i + 1], g);
            }
            fila++;
            return this;
        }
    }

    /** Ícono cuadrado con una letra, para la barra de herramientas (no requiere imágenes). */
    public static class IconoLetra implements Icon {
        private final String letra;
        private final Color color;

        public IconoLetra(String letra, Color color) {
            this.letra = letra;
            this.color = color;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(color);
            g2.fillRoundRect(x, y, getIconWidth(), getIconHeight(), 10, 10);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(letra, x + (getIconWidth() - fm.stringWidth(letra)) / 2,
                    y + (getIconHeight() + fm.getAscent() - fm.getDescent()) / 2);
            g2.dispose();
        }

        @Override
        public int getIconWidth() { return 26; }

        @Override
        public int getIconHeight() { return 26; }
    }
}
