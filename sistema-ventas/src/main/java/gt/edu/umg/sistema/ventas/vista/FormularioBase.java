package gt.edu.umg.sistema.ventas.vista;

import gt.edu.umg.sistema.ventas.dao.DataAccessException;
import gt.edu.umg.sistema.ventas.evento.BusEventos;
import gt.edu.umg.sistema.ventas.evento.EventoSistema;
import gt.edu.umg.sistema.ventas.servicio.ReglaNegocioException;

import javax.swing.*;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Clase base de todos los formularios HIJOS que viven dentro del escritorio (JDesktopPane)
 * de la VentanaPrincipal (formulario PADRE).
 */
public abstract class FormularioBase extends JInternalFrame {

    private final List<Runnable> suscripciones = new ArrayList<>();

    protected FormularioBase(String titulo, int ancho, int alto) {
        super(titulo, true, true, true, true); // redimensionable, cerrable, maximizable, minimizable
        setSize(ancho, alto);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        addInternalFrameListener(new InternalFrameAdapter() {
            @Override
            public void internalFrameClosed(InternalFrameEvent e) {
                suscripciones.forEach(BusEventos::cancelar);   // dejar de escuchar al cerrar
            }
        });
    }

    /**
     * Ejecuta "accion" cada vez que otro formulario o servicio publique alguno de los eventos.
     * Así, por ejemplo, la ventana de Productos se recarga sola después de una venta o una compra.
     */
    protected final void escuchar(Runnable accion, EventoSistema... eventos) {
        Runnable oyente = () -> SwingUtilities.invokeLater(() -> {
            if (!isClosed()) {
                ejecutar(accion);
            }
        });
        suscripciones.add(oyente);
        for (EventoSistema e : eventos) {
            BusEventos.suscribir(e, oyente);
        }
    }

    /** Ejecuta una acción mostrando los errores de forma amigable. */
    protected final void ejecutar(Runnable accion) {
        try {
            accion.run();
        } catch (ReglaNegocioException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Validación", JOptionPane.WARNING_MESSAGE);
        } catch (DataAccessException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Base de datos", JOptionPane.ERROR_MESSAGE);
        } catch (RuntimeException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error inesperado: " + ex, "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    protected final void info(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, getTitle(), JOptionPane.INFORMATION_MESSAGE);
    }

    protected final boolean confirmar(String mensaje) {
        return JOptionPane.showConfirmDialog(this, mensaje, "Confirmar",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION;
    }

    /** Mensaje breve en la barra de estado del formulario padre. */
    protected final void notificar(String mensaje) {
        VentanaPrincipal p = principal();
        if (p != null) {
            p.mensaje(mensaje);
        }
    }

    protected final VentanaPrincipal principal() {
        return (VentanaPrincipal) SwingUtilities.getAncestorOfClass(VentanaPrincipal.class, this);
    }
}
