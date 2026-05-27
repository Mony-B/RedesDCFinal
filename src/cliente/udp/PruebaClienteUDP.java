package cliente.udp;

public class PruebaClienteUDP{
    public static void main(String args[]) throws Exception{
        ClienteUDP clienteUDP =new ClienteUDP("26.190.167.15",50000);
        
       // Arrancamos los hilos de ejecución para enviar y recibir mensajes 
        clienteUDP.inicia();
    }
}
