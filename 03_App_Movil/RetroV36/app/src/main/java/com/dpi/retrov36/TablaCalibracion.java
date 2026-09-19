package com.dpi.retrov36;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tabla RF-CAL-37: metodo por codigo, fijado en el APK (SPEC-Calibracion-V3.6.md §12.2, corregida en
 * 1f1c4e3 tras P10-C2; sigue a 06_Calibracion/SLV-002/REFORMULACION-y-Simulacion-2026-09-19.md). El
 * operador no elige grado, metodo ni patron de re-medida. Java puro.
 *
 * 3.6.14 (QA-3613-03): la tabla es DE SLV-002. Para otro equipo todos los codigos se quedan en "solo
 * verificar" hasta que tenga su propia tabla; las decisiones de Diego se leen por equipo.
 *
 * SLV-002:
 * - 1, 2: escritos el 19-sep: no se reescriben (se cotejan: T-C41).
 * - 3, 4, 6: grado 1; recta anclada si la libre no pasa #S. Re-medida P123, P11, P86 (PA-16).
 * - 8: recta anclada en el OSCURO (REFORM §3.3). Re-medida P43. Sin dispensa. Va SOLO y PRIMERO: su acta
 *   aceptada antes de ofrecer el b y el 5 (P12 §6, condicion 3).
 * - b: recta anclada (REFORM §3.4), re-medida P49, solo con PA-24 (dispensa limitada a su alcance: P12 §5.2)
 *   Y con la regla de re-medida REMEDIDA-b decidida (QA-3612-06). Tras el 8.
 * - 5: recta anclada con la cobertura de RF-APP-42, solo con PA-14, con sus 13 pasos del banco hechos
 *   (P12 §6, condicion 6). Tras el 8.
 * - 7, a, c, d: solo verificar.
 */
public final class TablaCalibracion {

    /**
     * Version de las reglas (no del APK): un acta abierta con otra version no se acepta (QA-3614-09). r4 =
     * 3.6.15: decisiones de c836cae (PA-24 ampliada y como techo, REMEDIDA-b, P81 fuera del ajuste del 5).
     */
    public static final String VERSION = "RF-CAL-37 de SPEC-Calibracion-V3.6.md (1f1c4e3), reglas r5 (c836cae, 6048453)";
    /** Unico equipo con tabla en este APK. */
    public static final String EQUIPO_CON_TABLA = "SLV-002";
    /** MAC de SLV-002 (Coviandina, V3.6): la tabla y las decisiones de "SLV-002" son de ESTE equipo. */
    public static final String MAC_SLV002 = "00:21:13:05:19:3B";
    /** Series de ese equipo: SLV-002 y, desde la decision SERIE-2 de Diego (31d3b74), SLV-002-2026. */
    public static final java.util.List<String> SERIES_SLV002 = java.util.Arrays.asList("SLV-002", "SLV-002-2026");

    /**
     * 3.6.15: nombre con el que se buscan la tabla, las decisiones y los heredados. Si el equipo (por MAC) es el
     * de SLV-002 y su historial de series incluye un nombre de SLV-002, es "SLV-002" (alias SLV-002-2026). Un
     * equipo con otra MAC que se llame SLV-002 NO recibe la tabla ni las decisiones de SLV-002.
     */
    public static String canonico(java.util.List<String> historial, String mac) {
        if (mac != null && MAC_SLV002.equalsIgnoreCase(mac.trim())) {
            for (String h : historial) {
                for (String a : SERIES_SLV002) {
                    if (a.equalsIgnoreCase(h)) {
                        return EQUIPO_CON_TABLA;
                    }
                }
            }
        }
        for (String h : historial) {
            boolean deSlv = false;
            for (String a : SERIES_SLV002) {
                deSlv |= a.equalsIgnoreCase(h);
            }
            if (!deSlv) {
                return h;
            }
        }
        return historial.isEmpty() ? "" : historial.get(historial.size() - 1) + " (MAC distinta de SLV-002)";
    }

    public enum Metodo { GRADO1, ANCLADA, GRADO1_O_ANCLADA, NO_REESCRIBIR, SOLO_VERIFICAR }

    public static final class Fila {
        public final char codigo;
        public final Metodo metodo;
        public final String remedida;
        /** Criterios que se dispensan (vacio si ninguno). */
        public final String dispensa;
        /** Lo unico que la dispensa cubre (P12 §5.2). */
        public final List<Decisiones.Alcance> alcance;
        /** Regla de la re-medida con dispensa: RF-CAL-18 o CERTIFICADO; vacio = la normal. */
        public final String reglaRemedida;
        /** Codigo que tiene que tener un acta ACEPTADA antes (0 si ninguno). */
        public final char requiereAceptado;
        /** true: se calibra solo, sin otros codigos en la misma pulsacion. */
        public final boolean solo;
        /** true: exige todos los pasos PATRON del codigo hechos, no solo AJUSTE y RE-MEDIDA. */
        public final boolean todosLosPasos;
        /** Patrones que una decision saca del ajuste (P81 del 5). */
        public List<String> excluidos = Collections.emptyList();
        public final String origen;
        /** Aviso que la tarjeta y el acta muestran siempre. */
        public final String aviso;

        Fila(char codigo, Metodo metodo, String remedida, String dispensa, List<Decisiones.Alcance> alcance,
             String reglaRemedida, char requiereAceptado, boolean solo, boolean todosLosPasos, String origen, String aviso) {
            this.codigo = codigo;
            this.metodo = metodo;
            this.remedida = remedida;
            this.dispensa = dispensa;
            this.alcance = alcance;
            this.reglaRemedida = reglaRemedida;
            this.requiereAceptado = requiereAceptado;
            this.solo = solo;
            this.todosLosPasos = todosLosPasos;
            this.origen = origen;
            this.aviso = aviso;
        }

        static Fila simple(char k, Metodo m, String remedida, String origen, String aviso) {
            return new Fila(k, m, remedida, "", Collections.<Decisiones.Alcance>emptyList(), "", (char) 0, false, false,
                    origen, aviso);
        }

        public boolean escribible() {
            return metodo == Metodo.GRADO1 || metodo == Metodo.ANCLADA || metodo == Metodo.GRADO1_O_ANCLADA;
        }

        /** El incumplimiento (criterio, patron, desviacion %) queda dentro de lo dispensado. */
        public boolean dispensado(String criterio, String patron, double dev) {
            for (Decisiones.Alcance a : alcance) {
                if (a.cubre(criterio, patron, dev)) {
                    return true;
                }
            }
            return false;
        }

        public Decisiones.Alcance alcanceDe(String criterio, String patron) {
            for (Decisiones.Alcance a : alcance) {
                if (a.criterio.equals(criterio) && a.patron.equals(patron)) {
                    return a;
                }
            }
            return null;
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
            + "o −11,1 % (REFORM) del certificado y la re-medida exige ±10 %. La regla de la re-medida del b la confirma "
            + "Diego (REMEDIDA-b en decisiones.csv); el umbral no se toca.";

    /** Tabla de SLV-002 sin decisiones (pruebas). */
    public static Map<Character, Fila> tabla() {
        return tabla(Decisiones.ninguna(), EQUIPO_CON_TABLA);
    }

    public static Map<Character, Fila> tabla(Decisiones d, String equipo) {
        Map<Character, Fila> t = new LinkedHashMap<>();
        if (!EQUIPO_CON_TABLA.equals(equipo)) {
            String av = "La tabla RF-CAL-37 de este APK es la de " + EQUIPO_CON_TABLA + ": para " + equipo
                    + " no hay tabla ni decisiones. Sólo verificar (QA-3613-03).";
            for (char k : Fabrica.CODIGOS) {
                t.put(k, Fila.simple(k, Metodo.SOLO_VERIFICAR, "", "sin tabla para " + equipo, av));
            }
            return t;
        }
        t.put('1', Fila.simple('1', Metodo.NO_REESCRIBIR, "P28", "Diego 19-sep 11:20; REFORM §3.1", ""));
        t.put('2', Fila.simple('2', Metodo.NO_REESCRIBIR, "P25", "Diego, opción b; REFORM §3.2", ""));
        t.put('3', Fila.simple('3', Metodo.GRADO1_O_ANCLADA, "P123", "PA-16", ""));
        t.put('4', Fila.simple('4', Metodo.GRADO1_O_ANCLADA, "P11", "PA-16", ""));
        Decisiones.Decision pa14 = d.decision("PA-14", equipo);
        Fila f5 = pa14 != null
                ? new Fila('5', Metodo.ANCLADA, "P81", "", Collections.<Decisiones.Alcance>emptyList(), "", '8', false, true,
                "PA-14: " + pa14.texto(), "PA-14: recta anclada con la cobertura de RF-APP-42 (el ancla cuenta como un "
                + "nivel); exige sus 13 pasos del banco hechos; tras el acta ACEPTADA del 8"
                + (pa14.excluidos.isEmpty() ? "" : "; fuera del ajuste: " + pa14.excluidos + " (solo verificación y re-medida)"))
                : Fila.simple('5', Metodo.SOLO_VERIFICAR, "P81", "PA-14 sin decidir",
                "código 5 de fábrica, fuera de tolerancia, sin decisión (PA-14 sin decidir)");
        if (pa14 != null) {
            f5.excluidos = pa14.excluidos;
        }
        t.put('5', f5);
        t.put('6', Fila.simple('6', Metodo.GRADO1_O_ANCLADA, "P86", "PA-16", ""));
        t.put('7', Fila.simple('7', Metodo.SOLO_VERIFICAR, "", "§12.1", ""));
        t.put('8', new Fila('8', Metodo.ANCLADA, "P43", "", Collections.<Decisiones.Alcance>emptyList(), "", (char) 0,
                true, false, "REFORM §3.3", "El 8 va solo y primero; el b y el 5, en un acta posterior (P12 §6.3)"));
        t.put('a', Fila.simple('a', Metodo.SOLO_VERIFICAR, "", "§12.1", ""));
        Decisiones.Decision pa24 = d.decision("PA-24", equipo);
        String regla = d.valor("REMEDIDA-b", equipo);
        boolean reglaOk = "RF-CAL-18".equals(regla) || "CERTIFICADO".equals(regla);
        if (pa24 != null && reglaOk) {
            t.put('b', new Fila('b', Metodo.ANCLADA, "P49", "RF-CAL-14/15 (PA-24)", pa24.alcance, regla, '8', false, false,
                    "REFORM §3.4; " + pa24.texto() + "; re-medida: " + d.decision("REMEDIDA-b", equipo).texto(),
                    "Dispensa limitada a su alcance: " + alcanceTexto(pa24.alcance) + ". Re-medida del b por "
                            + regla + ("RF-CAL-18".equals(regla) ? ": sólo comprueba la reproducción de la campaña y "
                            + "la curva escrita, no el certificado (P12 §4.2)" : "")));
        } else {
            t.put('b', Fila.simple('b', Metodo.SOLO_VERIFICAR, "P49", "REFORM §3.4",
                    (pa24 == null ? "PA-24 sin decidir" : "falta la regla de la re-medida del b (REMEDIDA-b)")
                            + " en decisiones.csv: el b no se escribe. " + AVISO_B));
        }
        t.put('c', Fila.simple('c', Metodo.SOLO_VERIFICAR, "", "§12.1", ""));
        t.put('d', Fila.simple('d', Metodo.SOLO_VERIFICAR, "", "§12.1", ""));
        return t;
    }

    private static String alcanceTexto(List<Decisiones.Alcance> l) {
        StringBuilder sb = new StringBuilder();
        for (Decisiones.Alcance a : l) {
            sb.append(sb.length() == 0 ? "" : "; ").append(a.texto());
        }
        return sb.length() == 0 ? "nada" : sb.toString();
    }

    public static Fila fila(char k) {
        return tabla().get(k);
    }

    public static Fila fila(char k, Decisiones d, String equipo) {
        return tabla(d, equipo).get(k);
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
