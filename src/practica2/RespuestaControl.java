package practica2;

/** Respuesta unicast de un servidor a una orden de control. */
public class RespuestaControl {
    private int idServidor; // servidor que responde
    private boolean ok;
    private String detalle;

    public RespuestaControl() {
    }

    public RespuestaControl(int idServidor, boolean ok, String detalle) {
        this.idServidor = idServidor;
        this.ok = ok;
        this.detalle = detalle;
    }

    public int getIdServidor() {
        return idServidor;
    }

    public boolean isOk() {
        return ok;
    }

    public String getDetalle() {
        return detalle;
    }

    @Override
    public String toString() {
        return "[Servidor " + idServidor + "] " + (ok ? "OK" : "ERROR") + ": " + detalle;
    }
}