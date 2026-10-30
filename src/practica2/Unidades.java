package practica2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Catalogo de unidades y conversiones. Lo usan el servidor (para convertir y validar)
 * y el cliente (para mostrar las unidades disponibles de cada variable).
 */
public class Unidades {

    // Cada fila es un grupo de unidades convertibles entre si
    private static final String[][] GRUPOS = {
            { "C", "F", "K" },
            { "hPa", "kPa" },
            { "ug/m3", "mg/m3" },
            { "%" }
    };

    // Unidades a las que se puede cambiar una variable que esta en 'unidad' (incluida ella).
    public static List<String> compatibles(String unidad) {
        for (String[] grupo : GRUPOS) {
            if (Arrays.asList(grupo).contains(unidad)) {
                return new ArrayList<String>(Arrays.asList(grupo));
            }
        }
        return new ArrayList<String>();
    }

    // Devuelve el nombre oficial de la unidad (C, F, K, hPa...) o null si no existe
    public static String normalizar(String texto) {
        String t = texto.trim();
        if (t.equalsIgnoreCase("celsius")) return "C";
        if (t.equalsIgnoreCase("fahrenheit")) return "F";
        if (t.equalsIgnoreCase("kelvin")) return "K";
        for (String[] grupo : GRUPOS) {
            for (String u : grupo) {
                if (u.equalsIgnoreCase(t)) {
                    return u;
                }
            }
        }
        return null;
    }

    public static double convertir(double valor, String de, String a) {
        if (de.equals(a)) {
            return valor;
        }
        return desdeBase(haciaBase(valor, de), a);
    }

    // Bases: C, hPa, ug/m3
    private static double haciaBase(double v, String u) {
        switch (u) {
            case "F":     return (v - 32.0) * 5.0 / 9.0;
            case "K":     return v - 273.15;
            case "kPa":   return v * 10.0;
            case "mg/m3": return v * 1000.0;
            default:      return v;
        }
    }

    private static double desdeBase(double v, String u) {
        switch (u) {
            case "F":     return v * 9.0 / 5.0 + 32.0;
            case "K":     return v + 273.15;
            case "kPa":   return v / 10.0;
            case "mg/m3": return v / 1000.0;
            default:      return v;
        }
    }
}