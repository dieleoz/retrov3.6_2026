package com.dpi.retrov36;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Decisiones de Diego que cambian lo que la app puede escribir (3.6.13; QA-3612-07, P11-M5).
 * Java puro. Se leen de assets/decisiones.csv, que viaja dentro del APK y esta versionado en git:
 * la firma es el commit que la anade, con la fecha y el documento que la sostiene.
 *
 * Formato (una decision por linea; '#' comenta):
 *   id,valor,fecha,firmante,documento,texto
 *   PA-24,SI,2026-09-19,Diego,06_Calibracion/SLV-002/DECISIONES-Diego-2026-09-19.md,"palabras de Diego"
 *
 * Una decision vale solo si valor = SI, la fecha es AAAA-MM-DD valida, el firmante nombra a Diego y
 * el documento no esta vacio. Mientras no haya una linea valida, la decision NO esta tomada: la
 * casilla del operador no la sustituye nunca.
 */
public final class Decisiones {

    public static final String ASSET = "decisiones.csv";

    public static final class Decision {
        public final String id;
        public final String fecha;
        public final String firmante;
        public final String documento;
        /** Lo que decidio, con sus palabras (puede ir vacio). */
        public final String palabras;

        Decision(String id, String fecha, String firmante, String documento, String palabras) {
            this.id = id;
            this.fecha = fecha;
            this.firmante = firmante;
            this.documento = documento;
            this.palabras = palabras == null ? "" : palabras;
        }

        public String texto() {
            return id + " decidida por " + firmante + " el " + fecha + " (" + documento + ")"
                    + (palabras.isEmpty() ? "" : ": " + palabras);
        }
    }

    private final Map<String, Decision> validas = new LinkedHashMap<>();
    private final List<String> rechazadas = new ArrayList<>();

    private Decisiones() { }

    public static Decisiones ninguna() {
        return new Decisiones();
    }

    public static Decisiones leer(String csv) {
        Decisiones d = new Decisiones();
        if (csv == null) {
            return d;
        }
        for (String l : csv.split("\r?\n")) {
            String t = l.trim();
            if (t.isEmpty() || t.startsWith("#") || t.startsWith("id,")) {
                continue;
            }
            List<String> c = Csv.partir(t);
            if (c.size() < 5) {
                d.rechazadas.add(t + " (faltan campos)");
                continue;
            }
            String id = c.get(0).trim();
            String valor = c.get(1).trim();
            String fecha = c.get(2).trim();
            String firmante = c.get(3).trim();
            String doc = c.get(4).trim();
            if (!"SI".equals(valor) || !Calibracion.fechaValida(fecha) || !firmante.toLowerCase(java.util.Locale.ROOT)
                    .contains("diego") || doc.isEmpty()) {
                d.rechazadas.add(t + " (no vale: valor SI, fecha AAAA-MM-DD, firmante Diego y documento)");
                continue;
            }
            d.validas.put(id, new Decision(id, fecha, firmante, doc, c.size() > 5 ? c.get(5).trim() : ""));
        }
        return d;
    }

    public boolean tomada(String id) {
        return validas.containsKey(id);
    }

    public Decision decision(String id) {
        return validas.get(id);
    }

    public List<String> rechazadas() {
        return rechazadas;
    }

    public String texto() {
        if (validas.isEmpty()) {
            return "ninguna decisión registrada (PA-14 y PA-24 sin decidir)";
        }
        StringBuilder sb = new StringBuilder();
        for (Decision x : validas.values()) {
            sb.append(sb.length() == 0 ? "" : "; ").append(x.texto());
        }
        return sb.toString();
    }
}
