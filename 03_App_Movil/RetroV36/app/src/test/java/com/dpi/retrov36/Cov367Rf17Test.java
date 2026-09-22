/*
 * Tarea A4b, revision 2 (encargo del 21-sep-2026, contra HEAD 4ea680c de rtv-1.0-cierre, Cov_3.6.6_calibrar).
 * El arquitecto-iot dio NO APTO a la 3.6.6: RF-COV-17/21 (SPEC-App-Calibracion-Coviandina.md, de main,
 * :224-225 y :271: "el operador... nunca ve el numero de codigo"; "Ningun texto que vea el operador lleva
 * el numero de codigo") seguian llegando al operador de RTV Calibra por cuatro caminos verificados por el
 * arquitecto abriendo el fichero:
 *
 *  - C1. Ops.java:237 formaba "restauracion con #F,%c# ... relectura #G,%c" CON el numero, sin mirar
 *    `corto`; llega por restaurarCodigo() (FlujoCalibracion.java, invocado desde remedida()/escribirYVerificar
 *    con dos re-medidas fuera de tolerancia) y por escribir() (la restauracion tras un #S fallido).
 *  - C2. FlujoCalibracion.escribir(): `t = "#S," + k + ...` (la trama cruda) se embebia en el "return" de
 *    operador tanto si la restauracion posterior salia bien como si no.
 *  - C3. cotejarHeredados() ("el codigo k no coincide con...") y motivoMascara() ("no tiene el codigo %c")
 *    llegan por heredados() (T-C41), que corre SIEMPRE al empezar a escribir un codigo (tambien en corto,
 *    calibrar() -> CalibracionManual.escribirYVerificar() -> heredados()) — aqui probado calibrando la b
 *    justo despues de aceptar el 8 (el 8 pasa a ser "heredado" de la b, P14-02).
 *  - C4. remedida(): "Falta la serie del banco de <patron> (re-medida del k)" cuando la campana no tiene
 *    serie elegida para el patron de re-medida de un codigo que se escribe.
 *
 * Cierre (arquitecto): Ops.Restauracion lleva ahora dos textos — `texto` (la trama cruda, para el acta y
 * los registros, sin cambio) y `resumen` (lo que puede ver el operador; en corto, sin numero de codigo ni
 * trama, via Fabrica.elCodigo). Los cuatro sitios de FlujoCalibracion.java y los dos de CalibracionManual.java
 * (barrido propio: "El codigo " + k + " ya tiene un acta ACEPTADA vigente"/"no se escribe", alcanzables
 * desde el mismo calibrar() que usa CalibracionAutomatica) se convierten con Fabrica.elCodigo(k, corto).
 * FlujoCalibracion.java no crecio: 2003 lineas antes y despues (wc -l).
 *
 * Andamiaje calcado de Cov365Rf17Test/Cov366Rf17Test (mismo ZIP real de las 18:11, EquipoSimulado.slv002(),
 * FlujoCalibracionTest.Almacen/Operador). Cada valor esperado sale del texto literal de RF-COV-17/21 o del
 * propio codigo (Fabrica.nombreCorto), nunca inventado (CLAUDE.md del repositorio, §7).
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
import java.util.regex.Pattern;

public class Cov367Rf17Test {

    private static final String MAC = "00:21:13:05:19:3B";
    private static final String ZIP_1811 = "../../../06_Calibracion/SLV-002/campanas/soporte_SLV-002_20260919_181120.zip";
    /** Ningun texto en corto lleva "#" ni "," seguido del caracter del codigo (RF-COV-17/21 literal). */
    private static final Pattern SIN_NUMERO_8 = Pattern.compile("[#,]8\\b");

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
                return "2026-09-21T11:00:00-0500";
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
                "RTV Calibra, prueba A4b-2");
        FlujoCalibracion f = new FlujoCalibracion(e.sim, e.operador, e.almacen, e.cola, e.campana, catalogo,
                e.decisiones, ctx, reloj(), corto);
        f.pin("1234");
        return f;
    }

    // ============================================================ C1: restaurar() (Ops.java:237)

    /**
     * ALTO (C1): dos re-medidas del 8 fuera de +-10 % (RF-COV-12) fuerzan restaurarCodigo(), que devuelve
     * el resumen de Ops.Restauracion tal cual (FlujoCalibracion.java, "restaurado (" + motivo + "): " +
     * r.texto). Antes del cierre, r.texto SIEMPRE era la trama cruda ("restauracion con #F,8# -> ...;
     * relectura #G,8 ..."): con el numero de codigo, mirara o no `corto`.
     *
     * EN ROJO contra 4ea680c: Ops.restaurar(char,Ecuacion) no existia con un tercer parametro `corto`; su
     * unico texto (Restauracion.texto) era siempre la trama cruda.
     */
    @Test
    public void c1_restaurarCodigoEnCortoNoMuestraLaTramaNiElNumero() throws Exception {
        Escenario e = escenario();
        e.operador.factor = 0.85;   // colocacion valida, pero fuera de +-10 % (QA-3615-06 / RF-COV-12)
        FlujoCalibracion f = flujo(e, true);
        String r = f.calibrar(sel('8'), "Prueba", "Nota");
        assertTrue("tiene que restaurar por re-medida fuera de tolerancia: " + r, r.contains("re-medida fuera de ±10 %"));
        assertFalse("EN ROJO contra 4ea680c, restaurarCodigo() en corto decía \"código 8\": " + r, r.contains("código 8"));
        assertFalse("EN ROJO contra 4ea680c, restaurarCodigo() en corto llevaba la trama cruda: " + r,
                r.contains("restauración con") || r.contains("relectura #G"));
        assertFalse("ningún \"#8\"/\",8\" crudo: " + r, SIN_NUMERO_8.matcher(r).find());
        assertTrue("el nombre corto SÍ aparece (RF-COV-17): " + r, r.contains(Fabrica.nombreCorto('8')));

        // Regresión: en la app de campo, restaurarCodigo() sigue devolviendo la trama cruda con el número.
        Escenario e2 = escenario();
        e2.operador.factor = 0.85;
        FlujoCalibracion f2 = flujo(e2, false);
        String r2 = f2.calibrar(sel('8'), "Diego", "x");
        assertTrue("la app de campo sigue devolviendo la trama cruda de la restauración: " + r2,
                r2.contains("restauración con") && r2.contains("relectura #G,8"));
    }

    // ============================================================ C2: escribir() (FlujoCalibracion.java:1265,1269)

    /**
     * ALTO (C2): si el #S falla (ERR) y la restauración se verifica, escribir() devolvía "Escritura del
     * código 8 fallida: #S,8 -> ... . Restaurado." — con la trama cruda `t` (con el número) embebida en el
     * "return" del operador, mirara o no `corto` (la línea ya usaba Fabrica.elCodigo en otras partes del
     * mensaje, pero no en la `t` intercalada).
     *
     * EN ROJO contra 4ea680c: FlujoCalibracion.java:1265/1269 concatenaban `t` sin condicionar a `corto`.
     */
    @Test
    public void c2_escribirConFalloEnCortoNoMuestraLaTramaDelSFallido() throws Exception {
        Escenario e = escenario();
        e.sim.inyectar("#S,8", EquipoSimulado.Falla.ERR, 1);
        FlujoCalibracion f = flujo(e, true);
        String r = f.calibrar(sel('8'), "Prueba", "Nota");
        assertTrue("tiene que fallar la escritura y restaurar: " + r, r.contains("fallida") && r.contains("Restaurado"));
        assertFalse("EN ROJO contra 4ea680c, escribir() en corto decía \"código 8\": " + r, r.contains("código 8"));
        assertFalse("EN ROJO contra 4ea680c, escribir() en corto llevaba \"#S,8\" crudo: " + r, r.contains("#S,8"));
        assertFalse("ningún \"#8\"/\",8\" crudo: " + r, SIN_NUMERO_8.matcher(r).find());
        assertTrue("el nombre corto SÍ aparece (RF-COV-17): " + r, r.contains(Fabrica.nombreCorto('8')));

        // Regresión: en la app de campo, sigue devolviendo la trama cruda "#S,8 -> ...".
        Escenario e2 = escenario();
        e2.sim.inyectar("#S,8", EquipoSimulado.Falla.ERR, 1);
        FlujoCalibracion f2 = flujo(e2, false);
        String r2 = f2.calibrar(sel('8'), "Diego", "x");
        assertTrue("la app de campo sigue devolviendo \"#S,8\" crudo: " + r2, r2.contains("#S,8"));
    }

    // ============================================================ C3: cotejarHeredados()/motivoMascara() (T-C41)

    /**
     * ALTO (C3): al calibrar la b justo después de aceptar el 8 (el 8 pasa a ser heredado de la b,
     * P14-02), si el #G,8 de T-C41 no responde, cotejarHeredados() decía "el código 8 no coincide con...",
     * y ese motivo es justo lo que heredados() devuelve tal cual como "T-C41 FALLA: ...", sin mirar
     * `corto`. Alcanzable desde el mismo calibrar() que usa CalibracionAutomatica
     * (CalibracionManual.escribirYVerificar() -> heredados(), antes de escribir nada del código nuevo).
     *
     * EN ROJO contra 4ea680c: FlujoCalibracion.java:1104 formaba "el código " + k + " no coincide..." tal
     * cual.
     */
    @Test
    public void c3_heredadosEnCortoNoMuestraElNumeroDelCodigoHeredado() throws Exception {
        Escenario e = escenario();
        FlujoCalibracion f = flujo(e, true);
        String r8 = f.calibrar(sel('8'), "Prueba", "Nota 8");
        assertTrue(r8, r8.contains("Ahora: persistencia"));
        assertTrue(f.persistencia().startsWith("Persistencia OK"));
        assertTrue(f.aceptar().contains("ACEPTADA"));

        e.sim.inyectar("#G,8", EquipoSimulado.Falla.ERR, 1);
        FlujoCalibracion g = flujo(e, true);
        String rb = g.calibrar(sel('b'), "Prueba", "Nota b");
        assertTrue("tiene que fallar T-C41 por el heredado 8: " + rb, rb.contains("T-C41 FALLA"));
        assertFalse("EN ROJO contra 4ea680c, heredados() en corto decía \"código 8\": " + rb, rb.contains("código 8"));
        assertFalse("ningún \"#8\"/\",8\" crudo: " + rb, SIN_NUMERO_8.matcher(rb).find());
        assertTrue("el nombre corto SÍ aparece (RF-COV-17): " + rb, rb.contains(Fabrica.nombreCorto('8')));

        // Regresión: en la app de campo, T-C41 sigue diciendo "el código 8 no coincide...".
        Escenario e2 = escenario();
        FlujoCalibracion f2 = flujo(e2, false);
        String r82 = f2.calibrar(sel('8'), "Diego", "Nota 8 campo");
        assertTrue(r82, r82.contains("Ahora: persistencia"));
        assertTrue(f2.persistencia().startsWith("Persistencia OK"));
        assertTrue(f2.aceptar().contains("ACEPTADA"));
        e2.sim.inyectar("#G,8", EquipoSimulado.Falla.ERR, 1);
        FlujoCalibracion g2 = flujo(e2, false);
        String rb2 = g2.calibrar(sel('b'), "Diego", "Nota b campo");
        assertTrue("la app de campo sigue diciendo \"el código 8 no coincide\": " + rb2,
                rb2.contains("el código 8 no coincide"));
    }

    // ============================================================ C4: remedida() (FlujoCalibracion.java:1314)

    /**
     * ALTO (C4): sin ninguna serie ACEPTADA de P43 (el patrón de re-medida del 8; sus pasos del banco
     * siguen HECHO, Campana.java:557-565, así que ni cola.calibrable() ni protocoloIncumplido() paran
     * antes), remedida() decía "Falta la serie del banco de P43 (re-medida del código 8)." — con el
     * número, sin mirar
     * `corto`.
     *
     * EN ROJO contra 4ea680c: FlujoCalibracion.java:1314 concatenaba "(re-medida del " + k + ")." tal cual.
     */
    @Test
    public void c4_remedidaSinSerieDeBancoEnCortoNoMuestraElNumeroDeCodigo() throws Exception {
        Escenario e = escenario();
        // Los pasos siguen HECHO (Campana.pasos() solo mira `anulada`: Campana.java:557-565), pero ninguna
        // serie de P43 queda `aceptada`: campana.elegida("P43") no encuentra ninguna (Campana.java:870-882),
        // que es justo lo que remedida() necesita ver para "Falta la serie del banco...".
        for (Campana.Serie s : e.campana.seriesDe("P43")) {
            s.aceptada = false;
        }
        FlujoCalibracion f = flujo(e, true);
        String r = f.calibrar(sel('8'), "Prueba", "Nota");
        assertTrue("tiene que faltar la serie del banco de P43: " + r, r.contains("Falta la serie del banco de P43"));
        assertFalse("EN ROJO contra 4ea680c, remedida() en corto decía \"re-medida del 8\": " + r,
                r.contains("re-medida del 8"));
        assertFalse("ningún \"#8\"/\",8\" crudo: " + r, SIN_NUMERO_8.matcher(r).find());
        assertTrue("el nombre corto SÍ aparece (RF-COV-17): " + r, r.contains(Fabrica.nombreCorto('8')));

        // Regresión: en la app de campo, remedida() sigue diciendo "re-medida del código 8".
        Escenario e2 = escenario();
        for (Campana.Serie s : e2.campana.seriesDe("P43")) {
            s.aceptada = false;
        }
        FlujoCalibracion f2 = flujo(e2, false);
        String r2 = f2.calibrar(sel('8'), "Diego", "x");
        assertTrue("la app de campo sigue diciendo \"re-medida del código 8\": " + r2, r2.contains("re-medida del código 8"));
    }
}
