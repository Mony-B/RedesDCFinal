package cliente.tcp;

import datos.EntradaSalida;
import datos.Mensaje;

import java.net.*;
import java.io.*;

public class ClienteEnviaTCP2 extends Thread {
    protected Socket socket; //socket cliente
    protected final int PUERTO_SERVER;
    protected final String SERVER;

    public ClienteEnviaTCP2(String servidor,int puertoS) throws Exception {
        PUERTO_SERVER = puertoS;
        SERVER = servidor;

        // Se hace una invocación a primitva CONNECT
        socket = new Socket(SERVER,PUERTO_SERVER);
    }

    @Override
    public void run() {
        BufferedReader teclado = null;
        DataOutputStream out = null;

        try {
            EntradaSalida.mostrarMensaje( "Cliente conectado con servidor "+ socket.getInetAddress()
                                            + ":" + socket.getPort() + "\n");

            EntradaSalida.mostrarMensaje( "Escribe mensajes (fin para salir)\n");

            // Crear flujo de teclado
            teclado = new BufferedReader( new InputStreamReader(System.in));

            //Crea flujo de salida de red al socket
            out = new DataOutputStream(socket.getOutputStream());

            while (true) {
                //Invocamos a mètodo que envìa datos por la red
                Mensaje mensajeObj =  enviaMensaje(teclado, out);

                // condición de salida
                if (mensajeObj.getMensaje().equalsIgnoreCase("fin")) {
                    EntradaSalida.mostrarMensaje("Cerrando conexión...\n");
                    break;
                }
            }

        }
        catch (Exception e) {
            System.err.println("Error cliente: "+ e.getMessage());
        }
        finally {
            // cerrar recursos
            try {
                if (out != null) //flujos de socket
                    out.close();

                if (teclado != null) //flujos entrada de teclado
                    teclado.close();

                if (socket != null && !socket.isClosed()) { //cerramos socket
                    socket.close();
                }
            }
            catch (Exception e) {
                System.err.println("Error cerrando recursos: "+ e.getMessage());
            }
        }
    }

    private Mensaje enviaMensaje(BufferedReader teclado, DataOutputStream out) throws Exception {

        Mensaje mensajeObj = new Mensaje();
        // leer teclado
        String mensaje = teclado.readLine();

        // enviar UTF por el socket
        out.writeUTF(mensaje);

        // forzar envío
        out.flush();

        mensajeObj.setMensaje(mensaje);
        mensajeObj.setAddressServidor(socket.getInetAddress());
        mensajeObj.setPuertoServidor(socket.getPort());

        EntradaSalida.mostrarMensaje("Mensaje \""+ mensajeObj.getMensaje()+ "\" enviado a servidor "
                + mensajeObj.getAddressServidor() + ":"+ mensajeObj.getPuertoServidor()+ "\n");

        return mensajeObj;
    }
}