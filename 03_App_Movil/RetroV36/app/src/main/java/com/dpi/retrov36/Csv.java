package com.dpi.retrov36;

import java.util.ArrayList;
import java.util.List;

/** Lectura de una linea CSV con comillas (las escribe Medida.csv). Java puro. */
public final class Csv {

    private Csv() { }

    public static List<String> partir(String linea) {
        List<String> out = new ArrayList<>();
        StringBuilder campo = new StringBuilder();
        boolean comillas = false;
        for (int i = 0; i < linea.length(); i++) {
            char c = linea.charAt(i);
            if (comillas) {
                if (c == '"') {
                    if (i + 1 < linea.length() && linea.charAt(i + 1) == '"') {
                        campo.append('"');
                        i++;
                    } else {
                        comillas = false;
                    }
                } else {
                    campo.append(c);
                }
            } else if (c == '"') {
                comillas = true;
            } else if (c == ',') {
                out.add(campo.toString());
                campo.setLength(0);
            } else {
                campo.append(c);
            }
        }
        out.add(campo.toString());
        return out;
    }

    /** Une campos escapando los que lo necesitan. */
    public static String unir(Object... campos) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < campos.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            // QA-3614-02: un campo nunca lleva salto de linea (el diario se lee linea a linea).
            sb.append(Medida.csv(unaLinea(campos[i] == null ? "" : String.valueOf(campos[i]))));
        }
        return sb.toString();
    }

    /** Sustituye los saltos de linea por un espacio (textos libres: motivos, notas). */
    public static String unaLinea(String s) {
        return s == null ? null : s.replaceAll("[\\r\\n]+", " ");
    }

    /**
     * Lee una linea logica de un diario: si una linea deja unas comillas abiertas (diario escrito por la
     * 3.6.14 con un salto dentro de un motivo, QA-3614-02), se le une la siguiente. null al final.
     */
    public static String lineaLogica(java.io.BufferedReader br) throws java.io.IOException {
        String l = br.readLine();
        if (l == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder(l);
        while (comillasAbiertas(sb)) {
            String m = br.readLine();
            if (m == null) {
                break;
            }
            sb.append(' ').append(m);
        }
        return sb.toString();
    }

    private static boolean comillasAbiertas(CharSequence s) {
        int n = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '"') {
                n++;
            }
        }
        return n % 2 == 1;
    }
}
