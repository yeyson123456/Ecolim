package com.example.ecolim.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.ecolim.models.Registro;
import com.example.ecolim.models.Residuo;
import com.example.ecolim.models.Usuario;

import java.util.ArrayList;
import java.util.List;

public class EcolimDbHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME    = "ecolim.db";
    
    private static final int    DATABASE_VERSION = 2;

    // ── Tabla USUARIOS ───────────────────────────────────
    public static final String T_USUARIOS        = "usuarios";
    public static final String U_ID              = "_id";
    public static final String U_NOMBRE          = "nombre";
    public static final String U_APELLIDO        = "apellido";
    public static final String U_DNI             = "dni";
    public static final String U_CARGO           = "cargo";
    public static final String U_ROL             = "rol";          
    public static final String U_EMAIL           = "email";
    public static final String U_PASSWORD        = "password";
    public static final String U_TELEFONO        = "telefono";
    public static final String U_FECHA_REGISTRO  = "fecha_registro";
    public static final String U_ACTIVO          = "activo";

    // ── Tabla RESIDUOS ───────────────────────────────────
    public static final String T_RESIDUOS        = "residuos";
    public static final String R_ID              = "_id";
    public static final String R_NOMBRE          = "nombre";
    public static final String R_CATEGORIA       = "categoria";
    public static final String R_DESCRIPCION     = "descripcion";
    public static final String R_COLOR           = "color_hex";
    public static final String R_ICONO           = "icono";
    public static final String R_ACTIVO          = "activo";

    // ── Tabla REGISTROS ──────────────────────────────────
    public static final String T_REGISTROS       = "registros";
    public static final String REG_ID            = "_id";
    public static final String REG_USUARIO_ID    = "usuario_id";
    public static final String REG_NOMBRE_USR    = "nombre_usuario";
    public static final String REG_RESIDUO_ID    = "residuo_id";
    public static final String REG_TIPO          = "tipo_residuo";
    public static final String REG_CATEGORIA     = "categoria_residuo";
    public static final String REG_CANTIDAD      = "cantidad";
    public static final String REG_UNIDAD        = "unidad";
    public static final String REG_UBICACION     = "ubicacion";
    public static final String REG_OBSERVACIONES = "observaciones";
    public static final String REG_FECHA         = "fecha";
    public static final String REG_HORA          = "hora";
    public static final String REG_ESTADO        = "estado";
    public static final String REG_SINCRONIZADO  = "sincronizado";
    public static final String REG_FOTO          = "foto_uri";

    // ── Singleton ────────────────────────────────────────
    private static EcolimDbHelper instancia;

    public static synchronized EcolimDbHelper getInstance(Context ctx) {
        if (instancia == null)
            instancia = new EcolimDbHelper(ctx.getApplicationContext());
        return instancia;
    }

    private EcolimDbHelper(Context ctx) {
        super(ctx, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Tabla usuarios (incluye columna rol desde el inicio)
        db.execSQL("CREATE TABLE " + T_USUARIOS + " (" +
                U_ID             + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                U_NOMBRE         + " TEXT NOT NULL," +
                U_APELLIDO       + " TEXT NOT NULL," +
                U_DNI            + " TEXT UNIQUE NOT NULL," +
                U_CARGO          + " TEXT NOT NULL," +
                U_ROL            + " TEXT DEFAULT 'OPERARIO'," +   // ← NUEVO
                U_EMAIL          + " TEXT UNIQUE NOT NULL," +
                U_PASSWORD       + " TEXT NOT NULL," +
                U_TELEFONO       + " TEXT," +
                U_FECHA_REGISTRO + " TEXT NOT NULL," +
                U_ACTIVO         + " INTEGER DEFAULT 1);");

        db.execSQL("CREATE TABLE " + T_RESIDUOS + " (" +
                R_ID          + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                R_NOMBRE      + " TEXT NOT NULL," +
                R_CATEGORIA   + " TEXT NOT NULL," +
                R_DESCRIPCION + " TEXT," +
                R_COLOR       + " TEXT," +
                R_ICONO       + " TEXT," +
                R_ACTIVO      + " INTEGER DEFAULT 1);");

        db.execSQL("CREATE TABLE " + T_REGISTROS + " (" +
                REG_ID            + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                REG_USUARIO_ID    + " INTEGER NOT NULL," +
                REG_NOMBRE_USR    + " TEXT NOT NULL," +
                REG_RESIDUO_ID    + " INTEGER NOT NULL," +
                REG_TIPO          + " TEXT NOT NULL," +
                REG_CATEGORIA     + " TEXT NOT NULL," +
                REG_CANTIDAD      + " REAL NOT NULL," +
                REG_UNIDAD        + " TEXT NOT NULL," +
                REG_UBICACION     + " TEXT NOT NULL," +
                REG_OBSERVACIONES + " TEXT," +
                REG_FECHA         + " TEXT NOT NULL," +
                REG_HORA          + " TEXT NOT NULL," +
                REG_ESTADO        + " TEXT DEFAULT 'Pendiente'," +
                REG_SINCRONIZADO  + " INTEGER DEFAULT 0," +
                REG_FOTO          + " TEXT);");

        insertarResiduosIniciales(db);
        insertarUsuarioAdmin(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            // Agrega la columna rol sin borrar los datos existentes
            db.execSQL("ALTER TABLE " + T_USUARIOS +
                    " ADD COLUMN " + U_ROL + " TEXT DEFAULT 'OPERARIO'");

            // Actualiza el usuario admin para que tenga rol ADMIN
            ContentValues v = new ContentValues();
            v.put(U_ROL, "ADMIN");
            db.update(T_USUARIOS, v, U_EMAIL + "=?",
                    new String[]{"admin@ecolim.com"});
        }
        // Agrega aquí futuros bloques:
        // if (oldVersion < 3) { ... }
    }

    private void insertarResiduosIniciales(SQLiteDatabase db) {
        String[][] residuos = {
                {"Residuos Orgánicos",           "No Peligroso", "Restos de alimentos y materia orgánica",    "#4CAF50"},
                {"Papel y Cartón",               "No Peligroso", "Papeles, revistas, cajas de cartón",        "#2196F3"},
                {"Plásticos",                    "No Peligroso", "Botellas, envases y empaques plásticos",    "#FF9800"},
                {"Metales",                      "No Peligroso", "Latas, alambres y piezas metálicas",        "#9E9E9E"},
                {"Vidrio",                       "No Peligroso", "Botellas y envases de vidrio",              "#00BCD4"},
                {"Residuos Peligrosos",          "Peligroso",    "Residuos con características CRETIB",       "#F44336"},
                {"Residuos Electrónicos (RAEE)", "Especial",     "Equipos electrónicos en desuso",            "#9C27B0"},
                {"Residuos Químicos",            "Peligroso",    "Solventes, ácidos y sustancias químicas",   "#FF5722"},
                {"Residuos Biológicos",          "Peligroso",    "Residuos con agentes biológicos patógenos", "#E91E63"},
                {"Residuos de Construcción",     "Especial",     "Escombros y materiales de demolición",      "#795548"},
                {"Otros",                        "No Peligroso", "Residuos no clasificados",                  "#607D8B"},
        };
        for (String[] r : residuos) {
            ContentValues v = new ContentValues();
            v.put(R_NOMBRE,      r[0]);
            v.put(R_CATEGORIA,   r[1]);
            v.put(R_DESCRIPCION, r[2]);
            v.put(R_COLOR,       r[3]);
            v.put(R_ACTIVO,      1);
            db.insert(T_RESIDUOS, null, v);
        }
    }

    private void insertarUsuarioAdmin(SQLiteDatabase db) {
        ContentValues v = new ContentValues();
        v.put(U_NOMBRE,         "Administrador");
        v.put(U_APELLIDO,       "ECOLIM");
        v.put(U_DNI,            "00000000");
        v.put(U_CARGO,          "Supervisor");
        v.put(U_ROL,            "ADMIN");          // ← rol del admin
        v.put(U_EMAIL,          "admin@ecolim.com");
        v.put(U_PASSWORD,       "admin123");
        v.put(U_TELEFONO,       "999000000");
        v.put(U_FECHA_REGISTRO, "2024-01-01");
        v.put(U_ACTIVO,         1);
        db.insert(T_USUARIOS, null, v);
    }

    public long insertarUsuario(Usuario u) {
        SQLiteDatabase db = getWritableDatabase();
        long id = db.insert(T_USUARIOS, null, usuarioToValues(u));
        db.close();
        return id;
    }

    public Usuario loginUsuario(String email, String password) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(T_USUARIOS, null,
                U_EMAIL + "=? AND " + U_PASSWORD + "=? AND " + U_ACTIVO + "=1",
                new String[]{email, password}, null, null, null);
        Usuario u = null;
        if (cursor != null && cursor.moveToFirst()) {
            u = cursorToUsuario(cursor);
            cursor.close();
        }
        db.close();
        return u;
    }

    public Usuario obtenerUsuarioPorId(long id) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(T_USUARIOS, null,
                U_ID + "=?", new String[]{String.valueOf(id)}, null, null, null);
        Usuario u = null;
        if (cursor != null && cursor.moveToFirst()) {
            u = cursorToUsuario(cursor);
            cursor.close();
        }
        db.close();
        return u;
    }

    public List<Usuario> obtenerTodosUsuarios() {
        List<Usuario> lista = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(T_USUARIOS, null, U_ACTIVO + "=1",
                null, null, null, U_NOMBRE + " ASC");
        if (cursor != null && cursor.moveToFirst()) {
            do { lista.add(cursorToUsuario(cursor)); } while (cursor.moveToNext());
            cursor.close();
        }
        db.close();
        return lista;
    }

    public int actualizarUsuario(Usuario u) {
        SQLiteDatabase db = getWritableDatabase();
        int filas = db.update(T_USUARIOS, usuarioToValues(u),
                U_ID + "=?", new String[]{String.valueOf(u.getId())});
        db.close();
        return filas;
    }

    public int eliminarUsuario(long id) {
        SQLiteDatabase db = getWritableDatabase();
        int filas = db.delete(T_USUARIOS, U_ID + "=?",
                new String[]{String.valueOf(id)});
        db.close();
        return filas;
    }

    public List<Residuo> obtenerTodosResiduos() {
        List<Residuo> lista = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(T_RESIDUOS, null, R_ACTIVO + "=1",
                null, null, null, R_NOMBRE + " ASC");
        if (cursor != null && cursor.moveToFirst()) {
            do { lista.add(cursorToResiduo(cursor)); } while (cursor.moveToNext());
            cursor.close();
        }
        db.close();
        return lista;
    }

    public long insertarRegistro(Registro r) {
        SQLiteDatabase db = getWritableDatabase();
        long id = db.insert(T_REGISTROS, null, registroToValues(r));
        db.close();
        return id;
    }

    public int actualizarRegistro(Registro r) {
        SQLiteDatabase db = getWritableDatabase();
        int filas = db.update(T_REGISTROS, registroToValues(r),
                REG_ID + "=?", new String[]{String.valueOf(r.getId())});
        db.close();
        return filas;
    }

    public int eliminarRegistro(long id) {
        SQLiteDatabase db = getWritableDatabase();
        int filas = db.delete(T_REGISTROS, REG_ID + "=?",
                new String[]{String.valueOf(id)});
        db.close();
        return filas;
    }

    public List<Registro> obtenerTodosRegistros() {
        return queryRegistros(null, null);
    }

    public List<Registro> obtenerRegistrosPorFecha(String fecha) {
        return queryRegistros(REG_FECHA + "=?", new String[]{fecha});
    }

    public List<Registro> obtenerRegistrosPorTipo(String tipo) {
        return queryRegistros(REG_TIPO + "=?", new String[]{tipo});
    }

    public List<Registro> obtenerRegistrosPorRango(String inicio, String fin) {
        return queryRegistros(REG_FECHA + " BETWEEN ? AND ?", new String[]{inicio, fin});
    }

    public List<Registro> obtenerRegistrosPendientesSync() {
        return queryRegistros(REG_SINCRONIZADO + "=0", null);
    }

    public int contarRegistrosHoy(String fecha) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT COUNT(*) FROM " + T_REGISTROS + " WHERE " + REG_FECHA + "=?",
                new String[]{fecha});
        int count = 0;
        if (cursor != null && cursor.moveToFirst()) {
            count = cursor.getInt(0);
            cursor.close();
        }
        db.close();
        return count;
    }

    public double sumarCantidadPorFecha(String fecha) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT SUM(" + REG_CANTIDAD + ") FROM " + T_REGISTROS +
                        " WHERE " + REG_FECHA + "=?", new String[]{fecha});
        double total = 0;
        if (cursor != null && cursor.moveToFirst()) {
            total = cursor.getDouble(0);
            cursor.close();
        }
        db.close();
        return total;
    }

    private List<Registro> queryRegistros(String where, String[] args) {
        List<Registro> lista = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(T_REGISTROS, null, where, args, null, null,
                REG_FECHA + " DESC, " + REG_HORA + " DESC");
        if (cursor != null && cursor.moveToFirst()) {
            do { lista.add(cursorToRegistro(cursor)); } while (cursor.moveToNext());
            cursor.close();
        }
        db.close();
        return lista;
    }

    private ContentValues usuarioToValues(Usuario u) {
        ContentValues v = new ContentValues();
        v.put(U_NOMBRE,         u.getNombre());
        v.put(U_APELLIDO,       u.getApellido());
        v.put(U_DNI,            u.getDni());
        v.put(U_CARGO,          u.getCargo());
        v.put(U_ROL,            u.getRol());      // ← guarda el rol
        v.put(U_EMAIL,          u.getEmail());
        v.put(U_PASSWORD,       u.getPassword());
        v.put(U_TELEFONO,       u.getTelefono());
        v.put(U_FECHA_REGISTRO, u.getFechaRegistro());
        v.put(U_ACTIVO,         u.isActivo() ? 1 : 0);
        return v;
    }

    private ContentValues registroToValues(Registro r) {
        ContentValues v = new ContentValues();
        v.put(REG_USUARIO_ID,    r.getUsuarioId());
        v.put(REG_NOMBRE_USR,    r.getNombreUsuario());
        v.put(REG_RESIDUO_ID,    r.getResiduoId());
        v.put(REG_TIPO,          r.getTipoResiduo());
        v.put(REG_CATEGORIA,     r.getCategoriaResiduo());
        v.put(REG_CANTIDAD,      r.getCantidad());
        v.put(REG_UNIDAD,        r.getUnidad());
        v.put(REG_UBICACION,     r.getUbicacion());
        v.put(REG_OBSERVACIONES, r.getObservaciones());
        v.put(REG_FECHA,         r.getFecha());
        v.put(REG_HORA,          r.getHora());
        v.put(REG_ESTADO,        r.getEstado());
        v.put(REG_SINCRONIZADO,  r.isSincronizado() ? 1 : 0);
        v.put(REG_FOTO,          r.getFotoUri());
        return v;
    }

    private Usuario cursorToUsuario(Cursor c) {
        Usuario u = new Usuario();
        u.setId(c.getLong(c.getColumnIndexOrThrow(U_ID)));
        u.setNombre(c.getString(c.getColumnIndexOrThrow(U_NOMBRE)));
        u.setApellido(c.getString(c.getColumnIndexOrThrow(U_APELLIDO)));
        u.setDni(c.getString(c.getColumnIndexOrThrow(U_DNI)));
        u.setCargo(c.getString(c.getColumnIndexOrThrow(U_CARGO)));
        // Lee el rol — getColumnIndex devuelve -1 si no existe (compatibilidad)
        int colRol = c.getColumnIndex(U_ROL);
        u.setRol(colRol != -1 ? c.getString(colRol) : u.getCargo());
        u.setEmail(c.getString(c.getColumnIndexOrThrow(U_EMAIL)));
        u.setPassword(c.getString(c.getColumnIndexOrThrow(U_PASSWORD)));
        u.setTelefono(c.getString(c.getColumnIndexOrThrow(U_TELEFONO)));
        u.setFechaRegistro(c.getString(c.getColumnIndexOrThrow(U_FECHA_REGISTRO)));
        u.setActivo(c.getInt(c.getColumnIndexOrThrow(U_ACTIVO)) == 1);
        return u;
    }

    private Residuo cursorToResiduo(Cursor c) {
        Residuo r = new Residuo();
        r.setId(c.getLong(c.getColumnIndexOrThrow(R_ID)));
        r.setNombre(c.getString(c.getColumnIndexOrThrow(R_NOMBRE)));
        r.setCategoria(c.getString(c.getColumnIndexOrThrow(R_CATEGORIA)));
        r.setDescripcion(c.getString(c.getColumnIndexOrThrow(R_DESCRIPCION)));
        r.setColorHex(c.getString(c.getColumnIndexOrThrow(R_COLOR)));
        r.setActivo(c.getInt(c.getColumnIndexOrThrow(R_ACTIVO)) == 1);
        return r;
    }

    private Registro cursorToRegistro(Cursor c) {
        Registro r = new Registro();
        r.setId(c.getLong(c.getColumnIndexOrThrow(REG_ID)));
        r.setUsuarioId(c.getLong(c.getColumnIndexOrThrow(REG_USUARIO_ID)));
        r.setNombreUsuario(c.getString(c.getColumnIndexOrThrow(REG_NOMBRE_USR)));
        r.setResiduoId(c.getLong(c.getColumnIndexOrThrow(REG_RESIDUO_ID)));
        r.setTipoResiduo(c.getString(c.getColumnIndexOrThrow(REG_TIPO)));
        r.setCategoriaResiduo(c.getString(c.getColumnIndexOrThrow(REG_CATEGORIA)));
        r.setCantidad(c.getDouble(c.getColumnIndexOrThrow(REG_CANTIDAD)));
        r.setUnidad(c.getString(c.getColumnIndexOrThrow(REG_UNIDAD)));
        r.setUbicacion(c.getString(c.getColumnIndexOrThrow(REG_UBICACION)));
        r.setObservaciones(c.getString(c.getColumnIndexOrThrow(REG_OBSERVACIONES)));
        r.setFecha(c.getString(c.getColumnIndexOrThrow(REG_FECHA)));
        r.setHora(c.getString(c.getColumnIndexOrThrow(REG_HORA)));
        r.setEstado(c.getString(c.getColumnIndexOrThrow(REG_ESTADO)));
        r.setSincronizado(c.getInt(c.getColumnIndexOrThrow(REG_SINCRONIZADO)) == 1);
        r.setFotoUri(c.getString(c.getColumnIndexOrThrow(REG_FOTO)));
        return r;
    }
}