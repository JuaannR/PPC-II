package practica2;

import com.google.gson.Gson;

public class JsonSerializador {
    private static final Gson GSON = new Gson();

    /** Sirve para cualquier mensaje: distribucion, control o respuesta. */
    public static String serializar(Object mensaje) {
        return GSON.toJson(mensaje);
    }

    public static MensajeDistribucion deserializar(String json) {
        return GSON.fromJson(json, MensajeDistribucion.class);
    }

    public static MensajeControl deserializarControl(String json) {
        return GSON.fromJson(json, MensajeControl.class);
    }

    public static RespuestaControl deserializarRespuesta(String json) {
        return GSON.fromJson(json, RespuestaControl.class);
    }
}