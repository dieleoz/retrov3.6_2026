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
import java.util.Collections;
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
 * "Calibrar este equipo" de extremo a extremo contra el equipo simulado (3.6.13; P11-M7, T-S00,
 * T-S07, T-S09, T-S10, T-S12, T-S16). Cada prueba usa SOLO lo que el operador dispara: los botones
 * de FlujoCalibracion (calibrar, leerBateria, persistencia, aceptar, rechazar), las respuestas a los
 * dialogos y, desde Avanzado, "Restaurar (#F)". El equipo simulado hace de SLV-002 despues del acta
 * de las 12:23 (1 y 2 escritos, CAL,0003).
 */
public class FlujoCalibracionTest {

    private static final String MAC = "00:21:13:05:19:3B";
    private static final String HOY = "2026-09-20";
    private static final double PENDIENTE_8 = 0.21;
    private static final double PENDIENTE_B = 0.36;
    private static final double PENDIENTE_5 = 0.327;
    /** Donde lee P49 frente a la recta del b (1 = sobre la recta; 0,8 = la del -10 % de QA-3612-06). */
    private double factorP49 = 1.0;

    // ------------------------------------------------------------ andamiaje

    /** La app matada a mitad (el proceso muere; lo que hay en disco queda). */
    static final class AppMatada extends RuntimeException {
        AppMatada() {
            super("app matada (simulado)");
        }
    }

    /** El acta "en disco": un diario en memoria que sobrevive a matar la app. */
    static final class Almacen implements FlujoCalibracion.AlmacenActa {
        StringWriter diario;
        Acta vivo;
        final List<String> cerradas = new ArrayList<>();

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
            cerradas.add(diario.toString() + "\n" + a.texto());
            diario = null;
            vivo = null;
        }

        /** Tras matar la app: la memoria se pierde, el disco no. */
        Almacen reabrir() {
            Almacen n = new Almacen();
            n.diario = diario;
            n.cerradas.addAll(cerradas);
            return n;
        }
    }

    /** El operador: coloca el patron que se le pide (con un factor de error) y pulsa OK. */
    static final class Operador implements FlujoCalibracion.Operador {
        final EquipoSimulado sim;
        final Map<String, Double> xPatron;
        double factor = 1.0;
        /** Colocaciones que el operador hace sin el patron debajo (se queda en el oscuro). */
        int ausentes = 0;
        int respuestaRepetir = 0;
        String matarEn;
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
            Matcher m = COLOQUE.matcher(mensaje);
            if (m.find() && xPatron.containsKey(m.group(1))) {
                if (ausentes > 0) {
                    ausentes--;
                    sim.x = 566;
                } else {
                    sim.x = xPatron.get(m.group(1)) * factor;
                }
            }
            if (titulo.contains("no entró")) {
                return respuestaRepetir;
            }
            return 0;
        }

        @Override
        public void progreso(String texto) {
        }

        @Override
        public boolean apagarYEncender() {
            sim.apagarYEncender();
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

    @Before
    public void preparar() throws Exception {
        try (InputStreamReader r = new InputStreamReader(
                new FileInputStream("src/main/assets/patrones_certificados_P1-P132.csv"), StandardCharsets.UTF_8)) {
            catalogo = Patron.leer(r);
        }
        cola = BancoCola.cargar(Files.readAllBytes(new File("src/main/assets/cola_banco_P1-P132.csv").toPath()));
        sim = EquipoSimulado.slv002();
        campana = new Campana(catalogo, "SLV-002", MAC);
        campana.escribirEn(new StringWriter());
        almacen = new Almacen();
        operador = new Operador(sim, xPatron);
        ctx = new FlujoCalibracion.Contexto();
        ctx.conectado = true;
        ctx.es362 = true;
        ctx.firmware = "V3.6 2026-09-19 (3.6.2) CAL mascara 0003";
        ctx.apto = Boolean.TRUE;
        ctx.resumenPruebas = "6/6";
        ctx.serieGN = "SLV-002";
        ctx.nombreBT = "SLV-002";
        ctx.mac = MAC;
        ctx.app = "RTV 3.6.13 (3613)";
        decisiones = Decisiones.ninguna();
        // El banco: OSCURO de la sesion 1 en 565 y de la 2 en 571; A5 del inicio con ~1,6 % entre colocaciones;
        // los patrones de AJUSTE y RE-MEDIDA del 8 (sesion 1) y del b (sesion 2) sobre rectas por el oscuro.
        medirBanco(true, true);
    }

    /** Otra campana, con o sin OSCURO y A5 (T-S06, T-S19). */
    private void rehacerCampana(boolean oscuro, boolean a5) throws IOException {
        campana = new Campana(catalogo, "SLV-002", MAC);
        campana.escribirEn(new StringWriter());
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
                double x0 = "b".equals(p.codigo) ? 571 : 565;
                double a = "8".equals(p.codigo) ? PENDIENTE_8 : "b".equals(p.codigo) ? PENDIENTE_B : PENDIENTE_5;
                double x = x0 + ("P49".equals(p.patron) ? factorP49 : 1.0) * p.certificado / a;
                xPatron.put(p.patron, x);
                serie(p, p.patron, new double[]{x, x, x, x, x}, true, "OK");
            }
        }
    }

    private FlujoCalibracion flujo() throws IOException {
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

    // --------------------------------------------------------------- T-S07

    @Test
    public void caminoFelizDel8() throws Exception {
        FlujoCalibracion f = flujo();
        assertNull(f.motivoPrevias());
        assertTrue(tarjeta(f, '8').casilla);
        assertFalse(f.puedeAceptar());
        String r = f.calibrar(sel('8'), "Diego", "banco completo");
        assertTrue(r, r.contains("Ahora: persistencia"));
        Acta a = f.acta();
        assertTrue(a.codigo('8').conforme());
        assertEquals(1, a.codigo('8').intentos.size());
        assertTrue(sim.curvas.get('8').igualFloat32(a.codigo('8').leida));
        // T-C41 al empezar, con el acta de las 12:23.
        assertTrue(a.dato("heredados (T-C41)"), a.dato("heredados (T-C41)").startsWith("OK"));
        // QA-3612-01: tras la persistencia, "Aceptar" se habilita; la verificacion final va dentro.
        assertFalse(f.puedeAceptar());
        assertTrue(f.puedePersistencia());
        String p = f.persistencia();
        assertTrue(p, p.startsWith("Persistencia OK"));
        assertTrue(f.puedeAceptar());
        String ac = f.aceptar();
        assertTrue(ac, ac.contains("ACEPTADA"));
        assertEquals(HOY, sim.fecha);
        assertEquals(1, sim.cuantas("#SC,"));
        assertEquals(1, sim.cuantas("#S,8,"));
        assertEquals(0, sim.cuantas("#S,1,") + sim.cuantas("#S,2,"));
        String acta = almacen.cerradas.get(0);

        assertTrue(acta.contains("oscuro 8: Oscuro (x = 565"));
        assertTrue(acta.contains("sesión 1"));
        assertTrue(acta, acta.contains("código 5: código 5 de fábrica, fuera de tolerancia, sin decisión"));
        assertTrue(acta.contains("Estado: ACEPTADA"));
        assertTrue(sim.recibidas.contains("9"));
    }

    @Test
    public void conPruebasNoAptoNoSeCalibra() throws Exception {
        ctx.apto = Boolean.FALSE;
        FlujoCalibracion f = flujo();
        assertFalse(f.puedeCalibrar(sel('8')));
        String r = f.calibrar(sel('8'), "Diego", "x");
        assertTrue(r, r.contains("NO APTO"));
        assertTrue(sim.recibidas.isEmpty());
    }

    // ------------------------------------------------------------ P11-M5

    @Test
    public void elBSinPa24NoSeEscribe() throws Exception {
        FlujoCalibracion f = flujo();
        FlujoCalibracion.Tarjeta b = tarjeta(f, 'b');
        assertFalse(b.casilla);
        assertTrue(b.texto.contains("PA-24 sin decidir"));
        assertTrue(b.texto.contains("QA-3612-06"));       // la contradiccion de P49 queda a la vista
        String r = f.calibrar(sel('b'), "Operador", "marco la casilla");
        assertTrue(r, r.contains("no se escribe"));
        assertEquals(0, sim.cuantas("#S,"));
    }

    // ---------------------------------------------------------- D-20 / M1

    @Test
    public void dosReMedidasNoConformesRestauranYNoSeAcepta() throws Exception {
        operador.factor = 0.85;             // el patron lee un 15 % menos que en el banco
        FlujoCalibracion f = flujo();
        String r = f.calibrar(sel('8'), "Diego", "x");
        assertTrue(r, r.contains("restaurado"));
        Acta a = f.acta();
        assertEquals(2, a.codigo('8').validos());
        assertNotNull(a.codigo('8').restaurado);
        assertTrue(sim.curvas.get('8').igualFloat32(Fabrica.ecuacion('8')));
        assertEquals(1, sim.cuantas("#F,8#"));
        assertNotNull(a.motivoNoEscribir('8'));             // no se reescribe para reiniciar D-20
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
        assertTrue(a.motivoNoAceptable(false).contains("RESTAURACIÓN NO VERIFICADA"));
        assertFalse(f.puedeAceptar());
        assertTrue(f.puedeCalibrar(sel()));                  // "Continuar": reintenta la restauracion
        String r2 = f.calibrar(sel(), "", "");
        assertTrue(r2, r2.contains("restaurado"));
        assertNotNull(f.acta().codigo('8').restaurado);
        assertTrue(sim.curvas.get('8').igualFloat32(Fabrica.ecuacion('8')));
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
        assertFalse(f.puedeAceptar());
        sim.conectado = true;                                // reconectar
        FlujoCalibracion g = flujo();
        assertTrue(g.pendientes() > 0);
        String r = g.calibrar(sel(), "", "");
        assertTrue(r, r.contains("Ahora: persistencia"));
        Acta a = g.acta();
        assertEquals(0, a.escribiendo());
        assertTrue(a.codigo('8').conforme());
        assertTrue(a.codigo('8').metodo.contains("anclada"));     // P11-M3: el corte no pierde el metodo
        assertTrue(a.codigo('8').oscuro.contains("x = 565"));
        assertTrue(a.codigo('8').conformidad.contains("Diego"));
        assertEquals(1, sim.cuantas("#S,8,"));
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
        assertTrue(sim.curvas.get('8').igualFloat32(Fabrica.ecuacion('8')));
        sim.conectado = true;
        FlujoCalibracion g = flujo();
        String r = g.calibrar(sel(), "", "");
        assertTrue(operador.titulos.toString(), operador.titulos.contains("El #S del código 8 no entró"));
        assertTrue(r, r.contains("Ahora: persistencia"));
        assertTrue(g.acta().codigo('8').conforme());
        assertEquals(2, sim.cuantas("#S,8,"));
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
        assertTrue(g.puedeCalibrar(sel()));
        String r = g.calibrar(sel(), "", "");
        assertTrue(r, r.contains("Ahora: persistencia"));
        assertTrue(g.acta().codigo('8').conforme());
        assertEquals(1, sim.cuantas("#S,8,"));
    }

    // ---------------------------------------------------- P10-C5, P11 caso 3

    @Test
    public void fDesdeAvanzadoConElActaAbiertaLaInvalida() throws Exception {
        FlujoCalibracion f = flujo();
        f.calibrar(sel('8'), "Diego", "x");
        String t = FlujoCalibracion.fabricaDesdeAvanzado(sim, almacen, "1234", false, '1');
        assertTrue(t, t.contains("INVALIDADA"));
        FlujoCalibracion g = flujo();                      // la pantalla vuelve
        assertTrue(g.acta().invalidada());
        assertNotNull(g.motivoPrevias());
        assertFalse(g.puedeAceptar());
        assertFalse(g.puedePersistencia());
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
        sim.admin = true;
        sim.curvas.put('2', Fabrica.ecuacion('2'));
        sim.mascara &= ~(1 << Fabrica.indice('2'));
        String ac = f.aceptar();
        assertTrue(ac, ac.contains("Verificación final FALLA"));
        assertEquals("2026-09-19", sim.fecha);               // no se graba una #SC nueva
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
        assertNotNull(f.acta());
        assertFalse(f.acta().cerrada());
        assertTrue(f.puedeAceptar());
        String r2 = f.aceptar();
        assertTrue(r2, r2.contains("ACEPTADA"));
    }

    // ---------------------------------------------------------- QA-3612-05

    @Test
    public void bateriaACeroBloqueaYTrasCambiarlaSeSigue() throws Exception {
        sim.bateriaN = 0;
        FlujoCalibracion f = flujo();
        String r = f.calibrar(sel('8'), "Diego", "x");
        assertTrue(r, r.contains("No se escribe"));
        assertEquals(0, sim.cuantas("#S,"));
        assertFalse(f.puedeCalibrar(sel('8')));
        sim.bateriaN = 120;                                  // bateria cambiada
        f.leerBateria();
        assertTrue(f.puedeCalibrar(sel('8')));
        String r2 = f.calibrar(sel('8'), "Diego", "x");
        assertTrue(r2, r2.contains("Ahora: persistencia"));
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
        decisiones = Decisiones.leer("PA-24,SI,2026-09-20,Diego Zuniga,05_Documentacion/DECISION-PA-24.md");
        FlujoCalibracion f = flujo();
        assertTrue(tarjeta(f, 'b').casilla);
        f.calibrar(sel('8'), "Diego", "x");
        assertTrue(f.persistencia().startsWith("Persistencia OK"));
        assertTrue(f.puedeAceptar());
        String r = f.calibrar(sel('b'), "Diego", "PA-24 registrada");
        assertTrue(r, r.contains("Ahora: persistencia"));
        assertFalse(f.puedeAceptar());                      // la persistencia del 8 no vale para el b
        assertTrue(f.acta().motivoNoAceptable(true).contains("persistencia"));
        assertTrue(f.acta().dato("oscuro 8").contains("x = 565"));
        assertTrue(f.acta().dato("oscuro b").contains("x = 571"));
        assertTrue(f.acta().dato("oscuro b").contains("sesión 2"));
        f.persistencia();
        assertTrue(f.aceptar().contains("ACEPTADA"));
    }

    // ----------------------------------------------------------- QA-3612-17

    @Test
    public void rechazarRestauraLoEscrito() throws Exception {
        FlujoCalibracion f = flujo();
        f.calibrar(sel('8'), "Diego", "x");
        String r = f.rechazar("prueba", true);
        assertTrue(r, r.contains("código 8 restaurado"));
        assertTrue(sim.curvas.get('8').igualFloat32(Fabrica.ecuacion('8')));
        assertNull(f.acta());
        assertTrue(almacen.cerradas.get(0).contains("RECHAZADA"));
        assertEquals(Collections.emptyList(), new ArrayList<>(Collections.<String>emptyList()));
    }

    // ------------------------------------------------ T-S03, T-S18, T-S19, T-S06

    @Test
    public void tS03EquipoDeOtraSerieNoSeToca() throws Exception {
        ctx.serieGN = "SLV-003";
        FlujoCalibracion f = flujo();
        String r = f.calibrar(sel('8'), "Diego", "x");
        assertTrue(r, r.contains("no coincide con la campaña"));
        assertTrue(sim.recibidas.isEmpty());
    }

    @Test
    public void tS18FirmwareSin362NoCalibra() throws Exception {
        ctx.es362 = false;
        ctx.firmware = "V3.6 2026-09-18 (3.6.1)";
        FlujoCalibracion f = flujo();
        assertTrue(f.calibrar(sel('8'), "Diego", "x").contains("3.6.2"));
        assertTrue(sim.recibidas.isEmpty());
    }

    @Test
    public void tS19SinA5NoHaySrepYNoSeCalibra() throws Exception {
        rehacerCampana(true, false);
        FlujoCalibracion f = flujo();
        assertTrue(f.motivoPrevias().contains("A5"));
        assertFalse(f.puedeCalibrar(sel('8')));
        assertTrue(sim.recibidas.isEmpty() || f.calibrar(sel('8'), "Diego", "x").startsWith("No se calibra"));
        assertEquals(0, sim.cuantas("#S,"));
    }

    @Test
    public void tS06SinOscuroEl8NoSeEscribe() throws Exception {
        rehacerCampana(false, true);
        FlujoCalibracion f = flujo();
        FlujoCalibracion.Tarjeta t = tarjeta(f, '8');
        assertFalse(t.casilla);
        assertTrue(t.texto, t.texto.contains("OSCURO"));
        String r = f.calibrar(sel('8'), "Diego", "x");
        assertTrue(r, r.contains("no se escribe"));
        assertEquals(0, sim.cuantas("#S,"));
    }

    // ------------------------------------------------------ T-S09, T-S11, T-S23

    @Test
    public void tS09CorteEnLaReMedidaQuedaAnotadoYSeRepite() throws Exception {
        sim.inyectar("8", EquipoSimulado.Falla.CORTE_ANTES, 1);   // el primer disparo del codigo 8
        FlujoCalibracion f = flujo();
        try {
            f.calibrar(sel('8'), "Diego", "x");
            fail();
        } catch (IOException e) {
            // corte en la re-medida
        }
        Acta a = f.acta();
        assertEquals("INTERRUMPIDA", a.codigo('8').intentos.get(0).estado);
        assertEquals(0, a.codigo('8').validos());
        sim.conectado = true;
        String r = flujo().calibrar(sel(), "", "");
        assertTrue(r, r.contains("Ahora: persistencia"));
        assertEquals(1, sim.cuantas("#S,8,"));
    }

    @Test
    public void tS11ESinReproducirRestauraYNoCuenta() throws Exception {
        sim.inyectar("#E,8,2000#", EquipoSimulado.Falla.ERR, 1);
        FlujoCalibracion f = flujo();
        String r = f.calibrar(sel('8'), "Diego", "x");
        assertTrue(r, r.contains("Escritura del código 8 fallida") && r.contains("Restaurado"));
        assertNull(f.acta().codigo('8'));
        assertTrue(sim.curvas.get('8').igualFloat32(Fabrica.ecuacion('8')));
        assertTrue(f.acta().texto().contains("Código 8 sin escribir"));
    }

    @Test
    public void tS23ColocacionSinPatronSeAnotaYSeRepite() throws Exception {
        operador.ausentes = 1;
        FlujoCalibracion f = flujo();
        String r = f.calibrar(sel('8'), "Diego", "x");
        assertTrue(r, r.contains("Ahora: persistencia"));
        Acta.Codigo c = f.acta().codigo('8');
        assertEquals("NO_VALIDA", c.intentos.get(0).estado);
        assertEquals("CONFORME", c.intentos.get(1).estado);
        assertTrue(operador.titulos.contains("Colocación no válida"));
    }

    // ---------------------------------------- decisiones de Diego (assets/decisiones.csv)

    private static Decisiones decisionesDelApk() throws IOException {
        return Decisiones.leer(new String(Files.readAllBytes(new File("src/main/assets/decisiones.csv").toPath()),
                StandardCharsets.UTF_8));
    }

    @Test
    public void lasDecisionesDelApkSonPa24YPa14DeDiego() throws Exception {
        Decisiones d = decisionesDelApk();
        assertTrue(d.tomada("PA-24"));
        assertTrue(d.tomada("PA-14"));
        assertTrue(d.rechazadas().toString(), d.rechazadas().isEmpty());
        assertTrue(d.decision("PA-24").documento.contains("DECISIONES-Diego-2026-09-19.md"));
        assertTrue(d.decision("PA-24").palabras.contains("mucho mejor que fabrica"));
        assertEquals(TablaCalibracion.Metodo.ANCLADA, TablaCalibracion.fila('b', d).metodo);
        assertEquals(TablaCalibracion.Metodo.ANCLADA, TablaCalibracion.fila('5', d).metodo);
    }

    /**
     * El b con PA-24 y P49 a mas del 10 % bajo el certificado (QA-3612-06): se escribe, la re-medida pasa por
     * RF-CAL-18 frente a la curva escrita y el acta deja el incumplimiento frente al certificado a la vista.
     */
    @Test
    public void elBConPa24SeEscribeYElIncumplimientoQuedaEnElActa() throws Exception {
        factorP49 = 0.8;
        rehacerCampana(true, true);
        decisiones = decisionesDelApk();
        FlujoCalibracion f = flujo();
        FlujoCalibracion.Tarjeta b = tarjeta(f, 'b');
        assertTrue(b.texto, b.casilla);
        assertTrue(b.texto.contains("Incumple"));
        String r = f.calibrar(sel('b'), "Diego", "PA-24");
        assertTrue(r, r.contains("Ahora: persistencia"));
        Acta.Codigo c = f.acta().codigo('b');
        assertTrue(c.conforme());
        String t = c.intentos.get(c.intentos.size() - 1).texto;
        assertTrue(t, t.contains("RF-CAL-18 frente a la curva escrita"));
        assertTrue(t, t.contains("INCUMPLE, dispensado por RF-CAL-14/15 (PA-24)"));
        assertTrue(c.conformidad.contains("Dispensa"));
        assertTrue(f.acta().dato("decisiones").contains("PA-24 decidida por Diego"));
        f.persistencia();
        String ac = f.aceptar();
        assertTrue(ac, ac.contains("ACEPTADA"));
        assertTrue(almacen.cerradas.get(0).contains("dispensado por RF-CAL-14/15 (PA-24)"));
    }

    @Test
    public void el5ConPa14SeAjustaConLaRectaAncladaYLaCoberturaDeRfApp42() throws Exception {
        decisiones = decisionesDelApk();
        FlujoCalibracion f = flujo();
        FlujoCalibracion.Plan p = f.plan('5', null);
        assertTrue(p.motivoNo, p.escribible());
        assertTrue(p.propuesta.metodo.contains("anclada"));
        assertTrue(p.patrones, p.patrones.contains("P17") && p.patrones.contains("P81"));
        assertTrue(tarjeta(f, '5').casilla);
        String r = f.calibrar(sel('5'), "Diego", "PA-14");
        assertTrue(r, r.contains("Ahora: persistencia"));
        assertTrue(f.acta().codigo('5').conforme());
        assertTrue(f.acta().dato("código 5").contains("PA-14"));
    }
}
