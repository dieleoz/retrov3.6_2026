package com.dpi.retrov36;

import android.graphics.Typeface;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.InputType;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Modo administrador (solo V3.6): PIN, lectura de coeficientes, asistente de
 * calibracion, restaurar fabrica y cambio de PIN. Las funciones de escritura
 * solo se muestran con el modo abierto (RF-APP-19).
 */
public class AdminActivity extends Base {

    /** El firmware cierra el modo a los 10 min sin tramas '#'; la app, un poco antes. */
    private static final long CADUCIDAD_MS = 9 * 60 * 1000 + 30 * 1000;
    private static final int MAX_FALLOS_PIN = 5;

    private TextView txtAcceso;
    private Button btnEntrar;
    private LinearLayout panel;
    private TextView txtCoef;
    private Spinner spAjuste;
    private RadioGroup rgGrado;
    private TextView txtAjuste;
    private Button btnEscribir;
    private Spinner spFabrica;
    private EditText edPinActual;
    private EditText edPinNuevo;
    private TextView txtOp;
    private volatile boolean ocupado;
    private Asistente.Propuesta propuesta;
    private TextView txtCal;
    private EditText edSerie;
    private Button btnSerie;
    private Button btnFT;

    private interface Tarea {
        String correr() throws IOException, InterruptedException;
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        txtAcceso = texto("");
        btnEntrar = boton("Entrar en modo administrador (PIN)", v -> entrar());
        txtOp = texto("");
        txtOp.setTypeface(Typeface.MONOSPACE);

        // Todo lo demas cuelga de 'panel', visible solo con el modo abierto.
        LinearLayout exterior = raiz;
        panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        exterior.addView(panel);
        raiz = panel;

        titulo("Coeficientes del equipo");
        boton("Leer coeficientes (#G x12, #GT#) y guardar copia", v -> leerCoeficientes("lectura manual"));
        txtCoef = texto("");
        txtCoef.setTypeface(Typeface.MONOSPACE);

        titulo("Asistente de calibración");
        List<Patron> catalogo = catalogo();
        texto(catalogo == null ? "No se pudo leer el catálogo de patrones." : Asistente.cobertura(catalogo));
        texto("Usa las medias por patrón de la sesión de medida (pantalla Medida de patrones), de los patrones "
                + "del color y la clase del código (tipo I = opacas 7, 8, a-d). Hacen falta al menos grado + 2 "
                + "niveles certificados distintos y un rango certificado amplio: con rango estrecho no se ajusta.");
        spAjuste = new Spinner(this);
        List<String> aj = new ArrayList<>();
        for (char k : Fabrica.CODIGOS) {
            aj.add(catalogo == null ? k + "  " + Fabrica.nombre(k) : Asistente.cobertura(k, catalogo).texto());
        }
        spAjuste.setAdapter(adaptador(aj));
        panel.addView(spAjuste);
        rgGrado = new RadioGroup(this);
        rgGrado.setOrientation(RadioGroup.HORIZONTAL);
        RadioButton g1 = new RadioButton(this);
        g1.setText("Grado 1");
        g1.setId(View.generateViewId());
        RadioButton g2 = new RadioButton(this);
        g2.setText("Grado 2");
        g2.setId(View.generateViewId());
        rgGrado.addView(g1);
        rgGrado.addView(g2);
        rgGrado.check(g1.getId());
        panel.addView(rgGrado);
        Button ajustar = boton("Ajustar", v -> ajustar());
        btnEscribir = boton("Escribir en el equipo...", v -> confirmarEscritura());
        fila(ajustar, btnEscribir);
        txtAjuste = texto("");
        txtAjuste.setTypeface(Typeface.MONOSPACE);

        titulo("Serie y fecha de calibración (firmware 3.6.2)");
        txtCal = texto("");
        edSerie = campo("Serie del equipo (1-12 caracteres, sin # ni ,)", InputType.TYPE_CLASS_TEXT);
        btnSerie = boton("Grabar serie (#SN) y verificar (#GN)", v -> grabarSerie());
        btnFT = boton("Restaurar temperatura de fábrica (#FT#)", v -> restaurarTemperatura());

        titulo("Volver a fábrica");
        spFabrica = new Spinner(this);
        List<String> fa = new ArrayList<>();
        for (char k : Fabrica.CODIGOS) {
            fa.add(k + "  " + Fabrica.nombre(k));
        }
        spFabrica.setAdapter(adaptador(fa));
        panel.addView(spFabrica);
        Button uno = boton("Restaurar este (#F,k#)", v -> confirmarFabrica(false));
        Button todos = boton("Restaurar todos (#F,*#)", v -> confirmarFabrica(true));
        fila(uno, todos);

        titulo("Cambiar PIN");
        edPinActual = campo("PIN actual", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        edPinNuevo = campo("PIN nuevo (4 dígitos)", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        boton("Cambiar PIN (#P#)", v -> cambiarPin());

        titulo("Salir");
        boton("Cerrar modo administrador (#Q#)", v -> salir());
        raiz = exterior;
        btnEscribir.setEnabled(false);
    }

    private ArrayAdapter<String> adaptador(List<String> l) {
        ArrayAdapter<String> a = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, l);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return a;
    }

    /** Modo abierto y sin caducar. */
    private boolean abierto() {
        Sesion s = Sesion.get();
        if (s.adminDesbloqueado && SystemClock.elapsedRealtime() - s.ultimaAlmohadillaMs > CADUCIDAD_MS) {
            s.adminDesbloqueado = false;
            Registro.nota("modo admin caducado (10 min sin tramas #)");
        }
        return s.adminDesbloqueado && EnlaceSerie.instancia().estaConectado();
    }

    @Override
    protected void refrescar() {
        super.refrescar();
        if (panel == null) {
            return;
        }
        Sesion s = Sesion.get();
        boolean v36 = s.version == Sesion.Version.V36;
        boolean ab = abierto();
        panel.setVisibility(ab ? View.VISIBLE : View.GONE);
        btnEntrar.setEnabled(v36 && !ab && !ocupado && EnlaceSerie.instancia().estaConectado());
        if (!v36) {
            txtAcceso.setText("Modo administrador desactivado: el firmware detectado es " + s.firmware()
                    + ". Sólo el firmware V3.6 guarda la calibración en EEPROM y acepta órdenes #...#. "
                    + "En un V3 2020 las ecuaciones están en el código: calibrar exige recompilar y grabar por ICSP "
                    + "(PROCEDIMIENTO-Calibracion-V3-K42.md §6).");
        } else if (ab) {
            txtAcceso.setText("Modo administrador ABIERTO. Se cierra con #Q#, al salir de esta pantalla, "
                    + "al apagar el equipo o a los 10 min sin órdenes #.");
            txtAcceso.setBackgroundColor(AMARILLO);
        } else {
            txtAcceso.setText("Modo administrador cerrado." + (s.apto == null ? " Pruebas sin hacer."
                    : (s.apto ? "" : " Equipo NO APTO: entrar exige confirmación.")));
            txtAcceso.setBackgroundColor(GRIS);
        }
        btnEscribir.setEnabled(ab && !ocupado && propuesta != null && propuesta.escribible());
        boolean v362 = s.es362();
        txtCal.setText(s.datosCalibracion() + (v362 ? "" : "\nGrabar serie y restaurar temperatura exigen la 3.6.2."));
        btnSerie.setEnabled(ab && !ocupado && v362);
        btnFT.setEnabled(ab && !ocupado && v362);
    }

    /** Ejecuta una operacion con el equipo en el hilo de trabajo y muestra el resultado. */
    private void op(String etiqueta, boolean requiereAdmin, Tarea t) {
        if (ocupado) {
            return;
        }
        if (requiereAdmin && !abierto()) {
            alerta("Modo administrador", "El modo está cerrado o ha caducado. Vuelva a entrar con el PIN.");
            refrescar();
            return;
        }
        ocupado = true;
        txtOp.setText(etiqueta + "...");
        refrescar();
        Cliente.instancia().ejecutar(() -> {
            String res;
            try {
                res = t.correr();
            } catch (IOException | InterruptedException e) {
                res = "Error: " + EnlaceSerie.descripcion(e);
            } catch (RuntimeException e) {
                res = "Error interno: " + EnlaceSerie.descripcion(e);
            }
            final String fin = etiqueta + ": " + res;
            Registro.nota(fin);
            enUi(() -> {
                ocupado = false;
                txtOp.setText(fin);
                refrescar();
            });
        });
    }

    /** Analiza #OK# / #ERR,motivo#; cierra el modo si el firmware dice BLOQUEADO. */
    private String resultado(Cliente.Respuesta r) {
        if (Tramas.esOk(r.trama)) {
            return "OK";
        }
        String m = Tramas.motivoError(r.trama);
        if ("BLOQUEADO".equals(m)) {
            Sesion.get().adminDesbloqueado = false;
            return "el equipo responde BLOQUEADO: modo admin cerrado o caducado";
        }
        return m != null ? "ERROR " + m : r.describir();
    }

    // ------------------------------------------------------------------ PIN

    private void entrar() {
        Sesion s = Sesion.get();
        if (s.version != Sesion.Version.V36) {
            refrescar();
            return;
        }
        if (s.fallosPin >= MAX_FALLOS_PIN) {
            alerta("PIN bloqueado", "Van " + s.fallosPin + " fallos: el equipo bloquea #L hasta apagarlo. "
                    + "Apague y encienda el equipo, y vuelva a conectar.");
            return;
        }
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(16), dp(8), dp(16), 0);
        final CheckBox confirma = new CheckBox(this);
        boolean noApto = !Boolean.TRUE.equals(s.apto);
        if (noApto) {
            TextView t = new TextView(this);
            t.setText(s.apto == null ? "Las pruebas del equipo no se han hecho."
                    : "Las pruebas dieron NO APTO:\n" + s.resumenPruebas);
            c.addView(t);
            confirma.setText("Entiendo que el equipo no está apto y quiero entrar igualmente");
            c.addView(confirma);
        }
        final EditText pin = campoPin();
        c.addView(pin);
        new AlertDialog.Builder(this)
                .setTitle(noApto ? "Entrar sin APTO (override)" : "PIN del equipo")
                .setView(c)
                .setPositiveButton("Entrar", (d, w) -> {
                    if (noApto && !confirma.isChecked()) {
                        alerta("Override", "Hay que marcar la confirmación para entrar con el equipo no apto.");
                        return;
                    }
                    String p = pin.getText().toString().trim();
                    if (!p.matches("\\d{4}")) {
                        alerta("PIN", "El PIN son 4 dígitos.");
                        return;
                    }
                    if (noApto) {
                        s.overrideAdmin = true;
                        Registro.nota("OVERRIDE: el operador entra en modo admin con el equipo "
                                + (s.apto == null ? "sin pruebas" : "NO APTO"));
                    }
                    login(p);
                })
                .setNegativeButton("Cancelar", null).show();
    }

    private void login(String pin) {
        op("Entrar (#L)", false, () -> {
            Cliente.Respuesta r = Cliente.instancia().pedir("#L," + pin + "#", Tramas.Tipo.ADMIN, Cliente.TIMEOUT_ADMIN_MS);
            Sesion s = Sesion.get();
            if (Tramas.esOk(r.trama)) {
                s.adminDesbloqueado = true;
                s.fallosPin = 0;
                return "modo administrador abierto";
            }
            String m = Tramas.motivoError(r.trama);
            if ("PIN".equals(m)) {
                s.fallosPin++;
                return "PIN incorrecto (fallo " + s.fallosPin + " de " + MAX_FALLOS_PIN
                        + "; al " + MAX_FALLOS_PIN + "º el equipo bloquea #L hasta apagarlo)";
            }
            if ("BLOQUEADO".equals(m)) {
                s.fallosPin = MAX_FALLOS_PIN;
                return "el equipo tiene #L bloqueado por fallos de PIN: apáguelo y enciéndalo";
            }
            return r.describir();
        });
    }

    private void cambiarPin() {
        String a = edPinActual.getText().toString().trim();
        String n = edPinNuevo.getText().toString().trim();
        if (!a.matches("\\d{4}") || !n.matches("\\d{4}")) {
            alerta("PIN", "Los dos PIN son de 4 dígitos.");
            return;
        }
        new AlertDialog.Builder(this).setTitle("Cambiar PIN")
                .setMessage("Se cambiará el PIN del equipo. Anote el nuevo: la app no lo guarda.")
                .setPositiveButton("Cambiar", (d, w) -> op("Cambiar PIN (#P)", true, () -> {
                    Cliente.Respuesta r = Cliente.instancia().pedir("#P," + a + "," + n + "#",
                            Tramas.Tipo.ADMIN, Cliente.TIMEOUT_ADMIN_MS);
                    return resultado(r);
                }))
                .setNegativeButton("Cancelar", null).show();
        edPinActual.setText("");
        edPinNuevo.setText("");
    }

    private void salir() {
        op("Cerrar modo admin (#Q#)", false, () -> {
            Cliente.Respuesta r = Cliente.instancia().pedir("#Q#", Tramas.Tipo.ADMIN, Cliente.TIMEOUT_ADMIN_MS);
            Sesion.get().adminDesbloqueado = false;
            return resultado(r);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Al salir de la pantalla se cierra el modo (RF-APP-19).
        if (Sesion.get().adminDesbloqueado && EnlaceSerie.instancia().estaConectado()) {
            Sesion.get().adminDesbloqueado = false;
            Cliente.instancia().ejecutar(() -> {
                try {
                    Cliente.instancia().pedir("#Q#", Tramas.Tipo.ADMIN, Cliente.TIMEOUT_ADMIN_MS);
                } catch (IOException | InterruptedException e) {
                    Registro.nota("#Q# al salir: " + EnlaceSerie.descripcion(e));
                }
            });
        }
    }

    // --------------------------------------------------------- coeficientes

    /** Lee #G x12 y #GT# y guarda copia. Llamar desde el hilo de trabajo. */
    private String leerTodo(String motivo) throws IOException, InterruptedException {
        Sesion s = Sesion.get();
        StringBuilder sb = new StringBuilder();
        int fallos = 0;
        for (int i = 0; i < Fabrica.CODIGOS.length; i++) {
            char k = Fabrica.CODIGOS[i];
            Cliente.Respuesta r = Cliente.instancia().pedir("#G," + k + "#", Tramas.Tipo.ADMIN, Cliente.TIMEOUT_ADMIN_MS);
            Ecuacion e = r.valida() ? Tramas.parsearG(r.trama, k) : null;
            if (e == null) {
                fallos++;
                sb.append(k).append(": ").append(r.describir()).append('\n');
                continue;
            }
            s.leidas[i] = e;
            sb.append(k).append(' ').append(Tramas.coeficiente(e.c3)).append(' ').append(Tramas.coeficiente(e.c2))
                    .append(' ').append(Tramas.coeficiente(e.c1)).append(' ').append(Tramas.coeficiente(e.c0))
                    .append(e.igualFloat32(Fabrica.ecuacion(k)) ? "  = fábrica" : "  AJUSTADA").append('\n');
        }
        Cliente.Respuesta rt = Cliente.instancia().pedir("#GT#", Tramas.Tipo.ADMIN, Cliente.TIMEOUT_ADMIN_MS);
        double[] t = rt.valida() ? Tramas.parsearGT(rt.trama) : null;
        if (t != null) {
            s.temperatura = t;
            sb.append("T ").append(Tramas.coeficiente(t[0])).append(' ').append(Tramas.coeficiente(t[1]))
                    .append(' ').append(Tramas.coeficiente(t[2])).append('\n');
        } else {
            fallos++;
            sb.append("#GT#: ").append(rt.describir()).append('\n');
        }
        s.guardarCoeficientes(this, motivo);
        final String tabla = "código c3 c2 c1 c0\n" + sb;
        enUi(() -> txtCoef.setText(tabla));
        return fallos == 0 ? "12 juegos y temperatura leídos; copia guardada" : fallos + " lecturas fallidas";
    }

    private void leerCoeficientes(String motivo) {
        op("Leer coeficientes", false, () -> leerTodo(motivo));
    }

    // -------------------------------------------------------------- ajuste

    private char codigoAjuste() {
        int i = spAjuste.getSelectedItemPosition();
        return Fabrica.CODIGOS[Math.max(0, i)];
    }

    private List<Patron> catalogo() {
        try {
            return Sesion.get().patrones(this);
        } catch (java.io.IOException | RuntimeException e) {
            return null;
        }
    }

    private void ajustar() {
        char k = codigoAjuste();
        int grado = rgGrado.getCheckedRadioButtonId() == rgGrado.getChildAt(1).getId() ? 2 : 1;
        Sesion s = Sesion.get();
        if (s.leidas[Fabrica.indice(k)] == null) {
            alerta("Coeficientes", "Lea antes los coeficientes del equipo: el estado \"como llegó\" se calcula con ellos.");
            return;
        }
        List<Patron> cat = catalogo();
        if (cat == null) {
            alerta("Patrones", "No se pudo leer el catálogo de patrones: sin él no se ajusta.");
            return;
        }
        propuesta = Asistente.proponer(k, grado, s.medidasParaAjuste(), s.ecuacionVigente(k), cat);
        txtAjuste.setText(propuesta.informe);
        Registro.nota("asistente: " + propuesta.informe);
        refrescar();
    }

    private void confirmarEscritura() {
        final Asistente.Propuesta p = propuesta;
        if (p == null || !p.escribible()) {
            return;
        }
        final char k = p.codigo;
        final Tramas.TramaS ts = Tramas.tramaS(k, p.ajuste.ecuacion);
        if (ts == null) {
            alerta("Trama", "La trama #S no cabe en " + Tramas.MAX_TRAMA + " bytes.");
            return;
        }
        String fw = Asistente.criterioFirmwareS(ts.enviada);
        if (fw != null) {
            // Mismo criterio que #S del firmware 3.6.1: no se envia nada que vaya a dar #ERR sin explicacion.
            alerta("No se escribe", fw);
            return;
        }
        Ecuacion ant = Sesion.get().ecuacionVigente(k);
        Ecuacion nue = ts.enviada;
        StringBuilder m = new StringBuilder();
        m.append("Código ").append(k).append(" (").append(Fabrica.nombre(k)).append(")\n\n");
        m.append("         actual            nuevo\n");
        String[] nom = {"c3", "c2", "c1", "c0"};
        double[] a = ant.comoVector();
        double[] n = nue.comoVector();
        for (int i = 0; i < 4; i++) {
            m.append(nom[i]).append("  ").append(Tramas.coeficiente(a[i])).append("  ")
                    .append(Tramas.coeficiente(n[i])).append('\n');
        }
        m.append("\nR sobre los patrones (actual -> nuevo, certificado):\n");
        for (Asistente.Punto q : p.puntos) {
            m.append(String.format(Locale.US, "%s: %.0f -> %.0f  (%.0f)\n", q.patron.nombre,
                    (double) ant.respuestaFloat32((int) Math.round(q.x)),
                    (double) nue.respuestaFloat32((int) Math.round(q.x)), q.patron.valor));
        }
        m.append("\nTrama: ").append(ts.texto).append("\n\nAntes de escribir se guarda una copia de los 12 juegos y de la temperatura. "
                + "Ajuste contra patrones, no calibración trazable.");
        new AlertDialog.Builder(this).setTitle("Confirmar escritura en EEPROM")
                .setMessage(m.toString())
                .setPositiveButton("Escribir", (d, w) -> escribir(k, ts))
                .setNegativeButton("Cancelar", null).show();
    }

    /** Relee #G,k#; null si no se pudo. */
    private Ecuacion releer(char k) throws IOException, InterruptedException {
        Cliente.Respuesta g = Cliente.instancia().pedir("#G," + k + "#", Tramas.Tipo.ADMIN, Cliente.TIMEOUT_ADMIN_MS);
        Ecuacion e = g.valida() ? Tramas.parsearG(g.trama, k) : null;
        if (e != null) {
            Sesion.get().leidas[Fabrica.indice(k)] = e;
        }
        return e;
    }

    /**
     * Condicion C3: si la relectura no coincide con lo escrito, se vuelve al
     * estado anterior: #F,k# si el anterior era el de fabrica (RF-APP-18: nunca
     * se reescribe fabrica con #S); si no, #S con los coeficientes anteriores.
     */
    private String restaurar(char k, Ecuacion anterior) throws IOException, InterruptedException {
        boolean eraFabrica = anterior.igualFloat32(Fabrica.ecuacion(k));
        String trama;
        if (eraFabrica) {
            trama = "#F," + k + "#";
        } else {
            Tramas.TramaS ts = Tramas.tramaS(k, anterior);
            if (ts == null) {
                return "no se pudo formar la trama de restauración";
            }
            trama = ts.texto;
        }
        Cliente.Respuesta r = Cliente.instancia().pedir(trama, Tramas.Tipo.ADMIN, 5000);
        String res = resultado(r);
        Ecuacion e = releer(k);
        // #F repone el valor de ROM (misma impresion: ULP_G). #S con los valores
        // anteriores pasa otra vez por strtod: impresion + strtod + impresion.
        boolean ok = e != null && e.igualFloat32(anterior, eraFabrica ? Ecuacion.ULP_G
                : Ecuacion.ULP_S + Ecuacion.ULP_G);
        return "restauración con " + (eraFabrica ? "#F," + k + "#" : "#S (valores anteriores)") + " -> " + res
                + (ok ? "; la relectura coincide con el estado anterior" : "; la relectura NO coincide con el estado anterior: "
                + (e == null ? "sin relectura" : e.toString()));
    }

    /**
     * Comprobacion por evaluacion tras #S (la que importa de verdad): #E,k,x en
     * x = 500, 1000, 2000, 3000, 4000 tiene que dar lo mismo que la curva que la
     * app queria escribir, emulada en float32, con +/-1 cuenta.
     * @return null si cuadra; si no, el detalle.
     */
    private String comprobarPorE(char k, Ecuacion enviada) throws IOException, InterruptedException {
        StringBuilder mal = new StringBuilder();
        for (int x : Pruebas.X_COMPROBACION) {
            Cliente.Respuesta r = Cliente.instancia().pedir("#E," + k + "," + x + "#", Tramas.Tipo.ADMIN,
                    Cliente.TIMEOUT_ADMIN_MS);
            Integer v = r.valida() ? Tramas.parsearE(r.trama, k) : null;
            int esperado = enviada.respuestaFloat32(x);
            if (v == null) {
                mal.append(" x=").append(x).append(": ").append(r.describir()).append(';');
            } else if (Math.abs(v - esperado) > 1) {
                mal.append(" x=").append(x).append(": #E=").append(v).append(", esperado ").append(esperado).append(';');
            }
        }
        return mal.length() == 0 ? null : "#E no reproduce la curva enviada:" + mal;
    }

    private void escribir(char k, Tramas.TramaS ts) {
        op("Escribir código " + k, true, () -> {
            String copia = leerTodo("copia antes de escribir el codigo " + k);
            Registro.nota("copia previa: " + copia);
            Ecuacion anterior = Sesion.get().leidas[Fabrica.indice(k)];
            if (anterior == null) {
                return "no se pudo leer el estado anterior del código " + k + ": no se escribe nada";
            }
            Cliente.Respuesta r = Cliente.instancia().pedir(ts.texto, Tramas.Tipo.ADMIN, 5000);
            String res = resultado(r);
            Ecuacion e = releer(k);
            String estado = e == null ? "la relectura #G falló"
                    : (e.igualFloat32(ts.enviada, Ecuacion.ULP_S) ? "la relectura coincide con lo enviado"
                    : (e.igualFloat32(anterior) ? "la relectura coincide con el estado ANTERIOR"
                    : "la relectura no coincide ni con lo enviado ni con lo anterior: " + e));
            String salida;
            if (!"OK".equals(res)) {
                // Ante #ERR o sin respuesta no se puede afirmar que no haya cambiado nada.
                salida = "#S -> " + res + ". Estado tras releer: " + estado + ".";
                if (e == null || !e.igualFloat32(anterior)) {
                    salida += " Se restaura el estado anterior: " + restaurar(k, anterior);
                }
            } else if (e != null && e.igualFloat32(ts.enviada, Ecuacion.ULP_S)) {
                String porE = comprobarPorE(k, ts.enviada);
                if (porE == null) {
                    salida = "#S OK y " + estado + " (" + Ecuacion.ULP_S + " ulp); #E en 5 puntos coincide con la curva "
                            + "enviada (±1). " + grabarFechaHoy() + " Mida ahora al menos un patrón con el código " + k
                            + " para verificar.";
                } else {
                    salida = "#S OK pero " + porE + ". Se restaura el estado anterior: " + restaurar(k, anterior);
                }
            } else {
                salida = "#S OK pero " + estado + ". Se restaura el estado anterior: " + restaurar(k, anterior);
            }
            Sesion.get().guardarCoeficientes(this, "despues de escribir el codigo " + k);
            final String aviso = salida;
            if (!salida.startsWith("#S OK y")) {
                enUi(() -> alerta("Escritura del código " + k, aviso));
            }
            return salida;
        });
    }

    // ------------------------------------------------------ serie y fecha (3.6.2)

    /**
     * Tras una calibracion verificada: #SC con la fecha de hoy y relectura #GC.
     * Llamar desde el hilo de trabajo. Devuelve el texto para el operador.
     */
    private String grabarFechaHoy() throws IOException, InterruptedException {
        Sesion s = Sesion.get();
        if (!s.es362()) {
            return "Firmware " + s.firmware() + ": no tiene #SC, la fecha de calibración NO se graba.";
        }
        String hoy = Calibracion.hoy();
        Cliente.Respuesta r = Cliente.instancia().pedir("#SC," + hoy + "#", Tramas.Tipo.ADMIN, 5000);
        String res = resultado(r);
        Cliente.Respuesta g = Cliente.instancia().pedir("#GC#", Tramas.Tipo.ADMIN, Cliente.TIMEOUT_ADMIN_MS);
        String leida = Calibracion.fechaDe(g.trama);
        if (leida != null) {
            s.fechaCalibracion = leida;
        }
        if ("OK".equals(res) && hoy.equals(leida)) {
            return "Fecha de calibración " + hoy + " grabada y verificada con #GC# (vence "
                    + Calibracion.vencimiento(hoy) + ").";
        }
        return "ATENCIÓN: la fecha de calibración no quedó grabada (#SC -> " + res + ", #GC# -> "
                + g.describir() + ").";
    }

    private void grabarSerie() {
        final String serie = edSerie.getText().toString().trim();
        String mal = Calibracion.motivoSerieInvalida(serie);
        if (mal != null) {
            alerta("Serie no válida", mal);
            return;
        }
        new AlertDialog.Builder(this).setTitle("Grabar serie")
                .setMessage("Se grabará la serie \"" + serie + "\" en la EEPROM del equipo (actual: "
                        + Sesion.get().serieEquipo + ").\n\nTrama: #SN," + serie + "#")
                .setPositiveButton("Grabar", (d, w) -> op("Grabar serie", true, () -> {
                    Cliente.Respuesta r = Cliente.instancia().pedir("#SN," + serie + "#", Tramas.Tipo.ADMIN, 5000);
                    String res = resultado(r);
                    Cliente.Respuesta g = Cliente.instancia().pedir("#GN#", Tramas.Tipo.ADMIN, Cliente.TIMEOUT_ADMIN_MS);
                    String leida = g.valida() ? Calibracion.serieDe(g.trama) : null;
                    if (leida != null) {
                        Sesion.get().serieEquipo = leida;
                    }
                    return "#SN -> " + res + "; #GN# -> " + g.describir()
                            + (serie.equals(leida) ? " (verificada)" : " (NO coincide con lo enviado)");
                }))
                .setNegativeButton("Cancelar", null).show();
    }

    private void restaurarTemperatura() {
        double[] t = Sesion.get().temperatura;
        new AlertDialog.Builder(this).setTitle("Restaurar temperatura de fábrica")
                .setMessage("El factor de temperatura vuelve a los valores de fábrica (gui.c:42-44). Actual: "
                        + (t == null ? "sin leer" : Tramas.coeficiente(t[0]) + ", " + Tramas.coeficiente(t[1]) + ", "
                        + Tramas.coeficiente(t[2])) + ".\n\nTrama: #FT#")
                .setPositiveButton("Restaurar", (d, w) -> op("Restaurar temperatura (#FT#)", true, () -> {
                    Cliente.Respuesta r = Cliente.instancia().pedir("#FT#", Tramas.Tipo.ADMIN, 5000);
                    String res = resultado(r);
                    Cliente.Respuesta gt = Cliente.instancia().pedir("#GT#", Tramas.Tipo.ADMIN, Cliente.TIMEOUT_ADMIN_MS);
                    double[] nt = gt.valida() ? Tramas.parsearGT(gt.trama) : null;
                    if (nt != null) {
                        Sesion.get().temperatura = nt;
                    }
                    return "#FT# -> " + res + "; #GT# -> " + gt.describir();
                }))
                .setNegativeButton("Cancelar", null).show();
    }

    // ------------------------------------------------------------- fabrica

    private void confirmarFabrica(boolean todos) {
        int i = Math.max(0, spFabrica.getSelectedItemPosition());
        final char k = Fabrica.CODIGOS[i];
        final String trama = todos ? "#F,*#" : "#F," + k + "#";
        new AlertDialog.Builder(this).setTitle("Volver a fábrica")
                .setMessage((todos ? "Los 12 códigos" : "El código " + k + " (" + Fabrica.nombre(k) + ")")
                        + " volverán a las ecuaciones de fábrica de 2020. Antes se guarda una copia de los actuales.\n\nTrama: " + trama)
                .setPositiveButton("Restaurar", (d, w) -> op("Restaurar " + trama, true, () -> {
                    leerTodo("copia antes de " + trama);
                    Cliente.Respuesta r = Cliente.instancia().pedir(trama, Tramas.Tipo.ADMIN, 5000);
                    String res = resultado(r);
                    if ("OK".equals(res)) {
                        leerTodo("despues de " + trama);
                    }
                    return res;
                }))
                .setNegativeButton("Cancelar", null).show();
    }
}
