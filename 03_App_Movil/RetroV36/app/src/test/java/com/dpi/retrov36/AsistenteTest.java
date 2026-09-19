package com.dpi.retrov36;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Asistente de calibracion, codigos de pantalla y ocultacion del PIN. */
public class AsistenteTest {

    private static Medida m(Patron p, double x) {
        return new Medida("t", "SLV-002", "mac", "V3.6", p, 'e', "::" + (int) x, x, 0, "e directa", "");
    }

    /** x a la que la ecuacion de fabrica del codigo da el valor certificado. */
    private static double xDe(char k, double r) {
        Ecuacion e = Fabrica.ecuacion(k);
        double lo = 500;
        double hi = 3500;
        for (int i = 0; i < 60; i++) {
            double mid = (lo + hi) / 2;
            if (e.evaluar(mid) < r) {
                lo = mid;
            } else {
                hi = mid;
            }
        }
        return lo;
    }

    private static List<Medida> blancos() {
        List<Medida> l = new ArrayList<>();
        Patron[] ps = {new Patron("P2", 414, "IV", "blanco"), new Patron("P27", 471, "IX", "blanco"),
                new Patron("P1", 762, "XI", "blanco"), new Patron("P6", 772, "XI", "blanco"),
                new Patron("P3", 378, "IV", "blanco")};
        for (Patron p : ps) {
            double x = xDe('1', p.valor);
            l.add(m(p, x - 1));
            l.add(m(p, x + 1));
        }
        // Un amarillo que no debe entrar en la curva del blanco.
        l.add(m(new Patron("P5", 740, "XI", "amarillo"), 2000));
        return l;
    }

    @Test
    public void soloBlancoYAmarilloIntensosSonAjustables() {
        assertTrue(Asistente.ajustable('1'));
        assertTrue(Asistente.ajustable('2'));
        for (char k : new char[]{'3', '4', '5', '6', '7', '8', 'a', 'b', 'c', 'd'}) {
            assertFalse(String.valueOf(k), Asistente.ajustable(k));
            assertFalse(Asistente.motivoNoAjustable(k).isEmpty());
        }
        assertTrue(Asistente.motivoNoAjustable('7').contains("tipo I"));
    }

    @Test
    public void ajusteDelBlancoConCincoPatrones() {
        List<Medida> med = blancos();
        // Grado 2: reproduce la parabola de fabrica, que tiene techo en x ~ 3580:
        // C2 lo rechaza por no ser creciente en todo 200-4400.
        Asistente.Propuesta p2 = Asistente.proponer('1', 2, med, Fabrica.ecuacion('1'));
        assertEquals(5, p2.puntos.size());
        assertTrue(p2.ajuste.errorMaximo < 1);
        assertFalse(p2.escribible());
        assertTrue(p2.bloqueos.toString(), p2.bloqueos.get(0).contains("creciente"));
        // Grado 1: creciente; negativa solo por debajo de los patrones (aviso).
        Asistente.Propuesta p1 = Asistente.proponer('1', 1, med, Fabrica.ecuacion('1'));
        assertTrue(p1.informe, p1.escribible());
        assertTrue(p1.avisos.toString(), p1.avisos.get(0).contains("mezcla tipos"));
        for (Asistente.Punto q : p1.puntos) {
            assertEquals(2, q.n);
        }
    }

    @Test
    public void formaDeLaCurvaEnTodoElRango() {
        // Fabrica del blanco intenso: techo dentro de 200-4400 -> bloqueo.
        assertFalse(Asistente.validarForma(Fabrica.ecuacion('1'), 1700).bloqueos.isEmpty());
        // Recta creciente, negativa solo por debajo de x = 1000 -> aviso, no bloqueo.
        Ecuacion recta = new Ecuacion(0, 0, 0.2, -200);
        Asistente.Forma f = Asistente.validarForma(recta, 1700);
        assertTrue(f.bloqueos.toString(), f.bloqueos.isEmpty());
        assertEquals(1, f.avisos.size());
        // La misma recta con patrones desde x = 800: negativa en el rango medido -> bloqueo.
        assertFalse(Asistente.validarForma(recta, 800).bloqueos.isEmpty());
        // Pasa de 4000 dentro del rango -> bloqueo.
        assertFalse(Asistente.validarForma(new Ecuacion(0, 0, 1.0, 0), 500).bloqueos.isEmpty());
    }

    @Test
    public void pocosPatronesBloquean() {
        List<Medida> med = blancos().subList(0, 6); // 3 patrones
        Asistente.Propuesta p = Asistente.proponer('1', 2, med, Fabrica.ecuacion('1'));
        assertFalse(p.escribible());
        Asistente.Propuesta p1 = Asistente.proponer('1', 1, med, Fabrica.ecuacion('1'));
        assertTrue(p1.informe, p1.escribible());
    }

    @Test
    public void codigoNoAjustableBloquea() {
        Asistente.Propuesta p = Asistente.proponer('7', 1, blancos(), Fabrica.ecuacion('7'));
        assertFalse(p.escribible());
    }

    @Test
    public void curvaDecrecienteBloquea() {
        List<Medida> l = new ArrayList<>();
        l.add(m(new Patron("A", 800, "XI", "blanco"), 1000));
        l.add(m(new Patron("B", 600, "XI", "blanco"), 1500));
        l.add(m(new Patron("C", 400, "XI", "blanco"), 2000));
        Asistente.Propuesta p = Asistente.proponer('1', 1, l, Fabrica.ecuacion('1'));
        assertFalse(p.escribible());
        assertTrue(p.bloqueos.toString(), p.bloqueos.get(0).contains("creciente"));
    }

    @Test
    public void codigosDePantallaStone() {
        assertTrue(Fabrica.describirCodigoStone(0x01).contains("blanco intenso"));
        assertTrue(Fabrica.describirCodigoStone(0x07).contains("PAPEL TIPO I"));
        assertTrue(Fabrica.describirCodigoStone(0x0B).contains("rojo opaco"));
        assertTrue(Fabrica.describirCodigoStone(0x0E).contains("PRUEBA ADC"));
        assertTrue(Fabrica.describirCodigoStone(0x20).contains("fuera"));
    }

    @Test
    public void elPinNoLlegaAlRegistro() {
        assertEquals("#L,****#", new String(Hex.ocultarPin("#L,2026#".getBytes())));
        assertEquals("#P,****,****#", new String(Hex.ocultarPin("#P,2026,1234#".getBytes())));
        assertEquals("#G,1#", new String(Hex.ocultarPin("#G,1#".getBytes())));
    }
}
