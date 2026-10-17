package gt.edu.umg.sistema.ventas.vista;

import gt.edu.umg.sistema.ventas.dao.CategoriaDAO;
import gt.edu.umg.sistema.ventas.dao.CategoriaDAOImpl;
import gt.edu.umg.sistema.ventas.evento.BusEventos;
import gt.edu.umg.sistema.ventas.evento.EventoSistema;
import gt.edu.umg.sistema.ventas.modelo.Categoria;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class VistaCategoria extends FormularioBase {

    private final CategoriaDAO categoriaDAO = new CategoriaDAOImpl();

    private final JTextField txtNombre = new JTextField(20);
    private final JTextField txtDescripcion = new JTextField(30);
    private final DefaultTableModel modelo = UtilVista.modelo("ID", "Nombre", "Descripción");
    private final JTable tabla = UtilVista.tabla(modelo);
    private int idSeleccionado = 0;

    public VistaCategoria() {
        super("Categorías", 640, 460);
        construir();
        cargarTabla();
        escuchar(this::cargarTabla, EventoSistema.CATEGORIAS);
    }

    private void construir() {
        UtilVista.Formulario form = new UtilVista.Formulario()
                .fila("Nombre:", txtNombre)
                .fila("Descripción:", txtDescripcion);

        JButton btnNuevo = UtilVista.boton("Nuevo", null);
        JButton btnGuardar = UtilVista.boton("Guardar", UtilVista.PRIMARIO);
        JButton btnEliminar = UtilVista.boton("Eliminar", UtilVista.PELIGRO);

        JPanel norte = new JPanel(new BorderLayout());
        norte.setBorder(UtilVista.seccion("Datos de la categoría"));
        norte.add(form, BorderLayout.CENTER);
        norte.add(UtilVista.botones(FlowLayout.RIGHT, btnNuevo, btnEliminar, btnGuardar), BorderLayout.SOUTH);

        UtilVista.anchos(tabla, 50, 180, 300);
        add(norte, BorderLayout.NORTH);
        add(UtilVista.scroll(tabla, "Categorías registradas"), BorderLayout.CENTER);

        btnNuevo.addActionListener(e -> limpiar());
        btnGuardar.addActionListener(e -> ejecutar(this::guardar));
        btnEliminar.addActionListener(e -> ejecutar(this::eliminar));
        tabla.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                cargarSeleccion();
            }
        });
    }

    private void guardar() {
        Categoria c = new Categoria(idSeleccionado,
                UtilVista.requerido(txtNombre, "Nombre"), txtDescripcion.getText().trim());
        if (idSeleccionado == 0) {
            categoriaDAO.guardar(c);
        } else {
            categoriaDAO.actualizar(c);
        }
        limpiar();
        BusEventos.publicar(EventoSistema.CATEGORIAS, EventoSistema.PRODUCTOS);
        notificar("Categoría \"" + c.getNombre() + "\" guardada");
    }

    private void eliminar() {
        if (idSeleccionado == 0) {
            info("Seleccione una categoría de la tabla.");
            return;
        }
        if (confirmar("¿Eliminar la categoría \"" + txtNombre.getText() + "\"?")) {
            categoriaDAO.eliminar(idSeleccionado);
            limpiar();
            BusEventos.publicar(EventoSistema.CATEGORIAS, EventoSistema.PRODUCTOS);
            notificar("Categoría eliminada");
        }
    }

    private void cargarSeleccion() {
        int fila = UtilVista.filaModelo(tabla);
        if (fila < 0) {
            return;
        }
        idSeleccionado = (int) modelo.getValueAt(fila, 0);
        txtNombre.setText((String) modelo.getValueAt(fila, 1));
        txtDescripcion.setText((String) modelo.getValueAt(fila, 2));
    }

    private void limpiar() {
        idSeleccionado = 0;
        txtNombre.setText("");
        txtDescripcion.setText("");
        tabla.clearSelection();
        txtNombre.requestFocus();
    }

    private void cargarTabla() {
        modelo.setRowCount(0);
        for (Categoria c : categoriaDAO.listarTodos()) {
            modelo.addRow(new Object[]{c.getIdCategoria(), c.getNombre(), c.getDescripcion()});
        }
    }
}
