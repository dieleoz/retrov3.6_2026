package com.dpi.retrov36;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Orden del modo guiado de la campana ("Siguiente patron"). Java puro.
 *
 * 1. Prioritarios pendientes (Campana.prioritarios: huecos, series en
 *    conflicto, orden contradicho).
 * 2. Tipo I de los codigos que se pueden ajustar (Asistente.cobertura > 0):
 *    hoy amarillo (8) y rojo (b).
 * 3. El resto de tipo I, solo como comprobacion.
 * 4. Prueba de giro de PATRON_GIRO a 0 y 90 grados.
 * 5. Lo que quede sin medir o por repetir.
 * Lo medido bien se salta. Lo que el operador salta va al final. Como la cola
 * se recalcula del estado de la campana, al retomar sigue donde iba.
 */
public final class Cola {

    public static final String PATRON_GIRO = "P5";

    private Cola() { }

    public static final class Paso {
        public final String patron;
        public final int orientacion;
        public final String motivo;

        public Paso(String patron, int orientacion, String motivo) {
            this.patron = patron;
            this.orientacion = orientacion;
            this.motivo = motivo;
        }

        String clave() {
            return patron + "@" + orientacion;
        }
    }

    private static boolean girado(Campana c, String patron, int orient) {
        for (Campana.Serie s : c.seriesDe(patron)) {
            if (s.aceptada && s.orientacion == orient) {
                return true;
            }
        }
        return false;
    }

    /** @param saltados claves "patron@orientacion" que el operador salto (van al final, en ese orden). */
    public static List<Paso> construir(Campana c, double tolOrden, List<String> saltados) {
        List<Paso> l = new ArrayList<>();
        Set<String> usados = new LinkedHashSet<>();
        Map<String, String> prio = c.prioritarios(tolOrden);
        // Un prioritario queda resuelto cuando su ultima serie se midio en esta campana
        // (orientacion conocida, no importada) y se acepto.
        for (Map.Entry<String, String> e : prio.entrySet()) {
            Campana.Serie ult = ultima(c, e.getKey());
            boolean resuelto = ult != null && ult.aceptada && ult.orientacion >= 0;
            if (!resuelto) {
                agregar(l, usados, new Paso(e.getKey(), 0, "imprescindible: " + e.getValue()));
            }
        }
        List<Patron> cat = c.catalogo();
        for (Patron p : cat) {
            if (Campana.esTipoI(p) && Asistente.cobertura(Fabrica.CODIGOS[indiceOpaco(p)], cat).gradoMaximo > 0
                    && c.estado(p.nombre) != Campana.Estado.MEDIDO) {
                agregar(l, usados, new Paso(p.nombre, 0, "tipo I que ajusta el código " + Fabrica.CODIGOS[indiceOpaco(p)]));
            }
        }
        for (Patron p : cat) {
            if (Campana.esTipoI(p) && c.estado(p.nombre) != Campana.Estado.MEDIDO) {
                agregar(l, usados, new Paso(p.nombre, 0, "tipo I, sólo comprobación"));
            }
        }
        if (c.patron(PATRON_GIRO) != null) {
            for (int o : new int[]{0, 90}) {
                if (!girado(c, PATRON_GIRO, o)) {
                    Paso g = new Paso(PATRON_GIRO, o, "prueba de giro: " + PATRON_GIRO + " a " + o + "°");
                    if (usados.add(g.clave())) {
                        usados.add(PATRON_GIRO + "@*");
                        l.add(g);
                    }
                }
            }
        }
        for (Patron p : cat) {
            if (c.estado(p.nombre) != Campana.Estado.MEDIDO) {
                agregar(l, usados, new Paso(p.nombre, 0, c.estado(p.nombre) == Campana.Estado.REPETIR
                        ? "repetir" : "sin medir"));
            }
        }
        // Saltados al final, en el orden en que se saltaron.
        List<Paso> fin = new ArrayList<>();
        for (String k : saltados) {
            for (int i = 0; i < l.size(); i++) {
                if (l.get(i).clave().equals(k)) {
                    fin.add(l.remove(i));
                    break;
                }
            }
        }
        l.addAll(fin);
        return l;
    }

    private static void agregar(List<Paso> l, Set<String> usados, Paso p) {
        if (usados.add(p.patron + "@*")) {
            usados.add(p.clave());
            l.add(p);
        }
    }

    private static int indiceOpaco(Patron p) {
        for (int i = 0; i < Fabrica.CODIGOS.length; i++) {
            char k = Fabrica.CODIGOS[i];
            if (!Fabrica.esIntensa(k) && Fabrica.color(k).equalsIgnoreCase(p.color)) {
                return i;
            }
        }
        return 0;
    }

    private static Campana.Serie ultima(Campana c, String patron) {
        List<Campana.Serie> l = c.seriesDe(patron);
        return l.isEmpty() ? null : l.get(l.size() - 1);
    }
}
