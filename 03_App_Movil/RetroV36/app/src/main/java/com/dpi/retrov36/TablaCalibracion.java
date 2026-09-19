package com.dpi.retrov36;

import java.util.Collections;
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
 * - 5: solo verificar; recta anclada SOLO con PA-14 decidida por Diego (Decisiones; decidida el 19-sep),
 *   con la cobertura de RF-APP-42 (el ancla cuenta como nivel).
 * - 8: recta anclada en el OSCURO (REFORM §3.3). Re-medida P43. Sin dispensa.
 * - b: recta anclada en el OSCURO (REFORM §3.4). Re-medida P49. Exige la dispensa RF-CAL-14/15 de PA-24:
 *   SOLO con PA-24 decidida por Diego (Decisiones, P11-M5, QA-3612-07). La casilla del operador no la
 *   concede. PA-24 decidida el 19-sep (DECISIONES-Diego-2026-09-19.md, 8af526d): la re-medida del b se
 *   juzga con RF-CAL-18 frente a la curva escrita y el incumplimiento frente al certificado (P49 a
 *   -10,2/-11,1 %, QA-3612-06) queda escrito como dispensado. El umbral no se toca.
 * - 7, a, c, d: solo verificar.
 */
public final class TablaCalibracion {

    public static final String VERSION = "RF-CAL-37 de SPEC-Calibracion-V3.6.md (1f1c4e3), APK 3.6.13";

    public enum Metodo { GRADO1, ANCLADA, GRADO1_O_ANCLADA, NO_REESCRIBIR, SOLO_VERIFICAR }

    public static final class Fila {
        public final char codigo;
        public final Metodo metodo;
        public final String remedida;
        /** Criterios que se dispensan (vacio si ninguno). */
        public final String dispensa;
        /** Decision de Diego que exige para escribirse (PA-24, PA-14); vacio si ninguna. */
        public final String exigeDecision;
        public final String origen;
        /** Aviso que la tarjeta y el acta muestran siempre (contradicciones abiertas, estado sin decidir). */
        public final String aviso;

        Fila(char codigo, Metodo metodo, String remedida, String dispensa, String exigeDecision, String origen,
             String aviso) {
            this.codigo = codigo;
            this.metodo = metodo;
            this.remedida = remedida;
            this.dispensa = dispensa;
            this.exigeDecision = exigeDecision;
            this.origen = origen;
            this.aviso = aviso;
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

    private static final String AVISO_B = "Contradicción abierta QA-3612-06: la recta anclada da P49 a −10,2 % (SPEC) "
            + "o −11,1 % (REFORM) del certificado y la re-medida exige ±10 %: previsiblemente NO CONFORME. El umbral no "
            + "se toca; lo decide Diego y se cierra midiendo.";
    /** Con PA-24 decidida: la dispensa cubre el ajuste y la re-medida se juzga frente a la curva escrita. */
    private static final String AVISO_B_PA24 = "Con PA-24, la re-medida en P49 se juzga con RF-CAL-18 frente a la curva "
            + "escrita (y la reproducción de la campaña); la desviación frente al certificado se anota en el acta y, si pasa "
            + "de ±10 %, como incumplimiento DISPENSADO por PA-24 (QA-3612-06, sin tocar el umbral).";

    /** Tabla sin decisiones: el 5 y el b solo se verifican. */
    public static Map<Character, Fila> tabla() {
        return tabla(Decisiones.ninguna());
    }

    public static Map<Character, Fila> tabla(Decisiones d) {
        Map<Character, Fila> t = new LinkedHashMap<>();
        t.put('1', new Fila('1', Metodo.NO_REESCRIBIR, "P28", "RF-CAL-14/15/16", "", "Diego 19-sep 11:20; REFORM §3.1", ""));
        t.put('2', new Fila('2', Metodo.NO_REESCRIBIR, "P25", "RF-CAL-14/15", "", "Diego, opción b; REFORM §3.2", ""));
        t.put('3', new Fila('3', Metodo.GRADO1_O_ANCLADA, "P123", "", "", "PA-16", ""));
        t.put('4', new Fila('4', Metodo.GRADO1_O_ANCLADA, "P11", "", "", "PA-16", ""));
        t.put('5', d.tomada("PA-14")
                ? new Fila('5', Metodo.ANCLADA, "P81", "", "PA-14", "PA-14: " + d.decision("PA-14").texto(),
                "PA-14: recta anclada; cobertura de RF-APP-42 (el ancla cuenta como un nivel); ajuste con todos los "
                + "patrones del 5 medidos en el banco")
                : new Fila('5', Metodo.SOLO_VERIFICAR, "P81", "", "PA-14", "PA-14 sin decidir",
                "código 5 de fábrica, fuera de tolerancia, sin decisión (PA-14 sin decidir)"));
        t.put('6', new Fila('6', Metodo.GRADO1_O_ANCLADA, "P86", "", "", "PA-16", ""));
        t.put('7', new Fila('7', Metodo.SOLO_VERIFICAR, "", "", "", "§12.1", ""));
        t.put('8', new Fila('8', Metodo.ANCLADA, "P43", "", "", "REFORM §3.3", ""));
        t.put('a', new Fila('a', Metodo.SOLO_VERIFICAR, "", "", "", "§12.1", ""));
        t.put('b', d.tomada("PA-24")
                ? new Fila('b', Metodo.ANCLADA, "P49", "RF-CAL-14/15 (PA-24)", "PA-24",
                "REFORM §3.4; PA-24: " + d.decision("PA-24").texto(), AVISO_B_PA24)
                : new Fila('b', Metodo.SOLO_VERIFICAR, "P49", "", "PA-24", "REFORM §3.4; PA-24 sin decidir",
                "PA-24 sin decidir: el b no se escribe (sólo verificar). " + AVISO_B));
        t.put('c', new Fila('c', Metodo.SOLO_VERIFICAR, "", "", "", "§12.1", ""));
        t.put('d', new Fila('d', Metodo.SOLO_VERIFICAR, "", "", "", "§12.1", ""));
        return t;
    }

    public static Fila fila(char k) {
        return tabla().get(k);
    }

    public static Fila fila(char k, Decisiones d) {
        return tabla(d).get(k);
    }

    // ------------------------------------------------------ codigos heredados (T-C41)

    /**
     * Curvas escritas antes de este flujo que el acta nueva tiene que verificar (T-C41, P11-M6):
     * por serie del equipo, codigo -> trama #G certificada. SLV-002: acta de las 12:23:26 del 19-sep
     * (campana_SLV-002_20260919_122727.zip, md5 ce1f35fc64439cbb602d014b725fadfb), lineas 9 y 16.
     */
    public static Map<Character, String> heredados(String serie) {
        if ("SLV-002".equals(serie)) {
            Map<Character, String> m = new LinkedHashMap<>();
            m.put('1', "#G,1,0.00000000E+00,0.00000000E+00,2.98471571E-01,-1.62263885E+02#");
            m.put('2', "#G,2,0.00000000E+00,0.00000000E+00,3.65483810E-01,-2.06628295E+02#");
            return m;
        }
        return Collections.emptyMap();
    }

    public static String origenHeredados(String serie) {
        return "SLV-002".equals(serie) ? "acta de SLV-002 del 19-sep-2026 12:23:26 (ZIP md5 ce1f35fc…)" : "";
    }
}
