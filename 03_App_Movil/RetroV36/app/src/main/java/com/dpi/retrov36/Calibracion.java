package com.dpi.retrov36;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Serie, fecha de calibracion y vencimiento (firmware 3.6.2: #GN#, #GC#). Java puro.
 *
 * Vencimiento = fecha de calibracion + 1 ano. Un 29 de febrero vence el 28 de
 * febrero del ano siguiente (no el 1 de marzo): se elige el dia anterior para
 * no alargar nunca el plazo de un ano.
 */
public final class Calibracion {

    private Calibracion() { }

    public static final String NONE = "NONE";

    /** Variante del firmware V3.6 segun la respuesta a #GC# (enviado solo tras identificar una V3.6 con #V#). */
    public enum Variante { V362, V361, DESCONOCIDA }

    /**
     * #GC,...# -> 3.6.2 (tiene la orden); #ERR,FORMATO# -> 3.6.1 (no la conoce);
     * cualquier otra cosa (timeout, respuesta rara) -> desconocida: no se usan
     * las ordenes de la 3.6.2.
     */
    public static Variante variante(String respuestaGC) {
        String[] c = Tramas.campos(respuestaGC);
        if (c == null) {
            return Variante.DESCONOCIDA;
        }
        if (c.length == 2 && "GC".equals(c[0])) {
            return Variante.V362;
        }
        if ("FORMATO".equals(Tramas.motivoError(respuestaGC))) {
            return Variante.V361;
        }
        return Variante.DESCONOCIDA;
    }

    /** Fecha de "#GC,AAAA-MM-DD#" o "NONE"; null si la trama no cuadra. */
    public static String fechaDe(String trama) {
        String[] c = Tramas.campos(trama);
        if (c == null || c.length != 2 || !"GC".equals(c[0])) {
            return null;
        }
        String f = c[1].trim();
        if (NONE.equals(f) || fechaValida(f)) {
            return f;
        }
        return null;
    }

    /** Serie de "#GN,<serie>#" o "NONE"; null si la trama no cuadra. */
    public static String serieDe(String trama) {
        String[] c = Tramas.campos(trama);
        if (c == null || c.length != 2 || !"GN".equals(c[0])) {
            return null;
        }
        return c[1];
    }

    /** Serie grabable con #SN: 1-12 caracteres ASCII 0x20-0x7E, sin '#' ni ',', distinta de NONE. */
    public static String motivoSerieInvalida(String s) {
        if (s == null || s.isEmpty()) {
            return "la serie está vacía";
        }
        if (s.length() > 12) {
            return "más de 12 caracteres";
        }
        for (char ch : s.toCharArray()) {
            if (ch < 0x20 || ch > 0x7E) {
                return "carácter no ASCII imprimible: '" + ch + "'";
            }
            if (ch == '#' || ch == ',') {
                return "no se admiten '#' ni ','";
            }
        }
        if (NONE.equals(s)) {
            return "NONE está reservado (es lo que responde #GN# sin serie)";
        }
        return null;
    }

    private static boolean bisiesto(int a) {
        return (a % 4 == 0 && a % 100 != 0) || a % 400 == 0;
    }

    private static int diasMes(int a, int m) {
        switch (m) {
            case 2: return bisiesto(a) ? 29 : 28;
            case 4: case 6: case 9: case 11: return 30;
            default: return 31;
        }
    }

    public static boolean fechaValida(String f) {
        if (f == null || !f.matches("\\d{4}-\\d{2}-\\d{2}")) {
            return false;
        }
        int a = Integer.parseInt(f.substring(0, 4));
        int m = Integer.parseInt(f.substring(5, 7));
        int d = Integer.parseInt(f.substring(8, 10));
        return m >= 1 && m <= 12 && d >= 1 && d <= diasMes(a, m);
    }

    /** Fecha + 1 ano; el 29 de febrero pasa al 28 de febrero. */
    public static String vencimiento(String fecha) {
        if (!fechaValida(fecha)) {
            throw new IllegalArgumentException("fecha no válida: " + fecha);
        }
        int a = Integer.parseInt(fecha.substring(0, 4)) + 1;
        int m = Integer.parseInt(fecha.substring(5, 7));
        int d = Integer.parseInt(fecha.substring(8, 10));
        if (m == 2 && d == 29 && !bisiesto(a)) {
            d = 28;
        }
        return String.format(Locale.US, "%04d-%02d-%02d", a, m, d);
    }

    public static String hoy() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    /**
     * Estado del equipo:
     * - "Sin fecha" si #GC# responde NONE;
     * - "No calibrado" si hay fecha pero #V# dice DEF (se repuso fabrica despues de calibrar);
     * - "Calibración vencida" si hoy > vencimiento;
     * - "Calibrado" en otro caso.
     * @param fecha respuesta de #GC# ("NONE" o AAAA-MM-DD); null si no se sabe (firmware sin #GC).
     */
    public static String estado(String fecha, String marca, String hoy) {
        if (fecha == null) {
            return "Fecha de calibración no disponible (firmware sin #GC#)";
        }
        if (NONE.equals(fecha)) {
            return "Sin fecha";
        }
        if ("DEF".equals(marca)) {
            return "No calibrado (fecha " + fecha + " pero #V# dice DEF: se repusieron los de fábrica)";
        }
        String v = vencimiento(fecha);
        if (hoy.compareTo(v) > 0) {
            return "Calibración vencida (calibrado " + fecha + ", venció " + v + ")";
        }
        return "Calibrado " + fecha + ", vence " + v;
    }
}
