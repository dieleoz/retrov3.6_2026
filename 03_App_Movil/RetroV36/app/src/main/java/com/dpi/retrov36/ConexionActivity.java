package com.dpi.retrov36;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.Set;

/**
 * Pantalla principal: dispositivos emparejados, conexion SPP y acceso al resto.
 * "Conectado" solo aparece cuando connect() ha terminado sin excepcion
 * (EnlaceSerie). Al conectar se abren solas las pruebas del equipo.
 */
public class ConexionActivity extends Base {

    private static final int PIDE_ACTIVAR_BT = 101;
    private static final int PIDE_PERMISOS = 102;

    private LinearLayout listaDispositivos;
    private TextView txtLista;
    private Button btnPruebas;
    private Button btnMedir;
    private Button btnBotones;
    private Button btnCampana;
    private Button btnAdmin;
    private Button btnCalibrar;
    private Button btnMuestras;
    private Button btnGrabar;
    private Button btnGuardar;
    private LinearLayout avanzado;
    /** true entre el toque del operador y el final de connect(). */
    private boolean esperandoConexion;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        texto("RTV " + BuildConfig.VERSION_NAME + " - retrorreflectómetro vertical V3.6 y V4.6. Conecte el equipo en la lista "
                + "de abajo; las pruebas se hacen solas. Después: tomar muestras y grabar.");
        // 3.6.17 (principio de Diego: "tomar muestras y luego darle a grabar; poco más"): dos botones grandes.
        btnMuestras = boton("Tomar muestras", v -> startActivity(new Intent(this, BancoActivity.class)));
        btnMuestras.setTextSize(26);
        btnGuardar = boton("Guardar / Compartir", v -> guardarCompartir());
        btnGuardar.setTextSize(26);
        btnGrabar = boton("Calibrar", v -> startActivity(new Intent(this, CalibrarActivity.class)));
        Button av = boton("Avanzado ▸", null);
        avanzado = new LinearLayout(this);
        avanzado.setOrientation(LinearLayout.VERTICAL);
        avanzado.setVisibility(android.view.View.GONE);
        av.setOnClickListener(v -> {
            boolean ver = avanzado.getVisibility() != android.view.View.VISIBLE;
            avanzado.setVisibility(ver ? android.view.View.VISIBLE : android.view.View.GONE);
            ((Button) v).setText(ver ? "Avanzado ▾" : "Avanzado ▸");
        });
        int desde = raiz.getChildCount();
        btnPruebas = boton("1. Pruebas del equipo", v -> startActivity(new Intent(this, PruebasActivity.class)));
        btnMedir = boton("2. Medida de patrones", v -> startActivity(new Intent(this, MedidaActivity.class)));
        btnCampana = boton("3. Campaña de calibración (guiada, un solo envío)",
                v -> startActivity(new Intent(this, CampanaActivity.class)));
        btnCalibrar = boton("4. Calibrar este equipo", v -> startActivity(new Intent(this, CalibrarActivity.class)));
        btnBotones = boton("Botones de pantalla (sólo V3.6)", v -> startActivity(new Intent(this, BotonesActivity.class)));
        btnAdmin = boton("Modo administrador (sólo V3.6)", v -> startActivity(new Intent(this, AdminActivity.class)));
        boton("Compartir registro y datos", v -> compartirTodo());
        // Lo de siempre, fuera del camino normal: en "Avanzado".
        while (raiz.getChildCount() > desde) {
            android.view.View h = raiz.getChildAt(desde);
            raiz.removeViewAt(desde);
            avanzado.addView(h);
        }
        raiz.addView(avanzado);
        Button act = boton("Actualizar lista", v -> cargarEmparejados());
        Button des = boton("Desconectar", v -> {
            if (Pruebas.get().enCurso()) {
                Pruebas.get().cancelar();
            }
            EnlaceSerie.instancia().desconectar();
        });
        fila(act, des);
        titulo("Dispositivos emparejados (toque uno para conectar)");
        txtLista = texto("");
        listaDispositivos = new LinearLayout(this);
        listaDispositivos.setOrientation(LinearLayout.VERTICAL);
        raiz.addView(listaDispositivos);
        Campanas.iniciar(this);
        PerfilesApp.iniciar(this);
        pedirPermisos();
        avisoSinExportar();
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarEmparejados();
        refrescar();
    }

    @Override
    public void estado(boolean conectado, String texto) {
        if (conectado && esperandoConexion) {
            esperandoConexion = false;
            EnlaceSerie en = EnlaceSerie.instancia();
            Sesion.get().reiniciar(en.getNombre(), en.getMac());
            Cliente.instancia().reiniciarCuenta();
            Registro.nota("sesion nueva: " + Sesion.get().identidad());
            // La campana de ESTE equipo se abre al conectar, para que las pruebas queden en ella.
            if (Sesion.get().serieConocida()) {
                try {
                    Campanas.abrir(this, Sesion.get().serie(), en.getMac());
                } catch (java.io.IOException | RuntimeException e) {
                    Registro.nota("no se pudo abrir la campaña: " + e.getMessage());
                }
            }
            Intent i = new Intent(this, PruebasActivity.class);
            i.putExtra(PruebasActivity.EXTRA_AUTO, true);
            startActivity(i);
        } else if (!conectado && !EnlaceSerie.instancia().estaConectando()) {
            esperandoConexion = false;
        }
        super.estado(conectado, texto);
    }

    @Override
    protected void refrescar() {
        super.refrescar();
        if (btnPruebas == null) {
            return;
        }
        boolean con = EnlaceSerie.instancia().estaConectado();
        Sesion s = Sesion.get();
        btnPruebas.setEnabled(con);
        btnMuestras.setEnabled(con);
        btnGuardar.setEnabled(Campanas.abierta() != null || (s.mac != null && !s.mac.isEmpty() && s.serieConocida()));
        Protocolo pr = s.protocolo;
        btnGrabar.setEnabled(con && (pr == null || pr.calibra()));
        btnGrabar.setText("Calibrar" + (pr != null && !pr.calibra() ? "\n(" + pr.motivoNoCalibra() + ")" : ""));
        btnMuestras.setEnabled(con && (pr == null || pr.mideBanco()));
        btnMuestras.setText("Tomar muestras" + (pr != null && !pr.mideBanco() ? "\n(" + pr.motivoNoBanco() + ")" : ""));
        btnMedir.setEnabled(con);
        btnBotones.setEnabled(con && s.administra());
        // RF-APP-U07: con un firmware que no calibra, los botones se deshabilitan y dicen por que.
        Protocolo p = s.protocolo;
        boolean calibra = p == null || p.calibra();
        boolean banco = p == null || p.mideBanco();
        // RTV 1.0.0-rc5: el modo administrador lo gobierna administra(), no calibra(): detras viven el alta de
        // serie (#SN), el PIN y #Q, que no son calibracion. Con calibra(), un V4.6 no podia darse de alta la
        // serie y #GN# se quedaba en NONE. Lo que si es calibracion queda candado dentro de esa pantalla.
        boolean admin = p == null || p.administra();
        btnAdmin.setEnabled(con && admin);
        btnCalibrar.setEnabled(con && calibra);
        btnCampana.setEnabled(banco);
        btnCalibrar.setText("4. Calibrar este equipo" + (calibra ? "" : "\n(" + p.motivoNoCalibra() + ")"));
        btnAdmin.setText("Modo administrador" + (admin ? (calibra ? "" : "\n(alta de serie #SN; la calibración, no)")
                : "\n(" + p.nombre() + ": sin órdenes #...#)"));
        btnCampana.setText("3. Campaña de calibración (guiada, un solo envío)" + (banco ? "" : "\n(" + p.motivoNoBanco() + ")"));
        btnMedir.setText("2. Medida de patrones" + (p != null && !p.daX() ? " (R con @LEERV)" : ""));
        listaDispositivos.setEnabled(!EnlaceSerie.instancia().estaConectando());
    }

    /**
     * 3.6.17 (Diego): "Guardar / Compartir": el ZIP del banco (ligero) y el de soporte, con su copia en Download/RTV/,
     * en un solo selector. Sin conexion vale la campana abierta.
     */
    private void guardarCompartir() {
        Campana c = Campanas.abierta();
        Sesion s = Sesion.get();
        String serie = c != null ? c.serieActual() : s.serie();
        String mac = c != null ? c.mac : s.mac;
        try {
            if (c == null) {
                Campanas.abrir(this, serie, mac);
            }
            java.util.List<Campanas.Exportacion> l = new java.util.ArrayList<>();
            l.add(Campanas.exportarConHuellas(this, serie, mac));
            l.add(Campanas.exportarSoporte(this, serie, mac));
            compartirZips(l, "Banco de " + serie + " (" + mac + "): ZIP ligero y de soporte (copia en Download/RTV/)");
        } catch (java.io.IOException | RuntimeException e) {
            alerta("Guardar / Compartir", "No se pudo preparar el ZIP: " + e.getMessage());
        }
    }

    /** RF-APP-40: al arrancar, si hay series sin exportar, aviso de no desinstalar. */
    private void avisoSinExportar() {
        int n = Campanas.seriesSinExportarTodas(this);
        if (n > 0) {
            alerta("No desinstale la app", "Hay " + n + " series sin exportar. No desinstale la app: se borrarían. "
                    + "Para actualizar, instale la versión nueva encima. Exporte la campaña (queda una copia en "
                    + "Download/RTV/).");
        }
    }

    private void pedirPermisos() {
        // targetSdk 30: BLUETOOTH y BLUETOOTH_ADMIN se conceden al instalar;
        // solo la ubicacion se pide en ejecucion (mismo modelo que RetroDiagBT).
        java.util.List<String> faltan = new java.util.ArrayList<>();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            faltan.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        // Copia en Download/RTV/ con Android 7-9 (en 10+ va por MediaStore, sin permiso).
        if (android.os.Build.VERSION.SDK_INT < 29 && ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            faltan.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (!faltan.isEmpty()) {
            ActivityCompat.requestPermissions(this, faltan.toArray(new String[0]), PIDE_PERMISOS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int codigo, @NonNull String[] permisos, @NonNull int[] res) {
        super.onRequestPermissionsResult(codigo, permisos, res);
        cargarEmparejados();
    }

    private void cargarEmparejados() {
        listaDispositivos.removeAllViews();
        BluetoothAdapter bt = EnlaceSerie.adaptador();
        if (bt == null) {
            txtLista.setText("Este teléfono no tiene Bluetooth.");
            return;
        }
        if (!bt.isEnabled()) {
            txtLista.setText("El Bluetooth está apagado. Actívelo y pulse Actualizar lista.");
            try {
                startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), PIDE_ACTIVAR_BT);
            } catch (SecurityException | ActivityNotFoundException e) {
                aviso("No se pudo pedir que se active el Bluetooth: " + e.getClass().getSimpleName());
            }
            return;
        }
        try {
            Set<BluetoothDevice> emparejados = bt.getBondedDevices();
            txtLista.setText(emparejados.isEmpty()
                    ? "No hay dispositivos emparejados. Empareje el equipo desde los ajustes del teléfono."
                    : "El equipo aparece como COVIANDINA_<serie> (p. ej. COVIANDINA_SLV-002).");
            for (final BluetoothDevice d : emparejados) {
                Button b = new Button(this);
                b.setAllCaps(false);
                b.setText(EnlaceSerie.nombreDe(d) + "\n" + d.getAddress());
                b.setOnClickListener(v -> conectarCon(d));
                listaDispositivos.addView(b);
            }
        } catch (SecurityException e) {
            txtLista.setText("Falta el permiso de Bluetooth: " + e.getMessage());
        }
    }

    @Override
    protected void onActivityResult(int codigo, int resultado, Intent datos) {
        super.onActivityResult(codigo, resultado, datos);
        if (codigo == PIDE_ACTIVAR_BT) {
            cargarEmparejados();
        }
    }

    private void conectarCon(BluetoothDevice d) {
        if (Pruebas.get().enCurso()) {
            aviso("Hay pruebas en curso. Cancélelas antes de reconectar.");
            return;
        }
        if (EnlaceSerie.instancia().estaConectando()) {
            return;
        }
        esperandoConexion = true;
        EnlaceSerie.instancia().conectar(this, d);
        refrescar();
    }
}
