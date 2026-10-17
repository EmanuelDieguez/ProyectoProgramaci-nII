package gt.edu.umg.sistema.ventas.modelo;

public class Bodega {
    private int idBodega;
    private String nombre;
    private String ubicacion;
    private boolean activa = true;

    public Bodega() {}

    public Bodega(int idBodega, String nombre, String ubicacion, boolean activa) {
        this.idBodega = idBodega;
        this.nombre = nombre;
        this.ubicacion = ubicacion;
        this.activa = activa;
    }

    public int getIdBodega() { return idBodega; }
    public void setIdBodega(int idBodega) { this.idBodega = idBodega; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getUbicacion() { return ubicacion; }
    public void setUbicacion(String ubicacion) { this.ubicacion = ubicacion; }
    public boolean isActiva() { return activa; }
    public void setActiva(boolean activa) { this.activa = activa; }

    @Override
    public String toString() { return nombre; }
}
