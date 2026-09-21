package com.dpi.retrousuario.dominio;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * T-USR-01c (SPEC-App-Usuario-V3.6.md r6, RF-USR-01, M-3): las dos ramas de {@code exigir_362}, y
 * los 2 reintentos de #GN#/#GC# cuando no contestan nada (3 intentos en total, "sonda original más 2
 * reintentos", SPEC :568). Usa el overload de 3 argumentos de {@link Sonda362#sondear} para no
 * dormir 150 ms reales por cada prueba (M5): la pausa se captura en una lista en vez de dormir.
 */
public class Sonda362ExigirTest {

    private final List<Long> pausas = new ArrayList<>();

    private Sonda362.ResultadoSonda sondear(Canal canal, ParametrosRitmo params) {
        return Sonda362.sondear(canal, params, pausas::add);
    }

    /** exigir_362 = true (por defecto) y #GN#/#GC# responden "#ERR,FORMATO#": no mide, ACTUALIZAR_FIRMWARE. */
    @Test
    public void exigir362VerdaderoConErrFormatoPideActualizarYNoMide() {
        EquipoSimulado equipo = new EquipoSimulado()
                .responde("#GN#", "#ERR,FORMATO#")
                .responde("#GC#", "#ERR,FORMATO#");
        ParametrosRitmo params = new ParametrosRitmo();
        assertTrue(params.exigir362()); // por defecto.

        Sonda362.ResultadoSonda r = sondear(equipo, params);

        assertEquals(Sonda362.Resultado.ACTUALIZAR_FIRMWARE, r.resultado());
    }

    /** exigir_362 = false con las MISMAS respuestas "#ERR,FORMATO#": mide igual, sin serie ni fecha. */
    @Test
    public void exigir362FalsoConErrFormatoMideIgualConSinFechaYSerieNinguna() {
        EquipoSimulado equipo = new EquipoSimulado()
                .responde("#GN#", "#ERR,FORMATO#")
                .responde("#GC#", "#ERR,FORMATO#");
        ParametrosRitmo params = new ParametrosRitmo();
        params.exigir362(false);

        Sonda362.ResultadoSonda r = sondear(equipo, params);

        assertEquals(Sonda362.Resultado.OK, r.resultado());
        assertFalse(r.serieLeida());
        assertEquals("", r.serie());
        assertEquals("ninguna", r.origenSerie());
        assertFalse(r.fechaRegistrada());
    }

    /** exigir_362 = false con silencio puro en las dos: también mide igual (no sólo con ERR,FORMATO explícito). */
    @Test
    public void exigir362FalsoConSilencioTambienMideIgual() {
        EquipoSimulado equipo = new EquipoSimulado(); // #GN#/#GC# sin configurar: silencio.
        ParametrosRitmo params = new ParametrosRitmo();
        params.exigir362(false);

        Sonda362.ResultadoSonda r = sondear(equipo, params);

        assertEquals(Sonda362.Resultado.OK, r.resultado());
        assertFalse(r.serieLeida());
        assertFalse(r.fechaRegistrada());
    }

    /** exigir_362 = false con GN legible y GC en ERR,FORMATO: se queda con lo que sí pudo leer. */
    @Test
    public void exigir362FalsoConGnLegibleYGcErrFormatoConservaLoQueSiSeLeyo() {
        EquipoSimulado equipo = new EquipoSimulado()
                .responde("#GN#", "#GN,SLV-002#")
                .responde("#GC#", "#ERR,FORMATO#");
        ParametrosRitmo params = new ParametrosRitmo();
        params.exigir362(false);

        Sonda362.ResultadoSonda r = sondear(equipo, params);

        assertEquals(Sonda362.Resultado.OK, r.resultado());
        assertTrue(r.serieLeida());
        assertEquals("SLV-002", r.serie());
        assertFalse(r.fechaRegistrada());
    }

    /** RF-USR-01, reintentos: silencio puro con exigir_362 = true agota 2 reintentos por campo (3 intentos
     *  en total cada uno, 6 en total) antes de dar la sonda por "sin respuesta útil". */
    @Test
    public void silencioAgotaLosDosReintentosPorCampoAntesDeRendirse() {
        EquipoSimulado equipo = new EquipoSimulado(); // silencio puro en las dos.
        ParametrosRitmo params = new ParametrosRitmo();
        assertEquals(2, params.maxReintentosSonda());

        Sonda362.ResultadoSonda r = sondear(equipo, params);

        assertEquals(Sonda362.Resultado.SIN_RESPUESTA_REINTENTAR, r.resultado());
        List<String> esperado = Arrays.asList("#GN#", "#GN#", "#GN#", "#GC#", "#GC#", "#GC#");
        assertEquals(esperado, equipo.tramasRecibidas()); // 3 intentos de GN, luego 3 de GC (M5: en ese orden).
    }

    /** RF-USR-01, reintentos: si el SEGUNDO intento de #GN# contesta, no se gasta el tercero. */
    @Test
    public void unaRespuestaEnElSegundoIntentoNoGastaElTercero() {
        EquipoSimuladoConSilencioLosPrimeros equipo =
                new EquipoSimuladoConSilencioLosPrimeros("#GN#", 1, "#GN,SLV-002#", "#GC#", "#GC,2026-09-19#");
        ParametrosRitmo params = new ParametrosRitmo();

        Sonda362.ResultadoSonda r = sondear(equipo, params);

        assertEquals(Sonda362.Resultado.OK, r.resultado());
        assertTrue(r.serieLeida());
        assertEquals("SLV-002", r.serie());
        assertEquals(2, equipo.enviosDe("#GN#")); // 1 silencio + la respuesta: no se gasta un segundo reintento.
    }

    /** M5: se pausan 150 ms exactamente una vez, entre el envío de #GN# y el de #GC# (no antes, no después). */
    @Test
    public void sePausan150MsEntreGnYGc() {
        EquipoSimulado equipo = new EquipoSimulado()
                .responde("#GN#", "#GN,SLV-002#")
                .responde("#GC#", "#GC,2026-09-19#");
        ParametrosRitmo params = new ParametrosRitmo();

        sondear(equipo, params);

        assertEquals(Arrays.asList(150L), pausas);
    }

    /** Canal propio (no reutiliza {@link EquipoSimulado}, que es {@code final}): responde silencio
     *  (null) las primeras {@code vecesASilenciar} veces que llega {@code tramaConSilencioInicial}, y
     *  desde entonces {@code respuestaTrasSilencio}; {@code otraTrama} siempre responde {@code otraRespuesta}. */
    private static final class EquipoSimuladoConSilencioLosPrimeros implements Canal {
        private final String tramaConSilencioInicial;
        private final int vecesASilenciar;
        private final String respuestaTrasSilencio;
        private final String otraTrama;
        private final String otraRespuesta;
        private final java.util.Map<String, Integer> envios = new java.util.HashMap<>();

        EquipoSimuladoConSilencioLosPrimeros(String tramaConSilencioInicial, int vecesASilenciar,
                String respuestaTrasSilencio, String otraTrama, String otraRespuesta) {
            this.tramaConSilencioInicial = tramaConSilencioInicial;
            this.vecesASilenciar = vecesASilenciar;
            this.respuestaTrasSilencio = respuestaTrasSilencio;
            this.otraTrama = otraTrama;
            this.otraRespuesta = otraRespuesta;
        }

        @Override
        public String enviar(String trama, long plazoMs) {
            envios.merge(trama, 1, Integer::sum);
            if (trama.equals(tramaConSilencioInicial)) {
                return envios.get(trama) <= vecesASilenciar ? null : respuestaTrasSilencio;
            }
            if (trama.equals(otraTrama)) {
                return otraRespuesta;
            }
            return null;
        }

        int enviosDe(String trama) {
            return envios.getOrDefault(trama, 0);
        }
    }
}
