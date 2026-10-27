package practica2;

import java.util.ArrayList;
import java.util.List;

public class MensajeDistribucion {
    private int idServidor;
    private long timestamp;
    private List<Medicion> mediciones = new ArrayList<>();

    public MensajeDistribucion() {
    }

    public MensajeDistribucion(int idServidor, long timestamp) {
        this.idServidor = idServidor;
        this.timestamp = timestamp;
    }

    public void addMedicion(Medicion m) {
        mediciones.add(m);
    }

    public int getIdServidor() {
        return idServidor;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public List<Medicion> getMediciones() {
        return mediciones;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[Servidor " + idServidor + "]");
        for (Medicion m : mediciones) {
            sb.append(" | ").append(m);
        }
        return sb.toString();
    }
}