package com.dpi.retrousuario.dominio;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

/**
 * Nombre del ZIP exportado (RF-USR-15 bis, M-6, corrige r5, C4): {@code RTVU_<serie>_<AAAAMMDD-HHMMSS>.zip},
 * con la serie saneada a {@code [A-Za-z0-9_-]}, sin secuencias {@code ".."}, {@code SIN_SERIE} si no
 * hay serie leída, {@code VARIOS} si la sesión mezcla equipos o se exportó sin conexión, y un sufijo
 * {@code _2}, {@code _3}... si el nombre ya existe (colisión en el mismo segundo).
 */
final class NombreZip {

    private NombreZip() {
    }

    /** Cualquier carácter fuera de [A-Za-z0-9_-] pasa a '_'; cualquier ".." se elimina (evita salir de la carpeta). */
    static String sanear(String serie) {
        if (serie == null || serie.isEmpty()) {
            return "SIN_SERIE";
        }
        String sinPuntos = serie.replace("..", "");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < sinPuntos.length(); i++) {
            char c = sinPuntos.charAt(i);
            boolean valido = (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')
                    || c == '_' || c == '-';
            sb.append(valido ? c : '_');
        }
        return sb.length() == 0 ? "SIN_SERIE" : sb.toString();
    }

    /** "AAAAMMDD-HHMMSS" del instante dado, hora local del dispositivo. */
    static String marcaDeTiempo(Date instante, TimeZone zona) {
        SimpleDateFormat f = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ROOT);
        f.setTimeZone(zona);
        return f.format(instante);
    }

    /**
     * @param segmentoSerie ya resuelto: {@code sanear(serie)}, {@code "SIN_SERIE"} o {@code "VARIOS"}
     *                      (decide el llamante, RF-USR-15 bis: más de un equipo o exportar sin conexión).
     */
    static String base(String segmentoSerie, Date instante, TimeZone zona) {
        return "RTVU_" + segmentoSerie + "_" + marcaDeTiempo(instante, zona);
    }

    /** Añade `_2`, `_3`... si ya existe un fichero con ese nombre en {@code carpeta} (colisión en el mismo segundo). */
    static String resolverColision(String baseSinExtension, File carpeta) {
        String candidato = baseSinExtension + ".zip";
        if (!new File(carpeta, candidato).exists()) {
            return candidato;
        }
        int n = 2;
        while (new File(carpeta, baseSinExtension + "_" + n + ".zip").exists()) {
            n++;
        }
        return baseSinExtension + "_" + n + ".zip";
    }

    /**
     * QA-4: igual que {@link #resolverColision}, pero contra un conjunto de {@code DISPLAY_NAME} ya
     * existentes en vez del sistema de ficheros — la vía {@code MediaStore.Downloads} (A4, API 29+)
     * no expone un {@link File} que se pueda mirar con {@code exists()}; el llamante (capa Android)
     * consulta el {@code ContentResolver} ANTES de insertar y le pasa lo que encontró, para que un
     * nombre repetido dé {@code _2}, {@code _3}... (SPEC-App-Usuario-V3.6.md :279-280), no el "(1)"
     * que el propio MediaStore añadiría si se le dejara resolver la colisión solo.
     */
    static String resolverColisionEntreNombres(String baseSinExtension, Set<String> nombresExistentes) {
        String candidato = baseSinExtension + ".zip";
        if (!nombresExistentes.contains(candidato)) {
            return candidato;
        }
        int n = 2;
        while (nombresExistentes.contains(baseSinExtension + "_" + n + ".zip")) {
            n++;
        }
        return baseSinExtension + "_" + n + ".zip";
    }
}
