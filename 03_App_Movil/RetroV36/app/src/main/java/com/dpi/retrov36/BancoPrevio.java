package com.dpi.retrov36;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Banco mas corto (3.6.16, peticion de Diego del 19-sep ~17:10). Java puro.
 *
 * 1. Lo ya medido y valido cuenta: un paso PATRON sin estado queda HECHO si su patron (o un patron EQUIVALENTE de
 *    su grupo, 06_Calibracion/grupos_patrones_equivalentes.csv) tiene una serie elegida, aceptada y no anulada. No
 *    cuenta si es de lo que se escribe (AJUSTE/RE-MEDIDA de 8, b, 5, P81) y esa serie no esta en el protocolo de
 *    PROTOCOLO-AJUSTE (5 x 4), ni si Diego manda repetir el patron (TIPO-I-REPETIR).
 * 2. Lo que Diego manda repetir (TIPO-I-REPETIR, 6048453) y ya esta HECHO con una serie fuera de protocolo vuelve a
 *    la cola: la serie queda ANULADA con el motivo (nada se borra).
 * 3. Cuanto queda: patrones pendientes y minutos estimados, para ensenarlo antes de empezar.
 */
public final class BancoPrevio {

    private BancoPrevio() { }

    /** Grupos de patrones equivalentes: patron -> grupo, y grupo -> patrones. */
    public static final class Grupos {
        final Map<String, String> grupoDe = new HashMap<>();
        final Map<String, List<String>> miembros = new HashMap<>();

        public static Grupos leer(String csv) {
            Grupos g = new Grupos();
            if (csv == null) {
                return g;
            }
            Map<String, Integer> col = null;
            for (String l : csv.split("\r?\n")) {
                if (l.trim().isEmpty() || l.startsWith("#")) {
                    continue;
                }
                List<String> f = Csv.partir(l);
                if (col == null) {
                    col = new HashMap<>();
                    for (int i = 0; i < f.size(); i++) {
                        col.put(f.get(i).trim(), i);
                    }
                    continue;
                }
                Integer ig = col.get("grupo");
                Integer ip = col.get("patron");
                if (ig == null || ip == null || f.size() <= Math.max(ig, ip)) {
                    continue;
                }
                String gr = f.get(ig).trim();
                String pa = f.get(ip).trim();
                g.grupoDe.put(pa, gr);
                List<String> m = g.miembros.get(gr);
                if (m == null) {
                    m = new ArrayList<>();
                    g.miembros.put(gr, m);
                }
                m.add(pa);
            }
            return g;
        }

        public List<String> equivalentes(String patron) {
            String gr = grupoDe.get(patron);
            List<String> m = gr == null ? null : miembros.get(gr);
            return m == null ? new ArrayList<String>() : m;
        }

        public String grupo(String patron) {
            return grupoDe.get(patron);
        }
    }

    public static final class Resultado {
        public int hechos;
        public int repetir;
        public final List<String> texto = new ArrayList<>();
    }

    /** Aplica 1 y 2 sobre la campana (solo anade eventos; idempotente). */
    public static Resultado aplicar(Campana c, BancoCola cola, Grupos g, Decisiones d, String equipo, String fecha)
            throws IOException {
        Resultado r = new Resultado();
        if (c.cerrada()) {
            return r;
        }
        Decisiones.Decision rep = d.decision("TIPO-I-REPETIR", equipo);
        List<String> repetir = rep == null ? new ArrayList<String>() : rep.repetidos;
        int[] req = ProtocoloDisparos.requerido(d.valor("PROTOCOLO-AJUSTE", equipo));
        int[] preciso = {ProtocoloDisparos.K_PRECISO, ProtocoloDisparos.M_PRECISO};
        // 2. Repetir en preciso lo que Diego mando repetir y esta HECHO fuera de protocolo.
        Map<Integer, String> est = c.pasos();
        for (BancoCola.Paso p : cola.pasos) {
            if (!"PATRON".equals(p.tipo) || !repetir.contains(p.patron) || !"HECHO".equals(est.get(p.orden))) {
                continue;
            }
            String sid = c.seriePaso(p.orden);
            Campana.Serie s = sid == null || sid.isEmpty() ? null : c.serie(sid);
            String mal = ProtocoloDisparos.incumple(p.patron, s, preciso);
            if (s != null && mal != null && RehacerBanco.motivoNoRehacer(c, cola, p.orden) == null) {
                RehacerBanco.rehacer(c, cola, p.orden, "repetir en preciso " + preciso[0] + "×" + preciso[1]
                        + " (TIPO-I-REPETIR de Diego, 6048453): la serie estaba a " + mal.substring(p.patron.length() + 1), fecha);
                r.repetir++;
                r.texto.add(p.patron + ": a repetir en preciso");
            }
        }
        // 1. Lo ya medido y valido cuenta como hecho.
        est = c.pasos();
        for (BancoCola.Paso p : cola.pasos) {
            if (!"PATRON".equals(p.tipo) || est.containsKey(p.orden) || repetir.contains(p.patron)) {
                continue;
            }
            List<String> candidatos = new ArrayList<>();
            candidatos.add(p.patron);
            for (String e : g.equivalentes(p.patron)) {
                if (!candidatos.contains(e)) {
                    candidatos.add(e);
                }
            }
            for (String pat : candidatos) {
                Campana.Serie s = c.elegida(pat);
                if (s == null || s.anulada != null || !s.aceptada) {
                    continue;
                }
                if (ProtocoloDisparos.deAjuste(p) && ProtocoloDisparos.incumple(pat, s, req) != null) {
                    continue;          // de lo que se escribe, medido fuera del protocolo: se mide otra vez
                }
                boolean equiv = !pat.equals(p.patron);
                c.anotarPaso(p.orden, "HECHO", s.id, fecha, "ya medido: " + s.id + " " + pat
                        + (equiv ? " (equivalente de " + p.patron + ", grupo " + g.grupo(p.patron) + ")" : ""));
                r.hechos++;
                r.texto.add(p.patron + (equiv ? " por " + pat : "") + ": ya medido (" + s.id + ")");
                break;
            }
        }
        return r;
    }

    /** Patrones pendientes (sin estado o a rehacer) de la cola. */
    public static int patronesPendientes(BancoCola cola, Map<Integer, String> est) {
        int n = 0;
        for (BancoCola.Paso p : cola.pasos) {
            String e = est.get(p.orden);
            if ("PATRON".equals(p.tipo) && (e == null || "REHACER".equals(e) || "SALTADO".equals(e))) {
                n++;
            }
        }
        return n;
    }

    /** Minutos estimados de lo que queda (con el protocolo efectivo). */
    public static double minutosPendientes(BancoCola cola, Map<Integer, String> est, boolean rapido, String protocoloAjuste) {
        return minutosPendientes(cola, est, rapido, protocoloAjuste, null);
    }

    public static double minutosPendientes(BancoCola cola, Map<Integer, String> est, boolean rapido, String protocoloAjuste,
            List<String> repetir) {
        double s = 0;
        for (BancoCola.Paso p : cola.pasos) {
            String e = est.get(p.orden);
            if (e == null || "REHACER".equals(e) || "SALTADO".equals(e)) {
                s += BancoCola.segundos(p, ProtocoloDisparos.efectivo(p, rapido, protocoloAjuste, repetir));
            }
        }
        return s / 60.0;
    }

    public static String textoPendiente(BancoCola cola, Map<Integer, String> est, boolean rapido, String protocoloAjuste) {
        return textoPendiente(cola, est, rapido, protocoloAjuste, null);
    }

    public static String textoPendiente(BancoCola cola, Map<Integer, String> est, boolean rapido, String protocoloAjuste,
            List<String> repetir) {
        return String.format(Locale.US, "Quedan %d patrones, unos %.0f min", patronesPendientes(cola, est),
                minutosPendientes(cola, est, rapido, protocoloAjuste, repetir));
    }
}
