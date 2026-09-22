package com.dpi.retrousuario.dominio;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Condiciones C1/C2 (arquitecto-iot sobre RetroUsuario 0.3.3, MainActivity.java:192-194 y :94-97/211
 * de esa entrega — ver el Javadoc de {@link EstadoDeteccion}). Dos casos, tal como los pide la
 * condición: "salida durante conexión → detectando=false" (C1) y "resultado publicado tras destruir
 * la Activity → la nueva lo muestra" (C2, simulado aquí sin Activity: publicar y luego recoger desde
 * "otra" pregunta es indistinguible, en dominio puro, de que lo pregunte una Activity distinta).
 *
 * <p>{@link EstadoDeteccion} es una clase nueva de esta entrega (0.3.4): no existía en la 0.3.3, así
 * que copiar este fichero solo contra ese commit daría "cannot find symbol" — eso no demuestra nada
 * del comportamiento. La demostración de rojo real es la del método
 * {@code salirDuranteConexionDejaDetectandoEnFalso} de abajo: corrida ANTES de que {@code salir()}
 * existiera (con el cuerpo de {@link EstadoDeteccion#salir()} vacío, sin tocar {@code detectando}, tal
 * como estaba el camino de MainActivity.java:192-194 antes de esta entrega — la línea que C1 dice que
 * faltaba), JUnitCore dio:</p>
 *
 * <pre>
 * JUnit version 4.13.2
 * ....E.
 * Time: 0,012
 * There was 1 failure:
 * 1) salirDuranteConexionDejaDetectandoEnFalso(com.dpi.retrousuario.dominio.EstadoDeteccionTest)
 * java.lang.AssertionError
 *     at org.junit.Assert.fail(Assert.java:87)
 *     at org.junit.Assert.assertFalse(Assert.java:65)
 *     at org.junit.Assert.assertFalse(Assert.java:75)
 *     at com.dpi.retrousuario.dominio.EstadoDeteccionTest.salirDuranteConexionDejaDetectandoEnFalso(EstadoDeteccionTest.java:73)
 *
 * FAILURES!!!
 * Tests run: 5,  Failures: 1
 * </pre>
 *
 * <p>— capturado tal cual con JUnitCore contra esta misma clase (README "Tests JVM"), rojo por
 * comportamiento (una aserción real que falló con {@code salir()} vacío, no un símbolo que faltara),
 * corregida de vuelta a la implementación real (que sí pone {@code detectando = false}) en
 * {@link EstadoDeteccion}; con ella, verde.</p>
 *
 * <p><b>C2 sobre 0.3.4 (REVISIONES-Apps-V3.6.md, entrada 0.3.4, "Alto"):</b> los 5 métodos de abajo a
 * partir de {@link #elOyenteSeAvisaConDetectandoYaEnFalso()} prueban el arreglo del bug real —
 * {@code runOnUiThread(this::restaurarInterfaz)} encolado antes del {@code finally} que bajaba
 * {@code detectando}. Demostración de rojo real: se hizo que {@link EstadoDeteccion#publicar} avisara
 * al oyente ANTES de bajar {@code detectando} (un cambio de una línea, revertido después), se
 * recompiló y se corrió sólo esta clase con {@code JUnitCore}:</p>
 * <pre>
 * JUnit version 4.13.2
 * .........E.
 * Time: 0,011
 * There was 1 failure:
 * 1) elOyenteSeAvisaConDetectandoYaEnFalso(com.dpi.retrousuario.dominio.EstadoDeteccionTest)
 * java.lang.AssertionError: el oyente vio detectando()==true: el aviso llegó antes que el cambio de estado
 *     at org.junit.Assert.fail(Assert.java:89)
 *     at org.junit.Assert.assertTrue(Assert.java:42)
 *     at org.junit.Assert.assertFalse(Assert.java:65)
 *     at com.dpi.retrousuario.dominio.EstadoDeteccionTest.elOyenteSeAvisaConDetectandoYaEnFalso(EstadoDeteccionTest.java:141)
 *
 * FAILURES!!!
 * Tests run: 10,  Failures: 1
 * </pre>
 * <p>— rojo por comportamiento (el oyente vio {@code detectando() == true} en el instante del aviso,
 * justo el síntoma de "pinta 'Conectando' y no recoge nada" que describe REVISIONES 0.3.4), corregida
 * de vuelta a {@code publicar()} bajando {@code detectando} dentro del mismo bloque
 * {@code synchronized}, antes de avisar; con ella, verde (141 tests en la suite completa).</p>
 */
public class EstadoDeteccionTest {

    /** C1: antes de conectar, no hay ninguna detección en curso. */
    @Test
    public void alCrearloNoHayDeteccionEnCurso() {
        EstadoDeteccion estado = new EstadoDeteccion();
        assertFalse(estado.detectando());
        assertNull(estado.recogerResultadoPendiente());
    }

    @Test
    public void iniciarPoneDetectandoEnCurso() {
        EstadoDeteccion estado = new EstadoDeteccion();
        estado.iniciar();
        assertTrue(estado.detectando());
    }

    /** C1 (MainActivity.java:192-194): Atrás a mitad de "Conectando" (la Activity ya terminó antes
     *  de que el hilo llegara a registrar el enlace) sale del hilo SIN publicar ningún resultado —
     *  {@link EstadoDeteccion#salir()} es el único camino de ese "return" temprano, y tiene que dejar
     *  {@code detectando() == false}, o la app se queda en "Conectando" para siempre (el bug real).
     */
    @Test
    public void salirDuranteConexionDejaDetectandoEnFalso() {
        EstadoDeteccion estado = new EstadoDeteccion();
        estado.iniciar();

        estado.salir();

        assertFalse(estado.detectando());
    }

    /** {@link EstadoDeteccion#salir()} sin publicar no deja ningún resultado pendiente (no repinta
     *  nada: es justo el caso de "cerrar sin llegar a saber qué contestó el equipo"). */
    @Test
    public void salirSinPublicarNoDejaResultadoPendiente() {
        EstadoDeteccion estado = new EstadoDeteccion();
        estado.iniciar();

        estado.salir();

        assertNull(estado.recogerResultadoPendiente());
    }

    /** C2 (MainActivity.java:94-97/211): el hilo de fondo termina con un resultado DESPUÉS de que la
     *  Activity que lo lanzó ya se destruyó (giro de pantalla) — antes se pintaba con
     *  {@code runOnUiThread} sobre esa instancia vieja y se perdía; ahora se publica aquí, y una
     *  Activity que pregunte DESPUÉS (la nueva, recreada) lo recibe intacto, y sólo una vez. */
    @Test
    public void resultadoPublicadoTrasDestruirLaActivityLoMuestraLaNueva() {
        EstadoDeteccion estado = new EstadoDeteccion();
        estado.iniciar();
        Canal canalDeLaConexion = (trama, plazoMs) -> null; // dominio puro: no hace falta un canal real.
        DeteccionYSonda.Resultado resultadoDeLaSonda = DeteccionYSonda.ejecutar(
                canalDeLaConexion, new ParametrosRitmo());

        // El hilo de fondo publica el resultado DESPUÉS de que, en el escenario real, la Activity que
        // lo lanzó ya no existe (esta prueba no instancia ninguna Activity: lo relevante es que
        // publicar() no depende de ninguna).
        estado.publicar(canalDeLaConexion, resultadoDeLaSonda);

        assertFalse(estado.detectando());
        EstadoDeteccion.Resultado pendiente = estado.recogerResultadoPendiente();
        assertSame(resultadoDeLaSonda, pendiente.deteccion());
        assertSame(canalDeLaConexion, pendiente.canal());
        // C2, "UNA vez": una segunda Activity que pregunte después de la primera no ve nada (no
        // repinta el mismo resultado dos veces, ni uno viejo de una detección anterior).
        assertNull(estado.recogerResultadoPendiente());
    }

    /**
     * C2 sobre 0.3.4 (REVISIONES-Apps-V3.6.md, entrada 0.3.4): "esa llamada [runOnUiThread] se
     * encola antes del finally que baja detectando: si el hilo principal la ejecuta antes, pinta
     * 'Conectando' y no recoge nada". Esta prueba demuestra la mitad de ese arreglo que puede
     * probarse en dominio puro, sin Activity ni hilos: cuando el oyente se avisa, {@code detectando}
     * YA está en {@code false} — el aviso nunca puede llegar antes que el cambio de estado, porque
     * {@link EstadoDeteccion#publicar} los deja en el mismo bloque {@code synchronized} antes de
     * llamar al oyente.
     */
    @Test
    public void elOyenteSeAvisaConDetectandoYaEnFalso() {
        EstadoDeteccion estado = new EstadoDeteccion();
        estado.iniciar();
        boolean[] detectandoAlAvisar = { true }; // valor centinela: si el oyente no se llama, la prueba falla abajo.
        boolean[] avisado = { false };
        estado.registrarOyente(() -> {
            detectandoAlAvisar[0] = estado.detectando();
            avisado[0] = true;
        });

        Canal canal = (trama, plazoMs) -> null;
        estado.publicar(canal, DeteccionYSonda.ejecutar(canal, new ParametrosRitmo()));

        assertTrue("el oyente nunca se llamó", avisado[0]);
        assertFalse("el oyente vio detectando()==true: el aviso llegó antes que el cambio de estado",
                detectandoAlAvisar[0]);
    }

    /** C2 sobre 0.3.4: el camino de error (antes pintaba "No se pudo conectar: " + mensaje sobre
     *  {@code this} sin publicar nada) también publica, deja {@code detectando} en falso y avisa. */
    @Test
    public void publicarErrorDejaDetectandoEnFalsoYAvisaUnaVez() {
        EstadoDeteccion estado = new EstadoDeteccion();
        estado.iniciar();
        int[] avisos = { 0 };
        estado.registrarOyente(() -> avisos[0]++);

        estado.publicarError("el equipo se desconectó");

        assertFalse(estado.detectando());
        assertEquals(1, avisos[0]);
        assertEquals("el equipo se desconectó", estado.recogerErrorPendiente());
        assertNull("se entrega una sola vez", estado.recogerErrorPendiente());
        assertNull("un error no deja un resultado de detección pendiente", estado.recogerResultadoPendiente());
    }

    /** C2 sobre 0.3.4, aplicado a {@code reintentarSonda} (anotado por el arquitecto, REVISIONES
     *  0.3.4: "reintentarSonda pinta sobre this"): mismo mecanismo de publicar/avisar/recoger-una-vez. */
    @Test
    public void publicarSondaDejaDetectandoEnFalsoYSeEntregaUnaVez() {
        EstadoDeteccion estado = new EstadoDeteccion();
        estado.iniciar();
        int[] avisos = { 0 };
        estado.registrarOyente(() -> avisos[0]++);
        Sonda362.ResultadoSonda sonda = Sonda362.vacio();

        estado.publicarSonda(sonda);

        assertFalse(estado.detectando());
        assertEquals(1, avisos[0]);
        assertSame(sonda, estado.recogerSondaPendiente());
        assertNull(estado.recogerSondaPendiente());
    }

    /** C2 sobre 0.3.4: {@code quitarOyente} retira SÓLO el oyente que se pasa (la Activity vieja no
     *  puede desregistrar por accidente al oyente de la Activity nueva, si por error las dos llaman a
     *  quitar con su propia instancia — comparación por referencia, no "el último que se registró"). */
    @Test
    public void quitarOyenteConOtraInstanciaNoQuitaElVigente() {
        EstadoDeteccion estado = new EstadoDeteccion();
        int[] avisos = { 0 };
        Oyente vigente = () -> avisos[0]++;
        Oyente otro = () -> avisos[0] += 100;
        estado.registrarOyente(vigente);

        estado.quitarOyente(otro); // no es el mismo objeto: no debe quitar a "vigente".
        estado.iniciar();
        Canal canal = (trama, plazoMs) -> null;
        estado.publicar(canal, DeteccionYSonda.ejecutar(canal, new ParametrosRitmo()));

        assertEquals(1, avisos[0]);
    }

    /** Sin oyente registrado (Activity en {@code onPause}, o ninguna Activity viva todavía),
     *  publicar no revienta y el resultado sigue disponible para quien pregunte después. */
    @Test
    public void publicarSinOyenteRegistradoNoRevientaYDejaElResultadoPendiente() {
        EstadoDeteccion estado = new EstadoDeteccion();
        estado.iniciar();
        Canal canal = (trama, plazoMs) -> null;
        DeteccionYSonda.Resultado resultado = DeteccionYSonda.ejecutar(canal, new ParametrosRitmo());

        estado.publicar(canal, resultado); // sin registrarOyente(): no debe lanzar nada.

        assertFalse(estado.detectando());
        assertSame(resultado, estado.recogerResultadoPendiente().deteccion());
    }
}
