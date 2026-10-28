package practica2;

import java.net.BindException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Random;

public class Servidor {

    private static volatile long periodoMs = 5000;
    private static final Random RND = new Random();

    private static double aleatorio(double min, double max) {
        return Math.round((min + RND.nextDouble() * (max - min)) * 100.0) / 100.0;
    }

    private static MensajeDistribucion generarMensaje(int id) {
        MensajeDistribucion m = new MensajeDistribucion(id, System.currentTimeMillis());
        m.addMedicion(new Medicion("Temp", "C", aleatorio(-5, 40)));
        switch (id) {
            case 1:
                m.addMedicion(new Medicion("Hum", "%", aleatorio(10, 100)));
                m.addMedicion(new Medicion("PM10", "ug/m3", aleatorio(0, 150)));
                break;
            case 2:
                m.addMedicion(new Medicion("SO2", "ug/m3", aleatorio(0, 350)));
                m.addMedicion(new Medicion("NO2", "ug/m3", aleatorio(0, 200)));
                break;
            default:
                m.addMedicion(new Medicion("O3", "ug/m3", aleatorio(0, 240)));
                m.addMedicion(new Medicion("Presion", "hPa", aleatorio(980, 1040)));
        }
        return m;
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Error: Debes indicar el ID del servidor.");
            System.out.println("Uso: java Servidor <id>");
            return;
        }

        int id = Integer.parseInt(args[0]);
        int puertoControl = 6000 + id;
        int puertoBroadcast = 5000;

        System.out.println("=== INICIANDO SERVIDOR " + id + " ===");
        System.out.println("-> Intentando escuchar control unicast en puerto: " + puertoControl);

        // HILO 1: Envio periodico de tramas de datos en Broadcast
        Thread hiloBroadcast = new Thread(() -> {
            try (DatagramSocket socketBroadcast = new DatagramSocket()) {
                socketBroadcast.setBroadcast(true);

                while (true) {
                    MensajeDistribucion mensaje = generarMensaje(id);
                    byte[] buffer = new Trama("JSON", "DISTRIBUCION", JsonSerializador.serializar(mensaje)).codificar();

                    InetAddress direccionBroadcast = InetAddress.getByName("255.255.255.255");
                    DatagramPacket paquete = new DatagramPacket(buffer, buffer.length, direccionBroadcast,
                            puertoBroadcast);

                    socketBroadcast.send(paquete);
                    Thread.sleep(periodoMs); // 5 segundos de frecuencia
                }
            } catch (Exception e) {
                System.out.println("[ERROR] Hilo Broadcast: " + e.getMessage());
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

                String respuestaTexto;

                if (mensajeControl.equals("PING")) {
                    respuestaTexto = "PONG";
                } else {
                    System.out.println(
                            "\n[CONTROL RECIBIDO] De cliente " + ipCliente.getHostAddress() + ":" + puertoCliente);
                    System.out.println("-> Orden: " + mensajeControl);
                    respuestaTexto = "ACK: Servidor " + id + " ha procesado tu orden correctamente.";
                }

                byte[] bufferRespuesta = respuestaTexto.getBytes();

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