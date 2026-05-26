package cliente.udp;

import java.net.*;
//import java.io.*;
 
//declaramos la clase udp
public class ClienteUDP{
    protected final int PUERTO_SERVER;
    protected final String SERVER;
    
    public ClienteUDP(String servidor, int puertoS){
        PUERTO_SERVER=puertoS;
        SERVER=servidor;
    }
    
    public void inicia()throws Exception{
        DatagramSocket socket=new DatagramSocket(); //UDP
        
        //esttá haciendo un cliente q reciba y uno q envíe ambos con el mimso socket, el q envía necesita saber a dónde va a mandar
        // y el q recibe no, por eso lleva más parámetros uno q otro
        ClienteEscuchaUDP2 clienteEnvUDP=new ClienteEscuchaUDP2(socket);
        ClienteEnviaUDP2 clienteEscUDP=new ClienteEnviaUDP2(socket, SERVER, PUERTO_SERVER);
        
        //creamos el proceso de enviar y recibir al mimso tiempo
        clienteEnvUDP.start(); //método run del hilo
        clienteEscUDP.start();
    }
}
