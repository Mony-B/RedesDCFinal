package servidor.udp;

//import servidor.udp.ServidorEscuchaUDP;

public class ServidorUDP{
    public final int PUERTO_SERVER;
    
    public ServidorUDP(int puertoS){
        PUERTO_SERVER=puertoS;
    }
    public void inicia()throws Exception{
        ServidorEscuchaUDP2 servidorUDP=new ServidorEscuchaUDP2(PUERTO_SERVER);
        
        servidorUDP.start();
    }
}
