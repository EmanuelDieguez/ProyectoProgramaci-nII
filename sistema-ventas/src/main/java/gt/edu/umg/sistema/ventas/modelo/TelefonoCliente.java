package gt.edu.umg.sistema.ventas.modelo;

/** Un número de teléfono de un cliente (tabla telefono_cliente). */
public class TelefonoCliente {
    private int idTelefono;
    private int idCliente;
    private String numero;
    private String tipo;      // Celular, Casa, Trabajo, Otro

    public TelefonoCliente() {}

    public TelefonoCliente(String numero, String tipo) {
        this.numero = numero;
        this.tipo = tipo;
    }

    public TelefonoCliente(int idTelefono, int idCliente, String numero, String tipo) {
        this.idTelefono = idTelefono;
        this.idCliente = idCliente;
        this.numero = numero;
        this.tipo = tipo;
    }

    public int getIdTelefono() { return idTelefono; }
    public void setIdTelefono(int idTelefono) { this.idTelefono = idTelefono; }
    public int getIdCliente() { return idCliente; }
    public void setIdCliente(int idCliente) { this.idCliente = idCliente; }
    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    /** Así se muestra en la lista de la ventana: "5555-1234 (Celular)" */
    @Override
    public String toString() {
        return numero + " (" + tipo + ")";
    }
}