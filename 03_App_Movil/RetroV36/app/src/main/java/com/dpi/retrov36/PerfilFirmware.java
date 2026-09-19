package com.dpi.retrov36;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lo que difiere entre firmwares y es dato, no codigo (RTV 1.0, RF-APP-U09): una fila por firmware en el
 * asset firmwares.csv. Java puro. Sin fila, los valores de {@link #porDefecto}.
 *
 * Columnas: firmware,x_min,x_max,cola_banco,coherencia_def,bateria_unidad,nota
 * - coherencia_def: como se comprueba un codigo a fabrica: "G" (#G a 1 ulp contra la tabla de fabrica, V3.6)
 *   o "E" (#E contra el literal de fabrica, V4.6: su #G en DEF es equivalente, no bit a bit; RF-FW-33).
 * - bateria_unidad: "n9" (la n de la orden 9, Bateria.java) o "por_fijar" (#GB# de la V4.6: unidad sin fijar).
 */
public final class PerfilFirmware {

    public static final String ASSET = "firmwares.csv";

    public final Protocolo.Firmware firmware;
    public final double xMin;
    public final double xMax;
    /** Asset de la cola del banco; "" si no hay cola para este firmware. */
    public final String colaBanco;
    public final String coherenciaDef;
    public final String bateriaUnidad;
    public final String nota;

    PerfilFirmware(Protocolo.Firmware f, double xMin, double xMax, String cola, String coh, String bat, String nota) {
        this.firmware = f;
        this.xMin = xMin;
        this.xMax = xMax;
        this.colaBanco = cola;
        this.coherenciaDef = coh;
        this.bateriaUnidad = bat;
        this.nota = nota;
    }

    /** Valores sin CSV: los de la V3.6 para F36/F2020, y nada para el resto. */
    public static PerfilFirmware porDefecto(Protocolo.Firmware f) {
        switch (f) {
            case F36:
            case F2020:
                return new PerfilFirmware(f, 600, 4300, "cola_banco_P1-P132.csv", "G", "n9", "");
            case F46:
                return new PerfilFirmware(f, 0, 4095, "", "E", "por_fijar", "sin firmwares.csv");
            default:
                return new PerfilFirmware(f, Double.NaN, Double.NaN, "", "-", "-", "");
        }
    }

    /** Lee firmwares.csv. Una fila mal formada se ignora (y queda el valor por defecto). */
    public static Map<Protocolo.Firmware, PerfilFirmware> leer(String csv) {
        Map<Protocolo.Firmware, PerfilFirmware> m = new HashMap<>();
        if (csv == null) {
            return m;
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
            if (c.size() < 6) {
                continue;
            }
            try {
                Protocolo.Firmware f = Protocolo.Firmware.valueOf(c.get(0).trim());
                m.put(f, new PerfilFirmware(f, num(c.get(1)), num(c.get(2)), c.get(3).trim(), c.get(4).trim(),
                        c.get(5).trim(), c.size() > 6 ? c.get(6).trim() : ""));
            } catch (IllegalArgumentException e) {
                // fila de otro firmware o numero ilegible: se ignora
            }
        }
        return m;
    }

    private static double num(String s) {
        return s.trim().isEmpty() ? Double.NaN : Double.parseDouble(s.trim());
    }

    /** El perfil de f en el mapa, o el de por defecto. */
    public static PerfilFirmware de(Map<Protocolo.Firmware, PerfilFirmware> m, Protocolo.Firmware f) {
        PerfilFirmware p = m == null ? null : m.get(f);
        return p != null ? p : porDefecto(f);
    }

    /** Todos los perfiles del CSV del APK (o los de por defecto). */
    private static volatile Map<Protocolo.Firmware, PerfilFirmware> cargados = new HashMap<>();

    public static void cargar(String csv) {
        cargados = leer(csv);
    }

    public static PerfilFirmware de(Protocolo.Firmware f) {
        return de(cargados, f);
    }
}
