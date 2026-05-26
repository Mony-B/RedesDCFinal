package cliente.udp; 

import cliente.tcp.ClienteEnviaTCP2; 
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

public class InterfazChat extends JFrame {

    private JTextArea areaChat;
    private JTextField campoMensaje;
    private JButton botonEnviar;
    private JButton botonAdjuntar;
    private JLabel etiquetaArchivo;
    private File archivoSeleccionado;

    // --- CONFIGURACION DE RED ---
    private final String SERVER_IP = "26.198.149.216"; 
    private final int SERVER_PUERTO = 50000;       

    private DatagramSocket socketUDP;
    private ClienteEscuchaUDP2 hiloEscucha;

    public InterfazChat() {
        setTitle("Sala de Chat - Redes (UDP + TCP)");
        setSize(500, 550);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        try { 
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); 
        } catch(Exception e){
            System.err.println("No se pudo establecer el Look and Feel: " + e.getMessage());
        }

        areaChat = new JTextArea();
        areaChat.setEditable(false);
        areaChat.setLineWrap(true);
        areaChat.setWrapStyleWord(true); 
        areaChat.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        JScrollPane scrollChat = new JScrollPane(areaChat);
        scrollChat.setBorder(BorderFactory.createTitledBorder("Historial del Chat"));
        add(scrollChat, BorderLayout.CENTER);

        JPanel panelInferior = new JPanel(new BorderLayout(5, 5));
        panelInferior.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));

        JPanel panelArchivo = new JPanel(new BorderLayout(5, 5));
        botonAdjuntar = new JButton("Adjuntar Archivo");
        etiquetaArchivo = new JLabel("Ningun archivo seleccionado.");
        etiquetaArchivo.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        panelArchivo.add(botonAdjuntar, BorderLayout.WEST);
        panelArchivo.add(etiquetaArchivo, BorderLayout.CENTER);

        JPanel panelEscribir = new JPanel(new BorderLayout(5, 5));
        campoMensaje = new JTextField();
        campoMensaje.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        botonEnviar = new JButton("Enviar ->");
        botonEnviar.setBackground(new Color(34, 139, 34));
        botonEnviar.setForeground(Color.WHITE);
        
        panelEscribir.add(campoMensaje, BorderLayout.CENTER);
        panelEscribir.add(botonEnviar, BorderLayout.EAST);

        panelInferior.add(panelArchivo, BorderLayout.NORTH);
        panelInferior.add(panelEscribir, BorderLayout.SOUTH);
        add(panelInferior, BorderLayout.SOUTH);

        inicializarConexionRed();

        botonEnviar.addActionListener(e -> ejecutarEnvio());
        campoMensaje.addActionListener(e -> ejecutarEnvio());

        botonAdjuntar.addActionListener(e -> {
            JFileChooser selector = new JFileChooser();
            int resultado = selector.showOpenDialog(InterfazChat.this);
            if (resultado == JFileChooser.APPROVE_OPTION) {
                archivoSeleccionado = selector.getSelectedFile();
                etiquetaArchivo.setText("Listo: " + archivoSeleccionado.getName());
                areaChat.append("[Local]: Archivo cargado a la cola (" + archivoSeleccionado.getName() + ")\n");
                scrollAbajo();
            }
        });

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cerrarConexiones();
            }
        });
    }

    private void inicializarConexionRed() {
        try {
            //Le agregamos SERVER_PUERTO entre los paréntesis
            socketUDP = new DatagramSocket(SERVER_PUERTO);
            
            hiloEscucha = new ClienteEscuchaUDP2(socketUDP, this.areaChat);
            hiloEscucha.start();
            areaChat.append("[Sistema]: Chat activo. Destino -> " + SERVER_IP + ":" + SERVER_PUERTO + "\n");
        } catch (Exception e) {
            areaChat.append("[Error Red]: El puerto ya está en uso. Cierra las terminales negras.\n");
        }
    }

    private void ejecutarEnvio() {
        String texto = campoMensaje.getText().trim();

        // 1. PROCESAR ENVIO DE TEXTO NORMAL VIA UDP (CON CHECKSUM RESTAURADO)
        if (!texto.isEmpty()) {
            try {
                // Recuperamos la logica del Checksum que tenian en su ClienteEnviaUDP2
                byte[] bytesOriginales = texto.getBytes(StandardCharsets.UTF_8);
                long checksum = calcularChecksum(bytesOriginales);
                
                // Empacamos el mensaje con la barda separadora para que el servidor lo entienda
                String mensajeEmpacado = texto + "||" + checksum;
                byte[] buffer = mensajeEmpacado.getBytes(StandardCharsets.UTF_8);
                
                InetAddress ipServidor = InetAddress.getByName(SERVER_IP);
                DatagramPacket paquete = new DatagramPacket(buffer, buffer.length, ipServidor, SERVER_PUERTO);
                
                socketUDP.send(paquete);
                
                areaChat.append("Tu: " + texto + "\n");
                campoMensaje.setText(""); 
                scrollAbajo();

                if (texto.equalsIgnoreCase("fin")) {
                    areaChat.append("[Sistema]: Solicitando desconexion...\n");
                    cerrarConexiones();
                    this.dispose();
                    System.exit(0);
                }
            } catch (Exception e) {
                areaChat.append("[Error UDP]: " + e.getMessage() + "\n");
                scrollAbajo();
            }
        }

        // 2. PROCESAR ENVIO DE ARCHIVO USANDO TU CLASE CLIENTEENVIATCP2
        if (archivoSeleccionado != null) {
            try {
                areaChat.append("[Local]: Abriendo flujo TCP para enviar \"" + archivoSeleccionado.getName() + "\"...\n");
                
                ClienteEnviaTCP2 hiloArchivo = new ClienteEnviaTCP2(
                    SERVER_IP, 
                    SERVER_PUERTO, 
                    archivoSeleccionado, 
                    this.areaChat
                );
                
                hiloArchivo.start();

            } catch (Exception e) {
                areaChat.append("[Error Canal TCP]: " + e.getMessage() + "\n");
            }

            archivoSeleccionado = null;
            etiquetaArchivo.setText("Ningun archivo seleccionado.");
            scrollAbajo();
        }
    }

    private void scrollAbajo() {
        areaChat.setCaretPosition(areaChat.getDocument().getLength());
    }

    private void cerrarConexiones() {
        if (hiloEscucha != null) {
            hiloEscucha.detener();
        }
        if (socketUDP != null && !socketUDP.isClosed()) {
            socketUDP.close();
        }
    }

    // Metodo necesario para el punto 6 de la rubrica en la interfaz
    private long calcularChecksum(byte[] datos) {
        long suma = 0;
        for (byte b : datos) {
            suma += (b & 0xFF);
        }
        return suma;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new InterfazChat().setVisible(true));
    }
}