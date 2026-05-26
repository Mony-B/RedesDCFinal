package cliente.tcp;

public class PruebaClienteTCP{
    public static void main(String args[])throws Exception{
        ClienteTCP clienteTCP =new ClienteTCP("10.10.26.10",60000);
             
        clienteTCP.inicia();
    }
}
