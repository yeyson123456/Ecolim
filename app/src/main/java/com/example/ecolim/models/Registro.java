package com.example.ecolim.models;


public class Registro {

    // Estados del registro
    public static final String ESTADO_PENDIENTE  = "Pendiente";
    public static final String ESTADO_COMPLETADO = "Completado";
    public static final String ESTADO_ANULADO    = "Anulado";

    private long    id;
    private long    usuarioId;
    private String  nombreUsuario;    // Guardado para reportes sin JOIN
    private long    residuoId;
    private String  tipoResiduo;      // Guardado para reportes sin JOIN
    private String  categoriaResiduo; // Peligroso / No Peligroso / Especial
    private double  cantidad;
    private String  unidad;           // kg, L, t, unidades
    private String  ubicacion;        // Área o zona de recolección
    private String  observaciones;    // Notas adicionales del trabajador
    private String  fecha;            // Formato: yyyy-MM-dd
    private String  hora;             // Formato: HH:mm
    private String  estado;           // Pendiente / Completado / Anulado
    private boolean sincronizado;     // true si ya fue enviado al servidor
    private String  fotoUri;          // Foto opcional de la evidencia

    // ── Constructor vacío ────────────────────────────────
    public Registro() {
        this.estado       = ESTADO_PENDIENTE;
        this.sincronizado = false;
    }

    // ── Constructor completo ─────────────────────────────
    public Registro(long usuarioId, String nombreUsuario,
                    long residuoId, String tipoResiduo, String categoriaResiduo,
                    double cantidad, String unidad,
                    String ubicacion, String observaciones,
                    String fecha, String hora, String fotoUri) {
        this.usuarioId        = usuarioId;
        this.nombreUsuario    = nombreUsuario;
        this.residuoId        = residuoId;
        this.tipoResiduo      = tipoResiduo;
        this.categoriaResiduo = categoriaResiduo;
        this.cantidad         = cantidad;
        this.unidad           = unidad;
        this.ubicacion        = ubicacion;
        this.observaciones    = observaciones;
        this.fecha            = fecha;
        this.hora             = hora;
        this.fotoUri          = fotoUri;
        this.estado           = ESTADO_PENDIENTE;
        this.sincronizado     = false;
    }

    public long    getId()                { return id; }
    public long    getUsuarioId()         { return usuarioId; }
    public String  getNombreUsuario()     { return nombreUsuario; }
    public long    getResiduoId()         { return residuoId; }
    public String  getTipoResiduo()       { return tipoResiduo; }
    public String  getCategoriaResiduo()  { return categoriaResiduo; }
    public double  getCantidad()          { return cantidad; }
    public String  getUnidad()            { return unidad; }
    public String  getUbicacion()         { return ubicacion; }
    public String  getObservaciones()     { return observaciones; }
    public String  getFecha()             { return fecha; }
    public String  getHora()              { return hora; }
    public String  getEstado()            { return estado; }
    public boolean isSincronizado()       { return sincronizado; }
    public String  getFotoUri()           { return fotoUri; }

    // Texto para mostrar en lista
    public String getResumen() {
        return tipoResiduo + " — " + cantidad + " " + unidad;
    }

    
    public void setId(long v)                { this.id                = v; }
    public void setUsuarioId(long v)         { this.usuarioId         = v; }
    public void setNombreUsuario(String v)   { this.nombreUsuario     = v; }
    public void setResiduoId(long v)         { this.residuoId         = v; }
    public void setTipoResiduo(String v)     { this.tipoResiduo       = v; }
    public void setCategoriaResiduo(String v){ this.categoriaResiduo  = v; }
    public void setCantidad(double v)        { this.cantidad          = v; }
    public void setUnidad(String v)          { this.unidad            = v; }
    public void setUbicacion(String v)       { this.ubicacion         = v; }
    public void setObservaciones(String v)   { this.observaciones     = v; }
    public void setFecha(String v)           { this.fecha             = v; }
    public void setHora(String v)            { this.hora              = v; }
    public void setEstado(String v)          { this.estado            = v; }
    public void setSincronizado(boolean v)   { this.sincronizado      = v; }
    public void setFotoUri(String v)         { this.fotoUri           = v; }
}