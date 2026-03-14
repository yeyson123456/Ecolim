package com.example.ecolim.models;

public class Usuario {

    private long   id;
    private String nombre;
    private String apellido;
    private String dni;
    private String cargo;           // Operario, Supervisor, Jefe de área
    private String rol;             // Rol en el sistema: ADMIN, SUPERVISOR, OPERARIO
    private String email;
    private String password;
    private String telefono;
    private String fechaRegistro;
    private boolean activo;

    // ── Constructor vacío ────────────────────────────────
    public Usuario() {}

    // ── Constructor completo ─────────────────────────────
    public Usuario(String nombre, String apellido, String dni,
                   String cargo, String email, String password,
                   String telefono, String fechaRegistro) {
        this.nombre        = nombre;
        this.apellido      = apellido;
        this.dni           = dni;
        this.cargo         = cargo;
        this.rol           = "OPERARIO"; // rol por defecto
        this.email         = email;
        this.password      = password;
        this.telefono      = telefono;
        this.fechaRegistro = fechaRegistro;
        this.activo        = true;
    }

    // ── Constructor con rol ───────────────────────────────
    public Usuario(String nombre, String apellido, String dni,
                   String cargo, String rol, String email, String password,
                   String telefono, String fechaRegistro) {
        this.nombre        = nombre;
        this.apellido      = apellido;
        this.dni           = dni;
        this.cargo         = cargo;
        this.rol           = rol;
        this.email         = email;
        this.password      = password;
        this.telefono      = telefono;
        this.fechaRegistro = fechaRegistro;
        this.activo        = true;
    }

    // ── Getters ──────────────────────────────────────────
    public long    getId()            { return id; }
    public String  getNombre()        { return nombre; }
    public String  getApellido()      { return apellido; }
    public String  getDni()           { return dni; }
    public String  getCargo()         { return cargo; }
    public String  getRol()           { return rol != null ? rol : cargo; } // fallback a cargo si rol es null
    public String  getEmail()         { return email; }
    public String  getPassword()      { return password; }
    public String  getTelefono()      { return telefono; }
    public String  getFechaRegistro() { return fechaRegistro; }
    public boolean isActivo()         { return activo; }

    /** Nombre completo para mostrar en UI */
    public String getNombreCompleto() {
        return nombre + " " + apellido;
    }

    // ── Setters ──────────────────────────────────────────
    public void setId(long v)             { this.id            = v; }
    public void setNombre(String v)       { this.nombre        = v; }
    public void setApellido(String v)     { this.apellido      = v; }
    public void setDni(String v)          { this.dni           = v; }
    public void setCargo(String v)        { this.cargo         = v; }
    public void setRol(String v)          { this.rol           = v; }
    public void setEmail(String v)        { this.email         = v; }
    public void setPassword(String v)     { this.password      = v; }
    public void setTelefono(String v)     { this.telefono      = v; }
    public void setFechaRegistro(String v){ this.fechaRegistro = v; }
    public void setActivo(boolean v)      { this.activo        = v; }
}