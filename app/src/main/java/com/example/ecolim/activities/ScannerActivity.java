package com.example.ecolim.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;

import com.example.ecolim.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.zxing.ResultPoint;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

import java.util.List;

public class ScannerActivity extends BaseActivity {

    public static final String EXTRA_RESULTADO = "qr_resultado";
    public static final String EXTRA_TIPO      = "qr_tipo_residuo";

    private DecoratedBarcodeView scannerView;
    private MaterialButton       btnCancelar, btnLinterna;
    private MaterialToolbar      toolbar;
    private boolean              yaEscaneado    = false;
    private boolean              linternaActiva = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner);

        scannerView = findViewById(R.id.scanner_view);
        btnCancelar = findViewById(R.id.btn_cancelar_scan);
        btnLinterna = findViewById(R.id.btn_linterna);
        toolbar     = findViewById(R.id.toolbar_scanner);

        toolbar.setNavigationOnClickListener(v -> cancelar());
        btnCancelar.setOnClickListener(v -> cancelar());
        btnLinterna.setOnClickListener(v -> toggleLinterna());

        scannerView.setStatusText("Apunta al código QR del residuo");
        scannerView.decodeContinuous(callback);

        // ✅ Sin deprecated — forma correcta Android 13+
        getOnBackPressedDispatcher().addCallback(this,
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        cancelar();
                    }
                });
    }

    private final BarcodeCallback callback = new BarcodeCallback() {
        @Override
        public void barcodeResult(BarcodeResult result) {
            if (result.getText() == null || yaEscaneado) return;
            yaEscaneado = true;

            String texto       = result.getText().trim();
            String tipoResiduo = parsearQR(texto);

            Intent data = new Intent();
            data.putExtra(EXTRA_RESULTADO, texto);
            data.putExtra(EXTRA_TIPO, tipoResiduo != null ? tipoResiduo : "");
            setResult(RESULT_OK, data);

            if (tipoResiduo == null)
                Toast.makeText(ScannerActivity.this,
                        "QR leído: " + texto, Toast.LENGTH_SHORT).show();
            finish();
        }

        @Override
        public void possibleResultPoints(List<ResultPoint> resultPoints) {}
    };

    private String parsearQR(String texto) {
        if (texto == null || texto.isEmpty()) return null;
        if (texto.startsWith("ECOLIM:")) {
            String tipo = texto.substring(7).trim();
            return tipo.isEmpty() ? null : tipo;
        }
        String[] tiposValidos = {
                "Residuos Orgánicos", "Papel y Cartón", "Plásticos",
                "Metales", "Vidrio", "Residuos Peligrosos",
                "Residuos Electrónicos (RAEE)", "Residuos Químicos",
                "Residuos Biológicos", "Residuos de Construcción", "Otros"
        };
        for (String tipo : tiposValidos)
            if (tipo.equalsIgnoreCase(texto)) return tipo;
        return null;
    }

    private void cancelar() {
        setResult(RESULT_CANCELED);
        finish();
    }

    private void toggleLinterna() {
        if (linternaActiva) {
            scannerView.setTorchOff();
            btnLinterna.setText("Linterna");
        } else {
            scannerView.setTorchOn();
            btnLinterna.setText("Apagar");
        }
        linternaActiva = !linternaActiva;
    }

    @Override protected void onResume() { super.onResume(); scannerView.resume(); }
    @Override protected void onPause()  { super.onPause();  scannerView.pause();  }
}