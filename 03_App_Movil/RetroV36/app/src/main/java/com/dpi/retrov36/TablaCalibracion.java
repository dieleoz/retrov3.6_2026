package com.dpi.retrov36;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Tabla RF-CAL-37: metodo por codigo, fijado en el APK (SPEC-Calibracion-V3.6.md §12.2, corregida en
 * 1f1c4e3 tras P10-C2; sigue a 06_Calibracion/SLV-002/REFORMULACION-y-Simulacion-2026-09-19.md). El
 * operador no elige grado, metodo ni patron de re-medida. Java puro.
 *
 * - 1: grado 1, ESCRITO el 19-sep: no se reescribe. Re-medida P28. Dispensa RF-CAL-14/15/16 (acta, linea 11).
 * - 2: recta anclada, ESCRITO el 19-sep: no se reescribe. Re-medida P25. Dispensa RF-CAL-14/15 (acta, linea 18).
 * - 3, 4, 6: grado 1; recta anclada si la libre no pasa #S. Re-medida P123, P11, P86 (PA-16).
 * - 5: solo verificar; recta anclada si PA-14 (parametro PA14_ANCLADA_5, falso hasta que Diego decida).
 * - 8: recta anclada en el OSCURO (REFORM §3.3). Re-medida P43. Sin dispensa.
 * - b: recta anclada en el OSCURO (REFORM §3.4). Re-medida P49. Exige la conformidad a RF-CAL-14/15
 *   (PA-24, pendiente de Diego): sin ella, solo verificar.
 * - 7, a, c, d: solo verificar.
 */
public final class TablaCalibracion {

    public static final String VERSION = "RF-CAL-37 de SPEC-Calibracion-V3.6.md (1f1c4e3), APK 3.6.12";

    /** PA-14: el 5 con recta anclada. Falso hasta que Diego lo decida. */
    public static volatile boolean PA14_ANCLADA_5 = false;

    public enum Metodo { GRADO1, ANCLADA, GRADO1_O_ANCLADA, NO_REESCRIBIR, SOLO_VERIFICAR }

    public static final class Fila {
        public final char codigo;
        public final Metodo metodo;
        public final String remedida;
        /** Criterios que se dispensan (vacio si ninguno). */
        public final String dispensa;
        /** true si escribir exige la conformidad expresa del superadministrador (b, PA-24). */
        public final boolean exigeConformidad;
        public final String origen;

        Fila(char codigo, Metodo metodo, String remedida, String dispensa, boolean exigeConformidad, String origen) {
            this.codigo = codigo;
            this.metodo = metodo;
            this.remedida = remedida;
            this.dispensa = dispensa;
            this.exigeConformidad = exigeConformidad;
            this.origen = origen;
        }

        public boolean escribible() {
            return metodo == Metodo.GRADO1 || metodo == Metodo.ANCLADA || metodo == Metodo.GRADO1_O_ANCLADA;
        }

        public boolean necesitaOscuro() {
            return metodo == Metodo.ANCLADA;
        }

        public String texto() {
            String m;
            switch (metodo) {
                case GRADO1: m = "grado 1"; break;
                case ANCLADA: m = "recta anclada en el OSCURO"; break;
                case GRADO1_O_ANCLADA: m = "grado 1; recta anclada si la libre no pasa #S"; break;
                case NO_REESCRIBIR: m = "escrito el 19-sep: no se reescribe"; break;
                default: m = "sólo verificar"; break;
            }
            return codigo + " (" + Fabrica.nombre(codigo) + "): " + m
                    + (remedida.isEmpty() ? "" : ", re-medida " + remedida)
                    + (dispensa.isEmpty() ? "" : ", dispensa " + dispensa) + " [" + origen + "]";
        }
    }

    private TablaCalibracion() { }

    public static Map<Character, Fila> tabla() {
        Map<Character, Fila> t = new LinkedHashMap<>();
        t.put('1', new Fila('1', Metodo.NO_REESCRIBIR, "P28", "RF-CAL-14/15/16", false, "Diego 19-sep 11:20; REFORM §3.1"));
        t.put('2', new Fila('2', Metodo.NO_REESCRIBIR, "P25", "RF-CAL-14/15", false, "Diego, opción b; REFORM §3.2"));
        t.put('3', new Fila('3', Metodo.GRADO1_O_ANCLADA, "P123", "", false, "PA-16"));
        t.put('4', new Fila('4', Metodo.GRADO1_O_ANCLADA, "P11", "", false, "PA-16"));
        t.put('5', PA14_ANCLADA_5
                ? new Fila('5', Metodo.ANCLADA, "P81", "", false, "PA-14 = recta anclada")
                : new Fila('5', Metodo.SOLO_VERIFICAR, "P81", "", false, "PA-14 pendiente"));
        t.put('6', new Fila('6', Metodo.GRADO1_O_ANCLADA, "P86", "", false, "PA-16"));
        t.put('7', new Fila('7', Metodo.SOLO_VERIFICAR, "", "", false, "§12.1"));
        t.put('8', new Fila('8', Metodo.ANCLADA, "P43", "", false, "REFORM §3.3"));
        t.put('a', new Fila('a', Metodo.SOLO_VERIFICAR, "", "", false, "§12.1"));
        t.put('b', new Fila('b', Metodo.ANCLADA, "P49", "RF-CAL-14/15 (PA-24, pendiente de Diego)", true, "REFORM §3.4"));
        t.put('c', new Fila('c', Metodo.SOLO_VERIFICAR, "", "", false, "§12.1"));
        t.put('d', new Fila('d', Metodo.SOLO_VERIFICAR, "", "", false, "§12.1"));
        return t;
    }

    public static Fila fila(char k) {
        return tabla().get(k);
    }
}
