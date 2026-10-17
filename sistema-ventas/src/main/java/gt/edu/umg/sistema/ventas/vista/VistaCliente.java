package gt.edu.umg.sistema.ventas.vista;

import gt.edu.umg.sistema.ventas.dao.ClienteDAO;
import gt.edu.umg.sistema.ventas.dao.ClienteDAOImpl;
import gt.edu.umg.sistema.ventas.evento.BusEventos;
import gt.edu.umg.sistema.ventas.evento.EventoSistema;
import gt.edu.umg.sistema.ventas.modelo.Cliente;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class VistaCliente extends FormularioBase {

    private final ClienteDAO clienteDAO = new ClienteDAOImpl();

    private final JTextField txtNombre = new JTextField(20);
    private final JTextField txtNit = new JTextField(10);
    private final JTextField txtTelefono = new JTextField(10);
    private final JTextField txtDireccion = new JTextField(20);
    private final DefaultTableModel modelo = UtilVista.modelo("ID", "Nombre", "NIT", "Teléfono", "Dirección");
    private final JTable tabla = UtilVista.tabla(modelo);
    private int idSeleccionado = 0;

    public VistaCliente() {
        super("Clientes", 760, 500);
        construir();
        cargarTabla();
        escuchar(this::cargarTabla, EventoSistema.CLIENTES);
    }

    private void construir() {
        UtilVista.Formulario form = new UtilVista.Formulario()
                .fila("Nombre:", txtNombre, "NIT:", txtNit)
                .fila("Dirección:", txtDireccion, "Teléfono:", txtTelefono);

        JButton btnNuevo = UtilVista.boton("Nuevo", null);
        JButton btnGuardar = UtilVista.boton("Guardar", UtilVista.PRIMARIO);
        JButton btnEliminar = UtilVista.boton("Eliminar", UtilVista.PELIGRO);

        JPanel norte = new JPanel(new BorderLayout());
        norte.setBorder(UtilVista.seccion("Datos del cliente"));
        norte.add(form, BorderLayout.CENTER);
        norte.add(UtilVista.botones(FlowLayout.RIGHT, btnNuevo, btnEliminar, btnGuardar), BorderLayout.SOUTH);

        UtilVista.anchos(tabla, 50, 200, 90, 90, 220);
        add(norte, BorderLayout.NORTH);
        add(UtilVista.scroll(tabla, "Clientes registrados"), BorderLayout.CENTER);

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
        Cliente c = new Cliente();
        c.setId(idSeleccionado);
        c.setNombre(UtilVista.requerido(txtNombre, "Nombre"));
        String nit = txtNit.getText().trim();
        c.setNit(nit.isEmpty() ? "CF" : nit.toUpperCase());   // Consumidor Final
        c.setTelefono(txtTelefono.getText().trim());
        c.setDireccion(txtDireccion.getText().trim().isEmpty() ? "Ciudad" : txtDireccion.getText().trim());
        if (idSeleccionado == 0) {
            clienteDAO.guardar(c);
        } else {
            clienteDAO.actualizar(c);
        }
        limpiar();
        BusEventos.publicar(EventoSistema.CLIENTES, EventoSistema.FACTURAS);
        notificar("Cliente \"" + c.getNombre() + "\" guardado");
    }

    private void eliminar() {
        if (idSeleccionado == 0) {
            info("Seleccione un cliente de la tabla.");
            return;
        }
        if (confirmar("¿Eliminar al cliente \"" + txtNombre.getText() + "\"?")) {
            clienteDAO.eliminar(idSeleccionado);
            limpiar();
            BusEventos.publicar(EventoSistema.CLIENTES);
            notificar("Cliente eliminado");
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
        txtDireccion.setText((String) modelo.getValueAt(fila, 4));
    }

    private void limpiar() {
        idSeleccionado = 0;
        txtNombre.setText("");
        txtNit.setText("");
        txtTelefono.setText("");
        txtDireccion.setText("");
        tabla.clearSelection();
        txtNombre.requestFocus();
    }

    private void cargarTabla() {
        modelo.setRowCount(0);
        for (Cliente c : clienteDAO.listarTodos()) {
            modelo.addRow(new Object[]{c.getId(), c.getNombre(), c.getNit(), c.getTelefono(), c.getDireccion()});
        }
    }
}
