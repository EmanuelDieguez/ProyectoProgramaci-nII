package gt.edu.umg.sistema.ventas.servicio;

/** Error de validación o de regla de negocio (no es un fallo técnico). */
public class ReglaNegocioException extends RuntimeException {
    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
