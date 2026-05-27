package cliente.tcp;

import javax.swing.JTextArea;
import java.io.*;
import java.net.*;

public class ClienteEnviaTCP2 extends Thread {
    protected Socket socket; 
    protected final int PUERTO_SERVER;
    protected final String SERVER;
    private File archivoAEnviar;
    private JTextArea areaChatUI; 

    // constructor para recibir datos de la red, el archivo y la ventana
    public ClienteEnviaTCP2(String servidor, int puertoS, File archivo, JTextArea areaChatUI) throws Exception {
        this.PUERTO_SERVER = puertoS;
        this.SERVER = servidor;
        this.archivoAEnviar = archivo;
        this.areaChatUI = areaChatUI;
        // Primitiva de CONNECT, abre canal directo con el server por TCP
        this.socket = new Socket(SERVER, PUERTO_SERVER);
    }

    @Override
    public void run() {
        DataOutputStream out = null;
        try {
            mostrarEnChat("[TCP]: Conectado con servidor de archivos " + socket.getInetAddress()
                    + ":" + socket.getPort() + "\n");

            // Creamos flujo de salida para mandar los datos al socket
            out = new DataOutputStream(socket.getOutputStream());
            enviaArchivo(out);

        } catch (Exception e) {
            mostrarEnChat("[Error TCP]: " + e.getMessage() + "\n");
        } finally {
            try {
                if (out != null) out.close();
                if (socket != null && !socket.isClosed()) socket.close();
            } catch (Exception e) {
                System.err.println("Error cerrando recursos: " + e.getMessage());
            }
        }
    }

    private void enviaArchivo(DataOutputStream out) throws Exception {
        // validamos q el archivo si exista para q no truene
        if (archivoAEnviar != null && archivoAEnviar.exists()) {
            // Mandamos primero los datos basicos del archivo q va a llegar
            out.writeUTF(archivoAEnviar.getName());
            out.writeLong(archivoAEnviar.length());

            FileInputStream fis = new FileInputStream(archivoAEnviar);
            byte[] buffer = new byte[4096]; // El carrito de 4KB para ir cargando los bytes
            int bytesLeidos;
            
            long totalBytes = archivoAEnviar.length();
            long bytesEnviados = 0; // contador de bytes mandados a la red
            long tiempoInicio = System.currentTimeMillis(); // tiempo inicial para la latencia
            
            mostrarEnChat("[TCP]: Iniciando envío de: " + archivoAEnviar.getName() + " (" + totalBytes + " bytes)...\n");

            // El ciclo lee el archivo en pedacitos y los avienta al flujo
            while ((bytesLeidos = fis.read(buffer)) != -1) {
                out.write(buffer, 0, bytesLeidos); 
                bytesEnviados += bytesLeidos; 

                long tiempoActual = System.currentTimeMillis();
                long tiempoTranscurridoMs = tiempoActual - tiempoInicio;
                
                // si ya paso tiempo empezamos a calcular las tasas
                if (tiempoTranscurridoMs > 0) {
                    // Formula de bits por segundo multiplicando por 8.0
                    double velocidadBps = (bytesEnviados * 8.0) / (tiempoTranscurridoMs / 1000.0);
                    long bytesRestantes = totalBytes - bytesEnviados;
                    // calcula cuanto falta con la velocidad actual
                    double tiempoRestanteSeg = (bytesRestantes * 8.0) / velocidadBps;
                    double progreso = (bytesEnviados * 100.0) / totalBytes;

                    mostrarEnChat(String.format(">> Progreso: %.1f%% | T. Transcurrido: %.1fs | T. Restante: %.1fs | Velocidad: %.2f bps\n", 
                            progreso, (tiempoTranscurridoMs / 1000.0), tiempoRestanteSeg, velocidadBps));
                }
            }
            out.flush(); // vacia el flujo para q se mande todo completo
            
            long tiempoFin = System.currentTimeMillis(); // tiempo final cuando termina de enviar
            fis.close(); 
            
            // sacamos el calculo final de latencia y velocidad promedio
            long latenciaTotalMs = tiempoFin - tiempoInicio;
            double latenciaTotalSeg = latenciaTotalMs / 1000.0;
            if (latenciaTotalSeg == 0) latenciaTotalSeg = 0.001; 

            double bpsFinal = (totalBytes * 8.0) / latenciaTotalSeg;

            mostrarEnChat("\nTransferencia completada correctamente!\n");
            mostrarEnChat("Latencia Total: " + latenciaTotalSeg + " segundos\n");
            mostrarEnChat("Tasa de transferencia: " + String.format("%.2f", bpsFinal) + " bps\n\n");
            
        } else {
            mostrarEnChat("[TCP Error]: El archivo no existe o no fue seleccionado.\n");
        }
    }

    // imprime los textos en el area de texto de la pantalla visual
    private void mostrarEnChat(String texto) {
        if (areaChatUI != null) {
            areaChatUI.append(texto);
            areaChatUI.setCaretPosition(areaChatUI.getDocument().getLength());
        }
    }
}