package cliente.tcp;

public class PruebaClienteTCP{
    public static void main(String args[])throws Exception{
        ClienteTCP clienteTCP =new ClienteTCP("26.190.167.15",60000);
             
        clienteTCP.inicia();
    }
}
