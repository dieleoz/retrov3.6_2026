package com.dpi.retrov36;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Cola del banco P1-P132 (RF-APP-33, RF-CAL-35). Java puro.
 *
 * - Se carga de assets/cola_banco_P1-P132.csv SOLO si su md5 esta en MD5_PERMITIDOS;
 *   si no, "Cola no admitida" y ningun paso.
 * - K, M y asentamiento salen de cada fila, no son constantes (RF-APP-33, P10 A2).
 * - P10-C3: K = 5 en P81 y en TODOS los pasos OSCURO. La cola 9ddb7882 ya lo trae; la app
 *   lo comprueba igualmente y, si una fila no lo cumple, lo fuerza y lo dice en el paso.
 * - El codigo de cada patron sale de la columna codigo_equipo (RF-APP-47): cafe y lila,
 *   codigo 4 y VERIFICACION.
 * - OSCURO, A5, BATERIA, CALENTAMIENTO y EXPORTAR son pasos de control: no se saltan.
 */
public final class BancoCola {

    /**
     * md5 de las colas admitidas (06_Calibracion/cola_banco_P1-P132.csv, blob LF). Solo la de
     * 1f1c4e3 (todos los OSCURO, P81 y los tres del b a K = 5, P10-C3). La 5ba94658 ya no se admite.
     */
    public static final List<String> MD5_PERMITIDOS = Collections.unmodifiableList(
            Arrays.asList("9ddb7882fa6c32c50c90fcdd72ba8960"));
    public static final String ASSET = "cola_banco_P1-P132.csv";
    /** P10-C3. */
    public static final int K_FORZADO = 5;

    public static final class Paso {
        public final int orden;
        public final int sesion;
        public final String bloque;
        /** CALENTAMIENTO, BATERIA, OSCURO, A5, PATRON, PAUSA o EXPORTAR. */
        public final String tipo;
        public final String patron;
        public final String color;
        public final String tipoLamina;
        public final double certificado;
        public final String codigo;
        /** AJUSTE, RE-MEDIDA, VERIFICACION o CONTROL. */
        public final String uso;
        public final int k;
        public final int m;
        public final int asentamiento;
        public final String remedidaDe;
        /** NaN si la cola no la trae. */
        public final double xEsperada;
        public final String nota;
        /** Aviso de la app sobre la fila (p. ej., K forzado por P10-C3). */
        public final String ajusteApp;

        Paso(int orden, int sesion, String bloque, String tipo, String patron, String color, String tipoLamina,
             double certificado, String codigo, String uso, int k, int m, int asentamiento, String remedidaDe,
             double xEsperada, String nota, String ajusteApp) {
            this.orden = orden;
            this.sesion = sesion;
            this.bloque = bloque;
            this.tipo = tipo;
            this.patron = patron;
            this.color = color;
            this.tipoLamina = tipoLamina;
            this.certificado = certificado;
            this.codigo = codigo;
            this.uso = uso;
            this.k = k;
            this.m = m;
            this.asentamiento = asentamiento;
            this.remedidaDe = remedidaDe;
            this.xEsperada = xEsperada;
            this.nota = nota;
            this.ajusteApp = ajusteApp;
        }

        public boolean esMedida() {
            return "OSCURO".equals(tipo) || "A5".equals(tipo) || "PATRON".equals(tipo);
        }

        /** Solo los patrones se pueden saltar (RF-APP-33). */
        public boolean saltable() {
            return "PATRON".equals(tipo);
        }

        /** Disparos con 'e' que cuesta el paso: K (M + asentamiento). */
        public int disparos() {
            return esMedida() ? k * (m + asentamiento) : 0;
        }

        public String instruccion() {
            switch (tipo) {
                case "CALENTAMIENTO":
                    return "Encienda el equipo y espere 10 min antes de la primera serie. Pulse OK cuando hayan pasado.";
                case "BATERIA":
                    return "Batería: la app envía la orden 9 (el equipo pita). Pulse OK.";
                case "EXPORTAR":
                    return "Fin de la sesión " + sesion + ": exportar el ZIP (y copia en Download/RTV/).";
                case "OSCURO":
                    return "Coloque la superficie OSCURO (negra mate) y pulse OK";
                case "PAUSA":
                    return "Pausa: " + (nota.isEmpty() ? "fin de la sesión " + sesion : nota) + ". Pulse OK para seguir.";
                default:
                    return String.format(java.util.Locale.US, "Coloque %s (%s %s, cert. %s) y pulse OK",
                            patron, color, tipoLamina, Campana.fmt(certificado));
            }
        }
    }

    public final List<Paso> pasos;
    public final String md5;

    private BancoCola(List<Paso> pasos, String md5) {
        this.pasos = pasos;
        this.md5 = md5;
    }

    /** @throws IllegalArgumentException "Cola no admitida" si el md5 no esta permitido o el formato no cuadra. */
    public static BancoCola cargar(byte[] csv) throws java.io.IOException {
        String md5 = Resumen.hex(csv, "MD5");
        if (!MD5_PERMITIDOS.contains(md5)) {
            throw new IllegalArgumentException("Cola no admitida (md5 " + md5 + ")");
        }
        List<Paso> l = new ArrayList<>();
        Map<String, Integer> col = null;
        for (String linea : ImportadorCampana.lineas(csv)) {
            if (linea.trim().isEmpty()) {
                continue;
            }
            List<String> f = Csv.partir(linea);
            if (col == null) {
                col = new LinkedHashMap<>();
                for (int i = 0; i < f.size(); i++) {
                    col.put(f.get(i), i);
                }
                continue;
            }
            String tipo = c(f, col, "paso");
            String patron = c(f, col, "patron");
            int k = entero(c(f, col, "K"), 0);
            String ajuste = "";
            if (("OSCURO".equals(tipo) || "P81".equals(patron)) && k != K_FORZADO && k > 0) {
                ajuste = "K = " + K_FORZADO + " por P10-C3 (la cola dice " + k + ")";
                k = K_FORZADO;
            }
            String xs = c(f, col, "x_esperada");
            String cert = c(f, col, "valor_certificado");
            l.add(new Paso(entero(c(f, col, "orden"), l.size() + 1), entero(c(f, col, "sesion"), 0), c(f, col, "bloque"),
                    tipo, patron, c(f, col, "color"), c(f, col, "tipo"),
                    cert.isEmpty() ? Double.NaN : Double.parseDouble(cert), c(f, col, "codigo_equipo"), c(f, col, "uso"),
                    k, entero(c(f, col, "M"), 0), entero(c(f, col, "asentamiento"), 0), c(f, col, "remedida_de_codigo"),
                    xs.isEmpty() ? Double.NaN : Double.parseDouble(xs), c(f, col, "nota"), ajuste));
        }
        return new BancoCola(l, md5);
    }

    private static String c(List<String> f, Map<String, Integer> col, String n) {
        Integer i = col.get(n);
        return i == null || i >= f.size() ? "" : f.get(i).trim();
    }

    private static int entero(String s, int def) {
        try {
            return s.isEmpty() ? def : Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    public Paso paso(int orden) {
        for (Paso p : pasos) {
            if (p.orden == orden) {
                return p;
            }
        }
        return null;
    }

    /**
     * Primer paso pendiente: el primero sin estado en la campana. Si todos tienen estado,
     * el primer patron SALTADO (para medirlo al final). null si no queda nada.
     */
    public Paso siguiente(Map<Integer, String> estados) {
        for (Paso p : pasos) {
            if (!estados.containsKey(p.orden)) {
                return p;
            }
        }
        for (Paso p : pasos) {
            if ("SALTADO".equals(estados.get(p.orden))) {
                return p;
            }
        }
        return null;
    }

    /**
     * Un codigo solo se puede calibrar si todos sus pasos AJUSTE y RE-MEDIDA estan HECHOS
     * (RF-CAL-35): con P56 saltado, el 1 no es calibrable.
     */
    public boolean calibrable(String codigo, Map<Integer, String> estados) {
        boolean alguno = false;
        for (Paso p : pasos) {
            if ("PATRON".equals(p.tipo) && codigo.equals(p.codigo)
                    && ("AJUSTE".equals(p.uso) || "RE-MEDIDA".equals(p.uso))) {
                alguno = true;
                if (!"HECHO".equals(estados.get(p.orden))) {
                    return false;
                }
            }
        }
        return alguno;
    }
}
