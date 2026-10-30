package practica2;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Scanner;

public class Cliente {
    public static void main(String[] args) {
        // Puerto fijo comun de broadcast definido en la practica (5000)
        int puertoBroadcast = 5000;

        System.out.println("=== CLIENTE DE CALIDAD DEL AIRE INICIADO ===");

        // =========================================================================
        // HILO SECUNDARIO (RECEPTOR DE BROADCAST)
        // Se ejecuta en paralelo para escuchar de forma continua los datos que
        // envian todos los servidores sin bloquear la consola principal.
        // =========================================================================
        Thread hiloReceptor = new Thread(() -> {
            try {
                // Creamos el socket sin puerto primero para configurar el reuso de direcciones
                DatagramSocket socketEscucha = new DatagramSocket(null);
                socketEscucha.setReuseAddress(true);
                // Vinculamos al puerto broadcast (permite abrir multiples clientes a la vez)
                socketEscucha.bind(new InetSocketAddress(puertoBroadcast));

                byte[] buffer = new byte[8192];

                while (true) {
                    DatagramPacket paquete = new DatagramPacket(buffer, buffer.length);
                    socketEscucha.receive(paquete); // Bloqueante hasta que llega una trama UDP

                    try {
                        Trama t = Trama.decodificar(paquete.getData(), paquete.getLength());

                        MensajeDistribucion m;
                        if (t.getFormato().equals("JSON")) {
                            m = JsonSerializador.deserializar(t.getCuerpo());
                        } else if (t.getFormato().equals("XML")) {
                            m = XmlSerializador.deserializar(t.getCuerpo()); // valida contra el XSD
                        } else {
                            System.out.println("\n[DATOS " + t.getFormato() + "] formato no soportado");
                            continue;
                        }

                        System.out.println("\n[DATOS " + t.getFormato() + "] " + m);
                        Archivos.anadir("Servidor" + m.getIdServidor(), "mediciones.txt",
                                t.getCuerpo().trim() + System.lineSeparator());
                    } catch (Exception e) {
                        System.out.println("[ERROR] Trama no valida: " + e.getMessage());
                    }

                }
            } catch (Exception e) {
                System.out.println("[ERROR] Receptor de broadcast: " + e.getMessage());
            }
        });
        hiloReceptor.setDaemon(true); // Permite que el hilo muera automaticamente al cerrar el cliente
        hiloReceptor.start();

        // =========================================================================
        // HILO PRINCIPAL (PANEL DE CONTROL INTERACTIVO)
        // Gestiona la interaccion con el usuario por consola y el envio unicast.
        // =========================================================================
        Scanner scanner = new Scanner(System.in);

        while (true) {
            try {
                System.out.println("\n==================================");
                System.out.println("     PANEL DE CONTROL CLIENTE     ");
                System.out.println("==================================");
                System.out.print("Introduce el ID del servidor al que quieres mandar la orden (ej: 1, 2, 3...): ");

                int idServidor = scanner.nextInt();
                int puertoDestinoControl = 6000 + idServidor; // Calculo del puerto unicast por ID

                System.out.println("Comprobando conexion con el Servidor " + idServidor + "...");

                // =========================================================================
                // MECANISMO DE VERIFICACION (PING RAPIDO CON TIMEOUT):
                // En lugar de asumir que el servidor existe, enviamos un paquete ligero
                // ("PING")
                // al puerto de control especifico. Si responde, esta activo. Si no, salta la
                // excepcion y evitamos abrir el menu para un servidor apagado o inexistente.
                // =========================================================================
                try (DatagramSocket socketCheck = new DatagramSocket()) {
                    byte[] bufferCheck = "PING".getBytes();
                    InetAddress ipDestino = InetAddress.getByName("127.0.0.1");
                    DatagramPacket paqueteCheck = new DatagramPacket(bufferCheck, bufferCheck.length, ipDestino,
                            puertoDestinoControl);
                    socketCheck.send(paqueteCheck);

                    socketCheck.setSoTimeout(1500); // 1.5 segundos de margen maximo de respuesta
                    byte[] bufferResp = new byte[1024];
                    DatagramPacket paqueteResp = new DatagramPacket(bufferResp, bufferResp.length);
                    socketCheck.receive(paqueteResp);
                } catch (java.net.SocketTimeoutException e) {
                    System.out.println("\n[AVISO] El Servidor " + idServidor
                            + " no esta activo o no existe. Imposible mandar ordenes.");
                    continue; // Vuelve arriba al menu principal de forma limpia
                }

                // =========================================================================
                // SUBMENU DE ACCIONES (EXIGIDAS EN EL BOLETIN)
                // Si el servidor existe, se despliegan las opciones de control.
                // =========================================================================
                System.out.println("\n--- ACCIONES PARA EL SERVIDOR " + idServidor + " ---");
                System.out.println("1. Cambiar formato (XML / JSON)");
                System.out.println("2. Cambiar frecuencia de transmision");
                System.out.println("3. Cambiar unidades de una variable");
                System.out.println("4. Activar / Desactivar envio de datos");
                System.out.print("Selecciona una accion (1-4): ");

                int accion = scanner.nextInt();
                scanner.nextLine(); // Limpiar el salto de linea pendiente

                String operacion;
                String parametro;

                switch (accion) {
                    case 1:
                        System.out.print("Introduce el nuevo formato (XML o JSON): ");
                        operacion = "FORMATO";
                        parametro = scanner.nextLine();
                        break;
                    case 2:
                        System.out.print("Introduce la nueva frecuencia en segundos: ");
                        operacion = "FRECUENCIA";
                        parametro = scanner.nextLine();
                        break;
                    case 3:
                        System.out.print("Introduce variable y nuevas unidades (ej: Temp:Fahrenheit): ");
                        operacion = "UNIDADES";
                        parametro = scanner.nextLine();
                        break;
                    case 4:
                        System.out.print("¿Activar (ON) o Desactivar (OFF)?: ");
                        operacion = "ENVIO";
                        parametro = scanner.nextLine();
                        break;
                    default:
                        System.out.println("[!] Opcion no valida. Cancelando envio.");
                        continue;
                }

                // =========================================================================
                // ENVIO DE LA ORDEN DE CONTROL POR UNICAST Y ESPERA DE ACK
                // =========================================================================
                try (DatagramSocket socketEnvio = new DatagramSocket()) {
                    MensajeControl orden = new MensajeControl(idServidor, operacion, parametro);
                    byte[] bufferEnvio = new Trama("JSON", "CONTROL", JsonSerializador.serializar(orden)).codificar();
                    InetAddress ipDestino = InetAddress.getByName("127.0.0.1");

                    DatagramPacket paqueteControl = new DatagramPacket(bufferEnvio, bufferEnvio.length, ipDestino,
                            puertoDestinoControl);
                    socketEnvio.send(paqueteControl);
                    System.out.println(
                            "-> Comando enviado al Servidor " + idServidor + " (Puerto " + puertoDestinoControl + ")");

                    // Espera de la respuesta exclusiva del servidor (ACK)
                    socketEnvio.setSoTimeout(3000);
                    byte[] bufferRespuesta = new byte[4096];
                    DatagramPacket paqueteRespuesta = new DatagramPacket(bufferRespuesta, bufferRespuesta.length);

                    socketEnvio.receive(paqueteRespuesta);
                    Trama tr = Trama.decodificar(paqueteRespuesta.getData(), paqueteRespuesta.getLength());
                    RespuestaControl respuesta = JsonSerializador.deserializarRespuesta(tr.getCuerpo());
                    System.out.println("[RESPUESTA] " + respuesta);
                    Archivos.anadir("Servidor" + idServidor, "respuestas.txt", tr.getCuerpo() + System.lineSeparator());

                } catch (Exception e) {
                    e.printStackTrace();
                }

            } catch (Exception e) {
                // Control general de errores: Si se introduce una letra por error o se cruza
                // texto,
                // vaciamos por completo el buffer del scanner para evitar bucles infinitos.
                System.out.println("\n[ERROR] Entrada no valida o error inesperado. Vuelve a intentarlo.");
                scanner.nextLine();
            }
        }
    }
}