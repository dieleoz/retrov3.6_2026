package com.dpi.retrov36;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 3.6.17 ("Calibrar y grabar", peticion de Diego): al acabar, el informe de calibracion y los ZIP quedan en la
 * carpeta de descargas (Download/RTV/) sin que el operador haga nada. Java puro: la carpeta es una interfaz, que en
 * la app es MediaStore (Campanas.copiarADescargas) y en las pruebas un directorio temporal.
 *
 * El informe va en TEXTO en la 3.6.17; el PDF (SPEC-Calibracion-V3.6.md §12) queda para la siguiente version.
 */
public final class ExportadorFinal {

    public static final String TITULO = "INFORME DE AJUSTE Y VERIFICACIÓN CONTRA PATRONES CERTIFICADOS";

    private ExportadorFinal() { }

    /** La carpeta de descargas. */
    public interface Descargas {
        /** Guarda el fichero y devuelve donde quedo (para decirlo en pantalla). */
        String guardar(String nombre, byte[] datos) throws IOException;
    }

    /**
     * El informe de las actas aceptadas en esta pulsacion: serie y MAC, firmware, fecha de calibracion y vencimiento,
     * y el acta de cada codigo entera (patrones con su certificado, medias, curvas antes y despues, residuos,
     * re-medidas, decisiones de Diego citadas y SHA-256 del ZIP de soporte).
     */
    public static String informe(List<Acta> actas, String app, String hoy) {
        StringBuilder sb = new StringBuilder(TITULO).append('\n');
        sb.append("Ajuste contra patrones, no calibración trazable.\n");
        sb.append("App: ").append(app).append('\n');
        sb.append("Fecha de calibración: ").append(hoy).append("; vence: ").append(Calibracion.vencimiento(hoy)).append('\n');
        sb.append("Actas aceptadas: ").append(actas.size()).append("\n\n");
        for (Acta a : actas) {
            sb.append("==================================================================\n");
            sb.append(a.texto()).append('\n');
        }
        return sb.toString();
    }

    /** Nombre del informe: informe_calibracion_<serie>_<AAAA-MM-DD>.txt, sin caracteres raros. */
    public static String nombreInforme(String serie, String hoy) {
        return "informe_calibracion_" + (serie == null ? "equipo" : serie.replaceAll("[^A-Za-z0-9._-]", "_")) + "_" + hoy
                + ".txt";
    }

    /**
     * Guarda el informe y los ZIP en la carpeta de descargas. Devuelve donde quedo cada uno; un fallo se dice en la
     * lista ("SIN COPIA: ...") y no para los demas.
     */
    public static List<String> exportar(Descargas d, String nombreInforme, String informe, List<String> nombresZip,
                                        List<byte[]> zips) {
        List<String> r = new ArrayList<>();
        try {
            r.add(d.guardar(nombreInforme, informe.getBytes(StandardCharsets.UTF_8)));
        } catch (IOException | RuntimeException e) {
            r.add("SIN COPIA del informe: " + e.getMessage());
        }
        for (int i = 0; i < zips.size(); i++) {
            try {
                r.add(d.guardar(nombresZip.get(i), zips.get(i)));
            } catch (IOException | RuntimeException e) {
                r.add("SIN COPIA de " + nombresZip.get(i) + ": " + e.getMessage());
            }
        }
        return r;
    }
}
