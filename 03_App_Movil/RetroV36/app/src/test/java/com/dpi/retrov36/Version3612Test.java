package com.dpi.retrov36;

import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * App 3.6.12, corte B (calibrar con un boton): D-18, D-19, D-20, RF-APP-36/37, RF-CAL-37, T-A54,
 * T-A63 y los hallazgos de P10 §9 (#F invalida, #SC fallida no cierra, reescribir el mismo codigo).
 */
public class Version3612Test {

    private static final String MAC = "00:21:13:05:19:3B";


    private static List<Patron> catalogo132() throws Exception {
        try (InputStreamReader r = new InputStreamReader(
                new FileInputStream("src/main/assets/patrones_certificados_P1-P132.csv"), StandardCharsets.UTF_8)) {
            return Patron.leer(r);
        }
    }

    private static BancoCola cola() throws Exception {
        return BancoCola.cargar(Files.readAllBytes(new File("src/main/assets/cola_banco_P1-P132.csv").toPath()));
    }

    private static Ecuacion curva(double c1, double c0) {
        return new Ecuacion(0, 0, c1, c0);
    }

    /** Acta con diario en memoria y el codigo k escrito (con su #S anotado antes). */
    private static Acta actaCon(StringWriter w, char k) throws Exception {
        Acta a = new Acta("SLV-002", MAC, "3.6.2", 5, 4, 1, "2026-09-19T10:00:00-0500");
        a.escribirEn(w);
        a.tabla = TablaCalibracion.VERSION;
        a.conformidad("Diego: código " + k + ". Prueba");
        Ecuacion ant = Fabrica.ecuacion(k);
        Ecuacion env = curva(0.25, -120);
        a.escribiendo(k, ant, env);
        a.escrito(k, Tramas.tramaG(k, env), env, "Oscuro (x = 575.0)", "recta anclada en el OSCURO", "");
        return a;
    }

    // ------------------------------------------------------------ RF-CAL-37

    @Test
    public void tablaRfCal37() {
        assertEquals(TablaCalibracion.Metodo.ANCLADA, TablaCalibracion.fila('8').metodo);
        // 3.6.13 (P11-M5): sin PA-24 decidida por Diego, el b solo se verifica.
        assertEquals(TablaCalibracion.Metodo.SOLO_VERIFICAR, TablaCalibracion.fila('b').metodo);
        assertFalse(TablaCalibracion.fila('b').escribible());
        assertEquals("P43", TablaCalibracion.fila('8').remedida);
        assertEquals("P49", TablaCalibracion.fila('b').remedida);
        assertFalse(TablaCalibracion.fila('1').escribible());
        assertFalse(TablaCalibracion.fila('2').escribible());
        for (char k : new char[]{'7', 'a', 'c', 'd'}) {
            assertFalse(String.valueOf(k), TablaCalibracion.fila(k).escribible());
        }
        assertEquals(TablaCalibracion.Metodo.GRADO1_O_ANCLADA, TablaCalibracion.fila('3').metodo);
        assertFalse(TablaCalibracion.fila('5').escribible());
        assertTrue(TablaCalibracion.fila('5').aviso.contains("sin decisión"));
        // 3.6.14: decisiones por equipo; el b ademas exige la regla de su re-medida (REMEDIDA-b, QA-3612-06).
        Decisiones d = Decisiones.leer("PA-14,SLV-002,SI,2026-09-20,Diego Zuniga,DECISION-PA-14.md\n"
                + "PA-24,SLV-002,SI,2026-09-20,Diego Zuniga,DECISION-PA-24.md,\"RF-CAL-14 P49 -10 3\"");
        assertEquals(TablaCalibracion.Metodo.ANCLADA, TablaCalibracion.fila('5', d, "SLV-002").metodo);
        assertTrue(TablaCalibracion.fila('5', d, "SLV-002").escribible());
        assertEquals(TablaCalibracion.Metodo.SOLO_VERIFICAR, TablaCalibracion.fila('b', d, "SLV-002").metodo);
        assertTrue(TablaCalibracion.fila('b', d, "SLV-002").aviso.contains("REMEDIDA-b"));
        Decisiones d2 = Decisiones.leer("PA-24,SLV-002,SI,2026-09-20,Diego,DOC.md,\"RF-CAL-14 P49 -10 3\"\n"
                + "REMEDIDA-b,SLV-002,RF-CAL-18,2026-09-20,Diego,DOC.md");
        assertEquals(TablaCalibracion.Metodo.ANCLADA, TablaCalibracion.fila('b', d2, "SLV-002").metodo);
        assertEquals("RF-CAL-18", TablaCalibracion.fila('b', d2, "SLV-002").reglaRemedida);
        assertTrue(TablaCalibracion.fila('b', d2, "SLV-002").dispensado("RF-CAL-14", "P49", -11.5));
        assertFalse(TablaCalibracion.fila('b', d2, "SLV-002").dispensado("RF-CAL-14", "P49", -14.0));
        assertFalse(TablaCalibracion.fila('b', d2, "SLV-002").dispensado("RF-CAL-14", "P39", 15.0));
        // QA-3613-03: las decisiones y la tabla de SLV-002 no valen para otro equipo.
        for (char k : Fabrica.CODIGOS) {
            assertFalse(String.valueOf(k), TablaCalibracion.fila(k, d2, "SLV-003").escribible());
        }
        // Una decision sin firmante Diego, sin fecha, sin documento o sin SI no vale.
        assertFalse(Decisiones.leer("PA-24,SLV-002,SI,2026-09-20,Operador,DOC.md").tomada("PA-24", "SLV-002"));
        assertFalse(Decisiones.leer("PA-24,SLV-002,SI,,Diego,DOC.md").tomada("PA-24", "SLV-002"));
        assertFalse(Decisiones.leer("PA-24,SLV-002,SI,2026-09-20,Diego,").tomada("PA-24", "SLV-002"));
        assertFalse(Decisiones.leer("PA-24,SLV-002,NO,2026-09-20,Diego,DOC.md").tomada("PA-24", "SLV-002"));
        assertEquals(12, TablaCalibracion.tabla().size());
    }

    // ---------------------------------------------------------------- T-A54

    @Test
    public void tA54CriteriosConNumeros() {
        // b) P49: x 2216,2 frente a 2277,9 (K 1/1), s_rep 2,24 %, R 497,8 frente a 484 -> CONFORME.
        Remedida3611.Resultado b = Remedida3611.criterios("P49", 484, 2216.2, 1, 2277.9, 1, 0.0224, "A5", 497.8);
        assertEquals(b.texto, "CONFORME", b.estado);
        // c) P5: 2463,9 frente a 2459,3; R 693,2 frente a 740 -> CONFORME (|-46,8| <= 74).
        Remedida3611.Resultado c = Remedida3611.criterios("P5", 740, 2463.9, 5, 2459.3, 5, 0.0224, "A5", 693.2);
        assertEquals(c.texto, "CONFORME", c.estado);
        // d) R un 12 % por encima -> NO_CONFORME.
        Remedida3611.Resultado d = Remedida3611.criterios("P49", 484, 2277.9, 5, 2277.9, 5, 0.0224, "A5", 484 * 1.12);
        assertEquals("NO_CONFORME", d.estado);
        // e) x un 8 % por debajo del banco -> NO_CONFORME.
        Remedida3611.Resultado e = Remedida3611.criterios("P49", 484, 2277.9 * 0.92, 5, 2277.9, 5, 0.0224, "A5", 484);
        assertEquals("NO_CONFORME", e.estado);
        // Sin s_rep medida: no se evalua.
        Remedida3611.Resultado f = Remedida3611.criterios("P49", 484, 2277.9, 5, 2277.9, 5, Double.NaN, "", 484);
        assertEquals("NO_EVALUABLE", f.estado);
    }

    @Test
    public void colocacionConPatronAusenteNoCuenta() {
        Ecuacion g = curva(0.25, -120);
        double[] xe = {2000, 2001, 1999, 2000};
        double[] rk = new double[4];
        for (int i = 0; i < 4; i++) {
            rk[i] = g.respuestaFloat32((int) xe[i]);
        }
        assertNull(Remedida3611.colocacion("P43", 2000, 2000, xe, rk, g));
        assertNotNull(Remedida3611.colocacion("P43", 575, 2000, xe, rk, g));        // oscuro: sin patron
        rk[2] += 40;
        assertNotNull(Remedida3611.colocacion("P43", 2000, 2000, xe, rk, g));       // par incoherente
    }

    // ------------------------------------------------------------------ D-20

    @Test
    public void d20UnaRepeticionComoMaximoYCadaIntentoEnElActa() throws Exception {
        StringWriter w = new StringWriter();
        Acta a = actaCon(w, '8');
        a.intento('8', "t1", "NO_VALIDA", "patrón ausente");
        a.intento('8', "t2", "NO_EVALUABLE", "sin s_rep");
        assertTrue(a.codigo('8').puedeRepetir());
        a.intento('8', "t3", "NO_CONFORME", "R +12 %");
        assertTrue(a.codigo('8').puedeRepetir());
        a.intento('8', "t4", "NO_CONFORME", "R +11 %");
        assertFalse(a.codigo('8').puedeRepetir());
        try {
            a.intento('8', "t5", "CONFORME", "a la tercera");
            fail("no se permite una segunda repetición");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("máximo una repetición"));
        }
        assertNotNull(a.motivoNoAceptable(false));
        a.restaurado('8', "#F,8# -> OK");
        assertTrue(a.codigo('8').resuelto());
        assertEquals(4, a.codigo('8').intentos.size());
        assertTrue(w.toString().contains("NO_VALIDA"));
        // La curva anterior queda en el acta para restaurar.
        assertTrue(a.codigo('8').anterior.igualFloat32(Fabrica.ecuacion('8')));
    }

    // ----------------------------------------------- RF-APP-36: acta en disco

    @Test
    public void actaEnDiscoSeRetomaIgual() throws Exception {
        StringWriter w = new StringWriter();
        Acta a = actaCon(w, 'b');
        a.dato("md5 de la cola", "9ddb7882fa6c32c50c90fcdd72ba8960");
        a.intento('b', "t1", "NO_CONFORME", "x -8 %");
        Acta r = Acta.leer(new StringReader(w.toString()));
        assertNotNull(r);
        assertFalse(r.cerrada());
        assertEquals("SLV-002", r.equipo);
        assertEquals(1, r.codigos().size());
        assertEquals(1, r.codigo('b').validos());
        assertTrue(r.codigo('b').puedeRepetir());
        assertEquals("9ddb7882fa6c32c50c90fcdd72ba8960", r.dato("md5 de la cola"));
        assertTrue(r.codigo('b').leida.igualFloat32(curva(0.25, -120)));
        assertTrue(r.codigo('b').anterior.igualFloat32(Fabrica.ecuacion('b')));
        // Se sigue escribiendo en el mismo diario.
        StringWriter w2 = new StringWriter();
        r.continuarEn(w2);
        r.intento('b', "t2", "CONFORME", "ok");
        Acta r2 = Acta.leer(new StringReader(w + w2.toString()));
        assertTrue(r2.codigo('b').conforme());
    }

    // --------------------------------------------- RF-APP-37: corte durante #S

    @Test
    public void corteDuranteSQuedaPendienteYBloquea() throws Exception {
        StringWriter w = new StringWriter();
        Acta a = new Acta("SLV-002", MAC, "3.6.2", 5, 4, 1, "t0");
        a.escribirEn(w);
        a.escribiendo('8', Fabrica.ecuacion('8'), curva(0.25, -120));
        // Se corta aqui: al leer de disco el #S sigue sin resolver.
        Acta r = Acta.leer(new StringReader(w.toString()));
        assertEquals('8', r.escribiendo());
        assertTrue(r.escribiendoEnviada().igualFloat32(curva(0.25, -120)));
        assertTrue(r.escribiendoAnterior().igualFloat32(Fabrica.ecuacion('8')));
        assertNotNull(r.motivoNoEscribir('b'));
        assertNotNull(r.motivoNoAceptable(false));
        r.sinEscribir('8', "el #S no entró");
        assertEquals(0, r.escribiendo());
        assertNull(r.motivoNoEscribir('b'));
    }

    // --------------------------------------------------- P10 §9 (hallazgos)

    @Test
    public void reescribirElMismoCodigoNoBloquea() throws Exception {
        StringWriter w = new StringWriter();
        Acta a = actaCon(w, '8');
        assertNull(a.motivoNoEscribir('8'));
        assertNotNull(a.motivoNoEscribir('b'));      // otro codigo: primero la re-medida del 8
        a.escribiendo('8', curva(0.25, -120), curva(0.26, -125));
        a.escrito('8', Tramas.tramaG('8', curva(0.26, -125)), curva(0.26, -125), "", "recta anclada", "");
        assertEquals(1, a.codigos().size());
        assertTrue(a.codigo('8').leida.igualFloat32(curva(0.26, -125)));
    }

    @Test
    public void fTrasLaConformidadInvalidaElActa() throws Exception {
        Acta a = actaCon(new StringWriter(), '8');
        a.intento('8', "t", "CONFORME", "ok");
        assertNull(a.motivoNoAceptable(false));
        a.invalidar("#F,8# -> OK después de abrir el acta");
        assertTrue(a.invalidada());
        assertNotNull(a.motivoNoAceptable(false));
        assertNotNull(a.motivoNoEscribir('b'));
    }

    @Test
    public void siSCFallaElActaNoSeCierra() throws Exception {
        Acta a = actaCon(new StringWriter(), '8');
        a.intento('8', "t", "CONFORME", "ok");
        try {
            a.aceptar("hoy", null, true, false);
            fail("sin #SC grabada no se acepta");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("#SC"));
        }
        assertFalse(a.cerrada());
        a.aceptar("hoy", "19/09/2026", true, false);
        assertTrue(a.cerrada());
    }

    // ------------------------------------------------ T-A63 / D-18 / RF-APP-46

    @Test
    public void tA63DatosObligatoriosYVerificacionesFrescas() throws Exception {
        Acta a = actaCon(new StringWriter(), '8');
        a.intento('8', "t", "CONFORME", "ok");
        assertTrue(a.motivoNoAceptable(true).contains("md5 de la cola"));
        a.dato("md5 de la cola", "9ddb7882fa6c32c50c90fcdd72ba8960");
        a.dato("s_rep", "s_rep 2.24 %");
        a.dato("batería", "");                              // desconocido: "no conocido", nunca vacio
        assertEquals("no conocido", a.dato("batería"));
        a.dato("#V# posterior", "#V,3.6.2,...#");
        assertTrue(a.motivoNoAceptable(true).contains("heredados"));
        a.dato("heredados (T-C41)", "OK");
        a.dato("código 5", "de fábrica, sin decisión");
        assertEquals("falta oscuro 8", a.motivoNoAceptable(true));
        a.dato("oscuro 8", "Oscuro (x = 575.0)");
        assertTrue(a.motivoNoAceptable(true).contains("persistencia"));
        a.persistencia(true, "#G igual, #E coincide");
        // QA-3612-01: el boton se habilita ya; la verificacion final la hace la accion de aceptar.
        assertNull(a.motivoNoAceptableSalvoVerificacionFinal());
        assertTrue(a.motivoNoAceptable(true).contains("verificación final"));
        a.verificacionFinal(true, "#V# y #G frescos iguales");
        assertNull(a.motivoNoAceptable(true));
    }

    // ---------------------------------------------------- Anclas (oscuro, s_rep)

    @Test
    public void anclasSinSeriesNoInventanValores() throws Exception {
        Campana c = new Campana(catalogo132(), "SLV-002", MAC);
        BancoCola q = cola();
        assertTrue(Double.isNaN(Anclas.oscuro(q, c, '8').valor));
        assertTrue(Anclas.oscuro(q, c, '8').texto.contains("OSCURO"));
        assertTrue(Double.isNaN(Anclas.sRep(q, c).valor));
    }

    @Test
    public void oscuroDeLaSesionEsLaMediaDeInicioYFin() throws Exception {
        Campana c = new Campana(catalogo132(), "SLV-002", MAC);
        BancoCola q = cola();
        int ses = Anclas.sesionDe(q, '8');
        assertTrue(ses > 0);
        List<BancoCola.Paso> osc = new ArrayList<>();
        for (BancoCola.Paso p : q.pasos) {
            if ("OSCURO".equals(p.tipo) && p.sesion == ses) {
                osc.add(p);
            }
        }
        assertTrue("la cola trae OSCURO al inicio y al final de la sesión " + ses, osc.size() >= 2);
        double[] medias = {574, 578};
        for (int i = 0; i < 2; i++) {
            BancoCola.Paso p = osc.get(i == 0 ? 0 : osc.size() - 1);
            Campana.Serie s = c.nuevaSerie("f", "SLV-002", MAC, "V3.6", Campana.OSCURO.nombre, 0, 'e');
            for (int j = 0; j < 5; j++) {
                c.agregarDisparo(s, 1, "f", "::" + (int) medias[i], medias[i]);
            }
            c.anotarPaso(p.orden, "HECHO", s.id, "f", "");
        }
        Anclas.Valor v = Anclas.oscuro(q, c, '8');
        assertEquals(v.texto, 576.0, v.valor, 1e-9);
        // Con deriva mayor que el limite: no hay ancla.
        Campana d = new Campana(catalogo132(), "SLV-002", MAC);
        double[] deriva = {560, 600};
        for (int i = 0; i < 2; i++) {
            BancoCola.Paso p = osc.get(i == 0 ? 0 : osc.size() - 1);
            Campana.Serie s = d.nuevaSerie("f", "SLV-002", MAC, "V3.6", Campana.OSCURO.nombre, 0, 'e');
            for (int j = 0; j < 5; j++) {
                d.agregarDisparo(s, 1, "f", "::" + (int) deriva[i], deriva[i]);
            }
            d.anotarPaso(p.orden, "HECHO", s.id, "f", "");
        }
        Anclas.Valor w = Anclas.oscuro(q, d, '8');
        assertTrue(w.texto, Double.isNaN(w.valor));
        assertTrue(w.texto.contains("deriva"));
    }
}
