package com.dpi.retrov36;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Perfiles por MAC (RTV 1.0, RF-APP-U02 con la condicion A-3 de la revision P2). Java puro.
 *
 * Dos fuentes:
 * <ul>
 * <li><b>Firmados</b>, del asset equipos.csv (mismo regimen que decisiones.csv: fecha, firmante Diego y
 *     documento). Columnas: mac,perfil,variante,serie_declarada,caduca_con_v,fecha,firmante,documento,nota.
 *     perfil = SOLO_SONDA (a ese equipo solo se le envia la sonda: firmware sin fuente, como el V3-2), o el
 *     firmware esperado (F36, F46, V4_ORIGINAL, F2020), que la deteccion CONFIRMA. caduca_con_v: la version de
 *     #V# con la que el perfil deja de aplicarse (p. ej. "4.6" cuando el equipo se regrabe): el perfil caduca
 *     en vez de contradecir.</li>
 * <li><b>Aprendidos</b>, lo que la app vio en la ultima deteccion de cada MAC (fichero local, no firmado): el
 *     firmware y la huella de #V# (version y fecha de compilacion). Si #V# cambia, el aprendido caduca y se
 *     anota (A-3: "el perfil fija la huella del firmware y deja de aplicarse cuando cambia").</li>
 * </ul>
 */
public final class PerfilesEquipo {

    public static final String ASSET = "equipos.csv";
    public static final String SOLO_SONDA = "SOLO_SONDA";
    private static final Pattern FECHA = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");

    private PerfilesEquipo() { }

    /** Un perfil firmado de equipos.csv. */
    public static final class Perfil {
        public final String mac;
        /** SOLO_SONDA o el nombre de un Protocolo.Firmware. */
        public final String perfil;
        public final String variante;
        public final String serieDeclarada;
        public final String caducaConV;
        public final String documento;

        Perfil(String mac, String perfil, String variante, String serie, String caduca, String doc) {
            this.mac = mac;
            this.perfil = perfil;
            this.variante = variante;
            this.serieDeclarada = serie;
            this.caducaConV = caduca;
            this.documento = doc;
        }

        public boolean soloSonda() {
            return SOLO_SONDA.equals(perfil);
        }

        /** Firmware esperado; null si es SOLO_SONDA (se espera un V4 original) o no se reconoce. */
        public Protocolo.Firmware esperado() {
            if (soloSonda()) {
                return Protocolo.Firmware.V4_ORIGINAL;
            }
            try {
                return Protocolo.Firmware.valueOf(perfil);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }

    /** Resultado de leer equipos.csv: los validos por MAC y los rechazados con su motivo. */
    public static final class Firmados {
        public final Map<String, Perfil> porMac = new LinkedHashMap<>();
        public final List<String> rechazados = new ArrayList<>();

        public Perfil de(String mac) {
            return mac == null ? null : porMac.get(mac.trim().toUpperCase(Locale.ROOT));
        }
    }

    public static Firmados leerFirmados(String csv) {
        Firmados f = new Firmados();
        if (csv == null) {
            return f;
        }
        boolean cabecera = true;
        for (String l : csv.split("\r?\n")) {
            if (l.trim().isEmpty() || l.startsWith("#")) {
                continue;
            }
            if (cabecera) {
                cabecera = false;
                continue;
            }
            List<String> c = Csv.partir(l);
            if (c.size() < 8) {
                f.rechazados.add(l + " (faltan columnas)");
                continue;
            }
            String mac = c.get(0).trim().toUpperCase(Locale.ROOT);
            String perfil = c.get(1).trim();
            if (!FECHA.matcher(c.get(5).trim()).matches() || !"Diego".equalsIgnoreCase(c.get(6).trim())
                    || c.get(7).trim().isEmpty()) {
                f.rechazados.add(mac + " (sin fecha, firma de Diego o documento)");
                continue;
            }
            Perfil p = new Perfil(mac, perfil, c.get(2).trim(), c.get(3).trim(), c.get(4).trim(), c.get(7).trim());
            if (!p.soloSonda() && p.esperado() == null) {
                f.rechazados.add(mac + " (perfil " + perfil + " no reconocido)");
                continue;
            }
            f.porMac.put(mac, p);
        }
        return f;
    }

    /** Lo aprendido de un equipo en su ultima deteccion. */
    public static final class Aprendido {
        public final String mac;
        public final Protocolo.Firmware firmware;
        /** "V,<version>,<fecha>" si respondio a #V#; "9", "6" o "SONDA" si no. */
        public final String huella;
        public final String fecha;

        public Aprendido(String mac, Protocolo.Firmware firmware, String huella, String fecha) {
            this.mac = mac.trim().toUpperCase(Locale.ROOT);
            this.firmware = firmware;
            this.huella = huella;
            this.fecha = fecha;
        }

        public String linea() {
            return mac + ";" + firmware.name() + ";" + huella + ";" + fecha;
        }
    }

    /** Fichero de aprendidos: una linea "mac;firmware;huella;fecha" por equipo (la ultima manda). */
    public static Map<String, Aprendido> leerAprendidos(String texto) {
        Map<String, Aprendido> m = new LinkedHashMap<>();
        if (texto == null) {
            return m;
        }
        for (String l : texto.split("\r?\n")) {
            String[] c = l.split(";", -1);
            if (c.length != 4) {
                continue;
            }
            try {
                Aprendido a = new Aprendido(c[0], Protocolo.Firmware.valueOf(c[1]), c[2], c[3]);
                m.put(a.mac, a);
            } catch (IllegalArgumentException e) {
                // linea ilegible: se ignora
            }
        }
        return m;
    }

    public static String escribirAprendidos(Map<String, Aprendido> m) {
        StringBuilder sb = new StringBuilder();
        for (Aprendido a : m.values()) {
            sb.append(a.linea()).append('\n');
        }
        return sb.toString();
    }
}
