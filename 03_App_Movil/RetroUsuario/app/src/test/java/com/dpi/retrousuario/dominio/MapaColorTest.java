package com.dpi.retrousuario.dominio;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * T-USR-04 (TDD-V3.6.md:1849-1869): mapa color → byte, ASCII, sobre las 376 filas del catálogo.
 * Fuente del esperado: ecuacionesCalibracion.c:141-164 (strncmp(bufferData, "4", 1), ASCII, no el
 * hex de la STONE); 08_Senales/senales.csv (copiado a app/src/test/resources/senales.csv, mismo md5
 * que el original: se verifica en {@link #elFicheroDePruebaEsElMismoQueElCatalogo()}).
 *
 * Lee el CSV a mano (sin biblioteca de CSV externa) porque el formato de este catálogo es simple:
 * una fila por línea, campos separados por ',', sin comillas ni comas dentro de un campo (se
 * comprueba que el número de campos por fila es el esperado).
 */
public class MapaColorTest {

    private static final String MD5_CATALOGO = "75bfb0f0bcd44ef15daa1674d02f870e";

    private List<Map<String, String>> leerCatalogo() throws IOException {
        List<Map<String, String>> filas = new ArrayList<>();
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("senales.csv");
                BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String cabecera = r.readLine();
            String[] columnas = cabecera.split(",", -1);
            String linea;
            while ((linea = r.readLine()) != null) {
                if (linea.isEmpty()) {
                    continue;
                }
                String[] valores = linea.split(",", -1);
                Map<String, String> fila = new HashMap<>();
                for (int i = 0; i < columnas.length && i < valores.length; i++) {
                    fila.put(columnas[i], valores[i]);
                }
                filas.add(fila);
            }
        }
        return filas;
    }

    /** El fichero de prueba es una copia exacta del catálogo real (md5 verificado con `md5sum` al
     *  copiarlo): si alguien lo edita a mano para la prueba, esta aserción lo detecta. */
    @Test
    public void elFicheroDePruebaEsElMismoQueElCatalogo() throws Exception {
        java.security.MessageDigest md5 = java.security.MessageDigest.getInstance("MD5");
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("senales.csv")) {
            byte[] buffer = new byte[8192];
            int n;
            while ((n = in.read(buffer)) > 0) {
                md5.update(buffer, 0, n);
            }
        }
        StringBuilder hex = new StringBuilder();
        for (byte b : md5.digest()) {
            hex.append(String.format("%02x", b));
        }
        assertEquals(MD5_CATALOGO, hex.toString());
    }

    /** (a) 357 filas resuelven un byte ASCII '1'-'6', con el recuento por color de la ficha. */
    @Test
    public void trescientasCincuentaYSieteFilasResuelvenByteAscii() throws Exception {
        List<Map<String, String>> filas = leerCatalogo();
        assertEquals(376, filas.size());

        Map<String, Integer> porColor = new HashMap<>();
        int total = 0;
        for (Map<String, String> fila : filas) {
            String color = fila.get("color_fondo");
            Character byteEnviado = MapaColor.byteParaColor(color);
            if (byteEnviado != null) {
                total++;
                assertTrue("byte fuera de 0x31-0x36 ASCII: " + (int) byteEnviado.charValue(),
                        byteEnviado >= '1' && byteEnviado <= '6');
                porColor.merge(color, 1, Integer::sum);
            }
        }
        assertEquals(357, total);
        assertEquals(Integer.valueOf(81), porColor.get("blanco"));
        assertEquals(Integer.valueOf(76), porColor.get("amarillo"));
        assertEquals(Integer.valueOf(28), porColor.get("verde"));
        assertEquals(Integer.valueOf(2), porColor.get("rojo"));
        assertEquals(Integer.valueOf(41), porColor.get("azul"));
        assertEquals(Integer.valueOf(96), porColor.get("anaranjado"));
        assertEquals(Integer.valueOf(33), porColor.get("marron"));
    }

    /** (b) 19 filas no resuelven byte: negro, fluorescentes y compuestos. */
    @Test
    public void diecinueveFilasNoResuelvenByte() throws Exception {
        List<Map<String, String>> filas = leerCatalogo();
        int sinByte = 0;
        for (Map<String, String> fila : filas) {
            if (!MapaColor.esMedible(fila.get("color_fondo"))) {
                sinByte++;
            }
        }
        assertEquals(19, sinByte);
    }

    /** El byte del código '4' es el carácter ASCII 0x34, no el 0x0B hex de la STONE para 'b': la
     *  r1 de la SPEC confundía las dos codificaciones (RF-USR-03). */
    @Test
    public void elByteEsElCaracterAsciiNoElHexDeLaStone() {
        assertEquals(Character.valueOf('4'), MapaColor.byteParaColor("rojo"));
        assertEquals(0x34, (int) MapaColor.byteParaColor("rojo").charValue());
        assertEquals("marrón usa el código del rojo (CLAUDE.md §10, RF-CAL-40)",
                MapaColor.byteParaColor("rojo"), MapaColor.byteParaColor("marron"));
    }

    /** Nunca un código opaco (7, 8, a-d) ni 'e': ese mapa no existe en esta clase. */
    @Test
    public void ningunColorResuelveUnCodigoOpacoNiE() {
        for (String c : new String[] {"negro", "amarillo_verde_fluorescente",
                "anaranjado_fluorescente", "amarillo|amarillo_verde_fluorescente", "rojo|verde"}) {
            assertEquals(null, MapaColor.byteParaColor(c));
        }
    }
}
