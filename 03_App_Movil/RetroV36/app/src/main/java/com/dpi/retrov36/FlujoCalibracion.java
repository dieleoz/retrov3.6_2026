package com.dpi.retrov36;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * "Calibrar este equipo" (3.6.13): toda la logica del flujo, en Java puro, separada de la pantalla.
 * CalibrarActivity solo pinta y llama a las ACCIONES DEL OPERADOR de esta clase (los botones):
 * {@link #calibrar}, {@link #leerBateria}, {@link #persistencia}, {@link #aceptar}, {@link #rechazar}.
 * Las pruebas JVM recorren el flujo de extremo a extremo con esas mismas acciones contra un equipo
 * simulado (P11-M7, T-S07...): ningun paso interno se llama desde fuera.
 *
 * Arreglos de P11 (REVISION-Arquitectura-P11-V3.6.md §5.1) y de QA-App-3.6.12.md:
 * - M1 / QA-3612-03: una restauracion solo cuenta si la relectura #G coincide; si no, el codigo queda
 *   sin resolver, el acta no se acepta y el siguiente "Calibrar" reintenta la restauracion.
 * - M2 / QA-3612-04: cada escritura o restauracion anula la persistencia y la verificacion hechas.
 * - M3 / QA-3612-10: oscuro por codigo (plan propio; "oscuro k" en el acta); el corte durante #S
 *   conserva metodo, oscuro y conformidad.
 * - M4 / QA-3612-08: no se reescribe un codigo conforme, restaurado o con re-medida valida pendiente.
 * - M5 / QA-3612-07: el b (PA-24) y el 5 (PA-14) solo se escriben con la decision de Diego registrada.
 * - M6 / T-C41: los codigos heredados (1 y 2 de SLV-002) se leen y cotejan al empezar, en la
 *   persistencia y antes de aceptar; #V# tiene que decir CAL con su mascara.
 * - QA-3612-01: "Aceptar" se habilita con la persistencia hecha; la verificacion final va dentro.
 * - QA-3612-02: exige pruebas APTO. QA-3612-05: la bateria se relee desde aqui.
 */
public final class FlujoCalibracion {

    // ------------------------------------------------------------ colaboradores

    /** El operador: dialogos bloqueantes (en el hilo de trabajo) y avisos de progreso. */
    public interface Operador {
        /**
         * @return indice de la opcion elegida; -1 si la pantalla ya no existe (se para: QA-3612-11).
         */
        int preguntar(String titulo, String mensaje, String... opciones) throws InterruptedException;

        void progreso(String texto);

        /**
         * Pide apagar y encender el equipo y vuelve a conectar por MAC.
         * @return true SOLO si se vio caer el enlace y volver (P12 §5.1): un OK sin apagar devuelve false.
         */
        boolean apagarYEncender() throws IOException, InterruptedException;

        /** 3.6.16 (P14-07, -B02): un acta acaba de quedar ACEPTADA: la pantalla exporta el ZIP de soporte. */
        default void actaAceptada(Acta a) {
        }
    }

    /** Donde vive el acta en curso (en la app, en disco: Campanas). */
    public interface AlmacenActa {
        Acta enCurso() throws IOException;

        void adjuntar(Acta a) throws IOException;

        void cerrar(Acta a) throws IOException;

        /** true si el codigo k ya tiene un acta ACEPTADA de este equipo (P12 §6.3: el 8 antes que el b y el 5). */
        boolean aceptadoAntes(char k) throws IOException;

        /** 3.6.16 (P14-02): las actas ACEPTADAS de este equipo, de la mas antigua a la mas reciente. */
        default List<Acta> aceptadas() throws IOException {
            return new ArrayList<>();
        }

        /** 3.6.17 (F-03): la ultima acta cerrada (archivada) de este equipo; null si no hay. */
        default Acta ultimaCerrada() throws IOException {
            return null;
        }

        /** 3.6.17 (F-03): anade una linea al diario archivado de la ultima acta cerrada (append-only). */
        default void anadirAUltimaCerrada(String linea) throws IOException {
        }

        /**
         * 3.6.17 (P14-B02): exporta el ZIP de soporte justo antes de cerrar el acta aceptada y devuelve su SHA-256,
         * que el acta cita; null si no se pudo (o en las pruebas).
         */
        default String soporteSha256(Acta a) {
            return null;
        }
    }

    public interface Reloj {
        String ahoraIso();

        /** AAAA-MM-DD. */
        String hoy();
    }

    /** Lo que la sesion sabe del equipo conectado. */
    public static final class Contexto {
        public boolean conectado;
        /** true si el equipo habla el contrato de la 3.6.2 con serie y fecha (V3.6.2, o la V4.6 que lo adopta). */
        public boolean es362;
        /** Protocolo del firmware (RTV 1.0): con que tramas se mide, se lee x y la bateria. */
        public Protocolo protocolo = new ProtocoloV36();
        public String firmware = "";
        /** null: pruebas sin hacer. */
        public Boolean apto;
        public String resumenPruebas = "";
        /** Serie leida con #GN#. */
        public String serieGN;
        public String nombreBT = "";
        public String mac = "";
        public String app = "";
    }

    public static final class Previa {
        public final boolean ok;
        public final String texto;

        Previa(boolean ok, String texto) {
            this.ok = ok;
            this.texto = texto;
        }
    }

    /** Plan de escritura de un codigo, con SU oscuro (P11-M3). */
    public static final class Plan {
        public final char k;
        public final TablaCalibracion.Fila fila;
        public final Asistente.Propuesta propuesta;
        public final double xOscuro;
        public final String origenOscuro;
        public final String patrones;
        /** null si se puede escribir; si no, el motivo. */
        public final String motivoNo;

        Plan(char k, TablaCalibracion.Fila fila, Asistente.Propuesta propuesta, double xOscuro, String origenOscuro,
             String patrones, String motivoNo) {
            this.k = k;
            this.fila = fila;
            this.propuesta = propuesta;
            this.xOscuro = xOscuro;
            this.origenOscuro = origenOscuro;
            this.patrones = patrones;
            this.motivoNo = motivoNo;
        }

        public boolean escribible() {
            return motivoNo == null;
        }

        boolean anclada() {
            return propuesta != null && propuesta.metodo != null && propuesta.metodo.contains("anclada");
        }

        String oscuroTexto(Ecuacion e) {
            return String.format(Locale.US, "Oscuro del código %c (x = %.1f, %s): R %.1f (fábrica %.1f), P9-B13",
                    k, xOscuro, origenOscuro, e.evaluar(xOscuro), Fabrica.ecuacion(k).evaluar(xOscuro));
        }
    }

    /** Una tarjeta de la pantalla: un codigo de la tabla. */
    public static final class Tarjeta {
        public final char k;
        public final String texto;
        /** true: la casilla de conformidad se puede marcar. */
        public final boolean casilla;
        /** Texto de la casilla. */
        public final String textoCasilla;

        Tarjeta(char k, String texto, boolean casilla, String textoCasilla) {
            this.k = k;
            this.texto = texto;
            this.casilla = casilla;
            this.textoCasilla = textoCasilla;
        }
    }

    private final Canal canal;
    private final Ops ops;
    private final Operador operador;
    private final AlmacenActa almacen;
    private final BancoCola cola;
    private final Campana campana;
    private final List<Patron> catalogo;
    private final Decisiones decisiones;
    private final Contexto ctx;
    private final Reloj reloj;
    private String pin;
    private Acta acta;
    /** QA-3613-06: la ultima bateria leida aqui manda, aunque la campana este cerrada y no se anote. */
    private Bateria.Lectura ultimaBateria;
    /**
     * true tras una persistencia OK en esta sesion sin escribir nada despues: el T-C41 del acta siguiente
     * no vuelve a pedir apagar (el equipo se acaba de apagar y encender de verdad).
     */
    private boolean encendidoReciente;
    /** P14-01: el atajo de T-C41 solo vale dentro de una pulsacion de "Calibrar todo". */
    private boolean sesionTodo;
    /** 3.6.17: el ultimo aceptar() escribio #SC (una escritura: el siguiente T-C41 no usa el atajo). */
    private boolean scEnUltimoAceptar;
    /** Codigo cuya persistencia dejo el equipo recien encendido (para anotarlo en el acta siguiente). */
    private char encendidoPor;
    /** Codigos que se escriben en la pulsacion en curso (T-C41 no los coteja: van a cambiar). */
    private final Set<Character> escribiendoAhora = new HashSet<>();

    public FlujoCalibracion(Canal canal, Operador operador, AlmacenActa almacen, BancoCola cola, Campana campana,
                            List<Patron> catalogo, Decisiones decisiones, Contexto ctx, Reloj reloj) throws IOException {
        this.canal = canal;
        this.ops = new Ops(canal, ctx.protocolo);
        this.operador = operador;
        this.almacen = almacen;
        this.cola = cola;
        this.campana = campana;
        this.catalogo = catalogo;
        this.decisiones = decisiones == null ? Decisiones.ninguna() : decisiones;
        this.ctx = ctx;
        this.reloj = reloj;
        Acta a = almacen.enCurso();
        this.acta = a == null || a.cerrada() ? null : a;
    }

    /** PIN del equipo: se pide una vez por sesion (QA-3612-14) y solo vive en memoria. */
    public void pin(String p) {
        this.pin = p;
    }

    public Acta acta() {
        return acta;
    }

    /** QA-3613-02: tras cualquier fallo de #L el flujo olvida el PIN; la pantalla lo vuelve a pedir. */
    public boolean necesitaPin() {
        return pin == null || pin.isEmpty();
    }

    private String equipo() {
        if (campana != null) {
            return TablaCalibracion.canonico(campana.historialSeries(), campana.mac);
        }
        return ctx.serieGN == null ? "" : ctx.serieGN;
    }

    private Map<Character, TablaCalibracion.Fila> tabla() {
        return TablaCalibracion.tabla(decisiones, equipo());
    }

    private TablaCalibracion.Fila fila(char k) {
        return tabla().get(k);
    }

    /**
     * Lo que la pantalla sabe del equipo, leido del propio equipo (#V#, #GC#, #GN#), como hacen las pruebas
     * al conectar. Lo usan las pruebas JVM para no rellenar el Contexto a mano (P12 §4).
     */
    public static Contexto identificar(Canal canal, String nombreBT, String mac, Boolean apto, String resumen, String app)
            throws IOException, InterruptedException {
        Ops o = new Ops(canal);
        Contexto c = new Contexto();
        c.conectado = true;
        c.nombreBT = nombreBT;
        c.mac = mac;
        c.apto = apto;
        c.resumenPruebas = resumen;
        c.app = app;
        Tramas.InfoVersion v = o.leerV();
        Cliente.Respuesta gc = v == null ? null : o.pedir("#GC#");
        boolean v362 = gc != null && Calibracion.variante(gc.trama) == Calibracion.Variante.V362;
        boolean v46 = v != null && "4.6".equals(v.version);
        c.protocolo = v46 ? new ProtocoloV46() : new ProtocoloV36();
        // RTV 1.0: la V4.6 adopta el contrato de la 3.6.2 (#GN#, #GC#), asi que cuenta como tal.
        c.es362 = v != null && ("3.6".equals(v.version) && v362 || v46 && gc != null && gc.valida());
        c.firmware = v == null ? "sin detectar" : "V" + v.version + " " + v.fecha + (v362 && !v46 ? " (3.6.2) " : " ") + v.marca
                + " mascara " + String.format(Locale.US, "%04X", Math.max(0, v.mascara));
        if (c.es362) {
            Cliente.Respuesta gn = o.pedir("#GN#");
            c.serieGN = gn.valida() ? Calibracion.serieDe(gn.trama) : null;
        }
        return c;
    }

    // ------------------------------------------------------------- previas

    private Anclas.Valor sRep() {
        return campana == null ? null : Anclas.sRep(cola, campana);
    }

    /** C-P14-2: la s_rep de la A5 del inicio de la sesion del codigo k (dice de donde sale). */
    private Anclas.Valor sRep(char k) {
        if (campana == null) {
            return null;
        }
        int ses = cola == null ? 1 : Anclas.sesionDe(cola, k);
        return Anclas.sRep(cola, campana, ses > 0 ? ses : 1);
    }

    // ------------------------------------------------ actas aceptadas (P14-02, QA-3615-05)

    /** El acta ACEPTADA mas reciente con el codigo k conforme y no restaurado; null si ninguna. */
    private Acta aceptadaVigente(char k) {
        try {
            Acta v = null;
            for (Acta a : almacen.aceptadas()) {
                Acta.Codigo c = a.codigo(k);
                if (a.aceptada() && c != null && c.conforme() && c.restaurado == null) {
                    v = a;
                }
            }
            if (v == null && almacen.aceptadoAntes(k)) {
                return new Acta("", "", "", 0, 0, 0, "");   // almacen sin actas (Banco, Avanzado): solo se sabe que hay una
            }
            return v;
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * QA-3615-05: un codigo con acta ACEPTADA se puede volver a calibrar si alguna serie que uso (ajuste, re-medida u
     * OSCURO) se ha anulado en el banco despues ("rehacer" avisado). Devuelve la serie anulada, o null.
     */
    private String serieAnuladaDe(Acta a, char k) {
        String ids = a == null ? null : a.dato("series " + k);
        if (ids == null || campana == null) {
            return null;
        }
        for (String id : ids.trim().split("\\s+")) {
            Campana.Serie s = campana.serie(id);
            if (s != null && s.anulada != null) {
                return id + " (" + s.patron + ")";
            }
        }
        return null;
    }

    /** true si k tiene un acta ACEPTADA que sigue valiendo (ninguna serie suya anulada). */
    private boolean aceptadoVigente(char k) {
        Acta a = aceptadaVigente(k);
        return a != null && serieAnuladaDe(a, k) == null;
    }

    /** P14-02: heredados = el 1 y el 2 del acta de las 12:23, mas las curvas de las actas ACEPTADAS en esta app. */
    private Map<Character, String> heredadosTodos() {
        Map<Character, String> m = new LinkedHashMap<>(TablaCalibracion.heredados(equipo()));
        try {
            for (Acta a : almacen.aceptadas()) {
                for (Acta.Codigo c : a.certificados()) {
                    m.put(c.k, c.tramaG);
                }
            }
        } catch (IOException e) {
            // sin actas: solo los fijos
        }
        if (acta != null) {
            for (Acta.Codigo c : acta.codigos()) {
                m.remove(c.k);
            }
            m.remove(acta.escribiendo());
        }
        for (char k : escribiendoAhora) {
            m.remove(k);
        }
        return m;
    }

    private String origenHeredados() {
        String o = TablaCalibracion.origenHeredados(equipo());
        return (o.isEmpty() ? "" : o + " y ") + "las actas ACEPTADAS en esta app";
    }

    public List<Previa> previas() {
        List<Previa> l = new ArrayList<>();
        l.add(new Previa(ctx.conectado, ctx.conectado ? "Conectado con " + ctx.nombreBT : "Sin conexión: conecte con el equipo"));
        l.add(new Previa(ctx.es362, ctx.es362 ? "Firmware " + ctx.firmware
                : "El firmware no está detectado como 3.6.2 (" + ctx.firmware + "): pase las pruebas del equipo"));
        // RTV 1.0 (RF-APP-U07): el protocolo dice si este firmware se calibra.
        // RTV 1.0.0-rc6: y, si no se calibra, dice QUE HACER para que se pueda; dejarlo en la negación fue lo
        // que se vio en campo el 19-sep con SLV-028 (un V4 original, al que le falta la V4.6 grabada).
        l.add(new Previa(ctx.protocolo.calibra(), ctx.protocolo.calibra() ? "Protocolo " + ctx.protocolo.nombre()
                : ctx.protocolo.motivoNoCalibra() + (ctx.protocolo.queHacerParaCalibrar().isEmpty() ? ""
                : ". " + ctx.protocolo.queHacerParaCalibrar())));
        boolean apto = Boolean.TRUE.equals(ctx.apto);
        l.add(new Previa(apto, ctx.apto == null ? "Pruebas del equipo sin hacer: páselas antes"
                : apto ? "Pruebas: APTO (" + ctx.resumenPruebas + ")"
                : "Pruebas: NO APTO (" + ctx.resumenPruebas + "): con NO APTO no se calibra (QA-3612-02)"));
        l.add(new Previa(cola != null, cola != null ? "Cola del banco md5 " + cola.md5 : "Cola del banco no admitida"));
        l.add(new Previa(campana != null, campana != null ? "Campaña de " + campana.serieConHistoria() + " (" + campana.mac + ")"
                : "Sin campaña de este equipo"));
        String gn = ctx.serieGN;
        // 3.6.15: la serie del equipo casa con la campana si esta en su historial (RENOMBRA), con la misma MAC.
        boolean idOk = campana != null && gn != null && !Calibracion.NONE.equals(gn) && campana.esSerie(gn)
                && (ctx.mac == null || ctx.mac.isEmpty() || campana.mac.equalsIgnoreCase(ctx.mac));
        // 3.6.17 (S-01, S-02, S-04): la del equipo tiene que ser la serie ACTUAL de la campana, que es la que va al
        // acta. Si es otra del historial (un #SN que no entro, la app matada al renombrar), no se calibra.
        boolean actual = idOk && gn.equalsIgnoreCase(campana.serieActual());
        l.add(new Previa(actual, gn == null ? "Serie del equipo (#GN#) sin leer: pase las pruebas"
                : Calibracion.NONE.equals(gn) ? "El equipo no tiene serie (#GN# = NONE): dé de alta la serie en Avanzado"
                : actual ? "Serie #GN# " + gn + " = campaña"
                : idOk ? "El equipo dice " + gn + " (#GN#) y la campaña " + campana.serieActual() + ": repita \"Cambiar "
                + "serie\" en Avanzado hasta que coincidan (el acta lleva la serie de la campaña). No se calibra"
                : "La serie #GN# (" + gn + ") no coincide con la campaña (" + (campana == null ? "-" : campana.equipo)
                + "): no se calibra"));
        // 3.6.17 (F-03): tras un cierre SIN RESTAURAR no se calibra hasta que Diego lo libere.
        String sinR = bloqueoSinRestaurar();
        if (sinR != null) {
            l.add(new Previa(false, sinR));
        }
        Anclas.Valor sr = sRep();
        boolean srOk = sr != null && !Double.isNaN(sr.valor);
        l.add(new Previa(srOk, sr == null ? "s_rep: sin campaña" : sr.texto + (srOk ? "" : " — mida la A5 del inicio del banco")));
        if (ultimaBateria != null ? ultimaBateria.bloqueaEscrituras : campana != null && campana.bateriaBloqueaEscrituras()) {
            l.add(new Previa(false, "Batería: la última lectura bloquea las escrituras (n = 0 o sin respuesta). "
                    + "Cambie la batería y pulse \"Leer batería\""));
        }
        if (acta != null && acta.invalidada()) {
            l.add(new Previa(false, "El acta en curso está invalidada: recházela"));
        }
        if (campana != null && BancoCola.Tipo.VERIFICACION_ANUAL.name().equals(campana.colaTipo())) {
            l.add(new Previa(false, "Campaña de verificación anual: no se calibra (RF-APP-49)"));
        }
        return l;
    }

    public String motivoPrevias() {
        for (Previa p : previas()) {
            if (!p.ok) {
                return p.texto;
            }
        }
        return null;
    }

    // ---------------------------------------------------------------- planes

    /**
     * Patrones del ajuste del codigo k: los de AJUSTE y RE-MEDIDA de la cola. Si la cola no tiene ningun
     * AJUSTE para el codigo (el 5, que la cola solo verificaba antes de PA-14), todos sus patrones medidos,
     * VERIFICACION incluidos: es lo que permite ajustarlo con la recta anclada (PA-14, RF-APP-42).
     */
    private Set<String> patronesAjuste(char k) {
        TablaCalibracion.Fila f = fila(k);
        return patronesAjuste(cola, k, f == null ? new ArrayList<String>() : f.excluidos);
    }

    /** Los patrones del ajuste del codigo k con esta cola, sin los excluidos por decision (P81 del 5). */
    public static Set<String> patronesAjuste(BancoCola cola, char k, List<String> excluidos) {
        Set<String> s = new HashSet<>();
        Set<String> todos = new HashSet<>();
        boolean hayAjuste = false;
        if (cola != null) {
            for (BancoCola.Paso p : cola.pasos) {
                if ("PATRON".equals(p.tipo) && String.valueOf(k).equals(p.codigo)) {
                    todos.add(p.patron);
                    hayAjuste |= "AJUSTE".equals(p.uso);
                    if ("AJUSTE".equals(p.uso) || "RE-MEDIDA".equals(p.uso)) {
                        s.add(p.patron);
                    }
                }
            }
        }
        Set<String> r = hayAjuste ? s : todos;
        r.removeAll(excluidos);   // P81 fuera del ajuste del 5 (decision de Diego, c836cae)
        return r;
    }

    private List<Medida> medidasDe(char k) {
        Set<String> ps = patronesAjuste(k);
        List<Medida> l = new ArrayList<>();
        for (Medida m : campana.medidasElegidas()) {
            if (ps.contains(m.patron.nombre)) {
                l.add(m);
            }
        }
        return l;
    }

    /** Ids de las series elegidas del ajuste y de la re-medida del codigo k (QA-3614-03). */
    private String seriesDe(char k, TablaCalibracion.Fila f) {
        Set<String> pats = new java.util.TreeSet<>(patronesAjuste(k));
        if (f != null && !f.remedida.isEmpty()) {
            pats.add(f.remedida);
        }
        StringBuilder sb = new StringBuilder();
        for (String pt : pats) {
            Campana.Serie s = campana.elegida(pt);
            if (s != null) {
                sb.append(sb.length() == 0 ? "" : " ").append(s.id);
            }
        }
        // QA-3614-03 (P14-B08): tambien los OSCURO de la sesion del codigo, que dan su ancla.
        if (cola != null) {
            int ses = Anclas.sesionDe(cola, k);
            for (BancoCola.Paso p : cola.pasos) {
                String id = "OSCURO".equals(p.tipo) && p.sesion == ses ? campana.seriePaso(p.orden) : null;
                if (id != null && !id.isEmpty()) {
                    sb.append(' ').append(id);
                }
            }
        }
        return sb.toString();
    }

    /** K x M de las series elegidas del codigo (B3, PROTOCOLO-MIN): "P44 1×4, P37 1×4, ...". */
    private String protocoloDe(char k, TablaCalibracion.Fila f) {
        Set<String> pats = new java.util.TreeSet<>(patronesAjuste(k));
        if (f != null && !f.remedida.isEmpty()) {
            pats.add(f.remedida);
        }
        StringBuilder sb = new StringBuilder();
        for (String pt : pats) {
            Campana.Serie s = campana.elegida(pt);
            if (s != null) {
                int m = 0;
                for (double[] g : s.colocaciones()) {
                    m = Math.max(m, g.length);
                }
                sb.append(sb.length() == 0 ? "" : ", ").append(pt).append(' ').append(s.colocaciones().size())
                        .append('×').append(m);
            }
        }
        return sb + "; re-medida " + Remedida3611.K + "×" + Remedida3611.M + " (P12, P13)";
    }

    /** null si ninguna serie que uso el acta esta anulada; si no, el motivo (QA-3614-03). */
    private String motivoSeriesAnuladas() {
        if (acta == null || campana == null) {
            return null;
        }
        for (Acta.Codigo c : acta.codigos()) {
            String ids = acta.dato("series " + c.k);
            if (ids == null || ids.equals("no conocido")) {
                continue;
            }
            for (String id : ids.trim().split("\\s+")) {
                Campana.Serie s = campana.serie(id);
                if (s != null && s.anulada != null) {
                    return "la serie " + id + " (" + s.patron + ") que usó el código " + c.k + " se ANULÓ en el banco ("
                            + s.anulada + "): rechace el acta y vuelva a calibrar";
                }
            }
        }
        return null;
    }

    /** QA-3614-09 y QA-3614-03: lo que impide aceptar ademas del estado del acta. */
    private String motivoAceptarExtra() {
        if (acta == null) {
            return null;
        }
        if (!TablaCalibracion.VERSION.equals(acta.tabla)) {
            return "el acta se abrió con otras reglas (" + (acta.tabla.isEmpty() ? "sin tabla" : acta.tabla)
                    + "); las de este APK son " + TablaCalibracion.VERSION + ": recházela y vuelva a calibrar";
        }
        return motivoSeriesAnuladas();
    }

    private String textoPatrones(List<Medida> med) {
        Map<String, Integer> n = new LinkedHashMap<>();
        for (Medida m : med) {
            Integer v = n.get(m.patron.nombre);
            n.put(m.patron.nombre, v == null ? 1 : v + 1);
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> e : n.entrySet()) {
            sb.append(sb.length() == 0 ? "" : ", ").append(e.getKey()).append(" (n = ").append(e.getValue()).append(')');
        }
        return sb.toString();
    }

    /** Plan del codigo k con su propio oscuro. vigente: curva actual del equipo (o fabrica). */
    public Plan plan(char k, Ecuacion vigente) {
        return plan(k, vigente, false);
    }

    /**
     * ignorarOrden = true: como si el codigo requerido (el 8) ya tuviera su acta ACEPTADA. Lo usa "Calibrar
     * todo" para comprobar de antemano todo lo que va a escribir en la misma sesion, en orden.
     */
    Plan plan(char k, Ecuacion vigente, boolean ignorarOrden) {
        TablaCalibracion.Fila f = fila(k);
        if (f == null || !f.escribible()) {
            return new Plan(k, f, null, Double.NaN, "", "", f == null ? "código desconocido"
                    : f.aviso.isEmpty() ? "no se escribe (" + f.texto() + ")" : f.aviso);
        }
        if (cola == null || campana == null) {
            return new Plan(k, f, null, Double.NaN, "", "", "sin cola o sin campaña");
        }
        if (!cola.calibrable(String.valueOf(k), campana.pasos())) {
            // RTV 1.0.0-rc6 (D-1): en campo el motivo fue que lo medido venia de OTRO banco, y el mensaje de la rc5
            // no decia donde se arreglaba. Se dice aqui, que es donde el operador lo lee.
            return new Plan(k, f, null, Double.NaN, "", "",
                    "no calibrable: faltan patrones de AJUSTE o RE-MEDIDA del banco " + campana.colaTipo()
                            + " (medidos o saltados)" + (campana.pasos().isEmpty()
                            ? "; esta campaña no tiene ni un paso anotado en ese banco: si lo medido vino de un ZIP "
                            + "de otro banco, cámbielo en Banco, botón \"Banco: …\"" : ""));
        }
        if (f.todosLosPasos) {
            String falta = pasosSinHacer(k);
            if (falta != null) {
                return new Plan(k, f, null, Double.NaN, "", "", "exige todos sus pasos del banco hechos (P12 §6.6); faltan "
                        + falta);
            }
        }
        if (f.requiereAceptado != 0 && !ignorarOrden && !aceptadoVigente(f.requiereAceptado)) {
            return new Plan(k, f, null, Double.NaN, "", "", "va después de un acta ACEPTADA (y vigente) del código "
                    + f.requiereAceptado + ": el " + f.requiereAceptado + " solo y primero");
        }
        String proto = protocoloIncumplido(k, f);
        if (proto != null) {
            return new Plan(k, f, null, Double.NaN, "", "", proto);
        }
        Anclas.Valor osc = Anclas.oscuro(cola, campana, k);
        if (Double.isNaN(osc.valor)) {
            // Sin el oscuro de la sesion no se comprueba P9-B13 ni se ancla: no se escribe (QA-3612-10).
            return new Plan(k, f, null, Double.NaN, osc.texto, "", "sin oscuro de la sesión: " + osc.texto);
        }
        Anclas.Valor sr = sRep(k);
        List<Medida> med = medidasDe(k);
        Ecuacion vig = vigente == null ? Fabrica.ecuacion(k) : vigente;
        Asistente.Propuesta p;
        synchronized (Asistente.class) {
            // Los parametros de Asistente son estaticos: se fijan y se usan dentro del mismo bloque.
            Asistente.X_OSCURO = osc.valor;
            Asistente.S_REP_REL = sr == null ? Double.NaN : sr.valor;
            p = null;
            if (f.metodo == TablaCalibracion.Metodo.GRADO1 || f.metodo == TablaCalibracion.Metodo.GRADO1_O_ANCLADA) {
                p = Asistente.proponer(k, 1, med, vig, catalogo);
                if (f.metodo == TablaCalibracion.Metodo.GRADO1_O_ANCLADA && p.ajuste != null
                        && Asistente.criterioFirmwareS(p.ajuste.ecuacion) != null) {
                    p = null;
                }
            }
            if (p == null) {
                Asistente.X_ANCLA = osc.valor;
                Asistente.R_ANCLA = 0;
                Asistente.ORIGEN_ANCLA = osc.texto;
                p = Asistente.proponer(k, Asistente.GRADO_ANCLADA, med, vig, catalogo);
            }
        }
        String no = null;
        if (!p.escribible() || p.ajuste == null) {
            no = "la propuesta no se puede escribir: " + String.join("; ", p.bloqueos);
        } else {
            List<String> fuera = noDispensados(f, p.incumplimientos);
            if (!fuera.isEmpty()) {
                no = "incumple " + String.join("; ", fuera) + (f.dispensa.isEmpty() ? " y la tabla no le da dispensa"
                        : " y queda FUERA de lo dispensado (" + f.dispensa + ", P12 §5.2)");
            }
        }
        return new Plan(k, f, p, osc.valor, osc.texto, textoPatrones(med), no);
    }

    private static final java.util.regex.Pattern INC14 = java.util.regex.Pattern.compile(
            "RF-CAL-14: (P\\d+[a-z]?) \\([^)]*\\) residuo ([+-]?\\d+(?:\\.\\d+)?) %");

    private static final java.util.regex.Pattern INC15 = java.util.regex.Pattern.compile(
            "RF-CAL-15: RMS de (\\S+) (\\d+(?:\\.\\d+)?) %");

    /**
     * P12 §5.2: solo quedan dispensados los incumplimientos que figuran en el alcance de la decision:
     * RF-CAL-14 por patron y RF-CAL-15 por tipo de lamina, con su cifra y su margen. RF-CAL-16 nunca.
     */
    static List<String> noDispensados(TablaCalibracion.Fila f, List<String> incumplimientos) {
        List<String> l = new ArrayList<>();
        for (String s : incumplimientos) {
            java.util.regex.Matcher m = INC14.matcher(s);
            java.util.regex.Matcher m15 = INC15.matcher(s);
            boolean ok = m.find() && f.dispensado("RF-CAL-14", m.group(1), Double.parseDouble(m.group(2)))
                    || m15.find() && f.dispensado("RF-CAL-15", m15.group(1), Double.parseDouble(m15.group(2)));
            if (!ok) {
                l.add(s);
            }
        }
        return l;
    }

    /** Pasos PATRON del codigo sin HECHO (null si ninguno). */
    private String pasosSinHacer(char k) {
        Map<Integer, String> est = campana.pasos();
        StringBuilder sb = new StringBuilder();
        for (BancoCola.Paso p : cola.pasos) {
            if ("PATRON".equals(p.tipo) && String.valueOf(k).equals(p.codigo) && !"HECHO".equals(est.get(p.orden))) {
                sb.append(sb.length() == 0 ? "" : ", ").append(p.patron);
            }
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    /** Rango certificado del ajuste anclado: el ancla (0) y los certificados del codigo en el catalogo. */
    private String rango(char k, Plan p) {
        double max = 0;
        double min = Double.MAX_VALUE;
        java.util.Set<String> tipos = new java.util.TreeSet<>();
        for (Asistente.Punto q : p.propuesta.puntos) {
            max = Math.max(max, q.patron.valor);
            min = Math.min(min, q.patron.valor);
            tipos.add(q.patron.tipo);
        }
        return String.format(Locale.US, "%s: 0-%.0f (ancla del OSCURO en R = 0 y certificados %.0f-%.0f de %d patrones; "
                + "tipos %s). Por encima de %.0f la curva extrapola", p.anclada() ? "recta anclada" : "ajuste", max,
                min, max, p.propuesta.puntos.size(), tipos, max);
    }

    private boolean aceptadoSinFallo(char k) {
        return aceptadoVigente(k);
    }

    /** Valor de PROTOCOLO-AJUSTE en decisiones.csv; null = el provisional (PRECISO). */
    private String protocoloAjuste() {
        return decisiones.valor("PROTOCOLO-AJUSTE", equipo());
    }

    /**
     * C-P14-3 / B3: las series del ajuste y de la re-medida de un codigo que se escribe (8, b, 5) tienen que estar en
     * el protocolo que exige PROTOCOLO-AJUSTE (provisional: 5 × 4). null si cumplen; si no, el motivo.
     */
    private String protocoloIncumplido(char k, TablaCalibracion.Fila f) {
        if (!ProtocoloDisparos.CODIGOS_A_ESCRIBIR.contains(k) || campana == null) {
            return null;
        }
        int[] req = ProtocoloDisparos.requerido(protocoloAjuste());
        Set<String> pats = new java.util.TreeSet<>(patronesAjuste(k));
        if (!f.remedida.isEmpty()) {
            pats.add(f.remedida);
        }
        StringBuilder mal = new StringBuilder();
        for (String pt : pats) {
            String m = ProtocoloDisparos.incumple(pt, campana.elegida(pt), req);
            if (m != null) {
                mal.append(mal.length() == 0 ? "" : ", ").append(m);
            }
        }
        if (mal.length() == 0) {
            return null;
        }
        return "el banco del " + k + " no está en " + req[0] + "×" + req[1] + ": " + mal + ". Repítalo en preciso en el "
                + "Banco (\"Rehacer patrón\"), o que Diego acepte otro protocolo en decisiones.csv (PROTOCOLO-AJUSTE)"
                + (protocoloAjuste() == null ? "; 5×4 es la recomendación provisional de P14 §3" : "");
    }

    /** Motivo por el que el codigo k de ESTA acta no admite escritura (independiente de otros codigos). */
    private String motivoCodigo(char k) {
        if (acta == null) {
            return null;
        }
        if (acta.cerrada() || acta.invalidada()) {
            return acta.motivoNoEscribir(k);
        }
        Acta.Codigo c = acta.codigo(k);
        if (c == null) {
            return null;
        }
        String m = acta.motivoNoEscribir(k);
        return m != null && m.contains("código " + k) ? m : null;
    }

    public List<Tarjeta> tarjetas(Map<Character, Ecuacion> vigentes) {
        List<Tarjeta> l = new ArrayList<>();
        for (TablaCalibracion.Fila f : tabla().values()) {
            StringBuilder sb = new StringBuilder(f.texto());
            if (!f.aviso.isEmpty()) {
                sb.append("\n  ").append(f.aviso);
            }
            boolean casilla = false;
            if (f.escribible()) {
                Plan p = plan(f.codigo, vigentes == null ? null : vigentes.get(f.codigo), true);
                if (f.requiereAceptado != 0 && p.motivoNo == null && !aceptadoSinFallo(f.requiereAceptado)) {
                    sb.append("\n  Irá después del acta ACEPTADA del código ").append(f.requiereAceptado)
                            .append(" (misma sesión con \"Calibrar todo\")");
                }
                Acta vig = aceptadaVigente(f.codigo);
                if (p.motivoNo == null && vig != null && (acta == null || acta.codigo(f.codigo) == null)) {
                    String anul = serieAnuladaDe(vig, f.codigo);
                    if (anul == null) {
                        sb.append("\n  Ya tiene un acta ACEPTADA: no se vuelve a escribir");
                        l.add(new Tarjeta(f.codigo, sb.toString(), false, ""));
                        continue;
                    }
                    sb.append("\n  Su acta ACEPTADA usa la serie ").append(anul)
                            .append(", anulada en el banco: se puede volver a calibrar");
                }
                if (p.propuesta != null) {
                    sb.append("\n  ").append(p.propuesta.metodo).append(", ").append(p.patrones);
                    if (p.propuesta.ajuste != null) {
                        Ecuacion e = p.propuesta.ajuste.ecuacion;
                        sb.append(String.format(Locale.US, "\n  c1 %s  c0 %s  error máx %.1f", Tramas.coeficiente(e.c1),
                                Tramas.coeficiente(e.c0), p.propuesta.ajuste.errorMaximo));
                    }
                    List<String> fuera = noDispensados(f, p.propuesta.incumplimientos);
                    for (String s : p.propuesta.incumplimientos) {
                        sb.append("\n  Incumple (").append(fuera.contains(s) ? "SIN dispensa" : "dispensado por " + f.dispensa)
                                .append("): ").append(s);
                    }
                    sb.append("\n  ").append(p.origenOscuro);
                }
                String mc = motivoCodigo(f.codigo);
                if (p.motivoNo != null) {
                    sb.append("\n  NO SE ESCRIBE: ").append(p.motivoNo);
                } else if (mc != null) {
                    sb.append("\n  ").append(mc);
                } else {
                    casilla = true;
                }
            }
            if (acta != null && acta.codigo(f.codigo) != null) {
                Acta.Codigo c = acta.codigo(f.codigo);
                sb.append("\n  En el acta: ").append(c.conforme() ? "CONFORME" : c.restaurado != null ? "RESTAURADO"
                        : c.restauracionFallida != null ? "RESTAURACIÓN NO VERIFICADA" : "escrito, re-medida pendiente")
                        .append(" (").append(c.intentos.size()).append(" intentos)");
            }
            l.add(new Tarjeta(f.codigo, sb.toString(), casilla, "Conforme: escribir el código " + f.codigo
                    + (f.dispensa.isEmpty() ? "" : " (la dispensa " + f.dispensa + " es de Diego, en decisiones.csv; "
                    + "esta casilla es la conformidad del superadministrador con la escritura)")));
        }
        return l;
    }

    // --------------------------------------------------------- estado de botones

    /** Codigos a medias en el acta (re-medida, restauracion o corte pendientes). */
    public int pendientes() {
        int n = 0;
        if (acta != null && !acta.cerrada()) {
            for (Acta.Codigo c : acta.codigos()) {
                if (!c.resuelto()) {
                    n++;
                }
            }
            if (acta.escribiendo() != 0) {
                n++;
            }
        }
        return n;
    }

    public boolean puedeCalibrar(Set<Character> seleccion) {
        return motivoPrevias() == null && (acta == null || acta.rechazoPendiente() == null)
                && ((seleccion != null && !seleccion.isEmpty()) || pendientes() > 0);
    }

    /** P14-10: hay un rechazo pendiente (la pantalla ofrece "Cerrar sin restaurar"). */
    public boolean rechazoPendiente() {
        return acta != null && acta.rechazoPendiente() != null;
    }

    public boolean puedePersistencia() {
        return acta != null && !acta.cerrada() && ctx.conectado && acta.motivoNoAceptable(false) == null;
    }

    /** QA-3612-01: todo salvo la verificacion final, que se hace al pulsar. */
    public boolean puedeAceptar() {
        return acta != null && !acta.cerrada() && motivoPrevias() == null
                && acta.motivoNoAceptableSalvoVerificacionFinal() == null && motivoAceptarExtra() == null;
    }

    /** Por que no se puede aceptar (para la pantalla); null si se puede. */
    public String motivoNoAceptar() {
        if (acta == null) {
            return "sin acta en curso";
        }
        String m = acta.motivoNoAceptableSalvoVerificacionFinal();
        return m != null ? m : motivoAceptarExtra();
    }

    public boolean puedeRechazar() {
        return acta != null && !acta.cerrada();
    }

    // ----------------------------------------------------------------- acciones

    /** Boton "Leer batería" (QA-3612-05): una lectura nueva que puede levantar el bloqueo. */
    public String leerBateria() throws IOException, InterruptedException {
        Bateria.Lectura l = bateria();
        return l.texto;
    }

    private Bateria.Lectura bateria() throws IOException, InterruptedException {
        Bateria.Lectura l = ops.bateria();
        ultimaBateria = l;
        if (campana != null && !campana.cerrada()) {
            campana.anotarBateria(reloj.ahoraIso(), l.n, l.texto);
        }
        if (acta != null && !acta.cerrada()) {
            acta.dato("batería", l.texto);
            if (acta.dato("batería al inicio") == null) {
                acta.dato("batería al inicio", l.texto);
            }
            String min = acta.dato("batería mínima (n)");
            int nm = min == null ? Integer.MAX_VALUE : Integer.parseInt(min.replaceAll("[^0-9]", "").isEmpty() ? "0"
                    : min.replaceAll("[^0-9]", ""));
            int n = l.n == null ? 0 : l.n;
            if (n < nm) {
                acta.dato("batería mínima (n)", String.valueOf(n));
            }
        }
        if (l.aviso && !l.bloqueaEscrituras) {
            operador.preguntar("Batería baja", l.texto + "\nCambie la batería en cuanto pueda.", "Entendido");
        }
        return l;
    }

    private String entrar() throws IOException, InterruptedException {
        String m = ops.entrar(pin);
        if (m != null) {
            pin = null;   // QA-3613-02: cualquier fallo de #L hace que la pantalla vuelva a pedir el PIN
        }
        return m;
    }

    /** QA-3613-05: un acta retomada (p. ej. de la 3.6.12) recibe los datos obligatorios que le falten. */
    private void completarDatos() {
        if (acta == null || acta.cerrada()) {
            return;
        }
        if (acta.dato("código 5") == null) {
            TablaCalibracion.Fila f5 = fila('5');
            acta.dato("código 5", f5.escribible() ? f5.origen : f5.aviso);
        }
        if (acta.dato("decisiones") == null) {
            acta.dato("decisiones", decisiones.texto(equipo()));
        }
    }

    /**
     * Boton "Calibrar" (o "Continuar"): escribe los codigos marcados y resuelve lo que el acta tenga a
     * medias. Devuelve el texto para el operador.
     */
    /**
     * 3.6.17 (F-01): un acta abierta en la que no se escribio ningun codigo (bateria a 0, T-C41 que falla, apagado
     * cancelado) no bloquea: se cierra sola como RECHAZADA "acta vacia" y se dice. "" si no habia.
     */
    private String descartarActaVacia(String por) throws IOException {
        if (acta == null || acta.cerrada() || !acta.codigos().isEmpty() || acta.escribiendo() != 0
                || acta.rechazoPendiente() != null) {
            return "";
        }
        acta.rechazar(reloj.ahoraIso(), "acta vacía: no se escribió ningún código (" + por + "); se descarta sola");
        almacen.cerrar(acta);
        acta = null;
        return " El acta quedó vacía y se descartó sola: vuelva a pulsar cuando esté resuelto.";
    }

    public String calibrar(Set<Character> seleccion, String nombre, String nota) throws IOException, InterruptedException {
        descartarActaVacia("al empezar");
        String r = calibrarInterno(seleccion, nombre, nota);
        return r + descartarActaVacia(r.length() > 120 ? r.substring(0, 120) + "..." : r);
    }

    private String calibrarInterno(Set<Character> seleccion, String nombre, String nota) throws IOException, InterruptedException {
        Set<Character> sel = seleccion == null ? new HashSet<>() : new HashSet<>(seleccion);
        String pv = motivoPrevias();
        if (pv != null) {
            return "No se calibra: " + pv;
        }
        if (acta != null && acta.rechazoPendiente() != null) {
            // P14-06: con un rechazo pendiente no se continua la calibracion.
            return "Hay un rechazo pendiente (" + acta.rechazoPendiente() + "): vuelva a pulsar Rechazar, o ciérrela sin "
                    + "restaurar, firmándola con el PIN de administrador. No se calibra.";
        }
        for (char k : sel) {
            Acta vig = aceptadaVigente(k);
            if (vig != null && serieAnuladaDe(vig, k) == null && (acta == null || acta.codigo(k) == null)) {
                return "El código " + k + " ya tiene un acta ACEPTADA vigente: no se vuelve a escribir.";
            }
        }
        Map<Character, Plan> planes = new LinkedHashMap<>();
        for (char k : Fabrica.CODIGOS) {
            if (!sel.contains(k)) {
                continue;
            }
            Plan p = plan(k, null);
            if (!p.escribible()) {
                return "El código " + k + " no se escribe: " + p.motivoNo;
            }
            String mc = motivoCodigo(k);
            if (mc != null) {
                return mc;
            }
            planes.put(k, p);
        }
        for (Plan p : planes.values()) {
            if (p.fila.solo && (planes.size() > 1 || (acta != null && !acta.codigos().isEmpty() && acta.codigo(p.k) == null))) {
                return "El código " + p.k + " va solo, en un acta propia (P12 §6.3): desmarque los demás o acabe el acta en curso.";
            }
        }
        if (acta != null) {
            for (Acta.Codigo c : acta.codigos()) {
                TablaCalibracion.Fila fc = fila(c.k);
                if (fc != null && fc.solo && !planes.isEmpty() && !planes.containsKey(c.k)) {
                    return "El acta en curso es del código " + c.k + ", que va solo: acéptela o recházela antes.";
                }
            }
        }
        if (!planes.isEmpty() && (nombre == null || nombre.trim().isEmpty() || nota == null || nota.trim().isEmpty())) {
            return "Escriba el nombre y la nota de la conformidad.";
        }
        String e = entrar();
        if (e != null) {
            return e;
        }
        if (acta == null) {
            abrirActa();
        }
        completarDatos();
        String conformidad = "";
        if (!planes.isEmpty()) {
            conformidad = nombre.trim() + ". " + nota.trim();
            for (Plan p : planes.values()) {
                // QA-3613-08: una conformidad por codigo, con su dispensa (si la hay) y quien la decidio.
                acta.conformidad(nombre.trim() + ": código " + p.k + ". " + nota.trim()
                        + (p.fila.dispensa.isEmpty() ? "" : " | dispensa " + p.fila.dispensa + ": " + p.fila.origen));
            }
        }
        // T-C41: los heredados, antes de tocar nada.
        escribiendoAhora.clear();
        escribiendoAhora.addAll(planes.keySet());
        String t41 = heredados();
        if (t41 != null) {
            return t41;
        }
        operador.progreso("Batería antes de la calibración...");
        Bateria.Lectura b = bateria();
        if (b.bloqueaEscrituras) {
            return b.texto + " No se escribe nada.";
        }
        if (acta.escribiendo() != 0) {
            String r = resolverCorte();
            if (r != null) {
                return r;
            }
        }
        // Lo que el acta tiene a medias: restauraciones sin verificar, dos NO CONFORME, re-medidas.
        for (Acta.Codigo c : new ArrayList<>(acta.codigos())) {
            if (c.resuelto()) {
                continue;
            }
            String r = c.restauracionFallida != null || c.debeRestaurarse() ? restaurarCodigo(c.k) : remedida(c.k);
            if (r != null) {
                return r;
            }
        }
        for (Plan p : planes.values()) {
            String r = escribir(p, conformidad(p, "Conformidad de " + conformidad));
            if (r != null) {
                return r;
            }
            r = remedida(p.k);
            if (r != null) {
                return r;
            }
        }
        return acta.motivoNoAceptable(false) == null
                ? "Códigos escritos y verificados. Ahora: persistencia (apagar y encender) y después aceptar."
                : "Sin cambios pendientes: " + acta.motivoNoAceptable(false);
    }

    private static String conformidad(Plan p, String general) {
        String t = general;
        if (!p.propuesta.incumplimientos.isEmpty()) {
            t += " | Dispensa " + p.fila.dispensa + " (" + p.fila.origen + "): " + String.join("; ", p.propuesta.incumplimientos);
        }
        return t;
    }

    private void abrirActa() throws IOException {
        acta = new Acta(campana.serieConHistoria(), campana.mac, ctx.firmware, Remedida3611.K, Remedida3611.M, 1, reloj.ahoraIso());
        acta.tabla = TablaCalibracion.VERSION;
        almacen.adjuntar(acta);
        acta.dato("md5 de la cola", cola.md5);
        acta.dato("banco", campana.colaTipo());
        Anclas.Valor sr = sRep();
        acta.dato("s_rep", sr == null ? "" : sr.texto);
        acta.dato("app", ctx.app);
        acta.dato("decisiones", decisiones.texto(equipo()));
        TablaCalibracion.Fila f5 = fila('5');
        acta.dato("código 5", f5.escribible() ? f5.origen : f5.aviso);
        TablaCalibracion.Fila fb = fila('b');
        acta.dato("código b", fb.escribible() ? fb.origen + ". " + fb.aviso : fb.aviso);
    }

    /** Mascara de #V# con los bits de los heredados y de lo certificado, y marca CAL (P11-M6). */
    private String motivoMascara(Tramas.InfoVersion v, List<Character> requeridos) {
        if (v == null) {
            return "sin respuesta válida a #V#";
        }
        if (!v.tieneMascara()) {
            return "#V# sin máscara";
        }
        if (!requeridos.isEmpty() && !"CAL".equals(v.marca)) {
            return "#V# dice " + v.marca + " y debería decir CAL";
        }
        for (char k : requeridos) {
            if (!v.codigoAjustado(Fabrica.indice(k))) {
                return String.format(Locale.US, "la máscara de #V# (%04X) no tiene el código %c", v.mascara, k);
            }
        }
        return null;
    }

    /** Coteja los heredados: null si todos coinciden; si no, el motivo. Anota el resultado. */
    private String cotejarHeredados(StringBuilder t) throws IOException, InterruptedException {
        Map<Character, String> her = heredadosTodos();
        String mal = null;
        for (Map.Entry<Character, String> e : her.entrySet()) {
            char k = e.getKey();
            Ecuacion esperado = Tramas.parsearG(e.getValue(), k);
            Ecuacion g = ops.leerG(k);
            boolean igual = g != null && esperado != null && g.igualFloat32(esperado, Ecuacion.ULP_G);
            t.append("#G,").append(k).append(igual ? " igual al acta; " : " DISTINTO del acta (" + g + "); ");
            if (!igual && mal == null) {
                mal = "el código " + k + " no coincide con " + origenHeredados();
            }
        }
        return mal;
    }

    /** T-C41 / P11-M6: al empezar, #V# y #G de los heredados frente al acta anterior; y #E. */
    private String heredados() throws IOException, InterruptedException {
        if (acta.dato("heredados (T-C41)") != null) {
            return null;
        }
        Map<Character, String> her = heredadosTodos();
        if (her.isEmpty()) {
            acta.dato("heredados (T-C41)", "sin códigos heredados de otra acta");
            return null;
        }
        // P12 §6.2: T-C41 empieza apagando y encendiendo el equipo; tiene que verse caer el enlace. Dentro de una
        // pulsacion de "Calibrar todo" (P14-01), la persistencia del acta anterior ya fue ese apagado, y se anota.
        if (sesionTodo && encendidoReciente) {
            operador.progreso("T-C41: el equipo se apagó y encendió en la persistencia anterior de esta sesión");
            acta.dato("T-C41 apagado", "el de la persistencia del acta del código " + encendidoPor
                    + " (misma pulsación de Calibrar todo, sin escrituras en medio)");
        } else if (!operador.apagarYEncender()) {
            acta.dato("heredados (T-C41) FALLA", "no se vio caer y volver el enlace al apagar y encender");
            return "T-C41: hay que apagar y encender el equipo antes de empezar y no se vio caer el enlace. No se calibra.";
        }
        else {
            acta.dato("T-C41 apagado", "propio");
        }
        String e0 = entrar();
        if (e0 != null) {
            return "T-C41, tras encender: " + e0;
        }
        operador.progreso("Comprobando los códigos heredados (T-C41)...");
        Tramas.InfoVersion v = ops.leerV();
        StringBuilder t = new StringBuilder("#V# ").append(Ops.texto(v)).append("; ");
        String mal = motivoMascara(v, new ArrayList<>(her.keySet()));
        String g = cotejarHeredados(t);
        if (mal == null) {
            mal = g;
        }
        if (mal == null) {
            for (Map.Entry<Character, String> e : her.entrySet()) {
                String porE = ops.comprobarPorE(e.getKey(), Tramas.parsearG(e.getValue(), e.getKey()));
                t.append("#E,").append(e.getKey()).append(porE == null ? " coincide; " : " " + porE + "; ");
                if (porE != null && mal == null) {
                    mal = porE;
                }
            }
        }
        if (mal != null) {
            acta.dato("heredados (T-C41) FALLA", t + " -> " + mal);
            return "T-C41 FALLA: " + mal + ". No se calibra: el equipo no tiene lo que dicen " + origenHeredados()
                    + "; avise a Diego.";
        }
        acta.dato("heredados (T-C41)", "OK frente a " + origenHeredados() + ": " + t);
        return null;
    }

    /** RF-APP-37: resuelve un corte entre #S y la relectura. null si se puede seguir. */
    private String resolverCorte() throws IOException, InterruptedException {
        char k = acta.escribiendo();
        operador.progreso("Resolviendo el corte durante #S," + k + "...");
        Ecuacion g = ops.leerG(k);
        if (g == null) {
            return "Corte durante #S," + k + ": el equipo no responde a #G. Sigue sin resolver; vuelva a intentarlo.";
        }
        if (g.igualFloat32(acta.escribiendoEnviada(), Ecuacion.ULP_S)) {
            String porE = ops.comprobarPorE(k, acta.escribiendoEnviada());
            if (porE == null) {
                acta.escrito(k, Tramas.tramaG(k, g), g, acta.escribiendoOscuro(), acta.escribiendoMetodo(),
                        acta.escribiendoConformidad());
                acta.dato("#V# posterior", Ops.texto(ops.leerV()));
                return null;
            }
            Ops.Restauracion r = ops.restaurar(k, acta.escribiendoAnterior());
            if (!r.ok) {
                return "Corte durante #S," + k + ": " + porE + ". RESTAURACIÓN NO VERIFICADA (" + r.texto
                        + "): sigue sin resolver.";
            }
            acta.sinEscribir(k, "corte durante #S: " + porE + "; " + r.texto);
            return "Corte durante #S," + k + ": " + porE + ". Restaurado. Vuelva a marcar el código para escribirlo.";
        }
        if (g.igualFloat32(acta.escribiendoAnterior(), Ecuacion.ULP_G)) {
            String conf = acta.escribiendoConformidad();
            acta.sinEscribir(k, "corte durante #S: el #S no entró; el código sigue como estaba");
            int q = operador.preguntar("El #S del código " + k + " no entró",
                    "El código " + k + " sigue como estaba. ¿Repetir la escritura ahora (misma conformidad)?",
                    "Repetir", "No");
            if (q == 0) {
                Plan p = plan(k, g);
                if (!p.escribible()) {
                    return "No se puede repetir el código " + k + ": " + p.motivoNo;
                }
                String r = escribir(p, conf);
                return r != null ? r : remedida(k);
            }
            return "El #S del código " + k + " no entró; no se ha repetido.";
        }
        Ops.Restauracion r = ops.restaurar(k, acta.escribiendoAnterior());
        if (!r.ok) {
            return "Corte durante #S," + k + ": la curva no es ni la enviada ni la anterior. RESTAURACIÓN NO VERIFICADA ("
                    + r.texto + "): sigue sin resolver.";
        }
        acta.sinEscribir(k, "corte durante #S: curva desconocida; " + r.texto);
        return "Corte durante #S," + k + ": la curva no era ni la enviada ni la anterior; restaurada. Vuelva a marcarlo.";
    }

    /** #S del plan. null si fue bien; si no, el motivo para parar. */
    private String escribir(Plan p, String conformidad) throws IOException, InterruptedException {
        char k = p.k;
        String no = acta.motivoNoEscribir(k);
        if (no != null) {
            return no;
        }
        operador.progreso("Código " + k + ": batería antes de #S...");
        Bateria.Lectura b = bateria();
        if (b.bloqueaEscrituras) {
            return b.texto + " No se escribe el código " + k + ".";
        }
        String e0 = entrar();   // QA-3612-13: el modo administrador caduca a los 10 min
        if (e0 != null) {
            return e0;
        }
        Tramas.TramaS ts = Tramas.tramaS(k, p.propuesta.ajuste.ecuacion);
        if (ts == null || Asistente.criterioFirmwareS(ts.enviada) != null) {
            return "El código " + k + " no pasa el criterio de #S.";
        }
        Ecuacion anterior = ops.leerG(k);
        if (anterior == null) {
            return "No se pudo leer el estado anterior del código " + k + " (#G): no se escribe.";
        }
        if (p.anclada()) {
            acta.dato("oscuro " + k, p.origenOscuro);
        }
        acta.dato("patrones " + k, p.patrones);
        acta.dato("series " + k, seriesDe(k, p.fila));   // QA-3614-03: que series usa este codigo (y su OSCURO)
        Anclas.Valor srk = sRep(k);
        acta.dato("s_rep " + k, srk == null ? "" : srk.texto);   // C-P14-2: de donde sale
        acta.dato("protocolo banco " + k, protocoloDe(k, p.fila));   // B3: con que protocolo se midio el banco
        acta.dato("rango " + k, rango(k, p));
        String metodo = p.propuesta.metodo + "; tabla " + p.fila.texto();
        String osc = p.oscuroTexto(ts.enviada);
        operador.progreso("Código " + k + ": #S...");
        encendidoReciente = false;
        acta.escribiendo(k, anterior, ts.enviada, metodo, osc, conformidad);
        Cliente.Respuesta r = ops.escribir(ts.texto);
        Ecuacion e = ops.leerG(k);
        boolean igual = e != null && e.igualFloat32(ts.enviada, Ecuacion.ULP_S);
        String porE = igual ? ops.comprobarPorE(k, ts.enviada) : "la relectura #G no coincide con lo enviado";
        acta.dato("#V# posterior", Ops.texto(ops.leerV()));
        if (Tramas.esOk(r.trama) && igual && porE == null) {
            acta.escrito(k, Tramas.tramaG(k, e), e, osc, metodo, conformidad);
            return null;
        }
        String t = "#S," + k + " -> " + r.describir() + (porE == null ? "" : "; " + porE);
        Ops.Restauracion res = ops.restaurar(k, anterior);
        if (!res.ok) {
            return "Escritura del código " + k + " fallida (" + t + ") y RESTAURACIÓN NO VERIFICADA (" + res.texto
                    + "). El #S queda sin resolver: vuelva a pulsar Calibrar para reintentarlo.";
        }
        acta.sinEscribir(k, t + "; " + res.texto);
        return "Escritura del código " + k + " fallida: " + t + ". Restaurado.";
    }

    /** P11-M1: restauracion verificada, o el codigo queda sin resolver. */
    private String restaurarCodigo(char k) throws IOException, InterruptedException {
        Acta.Codigo c = acta.codigo(k);
        operador.progreso("Código " + k + ": restaurando la curva anterior...");
        String e0 = entrar();
        if (e0 != null) {
            return e0;
        }
        encendidoReciente = false;
        Ops.Restauracion r = ops.restaurar(k, c.anterior);
        acta.dato("#V# posterior", Ops.texto(ops.leerV()));
        if (r.ok) {
            acta.restaurado(k, r.texto);
            boolean alguno = false;
            for (Acta.Codigo x : acta.codigos()) {
                alguno |= x.conforme();
            }
            return "Código " + k + " restaurado tras dos re-medidas no conformes: " + r.texto + ". La secuencia se detiene."
                    + (alguno ? "" : " El acta no tiene ningún código conforme: pulse Rechazar.");
        }
        acta.restauracionFallida(k, r.texto);
        return "RESTAURACIÓN NO VERIFICADA del código " + k + ": " + r.texto
                + ". El acta no se puede aceptar; vuelva a pulsar Calibrar para reintentarla.";
    }

    /**
     * Re-medida (RF-CAL-39 con P10-C1): K x M, 'e' y el codigo alternados. Colocaciones no validas
     * se registran y se repiten. Una repeticion como maximo; si falla, restaurar (verificado).
     */
    public static final int MAX_NO_VALIDAS = 10;

    private String remedida(char k) throws IOException, InterruptedException {
        TablaCalibracion.Fila f = fila(k);
        Patron pat = campana.patron(f.remedida);
        Campana.Serie banco = pat == null ? null : campana.elegida(pat.nombre);
        if (banco == null) {
            return "Falta la serie del banco de " + f.remedida + " (re-medida del " + k + ").";
        }
        double xBanco = banco.media();
        int kBanco = Math.max(1, banco.colocaciones().size());
        Anclas.Valor sr = sRep(k);
        int noValidas = 0;
        while (true) {
            Acta.Codigo c = acta.codigo(k);
            if (c.conforme()) {
                return null;
            }
            if (c.debeRestaurarse()) {
                return restaurarCodigo(k);
            }
            int q = operador.preguntar("Re-medida del código " + k + (c.validos() == 1 ? " (repetición)" : ""),
                    "Coloque " + pat.nombre + " (" + pat + ") y pulse OK. " + Remedida3611.K + " colocaciones × "
                            + Remedida3611.M + " pares ('e' y código " + k + ").", "OK", "Parar aquí");
            if (q != 0) {
                return "Parado en la re-medida del código " + k + ". El acta queda a medias.";
            }
            List<double[]> xs = new ArrayList<>();
            List<double[]> rs = new ArrayList<>();
            while (xs.size() < Remedida3611.K) {
                operador.progreso("Código " + k + ": colocación " + (xs.size() + 1) + " de " + Remedida3611.K + "...");
                double[] xe = new double[Remedida3611.M];
                double[] rk = new double[Remedida3611.M];
                String falla = null;
                Double va;
                try {
                    // RTV 1.0: x con la trama del protocolo ('e' en la V3.6, "#X,k#" en la V4.6).
                    va = ops.medirX(k);
                    for (int i = 0; i < Remedida3611.M && falla == null; i++) {
                        Double vx = ops.medirX(k);
                        Integer vr = ops.medirR(k);
                        if (vx == null || vr == null) {
                            falla = "par " + (i + 1) + " sin respuesta";
                        } else {
                            xe[i] = vx;
                            rk[i] = vr;
                        }
                    }
                } catch (IOException ex) {
                    // T-S09: la colocacion interrumpida queda en el acta (no cuenta).
                    acta.intento(k, reloj.ahoraIso(), "INTERRUMPIDA", "colocación " + (xs.size() + 1)
                            + " cortada: " + ex.getMessage());
                    throw ex;
                }
                if (falla == null) {
                    falla = Remedida3611.colocacion(pat.nombre, va == null ? Double.NaN : va, xBanco, xe, rk, c.leida);
                }
                if (falla != null) {
                    acta.intento(k, reloj.ahoraIso(), "NO_VALIDA", falla);
                    if (++noValidas >= MAX_NO_VALIDAS) {
                        // O-14: sin limite, un operador que pulsa OK sin corregir no acaba nunca.
                        return "Demasiadas colocaciones no válidas seguidas (" + MAX_NO_VALIDAS + "): se para la re-medida del "
                                + "código " + k + ". Revise el patrón y el apoyo, y vuelva a pulsar Calibrar.";
                    }
                    int r = operador.preguntar("Colocación no válida", falla + "\nVuelva a colocar " + pat.nombre
                            + " y pulse OK.", "OK", "Parar aquí");
                    if (r != 0) {
                        return "Parado en la re-medida del código " + k + ". El acta queda a medias.";
                    }
                    continue;
                }
                xs.add(xe);
                rs.add(rk);
                if (xs.size() < Remedida3611.K) {
                    int r = operador.preguntar("Levante y apoye (" + (xs.size() + 1) + " de " + Remedida3611.K + ")",
                            "Levante el equipo y vuelva a apoyarlo sobre " + pat.nombre + ".", "OK", "Parar aquí");
                    if (r != 0) {
                        return "Parado en la re-medida del código " + k + ". El acta queda a medias.";
                    }
                }
            }
            Remedida3611.Resultado res = !"RF-CAL-18".equals(f.reglaRemedida)
                    ? Remedida3611.evaluar(pat.nombre, pat.valor, xs, rs, xBanco, kBanco,
                    sr == null ? Double.NaN : sr.valor, sr == null ? "" : sr.texto)
                    // Codigo con dispensa de Diego (b, PA-24): RF-CAL-18 frente a la curva escrita decide; el
                    // incumplimiento frente al certificado queda escrito en el acta.
                    : Remedida3611.evaluarConDispensa(pat.nombre, pat.valor, xs, rs, xBanco, kBanco,
                    sr == null ? Double.NaN : sr.valor, sr == null ? "" : sr.texto, c.leida, f.dispensa,
                    f.alcanceDe("RF-CAL-14", pat.nombre));
            acta.intento(k, reloj.ahoraIso(), res.estado, res.texto);
            if ("NO_EVALUABLE".equals(res.estado)) {
                return res.texto;
            }
            if ("NO_CONFORME".equals(res.estado) && acta.codigo(k).puedeRepetir()) {
                operador.preguntar("Re-medida NO CONFORME", res.texto + "\nQueda una repetición.", "OK");
            }
        }
    }

    /** Boton "Persistencia": apagar, encender, reconectar y releer (RF-APP-46). */
    public String persistencia() throws IOException, InterruptedException {
        if (!puedePersistencia()) {
            return acta == null ? "Sin acta en curso." : "Todavía no: " + acta.motivoNoAceptable(false);
        }
        completarDatos();
        if (!operador.apagarYEncender()) {
            return "Persistencia NO válida: no se vio caer y volver el enlace (P12 §5.1). Apague de verdad el equipo y repítala.";
        }
        String e0 = entrar();
        if (e0 != null) {
            return "Reconectado, pero " + e0;
        }
        Tramas.InfoVersion v = ops.leerV();
        StringBuilder t = new StringBuilder("#V# ").append(Ops.texto(v)).append("; ");
        String mal = motivoMascara(v, requeridos());
        for (Acta.Codigo c : acta.certificados()) {
            Ecuacion g = ops.leerG(c.k);
            boolean igual = g != null && g.igualFloat32(c.leida, Ecuacion.ULP_G);
            String porE = ops.comprobarPorE(c.k, c.leida);
            t.append("código ").append(c.k).append(": #G ").append(igual ? "igual" : "DISTINTO")
                    .append(", #E ").append(porE == null ? "coincide" : porE).append("; ");
            if (mal == null && (!igual || porE != null)) {
                mal = "el código " + c.k + " no se conserva tras apagar";
            }
        }
        String h = cotejarHeredados(t);
        if (mal == null) {
            mal = h;
        }
        acta.persistencia(mal == null, t.toString() + (mal == null ? "" : " -> " + mal));
        acta.dato("#V# posterior", Ops.texto(v));
        return (mal == null ? "Persistencia OK: " : "Persistencia FALLA (" + mal + "): ") + t;
    }

    /** Codigos cuyo bit de mascara tiene que estar: heredados y certificados. */
    private List<Character> requeridos() {
        List<Character> l = new ArrayList<>(heredadosTodos().keySet());
        for (Acta.Codigo c : acta.certificados()) {
            if (!l.contains(c.k)) {
                l.add(c.k);
            }
        }
        return l;
    }

    /** Boton "Aceptar y grabar fecha": verificacion final (dentro), bateria, #SC y #GC. */
    public String aceptar() throws IOException, InterruptedException {
        completarDatos();
        if (!puedeAceptar()) {
            return acta == null ? "Sin acta en curso." : "No aceptable: "
                    + (motivoPrevias() != null ? motivoPrevias() : motivoNoAceptar());
        }
        String e0 = entrar();
        if (e0 != null) {
            return e0;
        }
        Bateria.Lectura b = bateria();
        if (b.bloqueaEscrituras) {
            return b.texto + " No se graba la fecha.";
        }
        Tramas.InfoVersion v = ops.leerV();
        StringBuilder t = new StringBuilder("#V# ").append(Ops.texto(v)).append("; ");
        String mal = motivoMascara(v, requeridos());
        for (Acta.Codigo c : acta.certificados()) {
            Ecuacion g = ops.leerG(c.k);
            boolean igual = g != null && g.igualFloat32(c.leida, Ecuacion.ULP_G);
            t.append("código ").append(c.k).append(igual ? " igual a lo certificado; " : " DISTINTO de lo certificado; ");
            if (mal == null && !igual) {
                mal = "el código " + c.k + " no es el certificado";
            }
        }
        String h = cotejarHeredados(t);
        if (mal == null) {
            mal = h;
        }
        acta.verificacionFinal(mal == null, t.toString() + (mal == null ? "" : " -> " + mal));
        acta.dato("#V# posterior", Ops.texto(v));
        if (mal != null) {
            return "Verificación final FALLA: " + mal + ". El acta no se acepta y no se graba la fecha.";
        }
        String hoy = reloj.hoy();
        scEnUltimoAceptar = false;
        // P14-04: #SC una vez. Si el equipo ya tiene la fecha de hoy (otra acta de esta sesion), no se reescribe.
        Cliente.Respuesta g0 = ops.pedir("#GC#");
        String antes = g0.valida() ? Calibracion.fechaDe(g0.trama) : null;
        if (hoy.equals(antes)) {
            acta.dato("#SC", "no se reescribe: el equipo ya tiene la fecha de hoy (" + g0.describir() + ")");
        } else {
            Cliente.Respuesta r = ops.escribir("#SC," + hoy + "#");
            scEnUltimoAceptar = true;         // una escritura: el siguiente T-C41 apaga el suyo (F-04)
            encendidoReciente = false;
            Cliente.Respuesta g = ops.pedir("#GC#");
            String leida = g.valida() ? Calibracion.fechaDe(g.trama) : null;
            if (!Tramas.esOk(r.trama) || !hoy.equals(leida)) {
                return "#SC no quedó grabada (#SC -> " + r.describir() + ", #GC# -> " + g.describir()
                        + "): el acta NO se cierra. Reintente.";
            }
        }
        // P14-B02: el ZIP de soporte se exporta antes de cerrar y el acta cita su SHA-256.
        String sha = almacen.soporteSha256(acta);
        if (sha != null) {
            acta.dato("ZIP de soporte (SHA-256)", sha);
        }
        acta.aceptar(reloj.ahoraIso(), hoy, true, true);
        almacen.cerrar(acta);
        Acta cerrada = acta;
        acta = null;
        operador.actaAceptada(cerrada);
        return "Acta ACEPTADA; fecha " + hoy + " (vence " + Calibracion.vencimiento(hoy) + "). " + cerrada.equipo;
    }

    /**
     * Boton "Rechazar". Con restaurar = true (QA-3612-17) devuelve cada codigo escrito a su curva
     * anterior, verificado; lo que no se pueda restaurar queda dicho en el acta.
     */
    public String rechazar(String motivo) throws IOException, InterruptedException {
        if (acta == null) {
            return "Sin acta en curso.";
        }
        StringBuilder t = new StringBuilder();
        StringBuilder fallos = new StringBuilder();
        {
            // P12 §6.8: rechazar SIEMPRE restaura lo escrito, verificado con #G.
            String e0 = entrar();
            if (e0 != null) {
                return e0 + " No se ha rechazado.";
            }
            encendidoReciente = false;   // P14-01: restaurar es escribir
            if (acta.escribiendo() != 0) {
                // QA-3613-01: el codigo con un #S sin resolver tambien se restaura.
                char k = acta.escribiendo();
                Ops.Restauracion r = ops.restaurar(k, acta.escribiendoAnterior());
                if (r.ok) {
                    acta.sinEscribir(k, "al rechazar, con el #S sin resolver: " + r.texto);
                    t.append("código ").append(k).append(" (#S sin resolver) restaurado; ");
                } else {
                    t.append("código ").append(k).append(" (#S sin resolver) SIN RESTAURAR (").append(r.texto).append("); ");
                    fallos.append(k);
                }
            }
            for (Acta.Codigo c : new ArrayList<>(acta.codigos())) {
                if (c.restaurado != null || c.anterior == null) {
                    continue;
                }
                Ops.Restauracion r = ops.restaurar(c.k, c.anterior);
                if (r.ok) {
                    acta.restaurado(c.k, "al rechazar: " + r.texto);
                } else {
                    acta.restauracionFallida(c.k, "al rechazar: " + r.texto);
                    fallos.append(c.k);
                }
                t.append("código ").append(c.k).append(r.ok ? " restaurado; " : " SIN RESTAURAR (" + r.texto + "); ");
            }
        }
        if (fallos.length() > 0) {
            // QA-3613-01 / P13-05: con una restauracion NO verificada el acta no se cierra: el equipo puede
            // tener una curva sin acta. Queda abierta, con la restauracion fallida anotada, para reintentarlo.
            // P14-05/06: queda un RECHAZO PENDIENTE: no se escribe nada mas ni se continua; solo Rechazar otra vez o
            // cerrar sin restaurar, firmado con el PIN de administrador (P14-10). El motivo queda anotado.
            acta.rechazoPendiente(reloj.ahoraIso(), (motivo == null ? "" : motivo) + " | " + t);
            return "NO se rechaza: RESTAURACIÓN NO VERIFICADA del código " + fallos + " (" + t + "). El acta queda con un "
                    + "rechazo pendiente: vuelva a pulsar Rechazar para reintentarlo; si no se resuelve, avise a Diego "
                    + "(\"Cerrar sin restaurar\", con su firma).";
        }
        acta.rechazar(reloj.ahoraIso(), (motivo == null ? "" : motivo) + (t.length() == 0 ? "" : " | " + t));
        almacen.cerrar(acta);
        acta = null;
        return "Acta rechazada. " + t;
    }

    /**
     * P14-10: salida terminal de un rechazo pendiente. Cierra el acta como RECHAZADA SIN RESTAURAR, con los
     * codigos en estado desconocido. Solo con un rechazo pendiente.
     *
     * RTV 1.0.0-rc3 (decision FIRMA-ACTA de Diego): firma quien tenga el PIN de admin, y el acta dice SU nombre.
     * El nombre es parametro y no un campo de la clase a proposito: esta pantalla puede abrirse en una sesion en
     * la que nadie ha calibrado, y firmar con el nombre de quien calibro seria la misma atribucion falsa que
     * esta decision cierra.
     */
    public String cerrarSinRestaurar(String clave, String motivo, String nombre)
            throws IOException, InterruptedException {
        if (acta == null || acta.rechazoPendiente() == null) {
            return "Solo se cierra sin restaurar un acta con un rechazo pendiente.";
        }
        if (nombre == null || nombre.trim().isEmpty()) {
            return FALTA_NOMBRE;
        }
        String firmante = firma(clave, nombre);
        if (firmante == null) {
            return "Cerrar sin restaurar lo firma quien tenga el PIN de administrador del equipo. "
                    + "El PIN tecleado no vale: no se cierra.";
        }
        StringBuilder desconocidos = new StringBuilder();
        if (acta.escribiendo() != 0) {
            desconocidos.append(acta.escribiendo()).append(' ');
        }
        for (Acta.Codigo c : acta.codigos()) {
            if (c.restaurado == null) {
                desconocidos.append(c.k).append(' ');
            }
        }
        acta.rechazar(reloj.ahoraIso(), "RECHAZADA SIN RESTAURAR. " + firmante.trim() + ". Motivo: "
                + (motivo == null ? "" : motivo) + " | códigos en estado desconocido: " + desconocidos.toString().trim()
                + " | " + acta.rechazoPendiente());
        almacen.cerrar(acta);
        acta = null;
        return "Acta cerrada SIN RESTAURAR. " + firmante.trim() + ". Códigos en estado desconocido: "
                + desconocidos.toString().trim() + ": avise a Diego antes de calibrar otra vez.";
    }

    /** RTV 1.0.0-rc3: lo que se responde cuando falta el nombre de quien firma. */
    public static final String FALTA_NOMBRE = "Escriba el nombre de quien firma. El acta lo cita tal cual.";

    /** La empresa que consta en la firma del acta (decision FIRMA-ACTA de Diego, 19-sep-2026). */
    public static final String EMPRESA = "ITVIAL SAS";

    /**
     * RTV 1.0.0-rc3 (decision FIRMA-ACTA): la firma de "Cerrar sin restaurar" y "Liberar". La autoriza el PIN de
     * administrador del equipo, comprobado con #L, y **nada mas**: hasta la rc2 valia tambien una "frase
     * registrada" en la fila FRASE-DIEGO de decisiones.csv, que NUNCA HA EXISTIDO en el asset. Se ha quitado
     * entera: una barrera que el codigo promete y el dato no sostiene es peor que no tenerla, porque el que la
     * lea creera que protege algo.
     *
     * El texto que devuelve es el que el acta cita literalmente:
     * {@code Firmado por "<nombre>", ITVIAL SAS, <AAAA-MM-DD>} — el nombre de quien esta delante, no un literal
     * cableado, y la fecha del dia, del mismo reloj que el resto del acta. null si el PIN no entra.
     */
    private String firma(String clave, String nombre) throws IOException, InterruptedException {
        if (clave == null || clave.trim().isEmpty() || nombre == null || nombre.trim().isEmpty()) {
            return null;
        }
        if (ops.entrar(clave.trim()) != null) {
            return null;
        }
        return firmaDe(Csv.unaLinea(nombre.trim()), reloj.hoy());
    }

    /**
     * La linea de firma que el acta cita literalmente (decision FIRMA-ACTA de Diego):
     * {@code Firmado por "<nombre>", ITVIAL SAS, <AAAA-MM-DD>}.
     *
     * Publica y en un solo sitio a proposito: es un texto que ve el cliente, y quien lo incruste NO debe
     * anteponerle "firmado por" -ya lo dice la propia frase-. En la rc3 los cuatro puntos de incrustacion se
     * lo anteponian y el acta decia "firmado por Firmado por ...".
     *
     * La fecha sale de {@link Reloj#hoy()}, el mismo del que sale la que se graba con #SC (:1476), no de un
     * recorte de ahoraIso(): asi la firma y la fecha de calibracion no pueden discrepar.
     */
    public static String firmaDe(String nombre, String hoy) {
        return "Firmado por \"" + nombre + "\", " + EMPRESA + ", " + hoy;
    }

    /** F-03: motivo del bloqueo tras un cierre SIN RESTAURAR; null si no lo hay. */
    public String bloqueoSinRestaurar() {
        try {
            Acta u = almacen.ultimaCerrada();
            if (u != null && u.sinRestaurarPendiente()) {
                return "La última acta se cerró SIN RESTAURAR: el equipo tiene códigos en estado desconocido. No se "
                        + "calibra hasta que se libere (\"Liberar tras el cierre\", con el PIN de administrador).";
            }
        } catch (IOException e) {
            return "No se pudo leer la última acta cerrada: " + e.getMessage();
        }
        return null;
    }

    /**
     * F-03: se libera el equipo tras un cierre SIN RESTAURAR (queda en el diario de esa acta).
     *
     * RTV 1.0.0-rc3: el nombre es parametro, y aqui la razon se ve mejor que en ningun otro sitio: la linea
     * LIBERADA se anade al diario de un acta ANTERIOR, que pudo cerrarse otro dia y con otra persona delante.
     * Un acta liberada mostrara dos nombres, el de la conformidad de entonces y el de quien libera hoy, y eso
     * es lo correcto.
     */
    public String liberarTrasCierre(String clave, String motivo, String nombre)
            throws IOException, InterruptedException {
        Acta u = almacen.ultimaCerrada();
        if (u == null || !u.sinRestaurarPendiente()) {
            return "No hay ningún cierre SIN RESTAURAR pendiente.";
        }
        if (nombre == null || nombre.trim().isEmpty()) {
            return FALTA_NOMBRE;
        }
        String firmante = firma(clave, nombre);
        if (firmante == null) {
            return "Liberar lo firma quien tenga el PIN de administrador del equipo. El PIN tecleado no vale: "
                    + "no se libera.";
        }
        almacen.anadirAUltimaCerrada(Acta.lineaLiberada(reloj.ahoraIso(), "LIBERADA. " + firmante + ". Motivo: "
                + (motivo == null ? "" : motivo)));
        return "Equipo liberado. " + firmante + ". La nueva acta tomará como curva anterior la que lea con #G.";
    }

    // ------------------------------------------------------ una sola sesion (3.6.15)

    /** Orden de la sesion: el 8 primero (P12 §6.3), luego el b y el 5, luego 3, 4 y 6 si la cobertura lo permite. */
    public static final char[] ORDEN_SESION = {'8', 'b', '5', '3', '4', '6'};

    /**
     * "Calibrar todo": una sola sesion para los codigos marcados, en ORDEN_SESION. Para cada codigo: escribir,
     * re-medir, persistencia (apagar y encender) y aceptar su acta; despues sigue solo con el siguiente. El 8
     * queda ACEPTADO antes de tocar el b o el 5 (condicion del arquitecto). Un acta por codigo. Se para en el
     * primer codigo que no acabe ACEPTADO, y lo dice.
     */
    public String calibrarTodo(Set<Character> seleccion, String nombre, String nota) throws IOException, InterruptedException {
        sesionTodo = true;
        encendidoReciente = false;
        try {
            return calibrarTodoInterno(seleccion, nombre, nota);
        } finally {
            sesionTodo = false;          // P14-01: el atajo de T-C41 no sobrevive a la pulsacion
            encendidoReciente = false;
            escribiendoAhora.clear();
        }
    }

    private String calibrarTodoInterno(Set<Character> seleccion, String nombre, String nota)
            throws IOException, InterruptedException {
        List<Character> orden = new ArrayList<>();
        for (char k : ORDEN_SESION) {
            if (seleccion != null && seleccion.contains(k)) {
                orden.add(k);
            }
        }
        StringBuilder hecho = new StringBuilder();
        StringBuilder saltados = new StringBuilder();
        String vacia = descartarActaVacia("al retomar");
        if (!vacia.isEmpty()) {
            operador.progreso(vacia.trim());
        }
        if (acta != null) {
            if (acta.rechazoPendiente() != null) {
                return "Hay un rechazo pendiente (" + acta.rechazoPendiente() + "): vuelva a pulsar Rechazar. No se calibra.";
            }
            // Primero se termina el acta a medias (QA-3615-04: y se dice).
            char k0 = acta.codigos().isEmpty() ? acta.escribiendo() : acta.codigos().get(0).k;
            String r = calibrar(new HashSet<Character>(), "", "");
            if (acta != null && acta.motivoNoAceptable(false) == null) {
                String a = persistirConfirmarYAceptar(k0);
                if (a != null) {
                    return a;
                }
                hecho.append(k0).append(" ACEPTADO; ");
            } else if (acta != null) {
                return "El acta en curso no está lista: " + r + (acta.motivoNoAceptable(false) != null
                        && acta.motivoNoAceptable(false).contains("ningún código ha quedado conforme")
                        ? " Pulse Rechazar." : "");
            }
            orden.remove(Character.valueOf(k0));
        }
        for (char k : new ArrayList<>(orden)) {
            if (aceptadoVigente(k)) {
                orden.remove(Character.valueOf(k));        // QA-3615-04: ya aceptado; se dice, no se para
                saltados.append(k).append(' ');
            }
        }
        if (orden.isEmpty()) {
            return (hecho.length() == 0 ? "Nada que calibrar." : "Sesión completa: " + hecho)
                    + (saltados.length() == 0 ? "" : " Ya aceptados antes: " + saltados.toString().trim() + ".");
        }
        if (nombre == null || nombre.trim().isEmpty() || nota == null || nota.trim().isEmpty()) {
            return "Escriba el nombre y la nota de la conformidad.";
        }
        // Lo que se puede comprobar antes de empezar (no el T-C41, el PIN ni la bateria, que se ven al hacerlo).
        for (char k : orden) {
            Plan p = plan(k, null, true);
            if (!p.escribible()) {
                return "El código " + k + " no se escribe: " + p.motivoNo + ". No se ha empezado la sesión.";
            }
            TablaCalibracion.Fila f = fila(k);
            if (f.requiereAceptado != 0 && !aceptadoVigente(f.requiereAceptado) && !orden.contains(f.requiereAceptado)) {
                return "El código " + k + " va después del acta ACEPTADA del " + f.requiereAceptado + ": márquelo también. "
                        + "No se ha empezado la sesión.";
            }
        }
        for (char k : orden) {
            operador.progreso("Sesión: código " + k + " (" + (orden.indexOf(k) + 1) + " de " + orden.size() + ")");
            Set<Character> uno = new HashSet<>();
            uno.add(k);
            String r = calibrar(uno, nombre, nota);
            String previo = hecho.length() == 0 ? "" : "Hecho: " + hecho.toString().trim() + " ";
            if (acta == null || acta.motivoNoAceptable(false) != null) {
                return previo + "Código " + k + ": " + r;
            }
            String a = persistirConfirmarYAceptar(k);
            if (a != null) {
                return previo + "Código " + k + ": " + a;
            }
            hecho.append(k).append(" ACEPTADO; ");
        }
        return "Sesión completa: " + hecho + (saltados.length() == 0 ? "" : " Ya aceptados antes: "
                + saltados.toString().trim() + ".");
    }

    /**
     * P14-04: persistencia, y el acta a la vista de quien la acepta: "Aceptar", "Rechazar" o "Parar aquí". null si
     * queda ACEPTADA; si no, el texto.
     */
    private String persistirConfirmarYAceptar(char k) throws IOException, InterruptedException {
        String p = persistencia();
        if (!p.startsWith("Persistencia OK")) {
            return p;
        }
        int q = operador.preguntar("Acta del código " + k, acta.texto() + "\n¿Acepta esta acta? Se graba la fecha de "
                + "calibración (una vez al día).", "Aceptar", "Rechazar", "Parar aquí");
        if (q == 1) {
            String r = rechazar("rechazada al revisar el acta del código " + k);
            return "Acta del código " + k + " rechazada al revisarla: " + r;
        }
        if (q != 0) {
            return "Parado: el acta del código " + k + " espera a que la acepte o la rechace.";
        }
        String a = aceptar();
        if (a.startsWith("Acta ACEPTADA")) {
            // el siguiente T-C41 puede usar este apagado, salvo que despues se escribiera #SC (F-04)
            encendidoReciente = sesionTodo && !scEnUltimoAceptar;
            encendidoPor = k;
            return null;
        }
        return a;
    }

    /**
     * Banco, "Rehacer" (QA-3614-03): BLOQUEO si hay un acta de calibracion en curso; AVISO si el codigo del paso
     * ya tiene un acta ACEPTADA (quedaria apoyada en una serie anulada); null si se puede sin mas.
     */
    public static String bloqueoRehacer(AlmacenActa almacen, BancoCola cola, int orden) throws IOException {
        Acta a = almacen.enCurso();
        if (a != null && !a.cerrada()) {
            return "BLOQUEO: hay un acta de calibración en curso: acéptela o recházela en \"Calibrar\" antes de rehacer.";
        }
        BancoCola.Paso p = cola.paso(orden);
        if (p != null && ("OSCURO".equals(p.tipo) || "A5".equals(p.tipo))) {
            // P14-B08: el OSCURO y la A5 dan el ancla y la s_rep de los codigos de su sesion.
            for (char k : Fabrica.CODIGOS) {
                if (Anclas.sesionDe(cola, k) == p.sesion && almacen.aceptadoAntes(k)) {
                    return "AVISO: el " + p.tipo + " de la sesión " + p.sesion + " dio el ancla o la s_rep del código " + k
                            + ", que ya tiene un acta ACEPTADA. Si lo rehace, habrá que volver a calibrar el " + k + ".";
                }
            }
            return null;
        }
        if (p != null && p.codigo.length() == 1 && almacen.aceptadoAntes(p.codigo.charAt(0))) {
            return "AVISO: el código " + p.codigo + " ya tiene un acta ACEPTADA que usó este banco. Si rehace " + p.patron
                    + ", esa calibración queda apoyada en una serie anulada y habrá que volver a calibrar el " + p.codigo + ".";
        }
        return null;
    }

    // ---------------------------------------------------------- desde Avanzado

    /**
     * Avanzado, "Cambiar serie" (3.6.15, decision SERIE-2 de Diego): nueva serie escrita dos veces, #SN, verificada
     * con #GN, y RENOMBRA en el diario de la campana (anterior, nueva, fecha, operador). La campana, el banco,
     * las actas y las decisiones de la serie anterior siguen valiendo: la identidad es la MAC + el historial.
     */
    public static String renombrarSerie(Canal canal, Campana c, String pin, String nueva, String repetida, String operador,
                                        String fecha) throws IOException, InterruptedException {
        return renombrarSerie(canal, c, pin, nueva, repetida, operador, fecha, false);
    }

    /**
     * 3.6.16: el RENOMBRA se escribe ANTES del #SN (QA-3615-01): si la app muere o el enlace cae entre medias, la
     * campana ya reconoce la serie nueva. Si el #GN# confirma que no cambio, se revierte (RENOMBRA_REVIERTE). Si
     * el equipo ya tiene la serie nueva (un cambio a medias de la 3.6.15), se adopta sin #SN. No se renombra con
     * un acta en curso (P14-S06), ni a una serie de SLV-002 un equipo con otra MAC (QA-3615-09).
     */
    public static String renombrarSerie(Canal canal, Campana c, String pin, String nueva, String repetida, String operador,
                                        String fecha, boolean actaEnCurso) throws IOException, InterruptedException {
        String n = nueva == null ? "" : nueva.trim();
        String mal = Calibracion.motivoSerieInvalida(n);
        if (mal != null) {
            return "Serie no válida: " + mal + ".";
        }
        if (!n.equals(repetida == null ? "" : repetida.trim())) {
            return "Las dos entradas no coinciden: no se graba.";
        }
        if (operador == null || operador.trim().isEmpty()) {
            return "Falta el nombre de quien cambia la serie.";
        }
        if (c == null) {
            return "Sin campaña de este equipo: no se cambia.";
        }
        if (actaEnCurso) {
            return "Hay un acta de calibración en curso: acéptela o recházela antes de cambiar la serie.";
        }
        for (String s : TablaCalibracion.SERIES_SLV002) {
            if (s.equalsIgnoreCase(n) && !TablaCalibracion.MAC_SLV002.equalsIgnoreCase(c.mac)) {
                return "La serie " + n + " es de otro equipo (MAC " + TablaCalibracion.MAC_SLV002 + "): no se graba.";
            }
        }
        Ops ops = new Ops(canal);
        Cliente.Respuesta g0 = ops.pedir("#GN#");
        String anterior = g0.valida() ? Calibracion.serieDe(g0.trama) : null;
        if (anterior == null) {
            return "No se pudo leer la serie actual (#GN#): no se cambia.";
        }
        if (anterior.equals(n)) {
            // Cambio a medias (el #SN entro y el RENOMBRA no), o un REVIERTE que resulto falso (S-02): el equipo ya
            // tiene la serie nueva; se adopta y queda anotado.
            if (!n.equalsIgnoreCase(c.serieActual())) {
                c.renombrar(c.serieActual(), n, fecha, operador.trim() + " (recuperado: el equipo ya decía " + n + ")");
            }
            return "El equipo ya tiene la serie " + n + ": queda anotada en la campaña (" + c.serieConHistoria() + ").";
        }
        if (!c.esSerie(anterior) && !Calibracion.NONE.equals(anterior)) {
            return "La serie actual del equipo (" + anterior + ") no es de la campaña abierta: no se cambia.";
        }
        String e = pin == null ? null : ops.entrar(pin);
        if (e != null) {
            return e;
        }
        c.renombrar(anterior, n, fecha, operador.trim());      // antes del #SN (QA-3615-01)
        Cliente.Respuesta r = ops.escribir("#SN," + n + "#");
        Cliente.Respuesta g = ops.pedir("#GN#");
        String leida = g.valida() ? Calibracion.serieDe(g.trama) : null;
        if (Tramas.esOk(r.trama) && n.equals(leida)) {
            return "Serie cambiada: " + anterior + " -> " + n + " (verificada con #GN#). La campaña sigue siendo la misma.";
        }
        if (anterior.equals(leida)) {
            c.revertirRenombrado(n, anterior, fecha, "#SN -> " + r.describir() + ", #GN# -> " + g.describir());
            return "#SN -> " + r.describir() + ", #GN# -> " + g.describir() + ": la serie NO quedó cambiada (sigue "
                    + anterior + ").";
        }
        return "#SN -> " + r.describir() + ", #GN# -> " + g.describir() + ": no se pudo verificar. La campaña reconoce "
                + "las dos series; vuelva a entrar en Pruebas para leer la serie del equipo.";
    }

    /**
     * Avanzado, "Restaurar este (#F,k#)" / "Restaurar todos (#F,*#)": envia #F y, si hay un acta abierta,
     * la invalida (P10-C5), la haya o no en memoria (P11 §3, caso 3). Estatico: Avanzado no abre flujo.
     */
    public static String fabricaDesdeAvanzado(Canal canal, AlmacenActa almacen, String pin, boolean todos, char k)
            throws IOException, InterruptedException {
        Ops ops = new Ops(canal);
        if (pin != null && !ops.login(pin)) {   // null: Avanzado ya esta en modo administrador
            return "PIN rechazado: no se ha enviado #F.";
        }
        String trama = todos ? "#F,*#" : "#F," + k + "#";
        Cliente.Respuesta r = ops.escribir(trama);
        Acta a = almacen.enCurso();
        String t = trama + " -> " + r.describir();
        if (a != null && !a.cerrada()) {
            a.invalidar(t + " con el acta abierta");
            t += ". El acta en curso queda INVALIDADA: recházela";
        }
        return t;
    }
}
