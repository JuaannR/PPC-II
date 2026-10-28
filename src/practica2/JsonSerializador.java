package practica2;

import com.google.gson.Gson;

public class JsonSerializador {
    private static final Gson GSON = new Gson();

    public static String serializar(MensajeDistribucion m) {
        return GSON.toJson(m);
    }

    public static MensajeDistribucion deserializar(String json) {
        return GSON.fromJson(json, MensajeDistribucion.class);
    }

}
