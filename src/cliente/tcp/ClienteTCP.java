package cliente.tcp;

public  class ClienteTCP{
    protected final String SERVER;
    protected final int PUERTO_SERVER;
    
    public ClienteTCP(String servidor,int puertoS){
        SERVER=servidor;
        PUERTO_SERVER=puertoS;
    }
    public void inicia() throws Exception {
    // Le pasamos un File vacío y un JTextArea vacío directamente en los parámetros
    ClienteEnviaTCP2 clienteTCP = new ClienteEnviaTCP2(SERVER, PUERTO_SERVER, new java.io.File(""), new javax.swing.JTextArea());
    clienteTCP.start();
}
}
