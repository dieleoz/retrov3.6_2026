package com.dpi.retrov36;

import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * RTV 1.0.0-rc3: la columna de temperatura del diario (decision de Diego: se anota en cada disparo) y la
 * guarda del ajuste degenerado.
 *
 * <b>La que se ve en rojo contra la rc2 (a1fbc63).</b> {@link #t01LaOpticaEsElSegundoCampoNoElPrimero()}: el
 * firmware manda "#T,&lt;Tcirc&gt;,&lt;TO&gt;#" —circuito PRIMERO— (V4.6:Calibracion.c:986,988) y el
 * borrador dice lo mismo (PROTOCOLO-V4.6-BORRADOR.md:215,251). Contra a1fbc63 ni siquiera compila, porque
 * alli no existe Tramas.parsearT; pero la trampa que cierra es real y estuvo viva dentro de esta misma
 * entrega: la primera version de parsearT leia la optica del campo 1. Con la V4.6 de verdad eso habria
 * metido el 0 ESTRUCTURAL del circuito en la columna optica, que es justo la que se ajusta.
 *
 * Y el simulador de la rc2 respondia "#T,25.0,25.0#": simetrico. Una prueba contra el no habria delatado la
 * inversion jamas. Por eso {@link EquipoSimuladoV46} ya no es simetrico.
 *
 * Nada de esto se ha probado contra un equipo fisico: no existe ninguna V4.6 grabada.
 */
public class TemperaturaRtv10Test {

    private static final String MAC = "00:21:13:05:19:3B";

    private static Campana campana() throws IOException {
        Campana c = new Campana(new ArrayList<Patron>(), "SLV-003-2026", MAC);
        c.escribirEn(new StringWriter());
        return c;
    }

    // ------------------------------------------------------------------ T-01: el orden de los campos

    /** T-01 (RUPTURA): la optica es el SEGUNDO campo de "#T", no el primero. */
    @Test
    public void t01LaOpticaEsElSegundoCampoNoElPrimero() {
        // tal como lo manda el candidato: circuito 0 (estructural), optica 24,5
        Tramas.Temperatura t = Tramas.parsearT("#T,0.00000000E+00,2.45000000E+01#");
        assertNotNull(t);
        assertEquals("la OPTICA es el segundo campo", 24.5, t.optica, 1e-9);
        assertEquals("el CIRCUITO es el primero", 0.0, t.circuito, 1e-9);
        // y lo contrario, para que no pase por simetria
        Tramas.Temperatura u = Tramas.parsearT("#T,11.0,33.0#");
        assertEquals(33.0, u.optica, 1e-9);
        assertEquals(11.0, u.circuito, 1e-9);
        assertFalse("un caso asimetrico: si se invirtiera, esto fallaria", u.optica.equals(u.circuito));
    }

    /** T-02: el 0 del circuito no pasa en silencio; lleva su motivo, que es estructural y verificado. */
    @Test
    public void t02ElCeroDelCircuitoLlevaSuMotivo() {
        Tramas.Temperatura t = Tramas.parsearT("#T,0.00000000E+00,2.45000000E+01#");
        assertTrue(t.motivo, t.motivo.contains("ST_TEMPCIRC_AP"));
        assertTrue(t.hayOptica());
        // con un circuito que no es 0 (el dia que el firmware lo asigne) no hay aviso de ese tipo
        assertFalse(Tramas.parsearT("#T,22.0,24.5#").motivo.contains("ST_TEMPCIRC_AP"));
    }

    /**
     * T-03: estado DESC (RF-FW-B13). El firmware fuerza TO = 0 cuando el sensor esta desconectado o pasa de
     * 70 C (V4.6:Temp_Optica.c:63-66). Ese 0 NO es una temperatura: la columna va vacia con su motivo.
     */
    @Test
    public void t03DescDejaLaOpticaVaciaNoACero() {
        Tramas.Temperatura t = Tramas.parsearT("#T,0.0,0.0,DESC#");
        assertNotNull(t);
        assertNull("un 0 de centinela no es una temperatura: la columna va vacia", t.optica);
        assertFalse(t.hayOptica());
        assertTrue(t.motivo, t.motivo.contains("DESC"));
        // con OK si pasa
        Tramas.Temperatura ok = Tramas.parsearT("#T,0.0,24.5,OK#");
        assertEquals(24.5, ok.optica, 1e-9);
    }

    /** T-04: se admiten las dos formas del contrato, la del candidato y la del borrador; nada mas. */
    @Test
    public void t04SeAdmitenLasDosFormasDeT() {
        assertNotNull(Tramas.parsearT("#T,0.0,24.5#"));
        assertNotNull(Tramas.parsearT("#T,0.0,24.5,OK#"));
        for (String mala : new String[]{"#T#", "#T,24.5#", "#T,1,2,3,4#", "#GB,50#", "#X,1,100#", null}) {
            assertNull(String.valueOf(mala), Tramas.parsearT(mala));
        }
        // un campo ilegible deja ESA columna vacia, con motivo, y no tumba la otra
        Tramas.Temperatura t = Tramas.parsearT("#T,0.0,ABC#");
        assertNull(t.optica);
        assertTrue(t.motivo, t.motivo.contains("TO"));
    }

    // ------------------------------------------------------------------ T-05 y T-06: la columna del diario

    /** T-05: la columna va al diario y vuelve; un diario sin ella (rc2) se sigue leyendo. */
    @Test
    public void t05LaColumnaVaAlDiarioYVuelve() throws Exception {
        Campana c = new Campana(new ArrayList<Patron>(), "SLV-003-2026", MAC);
        StringWriter w = new StringWriter();
        c.escribirEn(w);
        Campana.Serie s = c.nuevaSerie("2026-09-20T10:00:00", "SLV-003-2026", MAC, "V4.6", "P28", 0, '1');
        c.anotarTemperaturaSerie("2026-09-20T10:00:00", s.id, Campana.T_APERTURA, 24.5, 0.0,
                Campana.T_ESTADO_OK, "");
        c.agregarDisparo(s, 1, "2026-09-20T10:00:01", "#X,1,1.0E+03#", 1000, 24.5, 0.0,
                Campana.T_ESTADO_OK);
        c.agregarDisparo(s, 1, "2026-09-20T10:00:03", "#X,1,1.0E+03#", 1001, null, null, Campana.T_ESTADO_SIN);
        c.anotarTemperaturaSerie("2026-09-20T10:00:40", s.id, Campana.T_CIERRE, 25.1, 0.0,
                Campana.T_ESTADO_OK, "");
        c.anotarTemperatura("2026-09-20T10:00:00", true, Campana.T_APERTURA + "+" + Campana.T_CIERRE,
                "con #T# (V4.6)");

        Campana leida = new Campana(new ArrayList<Patron>(), "SLV-003-2026", MAC);
        leida.escribirEn(new StringWriter());
        assertEquals(0, leida.leerDiario(new StringReader(w.toString())));
        List<Campana.Disparo> d = leida.series().get(0).disparos;
        assertEquals(24.5, d.get(0).temperaturaOptica, 1e-9);
        assertEquals(0.0, d.get(0).temperaturaCircuito, 1e-9);
        assertEquals(Campana.T_ESTADO_OK, d.get(0).estadoTemperatura);
        assertNull("vacia sigue vacia al releer: nunca 0", d.get(1).temperaturaOptica);
        assertNull(d.get(1).temperaturaCircuito);
        assertEquals(Campana.T_ESTADO_SIN, d.get(1).estadoTemperatura);
        // la pareja apertura/cierre queda en el diario y da la deriva de la serie
        String diario = w.toString();
        assertTrue(diario, diario.contains("TEMP_SERIE") && diario.contains(Campana.T_APERTURA));
        assertTrue(diario, diario.contains(Campana.T_CIERRE));
    }

    /** T-06: un diario de la rc2 (sin las tres columnas) se lee igual, y la temperatura queda vacia. */
    @Test
    public void t06UnDiarioDeLaRc2SeSigueLeyendo() throws Exception {
        String viejo = Campana.CABECERA_DIARIO + "\n"
                + "SERIE,S001,2026-09-19T10:00:00,SLV-002,00:21:13:05:19:3B,V3.6,P28,0,1\n"
                + "DISPARO,S001,1,2026-09-19T10:00:01,::1000,1000,1\n"
                + "DISPARO,S001,2,2026-09-19T10:00:03,::1001,1001\n";
        Campana c = new Campana(new ArrayList<Patron>(), "SLV-002", MAC);
        c.escribirEn(new StringWriter());
        assertEquals("ninguna linea se pierde", 0, c.leerDiario(new StringReader(viejo)));
        List<Campana.Disparo> d = c.series().get(0).disparos;
        assertEquals(2, d.size());
        assertEquals(1000.0, d.get(0).x, 1e-9);
        assertEquals(1, d.get(0).colocacion);
        assertNull(d.get(0).temperaturaOptica);
        assertEquals(Campana.T_ESTADO_SIN, d.get(1).estadoTemperatura);
    }

    // ------------------------------------------------------------------ T-07 a T-09: por protocolo

    /** T-07: solo la V4.6 da temperatura; los demas la dejan vacia CON MOTIVO, y el motivo dice por que. */
    @Test
    public void t07SoloLaV46DaTemperaturaYLosDemasDicenPorQue() {
        Protocolo v46 = new ProtocoloV46();
        assertEquals("#T#", v46.tramaTemperatura());
        assertEquals("", v46.motivoSinTemperatura());
        assertTrue(v46.permitida("#T#"));

        for (Protocolo p : new Protocolo[]{new ProtocoloV36(), new ProtocoloV2020(),
                new ProtocoloV4Original("F41")}) {
            assertNull(p.nombre(), p.tramaTemperatura());
            assertNull(p.valorTemperatura("#T,0.0,24.5#"));
            String m = p.motivoSinTemperatura();
            assertFalse(p.nombre() + ": el motivo no puede estar vacio", m.isEmpty());
            // el motivo dice POR QUE, no solo que no la hay
            assertTrue(p.nombre() + ": " + m, m.length() > 40);
        }
        // y el de la V3.6 no confunde los coeficientes con el sensor
        assertTrue(new ProtocoloV36().motivoSinTemperatura().contains("#GT#"));
    }

    /** T-08: contra el simulador, la V4.6 lee la temperatura con "#T#" y la coloca donde toca. */
    @Test
    public void t08LaV46LeeLaTemperaturaContraElSimulador() throws Exception {
        EquipoSimuladoV46 sim = new EquipoSimuladoV46(EquipoSimulado.slv002());
        sim.tempOptica = 27.25;
        Ops o = new Ops(sim, new ProtocoloV46());
        Ops.LecturaT t = o.temperatura();
        assertTrue(t.motivo, t.hay());
        assertEquals(27.25, t.optica(), 1e-6);
        assertEquals("el circuito del candidato es 0", 0.0, t.circuito(), 1e-9);
        assertEquals(1, sim.cuantas("#T#"));
        // DESC: la optica queda vacia, no a 0
        EquipoSimuladoV46 desc = new EquipoSimuladoV46(EquipoSimulado.slv002());
        desc.conEstado = true;
        desc.descOptica = true;
        Ops.LecturaT td = new Ops(desc, new ProtocoloV46()).temperatura();
        assertFalse(td.hay());
        assertNull(td.optica());
        assertTrue(td.motivo, td.motivo.contains("DESC"));
    }

    /**
     * T-09: si "#X" ya trae la temperatura (RF-FW-B13, "#X,k,x,TO,a#") se usa esa y NO se pide "#T#": es la
     * del propio disparo y no cuesta ni un viaje. Con el candidato de hoy, tres campos, hay que pedirla.
     */
    @Test
    public void t09SiXTraeLaTemperaturaNoHaceFaltaPedirla() {
        // candidato de hoy: tres campos, sin temperatura dentro
        Tramas.XCompleta hoy = Tramas.parsearXCompleta("#X,1,1.00000000E+03#", '1');
        assertNotNull(hoy);
        assertEquals(1000.0, hoy.x, 1e-9);
        assertFalse(hoy.traeTemperatura());
        // borrador: cinco campos, con TO y el ajuste aplicado
        Tramas.XCompleta man = Tramas.parsearXCompleta("#X,1,1.00000000E+03,2.45000000E+01,3.50000000E+00#", '1');
        assertNotNull(man);
        assertEquals(1000.0, man.x, 1e-9);
        assertTrue(man.traeTemperatura());
        assertEquals(24.5, man.temperaturaOptica, 1e-9);
        assertEquals(3.5, man.ajuste, 1e-9);
        // la x se lee igual en las dos formas: la app habla con el firmware de hoy y con el de manana
        assertEquals(Tramas.parsearX("#X,1,1.00000000E+03#", '1'),
                Tramas.parsearX("#X,1,1.00000000E+03,2.45000000E+01,3.50000000E+00#", '1'));
        // y cuatro campos no es ninguna de las dos: no se acepta
        assertNull(Tramas.parsearXCompleta("#X,1,1000,24.5#", '1'));
        assertNull("la clave tiene que coincidir", Tramas.parsearXCompleta("#X,1,1000#", '2'));
    }

    /** T-10: "#ERR,OCUPADO#" a un "#X" se dice como lo que es, no como "trama rara". */
    @Test
    public void t10OcupadoSeDiceComoOcupado() {
        assertEquals("OCUPADO", Tramas.motivoError("#ERR,OCUPADO#"));
        assertNull(Tramas.parsearX("#ERR,OCUPADO#", '1'));
        assertNull(Tramas.parsearT("#ERR,OCUPADO#"));
        // y el protocolo de la V4.6 admite recibirlo (es una trama '#' valida)
        assertNotNull(Tramas.campos("#ERR,OCUPADO#"));
    }

    // ------------------------------------------------------------------ T-11: la guarda del ajuste

    /**
     * T-11: la guarda del ajuste degenerado. Hoy NUNCA autoriza, porque el umbral no esta medido, y lo dice
     * con esas palabras en vez de dejar pasar un ajuste sin fundamento.
     */
    @Test
    public void t11LaGuardaNoAjustaMientrasNoHayaUmbralMedido() {
        assertTrue("el umbral no puede estar puesto sin medirlo",
                Double.isNaN(AjusteTemperatura.RECORRIDO_TO_MINIMO));

        // sin ninguna temperatura: no se ajusta, y se dice que no hay con que
        AjusteTemperatura.Decision v = AjusteTemperatura.evaluar(new Double[]{null, null, null});
        assertFalse(v.ajustar);
        assertEquals(3, v.sinTemperatura);
        assertEquals(0, v.conTemperatura);
        assertTrue(v.motivo, v.motivo.contains("fábrica"));

        // con recorrido amplio TAMPOCO se ajusta hoy: falta el umbral, y el motivo lo dice
        AjusteTemperatura.Decision a = AjusteTemperatura.evaluar(new Double[]{10.0, 20.0, 35.0, null});
        assertFalse("sin umbral medido no se ajusta, por mucho recorrido que haya", a.ajustar);
        assertEquals(25.0, a.recorrido, 1e-9);
        assertEquals(3, a.conTemperatura);
        assertEquals(1, a.sinTemperatura);
        assertTrue(a.motivo, a.motivo.contains("NO ESTÁ MEDIDO"));
        assertTrue(a.motivo, a.motivo.contains(AjusteTemperatura.SE_QUEDA_FABRICA));

        // un NaN suelto cuenta como "sin temperatura", no como 0
        AjusteTemperatura.Decision n = AjusteTemperatura.evaluar(new Double[]{Double.NaN, 20.0});
        assertEquals(1, n.sinTemperatura);
        assertEquals(1, n.conTemperatura);
        assertEquals(0.0, n.recorrido, 1e-9);
        assertNull(AjusteTemperatura.evaluar(null).motivo == null ? "" : null);
        assertFalse(AjusteTemperatura.evaluar(null).ajustar);
    }
}
