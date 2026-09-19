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
 * app 3.6.2; 06_Calibracion/SLV-002/ACTA-antes-y-despues-grabacion.md, G4).
 * Las respuestas por codigo no estan en el acta; se generan con la emulacion
 * float32 validada en el equipo (#E 60/60 exacto) sobre las x medidas, y se
 * usa el unico valor real anotado: codigo 1 = 776 sobre P1.
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

    @Test
    public void segundaPasadaRealSobreP1EsApta() {
        // e = 3004 al principio y 3023 al final (deriva real); codigo 1 = 776 (real).
        List<Coherencia.Entrada> l = pasada(3004, 3023);
        l.set(0, new Coherencia.Entrada('1', 776, Fabrica.ecuacion('1')));
        Coherencia.Resultado r = Coherencia.evaluar(l, 3004, 3023, Coherencia.TOLERANCIA_POR_DEFECTO);
        assertFalse(r.invalida);
        assertTrue(r.resumen, r.apto);
        // El blanco y el amarillo intensos, cerca de su vertice: excluidos, no fallo.
        assertEquals(Coherencia.Estado.NO_EVALUABLE_ZONA, linea(r, '1').estado);
        assertEquals(Coherencia.Estado.NO_EVALUABLE_ZONA, linea(r, '2').estado);
        assertTrue(linea(r, '1').texto(), linea(r, '1').texto().contains("no evaluable en esta zona"));
        for (Coherencia.Linea x : r.lineas) {
            assertTrue(x.texto(), x.estado != Coherencia.Estado.FUERA);
        }
    }

    @Test
    public void conElCriterioAntiguoLaSegundaPasadaFallaba() {
        // Reproduce el NO APTO de la 3.6.2: el codigo 1 a unas -22 cuentas de 'e'.
        Inversion.Resultado r1 = Inversion.invertir(Fabrica.ecuacion('1'), 776, 3016);
        assertEquals(-22.5, r1.x - 3016, 6);
        assertTrue("776 es ambigua: el blanco intenso tiene techo en x ~ 3580", r1.intervalos > 1);
    }

    @Test
    public void primeraPasadaRealEquipoApoyadoAMitadEsInvalida() {
        // Codigos 1-8 en oscuro (x ~ 575) y a-d sobre P1 (x ~ 3000); 'e' solo al final.
        List<Coherencia.Entrada> l = new ArrayList<>();
        for (char k : Fabrica.CODIGOS) {
            int x = (k >= '1' && k <= '8') ? 575 : 3000;
            Ecuacion e = Fabrica.ecuacion(k);
            l.add(new Coherencia.Entrada(k, e.respuestaFloat32(x), e));
        }
        Coherencia.Resultado r = Coherencia.evaluar(l, null, 3021, Coherencia.TOLERANCIA_POR_DEFECTO);
        assertTrue(r.resumen, r.invalida);
        assertFalse(r.apto);
        assertTrue(r.resumen, r.resumen.contains(Coherencia.MENSAJE_INVALIDA));
        assertTrue(r.resumen, r.resumen.contains("dos grupos"));
        // Sin ninguna 'e' (V3 2020), los grupos tambien la invalidan.
        assertTrue(Coherencia.evaluar(l, null, null, 10).invalida);
    }

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
