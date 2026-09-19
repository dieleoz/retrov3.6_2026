package com.dpi.retrov36;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Prueba 4 con los casos reales de SLV-002 grabado con la V3.6 (19-sep-2026,
 * app 3.6.2). Las dos pasadas usan las respuestas LITERALES del registro de
 * tramas; los tests de deriva y tolerancia local usan series sinteticas.
 */
public class CoherenciaRealTest {

    /** 12 codigos con x variando linealmente de x0 a x1 segun el orden de envio. */
    private static List<Coherencia.Entrada> pasada(double x0, double x1) {
        List<Coherencia.Entrada> l = new ArrayList<>();
        int n = Fabrica.CODIGOS.length;
        for (int i = 0; i < n; i++) {
            char k = Fabrica.CODIGOS[i];
            int x = (int) Math.round(x0 + (x1 - x0) * (i + 1) / (double) (n + 1));
            Ecuacion e = Fabrica.ecuacion(k);
            l.add(new Coherencia.Entrada(k, e.respuestaFloat32(x), e));
        }
        return l;
    }

    private static Coherencia.Linea linea(Coherencia.Resultado r, char k) {
        for (Coherencia.Linea l : r.lineas) {
            if (l.codigo == k) {
                return l;
            }
        }
        return null;
    }

    /** Respuestas literales, en el orden de envio 1-8, a-d. */
    private static List<Coherencia.Entrada> literales(int... r) {
        List<Coherencia.Entrada> l = new ArrayList<>();
        for (int i = 0; i < Fabrica.CODIGOS.length; i++) {
            char k = Fabrica.CODIGOS[i];
            l.add(new Coherencia.Entrada(k, r[i], Fabrica.ecuacion(k)));
        }
        return l;
    }

    /**
     * Segunda pasada, literal: registro "07 pruebas/19092026_0900/rtv36_20260919_085316 (1).txt",
     * lineas 996-1044 (app 3.6.2: 'e' solo al final, 3016). Sobre P1.
     */
    @Test
    public void segundaPasadaRealSobreP1EsApta() {
        List<Coherencia.Entrada> l = literales(776, 813, 803, 818, 804, 806, 810, 822, 809, 812, 812, 812);
        Coherencia.Resultado r = Coherencia.evaluar(l, null, 3016, Coherencia.TOLERANCIA_POR_DEFECTO);
        StringBuilder t = new StringBuilder(r.resumen);
        for (Coherencia.Linea x : r.lineas) {
            t.append('\n').append(x.texto());
        }
        assertFalse(t.toString(), r.invalida);
        assertTrue(t.toString(), r.apto);
        // El blanco y el amarillo intensos, cerca de su vertice: excluidos, no fallo.
        assertEquals(Coherencia.Estado.NO_EVALUABLE_ZONA, linea(r, '1').estado);
        assertEquals(Coherencia.Estado.NO_EVALUABLE_ZONA, linea(r, '2').estado);
        assertTrue(linea(r, '1').texto(), linea(r, '1').texto().contains("no evaluable en esta zona"));
    }

    @Test
    public void conElCriterioAntiguoLaSegundaPasadaFallaba() {
        // Reproduce el NO APTO de la 3.6.2: el codigo 1 a unas -22 cuentas de 'e'.
        Inversion.Resultado r1 = Inversion.invertir(Fabrica.ecuacion('1'), 776, 3016);
        assertEquals(-22.5, r1.x - 3016, 6);
        assertTrue("776 es ambigua: el blanco intenso tiene techo en x ~ 3580", r1.intervalos > 1);
    }

    /**
     * Primera pasada, literal: mismo registro, lineas 457-505. Codigos 1-8 en
     * oscuro, 'a' a medio apoyar, b-d sobre P1; 'e' al final = 3004.
     */
    @Test
    public void primeraPasadaRealEquipoApoyadoAMitadEsInvalida() {
        List<Coherencia.Entrada> l = literales(25, 22, 37, 41, 8, 9, 9, 14, 231, 794, 791, 805);
        Coherencia.Resultado r = Coherencia.evaluar(l, null, 3004, Coherencia.TOLERANCIA_POR_DEFECTO);
        assertTrue(r.resumen, r.invalida);
        assertFalse(r.apto);
        assertTrue(r.resumen, r.resumen.contains(Coherencia.MENSAJE_INVALIDA));
        assertTrue(r.resumen, r.resumen.contains("dos grupos"));
        // Sin ninguna 'e' (V3 2020), los grupos tambien la invalidan.
        assertTrue(Coherencia.evaluar(l, null, null, 10).invalida);
    }

    // Los tests siguientes usan series SINTETICAS (emulacion float32) para
    // aislar la deriva y la tolerancia local; no son datos del registro.

    @Test
    public void derivaMayorDe50EsInvalida() {
        Coherencia.Resultado r = Coherencia.evaluar(pasada(2950, 3021), 2950, 3021, 10);
        assertTrue(r.invalida);
        assertFalse(r.apto);
        Coherencia.Resultado ok = Coherencia.evaluar(pasada(2980, 3021), 2980, 3021, 10);
        assertFalse(ok.invalida);
        assertTrue(ok.resumen, ok.apto);
    }

    @Test
    public void laDerivaSeCorrigeConLaRecta() {
        // Deriva de 40 cuentas: con referencia constante los extremos se irian;
        // con la recta entre 'e' inicial y final cuadran sin gastar tolerancia.
        List<Coherencia.Entrada> l = pasada(1500, 1540);
        Coherencia.Resultado r = Coherencia.evaluar(l, 1500, 1540, 0);
        for (Coherencia.Linea x : r.lineas) {
            if (x.estado == Coherencia.Estado.OK) {
                assertTrue(x.texto(), Math.abs(x.desvio) < 1.0 / Fabrica.ecuacion(x.codigo).derivada(x.xRef) + 1);
            }
        }
    }

    @Test
    public void toleranciaLocalDelRojoOpacoHacia575() {
        Coherencia.Resultado r = Coherencia.evaluar(pasada(575, 575), 575, 575, 10);
        Coherencia.Linea b = linea(r, 'b');
        assertNotNull(b);
        assertEquals(Coherencia.Estado.OK, b.estado);
        // 1/f'(575) del rojo opaco ~ 18 cuentas por unidad de R.
        assertTrue("tolerancia de b: " + b.tolerancia, b.tolerancia > 25);
    }

    @Test
    public void grupos() {
        assertNull(Coherencia.dosGrupos(new double[]{3000, 3010, 2995, 3020, 3005}));
        assertNotNull(Coherencia.dosGrupos(new double[]{575, 580, 570, 3000, 3010}));
        // Un solo codigo descolgado no hace dos grupos: sera un FUERA, no una invalidez.
        assertNull(Coherencia.dosGrupos(new double[]{3000, 3010, 2995, 3020, 575}));
    }
}
