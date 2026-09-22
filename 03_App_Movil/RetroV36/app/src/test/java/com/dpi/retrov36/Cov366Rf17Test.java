/*
 * Tarea A4b (encargo del 21-sep-2026, contra HEAD 316a6bc de rtv-1.0-cierre). SPEC que manda:
 * 05_Documentacion/SPEC-App-Calibracion-Coviandina.md §8, RF-COV-17 (de main; no existe en esta rama) y
 * su cierre en RF-COV-21: "Ningún texto que vea el operador lleva el número de código (RF-COV-17)".
 *
 * Cov365Rf17Test ya cubrió tres sitios (escribir() con #S en ERR, rechazar() con #S sin resolver y
 * restauración, Decisiones.texto de la b). Este fichero cubre CUATRO sitios más, encontrados con grep
 * propio sobre FlujoCalibracion.java, que un agente anterior (solo lectura) había señalado sin abrir el
 * fichero para comprobar si de verdad son alcanzables desde el camino corto (RTV Calibra):
 *
 *  - resolverCorte() (FlujoCalibracion.java:1164-1210): SÍ alcanzable en corto (calibrar() -> Calibracion
 *    Manual.escribirYVerificar() -> resolverCorte(), el mismo calibrar() que usa CalibracionAutomatica).
 *    El diálogo "El #S del código k no entró" (java:1190-1191) no miraba `corto`.
 *  - escribir() (java:1213-1235): dos "return" sin mirar `corto` (java:1230, java:1234), aunque el mismo
 *    método ya usa Fabrica.elCodigoCap/elCodigo en otras líneas (java:1219, 1222, 1249).
 *  - persistencia() (java:1433-1465) y aceptar() (java:1479-1529): el StringBuilder `t` que arma el
 *    resumen de cada método usa "código " + c.k sin mirar `corto` (java:1452,1455,1499,1501); ese `t` es
 *    justo el texto que CalibracionAutomatica.persistirYAceptarAuto devuelve tal cual en el resumen de
 *    "Calibrar" cuando falla algo DESPUÉS de escribir (persistencia o verificación final), como aquí.
 *
 * Lo que el agente anterior señaló y que ESTE agente comprobó que NO hace falta tocar (no alcanzable
 * desde el camino corto, no hay botón que llegue): calibrarTodoInterno (java:1786-1827) y
 * persistirConfirmarYAceptar (java:1853-1875) solo los llama "Calibrar todo"/"Continuar" de
 * CalibrarActivity, botones que solo existen cuando !BuildConfig.CORTO (CalibrarActivity.java:88-96); y
 * bloqueoRehacer (java:1881-1899) es del botón "Rehacer" de Banco, inalcanzable desde RTV Calibra por
 * RF-COV-11. Se dejan tal cual: no forman parte de RF-COV-17 en la práctica.
 */
package com.dpi.retrov36;

import static org.junit.Assert.assertFalse;
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

public class Cov366Rf17Test {

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

    // ============================================================ resolverCorte() (java:1190-1191)

    /**
     * ALTO: tras un corte ANTES de que el #S llegara al equipo (el #G no cambia), al volver a pulsar
     * "Calibrar" en corto, resolverCorte() pregunta "El #S del código 8 no entró" — un diálogo real, con
     * el número de código en el título Y en el cuerpo, sin mirar `corto`.
     *
     * EN ROJO contra 316a6bc (sin tocar producción): el título y el cuerpo del diálogo llevan "código 8"
     * literal; el nombre corto (Fabrica.nombreCorto('8'), "amarillo, lámina tipo I") no aparece.
     */
    @Test
    public void a_resolverCorteEnCortoNoPreguntaConElNumeroDeCodigo() throws Exception {
        Escenario e = escenario();
        e.sim.inyectar("#S,8,", EquipoSimulado.Falla.CORTE_ANTES, 1);
        FlujoCalibracion f = flujo(e, true);
        try {
            f.calibrar(sel('8'), "Prueba", "Nota");
            org.junit.Assert.fail("CORTE_ANTES tiene que propagar la IOException (el #S nunca llegó)");
        } catch (IOException ex) {
            // corte antes de que el #S llegara al equipo
        }
        e.sim.conectado = true;
        FlujoCalibracion g = flujo(e, true);
        String r = g.resolverCorte();   // directo: aísla el diálogo, sin el "ya está CONFORME" de un segundo intento
        assertTrue("tiene que haber preguntado algo al resolver el corte: " + e.operador.titulos, !e.operador.titulos.isEmpty());
        for (String t : e.operador.titulos) {
            assertFalse("EN ROJO contra 316a6bc, el diálogo de resolverCorte() en corto preguntaba con "
                    + "\"código 8\": " + t, t.contains("código 8"));
        }
        // r puede ser null (corte resuelto sin más, remedida() conforme): eso no es el defecto bajo prueba.
        assertFalse("EN ROJO contra 316a6bc, el resultado de resolverCorte() en corto decía \"código 8\": " + r,
                r != null && r.contains("código 8"));

        // Regresión: en la app de campo, el diálogo sigue preguntando "El #S del código 8 no entró".
        Escenario e2 = escenario();
        e2.sim.inyectar("#S,8,", EquipoSimulado.Falla.CORTE_ANTES, 1);
        FlujoCalibracion f2 = flujo(e2, false);
        try {
            f2.calibrar(sel('8'), "Diego", "x");
            org.junit.Assert.fail();
        } catch (IOException ex) {
            // corte
        }
        e2.sim.conectado = true;
        FlujoCalibracion g2 = flujo(e2, false);
        g2.calibrar(sel('8'), "Diego", "x");
        assertTrue("la app de campo sigue preguntando \"El #S del código 8 no entró\": " + e2.operador.titulos,
                e2.operador.titulos.contains("El #S del código 8 no entró"));
    }

    // ============================================================ escribir() (java:1234)

    /**
     * ALTO: si el equipo no responde al #G previo al #S (ops.leerG falla), escribir() no escribe y
     * devuelve "No se pudo leer el estado anterior del código 8 (#G): no se escribe." — con el número de
     * código, sin mirar `corto`.
     *
     * EN ROJO contra 316a6bc: FlujoCalibracion.java:1234 devuelve el literal con "código " + k tal cual.
     */
    @Test
    public void b_escribirSinPoderLeerElEstadoAnteriorNoMuestraElNumeroDeCodigoEnCorto() throws Exception {
        Escenario e = escenario();
        e.sim.inyectar("#G,8", EquipoSimulado.Falla.ERR, 1);
        FlujoCalibracion f = flujo(e, true);
        String r = f.calibrar(sel('8'), "Prueba", "Nota");
        assertTrue(r, r.contains("se pudo leer el estado anterior"));
        assertFalse("EN ROJO contra 316a6bc, escribir() en corto decía \"código 8\": " + r, r.contains("código 8"));
        assertTrue("el nombre corto SÍ aparece (RF-COV-17): " + r, r.contains(Fabrica.nombreCorto('8')));

        // Regresión: en la app de campo, sigue diciendo "código 8" tal cual.
        Escenario e2 = escenario();
        e2.sim.inyectar("#G,8", EquipoSimulado.Falla.ERR, 1);
        FlujoCalibracion f2 = flujo(e2, false);
        String r2 = f2.calibrar(sel('8'), "Diego", "x");
        assertTrue("la app de campo sigue diciendo \"código 8\": " + r2,
                r2.contains("No se pudo leer el estado anterior del código 8 (#G): no se escribe."));
    }

    // ============================================================ persistencia() (java:1452,1455)

    /**
     * ALTO: si tras "apagar y encender" el #V# no responde, persistencia() sigue leyendo el #G de cada
     * código certificado de ESTA acta y arma el resumen con "código 8: #G ...; " sin mirar `corto` — ese
     * texto es justo el que CalibracionAutomatica.persistirYAceptarAuto devuelve tal cual (java:136-138)
     * cuando persistencia() no empieza por "Persistencia OK", y de ahí sale al resumen de "Calibrar".
     *
     * EN ROJO contra 316a6bc: FlujoCalibracion.java:1452 arma "código " + c.k tal cual, dentro de
     * "Persistencia FALLA (...): " + t.
     */
    @Test
    public void c_persistenciaFallidaNoMuestraElNumeroDeCodigoEnCorto() throws Exception {
        Escenario e = escenario();
        FlujoCalibracion f = flujo(e, true);
        String r8 = f.calibrar(sel('8'), "Prueba", "Nota 8");
        assertTrue(r8, r8.contains("Ahora: persistencia"));

        e.sim.inyectar("#V#", EquipoSimulado.Falla.ERR, 1);
        String p = f.persistencia();
        assertTrue(p, p.startsWith("Persistencia FALLA"));
        assertFalse("EN ROJO contra 316a6bc, persistencia() en corto decía \"código 8\": " + p, p.contains("código 8"));
        assertTrue("el nombre corto SÍ aparece (RF-COV-17): " + p, p.contains(Fabrica.nombreCorto('8')));

        // Regresión: en la app de campo, sigue diciendo "código 8" tal cual.
        Escenario e2 = escenario();
        FlujoCalibracion f2 = flujo(e2, false);
        String r82 = f2.calibrar(sel('8'), "Diego", "x");
        assertTrue(r82, r82.contains("Ahora: persistencia"));
        e2.sim.inyectar("#V#", EquipoSimulado.Falla.ERR, 1);
        String p2 = f2.persistencia();
        assertTrue("la app de campo sigue diciendo \"código 8\": " + p2, p2.contains("código 8"));
    }

    // ============================================================ aceptar() (java:1499,1501)

    /**
     * ALTO: mismo caso que el anterior, pero en la verificación final de aceptar() (java:1499): con la
     * persistencia ya OK y el #V# ya OK, si el #G del código certificado no coincide con lo certificado
     * (aquí, no responde), el propio `mal` de la verificación final se arma con "el código 8 no es el
     * certificado" (java:1501) y ESE es el texto que aceptar() devuelve tal cual, sin mirar `corto`.
     *
     * EN ROJO contra 316a6bc: FlujoCalibracion.java:1499/1501 arman "código " + c.k tal cual.
     */
    @Test
    public void d_aceptarFallidoNoMuestraElNumeroDeCodigoEnCorto() throws Exception {
        Escenario e = escenario();
        FlujoCalibracion f = flujo(e, true);
        String r8 = f.calibrar(sel('8'), "Prueba", "Nota 8");
        assertTrue(r8, r8.contains("Ahora: persistencia"));
        assertTrue(f.persistencia().startsWith("Persistencia OK"));

        e.sim.inyectar("#G,8", EquipoSimulado.Falla.ERR, 1);
        String a = f.aceptar();
        assertTrue(a, a.contains("Verificación final FALLA"));
        assertFalse("EN ROJO contra 316a6bc, aceptar() en corto decía \"código 8\": " + a, a.contains("código 8"));
        assertTrue("el nombre corto SÍ aparece (RF-COV-17): " + a, a.contains(Fabrica.nombreCorto('8')));

        // Regresión: en la app de campo, sigue diciendo "código 8" tal cual.
        Escenario e2 = escenario();
        FlujoCalibracion f2 = flujo(e2, false);
        String r82 = f2.calibrar(sel('8'), "Diego", "x");
        assertTrue(r82, r82.contains("Ahora: persistencia"));
        assertTrue(f2.persistencia().startsWith("Persistencia OK"));
        e2.sim.inyectar("#G,8", EquipoSimulado.Falla.ERR, 1);
        String a2 = f2.aceptar();
        assertTrue("la app de campo sigue diciendo \"código 8\": " + a2, a2.contains("código 8"));
    }
}
