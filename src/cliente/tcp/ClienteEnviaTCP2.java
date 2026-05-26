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
        // MONY/FER: Se crea el objeto para abrir la ventana exploradora de archivos de Windows/Mac
        JFileChooser selector = new JFileChooser();
        selector.setDialogTitle("Selecciona el archivo para enviar");
        
        // Abre la ventana y guarda la accion del usuario (si le dio Aceptar o Cancelar)
        int resultado = selector.showOpenDialog(null);

        // Si el usuario selecciono un archivo y le dio "Abrir"
        if (resultado == JFileChooser.APPROVE_OPTION) {
            // Obtenemos la ruta absoluta del archivo seleccionado
            File archivo = selector.getSelectedFile(); 

            // PRIMERO: Le avisamos al servidor como se llama el archivo y cuanto pesa en total
            out.writeUTF(archivo.getName());
            out.writeLong(archivo.length());

            // Preparamos el flujo para leer el archivo desde el disco duro
            FileInputStream fis = new FileInputStream(archivo);
            
            // El buffer es el  que transporta los bytes en bloques de 4KB para no saturar la RAM
            byte[] buffer = new byte[4096];
            int bytesLeidos;
            
            long totalBytes = archivo.length();
            long bytesEnviados = 0;
            
            // Tomamos el tiempo exacto antes de mandar el primer byte para calcular la latencia
            long tiempoInicio = System.currentTimeMillis();
            
            EntradaSalida.mostrarMensaje("Iniciando envio de: " + archivo.getName() + " (" + totalBytes + " bytes)...\n");

            // SEGUNDO: Ciclo que lee el archivo por pedazos y los empuja al socket
            while ((bytesLeidos = fis.read(buffer)) != -1) {
                out.write(buffer, 0, bytesLeidos); // Escribe el pedazo en la red
                bytesEnviados += bytesLeidos; // Actualizamos el contador de lo que ya se fue

                // TERCERO: Matematicas en tiempo real (Punto 3 de la rubrica)
                long tiempoActual = System.currentTimeMillis();
                long tiempoTranscurridoMs = tiempoActual - tiempoInicio;
                
                // Solo validamos que el tiempo sea mayor a 0 para no dividir entre cero
                if (tiempoTranscurridoMs > 0) {
                    // Formula: (Bytes enviados pasados a bits) / (Milisegundos pasados a segundos)
                    double velocidadBps = (bytesEnviados * 8.0) / (tiempoTranscurridoMs / 1000.0);
                    
                    // Restamos el total menos lo enviado para saber cuanto falta
                    long bytesRestantes = totalBytes - bytesEnviados;
                    
                    // Estimacion de tiempo restante usando la velocidad actual
                    double tiempoRestanteSeg = (bytesRestantes * 8.0) / velocidadBps;
                    
                    // Regla de 3 para sacar el porcentaje de progreso
                    double progreso = (bytesEnviados * 100.0) / totalBytes;

                    // Imprime los calculos dinamicos en cada vuelta del ciclo
                    EntradaSalida.mostrarMensaje(
                        String.format(">> Progreso: %.1f%% | Transcurrido: %.3fs | Restante est.: %.1fs | Vel: %.2f bps\n", 
                        progreso, 
                        (tiempoTranscurridoMs / 1000.0), 
                        tiempoRestanteSeg, 
                        velocidadBps)
                    );
                }
            }
            // Forzamos que se vacie el tubo de red por si quedo algun byte atorado
            out.flush();
            
            // Tomamos el tiempo final justo cuando termina el ciclo
            long tiempoFin = System.currentTimeMillis();
            fis.close(); // Cerramos la lectura del disco
            
            // CUARTO: Resumen final de la transferencia
            long latenciaTotalMs = tiempoFin - tiempoInicio;
            double latenciaTotalSeg = latenciaTotalMs / 1000.0;
            
            // Trampita por si se envio muy rapido (casi en 0s) y evitar errores de division
            if (latenciaTotalSeg == 0) latenciaTotalSeg = 0.001; 

            // Tasa de transferencia final promedio
            double bpsFinal = (totalBytes * 8.0) / latenciaTotalSeg;

            EntradaSalida.mostrarMensaje("\nTransferencia completada!\n");
            EntradaSalida.mostrarMensaje("Latencia (Tiempo total): " + latenciaTotalSeg + " segundos\n");
            EntradaSalida.mostrarMensaje("Velocidad promedio: " + String.format("%.2f", bpsFinal) + " bps\n\n");
            
        } else {
            // Si se cerro la ventanita sin elegir nada
            EntradaSalida.mostrarMensaje("Envio cancelado por el usuario.\n");
        }
    }
}