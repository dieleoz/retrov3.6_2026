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
         * Persistencia: pide apagar y encender el equipo y vuelve a conectar por MAC.
         * @return true si el enlace vuelve.
         */
        boolean apagarYEncender() throws IOException, InterruptedException;
    }

    /** Donde vive el acta en curso (en la app, en disco: Campanas). */
    public interface AlmacenActa {
        Acta enCurso() throws IOException;

        void adjuntar(Acta a) throws IOException;

        void cerrar(Acta a) throws IOException;
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
        l.add(new Previa(campana != null, campana != null ? "Campaña de " + campana.equipo + " (" + campana.mac + ")"
                : "Sin campaña de este equipo"));
        String gn = ctx.serieGN;
        boolean idOk = campana != null && gn != null && !Calibracion.NONE.equals(gn) && gn.equals(campana.equipo);
        l.add(new Previa(idOk, gn == null ? "Serie del equipo (#GN#) sin leer: pase las pruebas"
                : Calibracion.NONE.equals(gn) ? "El equipo no tiene serie (#GN# = NONE): dé de alta la serie en Avanzado"
                : idOk ? "Serie #GN# " + gn + " = campaña"
                : "La serie #GN# (" + gn + ") no coincide con la campaña (" + (campana == null ? "-" : campana.equipo)
                + "): no se calibra"));
        Anclas.Valor sr = sRep();
        boolean srOk = sr != null && !Double.isNaN(sr.valor);
        l.add(new Previa(srOk, sr == null ? "s_rep: sin campaña" : sr.texto + (srOk ? "" : " — mida la A5 del inicio del banco")));
        if (campana != null && campana.bateriaBloqueaEscrituras()) {
            l.add(new Previa(false, "Batería: la última lectura bloquea las escrituras (n = 0 o sin respuesta). "
                    + "Cambie la batería y pulse \"Leer batería\""));
        }
        if (acta != null && acta.invalidada()) {
            l.add(new Previa(false, "El acta en curso está invalidada: recházela"));
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
        return hayAjuste ? s : todos;
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
        TablaCalibracion.Fila f = TablaCalibracion.fila(k, decisiones);
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
        } else if (!p.incumplimientos.isEmpty() && f.dispensa.isEmpty()) {
            no = "incumple " + String.join("; ", p.incumplimientos) + " y la tabla no le da dispensa";
        }
        return new Plan(k, f, p, osc.valor, osc.texto, textoPatrones(med), no);
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
        for (TablaCalibracion.Fila f : TablaCalibracion.tabla(decisiones).values()) {
            StringBuilder sb = new StringBuilder(f.texto());
            if (!f.aviso.isEmpty()) {
                sb.append("\n  ").append(f.aviso);
            }
            boolean casilla = false;
            if (f.escribible()) {
                Plan p = plan(f.codigo, vigentes == null ? null : vigentes.get(f.codigo));
                if (p.propuesta != null) {
                    sb.append("\n  ").append(p.propuesta.metodo).append(", ").append(p.patrones);
                    if (p.propuesta.ajuste != null) {
                        Ecuacion e = p.propuesta.ajuste.ecuacion;
                        sb.append(String.format(Locale.US, "\n  c1 %s  c0 %s  error máx %.1f", Tramas.coeficiente(e.c1),
                                Tramas.coeficiente(e.c0), p.propuesta.ajuste.errorMaximo));
                    }
                    for (String s : p.propuesta.incumplimientos) {
                        sb.append("\n  Incumple (").append(f.dispensa.isEmpty() ? "SIN dispensa" : "dispensa " + f.dispensa)
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
                    + (f.dispensa.isEmpty() ? "" : " (dispensa " + f.dispensa + ", decisión de Diego registrada)")));
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
                && acta.motivoNoAceptableSalvoVerificacionFinal() == null;
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
        if (pin == null || pin.isEmpty()) {
            return "Falta el PIN del equipo.";
        }
        if (!ops.login(pin)) {
            pin = null;
            return "PIN rechazado: no se ha escrito nada.";
        }
        return null;
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
        String conformidad = "";
        if (!planes.isEmpty()) {
            StringBuilder cods = new StringBuilder();
            for (char k : planes.keySet()) {
                cods.append(k).append(' ');
            }
            conformidad = nombre.trim() + ": códigos " + cods.toString().trim() + ". " + nota.trim();
            acta.conformidad(conformidad);
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
            String r = escribir(p, conformidad(p, conformidad));
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
        acta = new Acta(campana.equipo, campana.mac, ctx.firmware, Remedida3611.K, Remedida3611.M, 1, reloj.ahoraIso());
        acta.tabla = TablaCalibracion.VERSION;
        almacen.adjuntar(acta);
        acta.dato("md5 de la cola", cola.md5);
        Anclas.Valor sr = sRep();
        acta.dato("s_rep", sr == null ? "" : sr.texto);
        acta.dato("app", ctx.app);
        acta.dato("decisiones", decisiones.texto());
        TablaCalibracion.Fila f5 = TablaCalibracion.fila('5', decisiones);
        acta.dato("código 5", f5.escribible() ? f5.origen : f5.aviso);
        TablaCalibracion.Fila fb = TablaCalibracion.fila('b', decisiones);
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
        Map<Character, String> her = TablaCalibracion.heredados(campana.equipo);
        String mal = null;
        for (Map.Entry<Character, String> e : her.entrySet()) {
            char k = e.getKey();
            Ecuacion esperado = Tramas.parsearG(e.getValue(), k);
            Ecuacion g = ops.leerG(k);
            boolean igual = g != null && esperado != null && g.igualFloat32(esperado, Ecuacion.ULP_G);
            t.append("#G,").append(k).append(igual ? " igual al acta; " : " DISTINTO del acta (" + g + "); ");
            if (!igual && mal == null) {
                mal = "el código " + k + " heredado no coincide con " + TablaCalibracion.origenHeredados(campana.equipo);
            }
        }
        return mal;
    }

    /** T-C41 / P11-M6: al empezar, #V# y #G de los heredados frente al acta anterior; y #E. */
    private String heredados() throws IOException, InterruptedException {
        if (acta.dato("heredados (T-C41)") != null) {
            return null;
        }
        Map<Character, String> her = TablaCalibracion.heredados(campana.equipo);
        if (her.isEmpty()) {
            acta.dato("heredados (T-C41)", "sin códigos heredados de otra acta");
            return null;
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
                    + TablaCalibracion.origenHeredados(campana.equipo) + ".";
        }
        acta.dato("heredados (T-C41)", "OK frente a " + TablaCalibracion.origenHeredados(campana.equipo) + ": " + t);
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
        String metodo = p.propuesta.metodo + "; tabla " + p.fila.texto();
        String osc = p.oscuroTexto(ts.enviada);
        operador.progreso("Código " + k + ": #S...");
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
        TablaCalibracion.Fila f = TablaCalibracion.fila(k, decisiones);
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
            Remedida3611.Resultado res = f.dispensa.isEmpty()
                    ? Remedida3611.evaluar(pat.nombre, pat.valor, xs, rs, xBanco, kBanco,
                    sr == null ? Double.NaN : sr.valor, sr == null ? "" : sr.texto)
                    // Codigo con dispensa de Diego (b, PA-24): RF-CAL-18 frente a la curva escrita decide; el
                    // incumplimiento frente al certificado queda escrito en el acta.
                    : Remedida3611.evaluarConDispensa(pat.nombre, pat.valor, xs, rs, xBanco, kBanco,
                    sr == null ? Double.NaN : sr.valor, sr == null ? "" : sr.texto, c.leida, f.dispensa);
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
        if (!operador.apagarYEncender()) {
            return "No se pudo reconectar con el equipo: repita la persistencia.";
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
        List<Character> l = new ArrayList<>(TablaCalibracion.heredados(campana.equipo).keySet());
        for (Acta.Codigo c : acta.certificados()) {
            if (!l.contains(c.k)) {
                l.add(c.k);
            }
        }
        return l;
    }

    /** Boton "Aceptar y grabar fecha": verificacion final (dentro), bateria, #SC y #GC. */
    public String aceptar() throws IOException, InterruptedException {
        if (!puedeAceptar()) {
            return acta == null ? "Sin acta en curso." : "No aceptable: "
                    + (motivoPrevias() != null ? motivoPrevias() : acta.motivoNoAceptableSalvoVerificacionFinal());
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
    public String rechazar(String motivo, boolean restaurar) throws IOException, InterruptedException {
        if (acta == null) {
            return "Sin acta en curso.";
        }
        StringBuilder t = new StringBuilder();
        if (restaurar) {
            String e0 = entrar();
            if (e0 != null) {
                return e0 + " No se ha rechazado.";
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
                }
                t.append("código ").append(c.k).append(r.ok ? " restaurado; " : " SIN RESTAURAR (" + r.texto + "); ");
            }
        }
        acta.rechazar(reloj.ahoraIso(), (motivo == null ? "" : motivo) + (t.length() == 0 ? "" : " | " + t));
        almacen.cerrar(acta);
        acta = null;
        return "Acta rechazada. " + t;
    }

    // ---------------------------------------------------------- desde Avanzado

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
