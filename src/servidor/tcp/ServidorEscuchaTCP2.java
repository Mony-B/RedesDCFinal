package servidor.tcp;

import datos.EntradaSalida;
import javax.swing.JTextArea; // <-- AGREGAMOS ESTO PARA LA INTERFAZ
import java.net.*;
import java.io.*;

public class ServidorEscuchaTCP2 extends Thread {
    protected ServerSocket socket; //Socket servidor
    protected Socket socket_cli; //Socket de datos cliente
    protected final int PUERTO_SERVER;
    private JTextArea areaChatUI; // <-- VARIABLE PARA GUARDAR LA REFERENCIA DE LA VENTANA

    // Modificamos el constructor para recibir el JTextArea de la interfaz
    public ServidorEscuchaTCP2(int puertoS, JTextArea areaChatUI) throws Exception {
        PUERTO_SERVER = puertoS;
        // Primitiva de LISTEN, crea socket con Ip (implìcita activa) y puerto
        socket = new ServerSocket(PUERTO_SERVER);
        this.areaChatUI = areaChatUI; // <-- GUARDAMOS LA REFERENCIA
    }

    @Override
    public void run() {
        try {
            mostrarEnChat("[Sistema TCP]: Servidor escuchando en puerto " + PUERTO_SERVER + "...\n");

            // El servidor queda esperando clientes siempre
            while (true) {
                // Primitiva ACCEPT, acepta conexiones de clientes //SOLO ESO
                socket_cli = socket.accept();

                mostrarEnChat("[Sistema TCP]: Cliente conectado " + socket_cli.getInetAddress() + ":" + socket_cli.getPort() + "\n");

                // Crear flujo de entrada de datos del socket para ese cliente
                // Este es el conducto por donde van a caer los bytes del archivo
                DataInputStream in = new DataInputStream(socket_cli.getInputStream());

                try {
                    // Invocamos nuestro método especial para cachar los bytes y armar el archivo
                    recibeArchivo(in); 
                }
                // Cliente cerró conexión normalmente
                catch (EOFException e) {
                    mostrarEnChat("[Sistema TCP]: Cliente desconectado\n");
                }
                // Error de socket
                catch (SocketException e) {
                    mostrarEnChat("[Sistema TCP]: Conexión perdida con cliente\n");
                }
                catch (Exception e) {
                    mostrarEnChat("[Error TCP]: Error recibiendo el archivo: " + e.getMessage() + "\n");
                }

                // cerrar socket del cliente
                socket_cli.close();
                mostrarEnChat("[Sistema TCP]: Esperando nuevo cliente...\n");
            }
        }
        catch (Exception e) {
            System.err.println("Error en servidor: " + e.getMessage());
        }
    }

    // MÉTODO DE RECIBIR ARCHIVO (Sustituye a recibeMensaje)
    // Aquí el servidor reconstruye el archivo pedacito a pedacito
    private void recibeArchivo(DataInputStream in) throws Exception {
        
        // Se queda bloqueante en espera de leer los datos del archivo
        // 1. Leemos lo que nos mandó el cliente (Nombre y peso)
        String nombreArchivo = in.readUTF(); // ESTOS DATOS SE LEEN DEL FLUJO (in)
        long tamanoArchivo = in.readLong();

        mostrarEnChat("[Descarga]: Recibiendo archivo: " + nombreArchivo + " (" + tamanoArchivo + " bytes)...\n");

        // 2. Preparamos el archivo vacío en la carpeta de recibidos para empezar a rellenarlo
        File archivoDestino = new File("src/archivos_recibidos/Copia_" + nombreArchivo);
        FileOutputStream fos = new FileOutputStream(archivoDestino);

        byte[] buffer = new byte[4096]; // El mismo flujo de 4KB para ir descargando
        int bytesLeidos;
        long bytesRecibidosTotal = 0; // Un contador para saber cuándo parar

        // 3. El ciclo que cacha los bytes de la red y los pone en el disco duro
        // Esto se repite hasta que los bytes recibidos sean iguales al peso total del archivo
        while (bytesRecibidosTotal < tamanoArchivo && 
              (bytesLeidos = in.read(buffer, 0, (int)Math.min(buffer.length, tamanoArchivo - bytesRecibidosTotal))) != -1) {
            
            fos.write(buffer, 0, bytesLeidos);
            bytesRecibidosTotal += bytesLeidos; // Sumamos lo que acaba de llegar al contador
        }

        fos.close(); // Cerramos la escritura
        mostrarEnChat("[Descarga]: Archivo guardado correctamente en src/archivos_recibidos/\n");
    }

    // Método auxiliar para escribir en la consola de comandos y en la Interfaz Gráfica a la vez
    private void mostrarEnChat(String texto) {
        EntradaSalida.mostrarMensaje(texto); // Mantiene las salidas en tu consola actual
        if (areaChatUI != null) {
            areaChatUI.append(texto); // Agrega el texto al historial visual del chat
            areaChatUI.setCaretPosition(areaChatUI.getDocument().getLength()); // Desplazamiento automático hacia abajo
        }
    }
}