/*
 * Pruebas de los arreglos ALTO/MEDIO/BAJO de las revisiones arquitecto-iot y qa-istqb a
 * "Cov_3.6.2_calibrar" (encargo del 21-sep-2026, contra HEAD 7b4391b de rtv-1.0-cierre). SPEC que manda:
 * 05_Documentacion/SPEC-App-Calibracion-Coviandina.md §8 (RF-COV-11 a 17, de main); decisiones VERIF-5-10
 * y FECHA-EQUIPO de 06_Calibracion/SLV-002/DECISIONES-Diego-2026-09-19.md (de main).
 *
 * Andamiaje calcado del de Cov362DefectosTest: el ZIP real de las 18:11 (md5 79b23590c3a6b1877d55b5c4818b93ce,
 * HUELLAS.txt de 06_Calibracion/SLV-002/), EquipoSimulado.slv002() y las clases de FlujoCalibracionTest
 * (Almacen, Operador). De donde sale cada valor esperado (CLAUDE.md del repositorio, §7):
 *  - Arreglo 1 (MAX_NO_VALIDAS): el propio codigo (restaurarCodigo ya existia para la via de
 *    debeRestaurarse(), FlujoCalibracion.java:1356-1358; aqui se comprueba que la otra via hace lo mismo).
 *  - Arreglo 2: INFORME-Ajuste-SLV-002-20260919-1811.md §4.2 (P49: cert 81, x banco 813,30, curva del b
 *    c1 = 3.20071157E-01 / c0 = -1.82592593E+02) y VERIF-5-10 (DECISIONES-Diego-2026-09-19.md, nota 3).
 *  - Arreglos 3, 4 y 6: el contrato #SC/#GC# y #Q# de 05_Documentacion/PROTOCOLO-V3.6.md:44,51-52, y
 *    Acta.java:403-404 (texto literal citado en el encargo).
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Cov363ArreglosTest {

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

    /** El mismo reloj de siempre en estas pruebas: "hoy" 2026-09-21, distinto de la fecha de slv002() (19-sep). */
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

    /** Campaña + banco del ZIP real de las 18:11 (REPRESENTATIVO), con P43 (del 8) y P49 (de la b) en xPatron. */
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

    // ============================================================ arreglo 1 (ALTO, QA): MAX_NO_VALIDAS

    /**
     * ALTO (QA, FlujoCalibracion.java:1396-1400): antes de este arreglo, al llegar a MAX_NO_VALIDAS
     * colocaciones no válidas seguidas la re-medida paraba SIN restaurar: la curva nueva (del ajuste)
     * quedaba escrita en el equipo sin verificar, a diferencia de la vía de {@code debeRestaurarse()}
     * (dos re-medidas NO CONFORME), que sí restaura. Aquí se fuerza a que el patrón "nunca esté puesto"
     * (Operador.ausentes, ya usado por FlujoCalibracionTest para T-S09 y afines): 20 colocaciones seguidas
     * en el oscuro, más que MAX_NO_VALIDAS = 10.
     *
     * EN ROJO contra 7b4391b (salida real, ver el informe): el mensaje NO contenía "restaurad" y
     * {@code sim.curvas.get('8')} quedaba en la curva NUEVA (ajustada), no en la de fábrica.
     */
    @Test
    public void arreglo1_maxNoValidasRestauraLaCurvaEnVezDeDejarlaSinVerificar() throws Exception {
        Escenario e = escenario();
        Ecuacion fabricaOcho = Fabrica.ecuacion('8');
        assertTrue("de partida, el 8 está en la curva de fábrica", e.sim.curvas.get('8').igualFloat32(fabricaOcho));

        FlujoCalibracion f = flujo(e, false);
        e.operador.ausentes = 20;   // más que MAX_NO_VALIDAS: nunca llega a una colocación válida
        String r = f.calibrar(sel('8'), "Prueba", "Nota arreglo 1");

        assertTrue("se para por demasiadas colocaciones no válidas: " + r,
                r.contains("Demasiadas colocaciones no válidas"));
        assertTrue("y el arreglo 1 dice que se restauró (o, si la restauración no se pudo verificar, lo dice "
                + "también, pero SIEMPRE pasa por restaurarCodigo — nunca se calla): " + r,
                r.contains("restaurad") || r.contains("RESTAURACIÓN"));
        assertTrue("la curva del 8 vuelve a ser la de fábrica: no queda una curva nueva sin verificar en el equipo",
                e.sim.curvas.get('8').igualFloat32(fabricaOcho));
    }

    // ============================================================ arreglo 2 (ALTO, arquitecto A-1)

    /**
     * ALTO (arquitecto A-1, RF-COV-12, VERIF-5-10): antes de este arreglo, en la app de calibrar
     * (corto = true) el código b seguía juzgándose con el criterio de {@code s_rep} (v&iacute;a
     * REMEDIDA-b/RF-CAL-18, Remedida3611.evaluarConDispensa), porque en
     * {@code FlujoCalibracion.remedida()} el chequeo de {@code f.reglaRemedida} iba ANTES que el de
     * {@code corto} (FlujoCalibracion.java, antes :1418-1422). VERIF-5-10 (confirmado por Diego): "la misma
     * regla para todos los códigos, la b incluida".
     *
     * Datos reales (informe §4.2): P49 certificado 81, x del banco 813,30; curva del b escrita
     * c1 = 3.20071157E-01, c0 = -1.82592593E+02. Se desplaza la x de la re-medida +7 cuentas (informe §6:
     * "el mismo patrón se mueve 7 cuentas entre dos series del mismo cuarto de hora"), que basta para que
     * el criterio de reproducibilidad (s_rep, ~2 cuentas de límite con K = 5) la declare NO CONFORME
     * aunque el certificado la de por buena (P49 queda dentro de ±10 %, informe: "−4,0 %" sin desplazar).
     *
     * EN ROJO contra 7b4391b (salida real, ver el informe): el resumen de calibrarAutomatico() NO incluía
     * la b entre los calibrados ("No calibrado: ... rojo ...").
     */
    @Test
    public void arreglo2_laBSeJuzgaComoEl8EnLaAppDeCalibrarAunqueLaXSeDesplace() throws Exception {
        Escenario e = escenario();
        e.xPatron.put("P49", e.campana.elegida("P49").media() + 7);   // re-medida desplazada 7 cuentas
        FlujoCalibracion f = flujo(e, true);   // corto = true: RF-COV-12

        String resumen = f.calibrarAutomatico("Prueba arreglo 2");

        assertTrue("el 8 se calibra primero, sin cambios de este arreglo: " + resumen,
                resumen.contains(Fabrica.nombreCorto('8')));
        assertTrue("VERIF-5-10 / A-1: la b (rojo, lámina tipo I) queda CALIBRADA pese al desplazamiento de "
                + "x, porque en corto = true se juzga con evaluarCertificado (±10 % del certificado), no con "
                + "s_rep: " + resumen, resumen.contains("Calibrado: ") && resumen.contains(Fabrica.nombreCorto('b')));
        assertFalse("y no debería estar en la lista de no calibrados: " + resumen,
                resumen.substring(resumen.indexOf("No calibrado:")).contains(Fabrica.nombreCorto('b')));
    }

    // ============================================================ arreglo 3 (MEDIO, A-06, RF-COV-13)

    /**
     * MEDIO (A-06): tras aceptar() con éxito, el acta cita, ANTES de enviar #SC, la fecha anterior del
     * equipo — dato que usa {@link FlujoCalibracion#rechazar} si hiciera falta devolverla.
     */
    @Test
    public void arreglo3a_aceptarAnotaLaFechaAnteriorAntesDeEnviarSC() throws Exception {
        Escenario e = escenario();
        FlujoCalibracion f = flujo(e, false);
        String r = f.calibrar(sel('8'), "Prueba", "Nota");
        assertTrue(r, r.contains("Ahora: persistencia"));
        assertTrue(f.persistencia().startsWith("Persistencia OK"));

        String ac = f.aceptar();
        assertTrue(ac, ac.startsWith("Acta ACEPTADA"));
        // El acta ya está cerrada (f.acta() == null); lo que quedó anotado se ve en el ZIP/almacen: aquí,
        // en el diario que Almacen guardó al cerrar.
        String cerrada = e.almacen.diariosCerrados.get(e.almacen.diariosCerrados.size() - 1);
        assertTrue("EN ROJO contra 7b4391b: no existía ningún DATO \"#SC emitido, fecha anterior\" — "
                + "rechazar() no tenía de dónde sacar la fecha anterior si hiciera falta devolverla:\n" + cerrada,
                cerrada.contains("#SC emitido, fecha anterior") && cerrada.contains("2026-09-19"));
    }

    /**
     * MEDIO (A-06): #GC# distinto tras escribir #SC (aquí, simulado con OK_SIN_HACER, como ya usa
     * {@code Cov362DefectosTest.h3_}) anota #SC y #GC# en el acta (antes de este arreglo, la rama de fallo
     * de aceptar() no anotaba nada: FlujoCalibracion.java, antes :1539-1541) y, al rechazar, se intenta
     * devolver la fecha anterior con #SC,<anterior># y se relee #GC# — aquí no hay nada que devolver de
     * verdad (el #SC no llegó a aplicarse), pero el intento queda hecho y anotado.
     */
    @Test
    public void arreglo3b_gcDistintoTrasSCSeAnotaYRechazarIntentaDevolverLaFechaAnterior() throws Exception {
        Escenario e = escenario();
        FlujoCalibracion f = flujo(e, false);
        String r = f.calibrar(sel('8'), "Prueba", "Nota");
        assertTrue(r, r.contains("Ahora: persistencia"));
        assertTrue(f.persistencia().startsWith("Persistencia OK"));

        e.sim.inyectar("#SC", EquipoSimulado.Falla.OK_SIN_HACER, 1);   // #OK# pero NO cambia sim.fecha
        String ac = f.aceptar();
        assertTrue("#SC no quedó grabada: " + ac, ac.contains("no quedó grabada"));
        assertFalse("el acta NO queda aceptada", ac.startsWith("Acta ACEPTADA"));
        assertNotNull("sigue en curso: se puede rechazar", f.acta());

        Acta acta = f.acta();
        assertTrue("EN ROJO contra 7b4391b: la rama de fallo no anotaba #SC (antes :1539-1541)",
                acta.dato("#SC") != null && acta.dato("#SC").contains("EEPROM") == false);
        assertNotNull("EN ROJO contra 7b4391b: la rama de fallo no anotaba #GC#", acta.dato("#GC#"));

        int scAntes = e.sim.cuantas("#SC,");
        String rech = f.rechazar("arreglo 3b");
        assertTrue(rech, rech.contains("Acta rechazada"));
        assertTrue("EN ROJO contra 7b4391b: rechazar() nunca escribía #SC — aquí, al haber #SC emitido en "
                + "esta acta, se intenta devolver la fecha anterior (2026-09-19)", e.sim.cuantas("#SC,") > scAntes);
        assertEquals("la fecha del equipo sigue siendo la de antes (nunca cambió: OK_SIN_HACER)", "2026-09-19",
                e.sim.fecha);
    }

    /**
     * MEDIO (A-06): si #GC# falla ANTES de decidir si hace falta escribir #SC (aceptar() no llega a
     * anotar nada ni a tocar #SC), rechazar() no debe intentar devolver ninguna fecha — no hay nada que
     * devolver. Curva restaurada igualmente (P11-M1, sin cambios de este encargo).
     */
    @Test
    public void arreglo3c_ioExceptionEnElPrimerGCNoDejaNadaQueDevolverAlRechazar() throws Exception {
        Escenario e = escenario();
        FlujoCalibracion f = flujo(e, false);
        String r = f.calibrar(sel('8'), "Prueba", "Nota");
        assertTrue(r, r.contains("Ahora: persistencia"));
        assertTrue(f.persistencia().startsWith("Persistencia OK"));

        e.sim.inyectar("#GC#", EquipoSimulado.Falla.CORTE_DESPUES, 1);
        try {
            f.aceptar();
            org.junit.Assert.fail("tenía que propagar la IOException del corte en #GC#");
        } catch (IOException ex) {
            // esperado: el enlace se corta antes de que aceptar() decida si hace falta #SC
        }
        e.sim.conectado = true;   // el enlace "vuelve a conectar" (como el resto de pruebas con CORTE_DESPUES)
        assertNotNull("el acta sigue en curso (la excepción no la cerró)", f.acta());
        assertNull("#SC nunca se emitió en esta acta", f.acta().dato("#SC emitido, fecha anterior"));

        int scAntes = e.sim.cuantas("#SC,");
        String rech = f.rechazar("arreglo 3c");
        assertTrue(rech, rech.contains("Acta rechazada"));
        assertEquals("rechazar() no escribe #SC si nunca se emitió en esta acta", scAntes, e.sim.cuantas("#SC,"));
        assertEquals("2026-09-19", e.sim.fecha);
    }

    // ============================================================ arreglo 4 (MEDIO)

    /**
     * MEDIO: si el código queda escrito y conforme pero la persistencia (o #SC) falla, el resumen de
     * calibrarAutomatico() no lo cuenta sin más como "No calibrado: <motivo>": dice que queda escrito sin
     * aceptar y qué hacer.
     */
    @Test
    public void arreglo4a_resumenDiceQuedaEscritoSinAceptarSiLaPersistenciaFalla() throws Exception {
        Escenario e = escenario();
        FlujoCalibracion f = flujo(e, true);
        // El 8 se escribe y se re-mide normalmente (T-C41 incluido, con el enlace cayendo de verdad): el
        // código queda CONFORME, listo para persistencia.
        String r = f.calibrar(sel('8'), "Prueba arreglo 4a", "Nota");
        assertTrue(r, r.contains("Ahora: persistencia"));

        // Segunda pulsación de "Calibrar": esta vez la persistencia no consigue ver caer el enlace.
        e.operador.sinApagar = 99;
        String resumen = f.calibrarAutomatico("Prueba arreglo 4a");

        assertTrue("EN ROJO contra 7b4391b: el 8 caía sin más en \"No calibrado: ... Persistencia NO "
                + "válida...\", sin decir que queda escrito sin aceptar: " + resumen,
                resumen.contains("queda escrito sin aceptar"));
        assertTrue("y da los dos caminos: " + resumen,
                resumen.contains("pulse Calibrar para terminar o Rechazar para restaurar"));
    }

    /**
     * MEDIO: tras un código restaurado (dos re-medidas NO CONFORME) que deja el acta sin ningún código
     * conforme, el siguiente calibrarAutomatico() no vuelve a tropezar con Acta.motivoNoEscribir ("no se
     * reescribe en esta acta", Acta.java:403-404): el arreglo rechaza sola esa acta muerta.
     */
    @Test
    public void arreglo4b_actaSinConformeSeRechazaSolaYElSiguienteCalibrarNoSeQuedaAtascado() throws Exception {
        Escenario e = escenario();
        // La re-medida del 8 nunca coincide con lo certificado: dos NO CONFORME -> restaurado (P11-M1).
        e.xPatron.put("P43", e.campana.elegida("P43").media() * 3);
        FlujoCalibracion f = flujo(e, true);

        String resumen1 = f.calibrarAutomatico("Prueba arreglo 4b");
        assertTrue("el 8 termina restaurado (dos NO CONFORME): " + resumen1, resumen1.contains("No calibrado"));
        assertNull("EN ROJO contra 7b4391b: sin el arreglo, el acta del 8 restaurado quedaba abierta, viva, "
                + "bloqueando la siguiente pulsación", f.acta());

        // Segunda pulsación de "Calibrar": no debe tropezar con "no se reescribe en esta acta".
        String resumen2 = f.calibrarAutomatico("Prueba arreglo 4b, segunda pulsación");
        assertFalse("EN ROJO contra 7b4391b: la segunda pulsación repetía \"no se reescribe en esta acta\" "
                + "(Acta.java:403-404) en vez de intentarlo de nuevo: " + resumen2,
                resumen2.contains("no se reescribe en esta acta"));
    }

    // ============================================================ arreglo 5 (BAJO, B-2)

    /**
     * BAJO B-2 (QA): el predicado que usa CalibrarActivity para NO disparar "No se puede calibrar" cuando
     * la calibración fue correcta, aunque el texto combinado mencione "falta" por el motivo de OTRO código.
     */
    @Test
    public void arreglo5_huboExitoNoSeConfundeConElMotivoDeOtroCodigoBloqueado() {
        String resumenConExito = "Calibrado: amarillo, lámina tipo I; . No calibrado: azul intenso: falta la "
                + "serie del banco de P81 (re-medida del 5).;";
        String resumenSinExito = "Calibrado: ninguno. No calibrado: amarillo, lámina tipo I: falta la serie "
                + "del banco de P43 (re-medida del 8).;";

        assertTrue("EN ROJO si se invierte la condición (ver el informe): un resumen con algo calibrado tiene "
                + "que contar como éxito aunque OTRO código bloqueado diga \"falta\"", huboExitoInvertible(resumenConExito));
        assertFalse("y uno sin nada calibrado, no", huboExitoInvertible(resumenSinExito));
    }

    /** Envoltorio de CalibracionAutomatica.huboExito: existe solo para que el comentario de la prueba tenga
     * un sitio donde decir, sin ambigüedad, qué se invirtió para ver la roja (ver el informe de entrega). */
    private static boolean huboExitoInvertible(String resumen) {
        return CalibracionAutomatica.huboExito(resumen);
    }

    // ============================================================ arreglo 6 (BAJO, QA)

    /** BAJO (QA): al terminar calibrarAutomatico(), se envía #Q# para cerrar el modo administrador. */
    @Test
    public void arreglo6_alTerminarCalibrarAutomaticoEnviaQParaCerrarElModoAdministrador() throws Exception {
        Escenario e = escenario();
        FlujoCalibracion f = flujo(e, true);

        f.calibrarAutomatico("Prueba arreglo 6");

        assertTrue("EN ROJO contra 7b4391b: calibrarAutomatico() nunca enviaba #Q#: " + e.sim.recibidas,
                e.sim.cuantas("#Q#") > 0);
    }
}
