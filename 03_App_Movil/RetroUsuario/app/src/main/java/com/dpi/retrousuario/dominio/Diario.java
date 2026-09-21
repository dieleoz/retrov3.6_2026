package com.dpi.retrousuario.dominio;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Persistencia por disparo (RF-USR-06, nuevo r6, C5): cada disparo aceptado se anota al recibirlo,
 * y la fila completa de una serie se anota **sólo cuando la serie cierra** (línea {@code FILA}). Al
 * reabrir (o, en la prueba, al construir una instancia nueva sobre el mismo fichero, T-USR-25), sólo
 * las líneas {@code FILA} cuentan como medidas guardadas: una serie que no llegó a cerrar (muerte del
 * proceso a mitad de sus disparos) no tiene línea {@code FILA} y se descarta, aunque sus disparos
 * sueltos ({@code DISPARO}) sigan en el fichero — no se completan solos ni salen como fila a medias.
 *
 * <p>Fichero de texto, un evento por línea, separador de campos {@code \u0001} (no imprimible: no
 * puede aparecer en ningún campo de {@link FilaMedida}, cuyos textos son ASCII imprimible 0x20-0x7E,
 * `calibracion_v36.c:523`) — distinto de `;`, que `serie_equipo` sí puede llevar (RF-USR-06).</p>
 */
public final class Diario {

    private static final char SEP = '\u0001';
    private static final String PREFIJO_DISPARO = "DISPARO";
    private static final String PREFIJO_FILA = "FILA";

    private final File fichero;
    private long contadorIntento = 0;

    public Diario(File fichero) {
        this.fichero = fichero;
    }

    /** Id de un intento nuevo (una serie de hasta {@code lecturasPorColor} disparos, para las líneas DISPARO). */
    public long nuevoIntento() {
        return ++contadorIntento;
    }

    public void registrarDisparo(long intentoId, int indice, int valor) {
        escribir(PREFIJO_DISPARO + SEP + intentoId + SEP + indice + SEP + valor);
    }

    public void registrarFila(FilaMedida f) {
        String[] campos = {
                f.fechaHoraIso, f.latitud, f.longitud, f.gpsEstado, f.color, String.valueOf(f.codigoBt),
                String.valueOf(f.n), f.lecturasTexto(), String.valueOf(f.media), String.valueOf(f.minimo),
                f.valido ? "1" : "0", f.motivo, f.serieEquipo, f.serieOrigen, f.mac, f.firmwareV,
                f.fechaCalibracion, f.vencimiento, f.estadoCalibracion
        };
        StringBuilder sb = new StringBuilder(PREFIJO_FILA);
        for (String c : campos) {
            sb.append(SEP).append(c == null ? "" : c);
        }
        escribir(sb.toString());
    }

    /** Sólo las series que llegaron a cerrar (línea FILA), en el orden en que se guardaron. */
    public List<FilaMedida> filasGuardadas() {
        List<FilaMedida> filas = new ArrayList<>();
        for (String linea : leerLineas()) {
            if (linea.startsWith(PREFIJO_FILA + SEP)) {
                filas.add(decodificarFila(linea));
            }
        }
        return filas;
    }

    private static FilaMedida decodificarFila(String linea) {
        String[] c = linea.split(String.valueOf(SEP), -1);
        // c[0] = "FILA"; c[1..19] = campos.
        List<Integer> lecturas = new ArrayList<>();
        for (String v : c[8].split("\\|")) {
            if (!v.isEmpty()) {
                lecturas.add(Integer.parseInt(v));
            }
        }
        return new FilaMedida(c[1], c[2], c[3], c[4], c[5], c[6].charAt(0), Integer.parseInt(c[7]), lecturas,
                Integer.parseInt(c[9]), Integer.parseInt(c[10]), "1".equals(c[11]), c[12], c[13], c[14], c[15],
                c[16], c[17], c[18], c[19]);
    }

    private void escribir(String linea) {
        try (FileWriter w = new FileWriter(fichero, true)) {
            w.write(linea);
            w.write('\n');
            w.flush();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo escribir en el diario: " + fichero, e);
        }
    }

    private List<String> leerLineas() {
        if (!fichero.exists()) {
            return new ArrayList<>();
        }
        try {
            byte[] datos = Files.readAllBytes(fichero.toPath());
            String texto = new String(datos, StandardCharsets.UTF_8);
            if (texto.isEmpty()) {
                return new ArrayList<>();
            }
            return Arrays.asList(texto.split("\n"));
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer el diario: " + fichero, e);
        }
    }
}
