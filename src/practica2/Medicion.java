package practica2;

public class Medicion {
    private String nombre;
    private String unidad;
    private double valor;

    public Medicion() {
    }

    public Medicion(String nombre, String unidad, double valor) {
        this.nombre = nombre;
        this.unidad = unidad;
        this.valor = valor;
    }

    public String getNombre() {
        return nombre;
    }

    public String getUnidad() {
        return unidad;
    }

    public double getValor() {
        return valor;
    }

    @Override
    public String toString() {
        return nombre + ": " + valor + " " + unidad;
    }
}