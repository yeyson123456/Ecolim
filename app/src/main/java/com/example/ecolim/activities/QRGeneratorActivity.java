package com.example.ecolim.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ecolim.R;
import com.example.ecolim.database.EcolimDbHelper;
import com.example.ecolim.models.Residuo;
import com.google.android.material.button.MaterialButton;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * QRGeneratorActivity — Genera y muestra los códigos QR de todos los
 * tipos de residuos registrados en la base de datos.
 *
 * Cada QR contiene: "ECOLIM:Nombre del Residuo"
 * El operario puede compartir o imprimir cada QR individualmente,
 * o imprimir todos de una vez.
 *
 * UBICACIÓN: app/src/main/java/com/example/ecolim/activities/QRGeneratorActivity.java
 *
 * Registrar en AndroidManifest.xml:
 *   <activity android:name=".activities.QRGeneratorActivity"
 *             android:exported="false"
 *             android:screenOrientation="portrait" />
 */
public class QRGeneratorActivity extends BaseActivity {

    private RecyclerView   recycler;
    private MaterialButton btnImprimirTodos;
    private EcolimDbHelper db;
    private List<Residuo>  listaResiduos;
    private QRAdapter      adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_generator);

        db = EcolimDbHelper.getInstance(this);

        // Toolbar — botón atrás
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        recycler         = findViewById(R.id.recycler_qr);
        btnImprimirTodos = findViewById(R.id.btn_imprimir_todos);

        listaResiduos = db.obtenerTodosResiduos();

        // Grid de 2 columnas
        recycler.setLayoutManager(new GridLayoutManager(this, 2));
        adapter = new QRAdapter(listaResiduos);
        recycler.setAdapter(adapter);

        btnImprimirTodos.setOnClickListener(v -> imprimirTodos());
    }

    // ── Generar bitmap QR con etiqueta ────────────────────────────────────────
    public static Bitmap generarQR(String texto, String etiqueta, int colorCategoria) {
        try {
            int qrSize = 400;

            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
            hints.put(EncodeHintType.MARGIN, 2);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");

            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix bitMatrix = writer.encode(texto, BarcodeFormat.QR_CODE, qrSize, qrSize, hints);

            // Crear bitmap del QR
            Bitmap qrBitmap = Bitmap.createBitmap(qrSize, qrSize, Bitmap.Config.ARGB_8888);
            for (int x = 0; x < qrSize; x++) {
                for (int y = 0; y < qrSize; y++) {
                    qrBitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }

            // Crear bitmap final con etiqueta debajo
            int totalHeight = qrSize + 120;
            Bitmap finalBitmap = Bitmap.createBitmap(qrSize, totalHeight, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(finalBitmap);
            canvas.drawColor(Color.WHITE);

            // Barra de color superior (indica categoría)
            Paint barPaint = new Paint();
            barPaint.setColor(colorCategoria);
            canvas.drawRect(0, 0, qrSize, 16, barPaint);

            // QR
            canvas.drawBitmap(qrBitmap, 0, 16, null);

            // Etiqueta (nombre del residuo)
            Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            textPaint.setColor(Color.BLACK);
            textPaint.setTextSize(28f);
            textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            textPaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(etiqueta.length() > 20
                    ? etiqueta.substring(0, 18) + "…"
                    : etiqueta, qrSize / 2f, qrSize + 55, textPaint);

            // Texto del código QR (pequeño)
            Paint codePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            codePaint.setColor(Color.GRAY);
            codePaint.setTextSize(18f);
            codePaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("ECOLIM", qrSize / 2f, qrSize + 90, codePaint);

            return finalBitmap;

        } catch (WriterException e) {
            return null;
        }
    }

    // ── Compartir un QR individual ────────────────────────────────────────────
    private void compartirQR(Residuo residuo) {
        try {
            String texto    = "ECOLIM:" + residuo.getNombre();
            int    color    = colorCategoria(residuo.getCategoria());
            Bitmap bitmap   = generarQR(texto, residuo.getNombre(), color);

            if (bitmap == null) { toast("Error generando QR"); return; }

            File dir = new File(getCacheDir(), "qr");
            if (!dir.exists()) dir.mkdirs();
            File archivo = new File(dir, "QR_" + residuo.getNombre()
                    .replaceAll("[^a-zA-Z0-9]", "_") + ".png");

            try (FileOutputStream fos = new FileOutputStream(archivo)) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            }

            Uri uri = FileProvider.getUriForFile(this,
                    getPackageName() + ".provider", archivo);

            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("image/png");
            share.putExtra(Intent.EXTRA_STREAM, uri);
            share.putExtra(Intent.EXTRA_SUBJECT, "QR Residuo: " + residuo.getNombre());
            share.putExtra(Intent.EXTRA_TEXT,
                    "Código QR para: " + residuo.getNombre()
                            + "\nEscanea con la app ECOLIM para registrar automáticamente.");
            share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(share, "Compartir o imprimir QR"));

        } catch (Exception e) {
            toast("Error: " + e.getMessage());
        }
    }

    // ── Imprimir todos los QR en un solo archivo ──────────────────────────────
    private void imprimirTodos() {
        try {
            int cols      = 2;
            int qrSize    = 420; // con etiqueta
            int padding   = 20;
            int rows      = (int) Math.ceil(listaResiduos.size() / (double) cols);
            int totalW    = cols * qrSize + (cols + 1) * padding;
            int totalH    = rows * qrSize + (rows + 1) * padding + 80; // +80 para título

            Bitmap sheet  = Bitmap.createBitmap(totalW, totalH, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(sheet);
            canvas.drawColor(Color.WHITE);

            // Título
            Paint titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            titlePaint.setColor(Color.parseColor("#1B5E20"));
            titlePaint.setTextSize(36f);
            titlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            titlePaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("ECOLIM S.A.C. — Códigos QR de Residuos",
                    totalW / 2f, 55, titlePaint);

            // QR de cada residuo
            for (int i = 0; i < listaResiduos.size(); i++) {
                Residuo r   = listaResiduos.get(i);
                String txt  = "ECOLIM:" + r.getNombre();
                int    color = colorCategoria(r.getCategoria());
                Bitmap qr   = generarQR(txt, r.getNombre(), color);
                if (qr == null) continue;

                int col = i % cols;
                int row = i / cols;
                int x   = padding + col * (qrSize + padding);
                int y   = 80 + padding + row * (qrSize + padding);
                canvas.drawBitmap(qr, x, y, null);
            }

            // Guardar en Documentos
            File dir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
            if (dir != null && !dir.exists()) dir.mkdirs();
            File archivo = new File(dir, "ECOLIM_QR_Todos.png");
            try (FileOutputStream fos = new FileOutputStream(archivo)) {
                sheet.compress(Bitmap.CompressFormat.PNG, 100, fos);
            }

            Uri uri = FileProvider.getUriForFile(this,
                    getPackageName() + ".provider", archivo);

            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("image/png");
            share.putExtra(Intent.EXTRA_STREAM, uri);
            share.putExtra(Intent.EXTRA_SUBJECT, "ECOLIM — Todos los códigos QR");
            share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(share, "Compartir o imprimir todos los QR"));

            toast("Archivo generado: " + archivo.getName());

        } catch (Exception e) {
            toast("Error al generar: " + e.getMessage());
        }
    }

    private int colorCategoria(String cat) {
        if (cat == null) return Color.parseColor("#9E9E9E");
        switch (cat) {
            case "Peligroso": return Color.parseColor("#F44336");
            case "Especial":  return Color.parseColor("#9C27B0");
            default:          return Color.parseColor("#4CAF50");
        }
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish(); return true;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  ADAPTER — grid de QR cards
    // ══════════════════════════════════════════════════════════════════════════
    class QRAdapter extends RecyclerView.Adapter<QRAdapter.VH> {

        private final List<Residuo> lista;

        QRAdapter(List<Residuo> lista) { this.lista = lista; }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_qr, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            Residuo r    = lista.get(pos);
            String  txt  = "ECOLIM:" + r.getNombre();
            int     color = colorCategoria(r.getCategoria());
            Bitmap  qr   = generarQR(txt, r.getNombre(), color);

            h.imgQR.setImageBitmap(qr);
            h.txtNombre.setText(r.getNombre());
            h.txtCategoria.setText(r.getCategoria());
            h.txtCategoria.setTextColor(color);
            h.txtCodigo.setText(txt);

            // Barra de color por categoría
            h.colorBar.setBackgroundColor(color);

            // Botón compartir
            h.btnCompartir.setOnClickListener(v -> compartirQR(r));
        }

        @Override public int getItemCount() { return lista.size(); }

        class VH extends RecyclerView.ViewHolder {
            ImageView      imgQR;
            TextView       txtNombre, txtCategoria, txtCodigo;
            View           colorBar;
            MaterialButton btnCompartir;

            VH(View v) {
                super(v);
                imgQR        = v.findViewById(R.id.img_qr);
                txtNombre    = v.findViewById(R.id.txt_qr_nombre);
                txtCategoria = v.findViewById(R.id.txt_qr_categoria);
                txtCodigo    = v.findViewById(R.id.txt_qr_codigo);
                colorBar     = v.findViewById(R.id.view_qr_color_bar);
                btnCompartir = v.findViewById(R.id.btn_compartir_qr);
            }
        }
    }
}