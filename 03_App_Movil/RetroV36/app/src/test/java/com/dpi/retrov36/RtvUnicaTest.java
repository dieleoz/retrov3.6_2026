package com.dpi.retrov36;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * RTV 1.0, app unica: pruebas T-U de TDD-V3.6.md §7 contra los simuladores ({@link EquipoSimulado} 3.6.2,
 * {@link EquipoSimuladoV4} V4.1, {@link EquipoSimuladoV46} con el contrato de PROTOCOLO-V4.6 §4, y un V3 de
 * 2020 minimo aqui). T-U15 (mismo flujo con la V4.6) esta en FlujoCalibracionTest.
 */
public class RtvUnicaTest {

    private static final String MAC = "00:21:13:05:19:3B";

    /** V3 de 2020 minimo: calla a '#', responde a 9 y/o a 6 segun se configure; registra lo recibido. */
    static final class Equipo2020 implements Canal {
        final List<String> recibidas = new ArrayList<>();
        boolean responde9 = true;
        boolean responde6 = true;

        @Override
        public Cliente.Respuesta pedir(String t, Tramas.Tipo tipo, long timeoutMs) {
            recibidas.add(t);
            if (t.indexOf('@') >= 0 || "e".equals(t)) {
                throw new AssertionError("trama prohibida a un V3 de 2020: " + t);
            }
            String r = "9".equals(t) && responde9 ? ":3:" : "6".equals(t) && responde6 ? "::512" : null;
            return r == null ? new Cliente.Respuesta(t, Receptor.Desenlace.TIMEOUT, null, "", -1)
                    : new Cliente.Respuesta(t, Receptor.Desenlace.VALIDA, r, r, 40);
        }
    }

    /** Nadie contesta (equipo apagado, modulo a otra velocidad o V4.1 ya bloqueado). */
    static final class Mudo implements Canal {
        final List<String> recibidas = new ArrayList<>();

        @Override
        public Cliente.Respuesta pedir(String t, Tramas.Tipo tipo, long timeoutMs) {
            recibidas.add(t);
            return new Cliente.Respuesta(t, Receptor.Desenlace.TIMEOUT, null, "", -1);
        }
    }

    private static void soloPermitidas(List<String> tramas) {
        for (String t : tramas) {
            assertTrue("trama de deteccion no permitida: " + t, Deteccion.permitida(t));
            assertFalse("e", "e".equals(t));
            assertTrue("'@' que no es la sonda: " + t, t.indexOf('@') < 0 || Tramas.SONDA_V4.equals(t));
        }
    }

    // ------------------------------------------------------------------ T-U01 a T-U06

    @Test
    public void tU01DeteccionDeF36() throws Exception {
        EquipoSimulado sim = EquipoSimulado.slv002();
        Deteccion.Resultado r = Deteccion.detectar(sim, null, null, false);
        assertEquals(Protocolo.Firmware.F36, r.firmware);
        assertTrue(r.protocolo instanceof ProtocoloV36);
        assertEquals(Arrays.asList("#V#", "#GC#", "#GN#"), sim.recibidas);
        assertEquals(Calibracion.Variante.V362, r.variante);
        assertEquals("SLV-002", r.serieEquipo);
    }

    @Test
    public void tU02DeteccionDeF46() throws Exception {
        EquipoSimuladoV46 sim = new EquipoSimuladoV46(EquipoSimulado.slv002());
        Deteccion.Resultado r = Deteccion.detectar(sim, null, null, false);
        assertEquals(Protocolo.Firmware.F46, r.firmware);
        assertTrue(r.protocolo instanceof ProtocoloV46);
        assertEquals(Arrays.asList("#V#", "#GC#", "#GN#"), sim.recibidas);
        assertEquals("4.6", r.version.version);
    }

    @Test
    public void tU03DeteccionDeF2020() throws Exception {
        Equipo2020 a = new Equipo2020();
        Deteccion.Resultado r = Deteccion.detectar(a, null, null, false);
        assertEquals(Protocolo.Firmware.F2020, r.firmware);
        assertEquals(Arrays.asList("#V#", "9"), a.recibidas);
        Equipo2020 b = new Equipo2020();
        b.responde9 = false;
        assertEquals(Protocolo.Firmware.F2020, Deteccion.detectar(b, null, null, false).firmware);
        assertEquals(Arrays.asList("#V#", "9", "6"), b.recibidas);
        assertTrue(Deteccion.detectar(b, null, null, false).protocolo instanceof ProtocoloV2020);
    }

    @Test
    public void tU04DeteccionDeV4OriginalConEsperaDe5s() throws Exception {
        EquipoSimuladoV4 v4 = new EquipoSimuladoV4();
        v4.retardoMs = 3200;              // el V4.1: 1 s de espera + la medida (con la 3.6.14 se perdia: 3000 ms)
        Deteccion.Resultado r = Deteccion.detectar(v4, null, null, false);
        assertEquals(Protocolo.Firmware.V4_ORIGINAL, r.firmware);
        assertTrue(r.protocolo instanceof ProtocoloV4Original);
        assertEquals(Arrays.asList("#V#", "9", "6", "@LEERV,BLA,2@"), v4.recibidas);
        assertTrue(Tramas.TIMEOUT_LEERV_MS >= 5000);
        assertFalse("sigue vivo", v4.bloqueado);
        // la sonda es de otros papeles: no escribe el error oculto (C-U01, BLA,2)
        assertEquals(0, v4.error, 0);
        // el saludo de arranque y el "/n/r" + 0x00 no estorban
        assertTrue(v4.ultimoBruto.startsWith("*****") && v4.ultimoBruto.endsWith("@/n/r\0"));
    }

    @Test
    public void tU05NadieContesta() throws Exception {
        Mudo m = new Mudo();
        Deteccion.Resultado r = Deteccion.detectar(m, null, null, false);
        assertFalse(r.opera());
        assertEquals(Arrays.asList("#V#", "9", "6", Tramas.SONDA_V4), m.recibidas);
        assertTrue(r.detalle.toString().contains(Deteccion.SIN_RESPUESTA));
        assertTrue(r.detalle.toString().contains("módulo Bluetooth"));
        // AT-U11: un V4.1 ya bloqueado por una '@' sin LEERV: sin respuesta a nada, y no se insiste
        EquipoSimuladoV4 bloq = new EquipoSimuladoV4();
        bloq.bloqueado = true;
        Deteccion.Resultado rb = Deteccion.detectar(bloq, null, null, false);
        assertFalse(rb.opera());
        assertEquals(4, bloq.recibidas.size());
    }

    @Test
    public void tU06NuncaSeEnviaLoProhibidoEnLaDeteccion() throws Exception {
        EquipoSimulado f36 = EquipoSimulado.slv002();
        Deteccion.detectar(f36, null, null, false);
        EquipoSimuladoV46 f46 = new EquipoSimuladoV46(EquipoSimulado.slv002());
        Deteccion.detectar(f46, null, null, false);
        Equipo2020 f20 = new Equipo2020();
        Deteccion.detectar(f20, null, null, false);
        EquipoSimuladoV4 v4 = new EquipoSimuladoV4();
        Deteccion.detectar(v4, null, null, false);
        Mudo m = new Mudo();
        Deteccion.detectar(m, null, null, false);
        for (List<String> l : Arrays.asList(f36.recibidas, f46.recibidas, f20.recibidas, v4.recibidas, m.recibidas)) {
            soloPermitidas(l);
        }
        assertEquals("@LEERV,BLA,2@", Tramas.SONDA_V4);
    }

    // ------------------------------------------------------------------ T-U07 y caducidad (A-3)

    private static final String CAB = "mac,perfil,variante,serie_declarada,caduca_con_v,fecha,firmante,documento,nota\n";

    @Test
    public void tU07PerfilPorMac() throws Exception {
        PerfilesEquipo.Firmados f = PerfilesEquipo.leerFirmados(CAB
                + "00:21:13:AA:BB:01,SOLO_SONDA,,SLV-003-2026,4.6,2026-09-19,Diego,DOC.md,V3-2\n"
                + "00:21:13:AA:BB:02,F36,,,,2026-09-19,Diego,DOC.md,\n"
                + "00:21:13:AA:BB:03,F36,,,,2026-09-19,Operador,DOC.md,sin firma de Diego\n"
                + "00:21:13:AA:BB:04,V5,,,,2026-09-19,Diego,DOC.md,perfil que no existe\n");
        assertEquals(2, f.porMac.size());
        assertEquals(2, f.rechazados.size());
        // (a) SOLO_SONDA: el registro es solo la sonda
        EquipoSimuladoV4 v4 = new EquipoSimuladoV4();
        Deteccion.Resultado a = Deteccion.detectar(v4, f.de("00:21:13:aa:bb:01"), null, false);
        assertEquals(Arrays.asList(Tramas.SONDA_V4), v4.recibidas);
        assertEquals(Protocolo.Firmware.V4_ORIGINAL, a.firmware);
        // (b) perfil F36 y un V4.1 que responde a la sonda: no se opera
        EquipoSimuladoV4 v4b = new EquipoSimuladoV4();
        Deteccion.Resultado b = Deteccion.detectar(v4b, f.de("00:21:13:AA:BB:02"), null, false);
        assertFalse(b.opera());
        assertTrue(b.contradicePerfil);
        assertTrue(b.detalle.toString().contains(Deteccion.CONTRADICE));
    }

    /** A-3 / H-07: el perfil SOLO_SONDA no bloquea para siempre a un equipo que se regraba con la V4.6. */
    @Test
    public void elPerfilCaducaCuandoCambiaV() throws Exception {
        PerfilesEquipo.Firmados f = PerfilesEquipo.leerFirmados(CAB
                + "00:21:13:AA:BB:01,SOLO_SONDA,,SLV-003-2026,4.6,2026-09-19,Diego,DOC.md,V3-2\n"
                + "00:21:13:AA:BB:02,F36,,,4.6,2026-09-19,Diego,DOC.md,\n");
        // Deteccion completa pedida por el operador tras regrabar: #V# responde 4.6
        EquipoSimuladoV46 v46 = new EquipoSimuladoV46(EquipoSimulado.slv002());
        Deteccion.Resultado r = Deteccion.detectar(v46, f.de("00:21:13:AA:BB:01"), null, true);
        assertEquals(Protocolo.Firmware.F46, r.firmware);
        assertTrue(r.perfilCaducado);
        assertEquals("#V#", v46.recibidas.get(0));
        // Con lo aprendido (la ultima deteccion vio una V4.6), el SOLO_SONDA ya no se aplica sin pedirlo
        PerfilesEquipo.Aprendido ap = new PerfilesEquipo.Aprendido("00:21:13:AA:BB:01", Protocolo.Firmware.F46,
                r.huella, "t");
        EquipoSimuladoV46 v46b = new EquipoSimuladoV46(EquipoSimulado.slv002());
        assertEquals(Protocolo.Firmware.F46, Deteccion.detectar(v46b, f.de("00:21:13:AA:BB:01"), ap, false).firmware);
        // Perfil F36 con caduca_con_v = 4.6: caduca, no contradice
        EquipoSimuladoV46 v46c = new EquipoSimuladoV46(EquipoSimulado.slv002());
        Deteccion.Resultado c = Deteccion.detectar(v46c, f.de("00:21:13:AA:BB:02"), null, false);
        assertTrue(c.opera() && c.perfilCaducado && !c.contradicePerfil);
        // Aprendido: si #V# cambia, caduca y se dice
        PerfilesEquipo.Aprendido viejo = new PerfilesEquipo.Aprendido(MAC, Protocolo.Firmware.F36,
                "V,3.6,2026-09-18", "t");
        Deteccion.Resultado d = Deteccion.detectar(EquipoSimulado.slv002(), null, viejo, false);
        assertTrue(d.perfilCaducado);
        assertTrue(d.detalle.toString().contains("Perfil aprendido caducado"));
        // ida y vuelta del fichero de aprendidos
        Map<String, PerfilesEquipo.Aprendido> m = PerfilesEquipo.leerAprendidos(ap.linea() + "\n" + viejo.linea());
        assertEquals(2, m.size());
        assertEquals(Protocolo.Firmware.F46, PerfilesEquipo.leerAprendidos(PerfilesEquipo.escribirAprendidos(m))
                .get("00:21:13:AA:BB:01").firmware);
    }

    @Test
    public void losAssetsDeEquiposYFirmwaresSeLeen() throws Exception {
        String eq = new String(Files.readAllBytes(new File("src/main/assets/equipos.csv").toPath()), StandardCharsets.UTF_8);
        PerfilesEquipo.Firmados f = PerfilesEquipo.leerFirmados(eq);
        assertTrue(f.rechazados.isEmpty());
        assertTrue("sin perfiles hasta M-3", f.porMac.isEmpty());
        String fw = new String(Files.readAllBytes(new File("src/main/assets/firmwares.csv").toPath()), StandardCharsets.UTF_8);
        Map<Protocolo.Firmware, PerfilFirmware> m = PerfilFirmware.leer(fw);
        assertEquals(4, m.size());
        assertEquals("cola_banco_P1-P132.csv", m.get(Protocolo.Firmware.F36).colaBanco);
        // T-U16 (parcial): la coherencia en DEF de la V4.6 es por #E contra el literal, no #G a 1 ulp
        assertEquals("E", m.get(Protocolo.Firmware.F46).coherenciaDef);
        assertEquals("G", m.get(Protocolo.Firmware.F36).coherenciaDef);
        assertEquals(4095, m.get(Protocolo.Firmware.F46).xMax, 0);
        // sin cola de la V4.6, la V4.6 no calibra ni mide banco (A-5)
        assertFalse(new ProtocoloV46(m.get(Protocolo.Firmware.F46)).calibra());
    }

    // ------------------------------------------------------------------ T-U08 y T-U09

    @Test
    public void tU08ParserLeervEstricto() {
        assertNull(Tramas.extraer(Tramas.Tipo.LEERV, "@LEERV,BLA,1@", true));
        assertNull(Tramas.extraer(Tramas.Tipo.LEERV, Tramas.SONDA_V4, true));
        assertEquals(Integer.valueOf(127), Tramas.valorLeerv(Tramas.extraer(Tramas.Tipo.LEERV, "@LEERV,127@/n/r\0", true)));
        assertEquals(Integer.valueOf(5), Tramas.valorLeerv(Tramas.extraer(Tramas.Tipo.LEERV,
                "***** RETROREFLECTOMETRO VERTICAL *****\r\nOFFSET= 12.30\r\n\r\n@LEERV,5@/n/r", true)));
        assertNull(Tramas.extraer(Tramas.Tipo.LEERV, "@LEERV,127,45@", true));
        assertNull(Tramas.extraer(Tramas.Tipo.LEERV, "@LEERV,@", true));
    }

    @Test
    public void tU09RestosEntreRespuestas() throws Exception {
        EquipoSimuladoV4 v4 = new EquipoSimuladoV4();
        Ops o = new Ops(v4, new ProtocoloV4Original("F41"));
        v4.x = 2000;
        Integer r1 = o.medirR('2');
        v4.x = 3000;
        Integer r2 = o.medirR('2');
        assertNotNull(r1);
        assertNotNull(r2);
        assertTrue("la segunda es la suya, no un resto de la primera", r2 > r1);
        // el Receptor: tras vaciar, los restos "/n/r" + 0x00 no llegan al principio de la siguiente
        Receptor rx = new Receptor();
        rx.agregar(0, "@LEERV,127@/n/r\0".getBytes(StandardCharsets.ISO_8859_1));
        assertEquals("@LEERV,127@", rx.completa(Tramas.Tipo.LEERV, 500));
        rx.vaciar();
        rx.agregar(600, "@LEERV,5@/n/r\0".getBytes(StandardCharsets.ISO_8859_1));
        assertFalse(rx.texto().startsWith("/n/r"));
        assertEquals("@LEERV,5@", rx.completa(Tramas.Tipo.LEERV, 1000));
    }

    // ------------------------------------------------------------------ T-U10, T-U11

    @Test
    public void tU10PermitidaDelV4Original() {
        Protocolo p = new ProtocoloV4Original();
        int n = 0;
        for (char k : Fabrica.CODIGOS) {
            assertTrue(p.permitida(Tramas.tramaLeerv(k)));
            assertEquals(k, Tramas.claveDeLeerv(Tramas.tramaLeerv(k)));
            n++;
        }
        assertEquals(12, n);
        for (String t : Arrays.asList("#V#", "9", "6", "e", "@VERS@", "@?@", "@LEERV@", "@LEERV,BL,1@", "@LEERV,BLA,3@",
                "@LEERV,BLA,1@@", "@leerv,BLA,1@", "@LEERV,BLA,1@\r\n", "#GB#", "#X,1#", "")) {
            assertFalse(t, p.permitida(t));
        }
        // V3.6: nunca '@'; V3 2020: ni 'e' ni '#'; V4.6: @LEERV y '#', ningun byte suelto
        assertFalse(new ProtocoloV36().permitida(Tramas.SONDA_V4));
        assertTrue(new ProtocoloV36().permitida("e"));
        assertFalse(new ProtocoloV2020().permitida("e"));
        assertFalse(new ProtocoloV2020().permitida("#V#"));
        assertTrue(new ProtocoloV46().permitida("@LEERV,NAR,1@"));
        assertTrue(new ProtocoloV46().permitida("#X,d#"));
        assertFalse(new ProtocoloV46().permitida("e"));
        assertFalse(new ProtocoloV46().permitida("9"));
        assertFalse(new ProtocoloV46().permitida("#V,@#"));
    }

    @Test
    public void tU11SinEsV36NiVersionV4FueraDeProtocolo() throws IOException {
        File dir = new File("src/main/java/com/dpi/retrov36");
        File[] fs = dir.listFiles();
        assertNotNull(fs);
        List<String> mal = new ArrayList<>();
        for (File f : fs) {
            if (!f.getName().endsWith(".java") || f.getName().startsWith("Protocolo")) {
                continue;
            }
            String s = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
            if (s.contains("esV36") || s.contains("Version.V4") || s.contains("Version.V36")) {
                mal.add(f.getName());
            }
        }
        assertTrue(mal.toString(), mal.isEmpty());
    }

    // ------------------------------------------------------------------ T-U12 a T-U14, T-U17, T-U18

    @Test
    public void tU12V4OriginalMideYNoCalibra() throws Exception {
        EquipoSimuladoV4 v4 = new EquipoSimuladoV4();
        Protocolo p = new ProtocoloV4Original("F41");
        Ops o = new Ops(v4, p);
        for (char k : Fabrica.CODIGOS) {
            assertNotNull(k + "", o.medirR(k));
        }
        assertNull(o.medirX('1'));
        Bateria.Lectura b = o.bateria();
        assertTrue(b.bloqueaEscrituras);
        assertEquals(12, v4.recibidas.size());
        for (String t : v4.recibidas) {
            assertTrue(t, Tramas.esLeervValida(t));
        }
        assertFalse(p.calibra());
        assertFalse(p.mideBanco());
        assertFalse(p.administra());
        assertEquals("Equipo V4 sin firmware V4.6: sólo medir y verificar", p.motivoNoCalibra());
        // T-U17 (segun el encargo de la RTV 1.0, el V4 original no mide banco): el motivo lo dice
        assertTrue(p.motivoNoBanco().contains("Sin x no hay banco"));
        // el flujo de calibracion lo niega en las previas
        FlujoCalibracion.Contexto ctx = new FlujoCalibracion.Contexto();
        ctx.protocolo = p;
        assertFalse(ctx.protocolo.calibra());
    }

    @Test
    public void tU13EtiquetaDelTipo1() {
        assertEquals(ProtocoloV4Original.ETIQUETA_TIPO1_V41, new ProtocoloV4Original("F41").etiquetaMedida('7'));
        assertEquals(ProtocoloV4Original.ETIQUETA_TIPO1_SIN, new ProtocoloV4Original("").etiquetaMedida('d'));
        assertEquals("R entera", new ProtocoloV4Original("F41").etiquetaMedida('1'));
        assertTrue(Tramas.esTipo1('7') && !Tramas.esTipo1('6'));
        assertEquals("@LEERV,BLA,1@", Tramas.tramaLeerv('7'));
        assertEquals("@LEERV,NAR,2@", Tramas.tramaLeerv('6'));
        assertEquals("@LEERV,ROJ,1@", Tramas.tramaLeerv('b'));
    }

    @Test
    public void tU14DiarioDelEstadoOculto() throws Exception {
        DiarioEstadoOculto d = new DiarioEstadoOculto();
        assertEquals(DiarioEstadoOculto.DESCONOCIDO, d.anotar('2', 300, false));
        assertFalse(d.aviso().isEmpty());
        d.anotar('7', 200, false);
        assertEquals("previa: BLA,1 = 200", d.anotar('2', 301, false));
        d.reiniciar();
        assertTrue(d.aviso().isEmpty());
        // Con el V4.1 simulado: la misma AMA,2 cambia tras una BLA,1 (el error arrastrado)
        EquipoSimuladoV4 v4 = new EquipoSimuladoV4();
        Ops o = new Ops(v4, new ProtocoloV4Original("F41"));
        v4.x = 2500;
        Integer antes = o.medirR('2');
        o.medirR('7');
        Integer despues = o.medirR('2');
        assertFalse("el tipo 1 arrastra error", antes.equals(despues));
        // la sonda BLA,2 no lo toca
        EquipoSimuladoV4 v4b = new EquipoSimuladoV4();
        v4b.x = 2500;
        Ops ob = new Ops(v4b, new ProtocoloV4Original("F41"));
        Integer a1 = ob.medirR('2');
        ob.medirR('1');
        assertEquals(a1, ob.medirR('2'));
    }

    @Test
    public void tU18SerieDeclarada() {
        assertEquals(Deteccion.MARCA_DECLARADA, Deteccion.marcaSerie(new ProtocoloV36(), Calibracion.Variante.V361, null));
        assertEquals(Deteccion.MARCA_DECLARADA, Deteccion.marcaSerie(new ProtocoloV4Original(), Calibracion.Variante.DESCONOCIDA,
                null));
        assertEquals(Deteccion.MARCA_DECLARADA, Deteccion.marcaSerie(new ProtocoloV2020(), Calibracion.Variante.DESCONOCIDA,
                "SLV-002"));
        assertEquals("", Deteccion.marcaSerie(new ProtocoloV36(), Calibracion.Variante.V362, "SLV-002-2026"));
        assertEquals("", Deteccion.marcaSerie(new ProtocoloV46(), Calibracion.Variante.DESCONOCIDA, "SLV-003-2026"));
        assertEquals(Deteccion.MARCA_DECLARADA, Deteccion.marcaSerie(new ProtocoloV46(), Calibracion.Variante.DESCONOCIDA,
                "NONE"));
    }

    // ------------------------------------------------------------------ T-U20 y bateria de la V4.6

    @Test
    public void tU20V46EnBlanco() throws Exception {
        EquipoSimuladoV46 sim = EquipoSimuladoV46.enBlanco();
        Deteccion.Resultado r = Deteccion.detectar(sim, null, null, false);
        assertEquals(Protocolo.Firmware.F46, r.firmware);
        assertEquals("DEF", r.version.marca);
        assertEquals(0, r.version.mascara);
        assertEquals(Calibracion.NONE, r.serieEquipo);
        assertEquals(Calibracion.NONE, r.fechaCalibracion);
        assertEquals(Deteccion.MARCA_DECLARADA, Deteccion.marcaSerie(r.protocolo, r.variante, r.serieEquipo));
        for (String t : sim.recibidas) {
            assertFalse(t, t.startsWith("#S") || t.startsWith("#F"));
        }
        // renombrar (alta de serie) por el flujo de RF-APP-35, y se relee con #GN#
        Campana c = new Campana(new ArrayList<Patron>(), "SLV-003-2026", MAC);
        c.escribirEn(new java.io.StringWriter());
        String t = FlujoCalibracion.renombrarSerie(sim, c, EquipoSimulado.PIN_FABRICA, "SLV-003-2026", "SLV-003-2026",
                "Diego", "t");
        assertTrue(t, t.startsWith("Serie cambiada") || t.startsWith("El equipo ya tiene"));
        assertEquals("SLV-003-2026", sim.base.serie);
    }

    @Test
    public void laV46LeeXYBateriaConSusTramas() throws Exception {
        EquipoSimuladoV46 sim = new EquipoSimuladoV46(EquipoSimulado.slv002());
        sim.base.x = 1234.5;
        Ops o = new Ops(sim, new ProtocoloV46());
        assertEquals(1234.5, o.medirX('3'), 1e-3);
        assertNotNull(o.medirR('3'));
        Bateria.Lectura b = o.bateria();
        assertFalse(b.bloqueaEscrituras);
        assertTrue(b.texto, b.texto.contains("unidad por fijar"));
        assertEquals(Arrays.asList("#X,3#", "@LEERV,VER,2@", "#GB#"), sim.recibidas);
        // con la V3.6, las mismas llamadas usan 'e', el byte y '9'
        EquipoSimulado v36 = EquipoSimulado.slv002();
        Ops o36 = new Ops(v36, new ProtocoloV36());
        o36.medirX('3');
        o36.medirR('3');
        o36.bateria();
        assertEquals(Arrays.asList("e", "3", "9"), v36.recibidas);
        try {
            new ProtocoloV4Original().tramaBateria().length();
            fail();
        } catch (NullPointerException e) {
            // sin bateria en el V4 original
        }
    }
}
