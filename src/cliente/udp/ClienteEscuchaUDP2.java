package cliente.udp;

import datos.EntradaSalida;
import datos.Mensaje;

import java.net.*;
import java.nio.charset.StandardCharsets;

public class ClienteEscuchaUDP2 extends Thread {
    protected final int PUERTO_CLIENTE;
    protected DatagramSocket socket; //socket del cliente
    private static final int MAX_BUFFER = 1024;
    private volatile boolean ejecutando = true; // esta es la misma q del envia, volatil es que: decirle q hay 2 hilos q comparten una variable

    public ClienteEscuchaUDP2(DatagramSocket socketNuevo) {
        socket = socketNuevo;
        PUERTO_CLIENTE =socket.getLocalPort(); // Mony: aquí saca el puerto aleatorio
    }

    @Override //invocan al run, cuando se crea el socket la IP se saca automática
    public void run() {
        try { //esto para q el usuario vea en la pantalla
            EntradaSalida.mostrarMensaje( "Cliente UDP escuchando en puerto "+ PUERTO_CLIENTE + "\n");

            while (ejecutando) {
                try { //este es el método que nos interesa, está dentro del ciclo infinito
                    Mensaje mensajeObj = recibeMensaje();
                    //es un agrupador
                    String mensaje = mensajeObj.getMensaje();

                    // protocolo simple
                    if (mensaje.equalsIgnoreCase("fin")) {
                        EntradaSalida.mostrarMensaje("Servidor finalizó comunicación\n");
                        ejecutando = false;
                    }
                }
                catch (SocketTimeoutException e) {
                    EntradaSalida.mostrarMensaje("Esperando mensajes UDP...\n");
                }
                catch (SocketException e) { //cuando el socket está cerrado marca esta excepción
                    if (socket.isClosed()) {
                        EntradaSalida.mostrarMensaje("Socket UDP cerrado\n");
                    }
                    else {
                        EntradaSalida.mostrarMensaje("Error de socket: "+ e.getMessage() + "\n");
                    }
                    ejecutando = false;
                }
                catch (Exception e) {
                    EntradaSalida.mostrarMensaje(ejecutando +" Error recibiendo mensaje: "+ e.getMessage() + "\n");
                }
            }
        }
        catch (Exception e) {
            System.err.println("Error cliente UDP: "+ e.getMessage());
        }
        finally {
            if (socket != null && !socket.isClosed()) {
                socket.close(); //cerrar socket
            }
            EntradaSalida.mostrarMensaje( "Cliente UDP finalizado\n");
        }
    }

    private Mensaje recibeMensaje() throws Exception {

        Mensaje mensajeObj = new Mensaje();

        // buffer recepción
        byte[] buffer = new byte[MAX_BUFFER]; //MONY: guardamos en una variable de tipo BYTE

        DatagramPacket paquete = new DatagramPacket(buffer,buffer.length); //MONY: creamos un PDU y aquí se guarda lo q recibimos

        // Se queda bloqueante recibiendo
        socket.receive(paquete); //MONY:De aquí lo recibimos, lo llenamos

        // bytes a String correctamente
        String mensaje = new String(paquete.getData(),0, paquete.getLength(), StandardCharsets.UTF_8);
        mensajeObj.setMensaje(mensaje);
        mensajeObj.setAddressServidor(paquete.getAddress());
        mensajeObj.setPuertoServidor(paquete.getPort());

        EntradaSalida.mostrarMensaje("Mensaje recibido \"" + mensajeObj.getMensaje()
                + "\" de servidor " + mensajeObj.getAddressServidor()+ ":"
                + mensajeObj.getPuertoServidor() + "\n");

        return mensajeObj;
    }

    // detener hilo manualmente
    public void detener() {
        ejecutando = false;
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }
}