package com.dpi.retrousuario.dominio;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * `tramas.log` (RF-USR-15 bis): una línea por trama, formato {@code t_ms;TX|RX;hex;ascii}, más
 * líneas de comentario {@code # t_ms texto} — mismo formato de columnas que ya usa la app RTV de
 * calibración (`06_Calibracion/SLV-002/tramas/rtv36_20260919_191255_HONOR.txt:1-13`, fin de línea
 * LF, sin BOM: esa app no lo lleva y RF-USR-15 bis no pide BOM para este fichero, a diferencia de
 * `medidas.csv`, que sí lo exige explícitamente).
 *
 * <p><b>M2, corrige un borrador anterior:</b> "abarca todas las sesiones registradas hasta el
 * momento de exportar" (RF-USR-15 bis) exige que sobreviva a que el proceso muera a mitad — igual
 * que {@link Diario} — no sólo a que la Activity se recree dentro del mismo proceso. Por eso este
 * registro admite un fichero de respaldo ({@link #RegistroTramas(File)}, mismo patrón de escritura
 * por línea, con flush inmediato, que {@link Diario}): cada evento se aplica al fichero al momento,
 * y {@link #lineas()}/{@link #aBytes()} lo releen entero, así que una `RegistroTramas` nueva sobre el
 * MISMO fichero (una Activity recreada, o el proceso relanzado) sigue viendo lo que ya había,
 * acumulado hasta que se exporte — exportar no lo borra (T-USR-27 aplica el mismo principio a
 * `medidas.csv`, y aquí se extiende a `tramas.log`). El constructor sin fichero ({@link
 * #RegistroTramas()}) sigue en memoria pura, para las pruebas del dominio que no necesitan disco.</p>
 */
public final class RegistroTramas {

    private static final String CABECERA_1 = "# RTV Usuario V3.6 - registro de tramas Bluetooth";
    private static final String CABECERA_2 =
            "# formato: t_ms;TX|RX;hex;ascii  (t_ms desde la apertura; en ascii, '.' = no imprimible o ';')";

    private final File fichero; // null: sólo memoria (pruebas del dominio, sin tocar disco).
    private final List<String> memoria;

    public RegistroTramas() {
        this(null);
    }

    /** M2: mismo fichero entre Activities/procesos (mismo path, p. ej. {@code getFilesDir()}): se acumula hasta exportar. */
    public RegistroTramas(File fichero) {
        this.fichero = fichero;
        if (fichero == null) {
            memoria = new ArrayList<>();
            memoria.add(CABECERA_1);
            memoria.add(CABECERA_2);
        } else {
            memoria = null;
            if (!fichero.exists() || fichero.length() == 0) {
                escribir(CABECERA_1);
                escribir(CABECERA_2);
            }
        }
    }

    public void tx(long tMs, String texto) {
        escribir(linea(tMs, "TX", texto));
    }

    public void rx(long tMs, String texto) {
        escribir(linea(tMs, "RX", texto));
    }

    /** RF-USR-16: un disparo anulado por plazo (`ANULADO`) o un byte descartado en cuarentena (`DESCARTADO_EN_CUARENTENA`). */
    public void comentario(long tMs, String texto) {
        escribir("# " + tMs + " " + texto);
    }

    private void escribir(String linea) {
        if (fichero == null) {
            memoria.add(linea);
            return;
        }
        try (FileWriter w = new FileWriter(fichero, true)) {
            w.write(linea);
            w.write('\n');
            w.flush();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo escribir en tramas.log: " + fichero, e);
        }
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
        for (String l : lineas()) {
            String[] campos = l.split(";", -1);
            if (campos.length >= 3 && "TX".equals(campos[1])) {
                out.add(campos[2]);
            }
        }
        return out;
    }

    public byte[] aBytes() {
        StringBuilder sb = new StringBuilder();
        for (String l : lineas()) {
            sb.append(l).append('\n');
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    /** M2: si hay fichero de respaldo, lo relee entero (acumulado entre instancias); si no, la copia en memoria. */
    public List<String> lineas() {
        if (fichero == null) {
            return new ArrayList<>(memoria);
        }
        if (!fichero.exists()) {
            return new ArrayList<>();
        }
        try {
            byte[] datos = Files.readAllBytes(fichero.toPath());
            String texto = new String(datos, StandardCharsets.UTF_8);
            List<String> out = new ArrayList<>();
            if (texto.isEmpty()) {
                return out;
            }
            for (String l : texto.split("\n", -1)) {
                if (!l.isEmpty()) {
                    out.add(l);
                }
            }
            return out;
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer tramas.log: " + fichero, e);
        }
    }
}
