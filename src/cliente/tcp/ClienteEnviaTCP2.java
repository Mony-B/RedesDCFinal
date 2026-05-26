package cliente.tcp;

import datos.EntradaSalida;
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
        DataOutputStream out = null;

        try {
            EntradaSalida.mostrarMensaje( "Cliente conectado con servidor "+ socket.getInetAddress()
                                            + ":" + socket.getPort() + "\n");

            // Crea flujo de salida de red al socket
            // MONY/FER: Este es como si fuera el tubo por donde vamos a aventar los bytes del 
            // archivo a la red.
            out = new DataOutputStream(socket.getOutputStream());

            // Invocamos a método que envía datos por la red 
            enviaArchivo(out);

        }
        catch (Exception e) {
            System.err.println("Error cliente: "+ e.getMessage());
        }
        finally {
            // cerrar recursos
            try {
                if (out != null) //flujos de socket
                    out.close();

                if (socket != null && !socket.isClosed()) { //cerramos socket
                    socket.close();
                }
            }
            catch (Exception e) {
                System.err.println("Error cerrando recursos: "+ e.getMessage());
            }
        }
    }

    // MONY/FER: Transformamos el 'enviaMensaje' en 'enviaArchivo'. 
    // Aquí es donde vamos calcular la latencia y la velocidad.
    private void enviaArchivo(DataOutputStream out) throws Exception {

        // 1. Apuntamos al archivo físico que pusimos en la carpeta
        File archivo = new File("src/archivos_enviados/besties.jpeg"); 

        if (!archivo.exists()) {
            EntradaSalida.mostrarMensaje("❌ ERROR: No se encontró el archivo.\n");
            return; // Si no hay foto, abortamos la misión
        }

        // 2. Le decimos al servidor cómo se llama el archivo y cuánto pesa para que se prepare
        out.writeUTF(archivo.getName());  // enviar UTF por el socket (Nombre)
        out.writeLong(archivo.length());  // Enviamos el peso total

        // 3. Preparamos la lectura del disco duro
        FileInputStream fis = new FileInputStream(archivo);
        byte[] buffer = new byte[4096]; // Este es el que transporta bytes de 4 en 4 KB
        int bytesLeidos;

        EntradaSalida.mostrarMensaje("Enviando archivo \"" + archivo.getName() + "\"...\n");

        // --- PUNTO 3 DEL PROYECTO: Cálculooos ---
        //MONY/FER: Justo antes de que salga el primer byte, tomamos la hora exacta en milisegundos
        long tiempoInicio = System.currentTimeMillis();

        // 4. El ciclo que lee la foto de la compu y la empuja al socket
        while ((bytesLeidos = fis.read(buffer)) != -1) {
            out.write(buffer, 0, bytesLeidos);
        }
        
        // forzar envío
        out.flush();

        //MONY/FER: Terminó de enviarse y Tomamos la hora exacta en la que terminó
        long tiempoFin = System.currentTimeMillis();
        fis.close(); // Cerramos la lectura del disco

        // 5. Calculamos la Latencia (Tiempo total que tardó el viaje)
        long latenciaMs = tiempoFin - tiempoInicio;
        double latenciaSeg = latenciaMs / 1000.0; // Lo pasamos a segundos
        
        // Trampita por si la compu es muy rápida y da 0 (para que no explote la división)
        if (latenciaSeg == 0) latenciaSeg = 0.001; 

        // 6. Calculamos la Tasa de Transferencia (bps = bits por segundo)
        // La fórmula es: (Bytes del archivo * 8 para hacerlo bits) / Segundos que tardó
        double bits = archivo.length() * 8;
        double bps = bits / latenciaSeg;

        EntradaSalida.mostrarMensaje("¡Archivo enviado con éxito jiji!\n");
        EntradaSalida.mostrarMensaje("Latencia (Tiempo total): " + latenciaSeg + " segundos\n");
        EntradaSalida.mostrarMensaje("Tasa de transferencia: " + String.format("%.2f", bps) + " bps\n\n");
    }
}