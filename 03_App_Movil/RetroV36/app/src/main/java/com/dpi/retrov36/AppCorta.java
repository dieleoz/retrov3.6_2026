package com.dpi.retrov36;

import java.util.ArrayList;
import java.util.List;

/**
 * RTV 1.0.0-rc6 — la parte con decisiones de la app corta "RTV Calibra"
 * (05_Documentacion/SPEC-App-Calibracion-Coviandina.md). **Java puro**: {@link CortoActivity} solo la pinta,
 * y las pruebas JVM la recorren entera.
 *
 * No calcula NADA propio. Las dos puertas que deciden si un codigo se puede calibrar son exactamente las que
 * usa {@link FlujoCalibracion#plan}: {@link BancoCola#calibrable} primero (FlujoCalibracion.java:584) y
 * {@link Anclas#oscuro} despues (FlujoCalibracion.java:604-607). Aqui se leen y se escriben en una linea por
 * codigo; el motivo que se ensena es el de {@code Anclas}, literal, sin reescribirlo.
 *
 * Si un numero de esta pantalla no coincide con el de "Calibrar este equipo" con el mismo ZIP, es un fallo de
 * esta clase, no una variante.
 */
public final class AppCorta {

    private AppCorta() { }

    // Lo que le falta a cada familia para poder calibrarse NO se escribe aqui. Vive donde le corresponde, que
    // es el protocolo: Protocolo.queHacerParaCalibrar() (Protocolo.java:98, ProtocoloV4Original.java:156).
    // Esta clase tuvo una copia suya durante unas horas, escrita en paralelo; se retiro para que no circulen
    // dos textos distintos diciendo lo mismo. La app corta llama a la del protocolo (CortoActivity.familia),
    // igual que la de campo (FlujoCalibracion.java:379-380).

    /**
     * Paso 1 de la app corta: importar el diario del ZIP en la campana del equipo conectado, con el rechazo
     * duro por familia de RF-COV-09 (SPEC-App-Calibracion-Coviandina.md §3) y RF-APP-U32.
     *
     * La guarda de familia de la rc6 (ImportadorCampana.comprobarFamilia) compara con la familia de la
     * CAMPANA. En esta app la campana nace vacia, con familia desconocida, y una familia desconocida no bloquea
     * (Familia.compatibles): el primer ZIP entraba fuera cual fuera su familia. Aqui se compara ANTES con la del
     * EQUIPO CONECTADO, que es lo que pide la SPEC ("el ZIP es de otra familia que el equipo conectado"). Despues
     * se importa por la via de siempre, con todas sus guardas (equipo, MAC, catalogo, atomicidad).
     *
     * @param familiaEquipo nombre de {@link Protocolo.Firmware} del equipo conectado ("F36", "F46", ...). Una
     *                      familia desconocida no bloquea, con la misma regla que {@link Familia#compatibles}.
     * @throws IllegalArgumentException si el ZIP es de otra familia; no se ha escrito nada.
     */
    public static ImportadorCampana.Resultado importar(Campana c, String diario, List<Patron> catalogo,
            String origen, String familiaEquipo) throws java.io.IOException {
        String equipo = Familia.de(familiaEquipo);
        String zip = familiaDelDiario(diario, catalogo);
        if (!Familia.compatibles(equipo, zip)) {
            throw new IllegalArgumentException("El equipo conectado es un " + Familia.texto(equipo)
                    + " y este ZIP trae medidas de un " + Familia.texto(zip) + " (" + c.equipo + ", MAC " + c.mac
                    + "; la MAC es del módulo Bluetooth y no cambia al grabar). La lectura x no está "
                    + "en la misma escala. No se ha importado nada. Si quiere conservar esas medidas, abra una "
                    + "campaña aparte para la etapa anterior (en la app de campo, RTV).");
        }
        return ImportadorCampana.importarDiario(c, diario, catalogo, origen);
    }

    /**
     * Familia de lo que trae un diario: la del evento FAMILIA si lo lleva (rc6 en adelante) y, si no, la del
     * texto de firmware de sus series (los diarios anteriores a la rc6). {@link Familia#DESCONOCIDA} si nada la
     * identifica. Si el diario mezclara dos familias, lo rechaza despues ImportadorCampana.importarDiario.
     */
    static String familiaDelDiario(String diario, List<Patron> catalogo) throws java.io.IOException {
        Campana todo = new Campana(catalogo);
        todo.leerDiario(new java.io.StringReader(diario));
        if (!Familia.DESCONOCIDA.equals(todo.familia())) {
            return todo.familia();
        }
        for (Campana.Serie s : todo.series()) {
            String f = Familia.de(s.firmware);
            if (!f.isEmpty()) {
                return f;
            }
        }
        return Familia.DESCONOCIDA;
    }

    /** Una linea por codigo: si esta listo para calibrar y, si no, por que. */
    public static final class Codigo {
        public final char k;
        public final boolean listo;
        /** "" cuando esta listo. */
        public final String motivo;

        Codigo(char k, boolean listo, String motivo) {
            this.k = k;
            this.listo = listo;
            this.motivo = motivo;
        }
    }

    /**
     * Estado por codigo tras cargar el ZIP. Mismo orden que {@link Fabrica#CODIGOS}.
     *
     * Un codigo que la cola solo VERIFICA (7, a, c, d en la cola completa) no tiene pasos de AJUSTE ni de
     * RE-MEDIDA, asi que {@link BancoCola#calibrable} devuelve false: no es que falte nada, es que ese codigo
     * no se ajusta. Se dice con esas palabras.
     */
    public static List<Codigo> codigos(BancoCola cola, Campana c) {
        List<Codigo> l = new ArrayList<>();
        if (cola == null || c == null) {
            return l;
        }
        java.util.Map<Integer, String> est = c.pasos();
        for (char k : Fabrica.CODIGOS) {
            String s = String.valueOf(k);
            if (!cola.calibrable(s, est)) {
                l.add(new Codigo(k, false, tieneAjuste(cola, s)
                        ? "faltan patrones de AJUSTE o RE-MEDIDA del banco"
                        : "esta cola solo lo verifica: no se ajusta"));
                continue;
            }
            Anclas.Valor osc = Anclas.oscuro(cola, c, k);
            if (Double.isNaN(osc.valor)) {
                l.add(new Codigo(k, false, osc.texto));
                continue;
            }
            l.add(new Codigo(k, true, ""));
        }
        return l;
    }

    /** true si la cola tiene algun paso de AJUSTE o RE-MEDIDA para ese codigo. */
    static boolean tieneAjuste(BancoCola cola, String codigo) {
        for (BancoCola.Paso p : cola.pasos) {
            if ("PATRON".equals(p.tipo) && codigo.equals(p.codigo)
                    && ("AJUSTE".equals(p.uso) || "RE-MEDIDA".equals(p.uso))) {
                return true;
            }
        }
        return false;
    }

    /** Los codigos listos, en una cadena corta ("8 b"); "" si ninguno. */
    public static String listos(List<Codigo> l) {
        StringBuilder sb = new StringBuilder();
        for (Codigo x : l) {
            if (x.listo) {
                sb.append(sb.length() == 0 ? "" : " ").append(x.k);
            }
        }
        return sb.toString();
    }

    /**
     * El texto que la pantalla ensena debajo de "Cargar el ZIP" (RF-APP-58). Dice, en este orden: cuantas
     * series han entrado, de que banco, y que codigos quedan listos. Despues, una linea por codigo que NO esta
     * listo con su motivo — que es lo que esta noche no se veia en ninguna pantalla.
     */
    public static String texto(ImportadorCampana.Resultado r, BancoCola cola, Campana c) {
        StringBuilder sb = new StringBuilder();
        if (r != null) {
            sb.append(r.texto()).append('\n');
        }
        if (cola == null || c == null) {
            sb.append("Sin cola o sin campaña: no se puede decir si está listo.");
            return sb.toString();
        }
        sb.append("Banco: ").append(c.colaTipo()).append('\n');
        List<Codigo> l = codigos(cola, c);
        String listos = listos(l);
        sb.append(listos.isEmpty() ? "NO hay ningún código listo para calibrar."
                : "Listos para calibrar: " + listos).append('\n');
        Anclas.Valor sr = Anclas.sRep(cola, c);
        sb.append(sr.texto).append('\n');
        for (Codigo x : l) {
            if (!x.listo) {
                sb.append("  ").append(x.k).append(": ").append(x.motivo).append('\n');
            }
        }
        return sb.toString();
    }
}
