package gt.edu.umg.sistema.ventas.evento;

import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Patrón Observador muy simple.
 * Ejemplo: al registrar una venta, FacturaServicio publica FACTURAS + INVENTARIO + PRODUCTOS
 * y TODAS las ventanas abiertas que escuchan esos eventos se recargan solas
 * (Productos, Bodegas, Kardex, Pagos...).
 */
public final class BusEventos {

    private static final Map<EventoSistema, List<Runnable>> OYENTES = new EnumMap<>(EventoSistema.class);

    static {
        for (EventoSistema e : EventoSistema.values()) {
            OYENTES.put(e, new CopyOnWriteArrayList<>());
        }
    }

    private BusEventos() {}

    public static void suscribir(EventoSistema evento, Runnable oyente) {
        OYENTES.get(evento).add(oyente);
    }

    public static void cancelar(Runnable oyente) {
        for (List<Runnable> lista : OYENTES.values()) {
            lista.remove(oyente);
        }
    }

    public static void publicar(EventoSistema... eventos) {
        // Un oyente suscrito a varios eventos se ejecuta una sola vez
        Set<Runnable> aEjecutar = new LinkedHashSet<>();
        for (EventoSistema e : eventos) {
            aEjecutar.addAll(OYENTES.get(e));
        }
        for (Runnable r : aEjecutar) {
            r.run();
        }
    }
}
