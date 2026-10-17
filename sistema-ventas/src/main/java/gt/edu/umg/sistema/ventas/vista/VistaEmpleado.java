package gt.edu.umg.sistema.ventas.vista;

import gt.edu.umg.sistema.ventas.dao.EmpleadoDAO;
import gt.edu.umg.sistema.ventas.dao.EmpleadoDAOImpl;
import gt.edu.umg.sistema.ventas.evento.BusEventos;
import gt.edu.umg.sistema.ventas.evento.EventoSistema;
import gt.edu.umg.sistema.ventas.modelo.Empleado;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class VistaEmpleado extends FormularioBase {

    private final EmpleadoDAO empleadoDAO = new EmpleadoDAOImpl();

    private final JTextField txtNombre = new JTextField(20);
    private final JTextField txtNit = new JTextField(10);
    private final JTextField txtTelefono = new JTextField(10);
    private final JTextField txtCodigo = new JTextField(10);
    private final JTextField txtPuesto = new JTextField(15);
    private final DefaultTableModel modelo = UtilVista.modelo("ID", "Nombre", "NIT", "Teléfono", "Código", "Puesto");
    private final JTable tabla = UtilVista.tabla(modelo);
    private int idSeleccionado = 0;

    public VistaEmpleado() {
        super("Empleados", 780, 520);
        construir();
        cargarTabla();
        escuchar(this::cargarTabla, EventoSistema.EMPLEADOS);
    }

    private void construir() {
        UtilVista.Formulario form = new UtilVista.Formulario()
                .fila("Nombre:", txtNombre, "NIT:", txtNit)
                .fila("Código:", txtCodigo, "Teléfono:", txtTelefono)
                .fila("Puesto:", txtPuesto);

        JButton btnNuevo = UtilVista.boton("Nuevo", null);
        JButton btnGuardar = UtilVista.boton("Guardar", UtilVista.PRIMARIO);
        JButton btnEliminar = UtilVista.boton("Eliminar", UtilVista.PELIGRO);

        JPanel norte = new JPanel(new BorderLayout());
        norte.setBorder(UtilVista.seccion("Datos del empleado"));
        norte.add(form, BorderLayout.CENTER);
        norte.add(UtilVista.botones(FlowLayout.RIGHT, btnNuevo, btnEliminar, btnGuardar), BorderLayout.SOUTH);

        UtilVista.anchos(tabla, 50, 200, 90, 90, 80, 140);
        add(norte, BorderLayout.NORTH);
        add(UtilVista.scroll(tabla, "Empleados registrados"), BorderLayout.CENTER);

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
        Empleado emp = new Empleado();
        emp.setId(idSeleccionado);
        emp.setNombre(UtilVista.requerido(txtNombre, "Nombre"));
        emp.setNit(txtNit.getText().trim());
        emp.setTelefono(txtTelefono.getText().trim());
        emp.setCodigoEmpleado(UtilVista.requerido(txtCodigo, "Código"));
        emp.setPuesto(txtPuesto.getText().trim());
        if (idSeleccionado == 0) {
            empleadoDAO.guardar(emp);
        } else {
            empleadoDAO.actualizar(emp);
        }
        limpiar();
        BusEventos.publicar(EventoSistema.EMPLEADOS);
        notificar("Empleado \"" + emp.getNombre() + "\" guardado");
    }

    private void eliminar() {
        if (idSeleccionado == 0) {
            info("Seleccione un empleado de la tabla.");
            return;
        }
        if (confirmar("¿Eliminar al empleado \"" + txtNombre.getText() + "\"?")) {
            empleadoDAO.eliminar(idSeleccionado);
            limpiar();
            BusEventos.publicar(EventoSistema.EMPLEADOS);
            notificar("Empleado eliminado");
        }
    }

    private void cargarSeleccion() {
        int fila = UtilVista.filaModelo(tabla);
        if (fila < 0) {
            return;
        }
        idSeleccionado = (int) modelo.getValueAt(fila, 0);
        txtNombre.setText((String) modelo.getValueAt(fila, 1));
        txtNit.setText((String) modelo.getValueAt(fila, 2));
        txtTelefono.setText((String) modelo.getValueAt(fila, 3));
        txtCodigo.setText((String) modelo.getValueAt(fila, 4));
        txtPuesto.setText((String) modelo.getValueAt(fila, 5));
    }

    private void limpiar() {
        idSeleccionado = 0;
        for (JTextField t : new JTextField[]{txtNombre, txtNit, txtTelefono, txtCodigo, txtPuesto}) {
            t.setText("");
        }
        tabla.clearSelection();
        txtNombre.requestFocus();
    }

    private void cargarTabla() {
        modelo.setRowCount(0);
        for (Empleado e : empleadoDAO.listarTodos()) {
            modelo.addRow(new Object[]{e.getId(), e.getNombre(), e.getNit(), e.getTelefono(),
                    e.getCodigoEmpleado(), e.getPuesto()});
        }
    }
}
