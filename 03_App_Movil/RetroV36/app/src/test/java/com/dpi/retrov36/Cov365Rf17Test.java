/*
 * Pruebas de las tres condiciones del arquitecto-iot sobre "APTO CON CONDICIONES" a Cov_3.6.4_calibrar
 * (encargo del 21-sep-2026, contra HEAD 978dd0e de rtv-1.0-cierre). SPEC que manda:
 * 05_Documentacion/SPEC-App-Calibracion-Coviandina.md §8, RF-COV-17 a 22 (de main).
 *
 * Andamiaje calcado del de Cov364Rf21Test/Cov363ArreglosTest: el ZIP real de las 18:11 (md5
 * 79b23590c3a6b1877d55b5c4818b93ce, HUELLAS.txt de 06_Calibracion/SLV-002/), EquipoSimulado.slv002() y
 * las clases de FlujoCalibracionTest (Almacen, Operador). De donde sale cada valor esperado (CLAUDE.md
 * del repositorio, §7):
 *  - condicion 1 (RF-COV-17/21, H-3): el texto literal del encargo ("ningún texto en corto lleva el
 *    número de código"; el nombre viene de Fabrica.nombreCorto, ya usado por el resto de la app corta).
 *  - condicion 3 (RF-COV-20): el texto literal del encargo ("sustituida por VERIF-5-10 (RF-COV-12)"; la
 *    fila REMEDIDA-b de decisiones.csv, ajena al codigo bajo prueba, es la fuente de "RF-CAL-18").
 * La condicion 2 (RF-COV-22, modularidad) no lleva prueba nueva: es un corte sin cambio de
 * comportamiento, verificado por la suite existente en verde y salida identica.
 */
package com.dpi.retrov36;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Cov365Rf17Test {

    private static final String MAC = "00:21:13:05:19:3B";
    private static final String ZIP_1811 = "../../../06_Calibracion/SLV-002/campanas/soporte_SLV-002_20260919_181120.zip";

    private static List<Patron> catalogo;

    @BeforeClass
    public static void una() throws Exception {
        try (InputStreamReader r = new InputStreamReader(
                new FileInputStream("src/main/assets/patrones_certificados_P1-P132.csv"), StandardCharsets.UTF_8)) {
            catalogo = Patron.leer(r);
        }
    }

    private static String decisionesDelApk() throws IOException {
        return new String(Files.readAllBytes(new File("src/main/assets/decisiones.csv").toPath()), StandardCharsets.UTF_8);
    }

    private static String asset(String n) throws IOException {
        return new String(Files.readAllBytes(new File("src/main/assets/" + n).toPath()), StandardCharsets.UTF_8);
    }

    private static BancoCola cola(BancoCola.Tipo t) throws IOException {
        return BancoCola.cargar(Files.readAllBytes(new File("src/main/assets/" + t.asset).toPath()));
    }

    private static String diarioDe(String zipPath) throws IOException {
        byte[] zip = Files.readAllBytes(new File(zipPath).toPath());
        return ImportadorCampana.diarioDeZip(zip);
    }

    private static Set<Character> sel(char... ks) {
        Set<Character> s = new HashSet<>();
        for (char k : ks) {
            s.add(k);
        }
        return s;
    }

    /** "hoy" 2026-09-21, distinto de la fecha de slv002() (19-sep). */
    private static FlujoCalibracion.Reloj reloj() {
        return new FlujoCalibracion.Reloj() {
            @Override
            public String ahoraIso() {
                return "2026-09-21T10:00:00-0500";
            }

            @Override
            public String hoy() {
                return "2026-09-21";
            }
        };
    }

    private static final class Escenario {
        Campana campana;
        BancoCola cola;
        Decisiones decisiones;
        EquipoSimulado sim;
        FlujoCalibracionTest.Almacen almacen;
        FlujoCalibracionTest.Operador operador;
        java.util.Map<String, Double> xPatron = new java.util.HashMap<>();
    }

    private static Escenario escenario() throws Exception {
        Escenario e = new Escenario();
        String diario = diarioDe(ZIP_1811);
        e.campana = new Campana(catalogo, "SLV-002", MAC);
        e.campana.escribirEn(new java.io.StringWriter());
        ImportadorCampana.importarDiario(e.campana, diario, catalogo, "zip 18:11");
        e.cola = cola(BancoCola.Tipo.de(e.campana.colaTipo()));
        BancoPrevio.aplicar(e.campana, e.cola, BancoPrevio.Grupos.leer(asset("grupos_patrones_equivalentes.csv")),
                Decisiones.leer(decisionesDelApk()), "SLV-002", "t");
        e.decisiones = Decisiones.leer(decisionesDelApk());
        e.xPatron.put("P43", e.campana.elegida("P43").media());
        e.xPatron.put("P49", e.campana.elegida("P49").media());
        e.sim = EquipoSimulado.slv002();
        e.almacen = new FlujoCalibracionTest.Almacen();
        e.operador = new FlujoCalibracionTest.Operador(e.sim, e.xPatron);
        return e;
    }

    private static FlujoCalibracion flujo(Escenario e, boolean corto) throws Exception {
        FlujoCalibracion.Contexto ctx = FlujoCalibracion.identificar(e.sim, "SLV-002", MAC, Boolean.TRUE, "6/6",
                "RTV Calibra, prueba");
        FlujoCalibracion f = new FlujoCalibracion(e.sim, e.operador, e.almacen, e.cola, e.campana, catalogo,
                e.decisiones, ctx, reloj(), corto);
        f.pin("1234");
        return f;
    }

    // ============================================================ condicion 1 (RF-COV-17/21, H-3)

    /**
     * ALTO, condicion 1a: el resumen de calibrarAutomatico, con un ERR inyectado en #S del 8, no lleva
     * "código 8" — el arquitecto senalo FlujoCalibracion.java:1352/1356 (escribir(), la escritura fallida
     * y su restauracion) como el texto que se filtraba sin pasar por Fabrica.elCodigo/nombreCorto.
     *
     * EN ROJO contra 978dd0e: escribir() decia literalmente "Escritura del código " + k + " fallida...",
     * sin mirar `corto`; el resumen de calibrarAutomatico (que incrusta ese texto vía CalibracionAutomatica
     * .anotarNoCalibrado) lo mostraba tal cual en la app de calibrar.
     */
    @Test
    public void a_resumenDeCalibrarAutomaticoConErrEnSNoMuestraElNumeroDeCodigoEnCorto() throws Exception {
        Escenario e = escenario();
        e.sim.inyectar("#S,8,", EquipoSimulado.Falla.ERR, 1);
        FlujoCalibracion f = flujo(e, true);
        String resumen = f.calibrarAutomatico("Prueba condición 1a");
        assertTrue(resumen, resumen.contains("fallida"));
        assertFalse("EN ROJO contra 978dd0e, el resumen en corto decía \"código 8\": " + resumen,
                resumen.contains("código 8"));
        assertTrue("el nombre corto SÍ aparece (RF-COV-17): " + resumen, resumen.contains(Fabrica.nombreCorto('8')));

        // Regresión: en la app de campo, "código 8" sigue apareciendo tal cual (no se le quitó nada).
        Escenario e2 = escenario();
        e2.sim.inyectar("#S,8,", EquipoSimulado.Falla.ERR, 1);
        FlujoCalibracion f2 = flujo(e2, false);
        String r2 = f2.calibrar(sel('8'), "Diego", "x");
        assertTrue("la app de campo sigue diciendo \"código 8\": " + r2, r2.contains("Escritura del código 8 fallida"));
    }

    /**
     * ALTO, condicion 1b: rechazar() en corto, tras un #S sin resolver (corte DESPUES), no lleva
     * "código 8" — FlujoCalibracion.java:1645/1647 (antes del corte de RF-COV-22, la rama "(#S sin
     * resolver)" de rechazar()).
     *
     * EN ROJO contra 978dd0e: rechazar() decia literalmente "código " + k + " (#S sin resolver)...".
     */
    @Test
    public void b1_rechazarEnCortoNoMuestraElNumeroDeCodigoTrasUnSSinResolver() throws Exception {
        Escenario e = escenario();
        e.sim.inyectar("#S,8,", EquipoSimulado.Falla.CORTE_DESPUES, 1);
        FlujoCalibracion f = flujo(e, true);
        try {
            f.calibrar(sel('8'), "Prueba", "Nota");
            org.junit.Assert.fail("el corte tiene que propagar la IOException, como en FlujoCalibracionTest");
        } catch (IOException ex) {
            // corte tras escribir: el #S entró pero el enlace cayó antes de confirmarlo
        }
        e.sim.conectado = true;
        FlujoCalibracion g = flujo(e, true);
        String r = g.rechazar("Prueba condición 1b");
        assertTrue(r, r.contains("(#S sin resolver) restaurado"));
        assertFalse("EN ROJO contra 978dd0e, el texto de rechazar() en corto decía \"código 8\": " + r,
                r.contains("código 8"));
        assertTrue("el nombre corto SÍ aparece: " + r, r.contains(Fabrica.nombreCorto('8')));

        // Regresión: en la app de campo, "código 8" sigue apareciendo (mismo escenario, corto = false).
        Escenario e2 = escenario();
        e2.sim.inyectar("#S,8,", EquipoSimulado.Falla.CORTE_DESPUES, 1);
        FlujoCalibracion f2 = flujo(e2, false);
        try {
            f2.calibrar(sel('8'), "Diego", "x");
            org.junit.Assert.fail();
        } catch (IOException ex) {
            // corte
        }
        e2.sim.conectado = true;
        FlujoCalibracion g2 = flujo(e2, false);
        String r2 = g2.rechazar("Prueba condición 1b campo");
        assertTrue("la app de campo sigue diciendo \"código 8\": " + r2, r2.contains("código 8 (#S sin resolver) restaurado"));
    }

    /**
     * ALTO, condicion 1b: rechazar() en corto, sobre un código YA ESCRITO (conforme pero sin persistir ni
     * aceptar) no lleva "código b" — FlujoCalibracion.java:1662 (el bucle sobre acta.codigos()), citado
     * literalmente por el encargo ("Acta rechazada. código 8 restaurado; …").
     *
     * EN ROJO contra 978dd0e: rechazar() decia literalmente "código " + c.k + (restaurado ? ...).
     */
    @Test
    public void b2_rechazarEnCortoNoMuestraElNumeroDeCodigoBYaEscrito() throws Exception {
        Escenario e = escenario();
        FlujoCalibracion f = flujo(e, true);
        String r8 = f.calibrar(sel('8'), "Prueba", "Nota 8");
        assertTrue(r8, r8.contains("Ahora: persistencia"));
        assertTrue(f.persistencia().startsWith("Persistencia OK"));
        assertTrue(f.aceptar().contains("ACEPTADA"));

        FlujoCalibracion g = flujo(e, true);
        String rb = g.calibrar(sel('b'), "Prueba", "Nota b");
        assertTrue(rb, rb.contains("Ahora: persistencia"));
        String rr = g.rechazar("Prueba condición 1b, código b escrito");
        assertTrue(rr, rr.contains("restaurado"));
        assertFalse("EN ROJO contra 978dd0e, el texto de rechazar() en corto decía \"código b\": " + rr,
                rr.contains("código b"));
        assertTrue("el nombre corto SÍ aparece: " + rr, rr.contains(Fabrica.nombreCorto('b')));
    }

    // ============================================================ condicion 3 (RF-COV-20)

    /**
     * MEDIO, condicion 3: el DATO,decisiones del acta de la b en corto no afirma "se juzga con RF-CAL-18"
     * (FlujoCalibracion.java, la línea que completarDatos()/abrirActa() anotan con
     * Decisiones.texto(equipo, corto)): va marcada "sustituida por VERIF-5-10 (RF-COV-12)".
     *
     * EN ROJO contra 978dd0e: Decisiones.texto(equipo) no miraba corto y siempre imprimía el texto
     * completo de REMEDIDA-b, que cita literalmente "se juzga con RF-CAL-18".
     */
    @Test
    public void c_actaDeLaBEnCortoNoAfirmaQueSeJuzgaConRfCal18() throws Exception {
        Escenario e = escenario();
        FlujoCalibracion f = flujo(e, true);
        String r8 = f.calibrar(sel('8'), "Prueba", "Nota 8");
        assertTrue(r8, r8.contains("Ahora: persistencia"));
        assertTrue(f.persistencia().startsWith("Persistencia OK"));
        assertTrue(f.aceptar().contains("ACEPTADA"));

        FlujoCalibracion g = flujo(e, true);
        g.calibrar(sel('b'), "Prueba", "Nota b");
        String decisionesActa = g.acta().dato("decisiones");
        assertNotNull(decisionesActa);
        assertFalse("EN ROJO contra 978dd0e, el acta de la b en corto decía \"se juzga con RF-CAL-18\": "
                + decisionesActa, decisionesActa.contains("se juzga con RF-CAL-18"));
        assertTrue("va marcada sustituida (RF-COV-20): " + decisionesActa,
                decisionesActa.contains("sustituida por VERIF-5-10 (RF-COV-12)"));

        // Regresión: en la app de campo, el acta SÍ sigue afirmando "se juzga con RF-CAL-18" (no se le
        // quitó nada a la app de campo: sigue usando RF-CAL-18/REMEDIDA-b para la b).
        Escenario e2 = escenario();
        FlujoCalibracion f2 = flujo(e2, false);
        String r82 = f2.calibrar(sel('8'), "Diego", "Nota 8 campo");
        assertTrue(r82, r82.contains("Ahora: persistencia"));
        assertTrue(f2.persistencia().startsWith("Persistencia OK"));
        assertTrue(f2.aceptar().contains("ACEPTADA"));
        FlujoCalibracion g2 = flujo(e2, false);
        g2.calibrar(sel('b'), "Diego", "Nota b campo");
        String decisionesActaCampo = g2.acta().dato("decisiones");
        assertTrue("la app de campo sigue citando RF-CAL-18: " + decisionesActaCampo,
                decisionesActaCampo != null && decisionesActaCampo.contains("se juzga con RF-CAL-18"));
    }

    // ============================================================ Bajo (RF-COV-18)

    /**
     * BAJO: cuando #GC# falla antes de #SC (aceptar()), el DATO del acta lleva la frase literal "no se
     * pudo leer la fecha del equipo; reintente", no solo el describir() crudo de la trama.
     *
     * EN ROJO contra 978dd0e: FechaCalibracion.grabar anotaba solo g0.describir() en el DATO del acta; la
     * frase literal solo vivía en el texto devuelto al operador (que no queda en el acta si nadie lo
     * transcribe a mano).
     */
    @Test
    public void bajo_elActaLlevaLaFraseLiteralCuandoNoSePudoLeerLaFechaDelEquipo() throws Exception {
        Escenario e = escenario();
        FlujoCalibracion f = flujo(e, false);
        String r = f.calibrar(sel('8'), "Diego", "x");
        assertTrue(r, r.contains("Ahora: persistencia"));
        assertTrue(f.persistencia().startsWith("Persistencia OK"));

        e.sim.inyectar("#GC#", EquipoSimulado.Falla.ERR, 1);
        String ac = f.aceptar();
        assertTrue(ac, ac.contains("no se pudo leer la fecha del equipo"));
        assertNotNull("el acta sigue en curso: no se aceptó", f.acta());

        String dato = f.acta().dato("#GC# antes de #SC");
        assertNotNull(dato);
        assertTrue("EN ROJO contra 978dd0e, el DATO del acta solo llevaba el describir() crudo, sin la frase "
                + "literal: " + dato, dato.contains("no se pudo leer la fecha del equipo; reintente"));
    }
}
