package com.dpi.retrov36;

import java.util.Locale;

/** Una lectura de x sobre un patron, tal como se exporta a CSV. Java puro. */
public final class Medida {

    public final String fechaHora;
    public final String serie;
    public final String mac;
    public final String firmware;
    public final Patron patron;
    /** Codigo enviado: 'e' o '6'. */
    public final char codigo;
    /** Respuesta bruta tal cual (vacia si no hubo). */
    public final String bruta;
    /** NaN si no valida. */
    public final double x;
    /** Resolucion de x (0 con 'e'; semiancho del intervalo con '6'). */
    public final double u;
    public final String metodo;
    public final String nota;

    public Medida(String fechaHora, String serie, String mac, String firmware, Patron patron,
                  char codigo, String bruta, double x, double u, String metodo, String nota) {
        this.fechaHora = fechaHora;
        this.serie = serie;
        this.mac = mac;
        this.firmware = firmware;
        this.patron = patron;
        this.codigo = codigo;
        this.bruta = bruta;
        this.x = x;
        this.u = u;
        this.metodo = metodo;
        this.nota = nota;
    }

    public boolean valida() {
        return !Double.isNaN(x);
    }

    public static String cabecera() {
        return "fecha_hora,serie,mac,firmware,patron,valor_certificado,tipo_lamina,color,codigo,respuesta_bruta,x,resolucion_x,metodo,nota";
    }

    public String aCsv() {
        return String.join(",",
                fechaHora, csv(serie), csv(mac), csv(firmware), csv(patron.nombre),
                num(patron.valor), csv(patron.tipo), csv(patron.color), String.valueOf(codigo),
                csv(bruta), Double.isNaN(x) ? "" : num(x), Double.isNaN(u) ? "" : num(u),
                csv(metodo), csv(nota));
    }

    private static String num(double v) {
        return v == Math.rint(v) ? String.valueOf((long) v) : String.format(Locale.US, "%.2f", v);
    }

    /** Entre comillas si hace falta; comillas internas duplicadas. */
    static String csv(String s) {
        if (s == null) {
            return "";
        }
        if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains(":")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
