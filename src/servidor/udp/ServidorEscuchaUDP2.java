package servidor.udp;

import datos.EntradaSalida;
import datos.Mensaje;

import java.net.*;
import java.nio.charset.StandardCharsets;

public class ServidorEscuchaUDP2 extends Thread {
    protected DatagramSocket socket; //socket UDP
    protected final int PUERTO_SERVER;

    // Tamaño de buffer razonable
    private static final int MAX_BUFFER = 1024;

    public ServidorEscuchaUDP2(int puertoS)throws Exception {
        PUERTO_SERVER = puertoS;

        // Crear socket UDP
        socket = new DatagramSocket(PUERTO_SERVER);
        // Timeout opcional
        // socket.setSoTimeout(5000);
    }

    @Override
    public void run() {
        try {
            EntradaSalida.mostrarMensaje("Servidor UDP escuchando en puerto "+ PUERTO_SERVER + "...\n");

            // El servidor UDP normalmente nunca termina
            while (true) {
                try {
                    // recibir datagrama
                    Mensaje mensajeObj = recibeMensaje();

                    // procesar lógica
                    procesaMensaje(mensajeObj);
                }
                catch (SocketTimeoutException e) {
                    EntradaSalida.mostrarMensaje("Timeout esperando paquetes...\n");
                }
                catch (Exception e) {
                    EntradaSalida.mostrarMensaje( "Error procesando paquete: "+ e.getMessage() + "\n");
                }
            }

        }
        catch (Exception e) {
            System.err.println("Error servidor UDP: "+ e.getMessage());
        }
        finally {
            if (socket != null && !socket.isClosed()) { //cerrar socket
                socket.close();
            }
        }
    }

    private void procesaMensaje(Mensaje mensajeObj) throws Exception {

        String mensaje = mensajeObj.getMensaje();
        String respuesta;

        // Comparaciones de msg
        if (mensaje.equalsIgnoreCase("hola")) {
            respuesta = "¿Cómo estás?";
        }
        else if (mensaje.equalsIgnoreCase("bien y tú?")) {
            respuesta = "Estoy pal arrastre, aunque gracias por preguntar";
        }
        else if (mensaje.equalsIgnoreCase("fin")) {
            // NO cerrar servidor
            respuesta = "Cliente finalizó comunicación";
        }
        else {
            respuesta = "Servidor no entiende: " + mensaje;
        }

        mensajeObj.setMensaje(respuesta);
        enviaMensaje(mensajeObj);
    }

    private Mensaje recibeMensaje() throws Exception {

        Mensaje mensajeObj = new Mensaje();
        // buffer recepción
        byte[] buffer = new byte[MAX_BUFFER];

        //Datagrama
        DatagramPacket paquete = new DatagramPacket( buffer, buffer.length);

        // Se queda bloqueante en espera
        socket.receive(paquete);

        // convertir bytes a cadena String
        // 1. Recibir los bytes y pasarlos a String 
        String mensajeRecibido = new String(paquete.getData(), 0, paquete.getLength(), StandardCharsets.UTF_8);

        //Mony: corto el texto justo donde encuentre las barritas "||" para separar el chisme de la prueba matemática
        //Fer: Ojo, en Java las barritas se escapan con doble diagonal invertida (\\) porque el símbolo "|" por sí solo significa "OR" lógico.
        String[] partes = mensajeRecibido.split("\\|\\|"); 

        //Mony: me aseguro de que el mensaje sí se partió en 2 pedazos (el texto y el número)
        if (partes.length == 2) {
            
            String mensajeReal = partes[0]; //Mony: la primera mitad, lo que realmente me quisieron decir
            long checksumDelCliente = Long.parseLong(partes[1]); //Mony: la segunda mitad, el número de seguridad que calculó el cliente
            
            //Mony: yo como servidor no confío, así que hago mi propia suma matemática con el texto que me llegó a ver si es cierto
            long checksumMio = calcularChecksum(mensajeReal.getBytes(StandardCharsets.UTF_8));
            
            //Mony: el momento de la verdad, comparamos si mi suma da exactamente lo mismo que la tuya
            if (checksumMio == checksumDelCliente) {
                
                mensajeObj.setMensaje(mensajeReal); //Mony: Todo chido, guardo el mensaje limpio para mostrarlo
                System.out.println("Checksum validado: El mensaje está completo y todo bieeen.");
                
            } else {
                
                //Mony: Si los números no dan lo mismo, significa que algún byte se perdió,
                //  hubo interferencia en la red o unos asqueles se metieron a morder el cable.
                mensajeObj.setMensaje("ERROR: Mensaje corrupto y malo.");
                System.out.println("ERROR: El Checksum no coincide.");
                
            }
        } else {
            //Mony: Por si llega un mensaje viejo o de prueba que no trae las barritas del Checksum, 
            // lo guardamos así nomás
            mensajeObj.setMensaje(mensajeRecibido); 
        }

        // datos cliente
        mensajeObj.setAddressCliente(paquete.getAddress());
        mensajeObj.setPuertoCliente(paquete.getPort());

        EntradaSalida.mostrarMensaje("Mensaje recibido \"" + mensajeObj.getMensaje() + "\" del cliente "
                + mensajeObj.getAddressCliente() + ":" + mensajeObj.getPuertoCliente()+ "\n");

        return mensajeObj;
    }

    private void enviaMensaje( Mensaje mensajeObj) throws Exception {

        // String a  bytes UTF-8
        byte[] buffer =  mensajeObj.getMensaje().getBytes(StandardCharsets.UTF_8);

        DatagramPacket paquete = new DatagramPacket(buffer, buffer.length, mensajeObj.getAddressCliente(), mensajeObj.getPuertoCliente());

        // envío UDP
        socket.send(paquete);

        EntradaSalida.mostrarMensaje( "Mensaje enviado \"" + mensajeObj.getMensaje() + "\" al cliente "
                + mensajeObj.getAddressCliente() + ":" + mensajeObj.getPuertoCliente() + "\n");
    }

    // Método manual para calcular el Checksum
public long calcularChecksum(byte[] datos) {
    long suma = 0;
    
    //Mony: recorro cada pedacito (byte) del mensaje uno por uno
    for (byte b : datos) {
        //Mony: Java a veces es raro y le pone signo negativo a los bytes grandes. 
        //Fer: Al hacer el "& 0xFF" lo forzamos a que sea un número positivo limpio de 0 a 255.
        suma += (b & 0xFF); 
    }
    //Mony: regreso el total de la suma para pegarlo al mensaje
    return suma;
}
}