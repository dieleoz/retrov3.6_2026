package com.dpi.retrov36;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * ZIP incremental y ZIP de soporte (3.6.15; PLAN-Banco-Representativo.md §7, RF-APP-50 a 53). Java puro.
 *
 * - Incremental: solo lo nuevo desde la exportacion anterior del mismo equipo. Una pieza nueva o cambiada va
 *   entera; una que solo crecio (el diario, que es de solo anadir) va como "nombre.desde_<byte>" con el tramo
 *   nuevo. Lleva "indice.sha256": SHA-256, tamano y byte de inicio de cada pieza completa, y el nombre y el
 *   SHA-256 del ZIP anterior. resumen.txt va siempre, con el md5 y el SHA-256 de cada pieza (RF-APP-52).
 * - Soporte: todo, entero, y los indices de todos los incrementales (RF-APP-51).
 * - DEFLATE nivel 9 (RF-APP-53).
 */
public final class PaquetesZip {

    private PaquetesZip() { }

    public static final class Pieza {
        public final String nombre;
        public final byte[] datos;

        public Pieza(String nombre, byte[] datos) {
            this.nombre = nombre;
            this.datos = datos;
        }
    }

    /** Lo que se sabe de una pieza en la exportacion anterior. */
    public static final class Entrada {
        public final long tamano;
        public final String sha256;

        Entrada(long tamano, String sha256) {
            this.tamano = tamano;
            this.sha256 = sha256;
        }
    }

    public static final class Incremental {
        /** Piezas que van en el ZIP (enteras o tramos), sin resumen.txt ni indice.sha256. */
        public final List<Pieza> piezas = new ArrayList<>();
        /** Texto de indice.sha256. */
        public String indice;
        /** Estado para la exportacion siguiente. */
        public final Map<String, Entrada> estado = new LinkedHashMap<>();
    }

    public static String sha256(byte[] b) {
        return hex(b, 0, b.length, "SHA-256");
    }

    public static String md5(byte[] b) {
        return hex(b, 0, b.length, "MD5");
    }

    static String hex(byte[] b, int desde, int n, String alg) {
        try {
            MessageDigest d = MessageDigest.getInstance(alg);
            d.update(b, desde, n);
            StringBuilder sb = new StringBuilder();
            for (byte x : d.digest()) {
                sb.append(String.format(Locale.US, "%02x", x));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Calcula el incremental. anterior: estado de la exportacion anterior (vacio si es la primera);
     * zipAnterior y shaZipAnterior: nombre y SHA-256 del ZIP anterior ("" si ninguno).
     */
    public static Incremental incremental(List<Pieza> actuales, Map<String, Entrada> anterior, String zipAnterior,
                                          String shaZipAnterior) {
        Incremental r = new Incremental();
        StringBuilder ind = new StringBuilder("# indice.sha256 (RF-APP-50): sha256  tamano  inicio  nombre\n");
        ind.append("anterior ").append(zipAnterior == null || zipAnterior.isEmpty() ? "-" : zipAnterior).append(' ')
                .append(shaZipAnterior == null || shaZipAnterior.isEmpty() ? "-" : shaZipAnterior).append('\n');
        for (Pieza p : actuales) {
            String sha = sha256(p.datos);
            r.estado.put(p.nombre, new Entrada(p.datos.length, sha));
            Entrada a = anterior.get(p.nombre);
            if (a != null && a.sha256.equals(sha) && a.tamano == p.datos.length) {
                continue;                                    // sin cambios: no va
            }
            long inicio = 0;
            if (a != null && p.datos.length > a.tamano
                    && hex(p.datos, 0, (int) a.tamano, "SHA-256").equals(a.sha256)) {
                inicio = a.tamano;                           // solo crecio: va el tramo nuevo
                byte[] t = new byte[(int) (p.datos.length - inicio)];
                System.arraycopy(p.datos, (int) inicio, t, 0, t.length);
                r.piezas.add(new Pieza(p.nombre + ".desde_" + inicio, t));
            } else {
                r.piezas.add(p);
            }
            ind.append(sha).append("  ").append(p.datos.length).append("  ").append(inicio).append("  ")
                    .append(p.nombre).append('\n');
        }
        r.indice = ind.toString();
        return r;
    }

    /** Lista de piezas con su md5 y SHA-256, para resumen.txt (RF-APP-52). */
    public static String listaHashes(List<Pieza> piezas) {
        StringBuilder sb = new StringBuilder("\nPiezas de este ZIP (md5, SHA-256, bytes, nombre):\n");
        for (Pieza p : piezas) {
            sb.append(String.format(Locale.US, "  %s  %s  %d  %s\n", md5(p.datos), sha256(p.datos), p.datos.length,
                    p.nombre));
        }
        return sb.toString();
    }

    /** Escribe un ZIP con DEFLATE nivel 9 (RF-APP-53). */
    public static void escribir(OutputStream out, List<Pieza> piezas) throws IOException {
        try (ZipOutputStream z = new ZipOutputStream(out)) {
            z.setLevel(9);
            for (Pieza p : piezas) {
                z.putNextEntry(new ZipEntry(p.nombre));
                z.write(p.datos);
                z.closeEntry();
            }
        }
    }

    public static byte[] zip(List<Pieza> piezas) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        escribir(b, piezas);
        return b.toByteArray();
    }

    /**
     * Reconstruye (como el script de RF-APP-50): aplica un incremental sobre lo reconstruido hasta ahora y
     * comprueba cada SHA-256 del indice. Un tramo que no empieza donde acaba lo reconstruido dice que falta
     * un incremental intermedio.
     */
    public static void aplicar(Map<String, byte[]> reconstruido, List<Pieza> incremental, String indice) {
        for (Pieza p : incremental) {
            int d = p.nombre.lastIndexOf(".desde_");
            if (d < 0) {
                reconstruido.put(p.nombre, p.datos);
                continue;
            }
            String base = p.nombre.substring(0, d);
            long inicio = Long.parseLong(p.nombre.substring(d + ".desde_".length()));
            byte[] ya = reconstruido.get(base);
            if (ya == null || ya.length != inicio) {
                throw new IllegalStateException("falta un incremental anterior: " + base + " tiene "
                        + (ya == null ? 0 : ya.length) + " bytes y el tramo empieza en " + inicio);
            }
            byte[] n = new byte[ya.length + p.datos.length];
            System.arraycopy(ya, 0, n, 0, ya.length);
            System.arraycopy(p.datos, 0, n, ya.length, p.datos.length);
            reconstruido.put(base, n);
        }
        for (String l : indice.split("\n")) {
            if (l.startsWith("#") || l.startsWith("anterior") || l.trim().isEmpty()) {
                continue;
            }
            String[] c = l.trim().split("\\s+", 4);
            byte[] b = reconstruido.get(c[3]);
            if (b == null || !sha256(b).equals(c[0])) {
                throw new IllegalStateException("el SHA-256 de " + c[3] + " no coincide con el índice");
            }
        }
    }

    /** Estado guardado entre exportaciones: una linea "sha256 tamano nombre" por pieza. */
    public static String estadoTexto(Map<String, Entrada> e) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Entrada> x : e.entrySet()) {
            sb.append(x.getValue().sha256).append(' ').append(x.getValue().tamano).append(' ').append(x.getKey()).append('\n');
        }
        return sb.toString();
    }

    public static Map<String, Entrada> leerEstado(String t) {
        Map<String, Entrada> m = new LinkedHashMap<>();
        if (t != null) {
            for (String l : t.split("\n")) {
                String[] c = l.trim().split(" ", 3);
                if (c.length == 3) {
                    m.put(c[2], new Entrada(Long.parseLong(c[1]), c[0]));
                }
            }
        }
        return m;
    }

    static byte[] utf8(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }
}
