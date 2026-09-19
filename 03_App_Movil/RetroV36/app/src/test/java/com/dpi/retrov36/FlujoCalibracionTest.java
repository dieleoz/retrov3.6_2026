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
public class FlujoCalibracionTest {

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
        campana.escribirEn(new StringWriter());
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

    // --------------------------------------------------------------- T-S07

    @Test
    public void caminoFelizDel8() throws Exception {
        FlujoCalibracion f = flujo();
        assertTrue(ctx.es362);
        assertEquals("SLV-002", ctx.serieGN);
        assertNull(f.motivoPrevias());
        assertTrue(tarjeta(f, '8').casilla);
        assertFalse(f.puedeAceptar());
        int apagados = sim.apagados;
        String r = f.calibrar(sel('8'), "Diego", "banco completo");
        assertTrue(r, r.contains("Ahora: persistencia"));
        assertEquals("T-C41 empieza apagando y encendiendo", apagados + 1, sim.apagados);
        Acta a = f.acta();
        assertTrue(a.codigo('8').conforme());
        assertTrue(a.dato("heredados (T-C41)"), a.dato("heredados (T-C41)").startsWith("OK"));
        assertTrue(a.dato("rango 8"), a.dato("rango 8").contains("0-122"));
        assertFalse(f.puedeAceptar());
        assertTrue(f.persistencia().startsWith("Persistencia OK"));
        assertEquals(apagados + 2, sim.apagados);
        assertTrue(sim.curvaEeprom('8').igualFloat32(a.codigo('8').leida));
        assertTrue(f.puedeAceptar());
        String ac = f.aceptar();
        assertTrue(ac, ac.contains("ACEPTADA"));
        assertEquals(HOY, sim.fecha);
        assertEquals(1, sim.cuantas("#SC,"));
        assertEquals(1, sim.cuantas("#S,8,"));
        assertEquals(0, sim.cuantas("#S,1,") + sim.cuantas("#S,2,"));
        String acta = almacen.cerradas.get(0);
        assertTrue(acta.contains("oscuro 8: Oscuro (x = 565"));
        assertTrue(acta, acta.contains("código 5: PA-14"));
        assertTrue(acta.contains("Conformidad: Diego: código 8."));
        assertTrue(acta.contains("Estado: ACEPTADA"));
    }

    @Test
    public void persistenciaSinApagarNoVale() throws Exception {
        FlujoCalibracion f = flujo();
        f.calibrar(sel('8'), "Diego", "x");
        operador.sinApagar = 1;
        String p = f.persistencia();
        assertTrue(p, p.contains("NO válida"));
        assertFalse(f.puedeAceptar());
        assertTrue(f.persistencia().startsWith("Persistencia OK"));
        assertTrue(f.puedeAceptar());
    }

    @Test
    public void tC41ExigeApagarAntesDeEmpezar() throws Exception {
        operador.sinApagar = 1;
        FlujoCalibracion f = flujo();
        String r = f.calibrar(sel('8'), "Diego", "x");
        assertTrue(r, r.contains("T-C41"));
        assertEquals(0, sim.cuantas("#S,"));
    }

    @Test
    public void conPruebasNoAptoNoSeCalibra() throws Exception {
        apto = Boolean.FALSE;
        FlujoCalibracion f = flujo();
        assertFalse(f.puedeCalibrar(sel('8')));
        String r = f.calibrar(sel('8'), "Diego", "x");
        assertTrue(r, r.contains("NO APTO"));
        assertTrue(sim.recibidas.isEmpty());
    }

    // ------------------------------------------------------- P12 §6.3, §5.2, §6.6

    @Test
    public void elOchoVaSoloYElBYEl5DespuesDeSuActa() throws Exception {
        FlujoCalibracion f = flujo();
        // 3.6.15: la casilla se puede marcar de antemano (irá en la misma sesión, tras el 8)...
        assertTrue(tarjeta(f, 'b').casilla);
        assertTrue(tarjeta(f, 'b').texto.contains("Irá después del acta ACEPTADA del código 8"));
        // ...pero "un código" no escribe el b ni el 5 antes del acta del 8.
        String r = f.calibrar(sel('8', '5'), "Diego", "x");
        assertTrue(r, r.contains("va después") || r.contains("va solo"));
        assertEquals(0, sim.cuantas("#S,"));
        calibrarYAceptar8();
        FlujoCalibracion g = flujo();
        assertTrue(tarjeta(g, 'b').casilla);
        assertTrue(tarjeta(g, '5').casilla);
    }

    @Test
    public void elBSinReglaDeReMedidaNoSeEscribe() throws Exception {
        decisiones = Decisiones.leer(decisionesDelApk().replaceAll("(?m)^REMEDIDA-b.*$", ""));
        calibrarYAceptar8();
        FlujoCalibracion f = flujo();
        FlujoCalibracion.Tarjeta b = tarjeta(f, 'b');
        assertFalse(b.casilla);
        assertTrue(b.texto, b.texto.contains("REMEDIDA-b"));
        String r = f.calibrar(sel('b'), "Operador", "marco la casilla");
        assertTrue(r, r.contains("no se escribe"));
        assertEquals(0, sim.cuantas("#S,b"));
    }

    /** El b con PA-24 y P49 al -10,7 %: dispensado dentro de su alcance; la re-medida por RF-CAL-18. */
    @Test
    public void elBConPa24DentroDelAlcance() throws Exception {
        factorP49 = 0.8;
        rehacerCampana(true, true);
        decisiones = Decisiones.leer("PA-24,SLV-002,SI,2026-09-19,Diego,DOC.md,\"RF-CAL-14 P39 +12 5; RF-CAL-14 P38 +12 5; "
                + "RF-CAL-14 P49 -10 3; RF-CAL-15 I 10 5\"\n" + REMEDIDA_B);
        calibrarYAceptar8();
        FlujoCalibracion f = flujo();
        FlujoCalibracion.Tarjeta b = tarjeta(f, 'b');
        assertTrue(b.texto, b.casilla);
        assertTrue(b.texto.contains("dispensado por"));
        String r = f.calibrar(sel('b'), "Diego", "PA-24");
        assertTrue(r, r.contains("Ahora: persistencia"));
        Acta.Codigo c = f.acta().codigo('b');
        String t = c.intentos.get(c.intentos.size() - 1).texto;
        assertTrue(t, t.contains("RF-CAL-18 frente a la curva escrita"));
        assertTrue(t, t.contains("INCUMPLE, dispensado por RF-CAL-14 (PA-24) (alcance RF-CAL-14 P49 -10.0 % ±3.0)"));
        assertTrue(f.acta().conformidades().toString().contains("código b"));
        f.persistencia();
        assertTrue(f.aceptar().contains("ACEPTADA"));
    }

    /** P12 §5.2: lo que no figura en el alcance (P39, P38, RMS) no queda dispensado. */
    @Test
    public void laDispensaDelBNoCubreLoQueNoFirmoDiego() throws Exception {
        factorP49 = 0.8;
        rehacerCampana(true, true);
        decisiones = Decisiones.leer("PA-24,SLV-002,SI,2026-09-19,Diego,DOC.md,\"RF-CAL-14 P49 -10 3\"\n" + REMEDIDA_B);
        calibrarYAceptar8();
        FlujoCalibracion f = flujo();
        FlujoCalibracion.Tarjeta b = tarjeta(f, 'b');
        assertFalse(b.casilla);
        assertTrue(b.texto, b.texto.contains("FUERA de lo dispensado"));
        assertTrue(b.texto.contains("SIN dispensa"));
    }

    @Test
    public void el5ExigeSus13PasosYDeclaraElRango() throws Exception {
        sinHacer = "P66";
        rehacerCampana(true, true);
        calibrarYAceptar8();
        FlujoCalibracion f = flujo();
        FlujoCalibracion.Tarjeta t = tarjeta(f, '5');
        assertFalse(t.casilla);
        assertTrue(t.texto, t.texto.contains("P66"));
        // Se mide el paso que faltaba (otra campana con el banco completo; el acta del 8 sigue aceptada).
        sinHacer = null;
        rehacerCampana(true, true);
        FlujoCalibracion g = flujo();
        FlujoCalibracion.Tarjeta t2 = tarjeta(g, '5');
        assertTrue(t2.texto, t2.casilla);
        assertTrue(t2.texto.contains("P17") && t2.texto.contains("P81") && t2.texto.contains("P66"));
        String r = g.calibrar(sel('5'), "Diego", "PA-14; rango 0-102; tipos IV, IX y XI");
        assertTrue(r, r.contains("Ahora: persistencia"));
        assertTrue(g.acta().dato("rango 5"), g.acta().dato("rango 5").contains("0-102"));
        assertTrue(g.acta().dato("rango 5").contains("[IV, IX, XI]"));
    }

    /** C-P12-4: la cobertura del 5 solo pasa porque el ancla cuenta como nivel (RF-APP-42). */
    @Test
    public void laCoberturaDel5DependeDelAncla() {
        List<Double> v = new ArrayList<>();
        for (Patron p : catalogo) {
            if (Asistente.deCodigo(p, '5')) {
                v.add(p.valor);
            }
        }
        assertEquals("sin el ancla el 5 no es ajustable", 0, Asistente.coberturaValores('5', v).gradoMaximo);
        v.add(0.0);
        assertTrue("con el ancla sí", Asistente.coberturaValores('5', v).gradoMaximo >= 1);
    }

    // ---------------------------------------------------------- D-20 / M1

    @Test
    public void dosReMedidasNoConformesRestauranYNoSeAcepta() throws Exception {
        operador.factor = 0.85;
        FlujoCalibracion f = flujo();
        String r = f.calibrar(sel('8'), "Diego", "x");
        assertTrue(r, r.contains("restaurado"));
        Acta a = f.acta();
        assertEquals(2, a.codigo('8').validos());
        assertNotNull(a.codigo('8').restaurado);
        assertFalse(ajustado('8'));
        assertEquals(1, sim.cuantas("#F,8#"));
        assertFalse(tarjeta(f, '8').casilla);
        assertFalse(f.puedePersistencia());
        assertFalse(f.puedeAceptar());
    }

    @Test
    public void restauracionNoVerificadaNoResuelveYSeReintenta() throws Exception {
        operador.factor = 0.85;
        sim.inyectar("#F,8#", EquipoSimulado.Falla.OK_SIN_HACER, 1);
        FlujoCalibracion f = flujo();
        String r = f.calibrar(sel('8'), "Diego", "x");
        assertTrue(r, r.contains("RESTAURACIÓN NO VERIFICADA"));
        Acta a = f.acta();
        assertNull(a.codigo('8').restaurado);
        assertNotNull(a.codigo('8').restauracionFallida);
        assertFalse(f.puedeAceptar());
        assertTrue(f.puedeCalibrar(sel()));
        String r2 = f.calibrar(sel(), "", "");
        assertTrue(r2, r2.contains("restaurado"));
        assertFalse(ajustado('8'));
    }

    // ------------------------------------------------ "Parar aquí" y "No" (P12 §4)

    @Test
    public void pararAquiDejaElActaAMediasYSeContinua() throws Exception {
        operador.respuestas.put("Re-medida del código 8", 1);
        FlujoCalibracion f = flujo();
        String r = f.calibrar(sel('8'), "Diego", "x");
        assertTrue(r, r.startsWith("Parado"));
        assertEquals(1, f.pendientes());
        assertEquals(0, f.acta().codigo('8').intentos.size());
        String r2 = f.calibrar(sel(), "", "");
        assertTrue(r2, r2.contains("Ahora: persistencia"));
        assertEquals(1, sim.cuantas("#S,8,"));
    }

    @Test
    public void corteDuranteSQueNoEntroYNoSeRepite() throws Exception {
        sim.inyectar("#S,8,", EquipoSimulado.Falla.CORTE_ANTES, 1);
        FlujoCalibracion f = flujo();
        try {
            f.calibrar(sel('8'), "Diego", "x");
            fail();
        } catch (IOException e) {
            // corte
        }
        sim.conectado = true;
        operador.respuestas.put("El #S del código 8 no entró", 1);   // "No"
        FlujoCalibracion g = flujo();
        String r = g.calibrar(sel(), "", "");
        assertTrue(r, r.contains("no se ha repetido"));
        assertEquals(0, g.acta().escribiendo());
        assertNull(g.acta().codigo('8'));
        assertEquals("con No, no se repite el #S", 0, sim.cuantas("#S,8,"));
    }

    // ------------------------------------------------------ RF-APP-37, T-S10

    @Test
    public void corteDuranteSQueEntroSeResuelveAlVolver() throws Exception {
        sim.inyectar("#S,8,", EquipoSimulado.Falla.CORTE_DESPUES, 1);
        FlujoCalibracion f = flujo();
        try {
            f.calibrar(sel('8'), "Diego", "x");
            fail("el corte llega como excepción");
        } catch (IOException e) {
            assertTrue(e.getMessage().contains("#S,8"));
        }
        assertEquals('8', f.acta().escribiendo());
        sim.conectado = true;
        FlujoCalibracion g = flujo();
        String r = g.calibrar(sel(), "", "");
        assertTrue(r, r.contains("Ahora: persistencia"));
        Acta a = g.acta();
        assertTrue(a.codigo('8').conforme());
        assertTrue(a.codigo('8').metodo.contains("anclada"));
        assertTrue(a.codigo('8').oscuro.contains("x = 565"));
        assertTrue(a.codigo('8').conformidad.contains("Diego"));
    }

    @Test
    public void corteDuranteSQueNoEntroOfreceRepetirlo() throws Exception {
        sim.inyectar("#S,8,", EquipoSimulado.Falla.CORTE_ANTES, 1);
        FlujoCalibracion f = flujo();
        try {
            f.calibrar(sel('8'), "Diego", "x");
            fail();
        } catch (IOException e) {
            // corte
        }
        sim.conectado = true;
        FlujoCalibracion g = flujo();
        String r = g.calibrar(sel(), "", "");
        assertTrue(operador.titulos.contains("El #S del código 8 no entró"));
        assertTrue(r, r.contains("Ahora: persistencia"));
        assertEquals("tras reconectar se repite el #S una vez", 1, sim.cuantas("#S,8,"));
    }

    /** QA-3613-01: rechazar con un #S sin resolver restaura ese codigo. */
    @Test
    public void rechazarConUnCorteSinResolverRestaura() throws Exception {
        sim.inyectar("#S,8,", EquipoSimulado.Falla.CORTE_DESPUES, 1);
        FlujoCalibracion f = flujo();
        try {
            f.calibrar(sel('8'), "Diego", "x");
            fail();
        } catch (IOException e) {
            // corte tras escribir
        }
        assertTrue(ajustado('8'));
        sim.conectado = true;
        FlujoCalibracion g = flujo();
        String r = g.rechazar("corte");
        assertTrue(r, r.contains("(#S sin resolver) restaurado"));
        assertEquals(1, sim.cuantas("#F,8#"));
        assertFalse(ajustado('8'));
        assertTrue(almacen.cerradas.get(0).contains("RECHAZADA"));
    }

    // ------------------------------------------------------------ T-S09

    @Test
    public void appMatadaEnLaReMedidaSeReanudaSinReescribir() throws Exception {
        operador.matarEn = "Levante y apoye (3";
        FlujoCalibracion f = flujo();
        try {
            f.calibrar(sel('8'), "Diego", "x");
            fail();
        } catch (AppMatada e) {
            // el proceso muere
        }
        almacen = almacen.reabrir();
        FlujoCalibracion g = flujo();
        assertEquals(1, g.pendientes());
        String r = g.calibrar(sel(), "", "");
        assertTrue(r, r.contains("Ahora: persistencia"));
        assertEquals("al reanudar no se reescribe", 0, sim.cuantas("#S,8,"));
    }

    @Test
    public void tS09CorteEnLaReMedidaQuedaAnotadoYSeRepite() throws Exception {
        sim.inyectar("8", EquipoSimulado.Falla.CORTE_ANTES, 1);
        FlujoCalibracion f = flujo();
        try {
            f.calibrar(sel('8'), "Diego", "x");
            fail();
        } catch (IOException e) {
            // corte en la re-medida
        }
        assertEquals("INTERRUMPIDA", f.acta().codigo('8').intentos.get(0).estado);
        sim.conectado = true;
        String r = flujo().calibrar(sel(), "", "");
        assertTrue(r, r.contains("Ahora: persistencia"));
    }

    // ---------------------------------------------------- P10-C5, P11 caso 3

    @Test
    public void fDesdeAvanzadoConElActaAbiertaLaInvalida() throws Exception {
        FlujoCalibracion f = flujo();
        f.calibrar(sel('8'), "Diego", "x");
        String t = FlujoCalibracion.fabricaDesdeAvanzado(sim, almacen, "1234", false, '1');
        assertTrue(t, t.contains("INVALIDADA"));
        FlujoCalibracion g = flujo();
        assertTrue(g.acta().invalidada());
        assertNotNull(g.motivoPrevias());
        assertFalse(g.puedeAceptar());
    }

    @Test
    public void unHeredadoDevueltoAFabricaSinActaImpideCalibrar() throws Exception {
        FlujoCalibracion.fabricaDesdeAvanzado(sim, almacen, "1234", false, '1');
        FlujoCalibracion f = flujo();
        String r = f.calibrar(sel('8'), "Diego", "x");
        assertTrue(r, r.contains("T-C41 FALLA"));
        assertEquals(0, sim.cuantas("#S,"));
    }

    @Test
    public void unHeredadoTocadoDespuesDeLaPersistenciaImpideAceptar() throws Exception {
        FlujoCalibracion f = flujo();
        f.calibrar(sel('8'), "Diego", "x");
        f.persistencia();
        // #F,2 por otra via (no la app): el acta no se entera, la verificacion final si.
        sim.pedir("#L,1234#", Tramas.Tipo.ADMIN, 2000);
        sim.pedir("#F,2#", Tramas.Tipo.ADMIN, 5000);
        sim.recibidas.clear();
        String ac = f.aceptar();
        assertTrue(ac, ac.contains("Verificación final FALLA"));
        assertEquals("2026-09-19", sim.fecha);
        assertEquals(0, sim.cuantas("#SC,"));
    }

    // ------------------------------------------------------------- P10-C5

    @Test
    public void siSCFallaElActaNoSeCierraYSeReintenta() throws Exception {
        FlujoCalibracion f = flujo();
        f.calibrar(sel('8'), "Diego", "x");
        f.persistencia();
        sim.inyectar("#SC,", EquipoSimulado.Falla.ERR, 1);
        String r = f.aceptar();
        assertTrue(r, r.contains("NO se cierra"));
        assertFalse(f.acta().cerrada());
        assertTrue(f.aceptar().contains("ACEPTADA"));
    }

    // ------------------------------------------------- QA-3612-05, QA-3613-06

    @Test
    public void bateriaACeroBloqueaYTrasCambiarlaSeSigue() throws Exception {
        sim.bateriaN = 0;
        FlujoCalibracion f = flujo();
        String r = f.calibrar(sel('8'), "Diego", "x");
        assertTrue(r, r.contains("No se escribe"));
        assertEquals(0, sim.cuantas("#S,"));
        assertFalse(f.puedeCalibrar(sel('8')));
        sim.bateriaN = 120;
        f.leerBateria();
        assertTrue(f.puedeCalibrar(sel('8')));
        assertTrue(f.calibrar(sel('8'), "Diego", "x").contains("Ahora: persistencia"));
    }

    @Test
    public void bateriaEnCampanaCerradaTambienLevantaElBloqueo() throws Exception {
        sim.bateriaN = 0;
        FlujoCalibracion f = flujo();
        f.calibrar(sel('8'), "Diego", "x");
        campana.cerrarCampana("2026-09-20");
        sim.bateriaN = 120;
        f.leerBateria();
        assertNull(f.motivoPrevias());
    }

    // ---------------------------------------------------------- QA-3613-02

    @Test
    public void unLSinRespuestaOlvidaElPinParaVolverAPedirlo() throws Exception {
        FlujoCalibracion f = flujo();
        sim.inyectar("#L,", EquipoSimulado.Falla.MUDO, 1);
        String r = f.calibrar(sel('8'), "Diego", "x");
        assertTrue(r, r.contains("no respondió a #L"));
        assertTrue(f.necesitaPin());
        f.pin("1234");
        assertFalse(f.necesitaPin());
        assertTrue(f.calibrar(sel('8'), "Diego", "x").contains("Ahora: persistencia"));
    }

    // ---------------------------------------------------------- QA-3612-04

    @Test
    public void unCodigoConformeNoSeReescribe() throws Exception {
        FlujoCalibracion f = flujo();
        f.calibrar(sel('8'), "Diego", "x");
        assertFalse(tarjeta(f, '8').casilla);
        String r = f.calibrar(sel('8'), "Diego", "otra vez");
        assertTrue(r, r.contains("ya está CONFORME"));
        assertEquals(1, sim.cuantas("#S,8,"));
    }

    // ------------------------------------------------------- P11-M2 y M3

    @Test
    public void escribirOtroCodigoAnulaLaPersistenciaYCadaUnoLlevaSuOscuro() throws Exception {
        decisiones = Decisiones.leer(decisionesDelApk() + REMEDIDA_B + "\n");
        calibrarYAceptar8();
        FlujoCalibracion f = flujo();
        assertTrue(f.calibrar(sel('5'), "Diego", "PA-14").contains("Ahora: persistencia"));
        assertTrue(f.persistencia().startsWith("Persistencia OK"));
        assertTrue(f.puedeAceptar());
        assertTrue(f.calibrar(sel('b'), "Diego", "PA-24").contains("Ahora: persistencia"));
        assertFalse(f.puedeAceptar());
        assertTrue(f.acta().motivoNoAceptable(true).contains("persistencia"));
        assertTrue(f.acta().dato("oscuro 5").contains("sesión 3"));
        assertTrue(f.acta().dato("oscuro b").contains("x = 571"));
        assertTrue(f.acta().dato("oscuro b").contains("sesión 2"));
        f.persistencia();
        assertTrue(f.aceptar().contains("ACEPTADA"));
    }

    // ----------------------------------------------------------- P12 §6.8

    @Test
    public void rechazarRestauraSiempre() throws Exception {
        FlujoCalibracion f = flujo();
        f.calibrar(sel('8'), "Diego", "x");
        String r = f.rechazar("prueba");
        assertTrue(r, r.contains("código 8 restaurado"));
        assertFalse(ajustado('8'));
        assertNull(f.acta());
        assertTrue(almacen.cerradas.get(0).contains("RECHAZADA"));
    }

    // -------------------------------------------- T-S03, T-S18, T-S19, T-S06, QA-3613-03

    @Test
    public void tS03EquipoDeOtraSerieNoSeToca() throws Exception {
        sim.pedir("#L,1234#", Tramas.Tipo.ADMIN, 2000);
        sim.pedir("#SN,SLV-003#", Tramas.Tipo.ADMIN, 5000);
        FlujoCalibracion f = flujo();
        assertEquals("SLV-003", ctx.serieGN);
        String r = f.calibrar(sel('8'), "Diego", "x");
        assertTrue(r, r.contains("no coincide con la campaña"));
        assertTrue(sim.recibidas.isEmpty());
    }

    @Test
    public void laTablaYLasDecisionesSonDeSlv002() throws Exception {
        sim.pedir("#L,1234#", Tramas.Tipo.ADMIN, 2000);
        sim.pedir("#SN,SLV-003#", Tramas.Tipo.ADMIN, 5000);
        campana = new Campana(catalogo, "SLV-003", MAC);
        FlujoCalibracion f = flujo();
        for (FlujoCalibracion.Tarjeta t : f.tarjetas(null)) {
            assertFalse(t.k + ": " + t.texto, t.casilla);
            assertTrue(t.texto.contains("QA-3613-03"));
        }
    }

    @Test
    public void tS18FirmwareSin362NoCalibra() throws Exception {
        sim.inyectar("#GC#", EquipoSimulado.Falla.ERR, 1);
        FlujoCalibracion f = flujo();
        assertFalse(ctx.es362);
        assertTrue(f.calibrar(sel('8'), "Diego", "x").contains("3.6.2"));
        assertTrue(sim.recibidas.isEmpty());
    }

    @Test
    public void tS19SinA5NoHaySrepYNoSeCalibra() throws Exception {
        rehacerCampana(true, false);
        FlujoCalibracion f = flujo();
        assertTrue(f.motivoPrevias().contains("A5"));
        assertFalse(f.puedeCalibrar(sel('8')));
        assertTrue(f.calibrar(sel('8'), "Diego", "x").startsWith("No se calibra"));
        assertEquals(0, sim.cuantas("#S,"));
    }

    @Test
    public void tS06SinOscuroEl8NoSeEscribe() throws Exception {
        rehacerCampana(false, true);
        FlujoCalibracion f = flujo();
        FlujoCalibracion.Tarjeta t = tarjeta(f, '8');
        assertFalse(t.casilla);
        assertTrue(t.texto, t.texto.contains("OSCURO"));
        assertTrue(f.calibrar(sel('8'), "Diego", "x").contains("no se escribe"));
        assertEquals(0, sim.cuantas("#S,"));
    }

    @Test
    public void tS11ESinReproducirRestauraYNoCuenta() throws Exception {
        sim.inyectar("#E,8,2000#", EquipoSimulado.Falla.ERR, 1);
        FlujoCalibracion f = flujo();
        String r = f.calibrar(sel('8'), "Diego", "x");
        assertTrue(r, r.contains("Escritura del código 8 fallida") && r.contains("Restaurado"));
        assertNull(f.acta().codigo('8'));
        assertFalse(ajustado('8'));
    }

    @Test
    public void tS15ElModoAdministradorCaducaYSeVuelveAEntrar() throws Exception {
        FlujoCalibracion f = flujo();
        operador.respuestas.put("Re-medida del código 8", 1);   // se para tras escribir
        f.calibrar(sel('8'), "Diego", "x");
        sim.avanzarMinutos(11);
        assertFalse(sim.admin);
        assertTrue(f.calibrar(sel(), "", "").contains("Ahora: persistencia"));
        f.persistencia();
        sim.avanzarMinutos(11);
        assertTrue(f.aceptar().contains("ACEPTADA"));   // #L antes de #SC
    }

    @Test
    public void tS23ColocacionSinPatronSeAnotaYSeRepite() throws Exception {
        operador.ausentes = 1;
        FlujoCalibracion f = flujo();
        assertTrue(f.calibrar(sel('8'), "Diego", "x").contains("Ahora: persistencia"));
        Acta.Codigo c = f.acta().codigo('8');
        assertEquals("NO_VALIDA", c.intentos.get(0).estado);
        assertEquals("CONFORME", c.intentos.get(1).estado);
    }

    // ------------------------------------------------ decisiones del APK

    @Test
    public void lasDecisionesDelApkSonDeSlv002YSinReglaDelB() throws Exception {
        Decisiones d = Decisiones.leer(decisionesDelApk());
        assertTrue(d.rechazadas().toString(), d.rechazadas().isEmpty());
        assertTrue(d.tomada("PA-24", "SLV-002"));
        assertTrue(d.tomada("PA-14", "SLV-002"));
        assertFalse(d.tomada("PA-24", "SLV-003"));
        // 3.6.15 (c836cae): REMEDIDA-b = RF-CAL-18, PA-24 con RF-CAL-15, P81 fuera del ajuste del 5.
        assertEquals("RF-CAL-18", d.valor("REMEDIDA-b", "SLV-002"));
        assertEquals(3, d.decision("PA-24", "SLV-002").alcance.size());
        assertEquals(java.util.Arrays.asList("P81"), d.decision("PA-14", "SLV-002").excluidos);
        assertTrue(d.decision("PA-24", "SLV-002").palabras.contains("mucho mejor que fabrica"));
        assertEquals(TablaCalibracion.Metodo.ANCLADA, TablaCalibracion.fila('b', d, "SLV-002").metodo);
        assertEquals(TablaCalibracion.Metodo.ANCLADA, TablaCalibracion.fila('5', d, "SLV-002").metodo);
        assertEquals("RF-CAL-18", TablaCalibracion.fila('b', d, "SLV-002").reglaRemedida);
        // P13-02: techo, no ventana. P39 a +11 % (mejor que lo aceptado) pasa; a +19 % o de otro signo, no.
        TablaCalibracion.Fila b = TablaCalibracion.fila('b', d, "SLV-002");
        assertTrue(b.dispensado("RF-CAL-14", "P39", 11.0));
        assertTrue(b.dispensado("RF-CAL-14", "P39", 17.9));
        assertFalse(b.dispensado("RF-CAL-14", "P39", 18.5));
        assertFalse(b.dispensado("RF-CAL-14", "P39", -11.0));
        assertTrue(b.dispensado("RF-CAL-14", "P49", -12.9));
        assertTrue(b.dispensado("RF-CAL-15", "I", 11.5));
        assertFalse(b.dispensado("RF-CAL-16", "I", 1.0));
    }

    // ------------------------------------------------------ 3.6.15: una sola sesion

    /** 8 -> b -> 5 en una sola pulsacion: cada uno escrito, re-medido, con persistencia y su acta ACEPTADA. */
    @Test
    public void calibrarTodoEnUnaSesion() throws Exception {
        FlujoCalibracion f = flujo();
        int apagados = sim.apagados;
        String r = f.calibrarTodo(sel('5', 'b', '8'), "Diego", "banco completo; PA-24 y PA-14");
        assertTrue(r, r.startsWith("Sesión completa: 8 ACEPTADO; b ACEPTADO; 5 ACEPTADO;"));
        assertEquals(3, almacen.cerradas.size());
        assertTrue(almacen.cerradas.get(0).contains("Código 8"));
        assertTrue(almacen.cerradas.get(1).contains("Código b"));
        assertTrue(almacen.cerradas.get(2).contains("Código 5"));
        // T-C41 una vez y una persistencia por codigo: el T-C41 de b y 5 aprovecha la persistencia anterior.
        assertEquals(apagados + 4, sim.apagados);
        assertEquals(3, sim.cuantas("#SC,"));
        assertTrue(almacen.cerradas.get(2).contains("patrones 5"));
        assertFalse(almacen.cerradas.get(2).contains("P81 (n ="));      // P81 fuera del ajuste del 5
        assertNull(f.acta());
    }

    /** Si un codigo no acaba ACEPTADO, la sesion se para ahi y lo dice; lo anterior queda aceptado. */
    @Test
    public void calibrarTodoSeParaEnElPrimerFallo() throws Exception {
        FlujoCalibracion f = flujo();
        operador.respuestas.put("Re-medida del código b", 1);          // "Parar aquí" en el b
        String r = f.calibrarTodo(sel('8', 'b', '5'), "Diego", "x");
        assertTrue(r, r.startsWith("Hecho: 8 ACEPTADO; . Código b: Parado"));
        assertEquals(1, almacen.cerradas.size());
        assertEquals(0, sim.cuantas("#S,5,"));
        // "Calibrar todo" otra vez: termina el b y sigue con el 5.
        String r2 = f.calibrarTodo(sel('5'), "Diego", "x");
        assertTrue(r2, r2.startsWith("Sesión completa: 5 ACEPTADO;"));
        assertEquals(3, almacen.cerradas.size());
    }

    @Test
    public void calibrarTodoCompruebaAntesDeEmpezar() throws Exception {
        sinHacer = "P66";
        rehacerCampana(true, true);
        FlujoCalibracion f = flujo();
        String r = f.calibrarTodo(sel('8', '5'), "Diego", "x");
        assertTrue(r, r.contains("El código 5 no se escribe") && r.contains("No se ha empezado"));
        assertTrue(sim.recibidas.isEmpty());
    }

    // ------------------------------------------------------- QA-3613-01, P13-05

    @Test
    public void unRechazoConLaRestauracionFallidaNoSeCierra() throws Exception {
        FlujoCalibracion f = flujo();
        f.calibrar(sel('8'), "Diego", "x");
        sim.inyectar("#F,8#", EquipoSimulado.Falla.OK_SIN_HACER, 1);
        String r = f.rechazar("prueba");
        assertTrue(r, r.startsWith("NO se rechaza"));
        assertNotNull(f.acta());
        assertFalse(f.acta().cerrada());
        assertNotNull(f.acta().codigo('8').restauracionFallida);
        assertTrue(almacen.cerradas.isEmpty());
        String r2 = f.rechazar("prueba");
        assertTrue(r2, r2.startsWith("Acta rechazada"));
        assertFalse(ajustado('8'));
    }

    @Test
    public void unRechazoConCorteYRestauracionFallidaNoSeCierra() throws Exception {
        sim.inyectar("#S,8,", EquipoSimulado.Falla.CORTE_DESPUES, 1);
        FlujoCalibracion f = flujo();
        try {
            f.calibrar(sel('8'), "Diego", "x");
            fail();
        } catch (IOException e) {
            // corte
        }
        sim.conectado = true;
        sim.inyectar("#F,8#", EquipoSimulado.Falla.OK_SIN_HACER, 1);
        FlujoCalibracion g = flujo();
        String r = g.rechazar("corte");
        assertTrue(r, r.startsWith("NO se rechaza"));
        assertEquals('8', g.acta().escribiendo());
        assertTrue(g.rechazar("corte").startsWith("Acta rechazada"));
        assertFalse(ajustado('8'));
    }

    // ------------------------------------------------------ QA-3614-09 y -03

    @Test
    public void unActaAbiertaConOtrasReglasNoSeAcepta() throws Exception {
        FlujoCalibracion f = flujo();
        f.calibrar(sel('8'), "Diego", "x");
        f.acta().tabla = "RF-CAL-37 ... APK 3.6.13";
        f.persistencia();
        assertFalse(f.puedeAceptar());
        assertTrue(f.motivoNoAceptar(), f.motivoNoAceptar().contains("otras reglas"));
        assertTrue(f.aceptar().contains("No aceptable"));
        assertEquals(0, sim.cuantas("#SC,"));
    }

    @Test
    public void unaSerieAnuladaEnElBancoImpideAceptar() throws Exception {
        FlujoCalibracion f = flujo();
        f.calibrar(sel('8'), "Diego", "x");
        f.persistencia();
        assertTrue(f.puedeAceptar());
        String ids = f.acta().dato("series 8");
        assertNotNull(ids);
        Campana.Serie s = campana.serie(ids.split(" ")[0]);
        campana.anular(s, "papel equivocado", "t");
        assertFalse(f.puedeAceptar());
        assertTrue(f.motivoNoAceptar(), f.motivoNoAceptar().contains("ANULÓ"));
    }

    @Test
    public void rehacerConActaEnCursoSeBloqueaYConActaAceptadaSeAvisa() throws Exception {
        FlujoCalibracion f = flujo();
        f.calibrar(sel('8'), "Diego", "x");
        int p43 = -1;
        for (BancoCola.Paso p : cola.pasos) {
            if ("P43".equals(p.patron)) {
                p43 = p.orden;
            }
        }
        assertTrue(FlujoCalibracion.bloqueoRehacer(almacen, cola, p43).startsWith("BLOQUEO"));
        f.persistencia();
        f.aceptar();
        assertTrue(FlujoCalibracion.bloqueoRehacer(almacen, cola, p43).startsWith("AVISO"));
    }

    // --------------------------------------------------------- renombrar la serie

    /** SERIE-2 de Diego: SLV-002 -> SLV-002-2026, sin perder campana, decisiones ni heredados. */
    @Test
    public void renombrarYCalibrarConLaSerieNueva() throws Exception {
        String t = FlujoCalibracion.renombrarSerie(sim, campana, "1234", "SLV-002-2026", "SLV-002-2026", "Diego",
                "2026-09-20T09:00:00-0500");
        assertTrue(t, t.startsWith("Serie cambiada: SLV-002 -> SLV-002-2026"));
        assertEquals("SLV-002-2026", sim.serie);
        assertTrue(campana.esSerie("SLV-002") && campana.esSerie("SLV-002-2026"));
        FlujoCalibracion f = flujo();
        assertEquals("SLV-002-2026", ctx.serieGN);
        assertNull(f.motivoPrevias());
        assertTrue(tarjeta(f, 'b').casilla);                  // decisiones de SLV-002 por el alias
        String r = f.calibrarTodo(sel('8'), "Diego", "tras renombrar");
        assertTrue(r, r.startsWith("Sesión completa: 8 ACEPTADO;"));
        assertTrue(almacen.cerradas.get(0).contains("Equipo: SLV-002-2026 (antes SLV-002)"));
        assertTrue(almacen.cerradas.get(0).contains("heredados (T-C41): OK"));
    }

    @Test
    public void renombrarValidaLaSerie() throws Exception {
        assertTrue(FlujoCalibracion.renombrarSerie(sim, campana, "1234", "SLV-002-20266", "SLV-002-20266", "Diego", "t")
                .contains("más de 12"));
        assertTrue(FlujoCalibracion.renombrarSerie(sim, campana, "1234", "SLV-002-2026", "SLV-002-2025", "Diego", "t")
                .contains("no coinciden"));
        assertEquals("SLV-002", sim.serie);
        assertEquals(12, "SLV-002-2026".length());
    }

    /** Otra MAC que se llame SLV-002 no recibe la tabla ni las decisiones de SLV-002. */
    @Test
    public void otraMacConLaSerieAntiguaNoHeredaNada() throws Exception {
        campana = new Campana(catalogo, "SLV-002", "00:21:13:AA:BB:CC");
        ctx = FlujoCalibracion.identificar(sim, "SLV-002", "00:21:13:AA:BB:CC", apto, "6/6", "x");
        FlujoCalibracion f = new FlujoCalibracion(sim, operador, almacen, cola, campana, catalogo, decisiones, ctx, reloj);
        for (FlujoCalibracion.Tarjeta t : f.tarjetas(null)) {
            assertFalse(t.k + ": " + t.texto, t.casilla);
        }
    }
}
