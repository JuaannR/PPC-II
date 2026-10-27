package practica2;

import java.net.BindException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class Servidor {
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
                    String tramaDatos = "[Servidor " + id + "] - Temp: 22.5 C | Hum: 55 | SO2: 10";
                    byte[] buffer = tramaDatos.getBytes();
                    
                    InetAddress direccionBroadcast = InetAddress.getByName("255.255.255.255");
                    DatagramPacket paquete = new DatagramPacket(buffer, buffer.length, direccionBroadcast, puertoBroadcast);
                    
                    socketBroadcast.send(paquete);
                    Thread.sleep(5000); // 5 segundos de frecuencia
                }
            } catch (Exception e) {
                // Hilo secundario
            }
        });
        hiloBroadcast.setDaemon(true);
        hiloBroadcast.start();

        // HILO PRINCIPAL: Apertura del puerto de control
        try (DatagramSocket socketControl = new DatagramSocket(puertoControl)) {
            System.out.println("-> Servidor " + id + " arrancado con exito!");
            byte[] bufferRecepcion = new byte[1024];

            while (true) {
                DatagramPacket paqueteRecibido = new DatagramPacket(bufferRecepcion, bufferRecepcion.length);
                socketControl.receive(paqueteRecibido);

                InetAddress ipCliente = paqueteRecibido.getAddress();
                int puertoCliente = paqueteRecibido.getPort();
                
                String mensajeControl = new String(paqueteRecibido.getData(), 0, paqueteRecibido.getLength());
                System.out.println("\n[CONTROL RECIBIDO] De cliente " + ipCliente.getHostAddress() + ":" + puertoCliente);
                System.out.println("-> Orden: " + mensajeControl);

                String respuestaTexto = "ACK: Servidor " + id + " ha procesado tu orden correctamente.";
                byte[] bufferRespuesta = respuestaTexto.getBytes();

                DatagramPacket paqueteRespuesta = new DatagramPacket(bufferRespuesta, bufferRespuesta.length, ipCliente, puertoCliente);
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