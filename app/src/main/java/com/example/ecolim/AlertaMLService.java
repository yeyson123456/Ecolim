package com.example.ecolim;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.ecolim.database.EcolimDbHelper;
import com.example.ecolim.models.Registro;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * AlertaMLService — Motor de detección inteligente de residuos.
 *
 * Carga el árbol de decisión desde assets/ecolim_model.json
 * y evalúa cada registro nuevo para decidir si enviar alerta WhatsApp.
 *
 * UBICACIÓN: app/src/main/java/com/example/ecolim/AlertaMLService.java
 *
 * CÓMO USAR desde RegistroFragment después de guardar un registro:
 *   AlertaMLService.get(requireContext()).analizar(registro);
 */
public class AlertaMLService {

    private static final String TAG       = "AlertaML";
    private static final String PREFS     = "ecolim_prefs";
    private static final String MODEL_FILE = "ecolim_model.json";

    // Umbrales configurables
    private static final double UMBRAL_CANTIDAD_KG    = 80.0;  // kg por registro
    private static final double UMBRAL_ACUMULADO_KG   = 150.0; // kg acumulados hoy
    private static final int    COOLDOWN_MINUTOS       = 30;    // evita spam de alertas

    private static AlertaMLService instancia;
    private final Context           ctx;
    private       JSONObject        modelo;

    // ── Singleton ────────────────────────────────────────────────────────────
    public static AlertaMLService get(Context ctx) {
        if (instancia == null)
            instancia = new AlertaMLService(ctx.getApplicationContext());
        return instancia;
    }

    private AlertaMLService(Context ctx) {
        this.ctx = ctx;
        cargarModelo();
    }

    // ── Cargar modelo desde assets/ ──────────────────────────────────────────
    private void cargarModelo() {
        try {
            InputStream is   = ctx.getAssets().open(MODEL_FILE);
            byte[] buffer    = new byte[is.available()];
            is.read(buffer);
            is.close();
            modelo = new JSONObject(new String(buffer, StandardCharsets.UTF_8));
            Log.i(TAG, "✔ Modelo cargado correctamente");
        } catch (Exception e) {
            Log.e(TAG, "Error cargando modelo: " + e.getMessage());
            modelo = null;
        }
    }

    // ── Método principal: analizar un registro recién guardado ───────────────
    public void analizar(Registro r) {
        if (r == null) return;

        // Calcular acumulado del día
        double acumuladoHoy = calcularAcumuladoHoy(r.getFecha(), r.getUsuarioId());

        // Convertir unidad a kg
        double cantidadKg = convertirAKg(r.getCantidad(), r.getUnidad());

        // Clasificar: 0 = normal, 1 = alerta
        boolean esPeligroso = "Peligroso".equalsIgnoreCase(r.getCategoriaResiduo());
        int resultado = clasificar(cantidadKg, esPeligroso ? 1.0 : 0.0, acumuladoHoy);

        Log.i(TAG, String.format(
                "Análisis: cantidad=%.1fkg, peligroso=%b, acumulado=%.1fkg → %s",
                cantidadKg, esPeligroso, acumuladoHoy,
                resultado == 1 ? "ALERTA" : "normal"));

        if (resultado == 1) {
            enviarAlertaSiProcede(r, cantidadKg, acumuladoHoy, esPeligroso);
        }
    }

    // ── Árbol de decisión (recorre el JSON del modelo) ───────────────────────
    private int clasificar(double cantidadKg, double esPeligroso, double acumuladoHoy) {
        if (modelo == null) {
            // Fallback: reglas simples si el modelo no cargó
            return (esPeligroso > 0 || cantidadKg > UMBRAL_CANTIDAD_KG
                    || acumuladoHoy > UMBRAL_ACUMULADO_KG) ? 1 : 0;
        }

        try {
            JSONArray childrenLeft  = modelo.getJSONArray("children_left");
            JSONArray childrenRight = modelo.getJSONArray("children_right");
            JSONArray feature       = modelo.getJSONArray("feature");
            JSONArray threshold     = modelo.getJSONArray("threshold");
            JSONArray value         = modelo.getJSONArray("value");

            double[] inputs = {cantidadKg, esPeligroso, acumuladoHoy};
            int nodo = 0; // raíz

            while (true) {
                int feat = feature.getInt(nodo);
                if (feat == -2) break; // hoja

                double umbral = threshold.getDouble(nodo);
                nodo = (inputs[feat] <= umbral)
                        ? childrenLeft.getInt(nodo)
                        : childrenRight.getInt(nodo);
            }

            // Leer clase de la hoja
            JSONArray clases = value.getJSONArray(nodo).getJSONArray(0);
            return clases.getDouble(0) >= clases.getDouble(1) ? 0 : 1;

        } catch (Exception e) {
            Log.e(TAG, "Error en clasificación: " + e.getMessage());
            return 0;
        }
    }

    // ── Enviar alerta con cooldown (evita mensajes repetidos) ────────────────
    private void enviarAlertaSiProcede(Registro r, double cantidadKg,
                                       double acumuladoHoy, boolean esPeligroso) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        long ultimaAlerta = prefs.getLong("ultima_alerta_whatsapp", 0);
        long ahora        = System.currentTimeMillis();
        long cooldown     = COOLDOWN_MINUTOS * 60 * 1000L;

        if (ahora - ultimaAlerta < cooldown) {
            Log.i(TAG, "Alerta en cooldown, faltan "
                    + ((cooldown - (ahora - ultimaAlerta)) / 60000) + " min");
            return;
        }

        // Construir mensaje
        String mensaje = construirMensaje(r, cantidadKg, acumuladoHoy, esPeligroso);

        // Enviar en hilo secundario (no bloquear UI)
        new Thread(() -> {
            boolean ok = WhatsAppAlertSender.enviar(ctx, mensaje);
            if (ok) {
                prefs.edit().putLong("ultima_alerta_whatsapp", System.currentTimeMillis()).apply();
                Log.i(TAG, "✔ Alerta WhatsApp enviada");
            }
        }).start();
    }

    // ── Construir el texto del mensaje ───────────────────────────────────────
    private String construirMensaje(Registro r, double cantidadKg,
                                    double acumuladoHoy, boolean esPeligroso) {
        StringBuilder sb = new StringBuilder();
        sb.append("🚨 *ALERTA ECOLIM* 🚨\n\n");

        if (esPeligroso) {
            sb.append("⚠️ *Residuo PELIGROSO detectado*\n");
            sb.append("Tipo: ").append(r.getTipoResiduo()).append("\n");
        } else {
            sb.append("📦 *Exceso de residuos detectado*\n");
        }

        sb.append("Cantidad: ").append(String.format("%.1f", cantidadKg)).append(" kg\n");
        sb.append("Acumulado hoy: ").append(String.format("%.1f", acumuladoHoy)).append(" kg\n");
        sb.append("Zona: ").append(r.getUbicacion()).append("\n");
        sb.append("Fecha: ").append(r.getFecha()).append(" ").append(r.getHora()).append("\n");
        sb.append("Operario: ").append(r.getNombreUsuario()).append("\n\n");
        sb.append("_Sistema ECOLIM S.A.C._");

        return sb.toString();
    }

    // ── Calcular kg acumulados en el día ─────────────────────────────────────
    private double calcularAcumuladoHoy(String fecha, long usuarioId) {
        try {
            EcolimDbHelper db = EcolimDbHelper.getInstance(ctx);
            List<Registro> registros = db.obtenerRegistrosPorFecha(fecha);
            double total = 0;
            for (Registro reg : registros) {
                total += convertirAKg(reg.getCantidad(), reg.getUnidad());
            }
            return total;
        } catch (Exception e) {
            Log.e(TAG, "Error calculando acumulado: " + e.getMessage());
            return 0;
        }
    }

    // ── Convertir cualquier unidad a kg ──────────────────────────────────────
    private double convertirAKg(double cantidad, String unidad) {
        if (unidad == null) return cantidad;
        String u = unidad.toLowerCase();
        if (u.contains("tonelada") || u.equals("t")) return cantidad * 1000;
        if (u.contains("litro")   || u.equals("l")) return cantidad * 0.8; // densidad aprox
        return cantidad; // ya está en kg o unidades
    }
}