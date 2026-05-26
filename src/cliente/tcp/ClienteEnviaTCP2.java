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

    // Constructor obligatorio PÚBLICO con los 4 parámetros exactos
    public ClienteEnviaTCP2(String servidor, int puertoS, File archivo, JTextArea areaChatUI) throws Exception {
        this.PUERTO_SERVER = puertoS;
        this.SERVER = servidor;
        this.archivoAEnviar = archivo;
        this.areaChatUI = areaChatUI;
        this.socket = new Socket(SERVER, PUERTO_SERVER);
    }

    @Override
    public void run() {
        DataOutputStream out = null;
        try {
            mostrarEnChat("[TCP]: Conectado con servidor de archivos " + socket.getInetAddress()
                    + ":" + socket.getPort() + "\n");

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
        if (archivoAEnviar != null && archivoAEnviar.exists()) {
            out.writeUTF(archivoAEnviar.getName());
            out.writeLong(archivoAEnviar.length());

            FileInputStream fis = new FileInputStream(archivoAEnviar);
            byte[] buffer = new byte[4096];
            int bytesLeidos;
            
            long totalBytes = archivoAEnviar.length();
            long bytesEnviados = 0;
            long tiempoInicio = System.currentTimeMillis();
            
            mostrarEnChat("[TCP]: Iniciando envío de: " + archivoAEnviar.getName() + " (" + totalBytes + " bytes)...\n");

            while ((bytesLeidos = fis.read(buffer)) != -1) {
                out.write(buffer, 0, bytesLeidos); 
                bytesEnviados += bytesLeidos; 

                long tiempoActual = System.currentTimeMillis();
                long tiempoTranscurridoMs = tiempoActual - tiempoInicio;
                
                if (tiempoTranscurridoMs > 0) {
                    double velocidadBps = (bytesEnviados * 8.0) / (tiempoTranscurridoMs / 1000.0);
                    long bytesRestantes = totalBytes - bytesEnviados;
                    double tiempoRestanteSeg = (bytesRestantes * 8.0) / velocidadBps;
                    double progreso = (bytesEnviados * 100.0) / totalBytes;

                    mostrarEnChat(String.format(">> Progreso: %.1f%% | Restante: %.1fs | Vel: %.2f bps\n", 
                            progreso, tiempoRestanteSeg, velocidadBps));
                }
            }
            out.flush();
            
            long tiempoFin = System.currentTimeMillis();
            fis.close(); 
            
            long latenciaTotalMs = tiempoFin - tiempoInicio;
            double latenciaTotalSeg = latenciaTotalMs / 1000.0;
            if (latenciaTotalSeg == 0) latenciaTotalSeg = 0.001; 

            double bpsFinal = (totalBytes * 8.0) / latenciaTotalSeg;

            mostrarEnChat("\n🎉 ¡Transferencia TCP completada con éxito!\n");
            mostrarEnChat("⏱️ Latencia Total: " + latenciaTotalSeg + " segundos\n");
            mostrarEnChat("⚡ Velocidad promedio: " + String.format("%.2f", bpsFinal) + " bps\n\n");
            
        } else {
            mostrarEnChat("[TCP Error]: El archivo no existe o no fue seleccionado.\n");
        }
    }

    private void mostrarEnChat(String texto) {
        if (areaChatUI != null) {
            areaChatUI.append(texto);
            areaChatUI.setCaretPosition(areaChatUI.getDocument().getLength());
        }
    }
}