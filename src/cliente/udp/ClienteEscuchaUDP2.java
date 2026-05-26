package cliente.udp;

import datos.EntradaSalida;
import datos.Mensaje;
import javax.swing.JTextArea; // <-- AGREGAMOS ESTO
import java.net.*;
import java.nio.charset.StandardCharsets;

public class ClienteEscuchaUDP2 extends Thread {
    protected final int PUERTO_CLIENTE;
    protected DatagramSocket socket; 
    private static final int MAX_BUFFER = 1024;
    private volatile boolean ejecutando = true; 
    
    // Referencia al área de texto del Chat UI
    private JTextArea areaChatUI; // <-- AGREGAMOS ESTO

    // Actualizamos el constructor para recibir el JTextArea
    public ClienteEscuchaUDP2(DatagramSocket socketNuevo, JTextArea areaChatUI) {
        socket = socketNuevo;
        PUERTO_CLIENTE = socket.getLocalPort(); 
        this.areaChatUI = areaChatUI; // <-- GUARDAMOS LA REFERENCIA
    }

    @Override 
    public void run() {
        try { 
            mostrarEnChat("Cliente UDP escuchando en puerto " + PUERTO_CLIENTE + "\n");

            while (ejecutando) {
                try { 
                    Mensaje mensajeObj = recibeMensaje();
                    String mensaje = mensajeObj.getMensaje();

                    // Mostrar mensaje recibido en la interfaz gráfica
                    mostrarEnChat("Servidor [" + mensajeObj.getPuertoServidor() + "]: " + mensaje + "\n");

                    if (mensaje.equalsIgnoreCase("fin")) {
                        mostrarEnChat("Servidor finalizó comunicación\n");
                        ejecutando = false;
                    }
                }
                catch (SocketTimeoutException e) {
                    // Opcional: no saturar la UI con "esperando mensajes"
                }
                catch (SocketException e) { 
                    if (socket.isClosed()) {
                        mostrarEnChat("Socket UDP cerrado\n");
                    }
                    else {
                        mostrarEnChat("Error de socket: " + e.getMessage() + "\n");
                    }
                    ejecutando = false;
                }
                catch (Exception e) {
                    mostrarEnChat("Error recibiendo mensaje: " + e.getMessage() + "\n");
                }
            }
        }
        catch (Exception e) {
            System.err.println("Error cliente UDP: " + e.getMessage());
        }
        finally {
            if (socket != null && !socket.isClosed()) {
                socket.close(); 
            }
            mostrarEnChat("Cliente UDP finalizado\n");
        }
    }

    private Mensaje recibeMensaje() throws Exception {
        Mensaje mensajeObj = new Mensaje();
        byte[] buffer = new byte[MAX_BUFFER]; 
        DatagramPacket paquete = new DatagramPacket(buffer, buffer.length); 

        socket.receive(paquete); 

        String mensaje = new String(paquete.getData(), 0, paquete.getLength(), StandardCharsets.UTF_8);
        mensajeObj.setMensaje(mensaje);
        mensajeObj.setAddressServidor(paquete.getAddress());
        mensajeObj.setPuertoServidor(paquete.getPort());

        return mensajeObj;
    }

    // Método auxiliar para escribir tanto en consola como en la Interfaz Gráfica
    private void mostrarEnChat(String texto) {
        EntradaSalida.mostrarMensaje(texto); // Mantiene tu consola actual
        if (areaChatUI != null) {
            areaChatUI.append(texto); // Lo añade visualmente al chat
            areaChatUI.setCaretPosition(areaChatUI.getDocument().getLength()); // Auto-scroll hacia abajo
        }
    }

    public void detener() {
        ejecutando = false;
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }
}