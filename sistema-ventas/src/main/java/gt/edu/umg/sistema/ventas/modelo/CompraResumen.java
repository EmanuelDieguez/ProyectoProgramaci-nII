package gt.edu.umg.sistema.ventas.modelo;

import java.time.LocalDateTime;

public record CompraResumen(int idCompra, LocalDateTime fecha, String proveedor,
                            String bodega, String empleado, double total) {
}
