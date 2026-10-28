package practica2;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class Archivos {

    /** Añade el contenido como una nueva línea al final de datos/<carpeta>/<nombre>. */
    public static void anadir(String carpeta, String nombre, String contenido) {
        try {
            Path dir = Paths.get("datos", carpeta);
            Files.createDirectories(dir);
            byte[] bytes = (contenido + System.lineSeparator()).getBytes(StandardCharsets.UTF_8);
            Files.write(dir.resolve(nombre), bytes,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.out.println("[ERROR] No se pudo guardar el archivo: " + e.getMessage());
        }
    }
}