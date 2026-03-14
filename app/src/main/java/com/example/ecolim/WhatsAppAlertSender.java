package com.example.ecolim;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * WhatsAppAlertSender — Envía mensajes de alerta por WhatsApp vía CallMeBot.
 *
 * ══════════════════════════════════════════════════════════
 *  CONFIGURACIÓN CALLMEBOT (solo una vez):
 *  1. Agrega +34 644 49 70 88 a tus contactos WhatsApp
 *  2. Envíale este mensaje exacto:
 *       I allow callmebot to send me messages
 *  3. Te responden con tu APIKEY (ej: 1234567)
 *  4. Reemplaza APIKEY_DEFAULT abajo con esa apikey
 * ══════════════════════════════════════════════════════════
 *
 * UBICACIÓN: app/src/main/java/com/example/ecolim/WhatsAppAlertSender.java
 */
public class WhatsAppAlertSender {

    private static final String TAG   = "WhatsAppSender";
    private static final String PREFS = "ecolim_prefs";

    // ══════════════════════════════════════════════════════
    //  TU NÚMERO YA CONFIGURADO (Perú +51)
    // ══════════════════════════════════════════════════════
    private static final String NUMERO_DEFAULT = "51913630512";

    // ⚠️ REEMPLAZA ESTE VALOR con la apikey que te envíe CallMeBot
    private static final String APIKEY_DEFAULT = "XXXXXXX";

    // URL CallMeBot API
    private static final String URL_BASE =
            "https://api.callmebot.com/whatsapp.php?phone=%s&text=%s&apikey=%s";

    // ──────────────────────────────────────────────────────
    /**
     * Envía el mensaje por WhatsApp.
     * SIEMPRE llamar desde un hilo secundario (no UI thread).
     * @return true si el envío fue exitoso
     */
    public static boolean enviar(Context ctx, String mensaje) {
        try {
            SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);

            // Leer configuración (SharedPreferences tiene prioridad sobre las constantes)
            String numero = prefs.getString("whatsapp_numero", NUMERO_DEFAULT);
            String apikey = prefs.getString("whatsapp_apikey", APIKEY_DEFAULT);

            // Verificar que la apikey ya fue configurada
            if (apikey.contains("X") || apikey.isEmpty()) {
                Log.w(TAG, "⚠ ApiKey no configurada. Envía 'I allow callmebot to send me messages' "
                        + "al +34 644 49 70 88 desde WhatsApp para obtener tu apikey.");
                return false;
            }

            // Codificar mensaje para URL
            String mensajeCodificado = URLEncoder.encode(mensaje, StandardCharsets.UTF_8.name());
            String urlStr = String.format(URL_BASE, numero, mensajeCodificado, apikey);

            Log.i(TAG, "Enviando alerta a: +" + numero);

            // Petición HTTP GET
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            conn.setRequestProperty("User-Agent", "ECOLIM-Android/2.0");

            int code = conn.getResponseCode();

            // Leer respuesta
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(
                            code == 200 ? conn.getInputStream() : conn.getErrorStream()));
            StringBuilder resp = new StringBuilder();
            String linea;
            while ((linea = br.readLine()) != null) resp.append(linea);
            br.close();
            conn.disconnect();

            if (code == 200) {
                Log.i(TAG, "✔ Mensaje enviado correctamente. Respuesta: " + resp);
                return true;
            } else {
                Log.e(TAG, "✗ Error HTTP " + code + ": " + resp);
                return false;
            }

        } catch (Exception e) {
            Log.e(TAG, "✗ Excepción enviando WhatsApp: " + e.getMessage());
            return false;
        }
    }

    // ──────────────────────────────────────────────────────
    /**
     * Guarda número y apikey en SharedPreferences.
     * Se puede llamar desde ConfiguracionFragment para actualizar.
     */
    public static void guardarConfiguracion(Context ctx, String numero, String apikey) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString("whatsapp_numero", numero)
                .putString("whatsapp_apikey", apikey)
                .apply();
        Log.i(TAG, "✔ Configuración guardada: +" + numero);
    }

    // ──────────────────────────────────────────────────────
    /**
     * Envía un mensaje de prueba para verificar que todo funciona.
     * Úsalo desde Configuración → "Probar alerta WhatsApp".
     */
    public static void enviarPrueba(Context ctx) {
        new Thread(() -> {
            boolean ok = enviar(ctx,
                    "✅ *ECOLIM S.A.C.* — Sistema de alertas activo.\n"
                            + "El modelo ML está funcionando correctamente.\n"
                            + "_Prueba enviada desde la app._");
            Log.i(TAG, ok ? "✔ Prueba exitosa" : "✗ Prueba fallida — revisa la apikey");
        }).start();
    }

    // ──────────────────────────────────────────────────────
    /**
     * Devuelve true si la apikey ya está configurada.
     */
    public static boolean estaConfigurado(Context ctx) {
        String apikey = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString("whatsapp_apikey", APIKEY_DEFAULT);
        return !apikey.contains("X") && !apikey.isEmpty();
    }
}