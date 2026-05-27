package cliente.tcp;

public class PruebaClienteTCP{
    public static void main(String args[])throws Exception{

        ClienteTCP clienteTCP =new ClienteTCP("26.198.149.216",60000);

             
        clienteTCP.inicia();
    }
}
