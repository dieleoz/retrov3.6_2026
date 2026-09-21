package com.dpi.retrousuario.dominio;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * RF-USR-03: color de la lámina (fondo) → byte ASCII que esta app enviaría para medirlo.
 *
 * Son los caracteres ASCII {@code '1'-'6'} ({@code 0x31-0x36}), los mismos que compara el firmware
 * por texto: {@code ecuacionesColoresApp()},
 * {@code 01_Firmware/RetroVertical_V3.6.X/ecuacionesCalibracion.c:141-164}
 * ({@code strncmp(bufferData, "4", 1) == 0}, literal ASCII) — NO los códigos hex {@code 0x01-0x0E}
 * de la pantalla STONE ({@code ecuacionesCalibracion.c:73-130}, protocolo ajeno a esta app; la r1 de
 * la SPEC confundió 'b' con {@code 0x0B}, cuando el firmware compara contra el carácter {@code "b"}
 * = {@code 0x62}).
 *
 * marrón (café) usa el código del rojo, {@code '4'} (CLAUDE.md §10, RF-CAL-40): no hay ecuación
 * propia de marrón en el firmware. Nunca se resuelve un byte para negro, fluorescentes ni color
 * compuesto ({@code "a|b"}), ni para los códigos opacos {@code 7,8,a-d} de la app de empresa: esta
 * app sólo mide lámina retrorreflectiva de los 6 colores intensos (SPEC r4, cabecera "Mide").
 */
public final class MapaColor {

    private static final Map<String, Character> MAPA = crearMapa();

    private static Map<String, Character> crearMapa() {
        Map<String, Character> m = new HashMap<>();
        m.put("blanco", '1');
        m.put("amarillo", '2');
        m.put("verde", '3');
        m.put("rojo", '4');
        m.put("azul", '5');
        m.put("anaranjado", '6');
        m.put("marron", '4');
        return Collections.unmodifiableMap(m);
    }

    private MapaColor() {
    }

    /**
     * @param colorFondo nombre del color de fondo (columna {@code color_fondo} del catálogo
     *                    08_Senales/senales.csv, o el nombre que use la pantalla de esta app).
     * @return el byte ASCII a enviar, o {@code null} si ese color no es medible con este equipo
     *         (negro, fluorescentes, compuesto, o cualquier nombre fuera del mapa).
     */
    public static Character byteParaColor(String colorFondo) {
        if (colorFondo == null) {
            return null;
        }
        return MAPA.get(colorFondo);
    }

    /** true si {@link #byteParaColor(String)} resuelve un byte medible para ese color. */
    public static boolean esMedible(String colorFondo) {
        return byteParaColor(colorFondo) != null;
    }
}
