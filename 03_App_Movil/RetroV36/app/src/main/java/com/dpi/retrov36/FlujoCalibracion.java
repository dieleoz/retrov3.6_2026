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
    }

    /** Donde vive el acta en curso (en la app, en disco: Campanas). */
    public interface AlmacenActa {
        Acta enCurso() throws IOException;

        void adjuntar(Acta a) throws IOException;

        void cerrar(Acta a) throws IOException;

        /** true si el codigo k ya tiene un acta ACEPTADA de este equipo (P12 §6.3: el 8 antes que el b y el 5). */
        boolean aceptadoAntes(char k) throws IOException;
    }

    public interface Reloj {
        String ahoraIso();

        /** AAAA-MM-DD. */
        String hoy();
    }

    /** Lo que la sesion sabe del equipo conectado. */
    public static final class Contexto {
        public boolean conectado;
        public boolean es362;
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

    public FlujoCalibracion(Canal canal, Operador operador, AlmacenActa almacen, BancoCola cola, Campana campana,
                            List<Patron> catalogo, Decisiones decisiones, Contexto ctx, Reloj reloj) throws IOException {
        this.canal = canal;
        this.ops = new Ops(canal);
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
        c.es362 = v != null && "3.6".equals(v.version) && v362;
        c.firmware = v == null ? "sin detectar" : "V" + v.version + " " + v.fecha + (v362 ? " (3.6.2) " : " ") + v.marca
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

    public List<Previa> previas() {
        List<Previa> l = new ArrayList<>();
        l.add(new Previa(ctx.conectado, ctx.conectado ? "Conectado con " + ctx.nombreBT : "Sin conexión: conecte con el equipo"));
        l.add(new Previa(ctx.es362, ctx.es362 ? "Firmware " + ctx.firmware
                : "El firmware no está detectado como 3.6.2 (" + ctx.firmware + "): pase las pruebas del equipo"));
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
        l.add(new Previa(idOk, gn == null ? "Serie del equipo (#GN#) sin leer: pase las pruebas"
                : Calibracion.NONE.equals(gn) ? "El equipo no tiene serie (#GN# = NONE): dé de alta la serie en Avanzado"
                : idOk ? "Serie #GN# " + gn + " = campaña"
                : "La serie #GN# (" + gn + ") no coincide con la campaña (" + (campana == null ? "-" : campana.equipo)
                + "): no se calibra"));
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
            return new Plan(k, f, null, Double.NaN, "", "",
                    "no calibrable: faltan patrones de AJUSTE o RE-MEDIDA del banco (medidos o saltados)");
        }
        if (f.todosLosPasos) {
            String falta = pasosSinHacer(k);
            if (falta != null) {
                return new Plan(k, f, null, Double.NaN, "", "", "exige todos sus pasos del banco hechos (P12 §6.6); faltan "
                        + falta);
            }
        }
        if (f.requiereAceptado != 0 && !ignorarOrden) {
            try {
                if (!almacen.aceptadoAntes(f.requiereAceptado)) {
                    return new Plan(k, f, null, Double.NaN, "", "", "va después de un acta ACEPTADA del código "
                            + f.requiereAceptado + " (P12 §6.3: el " + f.requiereAceptado + " solo y primero)");
                }
            } catch (IOException e) {
                return new Plan(k, f, null, Double.NaN, "", "", "no se pudieron leer las actas anteriores: " + e.getMessage());
            }
        }
        Anclas.Valor osc = Anclas.oscuro(cola, campana, k);
        if (Double.isNaN(osc.valor)) {
            // Sin el oscuro de la sesion no se comprueba P9-B13 ni se ancla: no se escribe (QA-3612-10).
            return new Plan(k, f, null, Double.NaN, osc.texto, "", "sin oscuro de la sesión: " + osc.texto);
        }
        Anclas.Valor sr = sRep();
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
        try {
            return almacen.aceptadoAntes(k);
        } catch (IOException e) {
            return false;
        }
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
                if (p.motivoNo == null && aceptadoSinFallo(f.codigo) && (acta == null || acta.codigo(f.codigo) == null)) {
                    sb.append("\n  Ya tiene un acta ACEPTADA: no se vuelve a escribir en esta sesión");
                    l.add(new Tarjeta(f.codigo, sb.toString(), false, ""));
                    continue;
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
        return motivoPrevias() == null && ((seleccion != null && !seleccion.isEmpty()) || pendientes() > 0);
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
    public String calibrar(Set<Character> seleccion, String nombre, String nota) throws IOException, InterruptedException {
        Set<Character> sel = seleccion == null ? new HashSet<>() : new HashSet<>(seleccion);
        String pv = motivoPrevias();
        if (pv != null) {
            return "No se calibra: " + pv;
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
        Map<Character, String> her = TablaCalibracion.heredados(equipo());
        String mal = null;
        for (Map.Entry<Character, String> e : her.entrySet()) {
            char k = e.getKey();
            Ecuacion esperado = Tramas.parsearG(e.getValue(), k);
            Ecuacion g = ops.leerG(k);
            boolean igual = g != null && esperado != null && g.igualFloat32(esperado, Ecuacion.ULP_G);
            t.append("#G,").append(k).append(igual ? " igual al acta; " : " DISTINTO del acta (" + g + "); ");
            if (!igual && mal == null) {
                mal = "el código " + k + " heredado no coincide con " + TablaCalibracion.origenHeredados(equipo());
            }
        }
        return mal;
    }

    /** T-C41 / P11-M6: al empezar, #V# y #G de los heredados frente al acta anterior; y #E. */
    private String heredados() throws IOException, InterruptedException {
        if (acta.dato("heredados (T-C41)") != null) {
            return null;
        }
        Map<Character, String> her = TablaCalibracion.heredados(equipo());
        if (her.isEmpty()) {
            acta.dato("heredados (T-C41)", "sin códigos heredados de otra acta");
            return null;
        }
        // P12 §6.2: T-C41 empieza apagando y encendiendo el equipo; tiene que verse caer el enlace. En una
        // sesion de "Calibrar todo", la persistencia del acta anterior ya fue ese apagado.
        if (encendidoReciente) {
            operador.progreso("T-C41: el equipo se apagó y encendió en la persistencia anterior de esta sesión");
        } else if (!operador.apagarYEncender()) {
            acta.dato("heredados (T-C41) FALLA", "no se vio caer y volver el enlace al apagar y encender");
            return "T-C41: hay que apagar y encender el equipo antes de empezar y no se vio caer el enlace. No se calibra.";
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
            return "T-C41 FALLA: " + mal + ". No se calibra: hay que reabrir "
                    + TablaCalibracion.origenHeredados(equipo()) + ".";
        }
        acta.dato("heredados (T-C41)", "OK frente a " + TablaCalibracion.origenHeredados(equipo()) + ": " + t);
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
        acta.dato("series " + k, seriesDe(k, p.fila));   // QA-3614-03: que series usa este codigo
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
            return "Código " + k + " restaurado tras dos re-medidas no conformes (PA-12): " + r.texto
                    + ". La secuencia se detiene.";
        }
        acta.restauracionFallida(k, r.texto);
        return "RESTAURACIÓN NO VERIFICADA del código " + k + ": " + r.texto
                + ". El acta no se puede aceptar; vuelva a pulsar Calibrar para reintentarla.";
    }

    /**
     * Re-medida (RF-CAL-39 con P10-C1): K x M, 'e' y el codigo alternados. Colocaciones no validas
     * se registran y se repiten. Una repeticion como maximo; si falla, restaurar (verificado).
     */
    private String remedida(char k) throws IOException, InterruptedException {
        TablaCalibracion.Fila f = fila(k);
        Patron pat = campana.patron(f.remedida);
        Campana.Serie banco = pat == null ? null : campana.elegida(pat.nombre);
        if (banco == null) {
            return "Falta la serie del banco de " + f.remedida + " (re-medida del " + k + ").";
        }
        double xBanco = banco.media();
        int kBanco = Math.max(1, banco.colocaciones().size());
        Anclas.Valor sr = sRep();
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
                Integer va;
                try {
                    va = ops.medir('e');
                    for (int i = 0; i < Remedida3611.M && falla == null; i++) {
                        Integer vx = ops.medir('e');
                        Integer vr = ops.medir(k);
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
        encendidoReciente = mal == null;
        acta.dato("#V# posterior", Ops.texto(v));
        return (mal == null ? "Persistencia OK: " : "Persistencia FALLA (" + mal + "): ") + t;
    }

    /** Codigos cuyo bit de mascara tiene que estar: heredados y certificados. */
    private List<Character> requeridos() {
        List<Character> l = new ArrayList<>(TablaCalibracion.heredados(equipo()).keySet());
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
        Cliente.Respuesta r = ops.escribir("#SC," + hoy + "#");
        Cliente.Respuesta g = ops.pedir("#GC#");
        String leida = g.valida() ? Calibracion.fechaDe(g.trama) : null;
        if (!Tramas.esOk(r.trama) || !hoy.equals(leida)) {
            return "#SC no quedó grabada (#SC -> " + r.describir() + ", #GC# -> " + g.describir()
                    + "): el acta NO se cierra. Reintente.";
        }
        acta.aceptar(reloj.ahoraIso(), hoy, true, true);
        almacen.cerrar(acta);
        Acta cerrada = acta;
        acta = null;
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
            return "NO se rechaza: RESTAURACIÓN NO VERIFICADA del código " + fallos + " (" + t + "). El acta sigue "
                    + "abierta: vuelva a pulsar Rechazar o Continuar para reintentarlo, y avise a Diego si no se resuelve.";
        }
        acta.rechazar(reloj.ahoraIso(), (motivo == null ? "" : motivo) + (t.length() == 0 ? "" : " | " + t));
        almacen.cerrar(acta);
        acta = null;
        return "Acta rechazada. " + t;
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
        List<Character> orden = new ArrayList<>();
        for (char k : ORDEN_SESION) {
            if (seleccion != null && seleccion.contains(k)) {
                orden.add(k);
            }
        }
        StringBuilder hecho = new StringBuilder();
        if (acta != null) {
            // Primero se termina el acta a medias.
            String r = calibrar(new HashSet<Character>(), "", "");
            if (acta != null && acta.motivoNoAceptable(false) == null) {
                String a = persistirYAceptar();
                if (a != null) {
                    return a;
                }
            } else if (acta != null) {
                return "El acta en curso no está lista: " + r;
            }
        }
        if (orden.isEmpty()) {
            return hecho.length() == 0 ? "Nada que calibrar." : "Sesión completa: " + hecho;
        }
        if (nombre == null || nombre.trim().isEmpty() || nota == null || nota.trim().isEmpty()) {
            return "Escriba el nombre y la nota de la conformidad.";
        }
        // Todo se comprueba antes de empezar: nada de descubrir a mitad de la sesion que el 5 no se puede.
        for (char k : orden) {
            if (aceptadoSinFallo(k)) {
                return "El código " + k + " ya tiene un acta ACEPTADA: desmárquelo.";
            }
            Plan p = plan(k, null, true);
            if (!p.escribible()) {
                return "El código " + k + " no se escribe: " + p.motivoNo + ". No se ha empezado la sesión.";
            }
        }
        for (char k : orden) {
            operador.progreso("Sesión: código " + k + " (" + (orden.indexOf(k) + 1) + " de " + orden.size() + ")");
            Set<Character> uno = new HashSet<>();
            uno.add(k);
            String r = calibrar(uno, nombre, nota);
            if (acta == null || acta.motivoNoAceptable(false) != null) {
                return (hecho.length() == 0 ? "" : "Hecho: " + hecho + ". ") + "Código " + k + ": " + r;
            }
            String a = persistirYAceptar();
            if (a != null) {
                return (hecho.length() == 0 ? "" : "Hecho: " + hecho + ". ") + "Código " + k + ": " + a;
            }
            hecho.append(k).append(" ACEPTADO; ");
        }
        return "Sesión completa: " + hecho;
    }

    /** null si la persistencia y la aceptacion salen bien; si no, el texto. */
    private String persistirYAceptar() throws IOException, InterruptedException {
        String p = persistencia();
        if (!p.startsWith("Persistencia OK")) {
            return p;
        }
        String a = aceptar();
        return a.startsWith("Acta ACEPTADA") ? null : a;
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
        Ops ops = new Ops(canal);
        Cliente.Respuesta g0 = ops.pedir("#GN#");
        String anterior = g0.valida() ? Calibracion.serieDe(g0.trama) : null;
        if (anterior == null) {
            return "No se pudo leer la serie actual (#GN#): no se cambia.";
        }
        if (c == null || !c.esSerie(anterior) && !Calibracion.NONE.equals(anterior)) {
            return "La serie actual del equipo (" + anterior + ") no es de la campaña abierta: no se cambia.";
        }
        String e = pin == null ? null : ops.entrar(pin);
        if (e != null) {
            return e;
        }
        Cliente.Respuesta r = ops.escribir("#SN," + n + "#");
        Cliente.Respuesta g = ops.pedir("#GN#");
        String leida = g.valida() ? Calibracion.serieDe(g.trama) : null;
        if (!Tramas.esOk(r.trama) || !n.equals(leida)) {
            return "#SN -> " + r.describir() + ", #GN# -> " + g.describir() + ": la serie NO quedó cambiada.";
        }
        c.renombrar(anterior, n, fecha, operador.trim());
        return "Serie cambiada: " + anterior + " -> " + n + " (verificada con #GN#). La campaña sigue siendo la misma.";
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
