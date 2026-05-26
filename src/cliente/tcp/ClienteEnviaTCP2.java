package cliente.tcp;

import datos.EntradaSalida;
import java.net.*;
import java.io.*;
import javax.swing.JFileChooser; 

public class ClienteEnviaTCP2 extends Thread {
    protected Socket socket; 
    protected final int PUERTO_SERVER;
    protected final String SERVER;

    public ClienteEnviaTCP2(String servidor, int puertoS) throws Exception {
        PUERTO_SERVER = puertoS;
        SERVER = servidor;
        socket = new Socket(SERVER, PUERTO_SERVER);
    }

    @Override
    public void run() {
        DataOutputStream out = null;
        try {
            EntradaSalida.mostrarMensaje("Cliente conectado con servidor " + socket.getInetAddress()
                    + ":" + socket.getPort() + "\n");

            out = new DataOutputStream(socket.getOutputStream());

            // MONY/FER: Aquí arranca la selección visual del archivo
            enviaArchivo(out);

        } catch (Exception e) {
            System.err.println("Error cliente: " + e.getMessage());
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
        // 1. Abrimos la ventanita para elegir el archivo
        JFileChooser selector = new JFileChooser();
        selector.setDialogTitle("Selecciona el archivo para enviar");
        int resultado = selector.showOpenDialog(null);

        // 2. Al dar click en abrir
        if (resultado == JFileChooser.APPROVE_OPTION) {
            File archivo = selector.getSelectedFile(); 

            // 3. Mandamos el nombre y el peso al servidor
            out.writeUTF(archivo.getName());
            out.writeLong(archivo.length());

            FileInputStream fis = new FileInputStream(archivo);
            byte[] buffer = new byte[4096];
            int bytesLeidos;
            
            long totalBytes = archivo.length();
            long bytesEnviados = 0;
            
            //MONY/FER: Arrancamos el cronómetro justo antes de empezar a mover bytes
            long tiempoInicio = System.currentTimeMillis();
            
            EntradaSalida.mostrarMensaje("🚀 Iniciando envío de: " + archivo.getName() + " (" + totalBytes + " bytes)...\n");

            while ((bytesLeidos = fis.read(buffer)) != -1) {
                out.write(buffer, 0, bytesLeidos);
                bytesEnviados += bytesLeidos;

                // --- CÁLCULOS DINÁMICOS 
                long tiempoActual = System.currentTimeMillis();
                long tiempoTranscurrido = tiempoActual - tiempoInicio;
                
                if (tiempoTranscurrido > 0) {
                    double velocidadBps = (bytesEnviados * 8.0) / (tiempoTranscurrido / 1000.0);
                    long bytesRestantes = totalBytes - bytesEnviados;
                    double tiempoRestanteSeg = (bytesRestantes * 8.0) / velocidadBps;

                    // MONY/FER: Imprimimos progreso cada 10% para no saturar la consola
                    if (bytesEnviados % (totalBytes / 10 + 1) == 0) { 
                        EntradaSalida.mostrarMensaje(
                            String.format(">> Progreso: %.1f%% | Transcurrido: %.1fs | Restante est.: %.1fs | Vel: %.2f bps\n", 
                            (bytesEnviados * 100.0 / totalBytes), 
                            (tiempoTranscurrido / 1000.0), 
                            tiempoRestanteSeg, 
                            velocidadBps)
                        );
                    }
                }
            }
            out.flush();
            fis.close();
            EntradaSalida.mostrarMensaje("¡Transferencia completada!\n");
        } else {
            EntradaSalida.mostrarMensaje("Envío cancelado por el usuario.\n");
        }
    }
}