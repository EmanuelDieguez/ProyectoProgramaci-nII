
package gt.edu.umg.sistema.ventas.modelo;

import java.util.ArrayList;
import java.util.List;

public class Cliente extends Persona {
    private String direccion;
        private List<TelefonoCliente> telefonos = new ArrayList<>();

    public Cliente() { super(); }

    public Cliente(int id, String nombre, String nit, String telefono, String direccion) {
        super(id, nombre, nit, telefono);
        this.direccion = direccion;
    }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
        public List<TelefonoCliente> getTelefonos() { return telefonos; }
    public void setTelefonos(List<TelefonoCliente> telefonos) { this.telefonos = telefonos; }

    public void agregarTelefono(TelefonoCliente telefono) {
        telefonos.add(telefono);
    }

    /** El primer número de la lista; se guarda también en persona.telefono. */
    public String getTelefonoPrincipal() {
        return telefonos.isEmpty() ? "" : telefonos.get(0).getNumero();
    }

    /** Todos los números separados por coma, para mostrarlos en la tabla. */
    public String getTelefonosTexto() {
        StringBuilder sb = new StringBuilder();
        for (TelefonoCliente t : telefonos) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(t.getNumero());
        }
        return sb.toString();
    }

    @Override
    public String mostrarInformacion() {
        return "Cliente: " + nombre + " | NIT: " + nit + " | Dir: " + direccion;
    }

    @Override
    public String toString() { return nombre + " (" + nit + ")"; }
}
