package servidor.tcp;

import datos.EntradaSalida;
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
                // MONY/FER: Este es el tubito por donde van a caer los bytes del archivo
                DataInputStream in = new DataInputStream(socket_cli.getInputStream());

                try {
                    // Invocamos nuestro método especial para cachar los bytes y armar el archivo
                    recibeArchivo(in); 
                }
                // Cliente cerró conexión normalmente
                catch (EOFException e) {
                    EntradaSalida.mostrarMensaje( "Cliente desconectado\n");
                }
                // Error de socket
                catch (SocketException e) {
                    EntradaSalida.mostrarMensaje( "Conexión perdida con cliente\n");
                }
                catch (Exception e) {
                    EntradaSalida.mostrarMensaje( "Error recibiendo el archivo: " + e.getMessage() + "\n");
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

    // MÉTODO DE RECIBIR ARCHIVO (Sustituye a recibeMensaje)
    // MONY/FER: Aquí el servidor reconstruye el archivo pedacito a pedacito
    private void recibeArchivo(DataInputStream in) throws Exception {
        
        // Se queda bloqueante en espera de leer los datos del archivo
        // 1. Leemos lo que nos mandó el cliente (Nombre y peso)
        String nombreArchivo = in.readUTF(); // ESTOS DATOS SE LEEN DEL FLUJO (in)
        long tamanoArchivo = in.readLong();

        EntradaSalida.mostrarMensaje("Descargando archivo: " + nombreArchivo + " (" + tamanoArchivo + " bytes)...\n");

        // 2. Preparamos el archivo vacío en la carpeta de recibidos para empezar a rellenarlo
        File archivoDestino = new File("src/archivos_recibidos/Copia_" + nombreArchivo);
        FileOutputStream fos = new FileOutputStream(archivoDestino);

        byte[] buffer = new byte[4096]; // El mismo carrito de 4KB para ir descargando
        int bytesLeidos;
        long bytesRecibidosTotal = 0; // Un contador para saber cuándo parar

        // 3. El ciclo que cacha los bytes de la red y los pone en el disco duro
        // MONY/FER: Esto se repite hasta que los bytes recibidos sean iguales al peso total del archivo
        while (bytesRecibidosTotal < tamanoArchivo && 
              (bytesLeidos = in.read(buffer, 0, (int)Math.min(buffer.length, tamanoArchivo - bytesRecibidosTotal))) != -1) {
            
            fos.write(buffer, 0, bytesLeidos);
            bytesRecibidosTotal += bytesLeidos; // Sumamos lo que acaba de llegar al contador
        }

        fos.close(); // Cerramos la escritura
        EntradaSalida.mostrarMensaje("¡Archivo guardado correctamente en src/archivos_recibidos/\n");
    }
}