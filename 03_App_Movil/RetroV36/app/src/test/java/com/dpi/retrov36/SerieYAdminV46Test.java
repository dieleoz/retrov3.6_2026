package com.dpi.retrov36;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * RTV 1.0.0-rc5. El SLV-003-2026, grabado con la V4.6 y respondiendo por Bluetooth, no dejaba medir: ni
 * aparecia el banco REPRESENTATIVO ni el boton de medir arrancaba. Esta clase fija las dos causas y el
 * arreglo.
 *
 * <p><b>Cobertura (de donde sale cada valor esperado, CLAUDE.md §7):</b>
 *
 * <ul>
 *   <li><b>Aseveran un requisito</b> (el valor viene de fuera del codigo): los siete primeros. "HC-06" sale
 *       de {@code V4.6:01_Firmware/RetroVertical_V4.6.X/CAMBIOS-V4.6.md:259-261} (al equipo le cambiaron el
 *       modulo Bluetooth y el de repuesto se anuncia con su nombre de fabrica); {@code #GN,NONE#} sale del
 *       registro de campo del 19-sep-2026 a las 18:53; "SLV-003-2026" es la decision SERIE-2 de Diego
 *       ({@code 06_Calibracion/SLV-002/DECISIONES-Diego-2026-09-19.md:22}); que la V4.6 tiene {@code #L},
 *       {@code #SN}, {@code #GN}, {@code #P} y {@code #Q} sale de la tabla unica de tramas
 *       ({@code V4.6:05_Documentacion/PROTOCOLO-V4.6-BORRADOR.md} §4.4), que es administracion y no
 *       calibracion; y que un V3 de 2020 no tiene ordenes {@code #...#} sale de
 *       {@code V3.6:05_Documentacion/PROTOCOLO-V3.6.md} y del barrido de 255 bytes del 18-sep.</li>
 *   <li><b>Fijan comportamiento</b> (el valor sale del propio codigo; detectan regresiones, no demuestran
 *       que el comportamiento sea el correcto): los tres ultimos, sobre el saneado de lo tecleado.</li>
 * </ul>
 *
 * <p><b>Nada de esto se ha probado contra un equipo fisico ni en un telefono.</b> Las dos pantallas que
 * cambian ({@code BancoActivity}, {@code AdminActivity}) son Activities y no corren en la JVM: lo que se
 * asevera aqui es la regla pura que ellas consultan.
 */
public class SerieYAdminV46Test {

    /** Nombre Bluetooth del modulo de repuesto del SLV-003-2026: el de fabrica, sin serie y sin '_'. */
    private static final String BT_REPUESTO = "HC-06";
    private static final String MAC = "00:21:13:05:19:3C";
    /** Decision SERIE-2 de Diego. */
    private static final String SERIE = "SLV-003-2026";

    private static String texto(String n) throws Exception {
        return new String(Files.readAllBytes(new File("src/main/assets/" + n).toPath()), StandardCharsets.UTF_8);
    }

    /** El protocolo de la V4.6 con el perfil real del APK (firmwares.csv trae su cola). */
    private static ProtocoloV46 v46() throws Exception {
        Map<Protocolo.Firmware, PerfilFirmware> m = PerfilFirmware.leer(texto(PerfilFirmware.ASSET));
        return new ProtocoloV46(PerfilFirmware.de(m, Protocolo.Firmware.F46));
    }

    /** Sesion como la del 19-sep a las 18:53: V4.6 detectada, #GN,NONE#, nombre BT sin serie. */
    private static Sesion sesionDeCampo() throws Exception {
        Sesion s = Sesion.get();
        s.reiniciar(BT_REPUESTO, MAC);
        s.protocolo = v46();
        s.detectado = true;
        s.fechaFirmware = "2026-09-19c";
        s.marca = "DEF";
        s.mascara = 0x0000;
        s.serieEquipo = Calibracion.NONE;
        s.fechaCalibracion = Calibracion.NONE;
        return s;
    }

    @Before
    public void limpiar() {
        Sesion.get().reiniciar("", "");
    }

    @After
    public void limpiarDespues() {
        Sesion.get().reiniciar("", "");
    }

    // ------------------------------------------------- aseveran un requisito

    /**
     * La causa. Las tres vias de {@code serieConocida()} fallan a la vez: #GN# dice NONE, no hay fila en
     * equipos.csv con esa MAC y el nombre Bluetooth del modulo de repuesto no trae serie.
     */
    @Test
    public void elSlv003ConModuloDeRepuestoNoTieneSerieConocida() throws Exception {
        Sesion s = sesionDeCampo();
        assertFalse("con #GN,NONE# y un nombre BT sin serie, la app no sabe de que equipo es la campana",
                s.serieConocida());
        assertTrue("y por eso hay que pedirsela al operador donde esta: en Tomar muestras", s.hayQuePedirSerie());
    }

    /** equipos.csv no declara ninguna serie: no hay tercera via que salve el caso. */
    @Test
    public void equiposCsvNoDeclaraNingunaSerie() throws Exception {
        PerfilesEquipo.Firmados f = PerfilesEquipo.leerFirmados(texto(PerfilesEquipo.ASSET));
        assertEquals("equipos.csv sigue sin filas: ninguna MAC trae serie declarada", null, f.de(MAC));
    }

    /** Con la serie tecleada, la campana ya tiene nombre y el banco puede abrirse. */
    @Test
    public void laSerieTecleadaDesbloqueaElBanco() throws Exception {
        Sesion s = sesionDeCampo();
        assertTrue(s.aceptarSerieTecleada(SERIE));
        assertFalse("ya no hay que preguntar nada", s.hayQuePedirSerie());
        assertTrue(s.serieConocida());
        assertEquals(SERIE, s.serie());
        assertEquals("y queda marcada como declarada, no leida del equipo",
                Deteccion.MARCA_DECLARADA, s.marcaSerie());
    }

    /**
     * La segunda causa: la V4.6 <b>administra</b> (#L, #SN, #GN, #P, #Q de la tabla §4.4) aunque su
     * calibracion siga candada. Gobernar el modo administrador con calibra() cerraba de paso la unica via de
     * darle serie a un equipo que responde NONE.
     */
    @Test
    public void laV46AdministraAunqueNoCalibre() throws Exception {
        Protocolo p = v46();
        assertTrue("la V4.6 habla el contrato #...#", p.administra());
        assertFalse("y su calibracion sigue candada (decision de la rc2)", p.calibra());
        assertTrue("pero el banco si se mide: firmwares.csv trae su cola", p.mideBanco());
        for (String t : new String[]{"#L,2026#", "#SN," + SERIE + "#", "#GN#", "#Q#"}) {
            assertTrue(t + " tiene que poder enviarse a una V4.6", p.permitida(t));
        }
    }

    /** La puerta del modo administrador la gobierna administra(); la calibracion, calibra(). */
    @Test
    public void laPuertaDeAdminLaGobiernaAdministra() throws Exception {
        Sesion s = sesionDeCampo();
        assertTrue("con una V4.6 se entra al modo administrador y se da de alta la serie", s.puedeAdmin());
        assertFalse("y la calibracion sigue cerrada", s.puedeCalibrar());
    }

    /** Lo que no puede cambiar: a un V3 de 2020 y a un V4 original no se les abre ninguna puerta nueva. */
    @Test
    public void elV32020YElV4OriginalSiguenSinModoAdministrador() {
        Sesion s = Sesion.get();
        s.reiniciar("SLV-002", MAC);
        s.protocolo = new ProtocoloV2020();
        assertFalse("un V3 de 2020 no tiene ordenes #...#", s.puedeAdmin());
        assertFalse(s.puedeCalibrar());
        s.protocolo = new ProtocoloV4Original();
        assertFalse("un V4 original tampoco", s.puedeAdmin());
        assertFalse(s.puedeCalibrar());
    }

    /** SLV-002 (Coviandina) no cambia de comportamiento: ni pierde permisos ni ve el dialogo nuevo. */
    @Test
    public void laV36YElSlv002NoCambian() throws Exception {
        Sesion s = Sesion.get();
        s.reiniciar("COVIANDINA_SLV-002", MAC);
        Map<Protocolo.Firmware, PerfilFirmware> m = PerfilFirmware.leer(texto(PerfilFirmware.ASSET));
        s.protocolo = new ProtocoloV36(PerfilFirmware.de(m, Protocolo.Firmware.F36));
        assertTrue(s.puedeAdmin());
        assertTrue(s.puedeCalibrar());
        assertEquals("SLV-002", s.serie());
        assertFalse("su nombre Bluetooth trae la serie: el dialogo nuevo no le aparece nunca",
                s.hayQuePedirSerie());
    }

    /**
     * Si se cae el Bluetooth a mitad del banco, la serie tecleada no se vuelve a pedir: la reconexion es al
     * mismo equipo. Hasta la rc4, {@code reiniciar()} la borraba siempre.
     */
    @Test
    public void laSerieTecleadaSobreviveAUnaReconexionAlMismoEquipo() throws Exception {
        Sesion s = sesionDeCampo();
        assertTrue(s.aceptarSerieTecleada(SERIE));
        s.reiniciar(BT_REPUESTO, MAC);          // se cayo el enlace y se vuelve a conectar
        s.protocolo = v46();
        s.serieEquipo = Calibracion.NONE;
        assertEquals("no hay que volver a teclearla", SERIE, s.serie());
        assertFalse(s.hayQuePedirSerie());
    }

    /**
     * Pero con otro equipo se olvida: la calibracion es por equipo fisico y nunca se mezcla (L-23,
     * {@code V3.6:README.md:20}).
     */
    @Test
    public void alConectarOtroEquipoLaSerieSeOlvida() throws Exception {
        Sesion s = sesionDeCampo();
        assertTrue(s.aceptarSerieTecleada(SERIE));
        s.reiniciar(BT_REPUESTO, "00:21:13:05:19:3B");
        s.protocolo = v46();
        s.serieEquipo = Calibracion.NONE;
        assertTrue("otro equipo: se vuelve a preguntar", s.hayQuePedirSerie());
        assertFalse(SERIE.equals(s.serie()));
    }

    // ------------------------------------------------------ fijan comportamiento

    @Test
    public void unaSerieVaciaOEnBlancoNoSeAcepta() throws Exception {
        Sesion s = sesionDeCampo();
        assertFalse(s.aceptarSerieTecleada(""));
        assertFalse(s.aceptarSerieTecleada("   "));
        assertFalse(s.aceptarSerieTecleada(null));
        assertTrue("nada ha cambiado", s.hayQuePedirSerie());
    }

    @Test
    public void laSerieSeGuardaSinEspaciosAlrededor() throws Exception {
        Sesion s = sesionDeCampo();
        assertTrue(s.aceptarSerieTecleada("  " + SERIE + "  "));
        assertEquals(SERIE, s.serie());
    }

    @Test
    public void sinEquipoConectadoNoSePideLaSerie() {
        Sesion s = Sesion.get();
        s.reiniciar(BT_REPUESTO, "");
        assertFalse("sin MAC manda el aviso de conectar primero, no el de la serie", s.hayQuePedirSerie());
    }
}
