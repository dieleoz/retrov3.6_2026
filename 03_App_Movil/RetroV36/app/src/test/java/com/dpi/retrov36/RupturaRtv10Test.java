package com.dpi.retrov36;

import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Pruebas de ruptura de la RTV 1.0 frente a la 3.6.17 (rama main, f5145ed), y cobertura de lo que la rc2
 * anadio sin pruebas: la cola del banco de la V4.6, la bateria en %, la clave de "#X,k#" por paso y la
 * separacion de colas V3.6 / V4.6 en el selector.
 *
 * <b>La que se ha visto en rojo.</b> {@link #r01SondaDeDeteccionNoEsDeTipo1()} compila contra los dos arboles
 * (solo usa {@code Tramas.SONDA_V4}) y falla contra la 3.6.17, donde la sonda es "@LEERV,BLA,1@"
 * (main:Tramas.java:24). El tipo 1 del V4.1 <b>escribe</b> el estado oculto ({@code error = R - 163},
 * V4.1:Aplicacion.c:297-298), asi que sondear con tipo 1 altera la siguiente medida del equipo de otros
 * papeles; el tipo 2 solo lo lee. Aqui la sonda es "@LEERV,BLA,2@" (Tramas.java:29).
 *
 * Las demas no llegan a compilar contra la 3.6.17 porque las clases que prueban no existen alli
 * ({@code Deteccion}, {@code Protocolo}, {@code ProtocoloV4Original}, {@code ProtocoloV46}); eso es lo que
 * se quiere demostrar, y por eso no se simula con un doble.
 *
 * Nada de esto se ha probado contra un equipo fisico ni en un telefono.
 */
public class RupturaRtv10Test {

    private static byte[] asset(String n) throws Exception {
        return Files.readAllBytes(new File("src/main/assets/" + n).toPath());
    }

    private static String texto(String n) throws Exception {
        return new String(asset(n), StandardCharsets.UTF_8);
    }

    // ---------------------------------------------------------------- R-01: la que se ve en rojo en la 3.6.17

    /**
     * R-01 (RUPTURA, roja en la 3.6.17): la sonda que se manda a un equipo sin identificar no puede ser de
     * tipo 1. En la 3.6.17 {@code SONDA_V4} vale "@LEERV,BLA,1@" y esta prueba falla alli.
     */
    @Test
    public void r01SondaDeDeteccionNoEsDeTipo1() {
        assertEquals("la sonda de deteccion debe ser de tipo 2 (no escribe el error oculto del V4.1)",
                "@LEERV,BLA,2@", Tramas.SONDA_V4);
        // y ha de ser una de las 12 peticiones validas, byte a byte
        assertTrue(Tramas.esLeervValida(Tramas.SONDA_V4));
        assertFalse("tipo 2: no toca el error oculto", Tramas.esTipo1(Tramas.claveDeLeerv(Tramas.SONDA_V4)));
    }

    /**
     * R-01 ter: ningun fuente puede llevar "@LEERV,BLA,1@" escrito como cadena de codigo. El rotulo de la
     * prueba 2 lo llevaba (Pruebas.java:54 en la 3.6.17 y hasta la rc2) y se quedo atras al cambiar la sonda:
     * el operador leia una trama que la app ya no enviaba. Solo se admite dentro de un comentario.
     */
    @Test
    public void r01terNingunFuenteLlevaLaSondaDeTipo1EnUnaCadena() throws Exception {
        File dir = new File("src/main/java/com/dpi/retrov36");
        File[] fs = dir.listFiles();
        assertNotNull(fs);
        List<String> mal = new ArrayList<>();
        for (File f : fs) {
            if (!f.getName().endsWith(".java")) {
                continue;
            }
            for (String l : new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8).split("\r?\n")) {
                String t = l.trim();
                boolean comentario = t.startsWith("//") || t.startsWith("*") || t.startsWith("/*");
                if (!comentario && l.contains("@LEERV,BLA,1@")) {
                    mal.add(f.getName() + ": " + t);
                }
            }
        }
        assertTrue(mal.toString(), mal.isEmpty());
    }

    /**
     * R-01 bis: el efecto que justifica R-01, contra el V4.1 simulado. Sondear con tipo 1 cambia la medida
     * siguiente de otros papeles; con la sonda de tipo 2 no cambia.
     */
    @Test
    public void r01bisLaSondaDeTipo1CambiaLaMedidaSiguiente() throws Exception {
        Protocolo p = new ProtocoloV4Original("F41");

        EquipoSimuladoV4 conTipo1 = new EquipoSimuladoV4();
        conTipo1.x = 2500;
        Ops o1 = new Ops(conTipo1, p);
        Integer antes1 = o1.medirR('2');
        // la sonda de la 3.6.17, tal cual la mandaba: "@LEERV,BLA,1@"
        conTipo1.pedir("@LEERV,BLA,1@", Tramas.Tipo.LEERV, Tramas.TIMEOUT_LEERV_MS);
        Integer despues1 = o1.medirR('2');
        assertNotNull(antes1);
        assertFalse("la sonda de tipo 1 arrastra el error oculto", antes1.equals(despues1));

        EquipoSimuladoV4 conTipo2 = new EquipoSimuladoV4();
        conTipo2.x = 2500;
        Ops o2 = new Ops(conTipo2, p);
        Integer antes2 = o2.medirR('2');
        conTipo2.pedir(Tramas.SONDA_V4, Tramas.Tipo.LEERV, Tramas.TIMEOUT_LEERV_MS);  // la de la RTV 1.0
        assertEquals("la sonda de tipo 2 no toca el estado oculto", antes2, o2.medirR('2'));
    }

    // ---------------------------------------------------------------- R-02 a R-04: las tres familias

    /** R-02: un "#V,4.6,...#" no puede acabar en DESCONOCIDA (en la 3.6.17 lo hace: Pruebas.java:259,296-298). */
    @Test
    public void r02LaV46NoEsDesconocida() throws Exception {
        Deteccion.Resultado r = Deteccion.detectar(EquipoSimuladoV46.enBlanco(), null, null, false);
        assertEquals(Protocolo.Firmware.F46, r.firmware);
        assertTrue(r.opera());
        assertTrue(r.protocolo instanceof ProtocoloV46);
        assertEquals("4.6", r.version.version);
        assertFalse(r.detalle.toString(), r.detalle.toString().contains("no es la del contrato"));
    }

    /** R-03: las tres familias salen de la deteccion, y ninguna se confunde con otra. */
    @Test
    public void r03LasTresFamiliasSeDistinguen() throws Exception {
        assertEquals(Protocolo.Firmware.F36,
                Deteccion.detectar(EquipoSimulado.slv002(), null, null, false).firmware);
        assertEquals(Protocolo.Firmware.F46,
                Deteccion.detectar(EquipoSimuladoV46.enBlanco(), null, null, false).firmware);
        Deteccion.Resultado v4 = Deteccion.detectar(new EquipoSimuladoV4(), null, null, false);
        assertEquals(Protocolo.Firmware.V4_ORIGINAL, v4.firmware);
        assertTrue(v4.protocolo instanceof ProtocoloV4Original);
        // y el V4 original se identifica sin haber enviado jamas 'e' ni una '@' que no sea la sonda
        for (String t : new EquipoSimuladoV4().recibidas) {
            fail("no deberia haber tramas aqui: " + t);
        }
    }

    /**
     * R-04: medir con @LEERV contra el V4 original, que es lo que permite sacar del equipo de Nordeste sus
     * lecturas sin grabarle nada. Las 12 claves miden, y NADA de lo enviado sale de las 12 @LEERV validas.
     */
    @Test
    public void r04ElV4OriginalMideConLeervYNadaMas() throws Exception {
        EquipoSimuladoV4 v4 = new EquipoSimuladoV4();
        Protocolo p = new ProtocoloV4Original("F41");
        Ops o = new Ops(v4, p);
        Set<String> enviadas = new LinkedHashSet<>();
        for (char k : Fabrica.CODIGOS) {
            Integer r = o.medirR(k);
            assertNotNull("clave " + k, r);
            String t = p.tramaMedida(k);
            assertTrue("clave " + k + " -> " + t, Tramas.esLeervValida(t));
            assertEquals("ida y vuelta de la clave", k, Tramas.claveDeLeerv(t));
            enviadas.add(t);
        }
        assertEquals("12 tramas distintas, una por clave", 12, enviadas.size());
        assertEquals(12, v4.recibidas.size());
        for (String t : v4.recibidas) {
            assertTrue("trama fuera de la lista cerrada: " + t, p.permitida(t));
        }
        assertFalse("el V4.1 no se bloquea con tramas de la lista", v4.bloqueado);
        // el tipo 1 se etiqueta y no se compara con el certificado
        assertEquals(ProtocoloV4Original.ETIQUETA_TIPO1_V41, p.etiquetaMedida('7'));
        assertEquals("R entera", p.etiquetaMedida('1'));
        // sin x: ni banco ni calibracion, y el motivo se dice
        assertFalse(p.daX());
        assertFalse(p.mideBanco());
        assertFalse(p.calibra());
        assertFalse(p.motivoNoBanco().isEmpty());
    }

    // ---------------------------------------------------------------- R-05: la guarda del byte desconocido

    /**
     * R-05: ningun perfil puede emitir hacia un equipo un byte que ese equipo no reconoce. El 18-sep-2026 un
     * 'e' a SLV-002 lo dejo sin Bluetooth; una '@' sin LEERV bloquea el V4.1 (V4.1:Serial.c:86-104). La
     * lista cerrada es por protocolo (Cliente.java:119-124), y aqui se comprueba perfil por perfil.
     */
    @Test
    public void r05NingunPerfilEmiteUnByteQueElEquipoNoConoce() {
        // V3 de 2020: ni 'e', ni '#', ni '@'
        Protocolo v2020 = new ProtocoloV2020();
        for (String t : Arrays.asList("e", "#V#", "#GC#", "#X,1#", "#GB#", Tramas.SONDA_V4, "@LEERV,BLA,1@",
                "@", "", "E", "ee")) {
            assertFalse("V3 2020 no puede emitir " + t, v2020.permitida(t));
        }
        for (char k : Fabrica.CODIGOS) {
            assertTrue(v2020.permitida(String.valueOf(k)));
        }
        assertTrue(v2020.permitida("9"));

        // V3.6: 'e' si, '@' jamas (ni siquiera la sonda)
        Protocolo v36 = new ProtocoloV36();
        assertTrue(v36.permitida("e"));
        for (String t : Arrays.asList(Tramas.SONDA_V4, "@LEERV,BLA,1@", "@", "#V,@#", "", "#")) {
            assertFalse("V3.6 no puede emitir " + t, v36.permitida(t));
        }

        // V4 original: SOLO las 12 @LEERV, byte a byte
        Protocolo v4 = new ProtocoloV4Original("F41");
        for (String t : Arrays.asList("e", "9", "6", "1", "a", "#V#", "#GB#", "@", "@@", "@LEERV@",
                "@LEERV,BLA,3@", "@LEERV,BLA,0@", "@leerv,BLA,1@", "@LEERV,BLA,1@ ", " @LEERV,BLA,1@",
                "@LEERV,BLA,1@\r\n", "@LEERV,BLA,1@@", "@LEERV,GRI,1@", "")) {
            assertFalse("V4 original no puede emitir " + t, v4.permitida(t));
        }

        // V4.6: @LEERV y "#...#"; ningun byte suelto (no adopta los de la V3.6)
        Protocolo v46 = new ProtocoloV46();
        for (String t : Arrays.asList("e", "9", "6", "1", "d", "@", "@LEERV,BLA,3@", "#V,@#", "", "#", "##")) {
            assertFalse("V4.6 no puede emitir " + t, v46.permitida(t));
        }
        assertTrue(v46.permitida("#X,1#"));
        assertTrue(v46.permitida("#GB#"));
        assertTrue(v46.permitida(Tramas.SONDA_V4));

        // Antes de identificar nada: solo las 6 de la deteccion, y 'e' no esta entre ellas
        assertEquals(6, Deteccion.PERMITIDAS.size());
        for (String t : Deteccion.PERMITIDAS) {
            assertFalse("la deteccion nunca envia 'e'", "e".equals(t));
            assertTrue("'@' que no es la sonda: " + t, t.indexOf('@') < 0 || Tramas.SONDA_V4.equals(t));
        }
        for (String t : Arrays.asList("e", "1", "a", "#GB#", "#X,1#", "@LEERV,BLA,1@", "@")) {
            assertFalse("antes de identificar no se envia " + t, Deteccion.permitida(t));
        }

        // Ninguna trama de medida de ningun protocolo puede ser 'e' salvo en la V3.6, que la tiene
        for (Protocolo p : Arrays.asList(v2020, v36, v4, v46)) {
            for (char k : Fabrica.CODIGOS) {
                String t = p.tramaMedida(k);
                if (t != null) {
                    assertTrue(p.nombre() + " mide con " + t, p.permitida(t));
                }
                String x = p.tramaX(k);
                if (x != null) {
                    assertTrue(p.nombre() + " lee x con " + x, p.permitida(x));
                }
            }
            String b = p.tramaBateria();
            if (b != null) {
                assertTrue(p.nombre() + " lee bateria con " + b, p.permitida(b));
            }
        }
    }

    // ---------------------------------------------------------------- R-06 a R-09: lo que anadio la rc2

    /**
     * R-06: la cola del banco de la V4.6 se carga (md5 admitido) y cada paso lleva la clave con la que la
     * V4.6 lee x. El OSCURO va dos veces, con la luz alta ('1') y la baja ('3').
     */
    @Test
    public void r06LaColaDeLaV46SeCargaConSusClaves() throws Exception {
        BancoCola c = BancoCola.cargar(asset(BancoCola.Tipo.REPRESENTATIVO_V46.asset));
        assertTrue("md5 de la cola V4.6 en la lista blanca", BancoCola.MD5_PERMITIDOS.contains(c.md5));
        assertFalse(c.pasos.isEmpty());
        List<Character> oscuros = new ArrayList<>();
        for (BancoCola.Paso p : c.pasos) {
            if ("OSCURO".equals(p.tipo)) {
                oscuros.add(p.claveX());
            }
            if (!"PATRON".equals(p.tipo) && !"A5".equals(p.tipo) && !"OSCURO".equals(p.tipo)) {
                continue;
            }
            char k = p.claveX();
            assertTrue("paso " + p.orden + " (" + p.patron + "): clave " + k + " no es un codigo",
                    Fabrica.esCodigo(k));
            // la clave del paso es la que manda: sale de codigo_equipo cuando lo hay
            if (p.codigo.length() == 1 && Fabrica.esCodigo(p.codigo.charAt(0))) {
                assertEquals("paso " + p.orden, p.codigo.charAt(0), k);
            }
            // y la trama que se enviaria es "#X,k#", permitida por el protocolo de la V4.6
            String tx = new ProtocoloV46().tramaX(k);
            assertEquals("#X," + k + "#", tx);
            assertTrue(new ProtocoloV46().permitida(tx));
        }
        assertTrue("el OSCURO se mide con la luz alta y con la baja: " + oscuros,
                oscuros.contains('1') && oscuros.contains('3'));
        assertTrue("cada sesion mide los dos OSCURO", oscuros.size() >= 4);
    }

    /**
     * R-07: la bateria de la V4.6 va en % ("#GB,<0-100>#"), no en la n de la orden 9. 0 % bloquea las
     * escrituras; por debajo del 20 % avisa; la medida nunca se bloquea.
     */
    @Test
    public void r07LaBateriaDeLaV46EnPorCiento() throws Exception {
        Map<Protocolo.Firmware, PerfilFirmware> m = PerfilFirmware.leer(texto(PerfilFirmware.ASSET));
        assertEquals("pct", m.get(Protocolo.Firmware.F46).bateriaUnidad);
        assertEquals("n9", m.get(Protocolo.Firmware.F36).bateriaUnidad);

        assertTrue(Bateria.interpretarPorcentaje(null, "#GB#").bloqueaEscrituras);
        Bateria.Lectura cero = Bateria.interpretarPorcentaje(0, "#GB#");
        assertTrue(cero.bloqueaEscrituras);
        assertTrue(cero.texto, cero.texto.contains("la medida sigue"));
        Bateria.Lectura baja = Bateria.interpretarPorcentaje(Bateria.AVISO_PCT - 1, "#GB#");
        assertTrue(baja.aviso);
        assertFalse(baja.bloqueaEscrituras);
        Bateria.Lectura buena = Bateria.interpretarPorcentaje(80, "#GB#");
        assertFalse(buena.aviso);
        assertFalse(buena.bloqueaEscrituras);
        assertTrue(buena.texto, buena.texto.contains("80 %"));

        // y de extremo a extremo: Ops la lee con #GB# y la interpreta en %
        EquipoSimuladoV46 sim = new EquipoSimuladoV46(EquipoSimulado.slv002());
        Ops o = new Ops(sim, new ProtocoloV46(PerfilFirmware.de(m, Protocolo.Firmware.F46)));
        Bateria.Lectura l = o.bateria();
        assertNotNull(l.texto);
        assertTrue("se leyo con #GB#", sim.cuantas("#GB") == 1);
    }

    /** R-08: el selector separa las colas de la V3.6 de la de la V4.6; ninguna cola sirve para las dos. */
    @Test
    public void r08ElSelectorSeparaLasColasDeV36YV46() throws Exception {
        int v46 = 0;
        for (BancoCola.Tipo t : BancoCola.Tipo.values()) {
            if (t.esV46()) {
                v46++;
            }
            // toda cola declarada existe en el APK y tiene md5 admitido
            BancoCola c = BancoCola.cargar(asset(t.asset));
            assertTrue(t.name() + ": md5 " + c.md5, BancoCola.MD5_PERMITIDOS.contains(c.md5));
        }
        assertEquals("una sola cola de la V4.6 hoy", 1, v46);
        assertTrue(BancoCola.Tipo.REPRESENTATIVO_V46.esV46());
        assertFalse(BancoCola.Tipo.REPRESENTATIVO.esV46());
        assertFalse(BancoCola.Tipo.COMPLETO.esV46());
        assertFalse(BancoCola.Tipo.VERIFICACION_ANUAL.esV46());
        assertEquals(BancoCola.Tipo.COMPLETO, BancoCola.Tipo.de("NO_EXISTE"));
        assertEquals(BancoCola.Tipo.REPRESENTATIVO_V46, BancoCola.Tipo.de("REPRESENTATIVO_V46"));
    }

    /**
     * R-09: con la cola de la V4.6 en firmwares.csv, la V4.6 mide el banco; calibrar sigue sin ofrecerse
     * fuera del simulador (A-5), y el boton dice por que.
     */
    @Test
    public void r09LaV46MideBancoYNoCalibraTodavia() throws Exception {
        Map<Protocolo.Firmware, PerfilFirmware> m = PerfilFirmware.leer(texto(PerfilFirmware.ASSET));
        PerfilFirmware f46 = m.get(Protocolo.Firmware.F46);
        assertNotNull(f46);
        assertEquals(BancoCola.Tipo.REPRESENTATIVO_V46.asset, f46.colaBanco);
        ProtocoloV46 p = new ProtocoloV46(f46);
        assertTrue("con cola, mide banco", p.mideBanco());
        assertTrue(p.motivoNoBanco().isEmpty());
        assertFalse("calibrar la V4.6 no se ofrece todavia", p.calibra());
        assertEquals(ProtocoloV46.MOTIVO_CALIBRAR, p.motivoNoCalibra());
        // en el simulador si, que es donde vive T-U15
        assertTrue(new ProtocoloV46(f46, true).calibra());
        // y la V3.6 no pierde nada: sigue calibrando y midiendo banco
        Protocolo v36 = new ProtocoloV36(PerfilFirmware.de(m, Protocolo.Firmware.F36));
        assertTrue(v36.calibra());
        assertTrue(v36.mideBanco());
        assertEquals("cola_banco_P1-P132.csv", v36.perfil().colaBanco);
        // el V4 original no mide banco por falta de x, no por falta de cola
        Protocolo v4 = new ProtocoloV4Original("F41");
        assertFalse(v4.daX());
        assertNull(v4.tramaX('1'));
    }

    /**
     * R-10: la V4.6 mide R con @LEERV y lee x con "#X,k#" segun la clave, y el 'e' de la V3.6 no aparece por
     * ningun lado en su camino.
     */
    @Test
    public void r10LaV46MideRConLeervYXConAlmohadillaX() throws Exception {
        EquipoSimuladoV46 sim = new EquipoSimuladoV46(EquipoSimulado.slv002());
        sim.base.x = 1234.5;
        Protocolo p = new ProtocoloV46();
        Ops o = new Ops(sim, p);
        for (char k : new char[]{'1', '3', '7', 'd'}) {
            assertEquals("x con #X," + k + "#", 1234.5, o.medirX(k), 1e-3);
            assertNotNull("R con @LEERV, clave " + k, o.medirR(k));
        }
        for (String t : sim.recibidas) {
            assertFalse("'e' enviado a una V4.6: " + t, "e".equals(t));
            assertTrue("trama fuera de la lista cerrada: " + t, p.permitida(t));
        }
        assertEquals(4, sim.cuantas("#X,"));
        assertEquals(4, sim.cuantas("@LEERV"));
    }
}
