package com.example.ecolim.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.Toast;

import com.example.ecolim.R;
import com.example.ecolim.database.EcolimDbHelper;
import com.example.ecolim.models.Usuario;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class LoginActivity extends BaseActivity {

    private static final String PREFS_NAME  = "ecolim_prefs";
    private static final String KEY_USER_ID = "usuario_id";

    private TextInputLayout   layoutEmail, layoutPassword;
    private TextInputEditText edtEmail, edtPassword;
    private MaterialButton    btnIngresar, btnRegistrarse;
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
        btnRegistrarse = findViewById(R.id.btn_registrarse);
        chkRecordar    = findViewById(R.id.chk_recordar);

        btnIngresar.setOnClickListener(v -> intentarLogin());
        btnRegistrarse.setOnClickListener(v -> mostrarDialogoRegistro());
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  LOGIN
    // ══════════════════════════════════════════════════════════════════════════

    private void intentarLogin() {
        String email    = txt(edtEmail);
        String password = txt(edtPassword);

        layoutEmail.setError(null);
        layoutPassword.setError(null);

        if (email.isEmpty()) {
            layoutEmail.setError("Ingrese su correo");
            edtEmail.requestFocus(); return;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            layoutEmail.setError("Correo no válido");
            edtEmail.requestFocus(); return;
        }
        if (password.isEmpty()) {
            layoutPassword.setError("Ingrese su contraseña");
            edtPassword.requestFocus(); return;
        }
        if (password.length() < 6) {
            layoutPassword.setError("Mínimo 6 caracteres");
            edtPassword.requestFocus(); return;
        }

        btnIngresar.setEnabled(false);
        btnIngresar.setText("Verificando...");

        Usuario usuario = db.loginUsuario(email, password);

        if (usuario != null) {
            if (chkRecordar.isChecked()) {
                getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                        .edit().putLong(KEY_USER_ID, usuario.getId()).apply();
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

    // ══════════════════════════════════════════════════════════════════════════
    //  REGISTRO DE NUEVO USUARIO
    // ══════════════════════════════════════════════════════════════════════════

    private void mostrarDialogoRegistro() {
        android.view.View form = getLayoutInflater()
                .inflate(R.layout.dialog_registro_usuario, null);

        // Campos del formulario
        TextInputEditText edtNombre    = form.findViewById(R.id.edt_reg_nombre);
        TextInputEditText edtApellido  = form.findViewById(R.id.edt_reg_apellido);
        TextInputEditText edtDni       = form.findViewById(R.id.edt_reg_dni);
        AutoCompleteTextView ddCargo   = form.findViewById(R.id.dd_reg_cargo);
        TextInputEditText edtTelefono  = form.findViewById(R.id.edt_reg_telefono);
        TextInputEditText edtEmail2    = form.findViewById(R.id.edt_reg_email);
        TextInputEditText edtPass      = form.findViewById(R.id.edt_reg_password);
        TextInputEditText edtPass2     = form.findViewById(R.id.edt_reg_password2);

        TextInputLayout layTelefono    = form.findViewById(R.id.layout_reg_telefono);
        TextInputLayout layEmail2      = form.findViewById(R.id.layout_reg_email);
        TextInputLayout layPass        = form.findViewById(R.id.layout_reg_password);
        TextInputLayout layPass2       = form.findViewById(R.id.layout_reg_password2);

        // Dropdown de cargos
        String[] cargos = {"Operario", "Supervisor", "Jefe de Área",
                "Técnico Ambiental", "Auxiliar", "Inspector"};
        ddCargo.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, cargos));

        final androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Crear cuenta nueva")
                .setView(form)
                .setPositiveButton("Registrarse", null) // null para manejar manualmente
                .setNegativeButton("Cancelar", null)
                .create();

// Mostramos el diálogo primero para que el botón exista en la vista
        dialog.show();

// Configuramos el click del botón manualmente
        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    if (validarYRegistrar(
                            edtNombre, edtApellido, edtDni, ddCargo,
                            edtTelefono, layTelefono,
                            edtEmail2, layEmail2,
                            edtPass, layPass,
                            edtPass2, layPass2,
                            dialog)) {
                        dialog.dismiss();
                    }
                });
    }

    private boolean validarYRegistrar(
            TextInputEditText edtNombre, TextInputEditText edtApellido,
            TextInputEditText edtDni, AutoCompleteTextView ddCargo,
            TextInputEditText edtTelefono, TextInputLayout layTelefono,
            TextInputEditText edtEmail2, TextInputLayout layEmail2,
            TextInputEditText edtPass, TextInputLayout layPass,
            TextInputEditText edtPass2, TextInputLayout layPass2,
            android.app.Dialog dialog) {

        String nombre   = txt(edtNombre);
        String apellido = txt(edtApellido);
        String dni      = txt(edtDni);
        String cargo    = ddCargo.getText().toString().trim();
        String telefono = txt(edtTelefono);
        String email    = txt(edtEmail2);
        String pass     = txt(edtPass);
        String pass2    = txt(edtPass2);

        // Limpiar errores
        layTelefono.setError(null);
        layEmail2.setError(null);
        layPass.setError(null);
        layPass2.setError(null);

        // Validaciones
        if (nombre.isEmpty())   { toast("Ingrese su nombre");    return false; }
        if (apellido.isEmpty()) { toast("Ingrese su apellido");  return false; }
        if (dni.length() != 8)  { toast("DNI debe tener 8 dígitos"); return false; }
        if (cargo.isEmpty())    { toast("Seleccione su cargo");  return false; }

        if (telefono.isEmpty()) {
            layTelefono.setError("Ingrese su teléfono WhatsApp"); return false;
        }
        if (telefono.length() < 9) {
            layTelefono.setError("Número inválido (mín. 9 dígitos)"); return false;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            layEmail2.setError("Correo no válido"); return false;
        }
        if (pass.length() < 6) {
            layPass.setError("Mínimo 6 caracteres"); return false;
        }
        if (!pass.equals(pass2)) {
            layPass2.setError("Las contraseñas no coinciden"); return false;
        }

        // Verificar que el correo no esté ya registrado
        if (db.loginUsuario(email, pass) != null) {
            layEmail2.setError("Este correo ya está registrado"); return false;
        }

        // Crear usuario
        String fecha = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(new Date());

        // Formatear teléfono con código Perú si no lo tiene
        String telFormateado = telefono.startsWith("51")
                ? telefono : "51" + telefono;

        Usuario nuevo = new Usuario(nombre, apellido, dni, cargo,
                "OPERARIO", email, pass, telFormateado, fecha);

        long id = db.insertarUsuario(nuevo);

        if (id != -1) {
            Toast.makeText(this,
                    "✔ Cuenta creada. Ahora puedes iniciar sesión.",
                    Toast.LENGTH_LONG).show();
            // Pre-llenar el email en el login
            edtEmail.setText(email);
            return true;
        } else {
            layEmail2.setError("Error al registrar. El correo o DNI ya existen.");
            return false;
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  HELPERS
    // ══════════════════════════════════════════════════════════════════════════

    private String txt(TextInputEditText e) {
        return e.getText() == null ? "" : e.getText().toString().trim();
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}