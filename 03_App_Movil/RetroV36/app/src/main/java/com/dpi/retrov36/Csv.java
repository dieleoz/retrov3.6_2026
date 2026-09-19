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
            sb.append(Medida.csv(campos[i] == null ? "" : String.valueOf(campos[i])));
        }
        return sb.toString();
    }
}
