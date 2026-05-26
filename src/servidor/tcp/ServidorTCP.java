package servidor.tcp;

public class ServidorTCP{
    protected final int PUERTO_SERVER;
    
    public ServidorTCP(int puertoS){
        PUERTO_SERVER=puertoS;
    }
    
    public void inicia()throws Exception{
        ServidorEscuchaTCP2 servidorTCP=new ServidorEscuchaTCP2(PUERTO_SERVER);
        
        servidorTCP.start(); //run
    }
}
