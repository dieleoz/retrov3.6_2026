/*
 * Prueba de ruptura del revisor P15 (05_Documentacion/REVISION-P15-QA-3.6.16.md, ef7ea10), llevada al arbol en la
 * 3.6.17. Original en el scratchpad de la revision: romper_serie/src/RomperSerieTest.java. Imprime lo observado (SALIDA) y, donde la 3.6.17 corrige
 * el defecto, lo comprueba con assert.
 */
package com.dpi.retrov36;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * "Calibrar este equipo" de extremo a extremo contra el equipo simulado fiel a la 3.6.2 (3.6.14; P11-M7,
 * P12 §4, T-S00 en TS00Test). Cada prueba usa SOLO lo que el operador dispara: los botones de
 * FlujoCalibracion (calibrar, leerBateria, persistencia, aceptar, rechazar), sus respuestas a los dialogos
 * (tambien "Parar aquí" y "No"), apagar y encender el equipo, y desde Avanzado "Restaurar (#F)". El
 * Contexto se lee del equipo (FlujoCalibracion.identificar), no se rellena a mano; ninguna prueba llama a
 * plan(). El equipo simulado es SLV-002 tras el acta de las 12:23 (1 y 2 escritos, CAL,0003).
 */
public class RupturaSerieTest {

    private static final String MAC = "00:21:13:05:19:3B";
    private static final String HOY = "2026-09-20";
    private static final double PENDIENTE_8 = 0.21;
    private static final double PENDIENTE_B = 0.36;
    private static final double PENDIENTE_5 = 0.327;
    /** Decisiones con todo lo que el b necesita (las del APK no traen REMEDIDA-b: QA-3612-06). */
    private static final String REMEDIDA_B = "REMEDIDA-b,SLV-002,RF-CAL-18,2026-09-20,Diego,DOC-prueba.md";

    // ------------------------------------------------------------ andamiaje

    /** La app matada a mitad (el proceso muere; lo que hay en disco queda). */
    static final class AppMatada extends RuntimeException {
        AppMatada() {
            super("app matada (simulado)");
        }
    }

    /** El acta "en disco": un diario en memoria que sobrevive a matar la app, y las actas cerradas. */
    static final class Almacen implements FlujoCalibracion.AlmacenActa {
        StringWriter diario;
        Acta vivo;
        final List<String> cerradas = new ArrayList<>();
        final List<String> diariosCerrados = new ArrayList<>();

        @Override
        public Acta enCurso() throws IOException {
            if (vivo != null && !vivo.cerrada()) {
                return vivo;
            }
            if (diario == null) {
                return null;
            }
            Acta a = Acta.leer(new StringReader(diario.toString()));
            if (a == null || a.cerrada()) {
                return null;
            }
            a.continuarEn(diario);
            vivo = a;
            return a;
        }

        @Override
        public void adjuntar(Acta a) throws IOException {
            diario = new StringWriter();
            a.escribirEn(diario);
            vivo = a;
        }

        @Override
        public void cerrar(Acta a) {
            diariosCerrados.add(diario.toString());
            cerradas.add(diario.toString() + "\n" + a.texto());
            diario = null;
            vivo = null;
        }

        @Override
        public boolean aceptadoAntes(char k) throws IOException {
            for (String d : diariosCerrados) {
                Acta a = Acta.leer(new StringReader(d));
                Acta.Codigo c = a == null ? null : a.codigo(k);
                if (a != null && a.aceptada() && c != null && c.conforme() && c.restaurado == null) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public List<Acta> aceptadas() throws IOException {
            List<Acta> l = new ArrayList<>();
            for (String d : diariosCerrados) {
                Acta a = Acta.leer(new StringReader(d));
                if (a != null && a.aceptada()) {
                    l.add(a);
                }
            }
            return l;
        }

        /** Tras matar la app: la memoria se pierde, el disco no. */
        Almacen reabrir() {
            Almacen n = new Almacen();
            n.diario = diario;
            n.cerradas.addAll(cerradas);
            n.diariosCerrados.addAll(diariosCerrados);
            return n;
        }
    }

    /** El operador: coloca el patron que se le pide (con un factor de error), contesta y apaga el equipo. */
    static final class Operador implements FlujoCalibracion.Operador {
        final EquipoSimulado sim;
        final Map<String, Double> xPatron;
        double factor = 1.0;
        /** Colocaciones que el operador hace sin el patron debajo (se queda en el oscuro). */
        int ausentes = 0;
        /** Respuestas por comienzo de titulo (por defecto 0: la primera opcion). Se usan una vez. */
        final Map<String, Integer> respuestas = new HashMap<>();
        String matarEn;
        /** Veces que el operador dice haber apagado sin apagar (el enlace no cae). */
        int sinApagar = 0;
        final List<String> titulos = new ArrayList<>();
        private static final Pattern COLOQUE = Pattern.compile("(?:Coloque|colocar) (P\\d+)");

        Operador(EquipoSimulado sim, Map<String, Double> xPatron) {
            this.sim = sim;
            this.xPatron = xPatron;
        }

        @Override
        public int preguntar(String titulo, String mensaje, String... opciones) {
            titulos.add(titulo);
            if (matarEn != null && titulo.startsWith(matarEn)) {
                matarEn = null;
                throw new AppMatada();
            }
            for (Map.Entry<String, Integer> e : respuestas.entrySet()) {
                if (titulo.startsWith(e.getKey())) {
                    int r = e.getValue();
                    respuestas.remove(e.getKey());
                    return r;
                }
            }
            Matcher m = COLOQUE.matcher(mensaje);
            if (m.find() && xPatron.containsKey(m.group(1))) {
                if (ausentes > 0) {
                    ausentes--;
                    sim.x = 566;
                } else {
                    sim.x = xPatron.get(m.group(1)) * factor;
                }
            }
            return 0;
        }

        @Override
        public void progreso(String texto) {
        }

        @Override
        public boolean apagarYEncender() {
            if (sinApagar > 0) {
                sinApagar--;
                return false;          // el enlace no cayo: la app no lo da por apagado (P12 §5.1)
            }
            sim.apagarYEncender();     // el enlace cae y vuelve; la RAM se recarga de la EEPROM
            return true;
        }
    }

    private List<Patron> catalogo;
    private BancoCola cola;
    private EquipoSimulado sim;
    private Campana campana;
    private StringWriter diarioCampana;
    private Almacen almacen;
    private Operador operador;
    private FlujoCalibracion.Contexto ctx;
    private Decisiones decisiones;
    private Boolean apto = Boolean.TRUE;
    private double factorP49 = 1.0;
    private String sinHacer;
    private final Map<String, Double> xPatron = new HashMap<>();
    private final FlujoCalibracion.Reloj reloj = new FlujoCalibracion.Reloj() {
        @Override
        public String ahoraIso() {
            return HOY + "T10:00:00-0500";
        }

        @Override
        public String hoy() {
            return HOY;
        }
    };

    private static String decisionesDelApk() throws IOException {
        return new String(Files.readAllBytes(new File("src/main/assets/decisiones.csv").toPath()), StandardCharsets.UTF_8);
    }

    @Before
    public void preparar() throws Exception {
        try (InputStreamReader r = new InputStreamReader(
                new FileInputStream("src/main/assets/patrones_certificados_P1-P132.csv"), StandardCharsets.UTF_8)) {
            catalogo = Patron.leer(r);
        }
        cola = BancoCola.cargar(Files.readAllBytes(new File("src/main/assets/cola_banco_P1-P132.csv").toPath()));
        sim = EquipoSimulado.slv002();
        almacen = new Almacen();
        operador = new Operador(sim, xPatron);
        decisiones = Decisiones.leer(decisionesDelApk());
        rehacerCampana(true, true);
    }

    /** Otra campana, con o sin OSCURO y A5 (T-S06, T-S19). */
    private void rehacerCampana(boolean oscuro, boolean a5) throws IOException {
        campana = new Campana(catalogo, "SLV-002", MAC);
        diarioCampana = new StringWriter();
        campana.escribirEn(diarioCampana);
        xPatron.clear();
        medirBanco(oscuro, a5);
    }

    private void serie(BancoCola.Paso p, String patron, double[] medias, boolean aceptada, String veredicto)
            throws IOException {
        Campana.Serie s = campana.nuevaSerie("2026-09-20T08:00:00-0500", "SLV-002", MAC, "V3.6", patron, 0, 'e');
        for (int k = 0; k < medias.length; k++) {
            for (int i = 0; i < 4; i++) {
                double v = medias[k] + (i % 2 == 0 ? 0.5 : -0.5);
                campana.agregarDisparo(s, k + 1, "f", "::" + Math.round(v), v);
            }
        }
        campana.cerrar(s, veredicto, aceptada, "banco paso " + p.orden);
        if (aceptada) {
            campana.elegir(s);
        }
        campana.anotarPaso(p.orden, "HECHO", s.id, "f", "");
    }

    /**
     * El banco: OSCURO en 565 (sesiones 1 y 3) y 571 (sesion 2); A5 del inicio con ~1,6 % entre colocaciones;
     * los patrones del 8 (sesion 1), del b (sesion 2) y del 5 (sesion 3) sobre rectas por el oscuro.
     */
    private void medirBanco(boolean conOscuro, boolean conA5) throws IOException {
        for (BancoCola.Paso p : cola.pasos) {
            if (!conOscuro && "OSCURO".equals(p.tipo) || !conA5 && "A5".equals(p.tipo)) {
                continue;
            }
            if ("OSCURO".equals(p.tipo)) {
                double o = p.sesion == 2 ? 571 : 565;
                serie(p, Campana.OSCURO.nombre, new double[]{o, o + 1, o - 1, o, o}, true, "OK");
            } else if ("A5".equals(p.tipo) && "A5-INICIO".equals(p.bloque) && p.sesion == 1) {
                double x = p.xEsperada;
                serie(p, p.patron, new double[]{x * 0.98, x * 0.99, x, x * 1.01, x * 1.02}, false, A5.VEREDICTO);
            } else if ("PATRON".equals(p.tipo) && ("8".equals(p.codigo) || "b".equals(p.codigo) || "5".equals(p.codigo))) {
                if (p.patron.equals(sinHacer)) {
                    continue;
                }
                double x0 = "b".equals(p.codigo) ? 571 : 565;
                double a = "8".equals(p.codigo) ? PENDIENTE_8 : "b".equals(p.codigo) ? PENDIENTE_B : PENDIENTE_5;
                double f = "P49".equals(p.patron) ? factorP49 : 1.0;
                double x = x0 + f * p.certificado / a;
                xPatron.put(p.patron, x);
                serie(p, p.patron, new double[]{x, x, x, x, x}, true, "OK");
            }
        }
    }

    /** Como hace la app al conectar y pasar las pruebas: el Contexto sale del equipo (#V#, #GC#, #GN#). */
    private FlujoCalibracion flujo() throws Exception {
        ctx = FlujoCalibracion.identificar(sim, "SLV-002", MAC, apto, "6/6", "RTV 3.6.14 (3614)");
        sim.recibidas.clear();
        FlujoCalibracion f = new FlujoCalibracion(sim, operador, almacen, cola, campana, catalogo, decisiones, ctx, reloj);
        f.pin("1234");
        return f;
    }

    private static Set<Character> sel(char... ks) {
        Set<Character> s = new HashSet<>();
        for (char k : ks) {
            s.add(k);
        }
        return s;
    }

    private FlujoCalibracion.Tarjeta tarjeta(FlujoCalibracion f, char k) {
        for (FlujoCalibracion.Tarjeta t : f.tarjetas(null)) {
            if (t.k == k) {
                return t;
            }
        }
        throw new AssertionError(k + " sin tarjeta");
    }

    private boolean ajustado(char k) {
        return (sim.mascara & (1 << Fabrica.indice(k))) != 0;
    }

    /** El 8 solo, hasta ACEPTADA (P12 §6.3). */
    private void calibrarYAceptar8() throws Exception {
        FlujoCalibracion f = flujo();
        String r = f.calibrar(sel('8'), "Diego", "8 solo y primero; C-CAL-15 anotada");
        assertTrue(r, r.contains("Ahora: persistencia"));
        assertTrue(f.persistencia().startsWith("Persistencia OK"));
        String ac = f.aceptar();
        assertTrue(ac, ac.contains("ACEPTADA"));
    }

    // =================================================================== RUPTURAS (revisor)
    private static final String ZIP_1510 = "../../../06_Calibracion/SLV-002/campanas/campana_SLV-002_20260919_151045.zip";
    private static final String MAC2 = "AA:BB:CC:DD:EE:01";

    private static void L(String tag, Object s) {
        System.out.println("SALIDA[" + tag + "] " + s);
    }

    /** Canal que en la n-esima trama con 'pref' corta (IOException), enmudece (timeout) o mata la app. */
    static final class Cortador implements Canal {
        final EquipoSimulado s; final String pref; int n; final String modo;
        Cortador(EquipoSimulado s, String pref, int n, String modo) { this.s = s; this.pref = pref; this.n = n; this.modo = modo; }
        @Override
        public Cliente.Respuesta pedir(String t, Tramas.Tipo tipo, long to) throws IOException, InterruptedException {
            if (t.startsWith(pref) && --n == 0) {
                if ("mudo".equals(modo)) {
                    return new Cliente.Respuesta(t, Receptor.Desenlace.TIMEOUT, null, "", -1);
                }
                if ("matar".equals(modo)) {
                    throw new AppMatada();
                }
                s.conectado = false;
                throw new IOException("corte antes de " + t);
            }
            return s.pedir(t, tipo, to);
        }
    }

    private String lineasDiario(String prefijo) {
        StringBuilder b = new StringBuilder();
        for (String l : diarioCampana.toString().split("\n")) {
            if (l.startsWith(prefijo)) {
                b.append(l).append(" | ");
            }
        }
        return b.toString();
    }

    private String equipoActa() {
        if (almacen.cerradas.isEmpty()) {
            return "(sin acta cerrada)";
        }
        for (String l : almacen.cerradas.get(almacen.cerradas.size() - 1).split("\n")) {
            if (l.startsWith("Equipo:")) {
                return l;
            }
        }
        return "?";
    }

    @Test
    public void a1_corteTrasSNAntesDeLeerGN() throws Exception {
        try {
            FlujoCalibracion.renombrarSerie(new Cortador(sim, "#GN#", 2, "corte"), campana, "1234", "SLV-002-2026",
                    "SLV-002-2026", "Diego", "t1");
            fail();
        } catch (IOException e) {
            L("a1", "1a vez -> IOException: " + e.getMessage() + "; equipo=" + sim.serie + "; serieActual=" + campana.serieActual());
        }
        sim.conectado = true;
        String t = FlujoCalibracion.renombrarSerie(sim, campana, "1234", "SLV-002-2026", "SLV-002-2026", "Diego", "t2");
        L("a1", "repetir -> " + t);
        L("a1", "diario: " + lineasDiario("RENOMBRA"));
    }

    @Test
    public void a2_gnMudoTrasSN() throws Exception {
        String t = FlujoCalibracion.renombrarSerie(new Cortador(sim, "#GN#", 2, "mudo"), campana, "1234", "SLV-002-2026",
                "SLV-002-2026", "Diego", "t1");
        L("a2", "1a vez -> " + t);
        String t2 = FlujoCalibracion.renombrarSerie(sim, campana, "1234", "SLV-002-2026", "SLV-002-2026", "Diego", "t2");
        L("a2", "repetir -> " + t2 + " ; serieActual=" + campana.serieActual());
    }

    @Test
    public void a3_corteAntesDelSNYRepetir() throws Exception {
        try {
            FlujoCalibracion.renombrarSerie(new Cortador(sim, "#SN,", 1, "corte"), campana, "1234", "SLV-002-2026",
                    "SLV-002-2026", "Diego", "t1");
            fail();
        } catch (IOException e) {
            L("a3", "corte antes del #SN; equipo=" + sim.serie);
        }
        sim.conectado = true;
        String t = FlujoCalibracion.renombrarSerie(sim, campana, "1234", "SLV-002-2026", "SLV-002-2026", "Diego", "t2");
        L("a3", "repetir -> " + t);
        L("a3", "diario: " + lineasDiario("RENOMBRA"));
    }

    @Test
    public void b1_snNoEntraYRevierte() throws Exception {
        sim.inyectar("#SN,", EquipoSimulado.Falla.OK_SIN_HACER, 1);
        String t = FlujoCalibracion.renombrarSerie(sim, campana, "1234", "SLV-002-2026", "SLV-002-2026", "Diego", "t1");
        L("b1", "-> " + t);
        L("b1", "serieActual=" + campana.serieActual() + " historial=" + campana.historialSeries()
                + " conHistoria=" + campana.serieConHistoria());
        L("b1", "diario: " + lineasDiario("RENOMBRA"));
        Campana re = new Campana(catalogo, "SLV-002", MAC);
        re.leerDiario(new StringReader(diarioCampana.toString()));
        L("b1", "releido serieActual=" + re.serieActual());
        FlujoCalibracion f = flujo();
        L("b1", "previas con equipo SLV-002: " + f.motivoPrevias());
        sim.serie = "SLV-002-2026";
        f = flujo();
        L("b1", "previas con equipo SLV-002-2026: " + f.motivoPrevias());
        String t2 = FlujoCalibracion.renombrarSerie(sim, campana, "1234", "SLV-002-2026", "SLV-002-2026", "Diego", "t2");
        L("b1", "repetir -> " + t2);
        L("b1", "serieActual tras repetir=" + campana.serieActual());
        f = flujo();
        String r = f.calibrarTodo(sel('8'), "Diego", "x");
        L("b1", "calibrar -> " + r.substring(0, Math.min(40, r.length())) + " ; acta " + equipoActa());
    }

    @Test
    public void b2_revierteNoViajaAlCopiarNiAlImportar() throws Exception {
        sim.inyectar("#SN,", EquipoSimulado.Falla.ERR, 1);
        String t = FlujoCalibracion.renombrarSerie(sim, campana, "1234", "SLV-002-2026", "SLV-002-2026", "Diego", "t1");
        L("b2", "-> " + t + " ; origen serieActual=" + campana.serieActual());
        Campana nueva = new Campana(catalogo, "SLV-002", MAC);
        nueva.escribirEn(new StringWriter());
        int k = nueva.copiarRenombrados(campana.renombrados(), "archivada");
        L("b2", "Nueva campana (copiarRenombrados): copiados=" + k + " serieActual=" + nueva.serieActual()
                + " conHistoria=" + nueva.serieConHistoria());
        Campana otra = new Campana(catalogo, "SLV-002", MAC);
        otra.escribirEn(new StringWriter());
        ImportadorCampana.Resultado ir = ImportadorCampana.importarDiario(otra, diarioCampana.toString(), catalogo, "zip A");
        L("b2", "importar en B -> renombrados=" + ir.renombrados + " serieActual=" + otra.serieActual());
    }

    @Test
    public void c_appMatadaEntreRenombraYSN() throws Exception {
        try {
            FlujoCalibracion.renombrarSerie(new Cortador(sim, "#SN,", 1, "matar"), campana, "1234", "SLV-002-2026",
                    "SLV-002-2026", "Diego", "t1");
            fail();
        } catch (AppMatada e) {
            L("c", "app matada; equipo=" + sim.serie + "; diario: " + lineasDiario("RENOMBRA"));
        }
        Campana re = new Campana(catalogo, "SLV-002", MAC);
        re.leerDiario(new StringReader(diarioCampana.toString()));
        re.escribirEn(diarioCampana);
        campana = re;
        almacen = almacen.reabrir();
        FlujoCalibracion f = flujo();
        L("c", "reabrir: serieActual=" + re.serieActual() + " previas=" + f.motivoPrevias());
        String r = f.calibrarTodo(sel('8'), "Diego", "x");
        L("c", "calibrar SIN repetir el cambio -> " + r.substring(0, Math.min(40, r.length())) + " ; acta " + equipoActa()
                + " ; #GN# del equipo=" + sim.serie);
    }

    @Test
    public void d_renombrarNuevaCampanaCalibrar() throws Exception {
        FlujoCalibracion.renombrarSerie(sim, campana, "1234", "SLV-002-2026", "SLV-002-2026", "Diego", "t1");
        Campana nueva = new Campana(catalogo, "SLV-002", MAC);
        nueva.escribirEn(new StringWriter());
        nueva.copiarRenombrados(campana.renombrados(), "campana_SLV-002_00211305193B_archivada_x.csv");
        campana = nueva;
        FlujoCalibracion f = flujo();
        StringBuilder b = new StringBuilder();
        for (FlujoCalibracion.Previa p : f.previas()) {
            if (!p.ok) {
                b.append(p.texto).append(" | ");
            }
        }
        L("d", "previas que fallan en la campana nueva: " + b + " ; canonico=" + TablaCalibracion.canonico(nueva.historialSeries(), MAC));
    }

    @Test
    public void d2_claveRenombradaEnJvm() throws Exception {
        File dir = Files.createTempDirectory("camp").toFile();
        Files.write(new File(dir, "campana_SLV-002_00211305193B_archivada_20260920_090000.csv").toPath(),
                ("RENOMBRA,t,SLV-002,SLV-002-2026,Diego," + MAC + "\n").getBytes(StandardCharsets.UTF_8));
        try {
            L("d2", "claveRenombrada(SLV-002-2026) con solo la archivada = " + Campanas.claveRenombrada(dir, "SLV-002-2026", MAC));
            Files.write(new File(dir, "campana_SLV-002-2026_00211305193B.csv").toPath(),
                    ("RENOMBRA,t,SLV-002,SLV-002-2026,Diego," + MAC + "\n").getBytes(StandardCharsets.UTF_8));
            L("d2", "claveRenombrada(SLV-002) con campana_SLV-002-2026_* = " + Campanas.claveRenombrada(dir, "SLV-002", MAC));
        } catch (Throwable e) {
            L("d2", "no ejecutable en JVM: " + e);
        }
    }

    @Test
    public void e_otroTelefonoImportaSoporteDeA() throws Exception {
        campana.elegirCola("REPRESENTATIVO", "70ef3b868db85ef75743936a6218935e");
        String diarioA = diarioCampana.toString();
        StringWriter dB = new StringWriter();
        Campana b = new Campana(catalogo, "SLV-002", MAC);
        b.escribirEn(dB);
        b.elegirCola("COMPLETO", cola.md5);
        ImportadorCampana.Resultado r1 = ImportadorCampana.importarDiario(b, diarioA, catalogo, "soporte A 1");
        L("e", "1a importacion en B (B abrio el Banco: COLA COMPLETO) -> " + r1.texto() + " ; pasos B=" + b.pasos().size());
        campana = b;
        String t = FlujoCalibracion.renombrarSerie(sim, b, "1234", "SLV-002-2026", "SLV-002-2026", "Diego", "t");
        L("e", "renombrar en B -> " + t);
        FlujoCalibracion f = flujo();
        L("e", "previas B -> " + f.motivoPrevias());
        ImportadorCampana.Resultado r2 = ImportadorCampana.importarDiario(b, diarioA, catalogo, "soporte A 2");
        L("e", "2a importacion en B -> " + r2.texto() + " ; renombrados=" + r2.renombrados + " serieActual=" + b.serieActual());
        // variante: B sin haber abierto el Banco
        Campana b2 = new Campana(catalogo, "SLV-002", MAC);
        b2.escribirEn(new StringWriter());
        ImportadorCampana.Resultado r3 = ImportadorCampana.importarDiario(b2, diarioA, catalogo, "soporte A");
        L("e", "B sin Banco abierto -> " + r3.texto() + " ; cola=" + b2.colaTipo() + " pasos=" + b2.pasos().size());
    }

    @Test
    public void f_zipReal1510EnCampanaRenombrada() throws Exception {
        String diario = ImportadorCampana.diarioDeZip(Files.readAllBytes(new File(ZIP_1510).toPath()));
        for (String modo : new String[]{"sin cola", "REPRESENTATIVO", "alias SLV-002-2026"}) {
            EquipoSimulado s = EquipoSimulado.slv002();
            Campana c = new Campana(catalogo, "alias SLV-002-2026".equals(modo) ? "SLV-002-2026" : "SLV-002", MAC);
            StringWriter w = new StringWriter();
            c.escribirEn(w);
            if ("alias SLV-002-2026".equals(modo)) {
                c.renombrar("SLV-002", "SLV-002-2026", "t", "Diego");
            } else {
                FlujoCalibracion.renombrarSerie(s, c, "1234", "SLV-002-2026", "SLV-002-2026", "Diego", "t");
            }
            if ("REPRESENTATIVO".equals(modo)) {
                c.elegirCola("REPRESENTATIVO", "70ef3b868db85ef75743936a6218935e");
            }
            try {
                ImportadorCampana.Resultado r = ImportadorCampana.importarDiario(c, diario, catalogo, "zip 15:10");
                L("f", modo + " -> " + r.texto() + " ; cola=" + c.colaTipo() + " md5='" + c.colaMd5() + "' pasos="
                        + c.pasos().size() + " serieActual=" + c.serieActual());
            } catch (RuntimeException e) {
                L("f", modo + " -> EXCEPCION " + e.getMessage());
            }
        }
    }

    @Test
    public void g_otraMac() throws Exception {
        EquipoSimulado s = EquipoSimulado.slv002();
        Campana c = new Campana(catalogo, "SLV-002", MAC2);
        c.escribirEn(new StringWriter());
        L("g", "canonico(SLV-002, MAC2) = " + TablaCalibracion.canonico(c.historialSeries(), MAC2));
        L("g", "renombrar a SLV-002-2026 -> " + FlujoCalibracion.renombrarSerie(s, c, "1234", "SLV-002-2026",
                "SLV-002-2026", "Diego", "t"));
        Campana c2 = new Campana(catalogo, "SLV-002-2026", MAC2);
        L("g", "canonico(SLV-002-2026, MAC2) = " + TablaCalibracion.canonico(c2.historialSeries(), MAC2));
        String diario = ImportadorCampana.diarioDeZip(Files.readAllBytes(new File(ZIP_1510).toPath()));
        try {
            ImportadorCampana.importarDiario(c, diario, catalogo, "zip 15:10");
            L("g", "importar ZIP de SLV-002 en MAC2 -> ENTRA");
        } catch (RuntimeException e) {
            L("g", "importar ZIP de SLV-002 en MAC2 -> " + e.getMessage());
        }
        EquipoSimulado blanco = new EquipoSimulado();
        Campana c3 = new Campana(catalogo, "SLV-002", MAC2);
        c3.escribirEn(new StringWriter());
        String t = FlujoCalibracion.renombrarSerie(blanco, c3, "2026", "OTRO-01", "OTRO-01", "Diego", "t");
        L("g", "equipo en blanco NONE -> OTRO-01: " + t + " ; historial=" + c3.historialSeries() + " canonico="
                + TablaCalibracion.canonico(c3.historialSeries(), MAC2));
        EquipoSimulado blanco2 = new EquipoSimulado();
        Campana c5 = new Campana(catalogo, "SLV-002", MAC);
        c5.escribirEn(new StringWriter());
        String t5 = FlujoCalibracion.renombrarSerie(blanco2, c5, "2026", "SLV-002-2026", "SLV-002-2026", "Diego", "t");
        L("g", "MAC real, placa en blanco NONE -> SLV-002-2026: " + t5 + " ; historial=" + c5.historialSeries());
    }

    @Test
    public void h_destinoConPasosYColaDistinta() throws Exception {
        int antes = campana.pasos().size();
        campana.elegirCola("COMPLETO", cola.md5);
        L("h", "COLA COMPLETO sobre banco hecho: pasos " + antes + " -> " + campana.pasos().size());
        StringWriter dr = new StringWriter();
        Campana rep = new Campana(catalogo, "SLV-002", MAC);
        rep.escribirEn(dr);
        rep.elegirCola("REPRESENTATIVO", "70ef3b868db85ef75743936a6218935e");
        Campana.Serie s = rep.nuevaSerie("2026-09-21T09:00:00-0500", "SLV-002", MAC, "V3.6", "P1", 0, 'e');
        for (int i = 0; i < 4; i++) {
            rep.agregarDisparo(s, 1, "g" + i, "::1500", 1500 + i);
        }
        rep.cerrar(s, "OK", true, "");
        rep.elegir(s);
        rep.anotarPaso(3, "HECHO", s.id, "t", "");
        ImportadorCampana.Resultado r = ImportadorCampana.importarDiario(campana, dr.toString(), catalogo, "zip rep");
        L("h", "ZIP REPRESENTATIVO -> destino COMPLETO con pasos: " + r.texto() + " ; cola=" + campana.colaTipo() + " pasos="
                + campana.pasos().size() + " paso3=" + campana.pasos().get(3));
        Campana d = new Campana(catalogo, "SLV-002", MAC);
        d.escribirEn(new StringWriter());
        ImportadorCampana.importarDiario(d, dr.toString(), catalogo, "zip rep");
        int pd = d.pasos().size();
        ImportadorCampana.Resultado r2 = ImportadorCampana.importarDiario(d, diarioCampana.toString(), catalogo, "zip completo");
        L("h", "ZIP COMPLETO -> destino REPRESENTATIVO con pasos: " + r2.texto() + " ; cola=" + d.colaTipo() + " pasos " + pd + " -> "
                + d.pasos().size());
    }

    @Test
    public void i_instalarEncimaDe3611ConElDiarioReal() throws Exception {
        String diario = ImportadorCampana.diarioDeZip(Files.readAllBytes(new File(ZIP_1510).toPath()));
        Campana c = new Campana(catalogo, "SLV-002", MAC);
        int malas = c.leerDiario(new StringReader(diario));
        boolean autoCola = !c.colaElegida() && c.pasos().isEmpty();
        L("i", "lineasMalas=" + malas + " series=" + c.series().size() + " colaElegida=" + c.colaElegida() + " colaTipo="
                + c.colaTipo() + " md5='" + c.colaMd5() + "' pasos=" + c.pasos() + " ; BancoActivity elige por defecto: " + autoCola
                + " -> cola " + BancoCola.Tipo.de(c.colaTipo()));
    }

    @Test
    public void j_diario3616ParaEl3615() throws Exception {
        sim.inyectar("#SN,", EquipoSimulado.Falla.ERR, 1);
        FlujoCalibracion.renombrarSerie(sim, campana, "1234", "SLV-002-2026", "SLV-002-2026", "Diego", "t1");
        campana.elegirCola("REPRESENTATIVO", "70ef3b868db85ef75743936a6218935e");
        java.util.Set<String> conocidos3615 = new java.util.HashSet<>(java.util.Arrays.asList("SERIE", "DISPARO", "DESCARTE",
                "VEREDICTO", "REASIGNA", "ELIGE", "CIERRE", "EXPORTA", "PASO", "RENOMBRA", "COLA", "ANULA", "BATERIA"));
        int desconocidas = 0;
        StringBuilder b = new StringBuilder();
        for (String l : diarioCampana.toString().split("\n")) {
            if (l.isEmpty() || l.startsWith("#")) {
                continue;
            }
            String ev = l.split(",")[0];
            if (!conocidos3615.contains(ev)) {
                desconocidas++;
                b.append(ev).append(' ');
            }
        }
        L("j", "lineas que el lector de la 3.6.15 no entiende: " + desconocidas + " (" + b + ")");
    }

    @Test
    public void k_diarioSinSeriesDeOtraMac() throws Exception {
        String ajeno = "RENOMBRA,t,SLV-002,SLV-003-2026,Nordeste,\"AA:BB:CC:DD:EE:01\"\nCOLA,REPRESENTATIVO,x\n";
        ImportadorCampana.Resultado r = ImportadorCampana.importarDiario(campana, ajeno, catalogo, "zip Nordeste");
        L("k", "diario de otra MAC sin SERIE -> " + r.texto() + " renombrados=" + r.renombrados + " ; serieActual="
                + campana.serieActual() + " historial=" + campana.historialSeries() + " cola=" + campana.colaTipo());
    }
}
