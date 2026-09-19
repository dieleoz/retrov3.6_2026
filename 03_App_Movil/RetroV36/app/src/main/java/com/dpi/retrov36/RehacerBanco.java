package com.dpi.retrov36;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * "Rehacer" en el banco (3.6.14, peticion de Diego; 3.6.15 con QA-App-3.6.14). Java puro: BancoActivity solo
 * pinta y llama aqui, y las pruebas JVM usan lo mismo.
 *
 * - QA-3614-05: no se rehace un OSCURO ni una A5 de una sesion ya terminada: se mediria fuera de su sesion y
 *   el ancla o la s_rep mezclarian horas distintas.
 * - QA-3614-04: si el EXPORTAR de la sesion del paso ya estaba HECHO, vuelve a la cola (el ZIP entregado queda
 *   viejo); y la anulacion cuenta como pendiente de exportar (Campana.seriesSinExportar).
 * - QA-3614-03: el bloqueo por acta en curso y el aviso por acta aceptada estan en
 *   FlujoCalibracion.bloqueoRehacer.
 */
public final class RehacerBanco {

    private RehacerBanco() { }

    /** true si la sesion s ya termino: su EXPORTAR, o algun paso de una sesion posterior, tiene estado. */
    static boolean sesionTerminada(BancoCola cola, Map<Integer, String> est, int sesion) {
        for (BancoCola.Paso p : cola.pasos) {
            String e = est.get(p.orden);
            if (e == null || "REHACER".equals(e)) {
                continue;
            }
            if (p.sesion > sesion || (p.sesion == sesion && "EXPORTAR".equals(p.tipo))) {
                return true;
            }
        }
        return false;
    }

    /** null si el paso se puede rehacer; si no, el motivo. */
    public static String motivoNoRehacer(Campana c, BancoCola cola, int orden) {
        BancoCola.Paso p = cola.paso(orden);
        if (p == null) {
            return "paso " + orden + " desconocido";
        }
        Map<Integer, String> est = c.pasos();
        if (!"HECHO".equals(est.get(orden))) {
            return "el paso " + orden + " no está hecho";
        }
        String sid = c.seriePaso(orden);
        if (sid == null || sid.isEmpty()) {
            return "el paso " + orden + " no tiene serie";
        }
        if (("OSCURO".equals(p.tipo) || "A5".equals(p.tipo)) && sesionTerminada(cola, est, p.sesion)) {
            return "el " + p.tipo + " de la sesión " + p.sesion + " no se rehace: la sesión ya terminó y se mediría "
                    + "fuera de ella (QA-3614-05). Si hace falta, se repite la sesión entera";
        }
        return null;
    }

    /**
     * Rehace el paso: anula su serie con el motivo (una sola linea) y lo devuelve a la cola; si el EXPORTAR de
     * su sesion ya estaba hecho, lo devuelve tambien. Nada se borra.
     */
    public static void rehacer(Campana c, BancoCola cola, int orden, String motivo, String fecha) throws IOException {
        String m = motivoNoRehacer(c, cola, orden);
        if (m != null) {
            throw new IllegalStateException(m);
        }
        String mot = Csv.unaLinea(motivo == null ? "" : motivo).trim();
        c.rehacer(orden, mot, fecha);
        int sesion = cola.paso(orden).sesion;
        Map<Integer, String> est = c.pasos();
        for (BancoCola.Paso p : cola.pasos) {
            if (p.sesion == sesion && "EXPORTAR".equals(p.tipo) && "HECHO".equals(est.get(p.orden))) {
                c.anotarPaso(p.orden, "REHACER", "", fecha, "volver a exportar: se rehízo el paso " + orden);
            }
        }
    }

    /** Texto corto de un paso para botones y listas (QA-3614 §6): "P37 amarillo I, paso 52". */
    public static String corto(Campana c, BancoCola cola, int orden) {
        BancoCola.Paso p = cola.paso(orden);
        if (p == null) {
            return "paso " + orden;
        }
        String quien = "OSCURO".equals(p.tipo) ? "OSCURO" : p.patron + ("A5".equals(p.tipo) ? " (A5)" : "");
        return quien + ", paso " + orden;
    }

    /** Linea de la lista de rehacer: patron, color, tipo, sesion, paso, serie, hora y x. */
    public static String linea(Campana c, BancoCola cola, int orden) {
        BancoCola.Paso p = cola.paso(orden);
        String sid = c.seriePaso(orden);
        Campana.Serie s = sid == null || sid.isEmpty() ? null : c.serie(sid);
        String hora = s == null || s.fecha.length() < 16 ? "" : s.fecha.substring(11, 16);
        return String.format(Locale.US, "%s %s %s · sesión %d · paso %d%s%s", "OSCURO".equals(p.tipo) ? "OSCURO" : p.patron,
                p.color, p.tipoLamina, p.sesion, orden, s == null ? "" : " · " + s.id + " " + hora,
                s == null ? "" : String.format(Locale.US, " · x %.0f", s.media())).replaceAll("\\s+", " ");
    }

    /** Pasos rehacibles cuyo texto contiene el filtro (sin mayusculas), el mas reciente primero. */
    public static List<Integer> filtrar(Campana c, BancoCola cola, String filtro) {
        List<Integer> todos = c.pasosRehacibles();
        List<Integer> l = new ArrayList<>();
        String f = filtro == null ? "" : filtro.trim().toLowerCase(Locale.ROOT);
        for (int i = todos.size() - 1; i >= 0; i--) {
            int o = todos.get(i);
            if (f.isEmpty() || linea(c, cola, o).toLowerCase(Locale.ROOT).contains(f)) {
                l.add(o);
            }
        }
        return l;
    }
}
