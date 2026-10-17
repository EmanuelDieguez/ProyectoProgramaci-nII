package gt.edu.umg.sistema.ventas.vista;

import gt.edu.umg.sistema.ventas.dao.ClienteDAO;
import gt.edu.umg.sistema.ventas.dao.ClienteDAOImpl;
import gt.edu.umg.sistema.ventas.evento.BusEventos;
import gt.edu.umg.sistema.ventas.evento.EventoSistema;
import gt.edu.umg.sistema.ventas.modelo.Cliente;
import gt.edu.umg.sistema.ventas.modelo.TelefonoCliente;
import gt.edu.umg.sistema.ventas.servicio.ReglaNegocioException;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class VistaCliente extends FormularioBase {

    private final ClienteDAO clienteDAO = new ClienteDAOImpl();

    private final JTextField txtNombre = new JTextField(20);
    private final JTextField txtNit = new JTextField(10);
    private final JTextField txtDireccion = new JTextField(20);

    // --- Teléfonos
    private final JTextField txtTelefono = new JTextField(12);
    private final JComboBox<String> cbTipoTelefono = new JComboBox<>(new String[]{"Celular", "Casa", "Trabajo", "Otro"});
    private final DefaultListModel<TelefonoCliente> modeloTelefonos = new DefaultListModel<>();
    private final JList<TelefonoCliente> listaTelefonos = new JList<>(modeloTelefonos);

    private final DefaultTableModel modelo = UtilVista.modelo("ID", "Nombre", "NIT", "Teléfonos", "Dirección");
    private final JTable tabla = UtilVista.tabla(modelo);
    private final Map<Integer, Cliente> clientes = new HashMap<>();
    private int idSeleccionado = 0;

    public VistaCliente() {
        super("Clientes", 820, 600);
        construir();
        cargarTabla();
        escuchar(this::cargarTabla, EventoSistema.CLIENTES);
    }

    private void construir() {
        UtilVista.Formulario form = new UtilVista.Formulario()
                .fila("Nombre:", txtNombre, "NIT:", txtNit)
                .fila("Dirección:", txtDireccion);

        // Panel de teléfonos
        JButton btnAgregarTel = UtilVista.boton("Agregar teléfono", UtilVista.PRIMARIO);
        JButton btnQuitarTel = UtilVista.boton("Quitar teléfono", UtilVista.PELIGRO);
        listaTelefonos.setVisibleRowCount(4);
        listaTelefonos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JPanel panelTelefonos = new JPanel(new BorderLayout());
        panelTelefonos.setBorder(UtilVista.seccion("Teléfonos (el primero es el principal)"));
        panelTelefonos.add(UtilVista.botones(FlowLayout.LEFT, new JLabel("Número:"), txtTelefono,
                new JLabel("Tipo:"), cbTipoTelefono, btnAgregarTel, btnQuitarTel), BorderLayout.NORTH);
        panelTelefonos.add(new JScrollPane(listaTelefonos), BorderLayout.CENTER);

        JButton btnNuevo = UtilVista.boton("Nuevo", null);
        JButton btnGuardar = UtilVista.boton("Guardar", UtilVista.PRIMARIO);
        JButton btnEliminar = UtilVista.boton("Eliminar", UtilVista.PELIGRO);

        JPanel sur = new JPanel(new BorderLayout());
        sur.add(panelTelefonos, BorderLayout.CENTER);
        sur.add(UtilVista.botones(FlowLayout.RIGHT, btnNuevo, btnEliminar, btnGuardar), BorderLayout.SOUTH);

        JPanel norte = new JPanel(new BorderLayout());
        norte.setBorder(UtilVista.seccion("Datos del cliente"));
        norte.add(form, BorderLayout.CENTER);
        norte.add(sur, BorderLayout.SOUTH);

        UtilVista.anchos(tabla, 50, 190, 90, 200, 200);
        add(norte, BorderLayout.NORTH);
        add(UtilVista.scroll(tabla, "Clientes registrados"), BorderLayout.CENTER);

        btnAgregarTel.addActionListener(e -> ejecutar(this::agregarTelefono));
        txtTelefono.addActionListener(e -> ejecutar(this::agregarTelefono));   // Enter también agrega
        btnQuitarTel.addActionListener(e -> quitarTelefono());
        btnNuevo.addActionListener(e -> limpiar());
        btnGuardar.addActionListener(e -> ejecutar(this::guardar));
        btnEliminar.addActionListener(e -> ejecutar(this::eliminar));
        tabla.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                cargarSeleccion();
            }
        });
    }

    /** Agrega el número escrito a la lista (todavía no se guarda en la base). */
    private void agregarTelefono() {
        String numero = UtilVista.requerido(txtTelefono, "Teléfono");
        if (!numero.matches("[0-9+\\- ]{8,20}")) {
            txtTelefono.requestFocus();
            throw new ReglaNegocioException("El teléfono debe tener entre 8 y 20 caracteres: dígitos, espacios, guiones o +.");
        }
        for (int i = 0; i < modeloTelefonos.size(); i++) {
            if (modeloTelefonos.get(i).getNumero().equals(numero)) {
                throw new ReglaNegocioException("El número " + numero + " ya está en la lista.");
            }
        }
        modeloTelefonos.addElement(new TelefonoCliente(numero, (String) cbTipoTelefono.getSelectedItem()));
        txtTelefono.setText("");
        txtTelefono.requestFocus();
    }

    private void quitarTelefono() {
        int i = listaTelefonos.getSelectedIndex();
        if (i < 0) {
            info("Seleccione un teléfono de la lista.");
            return;
        }
        modeloTelefonos.remove(i);
    }

    private void guardar() {
        // Si escribió un número pero no presionó "Agregar teléfono", se agrega automáticamente
        if (!txtTelefono.getText().isBlank()) {
            agregarTelefono();
        }
        Cliente c = new Cliente();
        c.setId(idSeleccionado);
        c.setNombre(UtilVista.requerido(txtNombre, "Nombre"));
        String nit = txtNit.getText().trim();
        c.setNit(nit.isEmpty() ? "CF" : nit.toUpperCase());   // Consumidor Final
        c.setDireccion(txtDireccion.getText().trim().isEmpty() ? "Ciudad" : txtDireccion.getText().trim());
        for (int i = 0; i < modeloTelefonos.size(); i++) {
            c.agregarTelefono(modeloTelefonos.get(i));
        }
        if (idSeleccionado == 0) {
            clienteDAO.guardar(c);
        } else {
            clienteDAO.actualizar(c);
        }
        limpiar();
        BusEventos.publicar(EventoSistema.CLIENTES, EventoSistema.FACTURAS);
        notificar("Cliente \"" + c.getNombre() + "\" guardado con " + c.getTelefonos().size() + " teléfono(s)");
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
        Cliente c = clientes.get((Integer) modelo.getValueAt(fila, 0));
        idSeleccionado = c.getId();
        txtNombre.setText(c.getNombre());
        txtNit.setText(c.getNit());
        txtDireccion.setText(c.getDireccion());
        txtTelefono.setText("");
        modeloTelefonos.clear();
        for (TelefonoCliente t : c.getTelefonos()) {
            modeloTelefonos.addElement(t);
        }
    }

    private void limpiar() {
        idSeleccionado = 0;
        txtNombre.setText("");
        txtNit.setText("");
        txtDireccion.setText("");
        txtTelefono.setText("");
        modeloTelefonos.clear();
        tabla.clearSelection();
        txtNombre.requestFocus();
    }

    private void cargarTabla() {
        modelo.setRowCount(0);
        clientes.clear();
        for (Cliente c : clienteDAO.listarTodos()) {
            clientes.put(c.getId(), c);
            modelo.addRow(new Object[]{c.getId(), c.getNombre(), c.getNit(), c.getTelefonosTexto(), c.getDireccion()});
        }
    }
}