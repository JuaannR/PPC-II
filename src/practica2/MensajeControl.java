package practica2;

/**
 * Orden que un cliente envia por unicast a un servidor concreto.
 * operacion: FORMATO | FRECUENCIA | UNIDADES | ENVIO
 * parametro: XML|JSON, segundos, "variable:unidad" u ON|OFF segun la operacion.
 */
public class MensajeControl {
    private int idServidor; // servidor destinatario
    private String operacion;
    private String parametro;

    public MensajeControl() {
    }

    public MensajeControl(int idServidor, String operacion, String parametro) {
        this.idServidor = idServidor;
        this.operacion = operacion;
        this.parametro = parametro;
    }

    public int getIdServidor() {
        return idServidor;
    }

    public String getOperacion() {
        return operacion;
    }

    public String getParametro() {
        return parametro;
    }

    @Override
    public String toString() {
        return operacion + (parametro != null ? " " + parametro : "");
    }
}