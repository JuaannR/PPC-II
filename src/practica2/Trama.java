package practica2;

import java.nio.charset.StandardCharsets;

public class Trama {

    private final String formato; // JSON o XML
    private final String tipo; // DISTRIBUCION, CONTROL, RESPUESTA
    private final String cuerpo;

    public Trama(String formato, String tipo, String cuerpo) {
        this.formato = formato;
        this.tipo = tipo;
        this.cuerpo = cuerpo;
    }

    public String getFormato() {
        return formato;
    }

    public String getTipo() {
        return tipo;
    }

    public String getCuerpo() {
        return cuerpo;
    }

    public byte[] codificar() {
        String s = "FORMATO=" + formato + "\n" + "TIPO=" + tipo + "\n\n" + cuerpo;
        return s.getBytes(StandardCharsets.UTF_8);
    }

    public static Trama decodificar(byte[] datos, int longitud) {
        String s = new String(datos, 0, longitud, StandardCharsets.UTF_8);
        int fin = s.indexOf("\n\n");
        if (fin < 0) {
            throw new IllegalArgumentException("Trama sin cabecera");
        }
        String formato = null;
        String tipo = null;
        for (String linea : s.substring(0, fin).split("\n")) {
            String[] kv = linea.split("=", 2);
            if (kv.length == 2) {
                if (kv[0].trim().equals("FORMATO"))
                    formato = kv[1].trim();
                if (kv[0].trim().equals("TIPO"))
                    tipo = kv[1].trim();
            }
        }
        if (formato == null || tipo == null) {
            throw new IllegalArgumentException("Cabecera incompleta");
        }
        return new Trama(formato, tipo, s.substring(fin + 2));
    }
}
