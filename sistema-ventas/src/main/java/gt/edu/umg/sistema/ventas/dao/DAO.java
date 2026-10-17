package gt.edu.umg.sistema.ventas.dao;

import java.util.List;

/** Operaciones CRUD básicas para los catálogos. */
public interface DAO<T> {
    void guardar(T obj);
    T buscarPorId(int id);
    List<T> listarTodos();
    void actualizar(T obj);
    void eliminar(int id);
}
