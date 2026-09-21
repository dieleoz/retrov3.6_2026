package com.dpi.retrousuario.dominio;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * `tramas.log` (RF-USR-15 bis): una línea por trama, formato {@code t_ms;TX|RX;hex;ascii}, más
 * líneas de comentario {@code # t_ms texto} — mismo formato de columnas que ya usa la app RTV de
 * calibración (`06_Calibracion/SLV-002/tramas/rtv36_20260919_191255_HONOR.txt:1-13`, fin de línea
 * LF, sin BOM: esa app no lo lleva y RF-USR-15 bis no pide BOM para este fichero, a diferencia de
 * `medidas.csv`, que sí lo exige explícitamente). Acumula en memoria durante toda la sesión
 * (RF-USR-15 bis: "abarca todas las sesiones registradas hasta el momento de exportar").
 */
public final class RegistroTramas {

    private final List<String> lineas = new ArrayList<>();

    public RegistroTramas() {
        lineas.add("# RTV Usuario V3.6 - registro de tramas Bluetooth");
        lineas.add("# formato: t_ms;TX|RX;hex;ascii  (t_ms desde la apertura; en ascii, '.' = no imprimible o ';')");
    }

    public void tx(long tMs, String texto) {
        lineas.add(linea(tMs, "TX", texto));
    }

    public void rx(long tMs, String texto) {
        lineas.add(linea(tMs, "RX", texto));
    }

    /** RF-USR-16: un disparo anulado por plazo (`ANULADO`) o un byte descartado en cuarentena (`DESCARTADO_EN_CUARENTENA`). */
    public void comentario(long tMs, String texto) {
        lineas.add("# " + tMs + " " + texto);
    }

    private static String linea(long tMs, String direccion, String texto) {
        return tMs + ";" + direccion + ";" + hex(texto) + ";" + ascii(texto);
    }

    /** Expuesto para que las pruebas comparen contra el mismo formato hex que usa este registro (T-USR-19a(c)). */
    public static String hexDe(String texto) {
        return hex(texto);
    }

    private static String hex(String texto) {
        StringBuilder sb = new StringBuilder();
        byte[] bytes = texto.getBytes(StandardCharsets.US_ASCII);
        for (int i = 0; i < bytes.length; i++) {
            if (i > 0) {
                sb.append(' ');
            }
            sb.append(String.format("%02X", bytes[i]));
        }
        return sb.toString();
    }

    private static String ascii(String texto) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            sb.append(c >= 0x20 && c <= 0x7E && c != ';' ? c : '.');
        }
        return sb.toString();
    }

    /** Sólo las líneas TX, en el mismo orden: proyección usada por T-USR-19a(c) contra el registro del simulador. */
    public List<String> lineasTx() {
        List<String> out = new ArrayList<>();
        for (String l : lineas) {
            String[] campos = l.split(";", -1);
            if (campos.length >= 3 && "TX".equals(campos[1])) {
                out.add(campos[2]);
            }
        }
        return out;
    }

    public byte[] aBytes() {
        StringBuilder sb = new StringBuilder();
        for (String l : lineas) {
            sb.append(l).append('\n');
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    public List<String> lineas() {
        return new ArrayList<>(lineas);
    }
}
