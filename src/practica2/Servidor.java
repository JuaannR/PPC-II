package practica2;

import java.net.BindException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class Servidor {

    private static volatile long periodoMs = 5000;
    private static volatile boolean activo = true;
    public static volatile String formato; // XML o JSON
    private static final Map<String, String> unidadesBase = new ConcurrentHashMap<>();
    private static final Map<String, String> unidadesActuales = new ConcurrentHashMap<>();
    private static Thread hiloBroadcast;
    private static final Random RND = new Random();

    private static double aleatorio(double min, double max) {
        return Math.round((min + RND.nextDouble() * (max - min)) * 100.0) / 100.0;
    }

    private static Medicion medir(String nombre, String unidadBase, double min, double max) {
        unidadesBase.putIfAbsent(nombre, unidadBase); //asi el servidor sabe que variables tiene
        String unidad = unidadesActuales.getOrDefault(nombre, unidadBase);
        double valor = Unidades.convertir(aleatorio(min, max), unidadBase, unidad);
        return new Medicion(nombre, unidad, Math.round(valor * 100.0) / 100.0);
    }

    private static MensajeDistribucion generarMensaje(int id) {
        MensajeDistribucion m = new MensajeDistribucion(id, System.currentTimeMillis());
        m.addMedicion(medir("Temp", "C", -5, 40));
        switch (id) {
            case 1:
                m.addMedicion(medir("Hum", "%", 10, 100));
                m.addMedicion(medir("PM10", "ug/m3", 0, 150));
                break;
            case 2:
                m.addMedicion(medir("SO2", "ug/m3", 0, 350));
                m.addMedicion(medir("NO2", "ug/m3", 0, 200));
                break;
            default:
                m.addMedicion(medir("O3", "ug/m3", 0, 240));
                m.addMedicion(medir("Presion", "hPa", 980, 1040));
        }
        return m;
    }

    private static byte[] procesarControl(int id, DatagramPacket paquete) {
        RespuestaControl r;
        try {
            Trama t = Trama.decodificar(paquete.getData(), paquete.getLength());
            if (!t.getTipo().equals("CONTROL")) {
                r = new RespuestaControl(id, false, "Tipo de trama no esperado: " + t.getTipo());
            } else if (!t.getFormato().equals("JSON")) {
                r = new RespuestaControl(id, false, "Formato de control no soportado todavia: " + t.getFormato());
            } else {
                MensajeControl c = JsonSerializador.deserializarControl(t.getCuerpo());
                Archivos.anadir("Servidor" + id, "control_recibido.txt", t.getCuerpo() + System.lineSeparator());
                System.out.println("-> Orden: " + c);
                r = aplicarOrden(id, c);
            }
        } catch (Exception e) {
            r = new RespuestaControl(id, false, "Trama de control no valida: " + e.getMessage());
        }
        return new Trama("JSON", "RESPUESTA", JsonSerializador.serializar(r)).codificar();
    }

    private static RespuestaControl aplicarOrden(int id, MensajeControl c) {
        String op = c.getOperacion() == null ? "" : c.getOperacion();
        String p = c.getParametro() == null ? "" : c.getParametro().trim();

        switch (op) {
            case "ENVIO":
                if (p.equalsIgnoreCase("ON")) {
                    activo = true;
                    hiloBroadcast.interrupt(); // reanuda sin esperar al sleep anterior
                    return new RespuestaControl(id, true, "Envio activado");
                }
                if (p.equalsIgnoreCase("OFF")) {
                    activo = false;
                    return new RespuestaControl(id, true, "Envio desactivado");
                }
                return new RespuestaControl(id, false, "Parametro no valido: usa ON u OFF");

            case "FRECUENCIA":
                try {
                    double segundos = Double.parseDouble(p.replace(',', '.'));
                    if (segundos < 0.1 || segundos > 3600) {
                        return new RespuestaControl(id, false, "El periodo debe estar entre 0.1 y 3600 segundos");
                    }
                    periodoMs = (long) (segundos * 1000);
                    hiloBroadcast.interrupt(); // aplica el nuevo periodo ya
                    return new RespuestaControl(id, true, "Se enviara cada " + segundos + " s");
                } catch (NumberFormatException e) {
                    return new RespuestaControl(id, false, "La frecuencia debe ser un numero (segundos)");
                }

            case "FORMATO":
                    String nuevo = p.toUpperCase();
                    if (!nuevo.equals("XML") && !nuevo.equals("JSON")) {
                        return new RespuestaControl(id, false, "Formato no valido: usa XML o JSON");
                    }
                    formato = nuevo;
                    hiloBroadcast.interrupt(); // siguiente trama sale en el nuevo formato
                    return new RespuestaControl(id, true, "Formato de distribucion: " + nuevo);

            case "UNIDADES":
                String[] partes = p.split(":", 2);
                if (partes.length != 2) {
                    return new RespuestaControl(id, false, "Usa variable:unidad (ej: Temp:F)");
                }
                String variable = null;
                for (String nombre : unidadesBase.keySet()) {
                    if (nombre.equalsIgnoreCase(partes[0].trim())) {
                        variable = nombre;
                    }
                }
                if (variable == null) {
                    return new RespuestaControl(id, false, "Este servidor no tiene la variable "
                            + partes[0].trim() + " (tiene: " + unidadesBase.keySet() + ")");
                }
                String destino = Unidades.normalizar(partes[1]);
                List<String> validas = Unidades.compatibles(unidadesBase.get(variable));
                if (destino ==null || !validas.contains(destino)) {
                    return new RespuestaControl(id, false, "Unidad no valida para " + variable
                + ". Disponibles: " + validas);
                }
                unidadesActuales.put(variable, destino);
                return new RespuestaControl(id, true, variable + " ahora en " + destino);
            
            default:
                return new RespuestaControl(id, false, "Operacion desconocida: " + op);
        }

    }

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Error: Debes indicar el ID del servidor y el formato.");
            System.out.println("Uso: java Servidor <id> <XML|JSON>");
            return;
        }

        int id = Integer.parseInt(args[0]);
        String formatoInicial = args[1].toUpperCase();
        if (!formatoInicial.equals("XML") && !formatoInicial.equals("JSON")) {
            System.out.println("Error: el formato debe ser XML o JSON.");
            return;
        }
        formato = formatoInicial;
        generarMensaje(id);

        int puertoControl = 6000 + id;
        int puertoBroadcast = 5000;

        System.out.println("=== INICIANDO SERVIDOR " + id + " ===");
        System.out.println("-> Intentando escuchar control unicast en puerto: " + puertoControl);

        // HILO 1: Envío periódico de tramas de datos en Broadcast
        hiloBroadcast = new Thread(() -> {
            try (DatagramSocket socketBroadcast = new DatagramSocket()) {
                socketBroadcast.setBroadcast(true);

                while (true) {
                    if (activo) {
                        MensajeDistribucion mensaje = generarMensaje(id);

                        String cuerpo;
                        if (formato.equals("XML")) {
                            cuerpo = XmlSerializador.serializar(mensaje);
                        } else {
                            cuerpo = JsonSerializador.serializar(mensaje);
                        }

                        byte[] buffer = new Trama(formato, "DISTRIBUCION", cuerpo).codificar();

                        InetAddress direccionBroadcast = InetAddress.getByName("255.255.255.255");

                        DatagramPacket paquete = new DatagramPacket(buffer, buffer.length, direccionBroadcast,
                                puertoBroadcast);

                        socketBroadcast.send(paquete);
                    }

                    try {
                        Thread.sleep(periodoMs);
                    } catch (InterruptedException e) {
                        // Cambio de frecuencia o reactivación:
                        // se recalcula la espera.
                    }
                }
            } catch (Exception e) {
                System.out.println(
                        "[ERROR] Hilo Broadcast: " + e.getMessage());
            }
        });

        hiloBroadcast.setDaemon(true);
        hiloBroadcast.start();

        // HILO PRINCIPAL: Apertura del puerto de control
        try (DatagramSocket socketControl = new DatagramSocket(puertoControl)) {
            System.out.println("-> Servidor " + id + " arrancado con exito!");
            byte[] bufferRecepcion = new byte[4096];

            while (true) {
                DatagramPacket paqueteRecibido = new DatagramPacket(bufferRecepcion, bufferRecepcion.length);
                socketControl.receive(paqueteRecibido);

                InetAddress ipCliente = paqueteRecibido.getAddress();
                int puertoCliente = paqueteRecibido.getPort();

                String mensajeControl = new String(paqueteRecibido.getData(), 0, paqueteRecibido.getLength());

                byte[] bufferRespuesta;

                if (mensajeControl.equals("PING")) {
                    bufferRespuesta = "PONG".getBytes();
                } else {
                    System.out.println(
                            "\n[CONTROL RECIBIDO] De cliente " + ipCliente.getHostAddress() + ":" + puertoCliente);
                    bufferRespuesta = procesarControl(id, paqueteRecibido);
                }

                DatagramPacket paqueteRespuesta = new DatagramPacket(bufferRespuesta, bufferRespuesta.length, ipCliente,
                        puertoCliente);
                socketControl.send(paqueteRespuesta);
                System.out.println("-> Respuesta unicast enviada al cliente.");
            }

        } catch (BindException e) {
            System.out.println("\n[ERROR CRITICO] El puerto " + puertoControl + " ya esta en uso.");
            System.out.println("-> Ya hay un Servidor " + id + " ejecutandose. Cerrando este proceso...");
            System.exit(1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}