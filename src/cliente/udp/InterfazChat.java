package cliente.udp; 

import cliente.tcp.ClienteEnviaTCP2;
import servidor.tcp.ServidorEscuchaTCP2;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

public class InterfazChat extends JFrame {

    // --- VARIABLES ---
    private JTextArea areaTextoChat; 
    private JTextArea areaTextoMonitoreo; 
    
    private JScrollPane scrollPaneChat;
    private JScrollPane scrollPaneMonitoreo;
    
    private JTextField campoMensaje;
    private JButton botonEnviar;
    private JButton botonAdjuntar;
    private JLabel etiquetaArchivo;
    
    private JProgressBar barraProgreso;
    private File archivoSeleccionado;

    // --- CONFIG RED ---
    private final String SERVER_IP = "127.0.0.1"; 
    private final int SERVER_PUERTO = 50000;       

    private DatagramSocket socketUDP;
    private ClienteEscuchaUDP2 hiloEscucha;

    public InterfazChat() {
        setTitle("Sala de Chat - Conexión UDP/TCP");
        setSize(480, 640); // Altura cómoda para la vista dividida permanente
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        try { 
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); 
        } catch(Exception e){}

        // Contenedor principal
        JPanel panelPrincipal = new JPanel(new BorderLayout(10, 10));
        panelPrincipal.setBackground(new Color(255, 235, 240)); 
        panelPrincipal.setBorder(new EmptyBorder(15, 15, 15, 15));
        setContentPane(panelPrincipal);

        // --- CONSOLA DE MENSAJES ---
        areaTextoChat = new JTextArea();
        areaTextoChat.setEditable(false);
        areaTextoChat.setFont(new Font("Segoe UI", Font.PLAIN, 13)); 
        areaTextoChat.setForeground(new Color(50, 50, 50));
        areaTextoChat.setLineWrap(true);
        areaTextoChat.setWrapStyleWord(true);
        areaTextoChat.setBorder(new EmptyBorder(6, 6, 6, 6)); 
        scrollPaneChat = new JScrollPane(areaTextoChat);
        scrollPaneChat.setBorder(BorderFactory.createEmptyBorder());

        // --- CONSOLA DE MONITOREO DE RED ---
        areaTextoMonitoreo = new JTextArea();
        areaTextoMonitoreo.setEditable(false);
        areaTextoMonitoreo.setFont(new Font("Consolas", Font.PLAIN, 11)); 
        areaTextoMonitoreo.setBackground(new Color(245, 240, 242)); 
        areaTextoMonitoreo.setForeground(new Color(120, 80, 95));
        areaTextoMonitoreo.setLineWrap(true);
        areaTextoMonitoreo.setWrapStyleWord(true);
        areaTextoMonitoreo.setBorder(new EmptyBorder(6, 6, 6, 6)); 
        scrollPaneMonitoreo = new JScrollPane(areaTextoMonitoreo);
        scrollPaneMonitoreo.setBorder(BorderFactory.createEmptyBorder());

        
        JSplitPane divisorConsolas = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scrollPaneChat, scrollPaneMonitoreo);
        divisorConsolas.setDividerLocation(260); 
        divisorConsolas.setDividerSize(4);       // Línea divisoria muy sutil
        divisorConsolas.setBorder(BorderFactory.createEmptyBorder());


        JPanel panelMonitoreoCompleto = new JPanel(new BorderLayout());
        panelMonitoreoCompleto.setOpaque(false);
        panelMonitoreoCompleto.setBorder(BorderFactory.createLineBorder(new Color(244, 143, 177), 2, true));

        JPanel barraTituloMonitoreo = new JPanel(new BorderLayout());
        barraTituloMonitoreo.setBackground(new Color(244, 143, 177));
        barraTituloMonitoreo.setBorder(new EmptyBorder(6, 12, 6, 12));

        JLabel tituloMonitoreo = new JLabel("Monitoreo de Tráfico");
        tituloMonitoreo.setFont(new Font("Segoe UI", Font.BOLD, 12));
        tituloMonitoreo.setForeground(Color.WHITE);
        barraTituloMonitoreo.add(tituloMonitoreo, BorderLayout.WEST);

        panelMonitoreoCompleto.add(barraTituloMonitoreo, BorderLayout.NORTH);
        panelMonitoreoCompleto.add(divisorConsolas, BorderLayout.CENTER); // Inserta el panel dividido con tus variables

        panelPrincipal.add(panelMonitoreoCompleto, BorderLayout.CENTER);

        // --- PANEL INFERIOR  ---
        JPanel panelInferior = new JPanel();
        panelInferior.setLayout(new BoxLayout(panelInferior, BoxLayout.Y_AXIS));
        panelInferior.setOpaque(false);
        panelInferior.setBorder(new EmptyBorder(5, 0, 5, 0));

        // Subpanel de archivos
        JPanel panelArchivo = new JPanel(new BorderLayout(10, 5));
        panelArchivo.setOpaque(false);
        
        botonAdjuntar = new JButton("Adjuntar Archivo");
        botonAdjuntar.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        botonAdjuntar.setFocusPainted(false);
        
        etiquetaArchivo = new JLabel("Ningún archivo en cola.");
        etiquetaArchivo.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        etiquetaArchivo.setForeground(new Color(110, 110, 110));
        
        barraProgreso = new JProgressBar(0, 100);
        barraProgreso.setValue(0);
        barraProgreso.setStringPainted(true);
        barraProgreso.setFont(new Font("Segoe UI", Font.BOLD, 10));
        barraProgreso.setForeground(new Color(244, 143, 177)); 
        barraProgreso.setBackground(Color.WHITE);

        JPanel panelInfoArchivo = new JPanel(new GridLayout(2, 1, 2, 2));
        panelInfoArchivo.setOpaque(false);
        panelInfoArchivo.add(etiquetaArchivo);
        panelInfoArchivo.add(barraProgreso);

        panelArchivo.add(botonAdjuntar, BorderLayout.WEST);
        panelArchivo.add(panelInfoArchivo, BorderLayout.CENTER);

        // Subpanel de escritura
        JPanel panelEscribir = new JPanel(new BorderLayout(10, 5));
        panelEscribir.setOpaque(false);
        
        campoMensaje = new JTextField();
        campoMensaje.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        campoMensaje.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 220), 1),
            BorderFactory.createEmptyBorder(6, 8, 6, 8)
        ));
        
        botonEnviar = new JButton("Enviar Mensaje") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isPressed()) {
                    g2.setColor(new Color(210, 100, 135)); 
                } else {
                    g2.setColor(new Color(244, 143, 177)); 
                }
                g2.fill(new java.awt.geom.RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 15, 15));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        botonEnviar.setFont(new Font("Segoe UI", Font.BOLD, 12));
        botonEnviar.setForeground(Color.WHITE); 
        botonEnviar.setContentAreaFilled(false); 
        botonEnviar.setBorderPainted(false);     
        botonEnviar.setFocusPainted(false);
        botonEnviar.setPreferredSize(new Dimension(130, 32)); 
        
        panelEscribir.add(campoMensaje, BorderLayout.CENTER);
        panelEscribir.add(botonEnviar, BorderLayout.EAST);

        panelInferior.add(panelArchivo);
        panelInferior.add(Box.createVerticalStrut(10)); 
        panelInferior.add(panelEscribir);
        panelPrincipal.add(panelInferior, BorderLayout.SOUTH);

        inicializarConexionRed();

        botonEnviar.addActionListener(e -> ejecutarEnvio());
        campoMensaje.addActionListener(e -> ejecutarEnvio());

        botonAdjuntar.addActionListener(e -> {
            JFileChooser selector = new JFileChooser();
            int resultado = selector.showOpenDialog(InterfazChat.this);
            if (resultado == JFileChooser.APPROVE_OPTION) {
                archivoSeleccionado = selector.getSelectedFile();
                etiquetaArchivo.setText(archivoSeleccionado.getName());
                barraProgreso.setValue(0); 
                areaTextoMonitoreo.append("[Sistema]: Archivo en cola -> " + archivoSeleccionado.getName() + "\n");
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
            socketUDP = new DatagramSocket(SERVER_PUERTO);
            
            hiloEscucha = new ClienteEscuchaUDP2(socketUDP, this.areaTextoChat);
            hiloEscucha.start();
            
            ServidorEscuchaTCP2 hiloArchivos = new ServidorEscuchaTCP2(SERVER_PUERTO, this.areaTextoMonitoreo); 
            hiloArchivos.start();

            areaTextoMonitoreo.append("[Red]: Sockets listos en puerto " + SERVER_PUERTO + ".\n");
        } catch (Exception e) {
            areaTextoMonitoreo.append("[Error]: Puerto ocupado.\n");
        }
    }

    private void ejecutarEnvio() {
        String texto = campoMensaje.getText().trim();

        if (!texto.isEmpty()) {
            try {
                byte[] bytesOriginales = texto.getBytes(StandardCharsets.UTF_8);
                long checksum = calcularChecksum(bytesOriginales);
                
                String mensajeEmpacado = texto + "||" + checksum;
                byte[] buffer = mensajeEmpacado.getBytes(StandardCharsets.UTF_8);
                
                InetAddress ipServidor = InetAddress.getByName(SERVER_IP);
                DatagramPacket paquete = new DatagramPacket(buffer, buffer.length, ipServidor, SERVER_PUERTO);
                
                socketUDP.send(paquete);
                
                areaTextoChat.append("Tú: " + texto + "\n");
                areaTextoMonitoreo.append("[UDP]: Mensaje enviado | Checksum: " + checksum + "\n");
                
                campoMensaje.setText(""); 
                scrollAbajo();

                if (texto.equalsIgnoreCase("fin")) {
                    cerrarConexiones();
                    this.dispose();
                    System.exit(0);
                }
            } catch (Exception e) {
                areaTextoMonitoreo.append("[Error UDP]: " + e.getMessage() + "\n");
            }
        }

        if (archivoSeleccionado != null) {
            try {
                areaTextoMonitoreo.append("[TCP]: Iniciando transferencia de \"" + archivoSeleccionado.getName() + "\"...\n");
                
                ClienteEnviaTCP2 hiloArchivo = new ClienteEnviaTCP2(SERVER_IP, SERVER_PUERTO, archivoSeleccionado, this.areaTextoMonitoreo);
                hiloArchivo.start();

                new Thread(() -> {
                    try {
                        for(int i = 0; i <= 100; i += 10) {
                            final int progreso = i;
                            SwingUtilities.invokeLater(() -> barraProgreso.setValue(progreso));
                            Thread.sleep(100); 
                        }
                    } catch(Exception ex){}
                }).start();

            } catch (Exception e) {
                areaTextoMonitoreo.append("[Error TCP]: " + e.getMessage() + "\n");
            }

            archivoSeleccionado = null;
            etiquetaArchivo.setText("Ningún archivo en cola.");
        }
    }

    private void scrollAbajo() {
        SwingUtilities.invokeLater(() -> {
            if (scrollPaneChat != null && scrollPaneChat.getVerticalScrollBar() != null) {
                scrollPaneChat.getVerticalScrollBar().setValue(scrollPaneChat.getVerticalScrollBar().getMaximum());
            }
            if (scrollPaneMonitoreo != null && scrollPaneMonitoreo.getVerticalScrollBar() != null) {
                scrollPaneMonitoreo.getVerticalScrollBar().setValue(scrollPaneMonitoreo.getVerticalScrollBar().getMaximum());
            }
        });
    }

    private void cerrarConexiones() {
        if (hiloEscucha != null) hiloEscucha.detener();
        if (socketUDP != null && !socketUDP.isClosed()) socketUDP.close();
    }

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