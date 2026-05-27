package cliente.udp;

import datos.EntradaSalida;
import datos.Mensaje;
import javax.swing.JTextArea; 
import java.net.*;
import java.nio.charset.StandardCharsets;

public class ClienteEscuchaUDP2 extends Thread {
    protected final int PUERTO_CLIENTE;
    protected DatagramSocket socket; 
    private static final int MAX_BUFFER = 1024;
    private volatile boolean ejecutando = true; 
    
    private JTextArea areaChatUI; 

    public ClienteEscuchaUDP2(DatagramSocket socketNuevo, JTextArea areaChatUI) {
        socket = socketNuevo;
        PUERTO_CLIENTE = socket.getLocalPort(); 
        this.areaChatUI = areaChatUI; 
    }

    @Override 
    public void run() {
        try { 
            mostrarEnChat("[Sistema]: Escuchando mensajes UDP en el puerto " + PUERTO_CLIENTE + "\n");

            while (ejecutando) {
                try { 
                    Mensaje mensajeObj = recibeMensaje();
                    
                    // Si el mensaje viene bien, lo mostramos
                    if (mensajeObj != null && mensajeObj.getMensaje() != null) {
                        String mensaje = mensajeObj.getMensaje();

                        if (mensaje.equalsIgnoreCase("fin")) {
                            mostrarEnChat("[Sistema]: El otro usuario se desconectó.\n");
                            ejecutando = false;
                        } else if (!mensaje.startsWith("ERROR")) {
                            // Imprimimos en la interfaz gráfica
                            mostrarEnChat("Fer: " + mensaje + "\n"); //aquí en la lap d Mony dice Fer y en la de Fer dice Mony
                        }
                    }
                }
                catch (SocketTimeoutException e) { }
                catch (SocketException e) { 
                    if (socket.isClosed()) {
                        mostrarEnChat("[Sistema]: Socket cerrado.\n");
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
        }
    }

    private Mensaje recibeMensaje() throws Exception {
        Mensaje mensajeObj = new Mensaje();
        byte[] buffer = new byte[MAX_BUFFER]; 
        DatagramPacket paquete = new DatagramPacket(buffer, buffer.length); 

        socket.receive(paquete); 

        String mensajeRecibido = new String(paquete.getData(), 0, paquete.getLength(), StandardCharsets.UTF_8);
        
        // --- LÓGICA DEL CHECKSUM DIRECTO EN LA INTERFAZ ---
        String[] partes = mensajeRecibido.split("\\|\\|"); 

        if (partes.length == 2) {
            String textoReal = partes[0]; 
            long checksumRecibido = Long.parseLong(partes[1].trim()); 
            
            long checksumCalculado = calcularChecksum(textoReal.getBytes(StandardCharsets.UTF_8));
            
            if (checksumCalculado == checksumRecibido) {
                // Si coinciden los números, mandamos el texto limpio
                mensajeObj.setMensaje(textoReal);
                mostrarEnChat("[Checksum Validado] "); 
            } else {
                mostrarEnChat("[Alerta]: Un mensaje se corrompió en la red y fue descartado.\n");
                mensajeObj.setMensaje("ERROR");
            }
        } else {
            // Si llega sin barras, lo pasamos tal cual (por seguridad)
            mensajeObj.setMensaje(mensajeRecibido);
        }

        mensajeObj.setAddressServidor(paquete.getAddress());
        mensajeObj.setPuertoServidor(paquete.getPort());

        return mensajeObj;
    }

    private void mostrarEnChat(String texto) {
        EntradaSalida.mostrarMensaje(texto); 
        if (areaChatUI != null) {
            areaChatUI.append(texto); 
            areaChatUI.setCaretPosition(areaChatUI.getDocument().getLength()); 
        }
    }

    public void detener() {
        ejecutando = false;
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }

    private long calcularChecksum(byte[] datos) {
        long suma = 0;
        for (byte b : datos) {
            suma += (b & 0xFF); 
        }
        return suma;
    }
}