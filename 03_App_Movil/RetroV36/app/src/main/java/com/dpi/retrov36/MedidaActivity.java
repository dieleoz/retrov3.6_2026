package com.dpi.retrov36;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Medida de patrones P1-P31: el operador elige patron y pulsa "Medir xN".
 * Cada lectura valida se guarda (memoria + CSV en el acto); las no validas
 * se muestran y van al registro de tramas, pero no al CSV.
 */
public class MedidaActivity extends Base {

    private Spinner spPatron;
    private EditText edN;
    private EditText edAsentamiento;
    private Button btnMedir;
    private Button btnParar;
    private TextView txtEstado;
    private TextView txtTabla;
    private List<Patron> patrones = new ArrayList<>();
    /** RTV 1.0: clave de @LEERV para medir R con un firmware sin x (V4 original). */
    private Spinner spClave;
    /** Medidas de R de esta pantalla (V4 original), en texto. */
    private final List<String> medidasR = new ArrayList<>();
    private volatile boolean midiendo;
    private volatile boolean parar;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        texto("Coloque el equipo sobre el patrón, elíjalo en la lista y pulse Medir. No toque el gatillo. "
                + "La app obtiene la lectura interna x: con 'e' si el equipo la tiene; si no, con '6' "
                + "(naranja intenso) invirtiendo su ecuación.");
        spPatron = new Spinner(this);
        raiz.addView(spPatron);
        spClave = new Spinner(this);
        List<String> claves = new ArrayList<>();
        for (char k : Fabrica.CODIGOS) {
            String t = Tramas.tramaLeerv(k);
            claves.add(k + "  " + t.substring(7, t.length() - 1) + "  (" + Fabrica.color(k) + ", tipo "
                    + (Tramas.esTipo1(k) ? "1" : "2 = otros papeles") + ")");
        }
        ArrayAdapter<String> adc = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, claves);
        adc.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spClave.setAdapter(adc);
        spClave.setVisibility(android.view.View.GONE);
        raiz.addView(spClave);
        edN = campo("Lecturas por patrón (N)", InputType.TYPE_CLASS_NUMBER);
        edN.setText("3");
        edAsentamiento = campo("Disparos de asentamiento descartados antes de la serie (0 = ninguno)",
                InputType.TYPE_CLASS_NUMBER);
        edAsentamiento.setText(String.valueOf(Sesion.get().disparosAsentamiento));
        btnMedir = boton("Medir ×N", v -> medir());
        btnParar = boton("Parar", v -> parar = true);
        fila(btnMedir, btnParar);
        txtEstado = texto("");
        titulo("Resultados de la sesión (x: lectura interna, no es retrorreflexión)");
        txtTabla = texto("");
        txtTabla.setTypeface(android.graphics.Typeface.MONOSPACE);
        boton("Compartir CSV y registro", v -> compartirTodo());
        try {
            patrones = Sesion.get().patrones(this);
        } catch (IOException | RuntimeException e) {
            alerta("Patrones", "No se pudo leer el CSV de patrones embebido: " + e.getMessage());
        }
        List<String> et = new ArrayList<>();
        for (Patron p : patrones) {
            et.add(p.toString());
        }
        ArrayAdapter<String> ad = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, et);
        ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spPatron.setAdapter(ad);
        pintarTabla();
    }

    @Override
    protected void refrescar() {
        super.refrescar();
        if (btnMedir == null) {
            return;
        }
        Sesion s = Sesion.get();
        boolean con = EnlaceSerie.instancia().estaConectado();
        btnMedir.setEnabled(con && !midiendo && !Pruebas.get().enCurso());
        spClave.setVisibility(sinX(s) ? android.view.View.VISIBLE : android.view.View.GONE);
        // P9-B3: el asentamiento no cambia mientras dura una calibracion.
        edAsentamiento.setEnabled(!s.calibrando());
        if (s.calibrando()) {
            edAsentamiento.setText(String.valueOf(s.acta.asentamiento));
        }
        btnParar.setEnabled(midiendo);
        if (!midiendo) {
            if (!con) {
                txtEstado.setText("Sin conexión.");
            } else if (s.apto == null) {
                txtEstado.setText("Antes de medir hay que pasar las pruebas del equipo.");
            } else if (sinX(s)) {
                txtEstado.setText(s.protocolo.nombre() + ": se mide R con @LEERV (sin x). Elija patrón y clave. "
                        + ProtocoloV4Original.MOTIVO + ".");
            } else if (!s.versionMedible()) {
                txtEstado.setText("Firmware " + s.versionTexto() + ": esta app no mide con él.");
            } else {
                String tx = s.protocolo.tramaX('1');
                txtEstado.setText("Listo. Método: " + (tx == null ? "'6' invertido" : "'" + tx + "' directa"));
            }
        }
    }

    private void medir() {
        Sesion s = Sesion.get();
        if (s.apto == null) {
            new AlertDialog.Builder(this).setTitle("Pruebas pendientes")
                    .setMessage("Antes de medir hay que pasar las pruebas del equipo.")
                    .setPositiveButton("Ir a pruebas", (d, w) -> startActivity(new Intent(this, PruebasActivity.class)))
                    .setNegativeButton("Cancelar", null).show();
            return;
        }
        if (sinX(s)) {
            medirR(s);
            return;
        }
        if (!s.versionMedible()) {
            alerta("No se puede medir", "Firmware " + s.versionTexto() + ".");
            return;
        }
        if (!s.apto && !s.medirNoAptoAceptado) {
            new AlertDialog.Builder(this).setTitle("Equipo NO APTO")
                    .setMessage("Las pruebas dieron NO APTO:\n" + s.resumenPruebas
                            + "\n\nPuede medir para diagnosticar, pero estas lecturas no deben usarse para calibrar. ¿Medir igualmente?")
                    .setPositiveButton("Medir igualmente", (d, w) -> {
                        s.medirNoAptoAceptado = true;
                        Registro.nota("el operador acepta medir con el equipo NO APTO");
                        medir();
                    })
                    .setNegativeButton("Cancelar", null).show();
            return;
        }
        int pos = spPatron.getSelectedItemPosition();
        if (pos < 0 || pos >= patrones.size()) {
            return;
        }
        int n;
        try {
            n = Integer.parseInt(edN.getText().toString().trim());
            if (n < 1 || n > 50) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            alerta("N", "Escriba un número de lecturas entre 1 y 50.");
            return;
        }
        try {
            int a = Integer.parseInt(edAsentamiento.getText().toString().trim());
            if (a < 0 || a > 5) {
                throw new NumberFormatException();
            }
            s.disparosAsentamiento = a;
        } catch (NumberFormatException e) {
            alerta("Asentamiento", "Escriba un número de disparos descartados entre 0 y 5.");
            return;
        }
        final Patron p = patrones.get(pos);
        final int total = n;
        midiendo = true;
        parar = false;
        pantallaEncendida(true);
        refrescar();
        Registro.nota("medida de " + p.nombre + " x" + total + " (" + s.identidad() + ")");
        Cliente.instancia().ejecutar(() -> {
            String fin = "Terminado.";
            try {
                if (s.disparosAsentamiento > 0) {
                    enUi(() -> txtEstado.setText("Disparo de asentamiento (se descarta)..."));
                    LecturaX.asentar(s);
                }
            } catch (IOException | InterruptedException e) {
                parar = true;
                fin = "Detenido en el asentamiento: " + EnlaceSerie.descripcion(e);
            }
            for (int i = 0; i < total && !parar; i++) {
                final int k = i + 1;
                enUi(() -> txtEstado.setText("Midiendo " + p.nombre + ": lectura " + k + " de " + total + "..."));
                try {
                    LecturaX.Lectura l = LecturaX.leer(s);
                    String bruta = l.respuesta.valida() ? l.respuesta.trama : l.respuesta.bruto;
                    if (l.valida()) {
                        Medida m = new Medida(Sesion.ahoraIso(), s.serie(), s.mac, s.firmware(), p,
                                l.codigo, bruta, l.x, l.u, l.metodo, "");
                        s.agregarMedida(this, m);
                    } else {
                        final String err = l.error;
                        enUi(() -> aviso("Lectura " + k + " no válida: " + err));
                    }
                } catch (IOException e) {
                    fin = "Detenido: " + EnlaceSerie.descripcion(e);
                    break;
                } catch (InterruptedException e) {
                    fin = "Interrumpido.";
                    break;
                } catch (RuntimeException e) {
                    fin = "Error interno: " + EnlaceSerie.descripcion(e);
                    Registro.nota(fin);
                    break;
                }
                enUi(this::pintarTabla);
            }
            if (parar && fin.equals("Terminado.")) {
                fin = "Parado por el operador.";
            }
            final String f = fin + Cliente.instancia().consejoSiMudo();
            enUi(() -> {
                midiendo = false;
                pantallaEncendida(false);
                refrescar();
                txtEstado.setText(f);
                pintarTabla();
            });
        });
    }

    /** true si el firmware no da x y solo se mide R (V4 original, RF-APP-U07). */
    private static boolean sinX(Sesion s) {
        return s.protocolo != null && !s.protocolo.daX();
    }

    /**
     * RF-APP-U07/U08: medida de R con @LEERV (V4 original). El entero se guarda como entero. El tipo 1 lleva su
     * etiqueta y no se compara con el certificado; cada medida de otros papeles lleva la previa de tipo 1 (el
     * error oculto del V4.1) o "estado previo desconocido".
     */
    private void medirR(Sesion s) {
        int pos = spPatron.getSelectedItemPosition();
        int kp = spClave.getSelectedItemPosition();
        if (pos < 0 || pos >= patrones.size() || kp < 0) {
            return;
        }
        int n;
        try {
            n = Integer.parseInt(edN.getText().toString().trim());
            if (n < 1 || n > 50) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            alerta("N", "Escriba un número de lecturas entre 1 y 50.");
            return;
        }
        final Patron p = patrones.get(pos);
        final char k = Fabrica.CODIGOS[kp];
        final Protocolo proto = s.protocolo;
        final int total = n;
        midiendo = true;
        parar = false;
        pantallaEncendida(true);
        refrescar();
        Registro.nota("medida R de " + p.nombre + " con " + proto.tramaMedida(k) + " x" + total + " (" + s.identidad() + ")");
        Cliente.instancia().ejecutar(() -> {
            String fin = "Terminado.";
            for (int i = 0; i < total && !parar; i++) {
                final int q = i + 1;
                enUi(() -> txtEstado.setText("Midiendo " + p.nombre + ": lectura " + q + " de " + total + "..."));
                try {
                    Cliente.Respuesta r = Cliente.instancia().pedir(proto.tramaMedida(k), proto.tipoMedida(),
                            proto.timeoutMedidaMs());
                    Integer v = r.valida() ? proto.valorMedida(r.trama) : null;
                    String nota = s.diario.anotar(k, v, false);
                    String et = proto.etiquetaMedida(k);
                    String cmp = v == null || Tramas.esTipo1(k) ? ""
                            : String.format(Locale.US, "; frente al certificado %.0f: %+.1f %%", p.valor,
                            100.0 * (v - p.valor) / p.valor);
                    String linea = String.format(Locale.US, "%s %s: %s [%s]%s; %s", p.nombre, proto.tramaMedida(k),
                            v == null ? r.describir() : String.valueOf(v), et, cmp, nota);
                    Registro.nota("medida R: " + linea + " (" + s.identidad() + ")");
                    synchronized (medidasR) {
                        medidasR.add(linea);
                    }
                } catch (IOException e) {
                    fin = "Detenido: " + EnlaceSerie.descripcion(e);
                    break;
                } catch (InterruptedException e) {
                    fin = "Interrumpido.";
                    break;
                } catch (RuntimeException e) {
                    fin = "Error interno: " + EnlaceSerie.descripcion(e);
                    Registro.nota(fin);
                    break;
                }
                enUi(this::pintarTabla);
            }
            final String f = fin + (s.diario.aviso().isEmpty() ? "" : "\n" + s.diario.aviso())
                    + Cliente.instancia().consejoSiMudo();
            enUi(() -> {
                midiendo = false;
                pantallaEncendida(false);
                refrescar();
                txtEstado.setText(f);
                pintarTabla();
            });
        });
    }

    private void pintarTabla() {
        synchronized (medidasR) {
            if (!medidasR.isEmpty()) {
                StringBuilder sb = new StringBuilder("Medidas de R (@LEERV, enteras):\n");
                for (String l : medidasR) {
                    sb.append(l).append('\n');
                }
                txtTabla.setText(sb.toString());
                return;
            }
        }
        Map<String, List<Medida>> por = new LinkedHashMap<>();
        for (Medida m : Sesion.get().medidas()) {
            List<Medida> l = por.get(m.patron.nombre);
            if (l == null) {
                l = new ArrayList<>();
                por.put(m.patron.nombre, l);
            }
            l.add(m);
        }
        if (por.isEmpty()) {
            txtTabla.setText("Sin medidas todavía.");
            return;
        }
        StringBuilder sb = new StringBuilder("patrón  cert  tipo color     n   media x   desv\n");
        for (Map.Entry<String, List<Medida>> e : por.entrySet()) {
            List<Double> xs = new ArrayList<>();
            for (Medida m : e.getValue()) {
                xs.add(m.x);
            }
            double[] v = Estadistica.aVector(xs);
            Patron p = e.getValue().get(0).patron;
            sb.append(String.format(Locale.US, "%-6s %5.0f %-4s %-8s %2d %9.1f %6.2f\n", p.nombre, p.valor,
                    p.tipo, p.color, v.length, Estadistica.media(v),
                    v.length > 1 ? Estadistica.desviacion(v) : Double.NaN));
        }
        txtTabla.setText(sb.toString());
    }
}
