/*
 * Pruebas de la app corta "RTV Calibra" (05_Documentacion/SPEC-App-Calibracion-Coviandina.md).
 *
 * De donde sale cada valor esperado — que es lo que decide si una prueba cuenta como cobertura
 * (CLAUDE.md del repositorio V3.6, §7):
 *
 *  - HUELLAS.txt de 06_Calibracion/SLV-002/campanas/, fila del ZIP de las 15:10: "App 3.6.11. Banco en
 *    curso: tipo I 20/20, blanco 5/16, amarillo 1/24; resto pendiente." Lo escribio Diego al archivar el
 *    ZIP, mirando el equipo.
 *  - decisiones.csv del APK, decision TIPO-I-REPETIR de Diego (6048453): las series de tipo I medidas
 *    fuera del protocolo preciso se anulan y se vuelven a medir.
 *  - RF-CAL-35 (SPEC-Calibracion-V3.6.md, citada en BancoCola.calibrable): un codigo solo se calibra si
 *    TODOS sus pasos AJUSTE y RE-MEDIDA estan hechos.
 *  - P11 §5.3 (SPEC-Calibracion-V3.6.md:750-751, citada en Anclas.oscuro:78-80): el ancla es la de la
 *    sesion, y sin el OSCURO de esa sesion no hay ancla.
 *  - El cuadro de familias de Diego del 19-sep-2026 por la noche.
 *  - La regla L-23 (README.md:20): la calibracion es por equipo fisico.
 *
 * AVISO SOBRE UN VALOR ESPERADO QUE ESTUVO MAL. La primera version de este fichero leyo el "tipo I 20/20"
 * de HUELLAS.txt como "los pasos del banco de tipo I estan hechos" y aseveraba que los codigos 8 y b
 * quedaban listos. **Es falso, y la prueba lo dijo en rojo.** Ese 20/20 cuenta LAMINAS MEDIDAS, no pasos
 * anotados: el ZIP de las 15:10 lo escribio la 3.6.11 y solo lleva 7 eventos PASO; tras BancoPrevio quedan
 * 25, y ademas TIPO-I-REPETIR anula 7 series. Con ese ZIP no queda ningun codigo listo, que es justo lo que
 * dice el "resto pendiente" de la misma linea de HUELLAS.txt. Se deja escrito el error junto al dato bueno.
 *
 * Nada de esto se ha probado en un telefono ni contra un equipo.
 */
package com.dpi.retrov36;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

public class AppCortaTest {

    static final String MAC = "00:21:13:05:19:3B";
    /** ZIP de soporte archivado en el repositorio; su md5 esta declarado en HUELLAS.txt. */
    static final String ZIP = "../../../06_Calibracion/SLV-002/campanas/campana_SLV-002_20260919_151045.zip";
    static final String MD5_ZIP = "f04fc0145aeafe951b4898607f4082da";
    static final String MOTIVO_FALTAN = "faltan patrones de AJUSTE o RE-MEDIDA del banco";
    static final String MOTIVO_VERIFICA = "esta cola solo lo verifica: no se ajusta";

    static List<Patron> catalogo;
    static byte[] zip;
    static String diario;

    @BeforeClass
    public static void una() throws Exception {
        try (InputStreamReader r = new InputStreamReader(
                new FileInputStream("src/main/assets/patrones_certificados_P1-P132.csv"), StandardCharsets.UTF_8)) {
            catalogo = Patron.leer(r);
        }
        zip = Files.readAllBytes(new File(ZIP).toPath());
        diario = ImportadorCampana.diarioDeZip(zip);
    }

    static String asset(String n) throws Exception {
        return new String(Files.readAllBytes(new File("src/main/assets/" + n).toPath()), StandardCharsets.UTF_8);
    }

    static BancoCola cola(BancoCola.Tipo t) throws Exception {
        return BancoCola.cargar(Files.readAllBytes(new File("src/main/assets/" + t.asset).toPath()));
    }

    /** La campana que crea la app corta: vacia, recien abierta para la serie y la MAC del equipo conectado. */
    static Campana vacia() throws Exception {
        Campana c = new Campana(catalogo, "SLV-002", MAC);
        c.escribirEn(new StringWriter());
        return c;
    }

    /** = lo que hace CortoActivity.importar: importar el diario y aplicar BancoPrevio, como la app. */
    static Campana cargada() throws Exception {
        Campana c = vacia();
        ImportadorCampana.importarDiario(c, diario, catalogo, "zip 15:10");
        bancoPrevio(c);
        return c;
    }

    /** = Base.aplicarBancoPrevio, que CortoActivity llama justo despues de importar. */
    static BancoPrevio.Resultado bancoPrevio(Campana c) throws Exception {
        return BancoPrevio.aplicar(c, cola(BancoCola.Tipo.de(c.colaTipo())),
                BancoPrevio.Grupos.leer(asset("grupos_patrones_equivalentes.csv")),
                Decisiones.leer(asset("decisiones.csv")), "SLV-002", "t");
    }

    /**
     * El ZIP del que se parte es el archivado, no otro: si alguien lo sustituye, esta prueba lo dice antes de
     * que los numeros de las demas dejen de significar nada. Valor esperado: HUELLAS.txt.
     */
    @Test
    public void a_elZipEsElArchivado() throws Exception {
        assertEquals("md5 del ZIP de las 15:10 declarado en HUELLAS.txt", MD5_ZIP, Resumen.hex(zip, "MD5"));
        assertNotNull("el ZIP de soporte trae el diario de la campana", diario);
    }

    /**
     * RF-COV-02. La campana de la app corta nace VACIA, asi que el banco y los pasos del ZIP entran enteros:
     * es lo que hace que cargar el ZIP no necesite ningun paso intermedio.
     *
     * El ZIP de las 15:10 lo escribio la 3.6.11, que no emitia el evento COLA (llego en la 3.6.15): su banco
     * se deduce de que trae PASO, y es el completo (ImportadorCampana.java:126). Por eso colaElegida() sigue
     * en false: no hay nada que adoptar, la campana vacia ya esta en COMPLETO por defecto
     * (Campana.java:215). La adopcion de verdad se prueba en b2.
     */
    @Test
    public void b_cargarElZipEnUnaCampanaVaciaTraeElBancoYSusPasos() throws Exception {
        Campana c = vacia();
        ImportadorCampana.Resultado r = ImportadorCampana.importarDiario(c, diario, catalogo, "zip 15:10");
        assertEquals("el banco del ZIP de las 15:10 es el completo P1-P132", "COMPLETO", c.colaTipo());
        assertTrue("entran los PASO del ZIP, no solo las series", r.pasos > 0);
        assertTrue("y entran las series", r.series > 0);
        assertFalse("la campana queda con pasos anotados", c.pasos().isEmpty());
    }

    /**
     * RF-COV-02, el caso que costo una hora la noche del 19-sep y que aqui no puede darse: un ZIP de un banco
     * DISTINTO del que tiene la campana. Como la de la app corta esta vacia, adopta el del ZIP y sus pasos
     * entran; no hay aviso que resolver en otra pantalla.
     */
    @Test
    public void b2_unaCampanaVaciaAdoptaElBancoDelZip() throws Exception {
        StringWriter dr = new StringWriter();
        Campana rep = new Campana(catalogo, "SLV-002", MAC);
        rep.escribirEn(dr);
        rep.elegirCola("REPRESENTATIVO", "70ef3b868db85ef75743936a6218935e");
        Campana.Serie s = rep.nuevaSerie("2026-09-21T09:00:00-0500", "SLV-002", MAC, "V3.6", "P1", 0, 'e');
        for (int i = 0; i < 4; i++) {
            rep.agregarDisparo(s, 1, "g" + i, "::1500", 1500 + i);
        }
        rep.cerrar(s, "OK", true, "");
        rep.elegir(s);
        rep.anotarPaso(3, "HECHO", s.id, "t", "");

        Campana c = vacia();
        assertEquals("la campana vacia arranca en el banco completo", "COMPLETO", c.colaTipo());
        ImportadorCampana.Resultado r = ImportadorCampana.importarDiario(c, dr.toString(), catalogo, "zip rep");
        assertEquals("adopta el banco del ZIP", "REPRESENTATIVO", r.bancoAdoptado);
        assertEquals("y la campana queda en el", "REPRESENTATIVO", c.colaTipo());
        assertTrue("con el banco ya elegido", c.colaElegida());
        assertEquals("no queda nada que preguntar al operador", "", r.bancoPorResolver);
        assertEquals("y los pasos del ZIP entran", 1, r.pasos);
    }

    /**
     * RF-COV-03, el corazon de esta app: tras cargar el ZIP hay que poder decir, codigo a codigo, si esta listo
     * y, si no, por que.
     *
     * Valor esperado, de HUELLAS.txt: "Banco en curso: ... resto pendiente". Con ese ZIP **no queda ningun
     * codigo listo**, y hay dos motivos distintos que no se pueden confundir:
     *   - 1, 2, 3, 4, 5, 6, 8, b: les faltan patrones de AJUSTE o RE-MEDIDA (RF-CAL-35). A los de tipo I les
     *     faltan porque TIPO-I-REPETIR de Diego anula sus series para repetirlas en preciso.
     *   - 7, a, c, d: en esta cola solo se VERIFICAN, no se ajustan. No es que falte nada.
     *
     * VISTA EN ROJO el 21-sep-2026 con dos roturas deliberadas de AppCorta.codigos (restauradas):
     *  1. motivo siempre "faltan patrones" (tieneAjuste(cola, s) -> true): falla esta y h_.
     *     org.junit.ComparisonFailure: codigo 7: esta cola solo lo verifica
     *     expected:<[esta cola solo lo verifica: no se ajusta]> but was:<[faltan patrones de AJUSTE o RE-MEDIDA del banco]>
     *  2. saltada la puerta BancoCola.calibrable (if (false && ...)): fallan esta, c2_ y h_.
     *     org.junit.ComparisonFailure: codigo 1: le faltan patrones del banco
     *     expected:<falta[n patrones de AJUSTE o RE-MEDIDA del banco]> but was:<falta[ el OSCURO del final de la sesión 1: no hay ancla]>
     *     Ojo: con esa rotura d_lasPuertasSonLasDeFlujoCalibracion SIGUE EN VERDE con este ZIP, porque a ningun
     *     codigo le queda el oscuro y "listo" sale false por las dos vias. d_ no vigila la puerta 1 por si sola.
     */
    @Test
    public void c_porCodigoDiceQueEstaListoYQueFalta() throws Exception {
        Campana c = cargada();
        List<AppCorta.Codigo> l = AppCorta.codigos(cola(BancoCola.Tipo.COMPLETO), c);
        assertEquals("un renglon por codigo de Fabrica.CODIGOS", Fabrica.CODIGOS.length, l.size());
        assertEquals("banco en curso, resto pendiente: ningun codigo listo", "", AppCorta.listos(l));
        for (char k : new char[]{'1', '2', '3', '4', '5', '6', '8', 'b'}) {
            assertEquals("codigo " + k + ": le faltan patrones del banco", MOTIVO_FALTAN, buscar(l, k).motivo);
        }
        for (char k : new char[]{'7', 'a', 'c', 'd'}) {
            assertEquals("codigo " + k + ": esta cola solo lo verifica", MOTIVO_VERIFICA, buscar(l, k).motivo);
        }
    }

    /**
     * El contrapunto de c: cuando el banco de un codigo SI esta completo, el codigo sale listo. Sin esto, la
     * prueba anterior no distinguiria "lo dice bien" de "nunca dice que si".
     *
     * Se monta el banco minimo del codigo 8 segun las dos reglas de la SPEC, no segun el codigo:
     *   - RF-CAL-35: todos sus pasos AJUSTE y RE-MEDIDA hechos.
     *   - P11 §5.3: el OSCURO de inicio y de fin de SU sesion, sin deriva entre los dos.
     */
    @Test
    public void c2_conSuBancoCompletoElCodigoQuedaListo() throws Exception {
        BancoCola q = cola(BancoCola.Tipo.COMPLETO);
        Campana c = vacia();
        int sesion = Anclas.sesionDe(q, '8');
        assertTrue("el codigo 8 se mide en alguna sesion de la cola", sesion > 0);
        for (BancoCola.Paso p : q.pasos) {
            boolean oscuroDeSuSesion = "OSCURO".equals(p.tipo) && p.sesion == sesion;
            boolean ajusteDel8 = "PATRON".equals(p.tipo) && "8".equals(p.codigo)
                    && ("AJUSTE".equals(p.uso) || "RE-MEDIDA".equals(p.uso));
            if (oscuroDeSuSesion || ajusteDel8) {
                hecho(c, p, oscuroDeSuSesion ? 565 : 1500);
            }
        }
        AppCorta.Codigo ocho = buscar(AppCorta.codigos(q, c), '8');
        assertTrue("con sus pasos hechos y su oscuro, el 8 queda listo: " + ocho.motivo, ocho.listo);
        assertEquals("y sin motivo que dar", "", ocho.motivo);
        assertEquals("solo el 8: los demas siguen sin banco", "8", AppCorta.listos(AppCorta.codigos(q, c)));
    }

    /** Una serie medida y aceptada en ese paso, con su paso anotado HECHO. */
    private static void hecho(Campana c, BancoCola.Paso p, double x) throws Exception {
        String nombre = "OSCURO".equals(p.tipo) ? Campana.OSCURO.nombre : p.patron;
        Campana.Serie s = c.nuevaSerie("2026-09-21T09:00:00-0500", "SLV-002", MAC, "V3.6", nombre, 0, 'e');
        for (int k = 1; k <= 5; k++) {
            for (int i = 0; i < 4; i++) {
                double v = x + (i % 2 == 0 ? 0.5 : -0.5);
                c.agregarDisparo(s, k, "t", "::" + Math.round(v), v);
            }
        }
        c.cerrar(s, "OK", true, "");
        c.elegir(s);
        c.anotarPaso(p.orden, "HECHO", s.id, "t", "");
    }

    /**
     * Las dos puertas que usa AppCorta tienen que ser LAS MISMAS que las de FlujoCalibracion.plan
     * (BancoCola.calibrable en :584 y Anclas.oscuro en :604-607): un codigo listo aqui no puede estar
     * bloqueado alli, ni al reves. Si un dia divergen, esta prueba lo dice.
     */
    @Test
    public void d_lasPuertasSonLasDeFlujoCalibracion() throws Exception {
        Campana c = cargada();
        BancoCola q = cola(BancoCola.Tipo.COMPLETO);
        for (AppCorta.Codigo x : AppCorta.codigos(q, c)) {
            boolean puerta1 = q.calibrable(String.valueOf(x.k), c.pasos());
            boolean puerta2 = puerta1 && !Double.isNaN(Anclas.oscuro(q, c, x.k).valor);
            assertEquals("codigo " + x.k + ": AppCorta y las puertas de FlujoCalibracion.plan", puerta2, x.listo);
        }
    }

    /**
     * RF-COV-02 / regla L-23: un ZIP de otro equipo no entra, y no entra NADA (atomico). La app corta no
     * relaja esta puerta: es la que impide calibrar un equipo con los patrones de otro.
     */
    @Test
    public void e_unZipDeOtroEquipoNoEntraNada() throws Exception {
        Campana otro = new Campana(catalogo, "SLV-099", "AA:BB:CC:DD:EE:01");
        otro.escribirEn(new StringWriter());
        try {
            ImportadorCampana.importarDiario(otro, diario, catalogo, "zip 15:10");
            org.junit.Assert.fail("un ZIP de SLV-002 no puede entrar en la campana de otro equipo");
        } catch (IllegalArgumentException e) {
            assertTrue("el mensaje dice de quien es el ZIP y de quien la campana",
                    e.getMessage().contains("otro equipo") && e.getMessage().contains("no se importa nada"));
        }
        assertTrue("no entro ni una serie", otro.series().isEmpty());
        assertTrue("ni un paso", otro.pasos().isEmpty());
    }

    /**
     * RF-COV-10. El APK acepta el ZIP de cualquier familia y, cuando no se puede calibrar, dice QUE HAY QUE
     * HACER con ese equipo en vez de negar sin explicar. Valor esperado: el cuadro de familias de Diego
     * (19-sep-2026, noche): V3.6 todo; V4.6 medir y banco; V4 original y V3 2020, ni banco ni calibrar.
     *
     * La frase sale de Protocolo.queHacerParaCalibrar(), no de esta app: durante unas horas hubo una copia en
     * AppCorta, escrita en paralelo, y se retiro. Esta prueba vigila que siga siendo una sola.
     */
    @Test
    public void f_diceQueHacerConCadaFamilia() {
        assertTrue("V4 original: necesita que le graben la V4.6",
                new ProtocoloV4Original().queHacerParaCalibrar().contains("V4.6"));
        assertFalse("y no se queda en negar: dice que hacer",
                new ProtocoloV4Original().queHacerParaCalibrar().trim().isEmpty());
        assertEquals("la V3.6 se calibra: no hay nada que hacer", "", new ProtocoloV36().queHacerParaCalibrar());
    }

    /**
     * Coherencia con el cuadro de Diego: hoy solo la V3.6 se calibra. Si manana se abre el candado de la V4.6,
     * esto obliga a pasar por aqui y revisar los textos en vez de que cambie sin que nadie lo note.
     */
    @Test
    public void g_soloLaV36SeCalibraHoy() {
        assertTrue("V3.6: todo, calibrar y escribir", new ProtocoloV36().calibra());
        assertFalse("V3 2020: ni banco ni calibrar", new ProtocoloV2020().calibra());
        assertFalse("V4 original: ni banco ni calibrar", new ProtocoloV4Original().calibra());
        assertFalse("V4 original: sin #X no hay x, asi que tampoco banco", new ProtocoloV4Original().mideBanco());
        assertEquals("y su motivo nombra la V4.6", ProtocoloV4Original.MOTIVO,
                new ProtocoloV4Original().motivoNoCalibra());
    }

    /**
     * El texto que ve el operador: nombra el banco, dice si hay codigos listos y, uno por uno, por que no.
     * Es lo que la noche del 19-sep no aparecia en ninguna pantalla — alli la app avisaba del desajuste y se
     * quedaba quieta, sin decir que ningun codigo era calibrable ni por que.
     */
    @Test
    public void h_elTextoDeLaPantallaDiceLoQueHaceFalta() throws Exception {
        Campana c = vacia();
        ImportadorCampana.Resultado r = ImportadorCampana.importarDiario(c, diario, catalogo, "zip 15:10");
        bancoPrevio(c);
        String t = AppCorta.texto(r, cola(BancoCola.Tipo.COMPLETO), c);
        System.out.println("SALIDA app corta, ZIP 15:10:\n" + t);
        assertTrue("dice el banco", t.contains("Banco: COMPLETO"));
        assertTrue("dice que no hay ninguno listo, en vez de callarse",
                t.contains("NO hay ningún código listo para calibrar."));
        assertTrue("y da el motivo de cada uno", t.contains("1: " + MOTIVO_FALTAN));
        assertTrue("distinguiendo el que solo se verifica", t.contains("7: " + MOTIVO_VERIFICA));
        assertTrue("y dice la s_rep, que es la otra puerta", t.contains("s_rep"));
    }

    /**
     * RF-COV-06 (y RF-APP-U37 del documento hermano). El acta va a nombre de una serie, y NO es lo mismo que
     * la haya dado el equipo a que la tecleara el operador. El dato existia desde la rc5
     * (Deteccion.marcaSerie:35-39) y su unico consumidor era el texto del correo (Sesion.java:300): al acta no
     * llegaba. Riesgo R-U18: un acta con serie declarada pasando por una con serie leida.
     *
     * Aqui se comprueba en las dos situaciones reales de campo: SLV-002, con la serie grabada en EEPROM, y un
     * equipo recien grabado, que responde "#GN,NONE#" — el caso de la noche del 19-sep.
     */
    @Test
    public void j_laSerieDeclaradaSeDistingueDeLaLeida() throws Exception {
        FlujoCalibracion.Contexto con = FlujoCalibracion.identificar(EquipoSimulado.slv002(),
                "COVIANDINA_SLV-002", MAC, Boolean.TRUE, "6/6", "prueba");
        assertEquals("SLV-002 tiene la serie en EEPROM: #GN# la da", "SLV-002", con.serieGN);
        assertEquals("y por tanto el acta no lleva marca", "", con.marcaSerie);

        FlujoCalibracion.Contexto sin = FlujoCalibracion.identificar(new EquipoSimulado(),
                "COVIANDINA", MAC, Boolean.TRUE, "6/6", "prueba");
        assertTrue("un equipo recien grabado responde #GN,NONE#",
                sin.serieGN == null || Calibracion.NONE.equals(sin.serieGN));
        assertEquals("la serie la declara el operador, y el acta tiene que decirlo",
                Deteccion.MARCA_DECLARADA, sin.marcaSerie);
    }

    /**
     * RF-COV-09 — ASEVERA UN REQUISITO. Valor esperado de fuera del codigo:
     *  - SPEC-App-Calibracion-Coviandina.md §3, fila "El ZIP es de otra familia que el equipo conectado":
     *    "No entra nada. Un dialogo, con el motivo y que hacer (RF-APP-U32)".
     *  - RF-COV-09 (misma SPEC): "detiene [el paso 1] ... El texto lleva siempre la salida: 'abra una campana
     *    aparte para la etapa anterior'".
     *  - RF-APP-U32 (SPEC-App-Unica-Familias-y-ZIP.md §3.3), texto de ejemplo: "La lectura x no esta en la misma
     *    escala. No se ha importado nada."
     *  - El caso es el de SPEC-App-Unica-Familias-y-ZIP.md §3.2: al SLV-003-2026 se le grabo la V4.6 y la MAC
     *    NO cambio; un ZIP suyo de ANTES (V4 original) tiene la misma serie y la misma MAC.
     *
     * Por que hace falta aqui y no basta la guarda de la rc6: esa guarda compara con la familia de la CAMPANA
     * (ImportadorCampana.comprobarFamilia) y en la app corta la campana nace VACIA, con familia desconocida,
     * que no bloquea nada (Familia.compatibles). Asi que en la app corta el primer ZIP entraba fuera cual fuera
     * su familia. La comparacion tiene que ser con el EQUIPO CONECTADO, que es lo que dice la SPEC.
     *
     * VISTA EN ROJO el 21-sep-2026 antes de arreglarlo, con AppCorta.importar delegando sin mas en
     * ImportadorCampana.importarDiario (= lo que hacia CortoActivity.importar en 72d00cd):
     *   k_unZipDeOtraFamiliaQueElEquipoConectadoNoEntra: java.lang.AssertionError:
     *   un ZIP de V4 original no puede entrar con un V4.6 conectado (RF-COV-09)
     */
    @Test
    public void k_unZipDeOtraFamiliaQueElEquipoConectadoNoEntra() throws Exception {
        String serie = "SLV-003-2026";
        String mac = "00:22:09:01:65:10";
        // ZIP de ANTES de grabar: V4 original, misma serie y misma MAC.
        StringWriter antes = new StringWriter();
        Campana v4 = new Campana(catalogo, serie, mac);
        v4.escribirEn(antes);
        Campana.Serie s = v4.nuevaSerie("2026-09-19T18:00:00-0500", serie, mac, "V4 original (V4.1)", "P20", 0, '2');
        v4.agregarDisparo(s, 1, "t", "@LEERV,716@", Double.NaN);
        v4.cerrar(s, "OK", true, "");

        // La app corta: campana recien abierta y VACIA, y conectado el mismo equipo ya con la V4.6.
        Campana c = new Campana(catalogo, serie, mac);
        c.escribirEn(new StringWriter());
        String familiaEquipo = new ProtocoloV46().firmware().name();
        try {
            AppCorta.importar(c, antes.toString(), catalogo, "ZIP de antes de grabar", familiaEquipo);
            org.junit.Assert.fail("un ZIP de V4 original no puede entrar con un V4.6 conectado (RF-COV-09)");
        } catch (IllegalArgumentException e) {
            String m = e.getMessage();
            assertTrue("RF-APP-U32: dice que no ha entrado nada: " + m, m.contains("No se ha importado nada"));
            assertTrue("RF-APP-U32: dice por que (la escala de x): " + m,
                    m.contains("La lectura x no está en la misma escala"));
            assertTrue("RF-COV-09: el texto lleva siempre la salida: " + m,
                    m.contains("abra una campaña aparte para la etapa anterior"));
        }
        assertTrue("no entra ni una serie", c.series().isEmpty());
        assertTrue("ni un paso", c.pasos().isEmpty());
    }

    /**
     * RF-COV-09, el contrapunto — ASEVERA UN REQUISITO (RF-COV-02: cargar el ZIP deja el equipo listo, sin paso
     * intermedio). El ZIP archivado de las 15:10 es de SLV-002 con la V3.6 (sus 38 SERIE dicen "V3.6 2026-09-19
     * (3.6.2) CAL mascara 0003"), y con un V3.6 conectado tiene que entrar. Sin esto, k no distinguiria
     * "rechaza la otra familia" de "rechaza todo".
     *
     * VISTA EN ROJO el 21-sep-2026 con una rotura deliberada de AppCorta.importar (rechazar siempre; restaurada):
     *   java.lang.IllegalArgumentException: El equipo conectado es un V3.6 y este ZIP trae medidas de un V3.6 ...
     */
    @Test
    public void k2_unZipDeLaMismaFamiliaEntra() throws Exception {
        Campana c = vacia();
        ImportadorCampana.Resultado r = AppCorta.importar(c, diario, catalogo, "zip 15:10",
                new ProtocoloV36().firmware().name());
        assertTrue("entran las series del ZIP", r.series > 0);
        assertFalse("y la campana deja de estar vacia", c.series().isEmpty());
    }

    /** Sin cola o sin campana no se inventa un veredicto: se dice que no se puede decir. */
    @Test
    public void i_sinColaNoSeInventaNada() throws Exception {
        assertTrue(AppCorta.codigos(null, vacia()).isEmpty());
        assertTrue(AppCorta.codigos(cola(BancoCola.Tipo.COMPLETO), null).isEmpty());
        assertTrue(AppCorta.texto(null, null, null).contains("no se puede decir"));
    }

    static AppCorta.Codigo buscar(List<AppCorta.Codigo> l, char k) {
        for (AppCorta.Codigo x : l) {
            if (x.k == k) {
                return x;
            }
        }
        throw new IllegalStateException("no hay renglon para el codigo " + k);
    }

    /** Silencia el aviso de import no usado de StringReader cuando se retoquen las pruebas. */
    @SuppressWarnings("unused")
    private static final StringReader SIN_USO = null;
}
