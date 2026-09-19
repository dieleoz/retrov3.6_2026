package com.dpi.retrov36;

import org.junit.Test;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests JVM de la parte de calculo. Datos reales de SLV-002 (18-sep-2026),
 * leidos en la pantalla con el codigo 1 (blanco intenso):
 * pantalla 513 -> x ~ 1739; 623 -> 2125; 780 -> 3029.
 */
public class CalculoTest {

    private static final double[] X_SLV = {1739, 2125, 3029};
    private static final int[] R_SLV = {513, 623, 780};

    // ---------------------------------------------------------------- inversion

    @Test
    public void inversionDel1ReproduceLosDatosDeSLV002() {
        Ecuacion e1 = Fabrica.ecuacion('1');
        for (int i = 0; i < R_SLV.length; i++) {
            Inversion.Resultado r = Inversion.invertir(e1, R_SLV[i], Double.NaN);
            assertTrue(r.valido);
            // La x de referencia es aproximada ("x ~"): cabe dentro de la
            // resolucion del codigo en ese punto, mas una cuenta. Hacia 780 el
            // blanco intenso ya casi no sube (~10 cuentas de x por unidad de R).
            assertEquals("R=" + R_SLV[i], X_SLV[i], r.x, r.u + 1);
            // Hacia delante: la x estimada da la R de pantalla.
            assertEquals(R_SLV[i], e1.respuestaFirmware((int) Math.round(r.x)));
        }
    }

    @Test
    public void el1NoEsMonotonoY780EsAmbiguo() {
        // Maximo de la parabola del blanco intenso en x = 0.6197/(2*8.654e-5) ~ 3580.
        Inversion.Resultado r = Inversion.invertir(Fabrica.ecuacion('1'), 780, Double.NaN);
        assertEquals(2, r.intervalos);
        assertTrue(r.x < 3580);
        Inversion.Resultado alto = Inversion.invertir(Fabrica.ecuacion('1'), 780, 4100);
        assertTrue(alto.x > 3580);
    }

    @Test
    public void inversionDel6RecuperaLaXDeSLV002() {
        Ecuacion e6 = Fabrica.ecuacion('6');
        for (double xv : X_SLV) {
            int x = (int) xv;
            int r6 = e6.respuestaFirmware(x);
            Inversion.Resultado r = Inversion.invertir(e6, r6, Double.NaN);
            assertTrue(r.valido);
            assertEquals(1, r.intervalos);
            assertTrue("x=" + x + " dentro del intervalo", x >= r.desde && x <= r.hasta);
            assertTrue("resolucion del 6 en x=" + x + ": " + r.u, r.u <= 3);
        }
    }

    @Test
    public void el6EsMonotonoYSinTechoDe500a4300() {
        Ecuacion e6 = Fabrica.ecuacion('6');
        int anterior = -1;
        for (int x = 500; x <= 4300; x++) {
            assertTrue(e6.derivada(x) > 0);
            int r = e6.respuestaFirmware(x);
            assertTrue("sin techo en x=" + x, r > 0);
            assertTrue(r >= anterior);
            anterior = r;
        }
        // Toda R de ese rango se invierte a un solo intervalo.
        for (int r = e6.respuestaFirmware(500); r <= e6.respuestaFirmware(4300); r += 37) {
            assertEquals(1, Inversion.intervalos(e6, r).size());
        }
    }

    @Test
    public void cadenaCompletaPantallaDel1AlCodigo6() {
        // El operador ve 623 en pantalla con el codigo 1; la app, con '6', debe
        // devolver la misma x dentro de la resolucion de ambos codigos.
        Inversion.Resultado por1 = Inversion.invertir(Fabrica.ecuacion('1'), 623, Double.NaN);
        int r6 = Fabrica.ecuacion('6').respuestaFirmware((int) Math.round(por1.x));
        Inversion.Resultado por6 = Inversion.invertir(Fabrica.ecuacion('6'), r6, Double.NaN);
        assertEquals(por1.x, por6.x, por1.u + por6.u + 1);
    }

    @Test
    public void ceroNoSeInvierte() {
        assertFalse(Inversion.invertir(Fabrica.ecuacion('6'), 0, Double.NaN).valido);
    }

    // ------------------------------------------------------------------ ajuste

    @Test(expected = IllegalArgumentException.class)
    public void grado2ConLosTresPuntosDeSLV002SeRechaza() {
        // RF-APP-15 / C2: grado + 2 niveles. Con 3 puntos, grado 2 no se admite.
        Ajuste.ajustar(X_SLV, new double[]{513, 623, 780}, 2);
    }

    @Test
    public void ajusteGrado2ConLosDatosDeSLV002YUnCuartoPunto() {
        // Los 3 puntos reales mas uno de la curva de fabrica en x = 2500.
        double[] x = {1739, 2125, 2500, 3029};
        double[] r = {513, 623, Fabrica.ecuacion('1').evaluar(2500), 780};
        Ajuste.Resultado a = Ajuste.ajustar(x, r, 2);
        assertEquals(0, a.ecuacion.c3, 0);
        assertTrue("error maximo " + a.errorMaximo, a.errorMaximo < 2);
        // Puntos sobre la curva de fabrica: la parabola ajustada se le parece.
        assertEquals(-0.000086541, a.ecuacion.c2, 2e-5);
    }

    @Test
    public void ajusteGrado1ConResiduosCoherentes() {
        double[] r = {513, 623, 780};
        Ajuste.Resultado a = Ajuste.ajustar(X_SLV, r, 1);
        assertEquals(0, a.ecuacion.c2, 0);
        double suma = 0;
        double max = 0;
        for (int i = 0; i < 3; i++) {
            assertEquals(r[i] - a.ecuacion.evaluar(X_SLV[i]), a.residuos[i], 1e-9);
            suma += a.residuos[i];
            max = Math.max(max, Math.abs(a.residuos[i]));
        }
        assertEquals(0, suma, 1e-6); // con termino independiente, los residuos suman 0
        assertEquals(max, a.errorMaximo, 1e-12);
        assertTrue(a.errorMaximo > 1); // la curva real no es recta
    }

    @Test
    public void ajusteRecuperaLaEcuacionDeFabricaDel1() {
        Ecuacion f = Fabrica.ecuacion('1');
        List<Double> xs = new ArrayList<>();
        List<Double> rs = new ArrayList<>();
        for (int x = 1000; x <= 3500; x += 250) {
            xs.add((double) x);
            rs.add(f.evaluar(x));
        }
        Ajuste.Resultado a = Ajuste.ajustar(Estadistica.aVector(xs), Estadistica.aVector(rs), 2);
        assertEquals(f.c2, a.ecuacion.c2, 1e-12);
        assertEquals(f.c1, a.ecuacion.c1, 1e-8);
        assertEquals(f.c0, a.ecuacion.c0, 1e-5);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ajusteRechazaPuntosInsuficientes() {
        Ajuste.ajustar(new double[]{1000, 1000, 2000}, new double[]{1, 1, 2}, 2);
    }

    // -------------------------------------------------------------- coherencia

    private static List<Coherencia.Entrada> entradas(int x) {
        List<Coherencia.Entrada> l = new ArrayList<>();
        for (char k : Fabrica.CODIGOS) {
            Ecuacion e = Fabrica.ecuacion(k);
            l.add(new Coherencia.Entrada(k, e.respuestaFirmware(x), e));
        }
        return l;
    }

    @Test
    public void coherenciaAptaConLas12DeFabricaHacia615() {
        Coherencia.Resultado r = Coherencia.evaluar(entradas(615), null, Coherencia.TOLERANCIA_POR_DEFECTO);
        assertTrue(r.resumen, r.apto);
        assertEquals(615, r.xRef, 10);
        for (Coherencia.Linea l : r.lineas) {
            assertEquals(l.texto(), Coherencia.Estado.OK, l.estado);
        }
    }

    @Test
    public void coherenciaUsaLaXDeE() {
        Coherencia.Resultado r = Coherencia.evaluar(entradas(615), 615, 15);
        assertTrue(r.apto);
        assertEquals(615, r.xRef, 0);
    }

    @Test
    public void rojoOpacoTienePocaResolucionYNoSeCastiga() {
        Coherencia.Resultado r = Coherencia.evaluar(entradas(615), 615, 15);
        Coherencia.Linea b = null;
        for (Coherencia.Linea l : r.lineas) {
            if (l.codigo == 'b') {
                b = l;
            }
        }
        assertNotNull(b);
        // Tolerancia = base 15 + resolucion local (1/f'(615) ~ 16 cuentas).
        assertTrue("tolerancia de b: " + b.tolerancia, b.tolerancia > 25);
        assertEquals(Coherencia.Estado.OK, b.estado);
    }

    @Test
    public void coherenciaDetectaUnCodigoDesplazado() {
        List<Coherencia.Entrada> l = entradas(615);
        // El blanco intenso responde como si x fuera 700: ~85 cuentas de desvio.
        Ecuacion e1 = Fabrica.ecuacion('1');
        l.set(0, new Coherencia.Entrada('1', e1.respuestaFirmware(700), e1));
        Coherencia.Resultado r = Coherencia.evaluar(l, null, 15);
        assertFalse(r.apto);
        assertEquals(Coherencia.Estado.FUERA, r.lineas.get(0).estado);
    }

    @Test
    public void coherenciaSinRespuestaNoEsApta() {
        List<Coherencia.Entrada> l = entradas(615);
        l.set(3, new Coherencia.Entrada('4', null, Fabrica.ecuacion('4')));
        assertFalse(Coherencia.evaluar(l, null, 15).apto);
    }

    @Test
    public void coherenciaConPatronOscuroNoEsEvaluable() {
        // Hacia x = 300 casi todas dan negativo -> 0: no hay base para decidir.
        Coherencia.Resultado r = Coherencia.evaluar(entradas(300), null, 15);
        assertFalse(r.apto);
    }

    @Test
    public void coherenciaEnVariosPuntos() {
        for (int x : new int[]{700, 1200, 1739, 2125}) {
            Coherencia.Resultado r = Coherencia.evaluar(entradas(x), null, 15);
            assertTrue("x=" + x + ": " + r.resumen, r.apto);
        }
    }

    // ---------------------------------------------------------- repetibilidad

    @Test
    public void repetibilidad() {
        assertTrue(Repetibilidad.evaluar(new double[]{615, 618, 612, 616, 614}, 0).apto);
        assertFalse(Repetibilidad.evaluar(new double[]{600, 640, 610, 630, 590}, 0).apto);
        assertFalse(Repetibilidad.evaluar(new double[]{615, 615, 615, 615}, 1).apto);
    }

    // ------------------------------------------------------------------ tramas

    @Test
    public void extraerMedidaEsperaElSilencio() {
        assertNull(Tramas.extraer(Tramas.Tipo.MEDIDA, "::12", false));
        assertEquals("::123", Tramas.extraer(Tramas.Tipo.MEDIDA, "::123", true));
        assertEquals(Integer.valueOf(123), Tramas.valorMedida("::123"));
        assertNull(Tramas.extraer(Tramas.Tipo.MEDIDA, ":5:", true));
        assertEquals(":5:", Tramas.extraer(Tramas.Tipo.BATERIA, ":5:", false));
        assertNull(Tramas.extraer(Tramas.Tipo.BATERIA, "::123", true));
    }

    @Test
    public void tramasAdmin() {
        Tramas.InfoVersion v = Tramas.parsearVersion("#V,3.6,2026-09-20,CAL#");
        assertNotNull(v);
        assertEquals("3.6", v.version);
        assertEquals("CAL", v.marca);
        assertNull(Tramas.parsearVersion("::512"));
        Ecuacion g = Tramas.parsearG("#G,1,0,-8.654100E-05,0.6196681,-303#", '1');
        assertNotNull(g);
        assertEquals(-8.6541e-5, g.c2, 1e-12);
        assertNull(Tramas.parsearG("#G,2,0,1,2,3#", '1'));
        double[] t = Tramas.parsearGT("#GT,0,4.321200E-04,9.014865E-01#");
        assertNotNull(t);
        assertEquals(0.00043212, t[1], 1e-12);
        assertTrue(Tramas.esOk("#OK#"));
        assertEquals("PIN", Tramas.motivoError("#ERR,PIN#"));
        assertEquals("#V,3.6,x,DEF#", Tramas.extraer(Tramas.Tipo.ADMIN, "basura#V,3.6,x,DEF#", false));
    }

    @Test
    public void soloLaSondaDeV4LlevaArroba() {
        assertTrue(Tramas.peticionPermitida("e"));
        assertTrue(Tramas.peticionPermitida("#V#"));
        assertTrue(Tramas.peticionPermitida(Tramas.SONDA_V4));
        assertFalse(Tramas.peticionPermitida("@"));
        assertFalse(Tramas.peticionPermitida("@LEERV,BLA,2@"));
        // 'e' solo a un V3.6 identificado.
        assertFalse(Tramas.peticionPermitida("e", false));
        assertTrue(Tramas.peticionPermitida("e", true));
        assertTrue(Tramas.peticionPermitida("6", false));
        assertFalse(Tramas.peticionPermitida("@", true));
    }

    @Test
    public void numerosEnElFormatoDelContrato() {
        assertEquals("-8.6541E-05", Tramas.numero(-0.000086541, 7));
        assertEquals("0.6196681", Tramas.numero(0.619668128, 7));
        assertEquals("-303", Tramas.numero(-303, 7));
        assertEquals("0", Tramas.numero(0, 7));
        assertEquals("-7.3E-08", Tramas.numero(-0.000000073, 7));
    }

    @Test
    public void tramaSConNueveCifrasCabeEn96Bytes() {
        for (char k : Fabrica.CODIGOS) {
            Tramas.TramaS t = Tramas.tramaS(k, Fabrica.ecuacion(k));
            assertNotNull(t);
            assertTrue(t.texto, t.texto.length() <= Tramas.MAX_TRAMA);
            assertTrue(t.texto, t.enviada.igualFloat32(Fabrica.ecuacion(k)));
        }
        Ecuacion peor = new Ecuacion(-1.234567891e-7, -8.765432109e-5, -0.1234567891, -123.4567891);
        Tramas.TramaS t = Tramas.tramaS('d', peor);
        assertNotNull(t);
        assertEquals(69, t.texto.length());
        assertTrue(t.texto.startsWith("#S,d,-1.23456789E-07,-8.76543211E-05,"));
    }

    private static float ulpsArriba(float v, int n) {
        for (int i = 0; i < n; i++) {
            v = Math.nextUp(v);
        }
        return v;
    }

    @Test
    public void igualdadFloat32ConCuatroUlp() {
        // XC8 2.10: %.8E con hasta 2 ulp y strtod con hasta 3 (CAMBIOS-V3.6.md §3.4).
        Ecuacion f = Fabrica.ecuacion('1');
        assertEquals(4, Ecuacion.ULP_G);
        Ecuacion a2 = new Ecuacion(0, ulpsArriba((float) f.c2, 2), f.c1, f.c0);
        Ecuacion a4 = new Ecuacion(0, f.c2, ulpsArriba((float) f.c1, 4), f.c0);
        Ecuacion a5 = new Ecuacion(0, f.c2, ulpsArriba((float) f.c1, 5), f.c0);
        assertTrue("2 ulp: chip recien grabado", f.igualFloat32(a2));
        assertTrue(f.igualFloat32(a4));
        assertFalse(f.igualFloat32(a5));
        assertTrue("tras #S: strtod 3 + impresion 2", f.igualFloat32(a5, Ecuacion.ULP_S));
        assertFalse(f.igualFloat32(new Ecuacion(0, f.c2 * 1.001, f.c1, f.c0)));
    }

    @Test
    public void emulacionFloat32ConcuerdaConDoubleSalvoRedondeo() {
        // float32 y double solo pueden diferir en una unidad, en las fronteras.
        int distintas = 0;
        for (char k : Fabrica.CODIGOS) {
            Ecuacion e = Fabrica.ecuacion(k);
            for (int x = 500; x <= 4000; x += 7) {
                int d = Math.abs(e.respuestaFloat32(x) - e.respuestaFirmware(x));
                if (d != 0) {
                    distintas++;
                }
                assertTrue(k + " x=" + x, d <= 1 || e.respuestaFloat32(x) == 0 || e.respuestaFirmware(x) == 0);
            }
        }
        assertTrue("distintas: " + distintas, distintas < 100);
    }

    @Test
    public void versionConMascaraYOrdenE() {
        Tramas.InfoVersion v = Tramas.parsearVersion("#V,3.6,2026-09-20,CAL,1003#");
        assertNotNull(v);
        assertTrue(v.tieneMascara());
        assertTrue(v.codigoAjustado(0));
        assertTrue(v.codigoAjustado(1));
        assertFalse(v.codigoAjustado(2));
        assertTrue(v.temperaturaAjustada());
        assertFalse(Tramas.parsearVersion("#V,3.6,2026-09-20,DEF#").tieneMascara());
        assertNull(Tramas.parsearVersion("#V,3.6,2026-09-20,CAL,XYZ#"));
        assertEquals(Integer.valueOf(512), Tramas.parsearE("#E,1,512#", '1'));
        assertNull(Tramas.parsearE("#E,2,512#", '1'));
    }

    @Test
    public void tramaSTConLimitesDeX0() {
        assertNotNull(Tramas.tramaST(0, 0.00043212, 0.90148651));
        assertNull(Tramas.tramaST(0, 0.00043212, 0.49));
        assertNull(Tramas.tramaST(0, 0.00043212, 1.51));
        assertNull(Tramas.tramaST(0, Double.NaN, 1.0));
        assertTrue(Tramas.tramaST(0, 0.00043212, 1.5).length() <= Tramas.MAX_TRAMA);
    }

    @Test
    public void registroDeLaPantallaStone() {
        Tramas.RegistroK k = Tramas.parsearK("#K,17,A55A06830010010001;A5 5A 06 83 00 10 01 00 07#");
        assertNotNull(k);
        assertEquals(17, k.total);
        assertEquals(2, k.tramas.size());
        assertEquals(0x01, k.tramas.get(0)[Tramas.POS_CODIGO_STONE] & 0xFF);
        assertEquals(0x07, k.tramas.get(1)[Tramas.POS_CODIGO_STONE] & 0xFF);
        Tramas.RegistroK vacio = Tramas.parsearK("#K,0,#");
        assertNotNull(vacio);
        assertEquals(0, vacio.tramas.size());
        assertNull(Tramas.parsearK("#K,3,ZZ#"));
    }

    // --------------------------------------------------------------- patrones

    @Test
    public void csvDePatronesEmbebido() throws Exception {
        List<Patron> p;
        try (InputStreamReader r = new InputStreamReader(
                new FileInputStream("src/test/resources/patrones_P1-P50_hasta_3.6.9.csv"),
                StandardCharsets.UTF_8)) {
            p = Patron.leer(r);
        }
        assertEquals(51, p.size()); // P1-P31 y los 20 tipo I (P32a-P50)
        assertEquals("P1", p.get(0).nombre);
        assertEquals(85, p.get(18).valor, 0);
        assertEquals("azul", p.get(18).color);
        for (Patron x : p) {
            assertTrue(x.color, Fabrica.codigoIntenso(x.color) != 0);
        }
    }

    @Test
    public void medidaEnCsv() {
        Patron p = new Patron("P4", 828, "XI", "blanco");
        Medida m = new Medida("2026-09-18T10:00:00", "SLV-002", "00:11:22:33:44:55", "V3 2020",
                p, '6', "::390", 2126.5, 1.5, "6 invertido", "");
        String l = m.aCsv();
        assertTrue(l, l.contains("\"::390\""));
        assertTrue(l, l.contains("2126.50"));
        assertEquals(Medida.cabecera().split(",").length, 14);
    }
}
