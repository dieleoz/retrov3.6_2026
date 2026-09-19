package com.dpi.retrov36;

import java.io.IOException;
import java.util.Locale;

/**
 * Operaciones con el equipo sobre un {@link Canal} (3.6.13). Java puro: no toca Sesion ni la
 * interfaz, asi que el flujo de calibracion se prueba en la JVM contra un equipo simulado.
 * Sustituye a OpsEquipo de la 3.6.12, que dependia de Cliente y Sesion.
 */
public final class Ops {

    /** Timeouts (los mismos valores que Cliente, sin cargar esa clase). */
    public static final long TIMEOUT_ADMIN_MS = 2000;
    public static final long TIMEOUT_MEDIDA_MS = 2500;
    public static final long TIMEOUT_ESCRITURA_MS = 5000;
    /** Puntos de la comprobacion por #E (los de la prueba 5). */
    public static final int[] X_COMPROBACION = {500, 1000, 2000, 3000, 4000};

    private final Canal canal;

    public Ops(Canal canal) {
        this.canal = canal;
    }

    public Cliente.Respuesta pedir(String t) throws IOException, InterruptedException {
        return canal.pedir(t, Tramas.Tipo.ADMIN, TIMEOUT_ADMIN_MS);
    }

    public Cliente.Respuesta escribir(String t) throws IOException, InterruptedException {
        return canal.pedir(t, Tramas.Tipo.ADMIN, TIMEOUT_ESCRITURA_MS);
    }

    /** Un disparo 'e' o de un codigo: el valor, o null si no hubo respuesta valida. */
    public Integer medir(char c) throws IOException, InterruptedException {
        Cliente.Respuesta r = canal.pedir(String.valueOf(c), Tramas.Tipo.MEDIDA, TIMEOUT_MEDIDA_MS);
        return r.valida() ? Tramas.valorMedida(r.trama) : null;
    }

    /** #L,pin#: true si #OK#. */
    public boolean login(String pin) throws IOException, InterruptedException {
        return entrar(pin) == null;
    }

    /**
     * #L,pin# (QA-3613-02): null si #OK#; si no, el motivo, distinguiendo el equipo mudo (enlace viejo tras
     * apagar), el PIN rechazado y el PIN bloqueado tras 5 fallos (P12 §5.3, hay que apagar y encender).
     */
    public String entrar(String pin) throws IOException, InterruptedException {
        if (pin == null || pin.isEmpty()) {
            return "Falta el PIN del equipo.";
        }
        Cliente.Respuesta r = pedir("#L," + pin + "#");
        if (!r.valida()) {
            return "El equipo no respondió a #L (" + r.describir() + "): reconecte y vuelva a intentarlo.";
        }
        if (Tramas.esOk(r.trama)) {
            return null;
        }
        String m = Tramas.motivoError(r.trama);
        if ("PIN".equals(m)) {
            return "PIN rechazado: vuelva a teclearlo (el firmware se bloquea tras 5 fallos).";
        }
        return "El equipo no admite el PIN (" + r.trama + "): si lleva 5 fallos, apague y encienda el equipo.";
    }

    /** #V#: la informacion, o null si no respondio bien. */
    public Tramas.InfoVersion leerV() throws IOException, InterruptedException {
        Cliente.Respuesta r = pedir("#V#");
        return r.valida() ? Tramas.parsearVersion(r.trama) : null;
    }

    public static String texto(Tramas.InfoVersion v) {
        return v == null ? "sin respuesta válida a #V#" : String.format(Locale.US, "#V,%s,%s,%s,%04X#", v.version,
                v.fecha, v.marca, Math.max(0, v.mascara));
    }

    public Ecuacion leerG(char k) throws IOException, InterruptedException {
        Cliente.Respuesta g = pedir("#G," + k + "#");
        return g.valida() ? Tramas.parsearG(g.trama, k) : null;
    }

    /** #E en 5 puntos contra una curva (+/-1). null si cuadra; si no, el detalle. */
    public String comprobarPorE(char k, Ecuacion curva) throws IOException, InterruptedException {
        StringBuilder mal = new StringBuilder();
        for (int x : X_COMPROBACION) {
            Cliente.Respuesta r = pedir("#E," + k + "," + x + "#");
            Integer v = r.valida() ? Tramas.parsearE(r.trama, k) : null;
            int esperado = curva.respuestaFloat32(x);
            if (v == null) {
                mal.append(" x=").append(x).append(": ").append(r.describir()).append(';');
            } else if (Math.abs(v - esperado) > 1) {
                mal.append(" x=").append(x).append(": #E=").append(v).append(", esperado ").append(esperado).append(';');
            }
        }
        return mal.length() == 0 ? null : "#E no reproduce la curva:" + mal;
    }

    /** Orden 9 (RF-CAL-41). */
    public Bateria.Lectura bateria() throws IOException, InterruptedException {
        Cliente.Respuesta r = canal.pedir("9", Tramas.Tipo.BATERIA, TIMEOUT_MEDIDA_MS);
        return Bateria.interpretar(r.valida() ? Bateria.n(r.trama) : null);
    }

    /** Resultado de una restauracion: ok solo si la relectura #G coincide con lo que habia. */
    public static final class Restauracion {
        public final boolean ok;
        public final String texto;

        Restauracion(boolean ok, String texto) {
            this.ok = ok;
            this.texto = texto;
        }
    }

    /**
     * Restaura el codigo k a 'anterior': #F,k# si es la de fabrica (RF-APP-18), #S con la copia si no.
     * Relee con #G y lo compara (P11-M1): una restauracion sin relectura igual NO es una restauracion.
     */
    public Restauracion restaurar(char k, Ecuacion anterior) throws IOException, InterruptedException {
        if (anterior == null) {
            return new Restauracion(false, "no se conoce la curva anterior del código " + k + ": no se restaura");
        }
        boolean fab = anterior.igualFloat32(Fabrica.ecuacion(k));
        String trama;
        if (fab) {
            trama = "#F," + k + "#";
        } else {
            Tramas.TramaS ts = Tramas.tramaS(k, anterior);
            if (ts == null) {
                return new Restauracion(false, "no se pudo formar la trama de restauración");
            }
            trama = ts.texto;
        }
        Cliente.Respuesta r = escribir(trama);
        Ecuacion e = leerG(k);
        boolean ok = e != null && e.igualFloat32(anterior, fab ? Ecuacion.ULP_G : Ecuacion.ULP_S + Ecuacion.ULP_G);
        return new Restauracion(ok, String.format(Locale.US, "restauración con %s -> %s; relectura #G,%c %s",
                fab ? "#F," + k + "#" : "#S (copia anterior)", r.describir(), k,
                ok ? "igual a la anterior" : e == null ? "SIN RESPUESTA: estado desconocido" : "DISTINTA de la anterior: " + e));
    }
}
