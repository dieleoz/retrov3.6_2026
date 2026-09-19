package com.dpi.retrov36;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
 * Patron certificado de 06_Calibracion/patrones_certificados_P1-P31.csv,
 * embebido en assets. El color NO es dato certificado (lo dice el propio CSV).
 */
public final class Patron {

    public final String nombre;
    public final double valor;
    public final String tipo;
    public final String color;

    public Patron(String nombre, double valor, String tipo, String color) {
        this.nombre = nombre;
        this.valor = valor;
        this.tipo = tipo;
        this.color = color;
    }

    /** Lee el CSV: ignora lineas '#' y la cabecera. */
    public static List<Patron> leer(Reader r) throws IOException {
        List<Patron> out = new ArrayList<>();
        BufferedReader br = new BufferedReader(r);
        String l;
        while ((l = br.readLine()) != null) {
            l = l.trim();
            if (l.isEmpty() || l.startsWith("#") || l.startsWith("patron,")) {
                continue;
            }
            String[] c = l.split(",");
            if (c.length < 4) {
                throw new IOException("linea de patron mal formada: " + l);
            }
            out.add(new Patron(c[0].trim(), Double.parseDouble(c[1].trim()), c[2].trim(), c[3].trim()));
        }
        return out;
    }

    @Override
    public String toString() {
        return nombre + "  " + (valor == Math.rint(valor) ? String.valueOf((long) valor) : String.valueOf(valor))
                + "  tipo " + tipo + "  " + color;
    }
}
