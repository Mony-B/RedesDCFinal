package servidor.tcp;

import datos.EntradaSalida;
import datos.Mensaje;

import java.net.*;
import java.io.*;

public class ServidorEscuchaTCP2 extends Thread {
    protected ServerSocket socket; //Socket servidor
    protected Socket socket_cli; //Socket de datos cliente
    protected final int PUERTO_SERVER;

    public ServidorEscuchaTCP2(int puertoS) throws Exception {
        PUERTO_SERVER = puertoS;
        // Primitiva de LISTEN, crea socket con Ip (implìcita activa) y puerto
        socket = new ServerSocket(PUERTO_SERVER);
    }

    @Override
    public void run() {
        try {
            EntradaSalida.mostrarMensaje("Servidor escuchando en puerto " + PUERTO_SERVER + "...\n");

            // El servidor queda esperando clientes siempre
            while (true) {
                // Primitiva ACCEPT, acepta conexiones de clientes //SOLO ESO
                socket_cli = socket.accept();

                EntradaSalida.mostrarMensaje("Cliente conectado "+ socket_cli.getInetAddress()+ ":" + socket_cli.getPort() + "\n");

                // Crear flujo de entrada de datos del socket para ese cliente
                DataInputStream in = new DataInputStream(socket_cli.getInputStream());

                // Leer mensajes del cliente conectado //TIEMPO INDEFINIDO DE C/CONEXIÓN DE CADA CLIENTE
                while (true) {
                    try {
                        //Se queda esperando mensajes
                        Mensaje mensajeObj = recibeMensaje(in); //EL IN ES EL FLUJO DEL SOCKET

                        if (mensajeObj != null) {
                            EntradaSalida.mostrarMensaje("Mensaje recibido \""+ mensajeObj.getMensaje()
                                    + "\" del cliente "+ mensajeObj.getAddressCliente()+ ":"
                                    + mensajeObj.getPuertoCliente() + "\n");
                        }

                    }
                    // Cliente cerró conexión normalmente
                    catch (EOFException e) {
                        EntradaSalida.mostrarMensaje( "Cliente desconectado\n");
                        break;
                    }
                    // Error de socket
                    catch (SocketException e) {
                        EntradaSalida.mostrarMensaje( "Conexión perdida con cliente\n");
                        break;
                    }
                }
                // cerrar socket del cliente
                socket_cli.close();
                EntradaSalida.mostrarMensaje( "Esperando nuevo cliente...\n");
            }
        }
        catch (Exception e) {
            System.err.println( "Error en servidor: " + e.getMessage());
        }
    }
    //MÉTODO DE RECIBIR MENSAJE
    private Mensaje recibeMensaje(DataInputStream in) throws Exception {
        Mensaje mensajeObj = new Mensaje();

        // Se queda bloqueante en espera de mensajes
        String mensaje = in.readUTF(); //ESTOS DATOS SE LEEN DEL FLUJO (in) Q VIENEN DESDE EL ARGUMENTO
        mensajeObj.setMensaje(mensaje);
        mensajeObj.setAddressCliente(socket_cli.getInetAddress());
        mensajeObj.setPuertoCliente(socket_cli.getPort());

        return mensajeObj;
    }
}