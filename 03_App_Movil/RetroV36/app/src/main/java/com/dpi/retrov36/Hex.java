package com.dpi.retrov36;

import java.io.ByteArrayOutputStream;

/** Conversiones de bytes a texto legible para el registro y la pantalla. */
public final class Hex {

    private static final char[] DIGITOS = "0123456789ABCDEF".toCharArray();

    private Hex() { }

    /** "3A 3A 31 32". Vacio si no hay bytes. */
    public static String hex(byte[] b) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < b.length; i++) {
            if (i > 0) {
                sb.append(' ');
            }
            int v = b[i] & 0xFF;
            sb.append(DIGITOS[v >> 4]).append(DIGITOS[v & 0x0F]);
        }
        return sb.toString();
    }

    /** "0x3A". */
    public static String byteHex(int v) {
        return "0x" + DIGITOS[(v >> 4) & 0x0F] + DIGITOS[v & 0x0F];
    }

    /**
     * ASCII imprimible (0x20-0x7E) tal cual; el resto como '.'.
     * El ';' tambien sale como '.', porque es el separador de campos del
     * registro. El dato fiel es siempre la columna hex.
     */
    public static String ascii(byte[] b) {
        StringBuilder sb = new StringBuilder();
        for (byte x : b) {
            int v = x & 0xFF;
            sb.append((v >= 0x20 && v <= 0x7E && v != ';') ? (char) v : '.');
        }
        return sb.toString();
    }

    /**
     * Interpreta "65 0D", "650D" o "0x65,0x0d" como bytes.
     *
     * @throws IllegalArgumentException si hay un digito no hexadecimal o un
     *                                  numero impar de digitos.
     */
    public static byte[] parsear(String texto) {
        String limpio = texto.replaceAll("(?i)0x", "").replaceAll("[\\s,;:]", "");
        if (limpio.isEmpty() || (limpio.length() % 2) != 0) {
            throw new IllegalArgumentException("Hace falta un número par de dígitos hex.");
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (int i = 0; i < limpio.length(); i += 2) {
            int alto = Character.digit(limpio.charAt(i), 16);
            int bajo = Character.digit(limpio.charAt(i + 1), 16);
            if (alto < 0 || bajo < 0) {
                throw new IllegalArgumentException("Dígito no hexadecimal en: "
                        + limpio.substring(i, i + 2));
            }
            out.write((alto << 4) | bajo);
        }
        return out.toByteArray();
    }

    /**
     * La app no guarda el PIN (RF-APP-19): en "#L,...#" y "#P,...#" los
     * digitos se sustituyen por '*' antes de escribir el registro.
     */
    public static byte[] ocultarPin(byte[] datos) {
        if (datos.length >= 3 && datos[0] == '#' && (datos[1] == 'L' || datos[1] == 'P') && datos[2] == ',') {
            byte[] c = datos.clone();
            for (int i = 3; i < c.length; i++) {
                if (c[i] >= '0' && c[i] <= '9') {
                    c[i] = '*';
                }
            }
            return c;
        }
        return datos;
    }
}
