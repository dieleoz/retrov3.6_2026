package com.dpi.retrov36;

import android.content.Context;

import java.io.IOException;
import java.util.Locale;

/**
 * "Linea base previa a grabar" (G3): con el firmware ORIGINAL del equipo
 * (V3 2020 en SLV-002) se registra y exporta, como referencia de no regresion:
 *
 *   T-B03  los 12 codigos sobre el mismo patron;
 *   T-B07  los 12 codigos con el cabezal en oscuro (cerca de los negativos);
 *   T-B09  la respuesta a '9', varias veces.
 *
 * Cada peticion va a lineabase_<serie>_<fecha>.csv con su respuesta literal
 * y su desenlace, y al registro de tramas. Llamar desde Cliente.ejecutar().
 */
public final class LineaBase {

    private LineaBase() { }

    public interface Progreso {
        void paso(String texto);
    }

    public static final int REPETICIONES_9 = 3;
    public static final String ETIQUETA = "LINEA_BASE_PRE_GRABACION";

    public static String cabecera() {
        return "etiqueta,fecha_hora,serie,mac,firmware,prueba,patron,codigo,desenlace,respuesta_bruta,valor,ms,x_invertida,resolucion_x";
    }

    /** T-B03 (patron != null) o T-B07 (patron == null: oscuro). */
    public static String codigos(Context ctx, String prueba, Patron patron, Progreso pr)
            throws IOException, InterruptedException {
        Sesion s = Sesion.get();
        String nombre = patron == null ? "oscuro" : patron.nombre;
        Registro.nota("=== " + ETIQUETA + " " + prueba + " sobre " + nombre + " (" + s.identidad() + ") ===");
        StringBuilder res = new StringBuilder();
        int validas = 0;
        for (char k : Fabrica.CODIGOS) {
            pr.paso(prueba + ": código " + k + "...");
            Cliente.Respuesta r = Cliente.instancia().pedir(String.valueOf(k), Tramas.Tipo.MEDIDA,
                    Cliente.TIMEOUT_MEDIDA_MS);
            Integer v = r.valida() ? Tramas.valorMedida(r.trama) : null;
            String x = "";
            String u = "";
            if (v != null) {
                validas++;
                Inversion.Resultado inv = Inversion.invertir(Fabrica.ecuacion(k), v, Double.NaN);
                if (inv.valido) {
                    x = String.format(Locale.US, "%.1f", inv.x);
                    u = String.format(Locale.US, "%.1f", inv.u);
                }
            }
            s.agregarLineaBase(ctx, fila(s, prueba, nombre, String.valueOf(k), r, v, x, u));
            res.append(k).append(": ").append(r.describir()).append(x.isEmpty() ? "" : " -> x=" + x).append('\n');
        }
        res.append(validas).append(" de 12 respondieron.");
        return res.toString();
    }

    /** T-B09: respuesta a '9'. */
    public static String bateria(Context ctx, Progreso pr) throws IOException, InterruptedException {
        Sesion s = Sesion.get();
        Registro.nota("=== " + ETIQUETA + " T-B09: respuesta a 9 (" + s.identidad() + ") ===");
        StringBuilder res = new StringBuilder();
        for (int i = 1; i <= REPETICIONES_9; i++) {
            pr.paso("T-B09: 9, vez " + i + " de " + REPETICIONES_9 + "...");
            Cliente.Respuesta r = Cliente.instancia().pedir("9", Tramas.Tipo.BATERIA, Cliente.TIMEOUT_MEDIDA_MS);
            s.agregarLineaBase(ctx, fila(s, "T-B09", "", "9", r, null, "", ""));
            res.append(i).append(": ").append(r.describir()).append('\n');
        }
        return res.toString().trim();
    }

    private static String fila(Sesion s, String prueba, String patron, String codigo, Cliente.Respuesta r,
                               Integer valor, String x, String u) {
        return String.join(",", ETIQUETA, Sesion.ahoraIso(), Medida.csv(s.serie()), Medida.csv(s.mac),
                Medida.csv(s.firmware()), prueba, Medida.csv(patron), codigo, r.desenlace.name(),
                Medida.csv(r.bruto), valor == null ? "" : String.valueOf(valor),
                r.ms < 0 ? "" : String.valueOf(r.ms), x, u);
    }
}
