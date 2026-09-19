package com.dpi.retrov36;

import android.os.Handler;
import android.os.Looper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Las seis pruebas del equipo, que se pasan antes de medir o calibrar
 * (LEEME.md de la app, seccion "Pruebas"). Orden de ejecucion: 1, 2, 5, 3, 4, 6
 * (la 5 lee los coeficientes que la 4 necesita en V3.6).
 */
public final class Pruebas {

    /** INVALIDA: la prueba no vale (colocacion); no es un fallo del equipo. */
    public enum Estado { PENDIENTE, EN_CURSO, OK, FALLO, NO_APLICA, INFO, INVALIDA }

    public static final class Prueba {
        public final int numero;
        public final String titulo;
        public volatile Estado estado = Estado.PENDIENTE;
        public volatile String detalle = "";

        Prueba(int numero, String titulo) {
            this.numero = numero;
            this.titulo = titulo;
        }
    }

    public interface Oyente {
        /** En el hilo de interfaz. */
        void cambio();
    }

    private static final Pruebas INSTANCIA = new Pruebas();

    public static Pruebas get() {
        return INSTANCIA;
    }

    private final Handler principal = new Handler(Looper.getMainLooper());
    private final CopyOnWriteArrayList<Oyente> oyentes = new CopyOnWriteArrayList<>();
    public final List<Prueba> lista = new ArrayList<>();
    private volatile boolean enCurso;
    private volatile boolean cancelar;
    public volatile double tolerancia = Coherencia.TOLERANCIA_POR_DEFECTO;

    private Pruebas() {
        lista.add(new Prueba(1, "Enlace SPP abierto, nombre y MAC"));
        lista.add(new Prueba(2, "Detección de versión (#V#, 9, 6, @LEERV,BLA,1@; nunca e)"));
        lista.add(new Prueba(3, "Cada código de medida responde en menos de 2,5 s"));
        lista.add(new Prueba(4, "Coherencia de fórmulas: las 12 dan la misma x"));
        lista.add(new Prueba(5, "Solo V3.6: #GT#, #G,k# de los 12 juegos, marca y máscara, #E en 5 puntos"));
        lista.add(new Prueba(6, "Repetibilidad: " + Repetibilidad.LECTURAS + " lecturas del mismo código"));
    }

    public void agregar(Oyente o) {
        oyentes.addIfAbsent(o);
    }

    public void quitar(Oyente o) {
        oyentes.remove(o);
    }

    public boolean enCurso() {
        return enCurso;
    }

    public void cancelar() {
        cancelar = true;
    }

    private void avisar() {
        principal.post(() -> {
            for (Oyente o : oyentes) {
                o.cambio();
            }
        });
    }

    private Prueba p(int n) {
        return lista.get(n - 1);
    }

    private void poner(int n, Estado e, String detalle) {
        Prueba x = p(n);
        x.estado = e;
        x.detalle = detalle;
        if (e != Estado.EN_CURSO) {
            Registro.nota("prueba " + n + " (" + x.titulo + "): " + e + (detalle.isEmpty() ? "" : "\n  " + detalle.replace("\n", "\n  ")));
        }
        avisar();
    }

    public void iniciar() {
        if (enCurso) {
            return;
        }
        enCurso = true;
        cancelar = false;
        for (Prueba x : lista) {
            x.estado = Estado.PENDIENTE;
            x.detalle = "";
        }
        Sesion.get().apto = null;
        Sesion.get().resumenPruebas = "Pruebas en curso...";
        avisar();
        Cliente.instancia().ejecutar(this::correr);
    }

    private void comprobarCancelacion() throws InterruptedException {
        if (cancelar) {
            throw new InterruptedException("cancelado por el operador");
        }
    }

    private void correr() {
        Sesion s = Sesion.get();
        int actual = 1;
        Registro.nota("=== pruebas del equipo: inicio ===");
        try {
            // 1. Enlace
            poner(1, Estado.EN_CURSO, "");
            EnlaceSerie en = EnlaceSerie.instancia();
            if (!en.estaConectado()) {
                poner(1, Estado.FALLO, "No hay enlace SPP abierto.");
                terminar(false, "NO APTO: sin enlace.");
                return;
            }
            poner(1, Estado.OK, en.getNombre() + "  " + en.getMac() + "  (serie " + s.serie() + ")");

            // 2. Version
            actual = 2;
            poner(2, Estado.EN_CURSO, "");
            String det = detectar(s);
            boolean medible = s.versionMedible();
            poner(2, medible ? Estado.OK : Estado.FALLO, det);
            if (!medible) {
                for (int n = 3; n <= 6; n++) {
                    poner(n, Estado.FALLO, "No ejecutada: versión " + s.version.texto + ".");
                }
                terminar(false, "NO APTO: " + (s.version == Sesion.Version.V4
                        ? "es un V4; esta app no aplica." : "versión de firmware no reconocida."));
                return;
            }
            comprobarCancelacion();

            // 5. Solo V3.6
            actual = 5;
            boolean ok5 = true;
            if (s.version == Sesion.Version.V36) {
                poner(5, Estado.EN_CURSO, "");
                ok5 = prueba5(s);
            } else {
                poner(5, Estado.NO_APLICA, "Firmware V3 2020: no tiene órdenes #...#.");
            }
            comprobarCancelacion();

            // 3. Codigos
            actual = 3;
            poner(3, Estado.EN_CURSO, "");
            List<Coherencia.Entrada> entradas = new ArrayList<>();
            Integer[] eIniFin = prueba3(s, entradas);
            comprobarCancelacion();

            // 4. Coherencia
            actual = 4;
            poner(4, Estado.EN_CURSO, "");
            Coherencia.Resultado c = Coherencia.evaluar(entradas, eIniFin[0], eIniFin[1], tolerancia);
            StringBuilder sb = new StringBuilder(c.resumen);
            for (Coherencia.Linea l : c.lineas) {
                sb.append('\n').append(l.texto());
            }
            sb.append("\nCoeficientes: ").append(s.version == Sesion.Version.V36
                    ? "los leídos con #G" : "los de fábrica (ecuacionesCalibracion.c:3-42)");
            poner(4, c.apto ? Estado.OK : (c.invalida ? Estado.INVALIDA : Estado.FALLO), sb.toString());
            comprobarCancelacion();

            // 6. Repetibilidad
            actual = 6;
            poner(6, Estado.EN_CURSO, "");
            List<Double> xs = new ArrayList<>();
            int fallidas = 0;
            StringBuilder d6 = new StringBuilder();
            for (int i = 0; i < Repetibilidad.LECTURAS; i++) {
                comprobarCancelacion();
                LecturaX.Lectura l = LecturaX.leer(s);
                if (l.valida()) {
                    xs.add(l.x);
                    d6.append(String.format(Locale.US, "%d: %s -> x=%.1f\n", i + 1, l.respuesta.trama, l.x));
                } else {
                    fallidas++;
                    d6.append(i + 1).append(": ").append(l.error).append('\n');
                }
            }
            Repetibilidad.Resultado r6 = Repetibilidad.evaluar(Estadistica.aVector(xs), fallidas);
            d6.append("Código ").append(s.version == Sesion.Version.V36 ? "e" : "6 (invertido)").append(". ").append(r6.texto);
            poner(6, r6.apto ? Estado.OK : Estado.FALLO, d6.toString());

            boolean apto = p(1).estado == Estado.OK && p(2).estado == Estado.OK
                    && p(3).estado == Estado.OK && p(4).estado == Estado.OK
                    && ok5 && p(6).estado == Estado.OK;
            String motivo = p(4).estado == Estado.INVALIDA
                    ? "NO APTO: la prueba 4 no es válida (" + Coherencia.MENSAJE_INVALIDA + "). No es un fallo del equipo."
                    : "NO APTO para calibrar: revise las pruebas en rojo.";
            terminar(apto, apto ? "APTO para calibrar." : motivo);
        } catch (InterruptedException e) {
            poner(actual, Estado.FALLO, "Interrumpida: " + e.getMessage());
            pendientesAFallo("No ejecutada: pruebas canceladas.");
            terminar(false, "NO APTO: pruebas incompletas (canceladas).");
        } catch (IOException e) {
            poner(actual, Estado.FALLO, "Enlace perdido: " + EnlaceSerie.descripcion(e));
            pendientesAFallo("No ejecutada: enlace perdido.");
            terminar(false, "NO APTO: se perdió el enlace durante las pruebas.");
        } catch (RuntimeException e) {
            poner(actual, Estado.FALLO, "Error interno: " + EnlaceSerie.descripcion(e));
            pendientesAFallo("No ejecutada.");
            terminar(false, "NO APTO: error interno de la app.");
        }
    }

    private void pendientesAFallo(String texto) {
        for (Prueba x : lista) {
            if (x.estado == Estado.PENDIENTE || x.estado == Estado.EN_CURSO) {
                poner(x.numero, Estado.FALLO, texto);
            }
        }
    }

    private void terminar(boolean apto, String resumen) {
        Sesion s = Sesion.get();
        s.apto = apto;
        s.resumenPruebas = resumen + Cliente.instancia().consejoSiMudo();
        Registro.nota("=== pruebas del equipo: " + resumen + " " + s.identidad() + " ===");
        enCurso = false;
        avisar();
    }

    /** Prueba 2, segun el contrato. Devuelve el detalle. */
    String detectar(Sesion s) throws IOException, InterruptedException {
        Cliente c = Cliente.instancia();
        StringBuilder d = new StringBuilder();
        Cliente.Respuesta rv = c.pedir("#V#", Tramas.Tipo.ADMIN, Cliente.TIMEOUT_ADMIN_MS + 500);
        d.append("#V# -> ").append(rv.describir()).append('\n');
        Tramas.InfoVersion iv = rv.valida() ? Tramas.parsearVersion(rv.trama) : null;
        if (iv != null && "3.6".equals(iv.version)) {
            s.version = Sesion.Version.V36;
            s.fechaFirmware = iv.fecha;
            s.marca = iv.marca;
            s.mascara = iv.mascara;
            s.eDisponible = true;
            d.append("Versión V3.6, fecha ").append(iv.fecha).append(", marca ").append(iv.marca);
            if (iv.tieneMascara()) {
                d.append(String.format(Locale.US, ", máscara %04X: ", iv.mascara));
                StringBuilder cal = new StringBuilder();
                for (int i = 0; i < Fabrica.CODIGOS.length; i++) {
                    if (iv.codigoAjustado(i)) {
                        cal.append(Fabrica.CODIGOS[i]).append(' ');
                    }
                }
                d.append(cal.length() == 0 ? "ningún código ajustado" : "ajustados " + cal.toString().trim());
                d.append(iv.temperaturaAjustada() ? "; temperatura ajustada" : "; temperatura de fábrica");
            } else {
                d.append(" (sin máscara: trama de la revisión 1.0 del contrato)");
            }
            return d.toString();
        }
        if (iv != null) {
            s.version = Sesion.Version.DESCONOCIDA;
            d.append("Responde a #V# con versión ").append(iv.version).append(": no es la del contrato.");
            return d.toString();
        }
        // NUNCA se envia 'e' a un equipo sin identificar: en SLV-002 (V3 2020
        // sin 'e'), tras 'e' el equipo dejo de responder a todo (18-sep-2026,
        // app 3.6.0 y barrido; hipotesis sin confirmar). En un V3 2020, #V#, 9
        // y 6 disparan una medida; la pausa de Cliente protege el siguiente envio.
        Cliente.Respuesta r9 = c.pedir("9", Tramas.Tipo.BATERIA, Cliente.TIMEOUT_MEDIDA_MS);
        d.append("9 -> ").append(r9.describir()).append('\n');
        if (r9.valida()) {
            s.version = Sesion.Version.V3_2020;
            s.eDisponible = false;
            d.append("Versión V3 2020 (responde :n: a 9). Se medirá con 6 invertido; nunca con e.");
            return d.toString();
        }
        Cliente.Respuesta r6 = c.pedir("6", Tramas.Tipo.MEDIDA, Cliente.TIMEOUT_MEDIDA_MS);
        d.append("6 -> ").append(r6.describir()).append('\n');
        if (r6.valida()) {
            s.version = Sesion.Version.V3_2020;
            s.eDisponible = false;
            d.append("Versión V3 2020 (responde ::n a 6). Se medirá con 6 invertido; nunca con e.");
            return d.toString();
        }
        Cliente.Respuesta r4 = c.pedir(Tramas.SONDA_V4, Tramas.Tipo.LEERV, 3000);
        d.append(Tramas.SONDA_V4).append(" -> ").append(r4.describir()).append('\n');
        if (r4.valida()) {
            s.version = Sesion.Version.V4;
            d.append("Es un V4 (protocolo @LEERV). Esta app no aplica.");
            return d.toString();
        }
        s.version = Sesion.Version.DESCONOCIDA;
        d.append("Ninguna de las tres sondas respondió como espera el contrato.");
        return d.toString();
    }

    /** Valores de x en los que se comprueba cada ecuacion con #E (revision 1.1, O-10). */
    static final int[] X_COMPROBACION = {500, 1000, 2000, 3000, 4000};

    /** Prueba 5. */
    private boolean prueba5(Sesion s) throws IOException, InterruptedException {
        Cliente c = Cliente.instancia();
        StringBuilder d = new StringBuilder();
        boolean ok = true;
        Cliente.Respuesta rt = c.pedir("#GT#", Tramas.Tipo.ADMIN, Cliente.TIMEOUT_ADMIN_MS);
        double[] t = rt.valida() ? Tramas.parsearGT(rt.trama) : null;
        s.temperatura = t;
        if (t == null) {
            ok = false;
            d.append("#GT# -> ").append(rt.describir()).append(" (no cuadra con el contrato)\n");
        } else {
            d.append("#GT# -> x2=").append(Tramas.coeficiente(t[0])).append(" x1=")
                    .append(Tramas.coeficiente(t[1])).append(" x0=").append(Tramas.coeficiente(t[2])).append('\n');
        }
        boolean conMascara = s.mascara >= 0;
        for (int i = 0; i < Fabrica.CODIGOS.length; i++) {
            comprobarCancelacion();
            char k = Fabrica.CODIGOS[i];
            Cliente.Respuesta r = c.pedir("#G," + k + "#", Tramas.Tipo.ADMIN, Cliente.TIMEOUT_ADMIN_MS);
            Ecuacion e = r.valida() ? Tramas.parsearG(r.trama, k) : null;
            s.leidas[i] = e;
            if (e == null) {
                ok = false;
                d.append("#G,").append(k).append("# -> ").append(r.describir()).append(" (no cuadra)\n");
                continue;
            }
            boolean igual = e.igualFloat32(Fabrica.ecuacion(k)); // ULP_G: ver Ecuacion
            boolean ajustado = conMascara ? ((s.mascara >> i) & 1) != 0 : "CAL".equals(s.marca);
            d.append(k).append(": ").append(igual ? "= fábrica" : "distinta de fábrica");
            if (conMascara) {
                d.append(ajustado ? ", máscara: ajustado" : ", máscara: fábrica");
                if (!ajustado && !igual) {
                    ok = false;
                    d.append("  <- FALLO: la máscara dice fábrica y los coeficientes no lo son");
                }
            } else if ("DEF".equals(s.marca) && !igual) {
                ok = false;
                d.append("  <- FALLO: con DEF deberían ser los de fábrica");
            }
            d.append('\n');
            poner(5, Estado.EN_CURSO, d.toString());
        }
        if ("DEF".equals(s.marca)) {
            d.append("Marca DEF: sin calibración válida en EEPROM; usa las de fábrica.\n");
        } else if ("CAL".equals(s.marca)) {
            d.append("Marca CAL: hay calibración válida en EEPROM (CRC comprobado por el firmware).\n");
        } else {
            ok = false;
            d.append("Marca \"").append(s.marca).append("\": no es CAL ni DEF.\n");
        }
        // #E: verificacion EXACTA de no regresion (SPEC RF-APP-07 [MOD r1.1]).
        // #E devuelve enteros, sin paso por texto: en un codigo a "fabrica" debe
        // coincidir EXACTAMENTE con la emulacion de 2020 hecha con la tabla de
        // fabrica de la app, no con lo leido por #G (que puede estar a 2 ulp).
        // En un codigo ajustado no hay tabla de referencia: se emula con #G y
        // una diferencia de 1 unidad se anota como posible error de impresion.
        int exactas = 0;
        int distintas = 0;
        int avisos = 0;
        for (int i = 0; i < Fabrica.CODIGOS.length; i++) {
            char k = Fabrica.CODIGOS[i];
            Ecuacion leida = s.leidas[i];
            boolean aFabrica = conMascara ? ((s.mascara >> i) & 1) == 0
                    : ("DEF".equals(s.marca) || (leida != null && leida.igualFloat32(Fabrica.ecuacion(k))));
            Ecuacion ref = aFabrica ? Fabrica.ecuacion(k) : leida;
            for (int x : X_COMPROBACION) {
                comprobarCancelacion();
                Cliente.Respuesta r = c.pedir("#E," + k + "," + x + "#", Tramas.Tipo.ADMIN,
                        Cliente.TIMEOUT_ADMIN_MS);
                Integer v = r.valida() ? Tramas.parsearE(r.trama, k) : null;
                if (v == null) {
                    ok = false;
                    distintas++;
                    d.append("#E,").append(k).append(',').append(x).append("# -> ").append(r.describir())
                            .append(" (no cuadra)\n");
                    continue;
                }
                if (ref == null) {
                    continue; // codigo ajustado sin #G: ya contado como fallo
                }
                int esperado = ref.respuestaFloat32(x);
                if (v == esperado) {
                    exactas++;
                } else if (!aFabrica && Math.abs(v - esperado) == 1) {
                    avisos++;
                    d.append("#E,").append(k).append(',').append(x).append("# = ").append(v)
                            .append(", la app espera ").append(esperado)
                            .append(" (código ajustado: 1 unidad, posible error de %.8E en #G; aviso, no fallo)\n");
                } else {
                    ok = false;
                    distintas++;
                    d.append("#E,").append(k).append(',').append(x).append("# = ").append(v)
                            .append(", la app espera ").append(esperado)
                            .append(aFabrica ? " (código a fábrica: REGRESIÓN respecto de 2020)" : "")
                            .append('\n');
                }
            }
            poner(5, Estado.EN_CURSO, d.toString());
        }
        if (avisos > 0) {
            d.append(avisos).append(" avisos de 1 unidad en códigos ajustados.\n");
        }
        d.append("#E: ").append(exactas).append(" coincidencias exactas, ").append(distintas)
                .append(" discrepancias, en x = 500, 1000, 2000, 3000, 4000.");
        poner(5, ok ? Estado.OK : Estado.FALLO, d.toString());
        return ok;
    }

    /**
     * Prueba 3. Rellena las entradas de coherencia y devuelve {e antes, e despues}
     * (null donde no hay). En V3.6 se lee 'e' antes y despues de los 12 codigos
     * para que la prueba 4 corrija la deriva; en V3 2020 nunca se envia 'e'.
     */
    private Integer[] prueba3(Sesion s, List<Coherencia.Entrada> entradas)
            throws IOException, InterruptedException {
        Cliente c = Cliente.instancia();
        StringBuilder d = new StringBuilder();
        boolean ok = true;
        boolean v36 = s.version == Sesion.Version.V36;
        Integer eIni = null;
        if (v36) {
            Cliente.Respuesta re = c.pedir("e", Tramas.Tipo.MEDIDA, Cliente.TIMEOUT_MEDIDA_MS);
            eIni = re.valida() ? Tramas.valorMedida(re.trama) : null;
            d.append("e (antes): ").append(re.describir());
            if (eIni == null) {
                ok = false;
                d.append("  <- obligatorio en V3.6");
            }
            d.append('\n');
        }
        for (char k : Fabrica.CODIGOS) {
            comprobarCancelacion();
            Cliente.Respuesta r = c.pedir(String.valueOf(k), Tramas.Tipo.MEDIDA, Cliente.TIMEOUT_MEDIDA_MS);
            Integer v = r.valida() ? Tramas.valorMedida(r.trama) : null;
            if (v == null) {
                ok = false;
            }
            entradas.add(new Coherencia.Entrada(k, v, s.ecuacionVigente(k)));
            d.append(k).append(" (").append(Fabrica.nombre(k)).append("): ").append(r.describir()).append('\n');
            poner(3, Estado.EN_CURSO, d.toString());
        }
        comprobarCancelacion();
        Integer eFin = null;
        if (v36) {
            Cliente.Respuesta re = c.pedir("e", Tramas.Tipo.MEDIDA, Cliente.TIMEOUT_MEDIDA_MS);
            eFin = re.valida() ? Tramas.valorMedida(re.trama) : null;
            d.append("e (después): ").append(re.describir());
            if (eFin == null) {
                ok = false;
                d.append("  <- obligatorio en V3.6");
            }
        } else {
            d.append("e: no se envía nunca a un V3 2020 (en SLV-002 deja el equipo sin responder)");
        }
        poner(3, ok ? Estado.OK : Estado.FALLO, d.toString());
        return new Integer[]{eIni, eFin};
    }
}
