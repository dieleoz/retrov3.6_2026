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
        texto(Asistente.cobertura());
        texto("Usa las medias por patrón de la sesión de medida (pantalla Medida de patrones), de los patrones "
                + "del color del código. Hacen falta al menos grado + 2 patrones distintos.");
        spAjuste = new Spinner(this);
        List<String> aj = new ArrayList<>();
        for (char k : Asistente.AJUSTABLES) {
            aj.add(k + "  " + Fabrica.nombre(k));
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
        return Asistente.AJUSTABLES[Math.max(0, i)];
    }

    private void ajustar() {
        char k = codigoAjuste();
        int grado = rgGrado.getCheckedRadioButtonId() == rgGrado.getChildAt(1).getId() ? 2 : 1;
        Sesion s = Sesion.get();
        if (s.leidas[Fabrica.indice(k)] == null) {
            alerta("Coeficientes", "Lea antes los coeficientes del equipo: el estado \"como llegó\" se calcula con ellos.");
            return;
        }
        propuesta = Asistente.proponer(k, grado, s.medidas(), s.ecuacionVigente(k));
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
        boolean ok = e != null && e.igualFloat32(anterior);
        return "restauración con " + (eraFabrica ? "#F," + k + "#" : "#S (valores anteriores)") + " -> " + res
                + (ok ? "; la relectura coincide con el estado anterior" : "; la relectura NO coincide con el estado anterior: "
                + (e == null ? "sin relectura" : e.toString()));
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
                    : (e.igualFloat32(ts.enviada) ? "la relectura coincide con lo enviado"
                    : (e.igualFloat32(anterior) ? "la relectura coincide con el estado ANTERIOR"
                    : "la relectura no coincide ni con lo enviado ni con lo anterior: " + e));
            String salida;
            if (!"OK".equals(res)) {
                // Ante #ERR o sin respuesta no se puede afirmar que no haya cambiado nada.
                salida = "#S -> " + res + ". Estado tras releer: " + estado + ".";
                if (e == null || !e.igualFloat32(anterior)) {
                    salida += " Se restaura el estado anterior: " + restaurar(k, anterior);
                }
            } else if (e != null && e.igualFloat32(ts.enviada)) {
                salida = "#S OK y " + estado + " (1 ulp). Mida ahora al menos un patrón con el código " + k
                        + " para verificar.";
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
