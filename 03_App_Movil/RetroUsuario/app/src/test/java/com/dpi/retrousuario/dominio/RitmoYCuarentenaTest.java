package com.dpi.retrousuario.dominio;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * T-USR-24 — trama partida, respuesta tardía acotada a la cuarentena, plazo vencido, cuarentena y
 * doble `::` (TDD-V3.6.md §8, RF-USR-16). Fichas (a)-(e). Prueba a bajo nivel, sobre
 * {@link LectorDisparo} y {@link EmisorRitmo} directamente (sin {@link SerieDisparos}, que ya cubre
 * el enlace con RF-USR-04 en {@code SerieDisparosTest}).
 */
public class RitmoYCuarentenaTest {

    private final EquipoSimuladoDisparos sim = new EquipoSimuladoDisparos();
    private final ParametrosRitmo params = new ParametrosRitmo();

    /** (a) trama partida: "::1" llega, sin silencio; luego "23" completa "::123". */
    @Test
    public void tramaPartidaSeCompletaConElSegundoFragmento() {
        sim.programarFragmentos(50L, "::1", 120L, "23");
        sim.enviarByte('4');
        ResultadoDisparo r = LectorDisparo.leer(sim, params, 0);
        assertEquals(ResultadoDisparo.Tipo.VALOR, r.tipo());
        assertEquals(123, r.valor());
    }

    /** (b) respuesta tardía de N, dentro de [plazo, plazo+Q): se descarta en la cuarentena, no se asigna a nada. */
    @Test
    public void respuestaTardiaDentroDeLaCuarentenaSeDescarta() {
        long tardio = params.plazoDisparoMs() + params.cuarentenaMs() / 2; // dentro de [plazo, plazo+Q).
        sim.programarFragmentos(tardio, "::110");
        sim.enviarByte('4');
        ResultadoDisparo anulado = LectorDisparo.leer(sim, params, 0);
        assertEquals(ResultadoDisparo.Tipo.ANULADO_PLAZO, anulado.tipo());
        assertEquals(params.plazoDisparoMs(), anulado.tFin());

        List<ResultadoDisparo.ByteRecibido> descartados = new EmisorRitmo().cuarentena(sim, params, anulado.tFin());
        String texto = textoDe(descartados);
        assertEquals("::110", texto);
        assertEquals(1, sim.tramasRecibidas().size()); // ningun otro byte se envio mientras duro la cuarentena.
    }

    /** (c) plazo vencido sin respuesta alguna: se anula por plazo, no se cuenta como "0". */
    @Test
    public void plazoVencidoSinNadaSeAnulaPorPlazo() {
        sim.programarSinRespuesta();
        sim.enviarByte('4');
        ResultadoDisparo r = LectorDisparo.leer(sim, params, 0);
        assertEquals(ResultadoDisparo.Tipo.ANULADO_PLAZO, r.tipo());
        assertEquals(params.plazoDisparoMs(), r.tFin());
        assertTrue(r.recibidos().isEmpty());
    }

    /** (d) doble "::" en la misma ventana: se descartan las dos, cuenta como anulado y abre cuarentena;
     *  una "::" adicional dentro de Q ms tambien se descarta (nuevo r6, C3). */
    @Test
    public void dobleRespuestaSeAnulaYAbreCuarentenaQueDescartaLoQueLlegueDespues() {
        sim.programarFragmentos(50L, "::100::110", 1200L, "::999");
        sim.enviarByte('4');
        ResultadoDisparo r = LectorDisparo.leer(sim, params, 0);
        assertEquals(ResultadoDisparo.Tipo.ANULADO_DOBLE, r.tipo());

        List<ResultadoDisparo.ByteRecibido> descartados = new EmisorRitmo().cuarentena(sim, params, r.tFin());
        assertEquals("::999", textoDe(descartados));
    }

    /** (e) cuarentena: todo lo que llega durante Q ms se descarta, sin excepcion, y no se envia nada mientras dura. */
    @Test
    public void cuarentenaDescartaTodoSinEnviarNadaMientrasDura() {
        sim.programarSinRespuesta();
        sim.enviarByte('4');
        ResultadoDisparo r = LectorDisparo.leer(sim, params, 0);
        long tInicio = r.tFin();
        List<ResultadoDisparo.ByteRecibido> descartados = new EmisorRitmo().cuarentena(sim, params, tInicio);
        assertTrue(descartados.isEmpty()); // nada programado: cuarentena vacia, pero corre hasta el limite sin enviar.
        assertEquals(1, sim.tramasRecibidas().size());
    }

    /** A3: "::12" llega antes del plazo pero sin silencio confirmado (180 ms) antes de que venza el
     *  plazo total; el "3" que lo habría completado a "::123" llega ya vencido el plazo. El disparo
     *  se anula por plazo (no se lee "12" como si fuera un valor completo), y el "3" tardío se
     *  descarta en la cuarentena que sigue y queda anotado (ByteRecibido devuelto por cuarentena). */
    @Test
    public void bytesSinSilencioConfirmadoAntesDelPlazoSeAnulanAunqueParezcanCompletos() {
        sim.programarFragmentos(2400L, "::12", 2600L, "3");
        sim.enviarByte('4');
        ResultadoDisparo r = LectorDisparo.leer(sim, params, 0);
        assertEquals(ResultadoDisparo.Tipo.ANULADO_PLAZO, r.tipo());
        assertEquals(params.plazoDisparoMs(), r.tFin());

        List<ResultadoDisparo.ByteRecibido> descartados = new EmisorRitmo().cuarentena(sim, params, r.tFin());
        assertEquals("3", textoDe(descartados)); // lo descartado en la pausa queda anotado (tramas.log lo hace via SerieDisparos).
    }

    private static String textoDe(List<ResultadoDisparo.ByteRecibido> bytes) {
        StringBuilder sb = new StringBuilder();
        for (ResultadoDisparo.ByteRecibido b : bytes) {
            sb.append((char) b.valor);
        }
        return sb.toString();
    }
}
