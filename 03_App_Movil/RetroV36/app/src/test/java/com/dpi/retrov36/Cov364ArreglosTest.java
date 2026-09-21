/*
 * Pruebas de H-1 y H-2 de la revision arquitecto-iot a "Cov_3.6.3_calibrar" (encargo del 21-sep-2026,
 * contra HEAD ed7e604 de rtv-1.0-cierre). H-3/B-1/B-2/B-3 y la validacion de marcarAceptadoReciente van
 * en Cov364Rf21Test (rules/modularidad.md: 500 lineas por fichero). SPEC que manda:
 * 05_Documentacion/SPEC-App-Calibracion-Coviandina.md §8, RF-COV-18 a 22 (de main).
 *
 * Andamiaje calcado del de Cov363ArreglosTest: el ZIP real de las 18:11 (md5 79b23590c3a6b1877d55b5c4818b93ce,
 * HUELLAS.txt de 06_Calibracion/SLV-002/), EquipoSimulado.slv002() y las clases de FlujoCalibracionTest
 * (Almacen, Operador). De donde sale cada valor esperado (CLAUDE.md del repositorio, §7):
 *  - H-1 (RF-COV-18/19): el propio contrato #GC#/#SC (PROTOCOLO-V3.6.md) y el texto literal del encargo
 *    ("no se pudo leer la fecha del equipo; reintente"; "si no hay fecha anterior leída, no se envía nada").
 *  - H-2 (RF-COV-20): decisiones.csv del APK (REMEDIDA-b = RF-CAL-18) y VERIF-5-10 (RF-COV-12 para todos).
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

public class Cov364ArreglosTest {

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

    /** "hoy" 2026-09-21, distinto de la fecha de slv002() (19-sep): para ver moverse #SC de verdad. */
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

    // ============================================================ H-1 (ALTO): RF-COV-18/19

    /**
     * ALTO H-1 (RF-COV-18): si #GC# no responde (MUDO) antes de grabar #SC, no se envía #SC — dos veces
     * seguidas (dos pulsaciones de "Aceptar"), para que quede claro que no es un accidente de una sola
     * lectura. El código queda "escrito sin aceptar": el acta sigue abierta.
     *
     * EN ROJO contra ed7e604 (salida real, ver el informe): {@code aceptar()} anotaba
     * {@code antes == null ? Calibracion.NONE : antes} y enviaba "#SC,2026-09-21#" de todas formas — el
     * resultado empezaba por "Acta ACEPTADA" y {@code e.sim.cuantas("#SC,")} valía 1, no 0.
     */
    @Test
    public void h1a_gcMudoAntesDeAceptarNoEnviaSCDosVecesSeguidas() throws Exception {
        Escenario e = escenario();
        FlujoCalibracion f = flujo(e, false);
        String r = f.calibrar(sel('8'), "Prueba", "Nota H-1a");
        assertTrue(r, r.contains("Ahora: persistencia"));
        assertTrue(f.persistencia().startsWith("Persistencia OK"));

        e.sim.inyectar("#GC#", EquipoSimulado.Falla.MUDO, 2);

        String a1 = f.aceptar();
        assertTrue("EN ROJO contra ed7e604: enviaba #SC igualmente: " + a1, a1.contains("no se pudo leer la fecha"));
        assertFalse("no puede quedar aceptada sin fecha leída: " + a1, a1.startsWith("Acta ACEPTADA"));
        assertNotNull("el código queda escrito sin aceptar: el acta sigue abierta", f.acta());
        assertEquals("EN ROJO contra ed7e604: #SC se enviaba pese al MUDO", 0, e.sim.cuantas("#SC,"));

        String a2 = f.aceptar();
        assertTrue("segunda pulsación, mismo resultado: " + a2, a2.contains("no se pudo leer la fecha"));
        assertNotNull(f.acta());
        assertEquals("sigue sin haberse enviado #SC tras la segunda pulsación", 0, e.sim.cuantas("#SC,"));
        assertEquals("la fecha del equipo no se tocó", "2026-09-19", e.sim.fecha);
    }

    /**
     * ALTO H-1 (RF-COV-19): tras el MUDO de h1a (ningún #SC emitido, ninguna "fecha anterior" anotada),
     * Rechazar no debe intentar devolver nada: "nunca se borra una fecha que no se leyó".
     *
     * EN ROJO contra ed7e604: la fecha anterior habría quedado anotada como {@code Calibracion.NONE} (el
     * "antes == null ? NONE : antes" de :1578) y rechazar() habría enviado "#SC,NONE#" — borrando la fecha
     * real del equipo sin haberla leído nunca.
     */
    @Test
    public void h1b_rechazarTrasGCMudoNoEnviaNingunSC() throws Exception {
        Escenario e = escenario();
        FlujoCalibracion f = flujo(e, false);
        String r = f.calibrar(sel('8'), "Prueba", "Nota H-1b");
        assertTrue(r, r.contains("Ahora: persistencia"));
        assertTrue(f.persistencia().startsWith("Persistencia OK"));
        e.sim.inyectar("#GC#", EquipoSimulado.Falla.MUDO, 1);
        String a1 = f.aceptar();
        assertTrue(a1, a1.contains("no se pudo leer la fecha"));
        assertNull("H-1 (RF-COV-18): sin fecha anterior leída, no se anota nada que devolver",
                f.acta().dato("#SC emitido, fecha anterior"));

        String rech = f.rechazar("H-1b: no se pudo leer la fecha, se rechaza");
        assertTrue(rech, rech.contains("Acta rechazada"));
        assertEquals("EN ROJO contra ed7e604: rechazar() habría enviado \"#SC,NONE#\"", 0, e.sim.cuantas("#SC,"));
        assertEquals("la fecha real del equipo sigue intacta", "2026-09-19", e.sim.fecha);
    }

    // ============================================================ H-2 (MEDIO): RF-COV-20

    /**
     * MEDIO H-2 (RF-COV-20): en el camino corto, la línea DATO "código b" del acta cita RF-COV-12
     * (VERIF-5-10), nunca RF-CAL-18 ni s_rep; en la app de campo (corto = false) sigue citando RF-CAL-18
     * (decisiones.csv del APK: REMEDIDA-b = RF-CAL-18), que es lo que de verdad usa esa app.
     *
     * EN ROJO contra ed7e604: la línea "código b" del acta en corto citaba literalmente "RF-CAL-18" (vía
     * TablaCalibracion.Fila.origen/aviso, construidos con la decisión REMEDIDA-b) y "s_rep", sin
     * distinguir corto de campo.
     */
    @Test
    public void h2a_lineaDatoCodigoBEnCortoCitaRFCOV12NoRFCAL18() throws Exception {
        Escenario corta = escenario();
        FlujoCalibracion fc = flujo(corta, true);
        fc.calibrar(sel('8'), "Prueba", "Nota H-2a");
        String datoCorto = fc.acta().dato("código b");
        assertNotNull(datoCorto);
        assertTrue("en corto cita RF-COV-12: " + datoCorto, datoCorto.contains("RF-COV-12"));
        assertFalse("EN ROJO contra ed7e604: citaba RF-CAL-18 también en corto: " + datoCorto,
                datoCorto.contains("RF-CAL-18"));
        assertFalse("y tampoco s_rep: " + datoCorto, datoCorto.contains("s_rep"));

        Escenario campo = escenario();
        FlujoCalibracion ff = flujo(campo, false);
        ff.calibrar(sel('8'), "Prueba", "Nota H-2a campo");
        String datoCampo = ff.acta().dato("código b");
        assertNotNull(datoCampo);
        assertTrue("la app de campo SÍ cita RF-CAL-18 (es la regla que de verdad usa, REMEDIDA-b): " + datoCampo,
                datoCampo.contains("RF-CAL-18"));
    }

    /**
     * MEDIO H-2 (RF-COV-20): la línea CONFORMIDAD del código b, en corto, tampoco cita RF-CAL-18.
     */
    @Test
    public void h2b_lineaConformidadDeBEnCortoCitaRFCOV12() throws Exception {
        Escenario e = escenario();
        e.xPatron.put("P49", e.campana.elegida("P49").media() + 7);   // arreglo2 (Cov363): la b se calibra igual
        FlujoCalibracion f = flujo(e, true);
        String resumen = f.calibrarAutomatico("Prueba H-2b");
        assertTrue("la b queda calibrada (VERIF-5-10, sin s_rep): " + resumen,
                resumen.contains("Calibrado: ") && resumen.contains(Fabrica.nombreCorto('b')));
        // El diario de la última acta cerrada (la del b): solo las líneas CONFORMIDAD/ESCRIBIENDO/ESCRITO
        // que RF-COV-20 nombra (la línea DATO,decisiones es el volcado íntegro de decisiones.csv, que SÍ
        // lista REMEDIDA-b = RF-CAL-18 por transparencia: eso no es "citar la regla con la que se juzgó").
        String diario = e.almacen.diariosCerrados.get(e.almacen.diariosCerrados.size() - 1);
        StringBuilder lineasDeJuicio = new StringBuilder();
        for (String linea : diario.split("\n")) {
            if (linea.startsWith("CONFORMIDAD,") || linea.startsWith("ESCRIBIENDO,") || linea.startsWith("ESCRITO,")) {
                lineasDeJuicio.append(linea).append('\n');
            }
        }
        String juicio = lineasDeJuicio.toString();
        assertFalse("hay al menos una línea CONFORMIDAD/ESCRIBIENDO/ESCRITO: " + diario, juicio.isEmpty());
        assertTrue("citan RF-COV-12 en corto: " + juicio, juicio.contains("RF-COV-12"));
        assertFalse("EN ROJO contra ed7e604: citaban RF-CAL-18 también en corto: " + juicio,
                juicio.contains("RF-CAL-18"));
    }
}
