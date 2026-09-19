package com.dpi.retrov36;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Equipo simulado para las pruebas JVM (T-S00): emula el protocolo de la 3.6.2 tal como lo describe
 * 05_Documentacion/PROTOCOLO-V3.6.md: 1-8 y a-d (::R), e (::x), 9 (:n:), #V#, #L, #Q#, #G, #S, #E,
 * #F (k y *), #SC/#GC y #GN. Las escrituras exigen el modo administrador (#ERR,BLOQUEADO#). La
 * EEPROM (curvas, mascara, fecha, serie) sobrevive a apagar y encender; el modo administrador no.
 *
 * Fallos inyectables: corte del enlace antes o despues de aplicar una trama, #ERR en una trama,
 * equipo mudo (timeout) en una trama, una trama que responde #OK# sin hacer nada, y la bateria.
 * Toda trama recibida queda en {@link #recibidas}; una con '@' o un #P hace fallar la prueba.
 */
final class EquipoSimulado implements Canal {

    enum Falla { CORTE_ANTES, CORTE_DESPUES, ERR, MUDO, OK_SIN_HACER }

    private static final class Inyeccion {
        final String prefijo;
        final Falla falla;
        int veces;

        Inyeccion(String prefijo, Falla falla, int veces) {
            this.prefijo = prefijo;
            this.falla = falla;
            this.veces = veces;
        }
    }

    final Map<Character, Ecuacion> curvas = new HashMap<>();
    int mascara;
    String fecha = "2026-09-19";
    String serie = "SLV-002";
    String pin = "1234";
    boolean admin;
    boolean conectado = true;
    int bateriaN = 120;
    /** x que lee el equipo sobre la superficie colocada (oscuro si no hay nada). */
    double x = 565;
    final List<String> recibidas = new ArrayList<>();
    private final List<Inyeccion> fallas = new ArrayList<>();

    EquipoSimulado() {
        for (char k : Fabrica.CODIGOS) {
            curvas.put(k, Fabrica.ecuacion(k));
        }
    }

    /** SLV-002 tras el acta de las 12:23: 1 y 2 escritos, CAL,0003. */
    static EquipoSimulado slv002() {
        EquipoSimulado s = new EquipoSimulado();
        for (Map.Entry<Character, String> e : TablaCalibracion.heredados("SLV-002").entrySet()) {
            s.curvas.put(e.getKey(), Tramas.parsearG(e.getValue(), e.getKey()));
            s.mascara |= 1 << Fabrica.indice(e.getKey());
        }
        return s;
    }

    void inyectar(String prefijo, Falla f, int veces) {
        fallas.add(new Inyeccion(prefijo, f, veces));
    }

    void apagarYEncender() {
        admin = false;
        conectado = true;
    }

    int cuantas(String prefijo) {
        int n = 0;
        for (String t : recibidas) {
            if (t.startsWith(prefijo)) {
                n++;
            }
        }
        return n;
    }

    private static Cliente.Respuesta valida(String p, String t) {
        return new Cliente.Respuesta(p, Receptor.Desenlace.VALIDA, t, t, 40);
    }

    private static Cliente.Respuesta timeout(String p) {
        return new Cliente.Respuesta(p, Receptor.Desenlace.TIMEOUT, null, "", -1);
    }

    @Override
    public Cliente.Respuesta pedir(String t, Tramas.Tipo tipo, long timeoutMs) throws IOException {
        if (t.contains("@") || t.startsWith("#P,")) {
            throw new AssertionError("trama prohibida enviada: " + t);
        }
        if (!conectado) {
            throw new IOException("sin enlace (simulado)");
        }
        recibidas.add(t);
        Inyeccion in = null;
        for (Inyeccion i : fallas) {
            if (i.veces > 0 && t.startsWith(i.prefijo)) {
                in = i;
                i.veces--;
                break;
            }
        }
        if (in != null) {
            switch (in.falla) {
                case CORTE_ANTES:
                    conectado = false;
                    throw new IOException("enlace perdido (simulado) antes de " + t);
                case CORTE_DESPUES:
                    responder(t);
                    conectado = false;
                    throw new IOException("enlace perdido (simulado) tras " + t);
                case ERR:
                    return valida(t, "#ERR,EEPROM#");
                case MUDO:
                    return timeout(t);
                case OK_SIN_HACER:
                    return valida(t, "#OK#");
                default:
            }
        }
        String r = responder(t);
        return r == null ? timeout(t) : valida(t, r);
    }

    private String responder(String t) {
        if (t.length() == 1) {
            char c = t.charAt(0);
            if (c == 'e') {
                return "::" + Math.round(x);
            }
            if (c == '9') {
                return ":" + bateriaN + ":";
            }
            if (Fabrica.esCodigo(c)) {
                return "::" + curvas.get(c).respuestaFloat32((int) Math.round(x));
            }
            return null;
        }
        String[] c = Tramas.campos(t);
        if (c == null) {
            return null;
        }
        switch (c[0]) {
            case "V":
                return String.format(Locale.US, "#V,3.6,2026-09-19,%s,%04X#", mascara == 0 ? "DEF" : "CAL", mascara);
            case "L":
                admin = c.length == 2 && pin.equals(c[1]);
                return admin ? "#OK#" : "#ERR,PIN#";
            case "Q":
                admin = false;
                return "#OK#";
            case "G": {
                if (c.length != 2) {
                    return "#ERR,FORMATO#";
                }
                char k = c[1].charAt(0);
                return Tramas.tramaG(k, curvas.get(k));
            }
            case "GC":
                return "#GC," + fecha + "#";
            case "GN":
                return "#GN," + serie + "#";
            case "E": {
                char k = c[1].charAt(0);
                return "#E," + k + "," + curvas.get(k).respuestaFloat32(Integer.parseInt(c[2])) + "#";
            }
            case "S": {
                if (!admin) {
                    return "#ERR,BLOQUEADO#";
                }
                char k = c[1].charAt(0);
                // strtod + float32: lo guardado es el valor redondeado a float.
                Ecuacion e = new Ecuacion((float) Double.parseDouble(c[2]), (float) Double.parseDouble(c[3]),
                        (float) Double.parseDouble(c[4]), (float) Double.parseDouble(c[5]));
                curvas.put(k, e);
                mascara |= 1 << Fabrica.indice(k);
                return "#OK#";
            }
            case "F": {
                if (!admin) {
                    return "#ERR,BLOQUEADO#";
                }
                if ("*".equals(c[1])) {
                    for (char k : Fabrica.CODIGOS) {
                        curvas.put(k, Fabrica.ecuacion(k));
                    }
                    mascara = 0;
                } else {
                    char k = c[1].charAt(0);
                    curvas.put(k, Fabrica.ecuacion(k));
                    mascara &= ~(1 << Fabrica.indice(k));
                }
                return "#OK#";
            }
            case "SC":
                if (!admin) {
                    return "#ERR,BLOQUEADO#";
                }
                fecha = c[1];
                return "#OK#";
            default:
                return "#ERR,FORMATO#";
        }
    }
}
