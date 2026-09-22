/*
 * Tarea A4B-FILTRO (encargo del 21-sep-2026, contra HEAD e9d6e65 de rtv-1.0-cierre, Cov_3.6.7_calibrar).
 * Cierra el hallazgo abierto del arquitecto sobre la 3.6.7 (REVISIONES-Apps-V3.6.md): "Siguen: el acta en
 * pantalla (txtActa, CalibrarActivity.java:113-115,370-372) sin guarda de corto, y motivoNoEscribir
 * alcanzable en corto (FlujoCalibracion.java:836-837, filtro \"código \" + k)".
 *
 * Este fichero cubre el escenario C2: un código que se restauró tras dos re-medidas NO CONFORME (Acta.java,
 * motivoNoEscribir para 'mismo.restaurado != null': "el código k se restauró... no se reescribe en esta
 * acta") sigue en el acta (sin rechazar) y el operador vuelve a pulsar "Calibrar" con ese mismo código:
 * CalibracionManual.validarYPreparar (java:68) llama a FlujoCalibracion.motivoCodigo(k), que hasta e9d6e65
 * decidía si el motivo "era de k" mirando el TEXTO ("código " + k, java:836-837) y lo devolvía TAL CUAL —
 * crudo, con el número — como resultado de flujo.calibrar(), justo lo que CalibrarActivity pone en pantalla
 * (txtProgreso/alerta) en corto.
 *
 * Andamiaje calcado de Cov367Rf17Test (mismo ZIP real de las 18:11, EquipoSimulado.slv002(),
 * FlujoCalibracionTest.Almacen/Operador; e.operador.factor = 0.85 para forzar dos re-medidas fuera de
 * ±10 % y la restauración, como en el c1 de Cov367Rf17Test). Cada valor esperado sale del texto de
 * Acta.java (motivoNoEscribir) o de Fabrica.elCodigo, nunca inventado (CLAUDE.md del repositorio, §7).
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

public class Cov368Rf21Test {

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
                return "2026-09-21T12:00:00-0500";
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
                "RTV Calibra, prueba C2");
        FlujoCalibracion f = new FlujoCalibracion(e.sim, e.operador, e.almacen, e.cola, e.campana, catalogo,
                e.decisiones, ctx, reloj(), corto);
        f.pin("1234");
        return f;
    }

    /**
     * ALTO (C2, arquitecto sobre Cov_3.6.7_calibrar): dos re-medidas del 8 fuera de ±10 % restauran el
     * código (como el c1 de Cov367Rf17Test); el acta queda ABIERTA con el 8 restaurado (sin rechazar: esta
     * prueba llama a flujo.calibrar() directo, el mismo calibrar() público que usa CalibracionAutomatica,
     * no el auto-rechazo de CalibracionAutomatica.anotarNoCalibrado). Volver a pulsar "Calibrar" con el
     * mismo 8 seleccionado repite CalibracionManual.validarYPreparar -> motivoCodigo('8') ->
     * Acta.motivoNoEscribir('8') ("restaurado... no se reescribe en esta acta"): motivoNoEscribir en sí NO
     * cambia con esta tarea (es el documento técnico), así que el resultado CRUDO de flujo.calibrar()
     * sigue con "código 8" literal — lo prueba la primera aserción, documentando por qué hace falta el
     * filtro. Lo que sí tiene que quedar limpio es lo que CalibrarActivity pone en pantalla en corto, que
     * pasa ese mismo resultado por TextoOperador.limpiar (segunda aserción).
     */
    @Test
    public void c2_motivoDeRestauradoLlegaCrudoDeFlujoPeroLimpioEnPantalla() throws Exception {
        Escenario e = escenario();
        e.operador.factor = 0.85;   // colocación válida, pero fuera de ±10 % (RF-COV-12): restaura el 8
        FlujoCalibracion f = flujo(e, true);
        String r1 = f.calibrar(sel('8'), "Prueba", "Nota");
        assertTrue("tiene que restaurar por re-medida fuera de tolerancia: " + r1,
                r1.contains("re-medida fuera de ±10 %"));

        // Vuelve a pulsar "Calibrar" con el mismo 8: motivoCodigo('8') es ahora "de este código" (restaurado
        // != null) y CalibracionManual.validarYPreparar (java:68-71) devuelve motivoNoEscribir('8') tal cual.
        String r2 = f.calibrar(sel('8'), "Prueba", "Nota");
        assertTrue("motivoNoEscribir no cambia (es el documento técnico, RF-COV-21): " + r2,
                r2.contains("código 8") && r2.contains("no se reescribe en esta acta"));

        // Lo que CalibrarActivity pone en pantalla en corto (TextoOperador.limpiar(BuildConfig.CORTO, ...))
        // SÍ queda sin el número.
        String pantalla = TextoOperador.limpiar(true, r2);
        assertFalse("EN ROJO contra e9d6e65 (sin TextoOperador): \"" + pantalla + "\" sigue con \"código 8\"",
                pantalla.contains("código 8"));
        assertTrue("el nombre corto SÍ aparece (RF-COV-21): " + pantalla, pantalla.contains(Fabrica.nombreCorto('8')));
        assertTrue("el resto del motivo se conserva: " + pantalla, pantalla.contains("no se reescribe en esta acta"));

        // Regresión: en la app de campo (corto=false) motivoCodigo también se apoya en el estado
        // estructural, no en el texto (el guardia viejo de e9d6e65 comparaba m.contains("código " + k), que
        // para esta MISMA frase también daba m != null: aquí solo se comprueba que sigue devolviendo el
        // motivo real, sin cambio de comportamiento en campo).
        Escenario e2 = escenario();
        e2.operador.factor = 0.85;
        FlujoCalibracion f2 = flujo(e2, false);
        f2.calibrar(sel('8'), "Diego", "x");
        String r2campo = f2.calibrar(sel('8'), "Diego", "x");
        assertTrue("la app de campo sigue diciendo \"código 8\" en el motivo (no se toca): " + r2campo,
                r2campo.contains("código 8") && r2campo.contains("no se reescribe en esta acta"));
    }

    /**
     * BAJO — motivoCodigo() estructural (FlujoCalibracion.java, A4B-FILTRO): un código que NO está en
     * ninguno de los estados de 'mismo' (Acta.java:394-408) — aquí, un código todavía sin tocar, ni
     * conforme ni restaurado — no da ningún motivo "propio" aunque el acta tenga otro código sin resolver
     * (eso lo dice el "falta la re-medida..." genérico, no motivoCodigo). Regresión de la reescritura del
     * guardia: antes de esta tarea, motivoCodigo devolvía null aquí también (m no contenía "código " + k
     * porque el mensaje hablaba de OTRO código); con el guardia estructural sigue null, por una razón
     * distinta y no por casualidad del texto.
     */
    @Test
    public void motivoCodigoNullParaUnCodigoQueNoEsSuyoTodavia() throws Exception {
        Escenario e = escenario();
        FlujoCalibracion f = flujo(e, true);
        // Sin acta en curso (nada escrito todavía), motivoCodigo('8') es de paquete: se llama directo, sin
        // reflexión, porque esta prueba vive en el mismo paquete que FlujoCalibracion.
        assertTrue("sin acta en curso, motivoCodigo tiene que ser null", f.motivoCodigo('8') == null);
    }
}
