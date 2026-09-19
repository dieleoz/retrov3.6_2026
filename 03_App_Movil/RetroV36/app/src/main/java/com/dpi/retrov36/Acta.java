package com.dpi.retrov36;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Acta de calibracion (3.6.8; reescrita en la 3.6.11 con las condiciones P10). Java puro.
 *
 * - P9-B3: protocolo de disparos fijo y escrito.
 * - P9-B8: un codigo cada vez: no se escribe otro hasta que el anterior este resuelto
 *   (re-medida conforme, o restaurado tras dos re-medidas validas no conformes).
 * - P9-B9: la curva certificada es la leida con #G tras #S.
 * - P10-C4 (D-20): el acta guarda TODOS los intentos de re-medida, tambien las colocaciones no
 *   validas. Como maximo una repeticion: dos re-medidas validas por codigo (PA-12). Reescribir un
 *   codigo anula su entrada anterior (queda en el texto como anulada), no la deja bloqueando.
 * - P10-C5: aceptable solo si lo que certifica esta en el equipo: la app anota la verificacion
 *   final (#V# y #G,k# frescos, iguales a lo certificado) y la persistencia (apagar y encender:
 *   #V#, #G y #E). Un #F con el acta abierta la invalida. Si #SC falla, el acta no se cierra.
 * - RF-APP-36: diario de solo anadir en disco (escribirEn / leer): reconectar no borra nada.
 * - RF-APP-37: el estado ESCRIBIENDO (entre #S y la relectura) queda en el diario para resolver
 *   un corte al reconectar.
 */
public final class Acta {

    public final String equipo;
    public final String mac;
    public final String firmware;
    public final int colocaciones;
    public final int disparos;
    public final int asentamiento;
    public final String abierta;
    /** Version de la tabla RF-CAL-37 con la que se abrio (vacia en actas de la 3.6.8). */
    public String tabla = "";
    private final List<Codigo> codigos = new ArrayList<>();
    private final List<String> anuladas = new ArrayList<>();
    private final List<String> notas = new ArrayList<>();
    /** null: pendiente; si no, ACEPTADA o RECHAZADA con fecha. */
    private String cierre;
    private String fechaGrabada;
    private String invalidada;
    private String persistencia;
    private boolean persistenciaOk;
    private String verificacionFinal;
    private boolean verificacionFinalOk;
    private final List<String> conformidades = new ArrayList<>();
    /** Campos de RF-CAL-43 (cola, s_rep, bateria, #V# posterior, oscuro...). */
    private final java.util.Map<String, String> datos = new java.util.LinkedHashMap<>();
    /** Campos obligatorios para aceptar en el flujo "Calibrar este equipo" (RF-CAL-43). */
    public static final String[] DATOS_OBLIGATORIOS = {"md5 de la cola", "s_rep", "batería", "#V# posterior",
            "heredados (T-C41)", "código 5"};
    /** Codigo con un #S enviado y sin relectura (corte durante #S): 0 si ninguno. */
    private char escribiendo;
    private Ecuacion escribiendoAnterior;
    private Ecuacion escribiendoEnviada;
    /** P11-M3: metodo, oscuro y conformidad del #S en curso, para no perderlos si se corta. */
    private String escribiendoMetodo = "";
    private String escribiendoOscuro = "";
    private String escribiendoConformidad = "";
    private Writer diario;

    public Acta(String equipo, String mac, String firmware, int colocaciones, int disparos, int asentamiento,
                String abierta) {
        this.equipo = equipo;
        this.mac = mac;
        this.firmware = firmware;
        this.colocaciones = colocaciones;
        this.disparos = disparos;
        this.asentamiento = asentamiento;
        this.abierta = abierta;
    }

    // ------------------------------------------------------------------ tipos

    public static final class Remedida {
        public final String patron;
        public final double rMedida;
        public final double rPredicha;
        public final double sRep;
        public final double tolerancia;
        public final boolean ok;
        public final String texto;

        Remedida(String patron, double rMedida, double rPredicha, double sRep, double tolerancia, boolean ok, String texto) {
            this.patron = patron;
            this.rMedida = rMedida;
            this.rPredicha = rPredicha;
            this.sRep = sRep;
            this.tolerancia = tolerancia;
            this.ok = ok;
            this.texto = texto;
        }
    }

    /** Un intento de re-medida: NO_VALIDA (colocacion), CONFORME, NO_CONFORME o NO_EVALUABLE. */
    public static final class Intento {
        public final String fecha;
        public final String estado;
        public final String texto;

        Intento(String fecha, String estado, String texto) {
            this.fecha = fecha;
            this.estado = estado;
            this.texto = texto;
        }

        public boolean valido() {
            return "CONFORME".equals(estado) || "NO_CONFORME".equals(estado);
        }
    }

    public static final class Codigo {
        public final char k;
        public final String tramaG;
        public final Ecuacion leida;
        public final String oscuro;
        public final String metodo;
        public final String conformidad;
        public final List<Intento> intentos = new ArrayList<>();
        /** Restaurado a su estado anterior tras dos re-medidas no conformes. */
        public String restaurado;
        /** Compatibilidad 3.6.8: ultima re-medida registrada por remedida(). */
        public Remedida remedida;
        /** Curva que habia antes del #S (para restaurar); null si no se conoce. */
        public Ecuacion anterior;
        /** P11-M1: ultima restauracion intentada que NO se pudo verificar (null si ninguna). */
        public String restauracionFallida;

        Codigo(char k, String tramaG, Ecuacion leida, String oscuro, String metodo, String conformidad) {
            this.k = k;
            this.tramaG = tramaG;
            this.leida = leida;
            this.oscuro = oscuro == null ? "" : oscuro;
            this.metodo = metodo == null ? "" : metodo;
            this.conformidad = conformidad == null ? "" : conformidad;
        }

        public int validos() {
            int n = 0;
            for (Intento i : intentos) {
                if (i.valido()) {
                    n++;
                }
            }
            return n;
        }

        public boolean conforme() {
            for (Intento i : intentos) {
                if ("CONFORME".equals(i.estado)) {
                    return true;
                }
            }
            return false;
        }

        /** Resuelto: conforme, o restaurado. */
        public boolean resuelto() {
            return conforme() || restaurado != null;
        }

        /** Tras una re-medida valida NO CONFORME queda una repeticion; tras dos, se restaura. */
        public boolean puedeRepetir() {
            return !conforme() && restaurado == null && validos() < 2;
        }

        /** Dos re-medidas validas no conformes y todavia sin restaurar (verificado). */
        public boolean debeRestaurarse() {
            return !conforme() && restaurado == null && validos() >= 2;
        }
    }

    // ------------------------------------------------------------- diario

    /** A partir de aqui, cada operacion se anade al diario del acta (RF-APP-36). */
    public void escribirEn(Writer w) throws IOException {
        diario = w;
        evento("ABRE", abierta, equipo, mac, firmware, colocaciones, disparos, asentamiento, tabla);
    }

    /** Sigue escribiendo en un diario ya existente (sin repetir ABRE). */
    public void continuarEn(Writer w) {
        diario = w;
    }

    private void evento(Object... campos) throws IOException {
        if (diario != null) {
            diario.write(Csv.unir(campos));
            diario.write("\n");
            diario.flush();
        }
    }

    private void eventoSinFallo(Object... campos) {
        try {
            evento(campos);
        } catch (IOException e) {
            throw new IllegalStateException("no se pudo escribir el diario del acta: " + e.getMessage(), e);
        }
    }

    /** Reconstruye un acta desde su diario; null si esta vacio o no empieza por ABRE. */
    public static Acta leer(Reader r) throws IOException {
        BufferedReader br = new BufferedReader(r);
        String l;
        Acta a = null;
        while ((l = br.readLine()) != null) {
            if (l.trim().isEmpty() || l.startsWith("#")) {
                continue;
            }
            List<String> c = Csv.partir(l);
            String ev = c.get(0);
            if (a == null) {
                if (!"ABRE".equals(ev)) {
                    return null;
                }
                a = new Acta(c.get(2), c.get(3), c.get(4), Integer.parseInt(c.get(5)), Integer.parseInt(c.get(6)),
                        Integer.parseInt(c.get(7)), c.get(1));
                a.tabla = c.size() > 8 ? c.get(8) : "";
                continue;
            }
            a.aplicar(ev, c);
        }
        return a;
    }

    private void aplicar(String ev, List<String> c) {
        switch (ev) {
            case "CONFORMIDAD":
                conformidades.add(c.get(1));
                break;
            case "DATO":
                datos.put(c.get(1), c.get(2));
                break;
            case "ESCRIBIENDO":
                escribiendo = c.get(1).charAt(0);
                escribiendoAnterior = new Ecuacion(d(c, 2), d(c, 3), d(c, 4), d(c, 5));
                escribiendoEnviada = new Ecuacion(d(c, 6), d(c, 7), d(c, 8), d(c, 9));
                escribiendoMetodo = c.size() > 10 ? c.get(10) : "";
                escribiendoOscuro = c.size() > 11 ? c.get(11) : "";
                escribiendoConformidad = c.size() > 12 ? c.get(12) : "";
                break;
            case "ESCRITO": {
                char k = c.get(1).charAt(0);
                Ecuacion e = Tramas.parsearG(c.get(2), k);
                anularAnterior(k);
                Codigo nuevo = new Codigo(k, c.get(2), e, c.get(4), c.get(3), c.size() > 5 ? c.get(5) : "");
                nuevo.anterior = escribiendo == k ? escribiendoAnterior : null;
                codigos.add(nuevo);
                escribiendo = 0;
                anularVerificaciones();
                break;
            }
            case "SIN_ESCRIBIR":
                escribiendo = 0;
                anularVerificaciones();
                notas.add("Código " + c.get(1) + " sin escribir: " + (c.size() > 2 ? c.get(2) : ""));
                break;
            case "RESTAURA_FALLA": {
                Codigo cod = codigo(c.get(1).charAt(0));
                if (cod != null) {
                    cod.restauracionFallida = c.get(2);
                }
                anularVerificaciones();
                break;
            }
            case "INTENTO": {
                Codigo cod = codigo(c.get(1).charAt(0));
                if (cod != null) {
                    cod.intentos.add(new Intento(c.get(2), c.get(3), c.get(4)));
                }
                break;
            }
            case "RESTAURADO": {
                Codigo cod = codigo(c.get(1).charAt(0));
                if (cod != null) {
                    cod.restaurado = c.get(2);
                    cod.restauracionFallida = null;
                }
                anularVerificaciones();
                break;
            }
            case "PERSISTENCIA":
                persistenciaOk = "1".equals(c.get(1));
                persistencia = c.get(2);
                break;
            case "FINAL":
                verificacionFinalOk = "1".equals(c.get(1));
                verificacionFinal = c.get(2);
                break;
            case "INVALIDA":
                invalidada = c.get(1);
                break;
            case "ACEPTADA":
                cierre = "ACEPTADA " + c.get(1);
                fechaGrabada = c.get(2).isEmpty() ? null : c.get(2);
                break;
            case "RECHAZADA":
                cierre = "RECHAZADA " + c.get(1) + (c.get(2).isEmpty() ? "" : ": " + c.get(2));
                break;
            default:
                break;
        }
    }

    private static double d(List<String> c, int i) {
        return Double.parseDouble(c.get(i));
    }

    // ------------------------------------------------------------- consultas

    public List<Codigo> codigos() {
        return codigos;
    }

    public boolean cerrada() {
        return cierre != null;
    }

    public boolean invalidada() {
        return invalidada != null;
    }

    public char escribiendo() {
        return escribiendo;
    }

    public Ecuacion escribiendoAnterior() {
        return escribiendoAnterior;
    }

    public Ecuacion escribiendoEnviada() {
        return escribiendoEnviada;
    }

    public List<String> conformidades() {
        return conformidades;
    }

    public Codigo codigo(char k) {
        for (int i = codigos.size() - 1; i >= 0; i--) {
            if (codigos.get(i).k == k) {
                return codigos.get(i);
            }
        }
        return null;
    }

    /** P9-B8: null si se puede escribir el codigo k; si no, el motivo. */
    public String motivoNoEscribir(char k) {
        if (cerrada()) {
            return "el acta está cerrada (" + cierre + "): abra otra";
        }
        if (invalidada()) {
            return "el acta está invalidada (" + invalidada + "): recházela y abra otra";
        }
        if (escribiendo != 0) {
            return "hay un #S del código " + escribiendo + " sin resolver (corte durante #S): resuélvalo antes";
        }
        Codigo mismo = codigo(k);
        if (mismo != null) {
            // P11-M4, QA-3612-04: ni reiniciar D-20 ni reescribir lo ya conforme sin una decision expresa.
            if (mismo.conforme()) {
                return "el código " + k + " ya está CONFORME en esta acta: para reescribirlo, rechace el acta y abra otra";
            }
            if (mismo.restauracionFallida != null) {
                return "la restauración del código " + k + " no se ha verificado: hay que restaurarlo antes";
            }
            if (mismo.restaurado != null) {
                return "el código " + k + " se restauró tras dos re-medidas no conformes: no se reescribe en esta acta";
            }
            if (mismo.validos() > 0) {
                return "el código " + k + " tiene una re-medida válida NO CONFORME: se repite la re-medida, no la escritura (D-20)";
            }
        }
        for (Codigo c : codigos) {
            if (c.k != k && !c.resuelto()) {
                return "falta la re-medida de verificación conforme del código " + c.k + " (P9-B8: un código cada vez)";
            }
        }
        return null;
    }

    // ------------------------------------------------------------- operaciones

    /** Anota un campo del acta (RF-CAL-43). Un dato desconocido se anota como "no conocido", nunca vacio. */
    public void dato(String clave, String valor) {
        String v = valor == null || valor.trim().isEmpty() ? "no conocido" : valor;
        datos.put(clave, v);
        eventoSinFallo("DATO", clave, v);
    }

    public String dato(String clave) {
        return datos.get(clave);
    }

    public void conformidad(String texto) {
        conformidades.add(texto);
        eventoSinFallo("CONFORMIDAD", texto);
    }

    /** Antes de enviar #S: queda anotado para resolver un corte (RF-APP-37). */
    public void escribiendo(char k, Ecuacion anterior, Ecuacion enviada) {
        escribiendo(k, anterior, enviada, "", "", "");
    }

    /** P11-M3: con el metodo, el oscuro y la conformidad, que un corte no puede perder. */
    public void escribiendo(char k, Ecuacion anterior, Ecuacion enviada, String metodo, String oscuro,
                            String conformidad) {
        escribiendo = k;
        escribiendoAnterior = anterior;
        escribiendoEnviada = enviada;
        escribiendoMetodo = metodo == null ? "" : metodo;
        escribiendoOscuro = oscuro == null ? "" : oscuro;
        escribiendoConformidad = conformidad == null ? "" : conformidad;
        eventoSinFallo("ESCRIBIENDO", String.valueOf(k), anterior.c3, anterior.c2, anterior.c1, anterior.c0,
                enviada.c3, enviada.c2, enviada.c1, enviada.c0, escribiendoMetodo, escribiendoOscuro,
                escribiendoConformidad);
    }

    public String escribiendoMetodo() {
        return escribiendoMetodo;
    }

    public String escribiendoOscuro() {
        return escribiendoOscuro;
    }

    public String escribiendoConformidad() {
        return escribiendoConformidad;
    }

    /** P11-M2: cualquier cambio en el equipo deja sin valor la persistencia y la verificacion hechas. */
    private void anularVerificaciones() {
        persistenciaOk = false;
        verificacionFinalOk = false;
        if (persistencia != null) {
            persistencia = persistencia + " [anulada: hubo cambios después]";
        }
    }

    /** El #S no entro (o se restauro): el codigo queda como estaba. */
    public void sinEscribir(char k, String texto) {
        escribiendo = 0;
        anularVerificaciones();
        notas.add("Código " + k + " sin escribir: " + texto);
        eventoSinFallo("SIN_ESCRIBIR", String.valueOf(k), texto);
    }

    private void anularAnterior(char k) {
        for (int i = codigos.size() - 1; i >= 0; i--) {
            if (codigos.get(i).k == k) {
                Codigo v = codigos.remove(i);
                anuladas.add("Código " + k + " anulado por reescritura: " + v.tramaG + " (" + v.intentos.size()
                        + " intentos)");
            }
        }
    }

    /** #S verificado con #E: queda anotado con la curva leida con #G (P9-B9). */
    public void escrito(char k, String tramaG, Ecuacion leida, String oscuro) {
        escrito(k, tramaG, leida, oscuro, "", "");
    }

    public void escrito(char k, String tramaG, Ecuacion leida, String oscuro, String metodo, String conformidad) {
        anularAnterior(k);
        Codigo nuevo = new Codigo(k, tramaG, leida, oscuro, metodo, conformidad);
        nuevo.anterior = escribiendo == k ? escribiendoAnterior : null;
        codigos.add(nuevo);
        escribiendo = 0;
        anularVerificaciones();
        eventoSinFallo("ESCRITO", String.valueOf(k), tramaG, metodo == null ? "" : metodo, oscuro == null ? "" : oscuro,
                conformidad == null ? "" : conformidad);
    }

    /**
     * Registra un intento de re-medida (D-20). Las colocaciones no validas y los intentos no
     * evaluables se registran siempre; como maximo dos re-medidas validas (una repeticion).
     * @throws IllegalStateException si ya hay dos validas, o una conforme, o el codigo esta restaurado.
     */
    public void intento(char k, String fecha, String estado, String texto) {
        Codigo c = codigo(k);
        if (c == null) {
            throw new IllegalArgumentException("el código " + k + " no se ha escrito en esta acta");
        }
        boolean valido = "CONFORME".equals(estado) || "NO_CONFORME".equals(estado);
        if (valido && !c.puedeRepetir()) {
            throw new IllegalStateException("máximo una repetición: el código " + k + " ya tiene "
                    + (c.conforme() ? "una re-medida conforme" : c.validos() + " re-medidas válidas")
                    + (c.restaurado != null ? " y está restaurado" : ""));
        }
        c.intentos.add(new Intento(fecha, estado, texto));
        eventoSinFallo("INTENTO", String.valueOf(k), fecha, estado, texto);
    }

    /** Compatibilidad 3.6.8: registra la re-medida como un intento valido. */
    public void remedida(char k, Remedida r) {
        intento(k, "", r.ok ? "CONFORME" : "NO_CONFORME", r.texto);
        codigo(k).remedida = r;
    }

    public void restaurado(char k, String texto) {
        Codigo c = codigo(k);
        if (c == null) {
            throw new IllegalArgumentException("el código " + k + " no se ha escrito en esta acta");
        }
        c.restaurado = texto;
        c.restauracionFallida = null;
        anularVerificaciones();
        eventoSinFallo("RESTAURADO", String.valueOf(k), texto);
    }

    /** P11-M1: restauracion cuya relectura no coincide: el codigo NO queda resuelto. */
    public void restauracionFallida(char k, String texto) {
        Codigo c = codigo(k);
        if (c == null) {
            throw new IllegalArgumentException("el código " + k + " no se ha escrito en esta acta");
        }
        c.restauracionFallida = texto;
        anularVerificaciones();
        eventoSinFallo("RESTAURA_FALLA", String.valueOf(k), texto);
    }

    /** P10-C5: un #F (o cualquier cambio fuera del flujo) con el acta abierta la invalida. */
    public void invalidar(String motivo) {
        if (cerrada()) {
            return;
        }
        invalidada = motivo;
        eventoSinFallo("INVALIDA", motivo);
    }

    /** RF-APP-46: apagar, encender y releer #V#, #G y #E de lo escrito. */
    public void persistencia(boolean ok, String texto) {
        persistenciaOk = ok;
        persistencia = texto;
        eventoSinFallo("PERSISTENCIA", ok ? 1 : 0, texto);
    }

    /** P10-C5: #V# y #G,k# frescos justo antes de aceptar, iguales a lo certificado. */
    public void verificacionFinal(boolean ok, String texto) {
        verificacionFinalOk = ok;
        verificacionFinal = texto;
        eventoSinFallo("FINAL", ok ? 1 : 0, texto);
    }

    /** Curvas certificadas (codigos resueltos por conformidad), para la verificacion final. */
    public List<Codigo> certificados() {
        List<Codigo> l = new ArrayList<>();
        for (Codigo c : codigos) {
            if (c.conforme() && c.restaurado == null) {
                l.add(c);
            }
        }
        return l;
    }

    /**
     * RF-CAL-39.1 (bloque "sin reescribir" del acta 3.6.8): se mantiene para las pruebas de la
     * 3.6.8. La app 3.6.11 evalua la re-medida con {@link Remedida3611}. Con una sola colocacion
     * no hay s_rep medida: devuelve NO evaluable (no la supone).
     */
    public static Remedida evaluarRemedida(String patron, List<double[]> rPorColocacion, List<double[]> xPorColocacion,
                                           Ecuacion leida, double sRepRelPorDefecto) {
        List<double[]> pred = new ArrayList<>();
        for (double[] xs : xPorColocacion) {
            double[] p = new double[xs.length];
            for (int i = 0; i < xs.length; i++) {
                p[i] = leida.respuestaFloat32((int) Math.round(xs[i]));
            }
            pred.add(p);
        }
        double rMed = mediaDeMedias(rPorColocacion);
        double rPred = mediaDeMedias(pred);
        double sRep = rPorColocacion.size() >= 2 ? Veredicto.sEntre(rPorColocacion) : sRepRelPorDefecto * rMed;
        double tol = Math.max(2, 2 * sRep);
        boolean ok = Math.abs(rMed - rPred) <= tol;
        String t = String.format(Locale.US, "%s: R medida %.1f, R predicha %.1f (curva #G, float32), diferencia %+.1f; "
                        + "tolerancia máx(2 ; 2·s_rep) = %.1f con s_rep %.1f%s -> %s",
                patron, rMed, rPred, rMed - rPred, tol, sRep,
                rPorColocacion.size() >= 2 ? " (entre " + rPorColocacion.size() + " colocaciones)" : " (supuesta)",
                ok ? "CONFORME" : "NO CONFORME");
        return new Remedida(patron, rMed, rPred, sRep, tol, ok, t);
    }

    static double mediaDeMedias(List<double[]> g) {
        List<Double> m = new ArrayList<>();
        for (double[] v : g) {
            if (v.length > 0) {
                m.add(Estadistica.media(v));
            }
        }
        return Estadistica.media(Estadistica.aVector(m));
    }

    /**
     * null si se puede aceptar; si no, el motivo. Con exigirVerificaciones = false (actas de la
     * 3.6.8, flujo de Avanzado sin persistencia) no se piden persistencia ni verificacion final.
     */
    public String motivoNoAceptable(boolean exigirVerificaciones) {
        return motivoNoAceptable(exigirVerificaciones, exigirVerificaciones);
    }

    /**
     * QA-3612-01: el boton "Aceptar" se habilita con todo lo demas cumplido; la verificacion final
     * (#V# y #G frescos) se hace DENTRO de la accion de aceptar.
     */
    public String motivoNoAceptableSalvoVerificacionFinal() {
        return motivoNoAceptable(true, false);
    }

    public String motivoNoAceptable(boolean exigirVerificaciones, boolean exigirFinal) {
        if (cerrada()) {
            return "el acta ya está cerrada (" + cierre + ")";
        }
        if (invalidada()) {
            return "el acta está invalidada: " + invalidada;
        }
        if (escribiendo != 0) {
            return "hay un #S del código " + escribiendo + " sin resolver";
        }
        if (codigos.isEmpty()) {
            return "no se ha escrito ningún código";
        }
        boolean alguno = false;
        for (Codigo c : codigos) {
            if (c.restauracionFallida != null) {
                return "RESTAURACIÓN NO VERIFICADA del código " + c.k + ": " + c.restauracionFallida
                        + ". El equipo puede tener una curva desconocida: no se acepta";
            }
            if (!c.resuelto()) {
                return c.intentos.isEmpty() ? "falta la re-medida de verificación del código " + c.k
                        : "la re-medida del código " + c.k + " no es conforme (queda " + (c.puedeRepetir() ? "una repetición" : "restaurar") + ")";
            }
            alguno |= c.conforme();
        }
        if (!alguno) {
            return "ningún código ha quedado conforme";
        }
        if (exigirVerificaciones) {
            for (String d : DATOS_OBLIGATORIOS) {
                if (!datos.containsKey(d)) {
                    return "falta " + d;
                }
            }
            for (Codigo c : certificados()) {
                if (c.metodo.contains("anclada") && !datos.containsKey("oscuro " + c.k)) {
                    return "falta oscuro " + c.k;
                }
            }
            if (!persistenciaOk) {
                return "falta la persistencia (apagar, encender y releer #V#, #G y #E)";
            }
            if (exigirFinal && !verificacionFinalOk) {
                return "falta la verificación final (#V# y #G frescos iguales a lo certificado)";
            }
        }
        return null;
    }

    public String motivoNoAceptable() {
        return motivoNoAceptable(false);
    }

    /**
     * Acepta el acta. P10-C5: si el firmware tiene #SC y la fecha no quedo grabada (fechaGrabadaEnEquipo
     * null), el acta NO se cierra.
     */
    public void aceptar(String fecha, String fechaGrabadaEnEquipo, boolean firmwareConSC, boolean exigirVerificaciones) {
        String m = motivoNoAceptable(exigirVerificaciones);
        if (m != null) {
            throw new IllegalStateException(m);
        }
        if (firmwareConSC && fechaGrabadaEnEquipo == null) {
            throw new IllegalStateException("la fecha de calibración (#SC) no quedó grabada: el acta no se cierra");
        }
        cierre = "ACEPTADA " + fecha;
        fechaGrabada = fechaGrabadaEnEquipo;
        eventoSinFallo("ACEPTADA", fecha, fechaGrabadaEnEquipo == null ? "" : fechaGrabadaEnEquipo);
    }

    /** Compatibilidad 3.6.8 (sin #SC obligatorio ni verificaciones). */
    public void aceptar(String fecha, String fechaGrabadaEnEquipo) {
        aceptar(fecha, fechaGrabadaEnEquipo, false, false);
    }

    public void rechazar(String fecha, String motivo) {
        cierre = "RECHAZADA " + fecha + (motivo == null || motivo.isEmpty() ? "" : ": " + motivo);
        eventoSinFallo("RECHAZADA", fecha, motivo == null ? "" : motivo);
    }

    public String texto() {
        StringBuilder sb = new StringBuilder("ACTA DE CALIBRACIÓN\n");
        sb.append("Equipo: ").append(equipo).append(" (MAC ").append(mac).append(")\n");
        sb.append("Firmware: ").append(firmware).append('\n');
        sb.append("Abierta: ").append(abierta).append('\n');
        if (!tabla.isEmpty()) {
            sb.append("Tabla de métodos: ").append(tabla).append('\n');
        }
        sb.append(String.format(Locale.US, "Protocolo de disparos (fijo, P9-B3): %d colocaciones × %d disparos, "
                + "%d de asentamiento por colocación\n", colocaciones, disparos, asentamiento));
        sb.append("Ajuste contra patrones, no calibración trazable. Por debajo del patrón más bajo de cada código "
                + "la lectura no está calibrada.\n");
        for (String c : conformidades) {
            sb.append("Conformidad: ").append(c).append('\n');
        }
        for (java.util.Map.Entry<String, String> e : datos.entrySet()) {
            sb.append(e.getKey()).append(": ").append(e.getValue()).append('\n');
        }
        sb.append('\n');
        for (Codigo c : codigos) {
            sb.append("Código ").append(c.k).append(" (").append(Fabrica.nombre(c.k)).append(")\n");
            if (!c.metodo.isEmpty()) {
                sb.append("  Método: ").append(c.metodo).append('\n');
            }
            sb.append("  Curva certificada = leída con #G tras #S (P9-B9): ").append(c.tramaG).append('\n');
            if (!c.conformidad.isEmpty()) {
                sb.append("  ").append(c.conformidad).append('\n');
            }
            sb.append("  #E en 5 puntos: coincide con la curva enviada (±1)\n");
            if (!c.oscuro.isEmpty()) {
                sb.append("  ").append(c.oscuro).append('\n');
            }
            if (c.intentos.isEmpty()) {
                sb.append("  Re-medida (RF-CAL-39): PENDIENTE\n");
            }
            int n = 1;
            for (Intento i : c.intentos) {
                sb.append("  Intento ").append(n++).append(i.fecha.isEmpty() ? "" : " (" + i.fecha + ")").append(": ")
                        .append(i.estado).append(" - ").append(i.texto).append('\n');
            }
            if (c.restaurado != null) {
                sb.append("  RESTAURADO: ").append(c.restaurado).append('\n');
            }
            if (c.restauracionFallida != null) {
                sb.append("  RESTAURACIÓN NO VERIFICADA: ").append(c.restauracionFallida).append('\n');
            }
        }
        for (String a : anuladas) {
            sb.append(a).append('\n');
        }
        for (String a : notas) {
            sb.append(a).append('\n');
        }
        if (escribiendo != 0) {
            sb.append("#S del código ").append(escribiendo).append(" SIN RESOLVER (corte durante #S)\n");
        }
        if (persistencia != null) {
            sb.append("Persistencia: ").append(persistenciaOk ? "OK" : "FALLA").append(" - ").append(persistencia).append('\n');
        }
        if (verificacionFinal != null) {
            sb.append("Verificación final: ").append(verificacionFinalOk ? "OK" : "FALLA").append(" - ")
                    .append(verificacionFinal).append('\n');
        }
        if (invalidada != null) {
            sb.append("INVALIDADA: ").append(invalidada).append('\n');
        }
        sb.append('\n').append("Estado: ").append(cierre == null ? "PENDIENTE" : cierre).append('\n');
        if (cierre != null && cierre.startsWith("ACEPTADA")) {
            sb.append("Fecha de calibración en el equipo (#SC/#GC): ")
                    .append(fechaGrabada == null ? "no grabada (firmware sin #SC)" : fechaGrabada).append('\n');
        }
        return sb.toString();
    }
}
