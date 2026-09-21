/*
 * Pruebas de H-3, B-1, B-2, B-3 (RF-COV-21) y de la validación de marcarAceptadoReciente, de la revision
 * arquitecto-iot a "Cov_3.6.3_calibrar" (encargo del 21-sep-2026, contra HEAD ed7e604 de rtv-1.0-cierre).
 * H-1 y H-2 van en Cov364ArreglosTest (rules/modularidad.md: 500 lineas por fichero). SPEC que manda:
 * 05_Documentacion/SPEC-App-Calibracion-Coviandina.md §8, RF-COV-18 a 22 (de main).
 *
 * Andamiaje calcado del de Cov363ArreglosTest: el ZIP real de las 18:11 (md5 79b23590c3a6b1877d55b5c4818b93ce,
 * HUELLAS.txt de 06_Calibracion/SLV-002/), EquipoSimulado.slv002() y las clases de FlujoCalibracionTest
 * (Almacen, Operador). De donde sale cada valor esperado (CLAUDE.md del repositorio, §7):
 *  - H-3/B-1/B-2/B-3 (RF-COV-21): el texto literal del encargo ("colocaciones no válidas" / "re-medida
 *    fuera de ±10 %"; "ningún texto... lleva el número de código"; "una válida lo pone a cero").
 *  - marcarAceptadoReciente: el propio encargo ("no debe poder llamarse sin persistencia").
 */
package com.dpi.retrov36;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Cov364Rf21Test {

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
        Map<String, Double> xPatron = new HashMap<>();
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

    /** Lee un campo privado por reflexion (para comprobar marcarAceptadoReciente sin exponer el campo). */
    private static Object campo(Object o, String nombre) throws Exception {
        Field f = FlujoCalibracion.class.getDeclaredField(nombre);
        f.setAccessible(true);
        return f.get(o);
    }

    private static void campoAsignar(Object o, String nombre, Object valor) throws Exception {
        Field f = FlujoCalibracion.class.getDeclaredField(nombre);
        f.setAccessible(true);
        f.set(o, valor);
    }

    // ============================================================ H-3 / B-1 / B-2 / B-3 (RF-COV-21)

    /** Como FlujoCalibracionTest.Operador, pero también guarda los MENSAJES (no solo los títulos) de cada
     *  preguntar()/progreso(), para comprobar que ninguno lleva el número de código en corto (H-3). */
    private static final class OperadorConMensajes implements FlujoCalibracion.Operador {
        final EquipoSimulado sim;
        final Map<String, Double> xPatron;
        int ausentes;
        final List<String> vistos = new java.util.ArrayList<>();
        private static final Pattern COLOQUE = Pattern.compile("(?:Coloque|colocar) (P\\d+)");

        OperadorConMensajes(EquipoSimulado sim, Map<String, Double> xPatron) {
            this.sim = sim;
            this.xPatron = xPatron;
        }

        @Override
        public int preguntar(String titulo, String mensaje, String... opciones) {
            vistos.add(titulo);
            vistos.add(mensaje);
            Matcher m = COLOQUE.matcher(mensaje);
            if (m.find() && xPatron.containsKey(m.group(1))) {
                if (ausentes > 0) {
                    ausentes--;
                    sim.x = 566;
                } else {
                    sim.x = xPatron.get(m.group(1));
                }
            }
            return 0;
        }

        @Override
        public void progreso(String texto) {
            vistos.add(texto);
        }

        @Override
        public boolean apagarYEncender() {
            sim.apagarYEncender();
            return true;
        }
    }

    /**
     * ALTO H-3 (RF-COV-17/21): en el camino corto, ningún texto visto por el operador (título, mensaje o
     * progreso) lleva el número de código: ni "código 8" en las preguntas de re-medida y colocación, ni al
     * forzar la restauración por demasiadas colocaciones no válidas. En la app de campo (corto = false)
     * SÍ lo lleva (regresión: RF-COV-17 no aplica ahí).
     *
     * EN ROJO contra ed7e604: "Re-medida del código 8" (FlujoCalibracion.java, antes :1423),
     * "Código 8: colocación..." (:1432) y "Demasiadas colocaciones no válidas seguidas...del código 8"
     * (:1467-1468) aparecían tal cual en corto.
     */
    @Test
    public void h3_ningunTextoAlOperadorLlevaElNumeroDeCodigoEnCorto() throws Exception {
        Escenario e = escenario();
        OperadorConMensajes op = new OperadorConMensajes(e.sim, e.xPatron);
        op.ausentes = 20;   // fuerza colocaciones no validas -> demasiadas -> restaurar (arreglo1, Cov363)
        FlujoCalibracion.Contexto ctx = FlujoCalibracion.identificar(e.sim, "SLV-002", MAC, Boolean.TRUE, "6/6", "t");
        FlujoCalibracion f = new FlujoCalibracion(e.sim, op, e.almacen, e.cola, e.campana, catalogo, e.decisiones,
                ctx, reloj(), true);
        f.pin("1234");

        String r = f.calibrar(sel('8'), "Prueba", "Nota H-3");
        assertTrue(r, r.contains("Demasiadas colocaciones no válidas"));
        for (String texto : op.vistos) {
            assertFalse("EN ROJO contra ed7e604, este texto llevaba \"código 8\": " + texto, texto.contains("código 8"));
        }
        assertTrue("el nombre corto SÍ aparece (RF-COV-17): " + r, r.contains(Fabrica.nombreCorto('8')));

        // Regresión: en la app de campo, "código 8" sigue apareciendo (no se le quitó nada).
        Escenario e2 = escenario();
        FlujoCalibracionTest.Operador op2 = new FlujoCalibracionTest.Operador(e2.sim, e2.xPatron);
        op2.ausentes = 20;
        FlujoCalibracion.Contexto ctx2 = FlujoCalibracion.identificar(e2.sim, "SLV-002", MAC, Boolean.TRUE, "6/6", "t");
        FlujoCalibracion f2 = new FlujoCalibracion(e2.sim, op2, e2.almacen, e2.cola, e2.campana, catalogo,
                e2.decisiones, ctx2, reloj(), false);
        f2.pin("1234");
        String r2 = f2.calibrar(sel('8'), "Prueba", "Nota H-3 campo");
        assertTrue("la app de campo sigue diciendo \"código 8\": " + r2, r2.contains("código 8"));
    }

    /**
     * MEDIO B-1 (RF-COV-21): tras una restauración por demasiadas colocaciones no válidas, en corto el
     * texto NO pide "pulse Rechazar" (porque el acta sin ningún código conforme se rechaza SOLA justo
     * después, en CalibracionAutomatica.anotarNoCalibrado): pedirlo sería mentira. En la app de campo (sin
     * ese cierre automático) sigue pidiéndolo.
     *
     * EN ROJO contra ed7e604: el texto de restaurarCodigo() no distinguía corto de campo y siempre decía
     * "pulse Rechazar" cuando ningún código quedaba conforme.
     */
    @Test
    public void b1_pulseRechazarNoApareceEnCortoTrasRestaurarSolaConAcierto() throws Exception {
        Escenario e = escenario();
        e.operador.ausentes = 20;
        FlujoCalibracion f = flujo(e, true);
        String resumen = f.calibrarAutomatico("Prueba B-1");
        assertTrue("el 8 termina restaurado: " + resumen, resumen.contains("No calibrado"));
        assertFalse("EN ROJO contra ed7e604: pedía \"pulse Rechazar\" aunque el acta ya se cerró sola: " + resumen,
                resumen.contains("pulse Rechazar"));
        assertNull("el acta ya quedó rechazada sola (anotarNoCalibrado)", f.acta());

        Escenario e2 = escenario();
        e2.operador.ausentes = 20;
        FlujoCalibracion f2 = flujo(e2, false);
        String r2 = f2.calibrar(sel('8'), "Prueba B-1 campo", "Nota");
        assertTrue("en campo SÍ pide pulsar Rechazar (no hay cierre automático): " + r2, r2.contains("pulse Rechazar"));
    }

    /**
     * MEDIO B-2/B-3 (RF-COV-21): el motivo de cada restauración es el real: "colocaciones no válidas"
     * cuando se llega a MAX_NO_VALIDAS, "re-medida fuera de ±10 %" cuando se restaura por dos re-medidas
     * NO CONFORME (en corto, donde el criterio es VERIF-5-10 ±10 %).
     */
    @Test
    public void b2b3_motivoDeLaRestauracionEsElRealSegunLaCausa() throws Exception {
        Escenario porColocaciones = escenario();
        porColocaciones.operador.ausentes = 20;
        FlujoCalibracion f1 = flujo(porColocaciones, true);
        String r1 = f1.calibrar(sel('8'), "Prueba", "Nota");
        assertTrue("motivo real: colocaciones no válidas: " + r1, r1.contains("colocaciones no válidas"));

        Escenario porRemedida = escenario();
        porRemedida.operador.factor = 0.85;   // colocación válida, pero fuera de tolerancia (QA-3615-06)
        FlujoCalibracion f2 = flujo(porRemedida, true);
        String r2 = f2.calibrar(sel('8'), "Prueba", "Nota");
        assertTrue("EN ROJO contra ed7e604: decía \"tras dos re-medidas no conformes\", no el motivo pedido "
                + "por el encargo: " + r2, r2.contains("re-medida fuera de ±10 %"));
    }

    /**
     * MEDIO B-3 (RF-COV-21): el contador de colocaciones no válidas es de SEGUIDAS: una colocación válida
     * lo pone a cero. Se fuerzan 9 no válidas, 1 válida (menos que MAX_NO_VALIDAS = 10: no restaura),
     * otras 9 no válidas (si el contador no se hubiera puesto a cero, 9 + 1 ya "gastada" + 9 = 18 sin reset
     * habría superado 10 mucho antes) y finalmente las válidas que faltan para completar las K = 5
     * colocaciones: la re-medida tiene que completarse, no restaurar.
     *
     * EN ROJO contra ed7e604 (contador declarado fuera del bucle de colocaciones, nunca puesto a cero):
     * al llegar a la 10ª colocación no válida ACUMULADA (la 1ª del segundo tramo de 9), se restauraba con
     * "Demasiadas colocaciones no válidas seguidas": el resultado no contenía "Ahora: persistencia".
     */
    @Test
    public void b3_elContadorDeNoValidasEsDeSeguidasUnaValidaLoReinicia() throws Exception {
        Escenario e = escenario();
        // Cola: 9 no validas, 1 valida (reinicia), 9 no validas, y validas de sobra para completar K=5.
        Deque<Boolean> validas = new ArrayDeque<>();
        for (int i = 0; i < 9; i++) {
            validas.add(false);
        }
        validas.add(true);
        for (int i = 0; i < 9; i++) {
            validas.add(false);
        }
        for (int i = 0; i < 10; i++) {
            validas.add(true);
        }
        OperadorSecuencia opSec = new OperadorSecuencia(e.sim, e.xPatron, validas);
        FlujoCalibracion.Contexto ctx = FlujoCalibracion.identificar(e.sim, "SLV-002", MAC, Boolean.TRUE, "6/6", "t");
        FlujoCalibracion f = new FlujoCalibracion(e.sim, opSec, e.almacen, e.cola, e.campana, catalogo, e.decisiones,
                ctx, reloj(), true);
        f.pin("1234");

        String r = f.calibrar(sel('8'), "Prueba", "Nota B-3");
        assertFalse("EN ROJO contra ed7e604: sin el reinicio, restauraba antes de completar la re-medida: " + r,
                r.contains("Demasiadas colocaciones no válidas"));
        assertTrue("con el reinicio, la re-medida se completa: " + r, r.contains("Ahora: persistencia"));
    }

    /** Solo para b3_: coloca "válida" o "no válida" según una cola fija, no según un contador. */
    private static final class OperadorSecuencia implements FlujoCalibracion.Operador {
        final EquipoSimulado sim;
        final Map<String, Double> xPatron;
        final Deque<Boolean> validas;
        // A diferencia del COLOQUE de FlujoCalibracionTest.Operador (solo "Coloque"/"colocar"), aquí hace
        // falta decidir también en "Levante y apoye" (transición entre colocaciones válidas: si no, la x
        // buena de la última colocación válida seguiría sirviendo para todas las siguientes sin más
        // "Coloque" de por medio, y la cola de válidas/no válidas nunca se consumiría del todo).
        private static final Pattern PATRON = Pattern.compile("(P\\d+)");

        OperadorSecuencia(EquipoSimulado sim, Map<String, Double> xPatron, Deque<Boolean> validas) {
            this.sim = sim;
            this.xPatron = xPatron;
            this.validas = validas;
        }

        @Override
        public int preguntar(String titulo, String mensaje, String... opciones) {
            Matcher m = PATRON.matcher(mensaje);
            if (m.find() && xPatron.containsKey(m.group(1))) {
                boolean valida = validas.isEmpty() || Boolean.TRUE.equals(validas.pollFirst());
                sim.x = valida ? xPatron.get(m.group(1)) : 566;
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

    // ============================================================ modularidad: marcarAceptadoReciente

    /**
     * Modularidad (Cov364): marcarAceptadoReciente no puede llamarse sin persistencia real: si se llama
     * con un acta todavía viva (evidencia de que NO hubo un "Acta ACEPTADA" de verdad), no tiene efecto.
     *
     * EN ROJO contra ed7e604 (el método no validaba nada, siempre asignaba): tras escribir el 8 pero SIN
     * persistencia ni aceptación, llamar a marcarAceptadoReciente ponía encendidoReciente = true (con
     * sesionTodo = true) igualmente.
     */
    @Test
    public void marcarAceptadoRecienteNoTieneEfectoSinAceptarDeVerdad() throws Exception {
        Escenario e = escenario();
        FlujoCalibracion f = flujo(e, true);
        String r = f.calibrar(sel('8'), "Prueba", "Nota marcarAceptado");
        assertTrue(r, r.contains("Ahora: persistencia"));
        assertNotNull("el acta sigue viva: no hubo persistencia ni aceptación", f.acta());

        campoAsignar(f, "sesionTodo", true);
        campoAsignar(f, "encendidoReciente", false);
        java.lang.reflect.Method m = FlujoCalibracion.class.getDeclaredMethod("marcarAceptadoReciente", char.class);
        m.setAccessible(true);
        m.invoke(f, '8');

        assertEquals("EN ROJO contra ed7e604: quedaba en true aunque el acta seguía abierta (sin persistencia)",
                Boolean.FALSE, campo(f, "encendidoReciente"));
    }
}
