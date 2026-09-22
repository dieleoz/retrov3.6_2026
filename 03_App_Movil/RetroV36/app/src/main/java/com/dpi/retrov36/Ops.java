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
    /** Protocolo del firmware (RTV 1.0): con que tramas se mide, se lee x y se lee la bateria. */
    private final Protocolo protocolo;

    public Ops(Canal canal) {
        this(canal, new ProtocoloV36());
    }

    public Ops(Canal canal, Protocolo protocolo) {
        this.canal = canal;
        this.protocolo = protocolo == null ? new ProtocoloV36() : protocolo;
    }

    public Protocolo protocolo() {
        return protocolo;
    }

    /** R de la clave k con la trama de medida del protocolo; null si no hubo respuesta valida. */
    public Integer medirR(char k) throws IOException, InterruptedException {
        String t = protocolo.tramaMedida(k);
        if (t == null) {
            return null;
        }
        Cliente.Respuesta r = canal.pedir(t, protocolo.tipoMedida(), protocolo.timeoutMedidaMs());
        return r.valida() ? protocolo.valorMedida(r.trama) : null;
    }

    /** x en bruto para la clave k ('e' en la V3.6, "#X,k#" en la V4.6); null si no hubo respuesta o no hay x. */
    public Double medirX(char k) throws IOException, InterruptedException {
        String t = protocolo.tramaX(k);
        if (t == null) {
            return null;
        }
        Cliente.Respuesta r = canal.pedir(t, protocolo.tipoX(),
                protocolo.tipoX() == Tramas.Tipo.ADMIN ? Tramas.TIMEOUT_LEERV_MS : TIMEOUT_MEDIDA_MS);
        return r.valida() ? protocolo.valorX(r.trama, k) : null;
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

    /** Bateria (RF-CAL-41, RF-APP-43): '9' en la V3.6, "#GB#" en la V4.6 (unidad por fijar). */
    public Bateria.Lectura bateria() throws IOException, InterruptedException {
        String t = protocolo.tramaBateria();
        if (t == null) {
            return Bateria.interpretar(null);
        }
        Cliente.Respuesta r = canal.pedir(t, protocolo.tipoBateria(), TIMEOUT_MEDIDA_MS);
        Integer n = r.valida() ? protocolo.valorBateria(r.trama) : null;
        String u = protocolo.perfil().bateriaUnidad;
        return "n9".equals(u) ? Bateria.interpretar(n) : "pct".equals(u) ? Bateria.interpretarPorcentaje(n, t)
                : Bateria.interpretarSinUnidad(n, t);
    }

    /**
     * RTV 1.0.0-rc3: lectura de temperatura, con su procedencia, para el diario.
     *
     * Nunca se rellena con 0. Si no hay temperatura, {@link #t} es null y {@link #motivo} dice por que; el
     * diario escribe la columna vacia y el motivo queda anotado una vez por campana.
     */
    public static final class LecturaT {
        /** null si no se pudo leer. */
        public final Tramas.Temperatura t;
        /** "" si se leyo bien. */
        public final String motivo;

        LecturaT(Tramas.Temperatura t, String motivo) {
            this.t = t;
            this.motivo = motivo == null ? "" : motivo;
        }

        public boolean hay() {
            return t != null && t.hayOptica();
        }

        public Double optica() {
            return t == null ? null : t.optica;
        }

        public Double circuito() {
            return t == null ? null : t.circuito;
        }

        /** "OK", "DESC" o "SIN": la tercera columna del diario (RTV 1.0.0-rc3). */
        public String estado() {
            return t == null ? "SIN" : t.estado;
        }
    }

    /**
     * Temperatura del equipo (RTV 1.0.0-rc3). Solo la V4.6 la da, con "#T#". En los demas firmwares no se
     * envia nada: se devuelve el motivo del protocolo, que es lo que va al diario.
     */
    public LecturaT temperatura() throws IOException, InterruptedException {
        String t = protocolo.tramaTemperatura();
        if (t == null) {
            return new LecturaT(null, protocolo.motivoSinTemperatura());
        }
        Cliente.Respuesta r = canal.pedir(t, protocolo.tipoTemperatura(), TIMEOUT_ADMIN_MS);
        if (!r.valida()) {
            return new LecturaT(null, t + " sin respuesta válida: " + r.describir());
        }
        String err = Tramas.motivoError(r.trama);
        if (err != null) {
            // La V4.6 contesta #ERR,OCUPADO# a una trama que llega durante una medida: se ve, no se inventa.
            return new LecturaT(null, t + " respondió #ERR," + err + "#");
        }
        Tramas.Temperatura v = protocolo.valorTemperatura(r.trama);
        if (v == null) {
            return new LecturaT(null, t + " respondió algo que no cuadra con el contrato: " + r.trama);
        }
        return new LecturaT(v, v.motivo);
    }

    /**
     * Resultado de una restauracion: ok solo si la relectura #G coincide con lo que habia.
     * {@code texto}: la trama cruda, para el acta y los registros (P11-M1, siempre igual, campo o corto).
     * {@code resumen}: lo que puede llegar a ver el operador; en corto (RF-COV-17/21, H-3, arquitecto-iot
     * a Cov_3.6.6_calibrar) va sin el numero de codigo ni la trama cruda.
     */
    public static final class Restauracion {
        public final boolean ok;
        public final String texto;
        public final String resumen;

        Restauracion(boolean ok, String texto, String resumen) {
            this.ok = ok;
            this.texto = texto;
            this.resumen = resumen;
        }
    }

    /**
     * Restaura el codigo k a 'anterior': #F,k# si es la de fabrica (RF-APP-18), #S con la copia si no.
     * Relee con #G y lo compara (P11-M1): una restauracion sin relectura igual NO es una restauracion.
     * `corto` (RF-COV-17/21): si el resumen para el operador lleva el numero de codigo o no.
     */
    public Restauracion restaurar(char k, Ecuacion anterior, boolean corto) throws IOException, InterruptedException {
        if (anterior == null) {
            String t = "no se conoce la curva anterior del código " + k + ": no se restaura";
            return new Restauracion(false, t, corto
                    ? "no se conoce la curva anterior de " + Fabrica.elCodigo(k, true) + ": no se restaura" : t);
        }
        boolean fab = anterior.igualFloat32(Fabrica.ecuacion(k));
        String trama;
        if (fab) {
            trama = "#F," + k + "#";
        } else {
            Tramas.TramaS ts = Tramas.tramaS(k, anterior);
            if (ts == null) {
                return new Restauracion(false, "no se pudo formar la trama de restauración",
                        "no se pudo formar la trama de restauración");
            }
            trama = ts.texto;
        }
        Cliente.Respuesta r = escribir(trama);
        Ecuacion e = leerG(k);
        boolean ok = e != null && e.igualFloat32(anterior, fab ? Ecuacion.ULP_G : Ecuacion.ULP_S + Ecuacion.ULP_G);
        String estado = ok ? "igual a la anterior" : e == null ? "SIN RESPUESTA: estado desconocido" : "DISTINTA de la anterior: " + e;
        String crudo = String.format(Locale.US, "restauración con %s -> %s; relectura #G,%c %s",
                fab ? "#F," + k + "#" : "#S (copia anterior)", r.describir(), k, estado);
        String resumen = corto ? "restauración de " + Fabrica.elCodigo(k, true) + ": "
                + (ok ? "verificada" : e == null ? "SIN RESPUESTA: estado desconocido" : "NO verificada") : crudo;
        return new Restauracion(ok, crudo, resumen);
    }
}
