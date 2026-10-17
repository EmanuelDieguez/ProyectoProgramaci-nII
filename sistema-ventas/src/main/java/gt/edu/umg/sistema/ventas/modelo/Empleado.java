package gt.edu.umg.sistema.ventas.modelo;

public class Empleado extends Persona {
    private String codigoEmpleado;
    private String puesto;

    public Empleado() { super(); }

    public Empleado(int id, String nombre, String nit, String telefono,
                    String codigoEmpleado, String puesto) {
        super(id, nombre, nit, telefono);
        this.codigoEmpleado = codigoEmpleado;
        this.puesto = puesto;
    }

    public String getCodigoEmpleado() { return codigoEmpleado; }
    public void setCodigoEmpleado(String codigoEmpleado) { this.codigoEmpleado = codigoEmpleado; }
    public String getPuesto() { return puesto; }
    public void setPuesto(String puesto) { this.puesto = puesto; }

    @Override
    public String mostrarInformacion() {
        return "Empleado: " + nombre + " | Puesto: " + puesto;
    }

    @Override
    public String toString() { return nombre; }
}
