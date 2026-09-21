package com.dpi.retrousuario.dominio;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

/**
 * `medidas.csv` (RF-USR-06, M-6): separador `;`, decimal `,`, BOM `EF BB BF`, fin de línea CRLF,
 * una sola regla de comillas RFC 4180 (comillas si el campo lleva `;`, `"`, CR o LF; comilla interna
 * duplicada), aplicada a **cualquier** columna. `firmware_v` lleva comas pero nunca `;`/`"`/CR/LF,
 * así que nunca se cita (corrige r5, que la citaba "por las comas internas", SPEC r6 RF-USR-06).
 */
public final class CsvMedidas {

    static final byte[] BOM = { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF };

    static final String CABECERA = "fecha_hora;latitud;longitud;gps_estado;color;codigo_bt;n;lecturas;media;"
            + "minimo;valido;motivo;serie_equipo;serie_origen;mac;firmware_v;fecha_calibracion;vencimiento;"
            + "estado_calibracion";

    private CsvMedidas() {
    }

    public static byte[] generar(List<FilaMedida> filas) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            out.write(BOM);
            escribirLinea(out, CABECERA);
            for (FilaMedida f : filas) {
                escribirLinea(out, columnas(f));
            }
        } catch (IOException imposibleEnMemoria) {
            throw new IllegalStateException(imposibleEnMemoria);
        }
        return out.toByteArray();
    }

    private static String columnas(FilaMedida f) {
        String[] campos = {
                f.fechaHoraIso, f.latitud, f.longitud, f.gpsEstado, f.color, String.valueOf(f.codigoBt),
                String.valueOf(f.n), f.lecturasTexto(), String.valueOf(f.media), String.valueOf(f.minimo),
                f.valido ? "SI" : "NO", f.motivo, f.serieEquipo, f.serieOrigen, f.mac, f.firmwareV,
                f.fechaCalibracion, f.vencimiento, f.estadoCalibracion
        };
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < campos.length; i++) {
            if (i > 0) {
                sb.append(';');
            }
            sb.append(citar(campos[i]));
        }
        return sb.toString();
    }

    /** Regla única de comillas RFC 4180, aplicada a cualquier columna (corrige r5, C2). */
    static String citar(String campo) {
        if (campo == null) {
            return "";
        }
        boolean necesita = campo.indexOf(';') >= 0 || campo.indexOf('"') >= 0
                || campo.indexOf('\r') >= 0 || campo.indexOf('\n') >= 0;
        if (!necesita) {
            return campo;
        }
        return '"' + campo.replace("\"", "\"\"") + '"';
    }

    /** `latitud`/`longitud`: 6 decimales fijos, decimal `,` (M-6). */
    public static String formatearCoordenada(double valor) {
        return String.format(Locale.ROOT, "%.6f", valor).replace('.', ',');
    }

    private static void escribirLinea(ByteArrayOutputStream out, String linea) throws IOException {
        out.write(linea.getBytes(StandardCharsets.UTF_8));
        out.write('\r');
        out.write('\n');
    }
}
