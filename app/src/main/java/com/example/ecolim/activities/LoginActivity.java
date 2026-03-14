package com.example.ecolim.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ecolim.R;
import com.example.ecolim.database.EcolimDbHelper;
import com.example.ecolim.models.Usuario;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class LoginActivity extends BaseActivity  {


    private static final String PREFS_NAME  = "ecolim_prefs";
    private static final String KEY_USER_ID = "usuario_id";

    private TextInputLayout   layoutEmail, layoutPassword;
    private TextInputEditText edtEmail, edtPassword;
    private MaterialButton    btnIngresar;
    private CheckBox          chkRecordar;
    private EcolimDbHelper    db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        db             = EcolimDbHelper.getInstance(this);
        layoutEmail    = findViewById(R.id.layout_email);
        layoutPassword = findViewById(R.id.layout_password);
        edtEmail       = findViewById(R.id.edt_email);
        edtPassword    = findViewById(R.id.edt_password);
        btnIngresar    = findViewById(R.id.btn_ingresar);
        chkRecordar    = findViewById(R.id.chk_recordar);

        btnIngresar.setOnClickListener(v -> intentarLogin());
    }

    
    //  Lógica de login
    private void intentarLogin() {
        String email    = txt(edtEmail);
        String password = txt(edtPassword);

        // Limpiar errores previos
        layoutEmail.setError(null);
        layoutPassword.setError(null);

        // Validaciones
        if (email.isEmpty()) {
            layoutEmail.setError("Ingrese su correo");
            edtEmail.requestFocus();
            return;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            layoutEmail.setError("Correo no válido");
            edtEmail.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            layoutPassword.setError("Ingrese su contraseña");
            edtPassword.requestFocus();
            return;
        }
        if (password.length() < 6) {
            layoutPassword.setError("Mínimo 6 caracteres");
            edtPassword.requestFocus();
            return;
        }

        // Deshabilitar botón mientras verifica
        btnIngresar.setEnabled(false);
        btnIngresar.setText("Verificando...");

        // Consultar base de datos
        Usuario usuario = db.loginUsuario(email, password);

        if (usuario != null) {
            // Guardar sesión si marcó "Recordar"
            if (chkRecordar.isChecked()) {
                getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                        .edit()
                        .putLong(KEY_USER_ID, usuario.getId())
                        .apply();
            }

            Toast.makeText(this,
                    "¡Bienvenido, " + usuario.getNombre() + "!",
                    Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("usuario_id", usuario.getId());
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);

        } else {
            layoutPassword.setError("Correo o contraseña incorrectos");
            btnIngresar.setEnabled(true);
            btnIngresar.setText("Ingresar");
        }
    }
    private String txt(TextInputEditText e) {
        return e.getText() == null ? "" : e.getText().toString().trim();
    }
}