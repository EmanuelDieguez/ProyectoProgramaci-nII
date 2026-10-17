package gt.edu.umg.sistema.ventas.modelo;

public class Cliente extends Persona {
    private String direccion;

    public Cliente() { super(); }

    public Cliente(int id, String nombre, String nit, String telefono, String direccion) {
        super(id, nombre, nit, telefono);
        this.direccion = direccion;
    }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    @Override
    public String mostrarInformacion() {
        return "Cliente: " + nombre + " | NIT: " + nit + " | Dir: " + direccion;
    }

    @Override
    public String toString() { return nombre + " (" + nit + ")"; }
}
