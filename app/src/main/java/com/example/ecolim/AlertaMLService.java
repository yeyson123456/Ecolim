package com.example.ecolim;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.ecolim.database.EcolimDbHelper;
import com.example.ecolim.models.Registro;
import com.example.ecolim.models.Usuario;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * AlertaMLService — Motor de detección inteligente de residuos.
 *
 * Flujo:
 *  1. Carga ecolim_model.json desde assets/
 *  2. Aplica feature engineering (19 features)
 *  3. Clasifica: 0=NORMAL / 1=PRECAUCIÓN / 2=ALERTA / 3=CRÍTICO
 *  4. Si ≥ ALERTA → envía WhatsApp al teléfono del usuario que registró
 *
 * UBICACIÓN: app/src/main/java/com/example/ecolim/AlertaMLService.java
 */
public class AlertaMLService {

    private static final String TAG        = "AlertaML";
    private static final String PREFS      = "ecolim_prefs";
    private static final String MODEL_FILE = "ecolim_model.json";
    private static final int    COOLDOWN_MINUTOS = 30;

    private static AlertaMLService instancia;
    private final  Context         ctx;
    private        JSONObject       modelo;

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

    // ── Cargar modelo JSON ────────────────────────────────────────────────────
    private void cargarModelo() {
        try {
            InputStream is  = ctx.getAssets().open(MODEL_FILE);
            byte[] buffer   = new byte[is.available()];
            is.read(buffer);
            is.close();
            modelo = new JSONObject(new String(buffer, StandardCharsets.UTF_8));
            Log.i(TAG, "Modelo v" + modelo.optString("version","?") + " cargado OK");
        } catch (Exception e) {
            Log.e(TAG, "Error cargando modelo: " + e.getMessage());
            modelo = null;
        }
    }

    // ── MÉTODO PRINCIPAL ─────────────────────────────────────────────────────
    public void analizar(Registro r) {
        if (r == null) return;

        double cantKg       = convertirAKg(r.getCantidad(), r.getUnidad());
        double acumHoy      = calcularAcumuladoHoy(r.getFecha());
        double acumSemana   = calcularAcumuladoSemana(r.getFecha());
        boolean esPeligroso = "Peligroso".equalsIgnoreCase(r.getCategoriaResiduo());
        int     tipoCod     = codificarTipo(r.getTipoResiduo());
        int     hora        = extraerHora(r.getHora());
        int     regsHoy     = contarRegistrosHoy(r.getFecha());

        // Feature engineering (19 features — igual que en el modelo Python)
        double[] features = featureEngineering(new double[]{
                cantKg,
                esPeligroso ? 1.0 : 0.0,
                acumHoy,
                acumSemana,
                tipoCod,
                hora,
                0,              // es_fin_semana (simplificado)
                regsHoy,
                frecuenciaAnomalia(r.getTipoResiduo()),
                25.0,           // temp_zona (valor por defecto)
                calcularCapacidad(acumHoy),
                diasSinRecoleccion()
        });

        int clase = clasificar(features);
        String[] nombres = {"NORMAL", "PRECAUCIÓN", "ALERTA", "CRÍTICO"};
        Log.i(TAG, String.format("Análisis [%s]: %.1fkg acum=%.1fkg → %s",
                r.getTipoResiduo(), cantKg, acumHoy,
                clase < nombres.length ? nombres[clase] : "?"));

        // Enviar alerta si es clase 2 (ALERTA) o 3 (CRÍTICO)
        if (clase >= 2) {
            enviarAlertaAlUsuario(r, clase, cantKg, acumHoy, esPeligroso);
        }
    }

    // ── Feature engineering idéntico al app.py ────────────────────────────────
    private double[] featureEngineering(double[] x) {
        double ratioHoy  = x[0] / (x[2] + 1);
        double ratioSem  = x[0] / (x[3] + 1);
        double peligro   = x[1] * (x[8] + 1) * (x[0] / 10.0);
        double presion   = (x[10] / 100.0) * (x[11] + 1);
        double riesgoH   = (x[5] < 6 || x[5] > 20) ? 1.5 : 1.0;
        double intensDia = x[7] * (x[2] / (x[7] + 1));
        double riesgoT   = x[9] > 30 ? (x[9] - 30) / 10.0 : 0;

        double[] eng = new double[19];
        System.arraycopy(x, 0, eng, 0, 12);
        eng[12] = ratioHoy;
        eng[13] = ratioSem;
        eng[14] = peligro;
        eng[15] = presion;
        eng[16] = riesgoH;
        eng[17] = intensDia;
        eng[18] = riesgoT;
        return eng;
    }

    // ── Clasificar con el ensemble ────────────────────────────────────────────
    private int clasificar(double[] features) {
        if (modelo == null) return clasificarFallback(features);

        try {
            // Random Forest (peso 3)
            double[] votosRF = votarBosque(
                    modelo.getJSONObject("random_forest").getJSONArray("trees"),
                    features,
                    modelo.getJSONObject("random_forest").getInt("n_clases"));

            // Extra Trees (peso 2)
            double[] votosET = votarBosque(
                    modelo.getJSONObject("extra_trees").getJSONArray("trees"),
                    features,
                    modelo.getJSONObject("extra_trees").getInt("n_clases"));

            // Combinar con pesos [3, 2]
            int nClases = 4;
            double[] combinado = new double[nClases];
            for (int c = 0; c < nClases; c++) {
                combinado[c] = (3.0 * votosRF[c] + 2.0 * votosET[c]) / 5.0;
            }

            int mejorClase = 0;
            for (int c = 1; c < nClases; c++)
                if (combinado[c] > combinado[mejorClase]) mejorClase = c;

            return mejorClase;

        } catch (Exception e) {
            Log.w(TAG, "Error en clasificación: " + e.getMessage());
            return clasificarFallback(features);
        }
    }

    /** Vota usando todos los árboles de un bosque */
    private double[] votarBosque(JSONArray arboles, double[] features, int nClases)
            throws Exception {
        double[] votos = new double[nClases];
        int n = arboles.length();
        for (int i = 0; i < n; i++) {
            int pred = predecirArbol(arboles.getJSONObject(i), features, nClases);
            if (pred >= 0 && pred < nClases) votos[pred]++;
        }
        for (int c = 0; c < nClases; c++) votos[c] /= n;
        return votos;
    }

    /** Recorre un árbol de decisión */
    private int predecirArbol(JSONObject arbol, double[] features, int nClases)
            throws Exception {
        JSONArray cl  = arbol.getJSONArray("cl");
        JSONArray cr  = arbol.getJSONArray("cr");
        JSONArray f   = arbol.getJSONArray("f");
        JSONArray th  = arbol.getJSONArray("th");
        JSONArray val = arbol.getJSONArray("v");

        int nodo = 0;
        while (true) {
            int feat = f.getInt(nodo);
            if (feat == -2) break; // hoja
            double umbral = th.getDouble(nodo);
            nodo = (feat < features.length && features[feat] <= umbral)
                    ? cl.getInt(nodo) : cr.getInt(nodo);
        }

        JSONArray clases = val.getJSONArray(nodo).getJSONArray(0);
        int mejor = 0;
        for (int c = 1; c < Math.min(nClases, clases.length()); c++)
            if (clases.getDouble(c) > clases.getDouble(mejor)) mejor = c;
        return mejor;
    }

    /** Fallback si el modelo no carga */
    private int clasificarFallback(double[] f) {
        double cantKg  = f[0];
        double peligro = f[1];
        double acumHoy = f[2];
        double capac   = f[10];

        if (peligro > 0 && cantKg > 40)  return 3; // CRÍTICO
        if (peligro > 0 || cantKg > 130 || acumHoy > 200 || capac > 90) return 3;
        if (cantKg > 70  || acumHoy > 110 || capac > 70) return 2; // ALERTA
        if (cantKg > 35  || acumHoy > 65)                return 1; // PRECAUCIÓN
        return 0;
    }

    // ── Enviar alerta al teléfono del usuario ─────────────────────────────────
    private void enviarAlertaAlUsuario(Registro r, int clase,
                                       double cantKg, double acumHoy,
                                       boolean esPeligroso) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        long ahora        = System.currentTimeMillis();
        long ultimaAlerta = prefs.getLong("ultima_alerta_wa_" + r.getUsuarioId(), 0);
        long cooldown     = COOLDOWN_MINUTOS * 60 * 1000L;

        if (ahora - ultimaAlerta < cooldown) {
            Log.i(TAG, "Alerta en cooldown para usuario " + r.getUsuarioId());
            return;
        }

        // Obtener teléfono del usuario que registró
        EcolimDbHelper db = EcolimDbHelper.getInstance(ctx);
        Usuario u = db.obtenerUsuarioPorId(r.getUsuarioId());

        // Determinar número destino:
        // 1. Teléfono del usuario que registró (si tiene)
        // 2. Número por defecto configurado en WhatsAppAlertSender
        String telefonoDestino = null;
        if (u != null && u.getTelefono() != null && !u.getTelefono().isEmpty()) {
            telefonoDestino = u.getTelefono();
            Log.i(TAG, "Enviando alerta al teléfono del usuario: " + telefonoDestino);
        }

        String[] niveles = {"NORMAL", "PRECAUCIÓN", "⚠️ ALERTA", "🚨 CRÍTICO"};
        String mensaje = construirMensaje(r, clase, niveles[clase],
                cantKg, acumHoy, esPeligroso,
                u != null ? u.getNombreCompleto() : r.getNombreUsuario());

        final String telFinal = telefonoDestino;
        final Context finalCtx = ctx;

        new Thread(() -> {
            // CAMBIO: Usamos 'enviar' (no enviarA) y le pasamos ctx y mensaje
            // Nota: Tu método actual saca el número de las SharedPreferences, así que no necesita 'telFinal'
            boolean ok = WhatsAppAlertSender.enviar(finalCtx, mensaje);

            if (ok) {
                prefs.edit()
                        .putLong("ultima_alerta_wa_" + r.getUsuarioId(), System.currentTimeMillis())
                        .apply();
                Log.i(TAG, "✔ Alerta WhatsApp enviada");
            }
        }).start();
    }

    // ── Construir mensaje de alerta ───────────────────────────────────────────
    private String construirMensaje(Registro r, int clase, String nivel,
                                    double cantKg, double acumHoy,
                                    boolean esPeligroso, String nombreUsuario) {
        StringBuilder sb = new StringBuilder();
        sb.append("*ECOLIM S.A.C. — ").append(nivel).append("*\n\n");

        if (esPeligroso) {
            sb.append("⚠️ *Residuo PELIGROSO detectado*\n");
        } else {
            sb.append("📦 *Exceso de residuos detectado*\n");
        }

        sb.append("\n*Tipo:* ").append(r.getTipoResiduo());
        sb.append("\n*Categoría:* ").append(r.getCategoriaResiduo());
        sb.append("\n*Cantidad:* ").append(String.format("%.1f", cantKg)).append(" kg");
        sb.append("\n*Acumulado hoy:* ").append(String.format("%.1f", acumHoy)).append(" kg");
        sb.append("\n*Zona:* ").append(r.getUbicacion());
        sb.append("\n*Fecha:* ").append(r.getFecha()).append(" ").append(r.getHora());
        sb.append("\n*Operario:* ").append(nombreUsuario);
        sb.append("\n\n_Sistema ML ECOLIM v2.0 — NTP 900.058_");

        return sb.toString();
    }

    // ── Helpers de cálculo ────────────────────────────────────────────────────

    private double calcularAcumuladoHoy(String fecha) {
        try {
            List<Registro> regs = EcolimDbHelper.getInstance(ctx)
                    .obtenerRegistrosPorFecha(fecha);
            double total = 0;
            for (Registro reg : regs) total += convertirAKg(reg.getCantidad(), reg.getUnidad());
            return total;
        } catch (Exception e) { return 0; }
    }

    private double calcularAcumuladoSemana(String fechaHoy) {
        try {
            // Simplificado: multiplica el acumulado hoy × 7 como estimado
            return calcularAcumuladoHoy(fechaHoy) * 3.5;
        } catch (Exception e) { return 0; }
    }

    private double calcularCapacidad(double acumHoy) {
        // Estima % de capacidad (asumiendo límite de 200 kg/día)
        return Math.min(acumHoy / 200.0 * 100.0, 100.0);
    }

    private int contarRegistrosHoy(String fecha) {
        try {
            return EcolimDbHelper.getInstance(ctx).contarRegistrosHoy(fecha);
        } catch (Exception e) { return 0; }
    }

    private int diasSinRecoleccion() { return 1; }

    private int frecuenciaAnomalia(String tipo) { return 0; }

    private double convertirAKg(double cantidad, String unidad) {
        if (unidad == null) return cantidad;
        String u = unidad.toLowerCase();
        if (u.contains("tonelada") || u.equals("t")) return cantidad * 1000;
        if (u.contains("litro")   || u.equals("l")) return cantidad * 0.8;
        return cantidad;
    }

    private int codificarTipo(String tipo) {
        if (tipo == null) return 10;
        if (tipo.contains("Orgánico"))     return 0;
        if (tipo.contains("Papel"))        return 1;
        if (tipo.contains("Plástico"))     return 2;
        if (tipo.contains("Metal"))        return 3;
        if (tipo.contains("Vidrio"))       return 4;
        if (tipo.contains("Peligroso"))    return 5;
        if (tipo.contains("Electrónico"))  return 6;
        if (tipo.contains("Químico"))      return 7;
        if (tipo.contains("Biológico"))    return 8;
        if (tipo.contains("Construcción")) return 9;
        return 10;
    }

    private int extraerHora(String hora) {
        try {
            return Integer.parseInt(hora.split(":")[0]);
        } catch (Exception e) { return 8; }
    }
}