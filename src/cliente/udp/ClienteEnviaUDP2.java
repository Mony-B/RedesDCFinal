package cliente.udp;

import datos.EntradaSalida;
import datos.Mensaje;

import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

public class ClienteEnviaUDP2 extends Thread {
    protected final int PUERTO_SERVER;
    protected final String SERVER;
    protected DatagramSocket socket; //socket de cliente
    protected InetAddress addressServer;
    private volatile boolean ejecutando = true; //variable en común

    //
    public ClienteEnviaUDP2(DatagramSocket nuevoSocket,String servidor,int puertoServidor) throws Exception {
        socket = nuevoSocket;
        SERVER = servidor;
        PUERTO_SERVER = puertoServidor;

        // resolver dirección - hace la consulta de IP, el nombre lo convierte a IP
        addressServer = InetAddress.getByName(SERVER);
    }

    @Override
    public void run() { 
        BufferedReader teclado = null;

        try {//Mony: le digo q yo como cliente puedo mandar
            EntradaSalida.mostrarMensaje("Cliente UDP listo para mandar...\n");

            // crear teclado
            teclado = new BufferedReader(new InputStreamReader(System.in));
                //ciclo infinito de mandar
            while (ejecutando) {
                Mensaje mensajeObj = enviaMensaje(teclado);

                // salir elegantemente
                if (mensajeObj.getMensaje().equalsIgnoreCase("fin")) {
                    EntradaSalida.mostrarMensaje("Finalizando envío UDP...\n");
                    ejecutando = false;
                }
            }
        }
        catch (Exception e) {
            System.err.println("Error cliente UDP envío: "+ e.getMessage());
        }
        finally {
            try {
                if (teclado != null) {
                    teclado.close(); //cerrar flujo teclado
                }
                if (socket != null && !socket.isClosed()) {
                    socket.close();  //cerrar socket
                }
            }
            catch (Exception e) {
                System.err.println("Error cerrando recursos: "+ e.getMessage());
            }
        }
    }

    private Mensaje enviaMensaje(BufferedReader teclado) throws Exception {
        Mensaje mensajeObj =new Mensaje();

        // leer mensaje teclado
        String mensaje = teclado.readLine();

        //CALCULAMOS EL CHECKSUM
        //Mony: agarramos el texto puro y lo hacemos pedacitos (bytes) para poder contarlo matemáticamente
        byte[] bytesOriginales = mensaje.getBytes(StandardCharsets.UTF_8);

        //Mony: mandamos esos bytes a la calculadora que hicimos abajo para sacar el total
        long checksum = calcularChecksum(bytesOriginales)+1; //AQUI ES PA VER QUE FUNCIONE CON ERROR

        //Mony: le pego el número de seguridad al final del texto para que viajen juntos. 
        // Uso "||" como si fuera una barda para separar el texto del número.
        String mensajeEmpacado = mensaje + "||" + checksum;

        // String -> bytes UTF-8
        //Mony: ahora sí, convierto TODO (mensaje + barda + numero) 
        // en un arreglo de bytes para que el socket lo pueda mandar por la red
        byte[] buffer = mensajeEmpacado.getBytes(StandardCharsets.UTF_8);

        // crear paquete UDP
        DatagramPacket paquete = new DatagramPacket(buffer,buffer.length,addressServer,PUERTO_SERVER);

        // enviar paquete
        socket.send(paquete);

        // llenar objeto mensaje 
        //Guardamos el mensaje original en el obj para que en la pantalla no se vea el '||numero'
        mensajeObj.setMensaje(mensaje);
        mensajeObj.setAddressServidor(paquete.getAddress());
        mensajeObj.setPuertoServidor(paquete.getPort());

        EntradaSalida.mostrarMensaje("Mensaje \""+ mensajeObj.getMensaje()
                + "\" enviado a servidor " + mensajeObj.getAddressServidor()
                + ":" + mensajeObj.getPuertoServidor() + "\n");
        return mensajeObj;
    }

    // detener hilo elegantemente
    public void detener() {
        ejecutando = false;
        if (socket != null  && !socket.isClosed()) {
            socket.close();
        }
    }

    // Método manual para calcular el Checksum
    public long calcularChecksum(byte[] datos) {
        long suma = 0;
        
        //Mony: recorro cada pedacito (byte) del mensaje uno por uno
        for (byte b : datos) {
            //Mony: Java a veces es raro y le pone signo negativo a los bytes grandes 
            //Fer: Al hacer el "& 0xFF" lo forzamos a que sea un número positivo limpio de 0 a 255.
            suma += (b & 0xFF); 
        }
        //Mony: regreso el total de la suma para pegarlo al mensaje
        return suma;
    }
}