package gt.edu.umg.sistema.ventas.dao;

import gt.edu.umg.sistema.ventas.modelo.Bodega;
import java.util.List;

public interface BodegaDAO extends DAO<Bodega> {
    List<Bodega> listarActivas();
}