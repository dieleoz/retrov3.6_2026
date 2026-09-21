/*
 * Pruebas de los arreglos a los tres Altos, y a M-1/B-2, de la revision arquitecto-iot que dio NO APTO al
 * APK "Cov_3.6.1_calibrar" (05_Documentacion/SPEC-App-Calibracion-Coviandina.md §8, RF-COV-11 a RF-COV-17,
 * commit b2d547d de main). Contra HEAD 9435f69 de rtv-1.0-cierre, antes de este arreglo.
 *
 * De donde sale cada valor esperado (CLAUDE.md del repositorio, §7):
 *  - H-2 (RF-COV-12): 06_Calibracion/SLV-002/INFORME-Ajuste-SLV-002-20260919-1811.md, tabla §2.1 (s_rep de
 *    la sesion 1, 0,149 %) y §4.1 (patron P43, certificado 122, x del banco 1133,90, curva anclada real
 *    c1 = 2.11652640E-01 / c0 = -1.20446226E+02); §6 ("el mismo patron se mueve 7 cuentas entre dos series
 *    del mismo cuarto de hora"). Nada de esto lo hemos medido nosotros: es del ZIP archivado.
 *  - H-3 (RF-COV-13): el propio codigo de FlujoCalibracion.aceptar() (ya releia #GC# antes de este encargo:
 *    ver el informe final).
 *  - M-1 (RF-COV-14): TablaCalibracion.java (NO_REESCRIBIR de 1 y 2; requiereAceptado de la b).
 *  - El ZIP real: soporte_SLV-002_20260919_181120.zip (md5 79b23590c3a6b1877d55b5c4818b93ce, HUELLAS.txt
 *    de 06_Calibracion/SLV-002/, comprobado con md5sum antes de copiarlo a este arbol) y el mismo informe,
 *    §4.1 y §4.2, para los coeficientes del 8 y del b (tambien recalculados de forma independiente por el
 *    arquitecto).
 */
package com.dpi.retrov36;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
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

public class Cov362DefectosTest {

    private static final String MAC = "00:21:13:05:19:3B";
    private static final String ZIP_1811 = "../../../06_Calibracion/SLV-002/campanas/soporte_SLV-002_20260919_181120.zip";
    private static final String MD5_ZIP_1811 = "79b23590c3a6b1877d55b5c4818b93ce";

    private static List<Patron> catalogo;

    @BeforeClass
    public static void una() throws Exception {
        try (InputStreamReader r = new InputStreamReader(
                new FileInputStream("src/main/assets/patrones_certificados_P1-P132.csv"), StandardCharsets.UTF_8)) {
            catalogo = Patron.leer(r);
        }
    }

    // ==================================================================== H-2 (RF-COV-12)

    private static List<double[]> rep(double v) {
        List<double[]> l = new ArrayList<>();
        for (int i = 0; i < Remedida3611.K; i++) {
            double[] m = new double[Remedida3611.M];
            java.util.Arrays.fill(m, v);
            l.add(m);
        }
        return l;
    }

    /**
     * H-2 — ASEVERA UN REQUISITO. EN ROJO contra 9435f69, porque en esa version Remedida3611 no tiene
     * evaluarCertificado y FlujoCalibracion.remedida() no tenia por donde elegirlo: no compilaba.
     *
     * Con los datos reales de P43 (informe §2.1 y §4.1): xBanco = 1133,90, s_rep de su sesion = 0,149 %,
     * certificado 122. Una re-medida con la R que da la curva real ya escrita (c1/c0 del §4.1) en
     * x = 1140,90 (banco + 7 cuentas, la reproducibilidad real citada en el informe §6) es CONFORME al
     * certificado (dentro del 10 %) pero el criterio de s_rep (limite ±2,14 cuentas) la declara NO
     * CONFORME solo por el desplazamiento de x. Salida real, en rojo, de esta prueba contra 9435f69
     * (Remedida3611 no compila con evaluarCertificado, asi que la salida es de compilacion):
     *   Cov362DefectosTest.java:XX: error: cannot find symbol
     *     Remedida3611.Resultado nuevo = Remedida3611.evaluarCertificado(...)
     * y, si solo se comprueba viejo.estado (posible sin tocar Remedida3611), sale en verde por accidente:
     * por eso la aserción clave es el contraste de las dos, no una sola.
     */
    @Test
    public void h2_elCriterioDeSRepRechazaUnaRemedidaBuenaPorLaVariabilidadRealDeP43() {
        double xBanco = 1133.90;
        double sRepRel = 0.00149;   // s_rep sesión 1 (informe §2.1): 0,149 %
        double cert = 122;          // certificado de P43
        // Curva anclada real del código 8 (informe §4.1): c1 = 2.11652640E-01, c0 = -1.20446226E+02.
        double xRem = xBanco + 7;   // "el mismo patrón se mueve 7 cuentas entre dos series..." (informe §6)
        double rMedida = 2.11652640E-01 * xRem + -1.20446226E+02;   // R que da la curva real en x = 1140,90

        Remedida3611.Resultado viejo = Remedida3611.evaluar("P43", cert, rep(xRem), rep(rMedida), xBanco,
                Remedida3611.K, sRepRel, "s_rep sesión 1: 0,149 %");
        assertEquals("el criterio de s_rep (Remedida3611.java:65-66) declara NO_CONFORME una re-medida cuya R "
                + "está a " + String.format(java.util.Locale.US, "%.1f", 100 * (rMedida - cert) / cert)
                + " %% del certificado, solo porque x se movió 7 cuentas (límite ±2,14 con s_rep = 0,149 % y "
                + "K = 5): " + viejo.texto, "NO_CONFORME", viejo.estado);

        Remedida3611.Resultado nuevo = Remedida3611.evaluarCertificado("P43", cert, rep(xRem), rep(rMedida));
        assertEquals("RF-COV-12: el mismo dato, juzgado solo frente al certificado (±10 %), es CONFORME: "
                + nuevo.texto, "CONFORME", nuevo.estado);
    }

    /** Contrapunto: si la R sí se sale del ±10 % del certificado, evaluarCertificado también lo rechaza. */
    @Test
    public void h2_evaluarCertificadoSiRechazaUnaRSueltaDelCertificado() {
        Remedida3611.Resultado r = Remedida3611.evaluarCertificado("P43", 122, rep(1140.90), rep(140.0));
        assertEquals("140 frente a 122 son +14,8 %, fuera del ±10 % de RF-COV-12: " + r.texto, "NO_CONFORME", r.estado);
    }

    // ==================================================================== M-1 (RF-COV-14)

    /**
     * M-1 — ASEVERA UN REQUISITO. Con el banco del código 1 completo (todos sus pasos AJUSTE/RE-MEDIDA
     * HECHO) y su OSCURO de sesión, las dos puertas de antes de este arreglo (BancoCola.calibrable y
     * Anclas.oscuro) lo daban por "listo" — EN ROJO contra 9435f69: las dos puertas solas, calculadas
     * aquí igual que hacía AppCorta.codigos antes del arreglo, sí lo dan por listo.
     */
    @Test
    public void m1_lasDosPuertasSolasDabanElCodigo1PorListoAunqueNoSeReescribe() throws Exception {
        BancoCola q = cola(BancoCola.Tipo.COMPLETO);
        Campana c = vacia();
        completarBancoDeUnCodigo(q, c, '1');
        boolean puerta1 = q.calibrable("1", c.pasos());
        boolean puerta2 = puerta1 && !Double.isNaN(Anclas.oscuro(q, c, '1').valor);
        assertTrue("EN ROJO contra 9435f69: las puertas de antes (banco + oscuro, sin TablaCalibracion) "
                + "dan el código 1 por listo aunque nunca se reescribe (TablaCalibracion.java:171)", puerta2);

        Decisiones decisiones = Decisiones.leer(decisionesDelApk());
        List<AppCorta.Codigo> l = AppCorta.codigos(q, c, decisiones, "SLV-002", java.util.Collections.<Character>emptySet());
        AppCorta.Codigo uno = buscar(l, '1');
        assertFalse("RF-COV-14: con TablaCalibracion, el código 1 (NO_REESCRIBIR) no sale listo: " + uno.motivo,
                uno.listo);
        assertTrue("y dice por qué (no se reescribe): " + uno.motivo, uno.motivo.contains("no se reescribe"));
    }

    /** M-1, el caso de la b: banco completo pero el 8 sin acta ACEPTADA todavía. */
    @Test
    public void m1_laBNoSaleListaSinElActaAceptadaDelOcho() throws Exception {
        BancoCola q = cola(BancoCola.Tipo.REPRESENTATIVO);
        Campana c = vacia();
        completarBancoDeUnCodigo(q, c, 'b');
        Decisiones decisiones = Decisiones.leer(decisionesDelApk());
        AppCorta.Codigo b = buscar(AppCorta.codigos(q, c, decisiones, "SLV-002", java.util.Collections.<Character>emptySet()), 'b');
        assertFalse("RF-COV-14: la b no sale lista hasta que el 8 tenga acta ACEPTADA: " + b.motivo, b.listo);
        assertTrue(b.motivo, b.motivo.contains("código 8"));

        Set<Character> conElOchoAceptado = new HashSet<>();
        conElOchoAceptado.add('8');
        AppCorta.Codigo b2 = buscar(AppCorta.codigos(q, c, decisiones, "SLV-002", conElOchoAceptado), 'b');
        assertTrue("y sale lista en cuanto el 8 queda aceptado: " + b2.motivo, b2.listo);
    }

    /** Los códigos que la cola no puede ni empezar (banco a medias) siguen dando exactamente el mismo motivo. */
    @Test
    public void m1_noCambiaElMotivoDeLosCodigosQueYaEstabanBloqueadosPorElBanco() throws Exception {
        Campana c = vacia();
        ImportadorCampana.importarDiario(c, diarioDe(ZIP_1811), catalogo, "zip 18:11 sin BancoPrevio");
        BancoCola q = cola(BancoCola.Tipo.REPRESENTATIVO);
        AppCorta.Codigo siete = buscar(AppCorta.codigos(q, c), '7');
        assertEquals("7 es sólo-verificar y ya lo decía así antes de este arreglo (no lo toca TablaCalibracion, "
                + "que también lo tiene como SOLO_VERIFICAR)", "esta cola solo lo verifica: no se ajusta", siete.motivo);
    }

    // ==================================================================== H-3 (RF-COV-13)

    /**
     * H-3 — confirma el comportamiento, SIN roja previa: al abrir el código en 9435f69
     * (FlujoCalibracion.java:1504-1521) la relectura de #GC# tras #SC YA estaba, desde el commit 5be257a
     * (anterior a este encargo; "git log -p -L 1500,1533" lo confirma). La revisión arquitecto-iot señaló
     * esta zona como Alto y la SPEC (RF-COV-13) dice "cierra H-3"; el código no cambia aquí. Se deja la
     * prueba en verde, sin roja, y se dice: no se fabrica una regresión que nunca existió.
     */
    @Test
    public void h3_siSCNoGrabaLaFechaDeVerdadElActaNoQuedaAceptada() throws Exception {
        Campana c = vacia();
        ImportadorCampana.importarDiario(c, diarioDe(ZIP_1811), catalogo, "zip 18:11");
        BancoCola q = cola(BancoCola.Tipo.de(c.colaTipo()));
        BancoPrevio.aplicar(c, q, BancoPrevio.Grupos.leer(asset("grupos_patrones_equivalentes.csv")),
                Decisiones.leer(decisionesDelApk()), "SLV-002", "t");
        Map<String, Double> xPatron = new HashMap<>();
        xPatron.put("P43", c.elegida("P43").media());
        EquipoSimulado sim = EquipoSimulado.slv002();
        FlujoCalibracionTest.Almacen almacen = new FlujoCalibracionTest.Almacen();
        FlujoCalibracionTest.Operador operador = new FlujoCalibracionTest.Operador(sim, xPatron);
        FlujoCalibracion.Contexto ctx = FlujoCalibracion.identificar(sim, "SLV-002", MAC, Boolean.TRUE, "6/6", "prueba");
        FlujoCalibracion.Reloj reloj = new FlujoCalibracion.Reloj() {
            @Override
            public String ahoraIso() {
                return "2026-09-21T10:00:00-0500";
            }

            @Override
            public String hoy() {
                return "2026-09-21";
            }
        };
        FlujoCalibracion f = new FlujoCalibracion(sim, operador, almacen, q, c, catalogo,
                Decisiones.leer(decisionesDelApk()), ctx, reloj);
        f.pin("1234");
        Set<Character> ocho = new HashSet<>();
        ocho.add('8');
        String rEscribir = f.calibrar(ocho, "Prueba", "Nota");
        assertTrue("el 8 queda escrito y re-medido, listo para persistencia: " + rEscribir,
                rEscribir.contains("Ahora: persistencia"));
        String rPer = f.persistencia();
        assertTrue("Persistencia OK: " + rPer, rPer.startsWith("Persistencia OK"));

        // H-3: el equipo responde #OK# a #SC pero no lo graba de verdad (firmware que no llega a escribir la
        // EEPROM). EquipoSimulado.Falla.OK_SIN_HACER simula exactamente eso (EquipoSimulado.java:341-343:
        // marca la trama y responde OK sin pasar por responder(t), que es donde de verdad cambiaría "fecha").
        sim.inyectar("#SC", EquipoSimulado.Falla.OK_SIN_HACER, 1);
        String rAceptar = f.aceptar();
        assertFalse("confirma H-3 (FlujoCalibracion.java:1504-1521, ya presente en 9435f69 desde 5be257a, "
                + "anterior a este encargo): si aceptar() no releyera #GC# tras #SC, esto daría el acta por "
                + "ACEPTADA con la fecha vieja en el equipo: " + rAceptar, rAceptar.startsWith("Acta ACEPTADA"));
        assertTrue("y lo dice: " + rAceptar, rAceptar.contains("no quedó grabada"));
    }

    // ==================================================================== B-2

    /**
     * B-2 — ASEVERA UN REQUISITO. El mensaje de "ZIP de otra familia" (mismo caso que
     * AppCortaTest.k_unZipDeOtraFamiliaQueElEquipoConectadoNoEntra) ya no remite a "la app de campo", que
     * en Coviandina no está instalada.
     */
    @Test
    public void b2_elMensajeDeOtraFamiliaNoRemiteALaAppDeCampo() throws Exception {
        String serie = "SLV-003-2026";
        String mac = "00:22:09:01:65:10";
        StringWriter antes = new StringWriter();
        Campana v4 = new Campana(catalogo, serie, mac);
        v4.escribirEn(antes);
        Campana.Serie s = v4.nuevaSerie("2026-09-19T18:00:00-0500", serie, mac, "V4 original (V4.1)", "P20", 0, '2');
        v4.agregarDisparo(s, 1, "t", "@LEERV,716@", Double.NaN);
        v4.cerrar(s, "OK", true, "");
        Campana c = new Campana(catalogo, serie, mac);
        c.escribirEn(new StringWriter());
        try {
            AppCorta.importar(c, antes.toString(), catalogo, "ZIP de antes de grabar", new ProtocoloV46().firmware().name());
            org.junit.Assert.fail("tenía que rechazar el ZIP de otra familia");
        } catch (IllegalArgumentException e) {
            assertFalse("EN ROJO contra 9435f69: el mensaje decía \"(en la app de campo, RTV)\", que ya no está "
                    + "instalada en Coviandina: " + e.getMessage(), e.getMessage().contains("app de campo"));
            assertTrue("y sigue diciendo la salida real (RF-COV-09): " + e.getMessage(),
                    e.getMessage().contains("abra una campaña aparte para la etapa anterior"));
        }
    }

    // ==================================================================== el ZIP real: el 8 y la b

    /**
     * El ZIP real — ASEVERA LOS REQUISITOS RF-COV-12 y RF-COV-17. Contra EquipoSimulado, con el camino
     * CORTO (FlujoCalibracion construido con corto = true): un solo calibrarAutomatico() dado por el
     * operador escribe y ACEPTA el 8 y, en la misma pulsación, la b (en TablaCalibracion.java:196 la b va
     * tras el 8). Los coeficientes escritos se comprueban contra el informe (§4.1 y §4.2, y el recálculo
     * independiente del arquitecto): 8 → c1 = 2.11652640E-01, c0 = -1.20446226E+02; b → c1 = 3.20071157E-01,
     * c0 = -1.82592593E+02.
     *
     * El ZIP md5 79b23590c3a6b1877d55b5c4818b93ce, copiado desde el árbol principal
     * (06_Calibracion/SLV-002/campanas/, comprobado con md5sum) porque no existía en este árbol.
     */
    @Test
    public void zip1811_calibrarUnaVezDejaElOchoYLaBEscritosYAceptados() throws Exception {
        assertEquals("el ZIP de partida es el archivado, no otro (HUELLAS.txt)", MD5_ZIP_1811,
                Resumen.hex(Files.readAllBytes(new File(ZIP_1811).toPath()), "MD5"));
        String diario = diarioDe(ZIP_1811);

        Campana c = vacia();
        ImportadorCampana.importarDiario(c, diario, catalogo, "zip 18:11");
        BancoCola q = cola(BancoCola.Tipo.de(c.colaTipo()));
        assertEquals("el banco de este ZIP es el representativo (informe: banco REPRESENTATIVO)",
                "REPRESENTATIVO", c.colaTipo());
        BancoPrevio.aplicar(c, q, BancoPrevio.Grupos.leer(asset("grupos_patrones_equivalentes.csv")),
                Decisiones.leer(decisionesDelApk()), "SLV-002", "t");

        Decisiones decisiones = Decisiones.leer(decisionesDelApk());
        Map<String, Double> xPatron = new HashMap<>();
        xPatron.put("P43", c.elegida("P43").media());
        xPatron.put("P49", c.elegida("P49").media());

        EquipoSimulado sim = EquipoSimulado.slv002();
        FlujoCalibracionTest.Almacen almacen = new FlujoCalibracionTest.Almacen();
        FlujoCalibracionTest.Operador operador = new FlujoCalibracionTest.Operador(sim, xPatron);
        FlujoCalibracion.Contexto ctx = FlujoCalibracion.identificar(sim, "SLV-002", MAC, Boolean.TRUE, "6/6",
                "RTV Calibra, prueba");
        FlujoCalibracion.Reloj reloj = new FlujoCalibracion.Reloj() {
            @Override
            public String ahoraIso() {
                return "2026-09-21T10:00:00-0500";
            }

            @Override
            public String hoy() {
                return "2026-09-21";
            }
        };
        // corto = true (H-2/RF-COV-12): SOLO así se juzga la re-medida frente al certificado y no frente al
        // s_rep, que con este banco real (s_rep 0,149-0,314 %) rechazaría el 8 y la b (ver h2_ arriba).
        FlujoCalibracion f = new FlujoCalibracion(sim, operador, almacen, q, c, catalogo, decisiones, ctx, reloj, true);
        f.pin("1234");

        String resumen = f.calibrarAutomatico("Prueba RF-COV-17");
        assertTrue("RF-COV-17: un solo Calibrar deja el 8 calibrado: " + resumen,
                resumen.contains(Fabrica.nombre('8')));
        assertTrue("y, en la misma pulsación, la b (después del 8): " + resumen,
                resumen.contains(Fabrica.nombre('b')));

        Ecuacion ocho = sim.curvas.get('8');
        assertEquals("c1 del código 8 (informe §4.1, y recálculo independiente del arquitecto)",
                2.11652640E-01, ocho.c1, 5e-4);
        assertEquals("c0 del código 8 (informe §4.1)", -1.20446226E+02, ocho.c0, 5e-2);

        Ecuacion b = sim.curvas.get('b');
        assertEquals("c1 del código b (informe §4.2, y recálculo independiente del arquitecto)",
                3.20071157E-01, b.c1, 5e-4);
        assertEquals("c0 del código b (informe §4.2)", -1.82592593E+02, b.c0, 5e-2);
    }

    // ==================================================================== andamiaje

    private static String decisionesDelApk() throws IOException {
        return new String(Files.readAllBytes(new File("src/main/assets/decisiones.csv").toPath()), StandardCharsets.UTF_8);
    }

    private static String asset(String n) throws IOException {
        return new String(Files.readAllBytes(new File("src/main/assets/" + n).toPath()), StandardCharsets.UTF_8);
    }

    private static BancoCola cola(BancoCola.Tipo t) throws IOException {
        return BancoCola.cargar(Files.readAllBytes(new File("src/main/assets/" + t.asset).toPath()));
    }

    private static Campana vacia() throws IOException {
        Campana c = new Campana(catalogo, "SLV-002", MAC);
        c.escribirEn(new StringWriter());
        return c;
    }

    private static String diarioDe(String zipPath) throws IOException {
        byte[] zip = Files.readAllBytes(new File(zipPath).toPath());
        return ImportadorCampana.diarioDeZip(zip);
    }

    private static AppCorta.Codigo buscar(List<AppCorta.Codigo> l, char k) {
        for (AppCorta.Codigo x : l) {
            if (x.k == k) {
                return x;
            }
        }
        throw new IllegalStateException("código " + k + " no está en la lista");
    }

    /** El banco mínimo de un código (sus AJUSTE/RE-MEDIDA, más el OSCURO de su sesión), como AppCortaTest.c2_. */
    private static void completarBancoDeUnCodigo(BancoCola q, Campana c, char k) throws IOException {
        int sesion = Anclas.sesionDe(q, k);
        for (BancoCola.Paso p : q.pasos) {
            boolean oscuroDeSuSesion = "OSCURO".equals(p.tipo) && p.sesion == sesion;
            boolean delCodigo = "PATRON".equals(p.tipo) && String.valueOf(k).equals(p.codigo)
                    && ("AJUSTE".equals(p.uso) || "RE-MEDIDA".equals(p.uso));
            if (oscuroDeSuSesion || delCodigo) {
                hecho(c, p, oscuroDeSuSesion ? 565 : 1500);
            }
        }
    }

    private static void hecho(Campana c, BancoCola.Paso p, double x) throws IOException {
        String nombre = "OSCURO".equals(p.tipo) ? Campana.OSCURO.nombre : p.patron;
        Campana.Serie s = c.nuevaSerie("2026-09-21T09:00:00-0500", "SLV-002", MAC, "V3.6", nombre, 0, 'e');
        for (int k = 1; k <= 5; k++) {
            for (int i = 0; i < 4; i++) {
                double v = x + (i % 2 == 0 ? 0.5 : -0.5);
                c.agregarDisparo(s, k, "t", "::" + Math.round(v), v);
            }
        }
        c.cerrar(s, "OK", true, "");
        c.elegir(s);
        c.anotarPaso(p.orden, "HECHO", s.id, "t", "");
    }
}
