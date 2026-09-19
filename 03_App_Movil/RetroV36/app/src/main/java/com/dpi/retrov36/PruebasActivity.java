package com.dpi.retrov36;

import android.graphics.Typeface;
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
import java.util.List;
import java.util.Locale;

/** Pantalla "Pruebas del equipo": seis pruebas en verde o rojo y APTO / NO APTO. */
public class PruebasActivity extends Base implements Pruebas.Oyente {

    public static final String EXTRA_AUTO = "auto";

    private TextView txtResumen;
    private EditText edTolerancia;
    private Button btnIniciar;
    private Button btnCancelar;
    private final List<TextView> filas = new ArrayList<>();
    private Spinner spPatronLb;
    private Button btnLineaBase;
    private Button btnLineaBaseForzada;
    private TextView txtLineaBase;
    private List<Patron> patrones = new ArrayList<>();
    private volatile boolean lineaBaseEnCurso;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        texto("Antes de iniciar: coloque el equipo sobre UN patrón de nivel medio o alto (p. ej. un blanco "
                + "XI) y no lo mueva ni toque el gatillo hasta que terminen. Son unas 20 medidas "
                + "(unos 45 s con V3 2020; más en V3.6 por las lecturas #G y #E). El equipo se enciende y pita en cada una.");
        edTolerancia = campo("Tolerancia de coherencia, cuentas de x", InputType.TYPE_CLASS_NUMBER);
        edTolerancia.setText(String.format(Locale.US, "%.0f", Pruebas.get().tolerancia));
        btnIniciar = boton("Iniciar pruebas", v -> iniciar());
        btnCancelar = boton("Cancelar", v -> Pruebas.get().cancelar());
        fila(btnIniciar, btnCancelar);
        txtResumen = titulo("");
        for (Pruebas.Prueba p : Pruebas.get().lista) {
            TextView t = texto("");
            t.setTextSize(14);
            filas.add(t);
        }
        boton("Compartir registro y datos", v -> compartirTodo());

        titulo("Línea base previa a grabar");
        texto("Con el firmware ORIGINAL del equipo, antes de grabar la V3.6. Registra, etiquetado "
                + LineaBase.ETIQUETA + ": T-B03, los 12 códigos sobre el patrón elegido; T-B09, la respuesta a 9 ("
                + LineaBase.REPETICIONES_9 + " veces); y T-B07, los 12 códigos con el cabezal TAPADO (oscuro). "
                + "Va al registro de tramas y a lineabase_<serie>_<fecha>.csv. Unos 2 minutos.");
        spPatronLb = new Spinner(this);
        raiz.addView(spPatronLb);
        try {
            patrones = Sesion.get().patrones(this);
        } catch (IOException | RuntimeException e) {
            aviso("No se pudo leer el CSV de patrones: " + e.getMessage());
        }
        List<String> et = new ArrayList<>();
        for (Patron p : patrones) {
            et.add(p.toString());
        }
        ArrayAdapter<String> ad = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, et);
        ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spPatronLb.setAdapter(ad);
        btnLineaBase = boton("Línea base", v -> lineaBase());
        btnLineaBaseForzada = boton("Línea base forzando V3 2020 sin e (sin detectar)", v -> lineaBaseForzada());
        txtLineaBase = texto("");
        txtLineaBase.setTypeface(Typeface.MONOSPACE);

        if (b == null && getIntent().getBooleanExtra(EXTRA_AUTO, false)
                && !Pruebas.get().enCurso() && Sesion.get().apto == null) {
            new AlertDialog.Builder(this)
                    .setTitle("Pruebas del equipo")
                    .setMessage("Conectado. Antes de medir o calibrar hay que pasar las pruebas.\n\n"
                            + "Coloque el equipo sobre un patrón y no lo mueva. ¿Iniciar ahora?")
                    .setPositiveButton("Iniciar", (d, w) -> iniciar())
                    .setNegativeButton("Más tarde", null)
                    .show();
        }
    }

    private void lineaBase() {
        Sesion s = Sesion.get();
        if (!EnlaceSerie.instancia().estaConectado()) {
            alerta("Sin conexión", "Conecte primero con el equipo.");
            return;
        }
        if (Pruebas.get().enCurso()) {
            return;
        }
        if (s.version == Sesion.Version.V4) {
            alerta("Firmware no admitido", "Es un V4: esta app no aplica.");
            return;
        }
        int pos = spPatronLb.getSelectedItemPosition();
        if (pos < 0 || pos >= patrones.size()) {
            return;
        }
        final Patron p = patrones.get(pos);
        new AlertDialog.Builder(this).setTitle("Línea base: T-B03 y T-B09")
                .setMessage("Firmware: " + s.firmware() + (s.version == Sesion.Version.SIN_DETECTAR
                        ? " (se detectará primero: #V#, 9, 6; nunca e)" : "") + "\n\nColoque el equipo sobre " + p.nombre
                        + " y no lo mueva ni toque el gatillo. Se enviarán los 12 códigos y después 9.")
                .setPositiveButton("Empezar", (d, w) -> fase1(p))
                .setNegativeButton("Cancelar", null).show();
    }

    private void lineaBaseForzada() {
        if (!EnlaceSerie.instancia().estaConectado() || Pruebas.get().enCurso()) {
            return;
        }
        int pos = spPatronLb.getSelectedItemPosition();
        if (pos < 0 || pos >= patrones.size()) {
            return;
        }
        final Patron p = patrones.get(pos);
        new AlertDialog.Builder(this).setTitle("Línea base forzada")
                .setMessage("No se detecta la versión: se trata el equipo como V3 2020 sin e. Se envían los 12 "
                        + "códigos (1-8, a-d) y 9; nunca e ni #.\n\nColoque el equipo sobre " + p.nombre + ".")
                .setPositiveButton("Empezar", (d, w) -> fase1(p, true))
                .setNegativeButton("Cancelar", null).show();
    }

    private void fase1(Patron p) {
        fase1(p, false);
    }

    /** @param forzar true: no se detecta; se trata el equipo como V3 2020 sin 'e'. */
    private void fase1(Patron p, boolean forzar) {
        lineaBaseEnCurso = true;
        pantallaEncendida(true);
        cambio();
        Cliente.instancia().ejecutar(() -> {
            String r;
            boolean ok = true;
            try {
                String det = "";
                Sesion s = Sesion.get();
                if (forzar) {
                    s.version = Sesion.Version.V3_2020;
                    s.eDisponible = false;
                    det = "Versión FORZADA por el operador: V3 2020 sin e (la detección no identificó el equipo).\n";
                    Registro.nota(LineaBase.ETIQUETA + " version forzada por el operador: V3 2020 sin e");
                } else if (s.version == Sesion.Version.SIN_DETECTAR || s.version == Sesion.Version.DESCONOCIDA) {
                    progresoLb("detección de versión...");
                    det = "Detección:\n" + Pruebas.get().detectar(s) + "\n";
                    Registro.nota(LineaBase.ETIQUETA + " deteccion: " + det);
                    if (!s.versionMedible()) {
                        final String detF = det;
                        final boolean esV4 = s.version == Sesion.Version.V4;
                        enUi(() -> {
                            lineaBaseEnCurso = false;
                            pantallaEncendida(false);
                            btnLineaBase.setText("Línea base");
                            txtLineaBase.setText(detF + Cliente.instancia().consejoSiMudo());
                            cambio();
                            if (esV4) {
                                alerta("Es un V4", "La detección identificó un V4: esta app no aplica.");
                                return;
                            }
                            new AlertDialog.Builder(this).setTitle("Detección sin resultado")
                                    .setMessage(detF + Cliente.instancia().consejoSiMudo()
                                            + "\n\n¿Forzar V3 2020 (sin e) y enviar igualmente los 12 códigos y 9? "
                                            + "Nunca se envía e.")
                                    .setPositiveButton("Forzar V3 2020", (d, w) -> fase1(p, true))
                                    .setNegativeButton("Cancelar", null).show();
                        });
                        return;
                    }
                }
                r = det + "T-B03 (" + p.nombre + "):\n" + LineaBase.codigos(this, "T-B03", p, this::progresoLb)
                        + "\nT-B09:\n" + LineaBase.bateria(this, this::progresoLb);
            } catch (IOException | InterruptedException | RuntimeException e) {
                r = "Interrumpida: " + EnlaceSerie.descripcion(e);
                ok = false;
            }
            final String res = r;
            final boolean seguir = ok;
            enUi(() -> {
                txtLineaBase.setText(res);
                if (!seguir) {
                    finLineaBase(res);
                    return;
                }
                new AlertDialog.Builder(this).setTitle("Línea base: T-B07 (oscuro)")
                        .setMessage("Tape ahora el cabezal óptico (oscuro total) y no lo mueva. "
                                + "Se enviarán otra vez los 12 códigos.")
                        .setCancelable(false)
                        .setPositiveButton("Continuar", (d, w) -> fase2(res))
                        .setNegativeButton("Omitir T-B07", (d, w) -> {
                            Registro.nota(LineaBase.ETIQUETA + " T-B07 omitida por el operador");
                            finLineaBase(res + "\nT-B07 omitida.");
                        }).show();
            });
        });
    }

    private void fase2(String previo) {
        Cliente.instancia().ejecutar(() -> {
            String r;
            try {
                r = previo + "\nT-B07 (oscuro):\n" + LineaBase.codigos(this, "T-B07", null, this::progresoLb);
            } catch (IOException | InterruptedException | RuntimeException e) {
                r = previo + "\nT-B07 interrumpida: " + EnlaceSerie.descripcion(e);
            }
            final String res = r;
            enUi(() -> finLineaBase(res));
        });
    }

    private void progresoLb(String t) {
        enUi(() -> btnLineaBase.setText("Línea base: " + t));
    }

    private void finLineaBase(String res) {
        lineaBaseEnCurso = false;
        pantallaEncendida(false);
        btnLineaBase.setText("Línea base");
        txtLineaBase.setText(res + Cliente.instancia().consejoSiMudo() + "\n\nGuardado en " + (Sesion.get().csvLineaBase() == null ? "(sin fichero)"
                : Sesion.get().csvLineaBase().getName()) + ". Use Compartir para sacarlo del teléfono.");
        Registro.nota("=== " + LineaBase.ETIQUETA + " terminada ===");
        cambio();
    }

    private void iniciar() {
        if (!EnlaceSerie.instancia().estaConectado()) {
            alerta("Sin conexión", "Conecte primero con el equipo.");
            return;
        }
        try {
            double t = Double.parseDouble(edTolerancia.getText().toString().trim());
            if (t < 1 || t > 200) {
                throw new NumberFormatException();
            }
            Pruebas.get().tolerancia = t;
        } catch (NumberFormatException e) {
            alerta("Tolerancia", "Escriba un número de cuentas entre 1 y 200.");
            return;
        }
        Pruebas.get().iniciar();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Pruebas.get().agregar(this);
        cambio();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Pruebas.get().quitar(this);
    }

    @Override
    public void cambio() {
        Pruebas pr = Pruebas.get();
        boolean curso = pr.enCurso();
        pantallaEncendida(curso || lineaBaseEnCurso);
        btnIniciar.setEnabled(!curso && !lineaBaseEnCurso && EnlaceSerie.instancia().estaConectado());
        if (btnLineaBase != null) {
            btnLineaBase.setEnabled(!curso && !lineaBaseEnCurso && EnlaceSerie.instancia().estaConectado());
            btnLineaBaseForzada.setEnabled(!curso && !lineaBaseEnCurso && EnlaceSerie.instancia().estaConectado());
        }
        btnCancelar.setEnabled(curso);
        edTolerancia.setEnabled(!curso);
        Sesion s = Sesion.get();
        if (curso) {
            txtResumen.setText("Pruebas en curso... no mueva el equipo.");
            txtResumen.setBackgroundColor(AMARILLO);
        } else if (s.apto == null) {
            txtResumen.setText("Pruebas sin hacer.");
            txtResumen.setBackgroundColor(GRIS);
        } else {
            txtResumen.setText(s.resumenPruebas);
            txtResumen.setBackgroundColor(s.apto ? VERDE : ROJO);
        }
        for (int i = 0; i < filas.size(); i++) {
            Pruebas.Prueba p = pr.lista.get(i);
            TextView t = filas.get(i);
            String estado;
            int color;
            switch (p.estado) {
                case OK: estado = "OK"; color = VERDE; break;
                case FALLO: estado = "FALLO"; color = ROJO; break;
                case EN_CURSO: estado = "en curso"; color = AMARILLO; break;
                case NO_APLICA: estado = "no aplica"; color = GRIS; break;
                case INVALIDA: estado = "INVÁLIDA: repetir (no es fallo del equipo)"; color = AMARILLO; break;
                case INFO: estado = "informativa"; color = AZUL; break;
                default: estado = "pendiente"; color = GRIS; break;
            }
            t.setText(p.numero + ". " + p.titulo + " - " + estado
                    + (p.detalle.isEmpty() ? "" : "\n" + p.detalle));
            t.setBackgroundColor(color);
            t.setTypeface(Typeface.MONOSPACE);
        }
        refrescar();
    }
}
